package com.flashphoner.fpwcsapi.webrtc;

/**
 * Created by TakeLeap05 on 12-07-2018.
 */

import android.util.Log;
import com.flashphoner.fpwcsapi.util.LooperExecutor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.webrtc.AudioTrack;
import org.webrtc.DataChannel;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaConstraints.KeyValuePair;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnection.IceConnectionState;
import org.webrtc.PeerConnection.IceGatheringState;
import org.webrtc.PeerConnection.Observer;
import org.webrtc.PeerConnection.RTCConfiguration;
import org.webrtc.PeerConnection.SignalingState;
import org.webrtc.PeerConnection.TcpCandidatePolicy;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;
import org.webrtc.VideoRenderer;
import org.webrtc.VideoRenderer.Callbacks;
import org.webrtc.VideoTrack;

public class MediaConnection {
    private static final String TAG = "MediaConnection";
    private static final String DTLS_SRTP_KEY_AGREEMENT_CONSTRAINT = "DtlsSrtpKeyAgreement";
    private WebRTCMediaProvider webRTCMediaProvider;
    private String id;
    private PeerConnection peerConnection;
    private final LooperExecutor executor;
    private MediaConnectionListener mediaConnectionListener;
    private LinkedList<IceCandidate> queuedRemoteCandidates;
    private Callbacks remoteRenderer;
    private final PCObserver pcObserver = new PCObserver();
    private final SDPObserver sdpObserver = new SDPObserver();
    private SessionDescription localSdp;
    private boolean isInitiator;
    private boolean isError;
    private boolean renderVideo = true;
    private MediaConnectionOptions options;
    private String videoCodec;
    private AudioTrack audioTrack;
    private VideoTrack videoTrack;

    public MediaConnection(String id, Callbacks remoteRenderer, WebRTCMediaProvider webRTCMediaProvider, LooperExecutor executor, String videoCodec) {
        this.id = id;
        this.remoteRenderer = remoteRenderer;
        this.webRTCMediaProvider = webRTCMediaProvider;
        this.executor = executor;
        this.queuedRemoteCandidates = new LinkedList();
        this.videoCodec = videoCodec;
        MediaConstraints pcConstraints = new MediaConstraints();
        pcConstraints.optional.add(new KeyValuePair("DtlsSrtpKeyAgreement", "true"));
        Log.d("MediaConnection", "Create peer connection");
        RTCConfiguration rtcConfig = new RTCConfiguration(new ArrayList());
        rtcConfig.tcpCandidatePolicy = TcpCandidatePolicy.DISABLED;
        this.peerConnection = webRTCMediaProvider.getFactory().createPeerConnection(rtcConfig, pcConstraints, this.pcObserver);
    }

    public SessionDescription getRemoteDescription() {
        return this.peerConnection.getRemoteDescription();
    }

    public void createOffer(final MediaConnectionOptions options) {
        this.options = options;
        Log.i("MediaConnection", "K1 - " + Thread.currentThread().getName());
        this.executor.execute(new Runnable() {
            public void run() {
                Log.i("MediaConnection", "K2 - " + Thread.currentThread().getName());
                if(MediaConnection.this.peerConnection != null && !MediaConnection.this.isError) {
                    Log.d("MediaConnection", "PC Create OFFER");
                    MediaConnection.this.isInitiator = true;
                    Iterator var1 = options.getLocalStreams().iterator();

                    while(var1.hasNext()) {
                        MediaStream mediaStream = (MediaStream)var1.next();
                        MediaConnection.this.peerConnection.addStream(mediaStream);
                        if(mediaStream.audioTracks.size() > 0) {
                            MediaConnection.this.audioTrack = (AudioTrack)mediaStream.audioTracks.get(0);
                        }

                        if(mediaStream.videoTracks.size() > 0) {
                            MediaConnection.this.videoTrack = (VideoTrack) mediaStream.videoTracks.get(0);
                        }
                    }

                    MediaConstraints sdpMediaConstraints = new MediaConstraints();
                    sdpMediaConstraints.mandatory.add(new KeyValuePair("OfferToReceiveAudio", Boolean.toString(options.isReceiveAudio())));
                    sdpMediaConstraints.mandatory.add(new KeyValuePair("OfferToReceiveVideo", Boolean.toString(options.isReceiveVideo())));
                    MediaConnection.this.peerConnection.createOffer(MediaConnection.this.sdpObserver, sdpMediaConstraints);
                }

            }
        });
        Log.i("MediaConnection", "K3 - " + Thread.currentThread().getName());
    }

