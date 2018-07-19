package com.flashphoner.fpwcsapi.session;

/**
 * Created by TakeLeap05 on 12-07-2018.
 */

import com.flashphoner.fpwcsapi.bean.StreamStatus;

public interface StreamStatusEvent {
    void onStreamStatus(Stream var1, StreamStatus var2);
}
