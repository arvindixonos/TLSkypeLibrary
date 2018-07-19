package org.webrtc;

/**
 * Created by TakeLeap05 on 12-07-2018.
 */

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.webrtc.DataChannel.Init;

public class PeerConnection {
    private final List<MediaStream> localStreams;
    private final long nativePeerConnection;
    private final long nativeObserver;
    private List<RtpSender> senders;
    private List<RtpReceiver> receivers;

    PeerConnection(long nativePeerConnection, long nativeObserver) {
        this.nativePeerConnection = nativePeerConnection;
        this.nativeObserver = nativeObserver;
        this.localStreams = new LinkedList();
        this.senders = new LinkedList();
        this.receivers = new LinkedList();
    }

    public native SessionDescription getLocalDescription();

    public native SessionDescription getRemoteDescription();

    public native DataChannel createDataChannel(String var1, Init var2);

    public native void createOffer(SdpObserver var1, MediaConstraints var2);

    public native void createAnswer(SdpObserver var1, MediaConstraints var2);

    public native void setLocalDescription(SdpObserver var1, SessionDescription var2);

    public native void setRemoteDescription(SdpObserver var1, SessionDescription var2);

    public native boolean setConfiguration(RTCConfiguration var1);

    public boolean addIceCandidate(IceCandidate candidate) {
        return this.nativeAddIceCandidate(candidate.sdpMid, candidate.sdpMLineIndex, candidate.sdp);
    }

    public boolean removeIceCandidates(IceCandidate[] candidates) {
        return this.nativeRemoveIceCandidates(candidates);
    }

    public boolean addStream(MediaStream stream) {
        boolean ret = this.nativeAddLocalStream(stream.nativeStream);
        if(!ret) {
            return false;
        } else {
            this.localStreams.add(stream);
            return true;
        }
    }

    public void removeStream(MediaStream stream) {
        this.nativeRemoveLocalStream(stream.nativeStream);
        this.localStreams.remove(stream);
    }

    public RtpSender createSender(String kind, String stream_id) {
        RtpSender new_sender = this.nativeCreateSender(kind, stream_id);
        if(new_sender != null) {
            this.senders.add(new_sender);
        }

        return new_sender;
    }

    public List<RtpSender> getSenders() {
        Iterator var1 = this.senders.iterator();

        while(var1.hasNext()) {
            RtpSender sender = (RtpSender)var1.next();
            sender.dispose();
        }

        this.senders = this.nativeGetSenders();
        return Collections.unmodifiableList(this.senders);
    }

    public List<RtpReceiver> getReceivers() {
        Iterator var1 = this.receivers.iterator();

        while(var1.hasNext()) {
            RtpReceiver receiver = (RtpReceiver)var1.next();
            receiver.dispose();
        }

        this.receivers = this.nativeGetReceivers();
        return Collections.unmodifiableList(this.receivers);
    }

    public boolean getStats(StatsObserver observer, MediaStreamTrack track) {
        return this.nativeGetStats(observer, track == null?0L:track.nativeTrack);
    }

    public native SignalingState signalingState();

    public native IceConnectionState iceConnectionState();

    public native IceGatheringState iceGatheringState();

    public native void close();

    public void dispose() {
        this.close();
        Iterator var1 = this.localStreams.iterator();

        while(var1.hasNext()) {
            MediaStream stream = (MediaStream)var1.next();
            this.nativeRemoveLocalStream(stream.nativeStream);
            stream.dispose();
        }

        this.localStreams.clear();
        var1 = this.senders.iterator();

        while(var1.hasNext()) {
            RtpSender sender = (RtpSender)var1.next();
            sender.dispose();
        }

        this.senders.clear();
        var1 = this.receivers.iterator();

        while(var1.hasNext()) {
            RtpReceiver receiver = (RtpReceiver)var1.next();
            receiver.dispose();
        }

        this.receivers.clear();
        freePeerConnection(this.nativePeerConnection);
        freeObserver(this.nativeObserver);
    }

    private static native void freePeerConnection(long var0);

    private static native void freeObserver(long var0);

    private native boolean nativeAddIceCandidate(String var1, int var2, String var3);

    private native boolean nativeRemoveIceCandidates(IceCandidate[] var1);

    private native boolean nativeAddLocalStream(long var1);