    public void createAnswer(final MediaConnectionOptions options) {
        this.options = options;
        Log.i("MediaConnection", "F1 - " + Thread.currentThread().getName());
        this.executor.execute(new Runnable() {
            public void run() {
                Log.i("MediaConnection", "F2 - " + Thread.currentThread().getName());
                if(MediaConnection.this.peerConnection != null && !MediaConnection.this.isError) {
                    Log.d("MediaConnection", "PC create ANSWER");
                    MediaConnection.this.isInitiator = false;
                    Iterator var1 = options.getLocalStreams().iterator();

                    while(var1.hasNext()) {
                        MediaStream mediaStream = (MediaStream)var1.next();
                        MediaConnection.this.peerConnection.addStream(mediaStream);
                        if(mediaStream.audioTracks.size() > 0) {
                            MediaConnection.this.audioTrack = (AudioTrack) mediaStream.audioTracks.get(0);
                        }

                        if(mediaStream.videoTracks.size() > 0) {
                            MediaConnection.this.videoTrack = (VideoTrack) mediaStream.videoTracks.get(0);
                        }
                    }

                    MediaConstraints sdpMediaConstraints = new MediaConstraints();
                    sdpMediaConstraints.mandatory.add(new KeyValuePair("OfferToReceiveAudio", Boolean.toString(options.isReceiveAudio())));
                    sdpMediaConstraints.mandatory.add(new KeyValuePair("OfferToReceiveVideo", Boolean.toString(options.isReceiveVideo())));
                    MediaConnection.this.peerConnection.createAnswer(MediaConnection.this.sdpObserver, sdpMediaConstraints);
                }

            }
        });
        Log.i("MediaConnection", "F3 - " + Thread.currentThread().getName());
    }

    public void setRemoteDescription(final SessionDescription sdp) {
        Log.i("MediaConnection", "E1 - " + Thread.currentThread().getName());
        this.executor.execute(new Runnable() {
            public void run() {
                Log.i("MediaConnection", "E2 - " + Thread.currentThread().getName());
                if(MediaConnection.this.peerConnection != null && !MediaConnection.this.isError) {
                    String sdpDescription = sdp.description;
                    SessionDescription sdpRemote = new SessionDescription(sdp.type, sdpDescription);
                    Log.d("MediaConnection", "Set remote SDP:" + sdpRemote.description);
                    MediaConnection.this.peerConnection.setRemoteDescription(MediaConnection.this.sdpObserver, sdpRemote);
                }
            }
        });
        Log.i("MediaConnection", "E3 - " + Thread.currentThread().getName());
    }

    void close() {
        Log.i("MediaConnection", "D1 - " + Thread.currentThread().getName());
        this.executor.execute(new Runnable() {
            public void run() {
                Log.i("MediaConnection", "D2 - " + Thread.currentThread().getName());
                if(MediaConnection.this.peerConnection != null) {
                    MediaConnection.this.peerConnection.dispose();
                    MediaConnection.this.peerConnection = null;
                }

            }
        });
        Log.i("MediaConnection", "D3 - " + Thread.currentThread().getName());
    }

    public void muteAudio() {
        if(this.audioTrack != null) {
            this.audioTrack.setEnabled(false);
        }

    }

    public void unmuteAudio() {
        if(this.audioTrack != null) {
            this.audioTrack.setEnabled(true);
        }

    }

    public boolean isAudioMuted() {
        return this.audioTrack == null || !this.audioTrack.enabled();
    }

    public void muteVideo() {
        if(this.videoTrack != null) {
            this.videoTrack.setEnabled(false);
        }

    }

    public void unmuteVideo() {
        if(this.videoTrack != null) {
            this.videoTrack.setEnabled(true);
        }

    }

    public boolean isVideoMuted() {
        return this.videoTrack == null || !this.videoTrack.enabled();
    }

    public String getId() {
        return this.id;
    }

    public void setMediaConnectionListener(MediaConnectionListener mediaConnectionListener) {
        this.mediaConnectionListener = mediaConnectionListener;
    }

    private void reportError(String errorMessage) {
        Log.e("MediaConnection", "Peerconnection error: " + errorMessage);
        if(!this.isError) {
            this.mediaConnectionListener.onPeerConnectionError(errorMessage);
            this.isError = true;
        }

    }

