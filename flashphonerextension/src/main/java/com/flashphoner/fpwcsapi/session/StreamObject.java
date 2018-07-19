package com.flashphoner.fpwcsapi.session;

/**
 * Created by TakeLeap05 on 12-07-2018.
 */

import com.flashphoner.fpwcsapi.bean.MediaProvider;
import com.flashphoner.fpwcsapi.bean.StreamStatus;

public class StreamObject extends AbstractStreamOptions {
    private String mediaSessionId;
    private String remoteMediaElementId;
    private Boolean published = Boolean.valueOf(false);
    private Boolean hasVideo = Boolean.valueOf(false);
    private Boolean hasAudio = Boolean.valueOf(true);
    private StreamStatus status;
    private String sdp;
    private String info;
    private String recordName;
    private int width;
    private int height;
    private int bitrate;
    private int quality;
    private MediaProvider mediaProvider;

    public StreamObject(StreamOptions streamOptions) {
        super(streamOptions.getName());
        this.status = StreamStatus.NEW;
        this.mediaProvider = MediaProvider.WebRTC;
        this.setRecord(streamOptions.isRecord());
        this.setHasAudio(Boolean.valueOf(streamOptions.getConstraints().getAudioConstraints() != null));
        this.setHasVideo(streamOptions.getConstraints().getVideoConstraints() != null);
        this.getCustom().putAll(streamOptions.getCustom());
    }

    public MediaProvider getMediaProvider() {
        return this.mediaProvider;
    }

    protected void setMediaProvider(MediaProvider mediaProvider) {
        this.mediaProvider = mediaProvider;
    }

    public String getMediaSessionId() {
        return this.mediaSessionId;
    }

    public void setMediaSessionId(String mediaSessionId) {
        this.mediaSessionId = mediaSessionId;
    }

    public boolean isPublished() {
        return this.published.booleanValue();
    }

    public void setPublished(boolean published) {
        this.published = Boolean.valueOf(published);
    }

    public boolean isHasVideo() {
        return this.hasVideo.booleanValue();
    }

    public void setHasVideo(boolean hasVideo) {
        this.hasVideo = Boolean.valueOf(hasVideo);
    }

    public StreamStatus getStatus() {
        return this.status;
    }

    public void setStatus(StreamStatus status) {
        this.status = status;
    }

    public String getSdp() {
        return this.sdp;
    }

    public void setSdp(String sdp) {
        this.sdp = sdp;
    }

    public String getInfo() {
        return this.info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public void setRecordName(String recordName) {
        this.recordName = recordName;
    }

    public String getRecordName() {
        return this.recordName;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getWidth() {
        return this.width;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getHeight() {
        return this.height;
    }

    public int getBitrate() {
        return this.bitrate;
    }

    public void setBitrate(int bitrate) {
        this.bitrate = bitrate;
    }

    public int getQuality() {
        return this.quality;
    }

    public void setQuality(int quality) {
        this.quality = quality;
    }

    public boolean equals(Object o) {
        if(this == o) {
            return true;
        } else if(o != null && this.getClass() == o.getClass()) {
            StreamObject stream = (StreamObject)o;
            if(this.published != stream.published) {
                return false;
            } else {
                if(this.mediaSessionId != null) {
                    if(!this.mediaSessionId.equals(stream.mediaSessionId)) {
                        return false;
                    }
                } else if(stream.mediaSessionId != null) {
                    return false;
                }

                if(this.getName() != null) {
                    if(!this.getName().equals(stream.getName())) {
                        return false;
                    }
                } else if(stream.getName() != null) {
                    return false;
                }

                return true;
            }
        } else {
            return false;
        }
    }

    public int hashCode() {
        int result = this.mediaSessionId != null?this.mediaSessionId.hashCode():0;
        result = 31 * result + (this.getName() != null?this.getName().hashCode():0);
        result = 31 * result + (this.published.booleanValue()?1:0);
        return result;
    }

    public String toString() {
        return "Stream{mediaSessionId='" + this.mediaSessionId + '\'' + "name='" + this.getName() + '\'' + ", status='" + this.status + '\'' + ", sdp='" + this.sdp + '\'' + ", remoteMediaElementId='" + this.remoteMediaElementId + '\'' + ", hasVideo='" + this.hasVideo + '\'' + ", hasAudio='" + this.hasAudio + '\'' + '}' + super.toString();
    }

    public String getRemoteMediaElementId() {
        return this.remoteMediaElementId;
    }

    public void setRemoteMediaElementId(String remoteMediaElementId) {
        this.remoteMediaElementId = remoteMediaElementId;
    }

    public Boolean isHasAudio() {
        return this.hasAudio;
    }

    public void setHasAudio(Boolean hasAudio) {
        this.hasAudio = hasAudio;
    }
}
