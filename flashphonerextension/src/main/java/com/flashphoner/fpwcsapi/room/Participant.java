package com.flashphoner.fpwcsapi.room;

/**
 * Created by TakeLeap05 on 12-07-2018.
 */
import com.flashphoner.fpwcsapi.room.Message;
import com.flashphoner.fpwcsapi.session.Session;
import com.flashphoner.fpwcsapi.session.Stream;
import com.flashphoner.fpwcsapi.session.StreamOptions;

import org.webrtc.SurfaceViewRenderer;

public class Participant {
    private String name;
    private String streamName;
    private Room room;
    private Stream stream;

    public Participant(String name, Room room) {
        this.name = name;
        this.room = room;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStreamName() {
        return this.streamName;
    }

    public void setStreamName(String streamName) {
        this.streamName = streamName;
    }

    public Stream getStream() {
        return this.stream;
    }

    public synchronized Stream play(SurfaceViewRenderer renderer) {
        if(this.streamName != null) {
            this.stop();
            Session session = this.room.getRoomManager().getSession();
            StreamOptions streamOptions = new StreamOptions(this.streamName);
            streamOptions.setRenderer(renderer);
            this.stream = session.createStream(streamOptions);
            this.stream.play();
            return this.stream;
        } else {
            return null;
        }
    }

    public synchronized void stop() {
        if(this.stream != null) {
            this.stream.stop();
            this.stream = null;
        }

    }

    public void sendMessage(String text) {
        Message message = new Message();
        message.setTo(this.name);
        message.setText(text);
        message.getRoomConfig().put("name", this.room.getName());
        this.room.sendAppCommand("sendMessage", message, null);
    }
}
