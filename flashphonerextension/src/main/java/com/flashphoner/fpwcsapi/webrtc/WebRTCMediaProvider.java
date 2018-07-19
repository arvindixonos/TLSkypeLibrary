package com.flashphoner.fpwcsapi.webrtc;

/**
 * Created by TakeLeap05 on 12-07-2018.
 */

import android.content.Context;
import android.hardware.Camera;
import android.util.Log;
import com.flashphoner.fpwcsapi.WCSAudioManager;
import com.flashphoner.fpwcsapi.constraints.AudioConstraints;
import com.flashphoner.fpwcsapi.constraints.Constraints;
import com.flashphoner.fpwcsapi.constraints.VideoConstraints;
import com.flashphoner.fpwcsapi.util.LooperExecutor;
import com.flashphoner.fpwcsapi.util.Utils;

import org.webrtc.AudioTrack;
import org.webrtc.CameraEnumerationAndroid;
import org.webrtc.EglBase;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.RendererCommon;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoCapturerAndroid;
import org.webrtc.VideoRenderer;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;


public class WebRTCMediaProvider {
    public static final String VIDEO_TRACK_ID = "ARDAMSv0-";
    public static final String AUDIO_TRACK_ID = "ARDAMSa0-";
    private static final String TAG = "WebRTCMediaProvider";
    private static final WebRTCMediaProvider instance = new WebRTCMediaProvider();
    private final LooperExecutor executor = new LooperExecutor();
    public org.webrtc.PeerConnectionFactory factory;
    org.webrtc.PeerConnectionFactory.Options options;
    public VideoSource videoSource;
    public VideoCapturerAndroid videoCapturer;
    public WCSAudioManager audioManager;
    public Map<String, MediaConnection> mediaConnections = new ConcurrentHashMap();
    public List<MediaStream> mediaStreams = new LinkedList();

    private WebRTCMediaProvider() {
        this.executor.requestStart();
    }

    public static WebRTCMediaProvider getInstance() {
        return instance;
    }

    public void setPeerConnectionFactoryOptions(org.webrtc.PeerConnectionFactory.Options options) {
        this.options = options;
    }

    public void init(final Context context) {
        this.executor.execute(new Runnable() {
            public void run() {
                Log.d("WebRTCMediaProvider", "Creating peer connection factory.");

                PeerConnectionFactory.initializeFieldTrials((String)null);
                if(!PeerConnectionFactory.initializeAndroidGlobals(context, true, true, true)) {
                    Log.e("WebRTCMediaProvider", "Can not initialize PeerConnectionFactory.initializeAndroidGlobals");
                }

                WebRTCMediaProvider.this.factory = new  PeerConnectionFactory(WebRTCMediaProvider.this.options);
//                Logging.enableTracing("logcat:", EnumSet.of(TraceLevel.TRACE_DEFAULT), Severity.LS_INFO);
                Log.d("WebRTCMediaProvider", "Peer connection factory created.");
            }
        });
        this.audioManager = WCSAudioManager.create(context, new Runnable() {
            public void run() {
            }
        });
    }

    public MediaConnection createMediaConnection(String id) {
        return this.createMediaConnection(id, (String)null, (SurfaceViewRenderer)null);
    }

    public MediaConnection createMediaConnection(String id, String videoCodec) {
        return this.createMediaConnection(id, videoCodec, (SurfaceViewRenderer)null);
    }

    public MediaConnection createMediaConnection(String id, SurfaceViewRenderer remoteRender) {
        return this.createMediaConnection(id, (String)null, remoteRender);
    }

    public synchronized MediaConnection createMediaConnection(String id, String videoCodec, SurfaceViewRenderer remoteRender) {
        if(this.factory == null) {
            Log.e("WebRTCMediaProvider", "Creating peer connection without initializing factory.");
            return null;
        } else {
            try {
                if(remoteRender != null) {
                    remoteRender.init(EglBase.create().getEglBaseContext(), (RendererCommon.RendererEvents)null);
                }
            } catch (IllegalStateException var5) {
                ;
            }

            MediaConnection mediaConnection = new MediaConnection(id, remoteRender, this, this.executor, videoCodec);
            this.mediaConnections.put(id, mediaConnection);
            return mediaConnection;
        }
    }

    public synchronized void removeMediaConnection(String id) {
        MediaConnection mediaConnection = (MediaConnection)this.mediaConnections.remove(id);
        if(mediaConnection != null) {
            mediaConnection.close();
        }

    }

    public MediaConnection getMediaConnection(String id) {
        return (MediaConnection)this.mediaConnections.get(id);
    }

    public void close() {
        this.executor.execute(new Runnable() {
            public void run() {
                Iterator iterator = WebRTCMediaProvider.this.mediaConnections.entrySet().iterator();

                while(iterator.hasNext()) {
                    ((MediaConnection)((Entry)iterator.next()).getValue()).close();
                    iterator.remove();
                }

                Iterator streamIterator = WebRTCMediaProvider.this.mediaStreams.iterator();

                while(streamIterator.hasNext()) {
                    MediaStream mediaStream = (MediaStream)streamIterator.next();
                    mediaStream.dispose();
                    streamIterator.remove();
                }

                if(WebRTCMediaProvider.this.videoCapturer != null) {
                    try {
                        WebRTCMediaProvider.this.videoCapturer.stopCapture();
                    } catch (InterruptedException var4) {
                        var4.printStackTrace();
                    }

                    WebRTCMediaProvider.this.videoCapturer.dispose();
                    WebRTCMediaProvider.this.videoCapturer = null;
                }

                if(WebRTCMediaProvider.this.factory != null) {
                    WebRTCMediaProvider.this.factory.dispose();
                    WebRTCMediaProvider.this.factory = null;
                }

                if(WebRTCMediaProvider.this.audioManager != null) {
                    WebRTCMediaProvider.this.audioManager.close();
                    WebRTCMediaProvider.this.audioManager = null;
                }

            }
        });
    }

