package com.flashphoner.fpwcsapi.session;

/**
 * Created by TakeLeap05 on 12-07-2018.
 */

import com.flashphoner.fpwcsapi.constraints.Constraints;
import com.flashphoner.fpwcsapi.webrtc.MediaConnection;
import com.flashphoner.fpwcsapi.webrtc.MediaConnectionListener;
import com.flashphoner.fpwcsapi.webrtc.MediaConnectionOptions;
import com.flashphoner.fpwcsapi.webrtc.WebRTCMediaProvider;

import org.webrtc.IceCandidate;
import org.webrtc.MediaStream;
import org.webrtc.SessionDescription;
import org.webrtc.StatsReport;

import java.util.List;
import java.util.UUID;

public class Call {
    private String id;
    private Session session;
    private CallOptions callOptions;
    private CallObject callObject;
    private CallStatusEvent callStatusEvent;

    public Call(CallObject callObject, Session session) {
        this.id = callObject.getCallId();
        this.callObject = callObject;
        this.callObject.setCallId(this.id);
        this.callOptions = new CallOptions(callObject);
        this.session = session;
    }

    public Call(CallOptions callOptions, Session session) {
        this.id = UUID.randomUUID().toString();
        this.callOptions = callOptions;
        this.callObject = new CallObject(callOptions);
        this.callObject.setCallId(this.id);
        this.session = session;
    }

    public void call() {
        final WebRTCMediaProvider instance = WebRTCMediaProvider.getInstance();
        Session.executor.execute(new Runnable() {
            public void run() {
                MediaConnection mediaConnection = instance.createMediaConnection(Call.this.id, Call.this.callOptions.getRemoteRenderer() != null? Call.this.callOptions.getRemoteRenderer(): Call.this.session.getSessionOptions().getRemoteRenderer());
                MediaConnectionListener mediaConnectionListener = new MediaConnectionListener() {
                    public void onLocalDescription(SessionDescription sdp) {
                        Call.this.callObject.setSdp(sdp.description);
                        Call.this.callObject.setHasAudio(Boolean.valueOf(Call.this.callOptions.getConstraints().getAudioConstraints() != null));
                        Call.this.callObject.setHasVideo(Boolean.valueOf(Call.this.callOptions.getConstraints().getVideoConstraints() != null));
                        Call.this.session.send("call", Call.this.callObject);
                    }

                    public void onIceCandidate(IceCandidate candidate) {
                    }

                    public void onIceConnected() {
                    }

                    public void onIceDisconnected() {
                    }

                    public void onPeerConnectionClosed() {
                    }

                    public void onPeerConnectionStatsReady(StatsReport[] reports) {
                    }

                    public void onPeerConnectionError(String description) {
                    }
                };
                mediaConnection.setMediaConnectionListener(mediaConnectionListener);
                MediaConnectionOptions mediaConnectionOptions = new MediaConnectionOptions();
                mediaConnectionOptions.setReceiveAudio(Call.this.callObject.getHasAudio().booleanValue());
                mediaConnectionOptions.setReceiveVideo(Call.this.callObject.getHasVideo().booleanValue());
                if(Call.this.callOptions.getConstraints() == null) {
                    Call.this.callOptions.setConstraints(new Constraints(true, false));
                }

                List<MediaStream> mediaStreams = instance.getLocalMediaStreams(Call.this.callOptions.getConstraints(),
                        Call.this.callOptions.getLocalRenderer() != null? Call.this.callOptions.getLocalRenderer(): Call.this.session.getSessionOptions().getLocalRenderer());
                mediaConnectionOptions.getLocalStreams().addAll(mediaStreams);
                mediaConnection.createOffer(mediaConnectionOptions);
            }
        });
    }

    public void answer() {
        final WebRTCMediaProvider instance = WebRTCMediaProvider.getInstance();
        Session.executor.execute(new Runnable() {
            public void run() {
                MediaConnection mediaConnection = instance.createMediaConnection(Call.this.id, Call.this.callOptions.getRemoteRenderer() != null? Call.this.callOptions.getRemoteRenderer(): Call.this.session.getSessionOptions().getRemoteRenderer());
                MediaConnectionListener mediaConnectionListener = new MediaConnectionListener() {
                    public void onLocalDescription(SessionDescription sdp) {
                        Call.this.callObject.setSdp(sdp.description);
                        Call.this.callObject.setHasAudio(Boolean.valueOf(Call.this.callOptions.getConstraints().getAudioConstraints() != null));
                        Call.this.callObject.setHasVideo(Boolean.valueOf(Call.this.callOptions.getConstraints().getVideoConstraints() != null));
                        Call.this.session.send("answer", Call.this.callObject);
                    }

                    public void onIceCandidate(IceCandidate candidate) {
                    }

                    public void onIceConnected() {
                    }

                    public void onIceDisconnected() {
                    }

                    public void onPeerConnectionClosed() {
                    }

                    public void onPeerConnectionStatsReady(StatsReport[] reports) {
                    }

                    public void onPeerConnectionError(String description) {
                    }
                };
                mediaConnection.setMediaConnectionListener(mediaConnectionListener);
                mediaConnection.setRemoteDescription((SessionDescription) Call.this.session.getSessionDescriptions().get(Call.this.id));
                MediaConnectionOptions mediaConnectionOptions = new MediaConnectionOptions();
                mediaConnectionOptions.setReceiveAudio(Call.this.callObject.getHasAudio().booleanValue());
                mediaConnectionOptions.setReceiveVideo(Call.this.callObject.getHasVideo().booleanValue());
                if(Call.this.callOptions.getConstraints() == null) {
                    Call.this.callOptions.setConstraints(new Constraints(true, false));
                }

                List<MediaStream> mediaStreams = instance.getLocalMediaStreams(Call.this.callOptions.getConstraints(), Call.this.callOptions.getLocalRenderer() != null? Call.this.callOptions.getLocalRenderer(): Call.this.session.getSessionOptions().getLocalRenderer());
                mediaConnectionOptions.getLocalStreams().addAll(mediaStreams);
                mediaConnection.createAnswer(mediaConnectionOptions);
            }
        });
    }

