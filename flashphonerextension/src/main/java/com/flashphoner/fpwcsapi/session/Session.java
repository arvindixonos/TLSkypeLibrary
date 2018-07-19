package com.flashphoner.fpwcsapi.session;

/**
 * Created by TakeLeap05 on 12-07-2018.
 */


import android.util.Log;

import com.flashphoner.fpwcsapi.webrtc.MediaConnection;
import com.flashphoner.fpwcsapi.webrtc.WebRTCMediaProvider;
import com.flashphoner.fpwcsapi.ws.IRequestCallback;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.webrtc.SessionDescription;
import org.webrtc.SessionDescription.Type;


public class Session {
    private static final String TAG = Session.class.getSimpleName();
    private String id = UUID.randomUUID().toString();
    private com.flashphoner.fpwcsapi.session.SessionOptions sessionOptions;
    private com.flashphoner.fpwcsapi.bean.Connection connection;
    private com.flashphoner.fpwcsapi.ws.WebSocketChannelClient webSocketChannelClient;
    private com.flashphoner.fpwcsapi.session.SessionEvent sessionEvent;
    private IncomingCallEvent incomingCallEvent;
    private IRequestCallback requestCallback;
    Map<String, Stream> streams = new ConcurrentHashMap();
    Map<String, Call> calls = new ConcurrentHashMap();
    private Map<String, SessionDescription> sessionDescriptions = new ConcurrentHashMap();
    private WebRTCMediaProvider webRTCMediaProvider = WebRTCMediaProvider.getInstance();
    private RestAppCommunicator restAppCommunicator;
    public static com.flashphoner.fpwcsapi.util.LooperExecutor executor = new com.flashphoner.fpwcsapi.util.LooperExecutor();

    public Session(com.flashphoner.fpwcsapi.session.SessionOptions sessionOptions) {
        this.sessionOptions = sessionOptions;
        this.restAppCommunicator = new RestAppCommunicator(this);
        this.requestCallback = new IRequestCallback() {
            public void ping() {
                Session.this.webSocketChannelClient.execute("pong");
            }

            public void getUserData(com.flashphoner.fpwcsapi.bean.Connection connection) {
                Session.this.connection = connection;
                if(Session.this.sessionEvent != null) {
                    Session.this.sessionEvent.onConnected(connection);
                }

            }

            public void getVersion(String version) {
            }

            public void registered() {
                if(Session.this.sessionEvent != null) {
                    Session.this.connection.setStatus("REGISTERED");
                    Session.this.sessionEvent.onRegistered(Session.this.connection);
                }

            }

            public void setRemoteSDP(String id, String sdp, Boolean isInitiator) {
                MediaConnection mediaConnection = Session.this.webRTCMediaProvider.getMediaConnection(id);
                if(mediaConnection != null) {
                    SessionDescription sessionDescription;
                    if(isInitiator.booleanValue()) {
                        sessionDescription = new SessionDescription(Type.ANSWER, sdp);
                        mediaConnection.setRemoteDescription(sessionDescription);
                    } else {
                        sessionDescription = new SessionDescription(Type.OFFER, sdp);
                        mediaConnection.setRemoteDescription(sessionDescription);
                    }
                } else if(!isInitiator.booleanValue()) {
                    Session.this.sessionDescriptions.put(id, new SessionDescription(Type.OFFER, sdp));
                }

            }

            public void notifyTryingResponse(com.flashphoner.fpwcsapi.session.CallObject callObject) {
                Call call = (Call) Session.this.calls.get(callObject.getCallId());
                if(call != null) {
                    call.onCallProperties(callObject);
                }

            }

            public void ring(com.flashphoner.fpwcsapi.session.CallObject callObject) {
                Call call = (Call) Session.this.calls.get(callObject.getCallId());
                if(call != null) {
                    call.onCallProperties(callObject);
                }

            }

            public void busy(com.flashphoner.fpwcsapi.session.CallObject callObject) {
                Call call = (Call) Session.this.calls.get(callObject.getCallId());
                if(call != null) {
                    call.onCallProperties(callObject);
                }

            }

            public void hold(com.flashphoner.fpwcsapi.session.CallObject callObject) {
                Call call = (Call) Session.this.calls.get(callObject.getCallId());
                if(call != null) {
                    call.onCallProperties(callObject);
                }

            }

            public void talk(com.flashphoner.fpwcsapi.session.CallObject callObject) {
                Call call = (Call) Session.this.calls.get(callObject.getCallId());
                if(call != null) {
                    call.onCallProperties(callObject);
                }

            }

            public void finish(com.flashphoner.fpwcsapi.session.CallObject callObject) {
                Call call = (Call) Session.this.calls.get(callObject.getCallId());
                if(call != null) {
                    call.onCallProperties(callObject);
                }

            }

            public void notifyIncomingCall(com.flashphoner.fpwcsapi.session.CallObject callObject) {
                Call call = new Call(callObject, Session.this);
                Session.this.calls.put(call.getId(), call);
                if(Session.this.incomingCallEvent != null) {
                    Session.this.incomingCallEvent.onCall(call);
                } else {
                    call.hangup();
                }

            }

            @Override
            public void OnDataEvent(com.flashphoner.fpwcsapi.bean.Data data) {
                if(Session.this.sessionEvent != null) {
                    Session.this.sessionEvent.onAppData(data);
                }

            }

            public void DataStatusEvent(com.flashphoner.fpwcsapi.bean.Data data) {
                Session.this.restAppCommunicator.resolveData(data);
            }

            public void notifyStreamStatusEvent(StreamObject streamObject) {
                Stream requestedStream = (Stream) Session.this.streams.get(streamObject.getMediaSessionId());
                if(requestedStream != null) {
                    requestedStream.onStreamProperties(streamObject);
                }

            }
        };
    }