    private static String preferCodec(String sdpDescription, String codec, boolean isAudio) {
        String[] lines = sdpDescription.split("\r\n");
        int mLineIndex = -1;
        String codecRtpMap = null;
        String regex = "^a=rtpmap:(\\d+) " + codec + "(/\\d+)+[\r]?$";
        Pattern codecPattern = Pattern.compile(regex);
        String mediaDescription = "m=video ";
        if(isAudio) {
            mediaDescription = "m=audio ";
        }

        for(int i = 0; i < lines.length && (mLineIndex == -1 || codecRtpMap == null); ++i) {
            if(lines[i].startsWith(mediaDescription)) {
                mLineIndex = i;
            } else {
                Matcher codecMatcher = codecPattern.matcher(lines[i]);
                if(codecMatcher.matches()) {
                    codecRtpMap = codecMatcher.group(1);
                }
            }
        }

        if(mLineIndex == -1) {
            Log.w("MediaConnection", "No " + mediaDescription + " line, so can't prefer " + codec);
            return sdpDescription;
        } else if(codecRtpMap == null) {
            Log.w("MediaConnection", "No rtpmap for " + codec);
            return sdpDescription;
        } else {
            Log.d("MediaConnection", "Found " + codec + " rtpmap " + codecRtpMap + ", prefer at " + lines[mLineIndex]);
            String[] origMLineParts = lines[mLineIndex].split(" ");
            StringBuilder newSdpDescription;
            if(origMLineParts.length > 3) {
                newSdpDescription = new StringBuilder();
                int origPartIndex = 0;
                int var17 = origPartIndex + 1;
                newSdpDescription.append(origMLineParts[origPartIndex]).append(" ");
                newSdpDescription.append(origMLineParts[var17++]).append(" ");
                newSdpDescription.append(origMLineParts[var17++]).append(" ");
                newSdpDescription.append(codecRtpMap);
                lines[mLineIndex] = newSdpDescription.toString();
                Log.d("MediaConnection", "Change media description: " + lines[mLineIndex]);
            } else {
                Log.e("MediaConnection", "Wrong SDP media description format: " + lines[mLineIndex]);
            }

            newSdpDescription = new StringBuilder();
            String[] var18 = lines;
            int var12 = lines.length;

            for(int var13 = 0; var13 < var12; ++var13) {
                String line = var18[var13];
                if((!line.contains("rtpmap:") || line.contains("rtpmap:" + codecRtpMap)) && (!line.contains("fmtp:") || line.contains("fmtp:" + codecRtpMap)) && (!line.contains("rtcp-fb:") || line.contains("rtcp-fb:" + codecRtpMap))) {
                    newSdpDescription.append(line).append("\r\n");
                }
            }

            return newSdpDescription.toString();
        }
    }

    private void drainCandidates() {
        if(this.queuedRemoteCandidates != null) {
            Log.d("MediaConnection", "Add " + this.queuedRemoteCandidates.size() + " remote candidates");
            Iterator var1 = this.queuedRemoteCandidates.iterator();

            while(var1.hasNext()) {
                IceCandidate candidate = (IceCandidate)var1.next();
                this.peerConnection.addIceCandidate(candidate);
            }

            this.queuedRemoteCandidates = null;
        }

    }

    private class SDPObserver implements SdpObserver {
        private SDPObserver() {
        }

        public void onCreateSuccess(SessionDescription origSdp) {
            if(MediaConnection.this.localSdp != null) {
                MediaConnection.this.reportError("Multiple SDP create.");
            } else {
                String sdpDescription = origSdp.description;
                if(MediaConnection.this.videoCodec != null) {
                    sdpDescription = MediaConnection.preferCodec(sdpDescription, MediaConnection.this.videoCodec, false);
                }

                MediaConnection.this.localSdp = new SessionDescription(origSdp.type, sdpDescription);
                Log.i("MediaConnection", "A1 - " + Thread.currentThread().getName());
                MediaConnection.this.executor.execute(new Runnable() {
                    public void run() {
                        Log.i("MediaConnection", "A2 - " + Thread.currentThread().getName());
                        if(MediaConnection.this.peerConnection != null && !MediaConnection.this.isError) {
                            Log.d("MediaConnection", "Set local SDP: " + MediaConnection.this.localSdp.description);
                            MediaConnection.this.peerConnection.setLocalDescription(MediaConnection.this.sdpObserver, MediaConnection.this.localSdp);
                        }

                    }
                });
                Log.i("MediaConnection", "A3 - " + Thread.currentThread().getName());
            }
        }