    public void hold() {
        this.session.send("hold", this.callObject);
    }

    public void unhold() {
        this.session.send("unhold", this.callObject);
    }

    public void hangup() {
        this.session.send("hangup", this.callObject);
        WebRTCMediaProvider.getInstance().removeMediaConnection(this.id);
    }

    public void muteAudio() {
        WebRTCMediaProvider instance = WebRTCMediaProvider.getInstance();
        Session.executor.execute(new Runnable() {
            public void run() {
                MediaConnection mediaConnection = WebRTCMediaProvider.getInstance().getMediaConnection(Call.this.id);
                if(mediaConnection != null) {
                    mediaConnection.muteAudio();
                }

            }
        });
    }

    public void unmuteAudio() {
        WebRTCMediaProvider instance = WebRTCMediaProvider.getInstance();
        Session.executor.execute(new Runnable() {
            public void run() {
                MediaConnection mediaConnection = WebRTCMediaProvider.getInstance().getMediaConnection(Call.this.id);
                if(mediaConnection != null) {
                    mediaConnection.unmuteAudio();
                }

            }
        });
    }

    public boolean isAudioMuted() {
        MediaConnection mediaConnection = WebRTCMediaProvider.getInstance().getMediaConnection(this.id);
        return mediaConnection == null || mediaConnection.isAudioMuted();
    }

    public void muteVideo() {
        WebRTCMediaProvider instance = WebRTCMediaProvider.getInstance();
        Session.executor.execute(new Runnable() {
            public void run() {
                MediaConnection mediaConnection = WebRTCMediaProvider.getInstance().getMediaConnection(Call.this.id);
                if(mediaConnection != null) {
                    mediaConnection.muteVideo();
                }

            }
        });
    }

    public void unmuteVideo() {
        WebRTCMediaProvider instance = WebRTCMediaProvider.getInstance();
        Session.executor.execute(new Runnable() {
            public void run() {
                MediaConnection mediaConnection = WebRTCMediaProvider.getInstance().getMediaConnection(Call.this.id);
                if(mediaConnection != null) {
                    mediaConnection.unmuteVideo();
                }

            }
        });
    }

    public boolean isVideoMuted() {
        MediaConnection mediaConnection = WebRTCMediaProvider.getInstance().getMediaConnection(this.id);
        return mediaConnection == null || mediaConnection.isVideoMuted();
    }

    public void on(CallStatusEvent callStatusEvent) {
        this.callStatusEvent = callStatusEvent;
    }

    public void onCallProperties(CallObject callObject) {
        this.callObject = callObject;
        if(this.callStatusEvent != null) {
            if("TRYING".equals(callObject.getStatus())) {
                this.callStatusEvent.onTrying(this);
            } else if("RING".equals(callObject.getStatus())) {
                this.callStatusEvent.onRing(this);
            } else if("BUSY".equals(callObject.getStatus())) {
                this.callStatusEvent.onBusy(this);
            } else if("HOLD".equals(callObject.getStatus())) {
                this.callStatusEvent.onHold(this);
            } else if("ESTABLISHED".equals(callObject.getStatus())) {
                this.callStatusEvent.onEstablished(this);
            } else if("FAILED".equals(callObject.getStatus())) {
                this.callStatusEvent.onFailed(this);
            } else if("FINISH".equals(callObject.getStatus())) {
                this.callStatusEvent.onFinished(this);
            }
        }

    }

    public String getId() {
        return this.id;
    }

    public Boolean getIncoming() {
        return this.callObject.getIncoming();
    }

    public String getStatus() {
        return this.callObject.getStatus();
    }

    public Integer getSipStatus() {
        return this.callObject.getSipStatus();
    }

    public String getCaller() {
        return this.callObject.getCaller();
    }

    public String getCallee() {
        return this.callObject.getCallee();
    }

    public Boolean getHasAudio() {
        return this.callObject.getHasAudio();
    }

    public Boolean getHasVideo() {
        return this.callObject.getHasVideo();
    }

    public String getVisibleName() {
        return this.callObject.getVisibleName();
    }

    public String getSipMessageRaw() {
        return this.callObject.getSipMessageRaw();
    }

    public Boolean getIsMsrp() {
        return this.callObject.getIsMsrp();
    }

    public CallOptions getCallOptions() {
        return this.callOptions;
    }
}
