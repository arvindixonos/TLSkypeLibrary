package org.webrtc;

/**
 * Created by TakeLeap05 on 12-07-2018.
 */

import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

class SurfaceTextureHelper {
    private static final String TAG = "SurfaceTextureHelper";
    private final Handler handler;
    private final EglBase eglBase;
    private final SurfaceTexture surfaceTexture;
    private final int oesTextureId;
    private YuvConverter yuvConverter;
    private OnTextureFrameAvailableListener listener;
    private boolean hasPendingTexture;
    private volatile boolean isTextureInUse;
    private boolean isQuitting;
    private OnTextureFrameAvailableListener pendingListener;
    final Runnable setListenerRunnable;

    public static SurfaceTextureHelper create(final String threadName, final EglBase.Context sharedContext) {
        HandlerThread thread = new HandlerThread(threadName);
        thread.start();
        final  Handler handler = new Handler(thread.getLooper());
        return (SurfaceTextureHelper) ThreadUtils.invokeUninterruptibly(handler, new Callable<SurfaceTextureHelper>() {
            public SurfaceTextureHelper call() {
                try {
                    return new SurfaceTextureHelper(sharedContext, handler);
                } catch (RuntimeException var2) {
                    Logging.e("SurfaceTextureHelper", threadName + " create failure", var2);
                    return null;
                }
            }
        });
    }

    private SurfaceTextureHelper(EglBase.Context sharedContext, Handler handler) {
        this.hasPendingTexture = false;
        this.isTextureInUse = false;
        this.isQuitting = false;
        this.setListenerRunnable = new Runnable() {
            public void run() {
                Logging.d("SurfaceTextureHelper", "Setting listener to " + SurfaceTextureHelper.this.pendingListener);
                SurfaceTextureHelper.this.listener = SurfaceTextureHelper.this.pendingListener;
                SurfaceTextureHelper.this.pendingListener = null;
                SurfaceTextureHelper.this.tryDeliverTextureFrame();
            }
        };
        if(handler.getLooper().getThread() != Thread.currentThread()) {
            throw new IllegalStateException("SurfaceTextureHelper must be created on the handler thread");
        } else {
            this.handler = handler;
            this.eglBase = EglBase.create(sharedContext, EglBase.CONFIG_PIXEL_BUFFER);

            try {
                this.eglBase.createDummyPbufferSurface();
                this.eglBase.makeCurrent();
            } catch (RuntimeException var4) {
                this.eglBase.release();
                handler.getLooper().quit();
                throw var4;
            }

            this.oesTextureId = GlUtil.generateTexture('赥');
            this.surfaceTexture = new SurfaceTexture(this.oesTextureId);
            this.surfaceTexture.setOnFrameAvailableListener(new SurfaceTexture.OnFrameAvailableListener() {
                public void onFrameAvailable(SurfaceTexture surfaceTexture) {
                    SurfaceTextureHelper.this.hasPendingTexture = true;
                    SurfaceTextureHelper.this.tryDeliverTextureFrame();
                }
            });
        }
    }

    private YuvConverter getYuvConverter() {
        if(this.yuvConverter != null) {
            return this.yuvConverter;
        } else {
            synchronized(this) {
                if(this.yuvConverter == null) {
                    this.yuvConverter = new YuvConverter(this.eglBase.getEglBaseContext());
                }

                return this.yuvConverter;
            }
        }
    }

    public void startListening(OnTextureFrameAvailableListener listener) {
        if(this.listener == null && this.pendingListener == null) {
            this.pendingListener = listener;
            this.handler.post(this.setListenerRunnable);
        } else {
            throw new IllegalStateException("SurfaceTextureHelper listener has already been set.");
        }
    }

    public void stopListening() {
        if(this.handler.getLooper().getThread() != Thread.currentThread()) {
            throw new IllegalStateException("Wrong thread.");
        } else {
            Logging.d("SurfaceTextureHelper", "stopListening()");
            this.handler.removeCallbacks(this.setListenerRunnable);
            this.listener = null;
            this.pendingListener = null;
        }
    }

    public SurfaceTexture getSurfaceTexture() {
        return this.surfaceTexture;
    }

    public Handler getHandler() {
        return this.handler;
    }

    public void returnTextureFrame() {
        this.handler.post(new Runnable() {
            public void run() {
                SurfaceTextureHelper.this.isTextureInUse = false;
                if(SurfaceTextureHelper.this.isQuitting) {
                    SurfaceTextureHelper.this.release();
                } else {
                    SurfaceTextureHelper.this.tryDeliverTextureFrame();
                }

            }
        });
    }

    public boolean isTextureInUse() {
        return this.isTextureInUse;
    }