    private native void nativeRemoveLocalStream(long var1);

    private native boolean nativeGetStats(StatsObserver var1, long var2);

    private native RtpSender nativeCreateSender(String var1, String var2);

    private native List<RtpSender> nativeGetSenders();

    private native List<RtpReceiver> nativeGetReceivers();

    static {
        System.loadLibrary("jingle_peerconnection_so");
    }

    public static class RTCConfiguration {
        public IceTransportsType iceTransportsType;
        public List<IceServer> iceServers;
        public BundlePolicy bundlePolicy;
        public RtcpMuxPolicy rtcpMuxPolicy;
        public TcpCandidatePolicy tcpCandidatePolicy;
        public int audioJitterBufferMaxPackets;
        public boolean audioJitterBufferFastAccelerate;
        public int iceConnectionReceivingTimeout;
        public int iceBackupCandidatePairPingInterval;
        public KeyType keyType;
        public ContinualGatheringPolicy continualGatheringPolicy;

        public RTCConfiguration(List<IceServer> iceServers) {
            this.iceTransportsType = IceTransportsType.ALL;
            this.bundlePolicy = BundlePolicy.BALANCED;
            this.rtcpMuxPolicy = RtcpMuxPolicy.NEGOTIATE;
            this.tcpCandidatePolicy = TcpCandidatePolicy.ENABLED;
            this.iceServers = iceServers;
            this.audioJitterBufferMaxPackets = 50;
            this.audioJitterBufferFastAccelerate = false;
            this.iceConnectionReceivingTimeout = -1;
            this.iceBackupCandidatePairPingInterval = -1;
            this.keyType = KeyType.ECDSA;
            this.continualGatheringPolicy = ContinualGatheringPolicy.GATHER_ONCE;
        }
    }

    public static enum ContinualGatheringPolicy {
        GATHER_ONCE,
        GATHER_CONTINUALLY;

        private ContinualGatheringPolicy() {
        }
    }

    public static enum KeyType {
        RSA,
        ECDSA;

        private KeyType() {
        }
    }

    public static enum TcpCandidatePolicy {
        ENABLED,
        DISABLED;

        private TcpCandidatePolicy() {
        }
    }

    public static enum RtcpMuxPolicy {
        NEGOTIATE,
        REQUIRE;

        private RtcpMuxPolicy() {
        }
    }

    public static enum BundlePolicy {
        BALANCED,
        MAXBUNDLE,
        MAXCOMPAT;

        private BundlePolicy() {
        }
    }

    public static enum IceTransportsType {
        NONE,
        RELAY,
        NOHOST,
        ALL;

        private IceTransportsType() {
        }
    }

    public static class IceServer {
        public final String uri;
        public final String username;
        public final String password;

        public IceServer(String uri) {
            this(uri, "", "");
        }

        public IceServer(String uri, String username, String password) {
            this.uri = uri;
            this.username = username;
            this.password = password;
        }

        public String toString() {
            return this.uri + "[" + this.username + ":" + this.password + "]";
        }
    }

    public interface Observer {
        void onSignalingChange(SignalingState var1);

        void onIceConnectionChange(IceConnectionState var1);

        void onIceConnectionReceivingChange(boolean var1);

        void onIceGatheringChange(IceGatheringState var1);

        void onIceCandidate(IceCandidate var1);

        void onIceCandidatesRemoved(IceCandidate[] var1);

        void onAddStream(MediaStream var1);

        void onRemoveStream(MediaStream var1);

        void onDataChannel(DataChannel var1);

        void onRenegotiationNeeded();
    }

    public static enum SignalingState {
        STABLE,
        HAVE_LOCAL_OFFER,
        HAVE_LOCAL_PRANSWER,
        HAVE_REMOTE_OFFER,
        HAVE_REMOTE_PRANSWER,
        CLOSED;

        private SignalingState() {
        }
    }

    public static enum IceConnectionState {
        NEW,
        CHECKING,
        CONNECTED,
        COMPLETED,
        FAILED,
        DISCONNECTED,
        CLOSED;

        private IceConnectionState() {
        }
    }

    public static enum IceGatheringState {
        NEW,
        GATHERING,
        COMPLETE;

        private IceGatheringState() {
        }
    }
}