    public static int   cameraID = 1;

    public List<MediaStream> getLocalMediaStreams(Constraints constraints, SurfaceViewRenderer renderer)
    {
        return getLocalMediaStreams(constraints, renderer, null);
    }

    public List<MediaStream> getLocalMediaStreams(Constraints constraints, SurfaceViewRenderer renderer, Camera.PreviewCallback targetPreviewCallback) {
        List<MediaStream> streams = new ArrayList();
        MediaConstraints mediaConstraintsForAudio = this.getMediaConstraintsForAudio(constraints.getAudioConstraints());
        if(mediaConstraintsForAudio != null) {
            MediaStream audioMediaStream = this.factory.createLocalMediaStream("ARDAMS-" + Utils.md5(mediaConstraintsForAudio.toString()));
            AudioTrack audioTrack = this.factory.createAudioTrack("ARDAMSa0-" + Utils.md5(mediaConstraintsForAudio.toString()),
                    this.factory.createAudioSource(mediaConstraintsForAudio));
            audioMediaStream.addTrack(audioTrack);
            streams.add(audioMediaStream);
            this.mediaStreams.add(audioMediaStream);
        }

        MediaConstraints mediaConstraintsForVideo = this.getMediaConstraintsForVideo(constraints.getVideoConstraints());
        if(mediaConstraintsForVideo != null) {
            MediaStream videoMediaStream = this.factory.createLocalMediaStream("ARDVMS-" + Utils.md5(mediaConstraintsForVideo.toString()));
            Integer cameraId = constraints.getVideoConstraints().getCameraId();
            String cameraDeviceName = CameraEnumerationAndroid.getDeviceName(cameraID);
            if(this.videoCapturer != null) {
                try {
                    this.videoCapturer.stopCapture();
                } catch (InterruptedException var13) {
                    var13.printStackTrace();
                }

                this.videoCapturer.dispose();
            }

            this.videoCapturer = VideoCapturerAndroid.create(cameraDeviceName, null, targetPreviewCallback);
            if(this.videoCapturer == null) {
                Log.e("WebRTCMediaProvider", "Failed to open camera");
                return streams;
            }

            VideoSource videoSource = this.factory.createVideoSource(this.videoCapturer, constraints.getVideoConstraints().getMediaConstraints());
            VideoTrack videoTrack = this.factory.createVideoTrack("ARDAMSv0-" + Utils.md5(mediaConstraintsForVideo.toString()), videoSource);
            if(renderer != null) {
                try {
                    renderer.init(EglBase.create().getEglBaseContext(), (RendererCommon.RendererEvents)null);
                } catch (IllegalStateException var12) {
                    ;
                }

                videoTrack.addRenderer(new VideoRenderer(renderer));
            }

            videoMediaStream.addTrack(videoTrack);
            streams.add(videoMediaStream);
            this.mediaStreams.add(videoMediaStream);
        }

        return streams;
    }

    private MediaConstraints getMediaConstraintsForAudio(AudioConstraints audioConstraints) {
        if(audioConstraints == null) {
            return null;
        } else {
            MediaConstraints mediaConstraints = audioConstraints.getMediaConstraints();
            mediaConstraints.optional.add(new MediaConstraints.KeyValuePair("googEchoCancellation", "true"));
            mediaConstraints.optional.add(new MediaConstraints.KeyValuePair("googAutoGainControl", "true"));
            mediaConstraints.optional.add(new MediaConstraints.KeyValuePair("googHighpassFilter", "true"));
            mediaConstraints.optional.add(new MediaConstraints.KeyValuePair("googNoiseSupression", "true"));
            mediaConstraints.optional.add(new MediaConstraints.KeyValuePair("googNoisesuppression2", "true"));
            mediaConstraints.optional.add(new MediaConstraints.KeyValuePair("googEchoCancellation2", "true"));
            mediaConstraints.optional.add(new MediaConstraints.KeyValuePair("googAutoGainControl2", "true"));
            return mediaConstraints;
        }
    }

    private MediaConstraints getMediaConstraintsForVideo(VideoConstraints videoConstraints) {
        if(videoConstraints == null) {
            return null;
        } else if(CameraEnumerationAndroid.getDeviceCount() == 0) {
            Log.w("WebRTCMediaProvider", "No camera on device. Switch to audio only call.");
            return new MediaConstraints();
        } else {
            return videoConstraints.getMediaConstraints();
        }
    }

    public int size() {
        return this.mediaConnections.size();
    }

    public PeerConnectionFactory getFactory() {
        return this.factory;
    }

    public WCSAudioManager getAudioManager() {
        return this.audioManager;
    }
}

