//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package org.webrtc;

import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.ErrorCallback;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;
import android.os.Handler;
import android.os.SystemClock;
import android.view.WindowManager;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.webrtc.CameraEnumerationAndroid.CaptureFormat;
import org.webrtc.SurfaceTextureHelper.OnTextureFrameAvailableListener;
import org.webrtc.ThreadUtils.ThreadChecker;
import org.webrtc.VideoCapturer.CapturerObserver;

public class VideoCapturerAndroid implements VideoCapturer, PreviewCallback, OnTextureFrameAvailableListener {
    private static final String TAG = "VideoCapturerAndroid";
    private static final int CAMERA_OBSERVER_PERIOD_MS = 2000;
    private static final int CAMERA_FREEZE_REPORT_TIMOUT_MS = 4000;
    private static final int CAMERA_STOP_TIMEOUT_MS = 7000;
    private boolean isDisposed = false;
//    public Camera camera;
    private final Object handlerLock = new Object();
    private Handler cameraThreadHandler;
    private Context applicationContext;
    private final Object cameraIdLock = new Object();
    private int id;
    private CameraInfo info;
    public VideoCapturerAndroid.CameraStatistics cameraStatistics;
    private int requestedWidth;
    private int requestedHeight;
    private int requestedFramerate;
    public CaptureFormat captureFormat;
    private final Object pendingCameraSwitchLock = new Object();
    private volatile boolean pendingCameraSwitch;
    public CapturerObserver frameObserver = null;
    public final VideoCapturerAndroid.CameraEventsHandler eventsHandler;
    public boolean firstFrameReported;
    private static final int NUMBER_OF_CAPTURE_BUFFERS = 3;
    private final Set<byte[]> queuedBuffers = new HashSet();
    private final boolean isCapturingToTexture;
    private SurfaceTextureHelper surfaceHelper;
    private boolean dropNextFrame = false;
    private static final int MAX_OPEN_CAMERA_ATTEMPTS = 3;
    private static final int OPEN_CAMERA_DELAY_MS = 500;
    private int openCameraAttempts;
    private final ErrorCallback cameraErrorCallback = new ErrorCallback() {
        public void onError(int error, Camera camera) {
            String errorMessage;
            if (error == 100) {
                errorMessage = "Camera server died!";
            } else {
                errorMessage = "Camera error: " + error;
            }

            Logging.e("VideoCapturerAndroid", errorMessage);
            if (VideoCapturerAndroid.this.eventsHandler != null) {
                VideoCapturerAndroid.this.eventsHandler.onCameraError(errorMessage);
            }

        }
    };
    private final Runnable cameraObserver = new Runnable() {
        private int freezePeriodCount;

        public void run() {
            int cameraFramesCount = VideoCapturerAndroid.this.cameraStatistics.getAndResetFrameCount();
            int cameraFps = (cameraFramesCount * 1000 + 1000) / 2000;
            Logging.d("VideoCapturerAndroid", "Camera fps: " + cameraFps + ".");
            if (cameraFramesCount == 0) {
                ++this.freezePeriodCount;
                if (2000 * this.freezePeriodCount >= 4000 && VideoCapturerAndroid.this.eventsHandler != null) {
                    Logging.e("VideoCapturerAndroid", "Camera freezed.");
                    if (VideoCapturerAndroid.this.surfaceHelper.isTextureInUse()) {
                        VideoCapturerAndroid.this.eventsHandler.onCameraFreezed("Camera failure. Client must return video buffers.");
                    } else {
                        VideoCapturerAndroid.this.eventsHandler.onCameraFreezed("Camera failure.");
                    }

                    return;
                }
            } else {
                this.freezePeriodCount = 0;
            }

            VideoCapturerAndroid.this.maybePostDelayedOnCameraThread(2000, this);
        }
    };
    public static PreviewCallback previewCallback = null;

    public static VideoCapturerAndroid create(String name, VideoCapturerAndroid.CameraEventsHandler eventsHandler) {
        previewCallback = null;
        return create(name, eventsHandler, false);
    }

