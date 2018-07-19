package org.webrtc;


public class AudioSource extends MediaSource {
    public AudioSource(long nativeSource) {
        super(nativeSource);
    }

    public String TestThisFunction()
    {
        return  "TestResultOK";
    }
}
