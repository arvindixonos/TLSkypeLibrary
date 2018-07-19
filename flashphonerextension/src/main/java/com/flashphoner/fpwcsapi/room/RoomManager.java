package com.flashphoner.fpwcsapi.room;

/**
 * Created by TakeLeap05 on 12-07-2018.
 */

import android.util.Log;

import com.flashphoner.fpwcsapi.bean.Connection;
import com.flashphoner.fpwcsapi.bean.CustomObject;
import com.flashphoner.fpwcsapi.bean.Data;
import com.flashphoner.fpwcsapi.session.RestAppCommunicator;
import com.flashphoner.fpwcsapi.session.Session;
import com.flashphoner.fpwcsapi.session.SessionEvent;

import com.flashphoner.fpwcsapi.Flashphoner;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

public class RoomManager {
    private static final String TAG = RoomManager.class.getSimpleName();
    private static final String ROOM_REST_APP = "roomApp";
    private com.flashphoner.fpwcsapi.room.RoomManagerEvent roomManagerEvent;
    private com.flashphoner.fpwcsapi.room.RoomManagerOptions options;
    private Session session;
    private final Map<String, Room> rooms = new ConcurrentHashMap();

    public RoomManager(com.flashphoner.fpwcsapi.room.RoomManagerOptions options) {
        this.options = options;
        this.session = Flashphoner.createSession(options);
        this.session.on(new SessionEvent() {
            public void onAppData(Data data) {
                CustomObject payload = new CustomObject((Map)data.getPayload());
                String roomName = payload.getCustomAsString("roomName", (String)null);
                Room room = (Room) RoomManager.this.rooms.get(roomName);
                if(room != null) {
                    room.onData(payload);
                } else {
                   // Log.w(VideoChatActivity.TAG, "Can not find room with name '" + roomName + "'");
                }

            }

            public void onConnected(Connection connection) {
                if(RoomManager.this.roomManagerEvent != null) {
                    RoomManager.this.roomManagerEvent.onConnected(connection);
                }

            }

            public void onRegistered(Connection connection) {
            }

            public void onDisconnection(Connection connection) {
                if(RoomManager.this.roomManagerEvent != null) {
                    RoomManager.this.roomManagerEvent.onDisconnection(connection);
                }

            }
        });
        Connection connection = new Connection();
        connection.setAppKey("roomApp");
        connection.setCustom("login", options.getUsername());
        this.session.connect(connection);
    }

    public Room join(com.flashphoner.fpwcsapi.room.RoomOptions options) {
        final Room room = new Room(options, this);
        room.sendAppCommand("join", options, new RestAppCommunicator.Handler() {
            public void onAccepted(Data data) {
            }

            public void onRejected(Data data) {
                if(room.getRoomEvent() != null) {
                    room.getRoomEvent().onFailed(room, data.getInfo());
                }

            }
        });
        this.rooms.put(room.getName(), room);
        return room;
    }

    public void disconnect() {
        Iterator iterator = this.rooms.entrySet().iterator();

        while(iterator.hasNext()) {
            ((Room)((Entry)iterator.next()).getValue()).leave((RestAppCommunicator.Handler)null);
            iterator.remove();
        }

        this.session.disconnect();
    }

    public void on(com.flashphoner.fpwcsapi.room.RoomManagerEvent roomManagerEvent) {
        this.roomManagerEvent = roomManagerEvent;
    }

    public String getUsername() {
        return this.options.getUsername();
    }

    public void removeRoom(Room room) {
        this.rooms.remove(room.getName());
    }

    public Session getSession() {
        return this.session;
    }
}