    public static VideoCapturerAndroid create(String name, VideoCapturerAndroid.CameraEventsHandler eventsHandler, PreviewCallback targetPreviewCallback) {
        previewCallback = targetPreviewCallback;
        return create(name, eventsHandler, false);
    }

    public static VideoCapturerAndroid create(String name, VideoCapturerAndroid.CameraEventsHandler eventsHandler, boolean captureToTexture) {
        int cameraId = lookupDeviceName(name);
        return cameraId == -1 ? null : new VideoCapturerAndroid(cameraId, eventsHandler, captureToTexture);
    }

    public void printStackTrace() {
        Thread cameraThread = null;
        Object var2 = this.handlerLock;
        Object var3 = this.handlerLock;
        synchronized(this.handlerLock) {
            if (this.cameraThreadHandler != null) {
                cameraThread = this.cameraThreadHandler.getLooper().getThread();
            }
        }

        if (cameraThread != null) {
            StackTraceElement[] cameraStackTraces = cameraThread.getStackTrace();
            if (cameraStackTraces.length > 0) {
                Logging.d("VideoCapturerAndroid", "VideoCapturerAndroid stacks trace:");
                int var4 = cameraStackTraces.length;

                for(int var5 = 0; var5 < var4; ++var5) {
                    StackTraceElement stackTrace = cameraStackTraces[var5];
                    Logging.d("VideoCapturerAndroid", stackTrace.toString());
                }
            }
        }

    }

    public void switchCamera(final VideoCapturerAndroid.CameraSwitchHandler switchEventsHandler) {
        if (Camera.getNumberOfCameras() < 2) {
            if (switchEventsHandler != null) {
                switchEventsHandler.onCameraSwitchError("No camera to switch to.");
            }
        } else {
            Object var2 = this.pendingCameraSwitchLock;
            Object var3 = this.pendingCameraSwitchLock;
            synchronized(this.pendingCameraSwitchLock) {
                if (this.pendingCameraSwitch) {
                    Logging.w("VideoCapturerAndroid", "Ignoring camera switch request.");
                    if (switchEventsHandler != null) {
                        switchEventsHandler.onCameraSwitchError("Pending camera switch already in progress.");
                    }

                    return;
                }

                this.pendingCameraSwitch = true;
            }

            boolean didPost = this.maybePostOnCameraThread(new Runnable() {
                public void run() {
                    VideoCapturerAndroid.this.switchCameraOnCameraThread();
                    synchronized(VideoCapturerAndroid.this.pendingCameraSwitchLock) {
                        VideoCapturerAndroid.this.pendingCameraSwitch = false;
                    }

                    if (switchEventsHandler != null) {
                        switchEventsHandler.onCameraSwitchDone(VideoCapturerAndroid.this.info.facing == 1);
                    }

                }
            });
            if (!didPost && switchEventsHandler != null) {
                switchEventsHandler.onCameraSwitchError("Camera is stopped.");
            }
        }

    }

    public void onOutputFormatRequest(final int width, final int height, final int framerate) {
        this.maybePostOnCameraThread(new Runnable() {
            public void run() {
                VideoCapturerAndroid.this.onOutputFormatRequestOnCameraThread(width, height, framerate);
            }
        });
    }

    public void changeCaptureFormat(final int width, final int height, final int framerate) {
        this.maybePostOnCameraThread(new Runnable() {
            public void run() {
                VideoCapturerAndroid.this.startPreviewOnCameraThread(width, height, framerate);
            }
        });
    }

    public int getCurrentCameraId() {
        Object var1 = this.cameraIdLock;
        Object var2 = this.cameraIdLock;
        synchronized(this.cameraIdLock) {
            return this.id;
        }
    }

    public List<CaptureFormat> getSupportedFormats() {
        return CameraEnumerationAndroid.getSupportedFormats(this.getCurrentCameraId());
    }

    public boolean isCapturingToTexture() {
        return this.isCapturingToTexture;
    }

