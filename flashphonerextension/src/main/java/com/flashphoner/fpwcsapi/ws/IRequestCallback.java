package com.flashphoner.fpwcsapi.ws;

/**
 * Created by TakeLeap05 on 12-07-2018.
 */

import com.flashphoner.fpwcsapi.bean.Connection;
import com.flashphoner.fpwcsapi.bean.Data;
import com.flashphoner.fpwcsapi.session.CallObject;
import com.flashphoner.fpwcsapi.session.StreamObject;

public interface IRequestCallback {
    void ping();

    void getUserData(Connection var1);

    void getVersion(String var1);

    void registered();

    void setRemoteSDP(String var1, String var2, Boolean var3);

    void notifyTryingResponse(CallObject var1);

    void ring(CallObject var1);

    void busy(CallObject var1);

    void hold(CallObject var1);

    void talk(CallObject var1);

    void finish(CallObject var1);

    void notifyIncomingCall(CallObject var1);

    void OnDataEvent(Data var1);

    void DataStatusEvent(Data var1);

    void notifyStreamStatusEvent(StreamObject var1);
}