        public void onSetSuccess() {
            Log.i("MediaConnection", "B1 - " + Thread.currentThread().getName());
            MediaConnection.this.executor.execute(new Runnable() {
                public void run() {
                    Log.i("MediaConnection", "B2 - " + Thread.currentThread().getName());
                    if(MediaConnection.this.peerConnection != null && !MediaConnection.this.isError) {
                        if(MediaConnection.this.isInitiator) {
                            if(MediaConnection.this.peerConnection.getRemoteDescription() == null) {
                                Log.d("MediaConnection", "Local SDP set succesfully");
                                MediaConnection.this.mediaConnectionListener.onLocalDescription(MediaConnection.this.localSdp);
                            } else {
                                Log.d("MediaConnection", "Remote SDP set succesfully");
                                MediaConnection.this.drainCandidates();
                            }
                        } else if(MediaConnection.this.peerConnection.getLocalDescription() != null) {
                            Log.d("MediaConnection", "Local SDP set succesfully");
                            MediaConnection.this.mediaConnectionListener.onLocalDescription(MediaConnection.this.localSdp);
                            MediaConnection.this.drainCandidates();
                        } else {
                            Log.d("MediaConnection", "Remote SDP set succesfully");
                        }

                    }
                }
            });
            Log.i("MediaConnection", "B3 - " + Thread.currentThread().getName());
        }

        public void onCreateFailure(String error) {
            MediaConnection.this.reportError("createSDP error: " + error);
        }

        public void onSetFailure(String error) {
            MediaConnection.this.reportError("setSDP error: " + error);
        }
    }

    private class PCObserver implements Observer {
        private PCObserver() {
        }

        public void onIceCandidate(IceCandidate candidate) {
            MediaConnection.this.mediaConnectionListener.onIceCandidate(candidate);
        }

        public void onIceCandidatesRemoved(IceCandidate[] iceCandidates) {
        }

        public void onSignalingChange(SignalingState newState) {
            if(SignalingState.CLOSED.equals(newState)) {
                Log.d("MediaConnection", "Closing the audio manager...");
                if(MediaConnection.this.webRTCMediaProvider.size() <= 1) {
                    MediaConnection.this.webRTCMediaProvider.getAudioManager().close();
                }
            } else if(SignalingState.STABLE.equals(newState)) {
                Log.d("MediaConnection", "Initializing the audio manager...");
                MediaConnection.this.webRTCMediaProvider.getAudioManager().init();
            }

            Log.d("MediaConnection", "SignalingState: " + newState);
        }

        public void onIceConnectionChange(IceConnectionState newState) {
            Log.d("MediaConnection", "IceConnectionState: " + newState);
            if(newState == IceConnectionState.CONNECTED) {
                MediaConnection.this.mediaConnectionListener.onIceConnected();
            } else if(newState == IceConnectionState.DISCONNECTED) {
                MediaConnection.this.mediaConnectionListener.onIceDisconnected();
            } else if(newState == IceConnectionState.FAILED) {
                MediaConnection.this.reportError("ICE connection failed.");
            }

        }

        public void onIceConnectionReceivingChange(boolean b) {
        }

        public void onIceGatheringChange(IceGatheringState newState) {
            Log.d("MediaConnection", "IceGatheringState: " + newState);
        }

        public void onAddStream(final MediaStream stream) {
            Log.i("MediaConnection", "C1 - " + Thread.currentThread().getName());
            MediaConnection.this.executor.execute(new Runnable() {
                public void run() {
                    Log.i("MediaConnection", "C2 - " + Thread.currentThread().getName());
                    if(MediaConnection.this.peerConnection != null && !MediaConnection.this.isError) {
                        if(stream.audioTracks.size() <= 1 && stream.videoTracks.size() <= 1) {
                            if("mixedmslabel".equals(stream.label())) {
                                Log.d("MediaConnection", "Ignoring mixed stream");
                            } else if(stream.videoTracks.size() >= 1) {
                                Log.d("MediaConnection", "on add stream " + stream);
                                if(MediaConnection.this.remoteRenderer != null) {
                                    VideoTrack remoteVideoTrack = (VideoTrack)stream.videoTracks.get(0);
                                    remoteVideoTrack.setEnabled(MediaConnection.this.renderVideo);
                                    remoteVideoTrack.addRenderer(new VideoRenderer(MediaConnection.this.remoteRenderer));
                                }

                            }
                        } else {
                            MediaConnection.this.reportError("Weird-looking stream: " + stream);
                        }
                    }
                }
            });
            Log.i("MediaConnection", "C3 - " + Thread.currentThread().getName());
        }

        public void onRemoveStream(MediaStream stream) {
            Log.d("MediaConnection", "on remove stream " + stream);
            if(MediaConnection.this.peerConnection != null && !MediaConnection.this.isError) {
                Log.d("MediaConnection", "on removed stream " + stream);
            }
        }

        public void onDataChannel(DataChannel dc) {
        }

        public void onRenegotiationNeeded() {
        }
    }
}
