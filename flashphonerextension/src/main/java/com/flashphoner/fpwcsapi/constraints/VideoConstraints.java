//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.flashphoner.fpwcsapi.constraints;

import org.webrtc.MediaConstraints;
import org.webrtc.MediaConstraints.KeyValuePair;

public class VideoConstraints {
    private static final String MAX_VIDEO_WIDTH_CONSTRAINT = "maxWidth";
    private static final String MIN_VIDEO_WIDTH_CONSTRAINT = "minWidth";
    private static final String MAX_VIDEO_HEIGHT_CONSTRAINT = "maxHeight";
    private static final String MIN_VIDEO_HEIGHT_CONSTRAINT = "minHeight";
    private static final int MAX_VIDEO_WIDTH = 3000;
    private static final int MAX_VIDEO_HEIGHT = 3000;
    private static final int MIN_VIDEO_WIDTH = 128;
    private static final int MIN_VIDEO_HEIGHT = 96;
    private static final String MAX_VIDEO_FPS_CONSTRAINT = "maxFrameRate";
    private static final String MIN_VIDEO_FPS_CONSTRAINT = "minFrameRate";
    private static final int MAX_VIDEO_FPS = 30;
    private Integer cameraId;
    private MediaConstraints mediaConstraints = new MediaConstraints();

    public VideoConstraints() {
    }

    public Integer getCameraId() {
        return this.cameraId;
    }

    public void setCameraId(Integer cameraId) {
        this.cameraId = cameraId;
    }

    public void setResolution(int videoWidth, int videoHeight) {
        if (videoWidth > 0 && videoHeight > 0)
        {
//            videoWidth = Math.min(videoWidth, MAX_VIDEO_WIDTH);
//            videoHeight = Math.min(videoHeight, MAX_VIDEO_HEIGHT);
//            videoWidth = Math.max(videoWidth, 128);
//            videoHeight = Math.max(videoHeight, 96);
            this.mediaConstraints.mandatory.add(new KeyValuePair("maxWidth", Integer.toString(videoWidth)));
            this.mediaConstraints.mandatory.add(new KeyValuePair("maxHeight", Integer.toString(videoHeight)));
        }

    }

    public void setVideoFps(int videoFps) {
        if (videoFps > 0) {
            videoFps = Math.min(videoFps, 30);
            this.mediaConstraints.mandatory.add(new KeyValuePair("minFrameRate", Integer.toString(videoFps)));
            this.mediaConstraints.mandatory.add(new KeyValuePair("maxFrameRate", Integer.toString(videoFps)));
        }

    }

    public MediaConstraints getMediaConstraints() {
        return this.mediaConstraints;
    }
}
