package com.flashphoner.fpwcsapi.session;

import com.flashphoner.fpwcsapi.bean.Data;

import java.lang.Object;
import java.lang.String;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class RestAppCommunicator {
    private Map<String, Handler> handlers = new ConcurrentHashMap();
    private Session session;

    public RestAppCommunicator(Session session) {
        this.session = session;
    }

    public void sendData(Object data, Handler handler) {
        Data d = new Data();
        String operationId = UUID.randomUUID().toString();
        d.setOperationId(operationId);
        d.setPayload(data);
        this.session.send("sendData", d);
        if (handler == null) return;
        this.handlers.put((String) operationId, (Handler)handler);
    }

    void resolveData(Data data) {
        if ("FAILED".equals((Object)data.getStatus())) {
            ((Handler)this.handlers.remove((Object)data.getOperationId())).onRejected(data);
            return;
        }
        ((Handler)this.handlers.remove((Object)data.getOperationId())).onAccepted(data);
    }

    public static interface Handler {
        public void onAccepted(Data var1);

        public void onRejected(Data var1);
    }

}
