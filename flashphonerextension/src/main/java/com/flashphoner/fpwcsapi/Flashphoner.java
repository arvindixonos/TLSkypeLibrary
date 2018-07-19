package com.flashphoner.fpwcsapi;

/**
 * Created by TakeLeap05 on 12-07-2018.
 */
import android.content.Context;
import android.hardware.Camera;

import com.flashphoner.fpwcsapi.MediaDeviceList;
import com.flashphoner.fpwcsapi.constraints.Constraints;
import com.flashphoner.fpwcsapi.room.RoomManager;
import com.flashphoner.fpwcsapi.room.RoomManagerOptions;
import com.flashphoner.fpwcsapi.session.Session;
import com.flashphoner.fpwcsapi.session.SessionOptions;
import com.flashphoner.fpwcsapi.webrtc.MediaDevice;
import com.flashphoner.fpwcsapi.webrtc.WebRTCMediaProvider;

import org.webrtc.CameraEnumerationAndroid;
import org.webrtc.MediaStream;
import org.webrtc.SurfaceViewRenderer;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Flashphoner {
    private static final String TAG = Flashphoner.class.getSimpleName();
    public static final String VIDEO_CODEC_VP8 = "VP8";
    public static final String VIDEO_CODEC_VP9 = "VP9";
    public static final String VIDEO_CODEC_H264 = "H264";
    private static Context context;
    private static Map<String, Session> sessions = new ConcurrentHashMap();

    public Flashphoner() {
    }

    public static void init(Context context) {
        context = context;
        WebRTCMediaProvider.getInstance().init(context);
    }

    public static Collection<Session> getSessions() {
        return sessions.values();
    }

    public Session getSession(String id) {
        return (Session)sessions.get(id);
    }

    public static Session createSession(SessionOptions options) {
        Session session = new Session(options);
        sessions.put(session.getId(), session);
        return session;
    }

    public static List<MediaStream> getMediaAccess(Constraints constraints, SurfaceViewRenderer renderer) {
        return WebRTCMediaProvider.getInstance().getLocalMediaStreams(constraints, renderer, null);
    }

    public static MediaDeviceList getMediaDevices() {
        MediaDeviceList mediaDeviceList = new MediaDeviceList();
        MediaDevice mediaDevice = new MediaDevice(0, "mic", "Built-in microphone");
        mediaDeviceList.addAudioDevice(mediaDevice);

        for(int i = 0; i < Camera.getNumberOfCameras(); ++i) {
            mediaDevice = new MediaDevice(i, "camera", CameraEnumerationAndroid.getDeviceName(i));
            mediaDeviceList.addVideoDevice(mediaDevice);
        }

        return mediaDeviceList;
    }

    public static RoomManager createRoomManager(RoomManagerOptions options) {
        return new RoomManager(options);
    }

    public static void setVolume(int volume) {
        WebRTCMediaProvider.getInstance().getAudioManager().getAudioManager().setStreamVolume(0, volume, 0);
    }

    public static int getMaxVolume() {
        return WebRTCMediaProvider.getInstance().getAudioManager().getAudioManager().getStreamMaxVolume(0);
    }

    public static int getVolume() {
        return WebRTCMediaProvider.getInstance().getAudioManager().getAudioManager().getStreamVolume(0);
    }
}
