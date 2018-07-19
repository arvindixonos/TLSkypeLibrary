package org.webrtc;


public class VideoSource extends MediaSource {
    public VideoSource(long nativeSource) {
        super(nativeSource);
    }

    public void stop() {
        stop(this.nativeSource);
    }

    public void restart() {
        restart(this.nativeSource);
    }

    public void dispose() {
        super.dispose();
    }

    private static native void stop(long var0);

    private static native void restart(long var0);
}
