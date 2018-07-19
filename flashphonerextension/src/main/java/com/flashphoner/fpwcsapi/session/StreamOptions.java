package com.flashphoner.fpwcsapi.session;

/**
 * Created by TakeLeap05 on 12-07-2018.
 */

import com.flashphoner.fpwcsapi.constraints.Constraints;
import com.flashphoner.fpwcsapi.session.AbstractStreamOptions;

import org.webrtc.SurfaceViewRenderer;

public class StreamOptions extends AbstractStreamOptions {
    private SurfaceViewRenderer renderer;
    private Constraints constraints = new Constraints(true, true);
    private String videoCodec;

    public StreamOptions(String name) {
        super(name);
    }

    public SurfaceViewRenderer getRenderer() {
        return this.renderer;
    }

    public void setRenderer(SurfaceViewRenderer renderer) {
        this.renderer = renderer;
    }

    public Constraints getConstraints() {
        return this.constraints;
    }

    public void setConstraints(Constraints constraints) {
        this.constraints = constraints;
    }

    String getVideoCodec() {
        return this.videoCodec;
    }

    void setVideoCodec(String videoCodec) {
        this.videoCodec = videoCodec;
    }
}