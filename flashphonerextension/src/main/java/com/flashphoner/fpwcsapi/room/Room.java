package com.flashphoner.fpwcsapi.room;


import android.hardware.Camera;

import com.flashphoner.fpwcsapi.bean.CustomObject;
import com.flashphoner.fpwcsapi.bean.Data;
import com.flashphoner.fpwcsapi.session.RestAppCommunicator;
import com.flashphoner.fpwcsapi.session.Stream;
import com.flashphoner.fpwcsapi.session.StreamOptions;

import org.webrtc.SurfaceViewRenderer;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

public class Room {
    private RoomOptions options;
    private RoomManager roomManager;
    private Stream stream;
    private RoomEvent roomEvent;
    private final Map<String, Participant> participants = new ConcurrentHashMap();
    private boolean leaved = false;

    public Room(RoomOptions options, RoomManager roomManager) {
        this.options = options;
        this.roomManager = roomManager;
    }

    public synchronized void leave(RestAppCommunicator.Handler handler) {
        if(this.stream != null) {
            this.stream.stop();
        }

        Map var2 = this.participants;
        synchronized(this.participants) {
            Iterator iterator = this.participants.entrySet().iterator();

            while(true) {
                if(!iterator.hasNext()) {
                    break;
                }

                Participant participant = (Participant)((Entry)iterator.next()).getValue();
                participant.stop();
                iterator.remove();
            }
        }

        this.roomManager.removeRoom(this);
        this.sendAppCommand("leave", this.options, handler);
        this.leaved = true;
    }

    public Stream publish(SurfaceViewRenderer renderer, Camera.PreviewCallback targetPreviewCallback) {
        if(this.stream == null) {
            StreamOptions streamOptions = new StreamOptions(this.options.getName() + "-" + this.roomManager.getUsername());
            streamOptions.setRenderer(renderer);
            streamOptions.setCustom("name", this.getName());
            streamOptions.getConstraints().updateVideo(true);
            this.stream = this.roomManager.getSession().createStream(streamOptions);
            this.stream.publish(targetPreviewCallback);
        }

        return this.stream;
    }

    public Stream publish(SurfaceViewRenderer renderer) {
        return publish(renderer, null);
    }

    public void unpublish() {
        if(this.stream != null) {
            this.stream.stop();
            this.stream = null;
        }

    }

    public void on(RoomEvent roomEvent) {
        this.roomEvent = roomEvent;
    }

    public String getName() {
        return this.options.getName();
    }

    public Collection<Participant> getParticipants() {
        return this.participants.values();
    }

    void sendAppCommand(String name, Object data, RestAppCommunicator.Handler handler) {
        RoomCommand roomCommand = new RoomCommand(name, data);
        this.roomManager.getSession().getRestAppCommunicator().sendData(roomCommand, handler);
    }

    synchronized void onData(CustomObject payload) {
        if(!this.leaved) {
            if("STATE".equals(payload.getCustomAsString("name", (String)null))) {
                List<Object> info = payload.getCustomAsList("info", (List)null);
                Iterator var3 = info.iterator();

                while(true) {
                    while(var3.hasNext()) {
                        Object i = var3.next();
                        Participant participant;
                        if(i instanceof Map && ((Map)i).get("login") != null) {
                            participant = new Participant((String)((Map)i).get("login"), this);
                            participant.setStreamName((String)((Map)i).get("name"));
                            this.participants.put(participant.getName(), participant);
                        } else if(i instanceof String) {
                            participant = new Participant((String)i, this);
                            this.participants.put(participant.getName(), participant);
                        }
                    }

                    if(this.roomEvent != null) {
                        this.roomEvent.onState(this);
                    }
                    break;
                }
            } else {
                Participant participant;
                if("JOINED".equals(payload.getCustomAsString("name", (String)null))) {
                    participant = new Participant(payload.getCustomAsString("info", (String)null), this);
                    this.participants.put(participant.getName(), participant);
                    if(this.roomEvent != null) {
                        this.roomEvent.onJoined(participant);
                    }
                } else if("LEFT".equals(payload.getCustomAsString("name", (String)null))) {
                    participant = (Participant)this.participants.remove(payload.getCustomAsString("info", (String)null));
                    if(this.roomEvent != null) {
                        this.roomEvent.onLeft(participant);
                    }
                } else if("PUBLISHED".equals(payload.getCustomAsString("name", (String)null))) {
                    CustomObject info = payload.getCustomAsContext("info", (CustomObject)null);
                    participant = (Participant)this.participants.get(info.getCustomAsString("login", (String)null));
                    participant.setStreamName(info.getCustomAsString("name", (String)null));
                    if(this.roomEvent != null) {
                        this.roomEvent.onPublished(participant);
                    }
                } else if("MESSAGE".equals(payload.getCustomAsString("name", (String)null)) && this.roomEvent != null) {
                    Message message = new Message();
                    CustomObject info = payload.getCustomAsContext("info", (CustomObject)null);
                    message.setFrom(info.getCustomAsString("from", (String)null));
                    message.setText(info.getCustomAsString("text", (String)null));
                    this.roomEvent.onMessage(message);
                }
            }

        }
    }

    RoomManager getRoomManager() {
        return this.roomManager;
    }

    RoomEvent getRoomEvent() {
        return this.roomEvent;
    }
}