    public void dispose() {
        Logging.d("SurfaceTextureHelper", "dispose()");
        if(this.handler.getLooper().getThread() == Thread.currentThread()) {
            this.isQuitting = true;
            if(!this.isTextureInUse) {
                this.release();
            }

        } else {
            final CountDownLatch barrier = new CountDownLatch(1);
            this.handler.postAtFrontOfQueue(new Runnable() {
                public void run() {
                    SurfaceTextureHelper.this.isQuitting = true;
                    barrier.countDown();
                    if(!SurfaceTextureHelper.this.isTextureInUse) {
                        SurfaceTextureHelper.this.release();
                    }

                }
            });
            ThreadUtils.awaitUninterruptibly(barrier);
        }
    }

    public void textureToYUV(ByteBuffer buf, int width, int height, int stride, int textureId, float[] transformMatrix) {
        if(textureId != this.oesTextureId) {
            throw new IllegalStateException("textureToByteBuffer called with unexpected textureId");
        } else {
            this.getYuvConverter().convert(buf, width, height, stride, textureId, transformMatrix);
        }
    }

    private void tryDeliverTextureFrame() {
        if(this.handler.getLooper().getThread() != Thread.currentThread()) {
            throw new IllegalStateException("Wrong thread.");
        } else if(!this.isQuitting && this.hasPendingTexture && !this.isTextureInUse && this.listener != null) {
            this.isTextureInUse = true;
            this.hasPendingTexture = false;
            Object var1 = EglBase.lock;
            synchronized(EglBase.lock) {
                this.surfaceTexture.updateTexImage();
            }

            float[] transformMatrix = new float[16];
            this.surfaceTexture.getTransformMatrix(transformMatrix);
            long timestampNs = Build.VERSION.SDK_INT >= 14?this.surfaceTexture.getTimestamp(): TimeUnit.MILLISECONDS.toNanos(SystemClock.elapsedRealtime());
            this.listener.onTextureFrameAvailable(this.oesTextureId, transformMatrix, timestampNs);
        }
    }

    private void release() {
        if(this.handler.getLooper().getThread() != Thread.currentThread()) {
            throw new IllegalStateException("Wrong thread.");
        } else if(!this.isTextureInUse && this.isQuitting) {
            synchronized(this) {
                if(this.yuvConverter != null) {
                    this.yuvConverter.release();
                }
            }

            GLES20.glDeleteTextures(1, new int[]{this.oesTextureId}, 0);
            this.surfaceTexture.release();
            this.eglBase.release();
            this.handler.getLooper().quit();
        } else {
            throw new IllegalStateException("Unexpected release.");
        }
    }

    private static class YuvConverter {
        private final EglBase eglBase;
        private final GlShader shader;
        private boolean released = false;
        private static final FloatBuffer DEVICE_RECTANGLE = GlUtil.createFloatBuffer(new float[]{-1.0F, -1.0F, 1.0F, -1.0F, -1.0F, 1.0F, 1.0F, 1.0F});
        private static final FloatBuffer TEXTURE_RECTANGLE = GlUtil.createFloatBuffer(new float[]{0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F});
        private static final String VERTEX_SHADER = "varying vec2 interp_tc;\nattribute vec4 in_pos;\nattribute vec4 in_tc;\n\nuniform mat4 texMatrix;\n\nvoid main() {\n    gl_Position = in_pos;\n    interp_tc = (texMatrix * in_tc).xy;\n}\n";
        private static final String FRAGMENT_SHADER = "#extension GL_OES_EGL_image_external : require\nprecision mediump float;\nvarying vec2 interp_tc;\n\nuniform samplerExternalOES oesTex;\nuniform vec2 xUnit;\nuniform vec4 coeffs;\n\nvoid main() {\n  gl_FragColor.r = coeffs.a + dot(coeffs.rgb,\n      texture2D(oesTex, interp_tc - 1.5 * xUnit).rgb);\n  gl_FragColor.g = coeffs.a + dot(coeffs.rgb,\n      texture2D(oesTex, interp_tc - 0.5 * xUnit).rgb);\n  gl_FragColor.b = coeffs.a + dot(coeffs.rgb,\n      texture2D(oesTex, interp_tc + 0.5 * xUnit).rgb);\n  gl_FragColor.a = coeffs.a + dot(coeffs.rgb,\n      texture2D(oesTex, interp_tc + 1.5 * xUnit).rgb);\n}\n";
        private int texMatrixLoc;
        private int xUnitLoc;
        private int coeffsLoc;