    private VideoCapturerAndroid(int cameraId, VideoCapturerAndroid.CameraEventsHandler eventsHandler, boolean captureToTexture) {
        this.id = cameraId;
        this.eventsHandler = eventsHandler;
        this.isCapturingToTexture = captureToTexture;
        this.cameraStatistics = new VideoCapturerAndroid.CameraStatistics();
        Logging.d("VideoCapturerAndroid", "VideoCapturerAndroid isCapturingToTexture : " + this.isCapturingToTexture);
    }

    private void checkIsOnCameraThread() {
//        if (Thread.currentThread() != this.cameraThreadHandler.getLooper().getThread()) {
//            throw new IllegalStateException("Wrong thread");
//        }
    }

    private static int lookupDeviceName(String deviceName) {
        Logging.d("VideoCapturerAndroid", "lookupDeviceName: " + deviceName);
        if (deviceName != null && Camera.getNumberOfCameras() != 0) {
            if (deviceName.isEmpty()) {
                return 0;
            } else {
                for(int i = 0; i < Camera.getNumberOfCameras(); ++i) {
                    if (deviceName.equals(CameraEnumerationAndroid.getDeviceName(i))) {
                        return i;
                    }
                }

                return -1;
            }
        } else {
            return -1;
        }
    }

    private boolean maybePostOnCameraThread(Runnable runnable) {
        return this.maybePostDelayedOnCameraThread(0, runnable);
    }

    private boolean maybePostDelayedOnCameraThread(int delayMs, Runnable runnable) {
        Object var3 = this.handlerLock;
        Object var4 = this.handlerLock;
        synchronized(this.handlerLock) {
            return this.cameraThreadHandler != null && this.cameraThreadHandler.postAtTime(runnable, this, SystemClock.uptimeMillis() + (long)delayMs);
        }
    }

    public void dispose() {
        Logging.d("VideoCapturerAndroid", "release");
        if (this.isDisposed()) {
            throw new IllegalStateException("Already released");
        } else {
            Object var1 = this.handlerLock;
            Object var2 = this.handlerLock;
            synchronized(this.handlerLock) {
                if (this.cameraThreadHandler != null) {
                    throw new IllegalStateException("dispose() called while camera is running");
                }
            }

            this.isDisposed = true;
        }
    }

    public boolean isDisposed() {
        return this.isDisposed;
    }

    public void startCapture(final int width, final int height, final int framerate, SurfaceTextureHelper surfaceTextureHelper, final Context applicationContext, final CapturerObserver frameObserver) {
        Logging.d("VideoCapturerAndroid", "startCapture requested: " + width + "x" + height + "@" + framerate);
        this.cameraThreadHandler = surfaceTextureHelper.getHandler();
        this.surfaceHelper = surfaceTextureHelper;
        VideoCapturerAndroid.this.openCameraAttempts = 0;
        this.applicationContext = applicationContext;
        this.frameObserver = frameObserver;
        this.firstFrameReported = false;
        this.info = new CameraInfo();
        Camera.getCameraInfo(this.id, this.info);
        this.requestedWidth = width;
        this.requestedHeight = height;
        this.requestedFramerate = framerate;
        CaptureFormat captureFormat = new CaptureFormat(width, height, 13000, 15000);
        this.captureFormat = captureFormat;
        if (!this.isCapturingToTexture) {
            this.queuedBuffers.clear();
            int frameSize = captureFormat.frameSize();

            for(int i = 0; i < 3; ++i) {
                ByteBuffer buffer = ByteBuffer.allocateDirect(frameSize);
                this.queuedBuffers.add(buffer.array());
            }
        }

        frameObserver.onCapturerStarted(true);
    }

    private void startCaptureOnCameraThread(final int width, final int height, final int framerate, final CapturerObserver frameObserver, final Context applicationContext) {

        this.applicationContext = applicationContext;
        this.frameObserver = frameObserver;
        this.firstFrameReported = false;
        this.info = new CameraInfo();
        Camera.getCameraInfo(this.id, this.info);
        this.startPreviewOnCameraThread(width, height, framerate);
        frameObserver.onCapturerStarted(true);
    }

