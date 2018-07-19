package com.flashphoner.fpwcsapi.webrtc;

import org.webrtc.MediaStream;

import java.util.LinkedList;
import java.util.List;

public class MediaConnectionOptions {
    private List<MediaStream> localStreams = new LinkedList();
    private boolean receiveAudio = true;
    private boolean receiveVideo = true;

    public MediaConnectionOptions() {
    }

    public List<MediaStream> getLocalStreams() {
        return this.localStreams;
    }

    public boolean isReceiveAudio() {
        return this.receiveAudio;
    }

    public void setReceiveAudio(boolean receiveAudio) {
        this.receiveAudio = receiveAudio;
    }

    public boolean isReceiveVideo() {
        return this.receiveVideo;
    }

    public void setReceiveVideo(boolean receiveVideo) {
        this.receiveVideo = receiveVideo;
    }
}
