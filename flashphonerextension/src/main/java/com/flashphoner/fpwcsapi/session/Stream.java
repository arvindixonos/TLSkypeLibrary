package com.flashphoner.fpwcsapi.session;

/**
 * Created by TakeLeap05 on 12-07-2018.
 */

import android.hardware.Camera;

import com.flashphoner.fpwcsapi.bean.StreamStatus;
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

public class Stream {
    private String id = UUID.randomUUID().toString();
    private Session session;
    private StreamOptions streamOptions;
    private StreamObject streamObject;
    private StreamStatusEvent streamStatusEvent;
    private boolean stopped = false;

    Stream(StreamOptions streamOptions, Session session) {
        this.streamOptions = streamOptions;
        this.streamObject = new StreamObject(streamOptions);
        this.streamObject.setMediaSessionId(this.getId());
        this.session = session;
    }

    public synchronized void publish() {
       publish(null);
    }

    public synchronized void publish(final Camera.PreviewCallback targetPreviewCallback) {
        if(!this.stopped) {
            final WebRTCMediaProvider instance = WebRTCMediaProvider.getInstance();
            Session.executor.execute(new Runnable() {
                public void run() {
                    MediaConnection mediaConnection = instance.createMediaConnection(Stream.this.id, Stream.this.streamOptions.getVideoCodec());
                    Stream.this.streamObject.setPublished(true);
                    Stream.this.streamObject.setHasVideo(true);
                    MediaConnectionListener mediaConnectionListener = new MediaConnectionListener() {
                        public void onLocalDescription(SessionDescription sdp) {
                            Stream.this.streamObject.setSdp(sdp.description);
                            Stream.this.session.send("publishStream", Stream.this.streamObject);
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
                    if(Stream.this.streamOptions.getConstraints() == null) {
                        Stream.this.streamOptions.setConstraints(new Constraints(true, false));
                    }

                    List<MediaStream> mediaStreams = instance.getLocalMediaStreams(Stream.this.streamOptions.getConstraints(), Stream.this.streamOptions.getRenderer() != null? Stream.this.streamOptions.getRenderer(): Stream.this.session.getSessionOptions().getLocalRenderer(), targetPreviewCallback);
                    mediaConnectionOptions.getLocalStreams().addAll(mediaStreams);
                    mediaConnection.createOffer(mediaConnectionOptions);
                }
            });
        }
    }

    public synchronized void play() {
        if(!this.stopped) {
            Session.executor.execute(new Runnable() {
                public void run() {
                    MediaConnection mediaConnection = WebRTCMediaProvider.getInstance().createMediaConnection(Stream.this.id, Stream.this.streamOptions.getVideoCodec(), Stream.this.streamOptions.getRenderer() != null? Stream.this.streamOptions.getRenderer(): Stream.this.session.getSessionOptions().getRemoteRenderer());
                    Stream.this.streamObject.setPublished(false);
                    MediaConnectionListener mediaConnectionListener = new MediaConnectionListener() {
                        public void onLocalDescription(SessionDescription sdp) {
                            Stream.this.streamObject.setSdp(sdp.description);
                            Stream.this.session.send("playStream", Stream.this.streamObject);
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
                    mediaConnection.createOffer(new MediaConnectionOptions());
                }
            });
        }
    }

    public void on(StreamStatusEvent streamStatusEvent) {
        this.streamStatusEvent = streamStatusEvent;
    }

    public synchronized void stop() {
        if(this.streamObject.isPublished()) {
            this.session.send("unPublishStream", this.streamObject);
        } else {
            this.session.send("stopStream", this.streamObject);
        }

        WebRTCMediaProvider.getInstance().removeMediaConnection(this.streamObject.getMediaSessionId());
        this.stopped = true;
    }

    public void muteAudio() {
        Session.executor.execute(new Runnable() {
            public void run() {
                MediaConnection mediaConnection = WebRTCMediaProvider.getInstance().getMediaConnection(Stream.this.streamObject.getMediaSessionId());
                if(mediaConnection != null) {
                    mediaConnection.muteAudio();
                }

            }
        });
    }

    public void unmuteAudio() {
        Session.executor.execute(new Runnable() {
            public void run() {
                MediaConnection mediaConnection = WebRTCMediaProvider.getInstance().getMediaConnection(Stream.this.streamObject.getMediaSessionId());
                if(mediaConnection != null) {
                    mediaConnection.unmuteAudio();
                }

            }
        });
    }

    public boolean isAudioMuted() {
        MediaConnection mediaConnection = WebRTCMediaProvider.getInstance().getMediaConnection(this.streamObject.getMediaSessionId());
        return mediaConnection == null || mediaConnection.isAudioMuted();
    }

    public void muteVideo() {
        Session.executor.execute(new Runnable() {
            public void run() {
                MediaConnection mediaConnection = WebRTCMediaProvider.getInstance().getMediaConnection(Stream.this.streamObject.getMediaSessionId());
                if(mediaConnection != null) {
                    mediaConnection.muteVideo();
                }

            }
        });
    }

    public void unmuteVideo() {
        Session.executor.execute(new Runnable() {
            public void run() {
                MediaConnection mediaConnection = WebRTCMediaProvider.getInstance().getMediaConnection(Stream.this.streamObject.getMediaSessionId());
                if(mediaConnection != null) {
                    mediaConnection.unmuteVideo();
                }

            }
        });
    }

    public boolean isVideoMuted() {
        MediaConnection mediaConnection = WebRTCMediaProvider.getInstance().getMediaConnection(this.streamObject.getMediaSessionId());
        return mediaConnection == null || mediaConnection.isVideoMuted();
    }

    void onStreamProperties(StreamObject streamObject) {
        this.streamObject = streamObject;
        if(StreamStatus.FAILED.equals(streamObject.getStatus()) || StreamStatus.STOPPED.equals(streamObject.getStatus()) || StreamStatus.UNPUBLISHED.equals(streamObject.getStatus())) {
            this.session.streams.remove(streamObject.getMediaSessionId());
            WebRTCMediaProvider.getInstance().removeMediaConnection(streamObject.getMediaSessionId());
        }

        if(this.streamStatusEvent != null) {
            this.streamStatusEvent.onStreamStatus(this, streamObject.getStatus());
        }

    }

    public String getId() {
        return this.id;
    }

    public String getName() {
        return this.streamObject.getName();
    }

    public boolean isPublished() {
        return this.streamObject.isPublished();
    }

    public Boolean isHasAudio() {
        return this.streamObject.isHasAudio();
    }

    public boolean isHasVideo() {
        return this.streamObject.isHasVideo();
    }

    public StreamStatus getStatus() {
        return this.streamObject.getStatus();
    }

    public String getInfo() {
        return this.streamObject.getInfo();
    }

    public Boolean isRecord() {
        return Boolean.valueOf(this.streamObject.isRecord());
    }

    public String getRecordName() {
        return this.streamObject.getRecordName();
    }

    public int getWidth() {
        return this.streamObject.getWidth();
    }

    public int getHeight() {
        return this.streamObject.getHeight();
    }

    public int getBitrate() {
        return this.streamObject.getBitrate();
    }

    public int getQuality() {
        return this.streamObject.getQuality();
    }
}