    private void startPreviewOnCameraThread(int width, int height, int framerate) {

            this.requestedWidth = width;
            this.requestedHeight = height;
            this.requestedFramerate = framerate;
            Parameters parameters = CameraMock.getEmptyParameters();// this.camera.getParameters();
//            Size previewSize = CameraEnumerationAndroid.getClosestSupportedSize(parameters.getSupportedPreviewSizes(), width, height);
            CaptureFormat captureFormat = new CaptureFormat(width, height, 13000, 15000);
            this.captureFormat = captureFormat;
            if (!this.isCapturingToTexture) {
            this.queuedBuffers.clear();
            int frameSize = captureFormat.frameSize();

            for(int i = 0; i < 3; ++i) {
                ByteBuffer buffer = ByteBuffer.allocateDirect(frameSize);
                this.queuedBuffers.add(buffer.array());
                }
            }
    }

    public void stopCapture() throws InterruptedException {
        Logging.d("VideoCapturerAndroid", "stopCapture");
        final CountDownLatch barrier = new CountDownLatch(1);
        boolean didPost = this.maybePostOnCameraThread(new Runnable() {
            public void run() {
                VideoCapturerAndroid.this.stopCaptureOnCameraThread();
                synchronized(VideoCapturerAndroid.this.handlerLock) {
                    VideoCapturerAndroid.this.cameraThreadHandler.removeCallbacksAndMessages(this);
                    VideoCapturerAndroid.this.cameraThreadHandler = null;
                    VideoCapturerAndroid.this.surfaceHelper = null;
                }

                barrier.countDown();
            }
        });
        if (!didPost) {
            Logging.e("VideoCapturerAndroid", "Calling stopCapture() for already stopped camera.");
        } else {
            if (!barrier.await(7000L, TimeUnit.MILLISECONDS)) {
                Logging.e("VideoCapturerAndroid", "Camera stop timeout");
                this.printStackTrace();
                if (this.eventsHandler != null) {
                    this.eventsHandler.onCameraError("Camera stop timeout");
                }
            }

            Logging.d("VideoCapturerAndroid", "stopCapture done");
        }

    }

    private void stopCaptureOnCameraThread() {
        this.checkIsOnCameraThread();
        Logging.d("VideoCapturerAndroid", "stopCaptureOnCameraThread");
        if (this.surfaceHelper != null) {
            this.surfaceHelper.stopListening();
        }

        this.cameraThreadHandler.removeCallbacks(this.cameraObserver);
        this.cameraStatistics.getAndResetFrameCount();
        Logging.d("VideoCapturerAndroid", "Stop preview.");
//        if (this.camera != null) {
//            this.camera.stopPreview();
//            this.camera.setPreviewCallbackWithBuffer((PreviewCallback)null);
//        }

        this.queuedBuffers.clear();
        this.captureFormat = null;
        Logging.d("VideoCapturerAndroid", "Release camera.");
//        if (this.camera != null) {
//            this.camera.release();
//            this.camera = null;
//        }

        if (this.eventsHandler != null) {
            this.eventsHandler.onCameraClosed();
        }

        Logging.d("VideoCapturerAndroid", "stopCaptureOnCameraThread done");
    }

    private void switchCameraOnCameraThread() {
        this.checkIsOnCameraThread();
        Logging.d("VideoCapturerAndroid", "switchCameraOnCameraThread");
        this.stopCaptureOnCameraThread();
        Object var1 = this.cameraIdLock;
        Object var2 = this.cameraIdLock;
        synchronized(this.cameraIdLock) {
            if (this.id == 1) {
                this.id = 0;
            } else if (this.id == 0) {
                this.id = 1;
            }
        }

        this.dropNextFrame = true;
        this.startCaptureOnCameraThread(this.requestedWidth, this.requestedHeight, this.requestedFramerate, this.frameObserver, this.applicationContext);
        Logging.d("VideoCapturerAndroid", "switchCameraOnCameraThread done");
    }