        YuvConverter(EglBase.Context sharedContext) {
            this.eglBase = EglBase.create(sharedContext, EglBase.CONFIG_PIXEL_RGBA_BUFFER);
            this.eglBase.createDummyPbufferSurface();
            this.eglBase.makeCurrent();
            this.shader = new GlShader("varying vec2 interp_tc;\nattribute vec4 in_pos;\nattribute vec4 in_tc;\n\nuniform mat4 texMatrix;\n\nvoid main() {\n    gl_Position = in_pos;\n    interp_tc = (texMatrix * in_tc).xy;\n}\n", "#extension GL_OES_EGL_image_external : require\nprecision mediump float;\nvarying vec2 interp_tc;\n\nuniform samplerExternalOES oesTex;\nuniform vec2 xUnit;\nuniform vec4 coeffs;\n\nvoid main() {\n  gl_FragColor.r = coeffs.a + dot(coeffs.rgb,\n      texture2D(oesTex, interp_tc - 1.5 * xUnit).rgb);\n  gl_FragColor.g = coeffs.a + dot(coeffs.rgb,\n      texture2D(oesTex, interp_tc - 0.5 * xUnit).rgb);\n  gl_FragColor.b = coeffs.a + dot(coeffs.rgb,\n      texture2D(oesTex, interp_tc + 0.5 * xUnit).rgb);\n  gl_FragColor.a = coeffs.a + dot(coeffs.rgb,\n      texture2D(oesTex, interp_tc + 1.5 * xUnit).rgb);\n}\n");
            this.shader.useProgram();
            this.texMatrixLoc = this.shader.getUniformLocation("texMatrix");
            this.xUnitLoc = this.shader.getUniformLocation("xUnit");
            this.coeffsLoc = this.shader.getUniformLocation("coeffs");
            GLES20.glUniform1i(this.shader.getUniformLocation("oesTex"), 0);
            GlUtil.checkNoGLES2Error("Initialize fragment shader uniform values.");
            this.shader.setVertexAttribArray("in_pos", 2, DEVICE_RECTANGLE);
            this.shader.setVertexAttribArray("in_tc", 2, TEXTURE_RECTANGLE);
            this.eglBase.detachCurrent();
        }

        synchronized void convert(ByteBuffer buf, int width, int height, int stride, int textureId, float[] transformMatrix) {
            if(this.released) {
                throw new IllegalStateException("YuvConverter.convert called on released object");
            } else if(stride % 8 != 0) {
                throw new IllegalArgumentException("Invalid stride, must be a multiple of 8");
            } else if(stride < width) {
                throw new IllegalArgumentException("Invalid stride, must >= width");
            } else {
                int y_width = (width + 3) / 4;
                int uv_width = (width + 7) / 8;
                int uv_height = (height + 1) / 2;
                int total_height = height + uv_height;
                int size = stride * total_height;
                if(buf.capacity() < size) {
                    throw new IllegalArgumentException("YuvConverter.convert called with too small buffer");
                } else {
                    transformMatrix = RendererCommon.multiplyMatrices(transformMatrix, RendererCommon.verticalFlipMatrix());
                    if(this.eglBase.hasSurface()) {
                        if(this.eglBase.surfaceWidth() != stride / 4 || this.eglBase.surfaceHeight() != total_height) {
                            this.eglBase.releaseSurface();
                            this.eglBase.createPbufferSurface(stride / 4, total_height);
                        }
                    } else {
                        this.eglBase.createPbufferSurface(stride / 4, total_height);
                    }

                    this.eglBase.makeCurrent();
                    GLES20.glActiveTexture('蓀');
                    GLES20.glBindTexture('赥', textureId);
                    GLES20.glUniformMatrix4fv(this.texMatrixLoc, 1, false, transformMatrix, 0);
                    GLES20.glViewport(0, 0, y_width, height);
                    GLES20.glUniform2f(this.xUnitLoc, transformMatrix[0] / (float)width, transformMatrix[1] / (float)width);
                    GLES20.glUniform4f(this.coeffsLoc, 0.299F, 0.587F, 0.114F, 0.0F);
                    GLES20.glDrawArrays(5, 0, 4);
                    GLES20.glViewport(0, height, uv_width, uv_height);
                    GLES20.glUniform2f(this.xUnitLoc, 2.0F * transformMatrix[0] / (float)width, 2.0F * transformMatrix[1] / (float)width);
                    GLES20.glUniform4f(this.coeffsLoc, -0.169F, -0.331F, 0.499F, 0.5F);
                    GLES20.glDrawArrays(5, 0, 4);
                    GLES20.glViewport(stride / 8, height, uv_width, uv_height);
                    GLES20.glUniform4f(this.coeffsLoc, 0.499F, -0.418F, -0.0813F, 0.5F);
                    GLES20.glDrawArrays(5, 0, 4);
                    GLES20.glReadPixels(0, 0, stride / 4, total_height, 6408, 5121, buf);
                    GlUtil.checkNoGLES2Error("YuvConverter.convert");
                    GLES20.glBindTexture('赥', 0);
                    this.eglBase.detachCurrent();
                }
            }
        }

        synchronized void release() {
            this.released = true;
            this.eglBase.makeCurrent();
            this.shader.release();
            this.eglBase.release();
        }
    }

    public interface OnTextureFrameAvailableListener {
        void onTextureFrameAvailable(int var1, float[] var2, long var3);
    }
}