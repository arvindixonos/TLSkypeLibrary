package com.flashphoner.fpwcsapi.room;

import com.flashphoner.fpwcsapi.room.Message;

/**
 * Created by TakeLeap05 on 12-07-2018.
 */

public interface RoomEvent
{
    void onState(Room var1);

    void onJoined(Participant var1);

    void onLeft(Participant var1);

    void onPublished(Participant var1);

    void onFailed(Room var1, String var2);

    void onMessage(Message var1);
}
