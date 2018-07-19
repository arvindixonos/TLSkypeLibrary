package com.flashphoner.fpwcsapi.session;

/**
 * Created by TakeLeap05 on 12-07-2018.
 */


public interface CallStatusEvent {
    void onTrying(Call var1);

    void onBusy(Call var1);

    void onFailed(Call var1);

    void onRing(Call var1);

    void onHold(Call var1);

    void onEstablished(Call var1);

    void onFinished(Call var1);
}