    public void connect(final com.flashphoner.fpwcsapi.bean.Connection connection) {
        this.connection = connection;
        this.webSocketChannelClient = new com.flashphoner.fpwcsapi.ws.WebSocketChannelClient(new com.flashphoner.fpwcsapi.ws.WebSocketChannelEvents() {
            public void onWebSocketOpen() {
                Session.this.webSocketChannelClient.execute("connection", connection);
            }

            public void onWebSocketMessage(com.flashphoner.fpwcsapi.ws.WSMessage wsMessage) {
                Gson gson = new Gson();
                com.flashphoner.fpwcsapi.ws.CallArguments callArguments = new com.flashphoner.fpwcsapi.ws.CallArguments(wsMessage.getData());

                try {
                    Method[] var4 = Session.this.requestCallback.getClass().getMethods();
                    int var5 = var4.length;

                    for(int var6 = 0; var6 < var5; ++var6) {
                        Method method = var4[var6];
                        if(wsMessage.getMessage().equals(method.getName())) {
                            Class[] parameterTypes = method.getParameterTypes();
                            Object[] args = new Object[parameterTypes.length];

                            for(int i = 0; i < parameterTypes.length; ++i) {
                                JsonElement jsonElement = gson.toJsonTree(callArguments.getArgument(Integer.valueOf(i)));
                                args[i] = gson.fromJson(jsonElement, parameterTypes[i]);
                            }

                            Log.i(Session.TAG, "Invoker class method: " + method.getName());
                            method.invoke(Session.this.requestCallback, args);
                            return;
                        }
                    }

                    Log.w(Session.TAG, "No such method " + wsMessage);
                } catch (Throwable var12) {
                    Log.e(Session.TAG, var12.getMessage(), var12);
                }

            }

            public void onWebSocketClose(int code) {
                if(code != 1 && code != 3) {
                    Session.this.disconnect("FAILED");
                } else {
                    Session.this.disconnect();
                }

            }

            public void onWebSocketError(String description) {
                Session.this.disconnect("FAILED");
            }
        });
        this.webSocketChannelClient.connect(this.sessionOptions.getUrlWsServer());
    }

    public Stream createStream(StreamOptions streamOptions) {
        Stream stream = new Stream(streamOptions, this);
        this.streams.put(stream.getId(), stream);
        return stream;
    }

    public Call createCall(com.flashphoner.fpwcsapi.session.CallOptions callOptions) {
        Call call = new Call(callOptions, this);
        this.calls.put(call.getId(), call);
        return call;
    }

    public void send(String message) {
        this.send(message, (Object)null);
    }

    public void send(String message, Object data) {
        if(this.webSocketChannelClient != null) {
            this.webSocketChannelClient.execute(message, data);
        }

    }

    public void disconnect() {
        this.disconnect("DISCONNECTED");
    }

    public void disconnect(String status) {
        this.connection.setStatus(status);
        if(this.webSocketChannelClient != null) {
            this.webSocketChannelClient.disconnect(false);
            this.webSocketChannelClient = null;
        }

        if(this.sessionEvent != null) {
            this.sessionEvent.onDisconnection(this.connection);
        }

        Iterator iterator = this.streams.entrySet().iterator();

        while(iterator.hasNext()) {
            ((Stream)((Entry)iterator.next()).getValue()).stop();
            iterator.remove();
        }

    }

    public String getId() {
        return this.id;
    }

    public com.flashphoner.fpwcsapi.session.SessionOptions getSessionOptions() {
        return this.sessionOptions;
    }

    public RestAppCommunicator getRestAppCommunicator() {
        return this.restAppCommunicator;
    }

    public void on(com.flashphoner.fpwcsapi.session.SessionEvent sessionEvent) {
        this.sessionEvent = sessionEvent;
    }

    public void on(IncomingCallEvent incomingCallEvent) {
        this.incomingCallEvent = incomingCallEvent;
    }

   public Map<String, SessionDescription> getSessionDescriptions() {
        return this.sessionDescriptions;
    }

    static {
        executor.requestStart();
    }
}