    private void onOutputFormatRequestOnCameraThread(int width, int height, int framerate) {
        this.checkIsOnCameraThread();
//        if (this.camera == null)
//        {
//            Logging.e("VideoCapturerAndroid", "Calling onOutputFormatRequest() on stopped camera.");
//        }
//        else
        {
            Logging.d("VideoCapturerAndroid", "onOutputFormatRequestOnCameraThread: " + width + "x" + height + "@" + framerate);
            this.frameObserver.onOutputFormatRequest(width, height, framerate);
        }

    }

    Handler getCameraThreadHandler() {
        return this.cameraThreadHandler;
    }

    private int getDeviceOrientation() {
        int orientation = 0;
        WindowManager wm = (WindowManager)this.applicationContext.getSystemService(Context.WINDOW_SERVICE);
        switch(wm.getDefaultDisplay().getRotation())
        {
            case 0:
                orientation = 0;
                break;
            case 1:
                orientation = 90;
                break;
            case 2:
                orientation = 180;
                break;
            case 3:
                orientation = 270;
        }

        return orientation;
    }

    public int getFrameOrientation() {
        int rotation = this.getDeviceOrientation();
        if (this.info.facing == 0) {
            rotation = 360 - rotation;
        }

        return (this.info.orientation + rotation) % 360;
    }

    public void onPreviewFrame(byte[] data, Camera callbackCamera) {
        long captureTimeNs = TimeUnit.MILLISECONDS.toNanos(SystemClock.elapsedRealtime());
        if (this.eventsHandler != null && !this.firstFrameReported) {
            this.eventsHandler.onFirstFrameAvailable();
            this.firstFrameReported = true;
        }

        this.cameraStatistics.addFrame();
        this.frameObserver.onByteBufferFrameCaptured(data, this.captureFormat.width, this.captureFormat.height, this.getFrameOrientation(), captureTimeNs);
//        this.camera.addCallbackBuffer(data);
    }

    public void onTextureFrameAvailable(int oesTextureId, float[] transformMatrix, long timestampNs) {
        if (this.cameraThreadHandler == null) {
            throw new RuntimeException("onTextureFrameAvailable() called after stopCapture().");
        } else {
            this.checkIsOnCameraThread();
            if (this.dropNextFrame) {
                this.surfaceHelper.returnTextureFrame();
                this.dropNextFrame = false;
            } else {
                if (this.eventsHandler != null && !this.firstFrameReported) {
                    this.eventsHandler.onFirstFrameAvailable();
                    this.firstFrameReported = true;
                }

                int rotation = this.getFrameOrientation();
                if (this.info.facing == 1) {
                    transformMatrix = RendererCommon.multiplyMatrices(transformMatrix, RendererCommon.horizontalFlipMatrix());
                }

                this.cameraStatistics.addFrame();
                this.frameObserver.onTextureFrameCaptured(this.captureFormat.width, this.captureFormat.height, oesTextureId, transformMatrix, rotation, timestampNs);
            }

        }
    }

    public static class CameraStatistics {
        private int frameCount = 0;
        public ThreadChecker threadChecker = new ThreadChecker();

        CameraStatistics() {
            this.threadChecker.detachThread();
        }

        public void addFrame() {
            this.threadChecker.checkIsOnValidThread();
            ++this.frameCount;
        }

        public int getAndResetFrameCount() {
            this.threadChecker.checkIsOnValidThread();
            int count = this.frameCount;
            this.frameCount = 0;
            return count;
        }
    }

    public interface CameraEventsHandler {
        void onCameraError(String var1);

        void onCameraFreezed(String var1);

        void onCameraOpening(int var1);

        void onFirstFrameAvailable();

        void onCameraClosed();
    }

    public interface CameraSwitchHandler {
        void onCameraSwitchDone(boolean var1);

        void onCameraSwitchError(String var1);
    }
}
