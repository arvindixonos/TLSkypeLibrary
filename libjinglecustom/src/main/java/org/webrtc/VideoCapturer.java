package org.webrtc;

/**
 * Created by TakeLeap05 on 12-07-2018.
 */

import android.content.Context;
import java.util.List;
import org.webrtc.CameraEnumerationAndroid.CaptureFormat;

public interface VideoCapturer {
    List<CaptureFormat> getSupportedFormats();

    void startCapture(int var1, int var2, int var3, SurfaceTextureHelper var4, Context var5, CapturerObserver var6);

    void stopCapture() throws InterruptedException;

    void dispose();

    public static class NativeObserver implements CapturerObserver {
        private final long nativeCapturer;

        public NativeObserver(long nativeCapturer) {
            this.nativeCapturer = nativeCapturer;
        }

        public void onCapturerStarted(boolean success) {
            this.nativeCapturerStarted(this.nativeCapturer, success);
        }

        public void onByteBufferFrameCaptured(byte[] data, int width, int height, int rotation, long timeStamp) {
            this.nativeOnByteBufferFrameCaptured(this.nativeCapturer, data, data.length, width, height, rotation, timeStamp);
        }

        public void onTextureFrameCaptured(int width, int height, int oesTextureId, float[] transformMatrix, int rotation, long timestamp) {
            this.nativeOnTextureFrameCaptured(this.nativeCapturer, width, height, oesTextureId, transformMatrix, rotation, timestamp);
        }

        public void onOutputFormatRequest(int width, int height, int framerate) {
            this.nativeOnOutputFormatRequest(this.nativeCapturer, width, height, framerate);
        }

        private native void nativeCapturerStarted(long var1, boolean var3);

        private native void nativeOnByteBufferFrameCaptured(long var1, byte[] var3, int var4, int var5, int var6, int var7, long var8);

        private native void nativeOnTextureFrameCaptured(long var1, int var3, int var4, int var5, float[] var6, int var7, long var8);

        private native void nativeOnOutputFormatRequest(long var1, int var3, int var4, int var5);
    }

    public interface CapturerObserver {
        void onCapturerStarted(boolean var1);

        void onByteBufferFrameCaptured(byte[] var1, int var2, int var3, int var4, long var5);

        void onTextureFrameCaptured(int var1, int var2, int var3, float[] var4, int var5, long var6);

        void onOutputFormatRequest(int var1, int var2, int var3);
    }
}