package com.leo.ipcsocket.server.entity;

import android.os.Parcel;
import android.os.Parcelable;

import com.leo.ipcsocket.util.SocketParams;

public class ServerConfig implements Parcelable {
    /**
     * 消息有效时长，发送指定包名信息时生效，如遇应用未绑定，会加入缓存
     */
    public int msgEffectiveSecond;
    /**
     * 最大缓存消息数量
     */
    public int maxCacheMsgCount;
    /**
     * 端口号
     */
    public int port;

    private ServerConfig() {
    }

    public static class Builder {
        int msgEffectiveSecond = 10;
        int maxCacheMsgCount = 10;
        int port = SocketParams.DEFAULT_PORT;

        public Builder setMsgEffectiveSecond(int msgEffectiveSecond) {
            this.msgEffectiveSecond = msgEffectiveSecond;
            return this;
        }

        public Builder setMaxCacheMsgCount(int maxCacheMsgCount) {
            this.maxCacheMsgCount = maxCacheMsgCount;
            return this;
        }

        public Builder setPort(int port) {
            this.port = port;
            return this;
        }

        public ServerConfig build() {
            ServerConfig config = new ServerConfig();
            config.msgEffectiveSecond = msgEffectiveSecond;
            config.maxCacheMsgCount = maxCacheMsgCount;
            config.port = port;
            return config;
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.msgEffectiveSecond);
        dest.writeInt(this.maxCacheMsgCount);
        dest.writeInt(this.port);
    }

    public void readFromParcel(Parcel source) {
        this.msgEffectiveSecond = source.readInt();
        this.maxCacheMsgCount = source.readInt();
        this.port = source.readInt();
    }

    protected ServerConfig(Parcel in) {
        this.msgEffectiveSecond = in.readInt();
        this.maxCacheMsgCount = in.readInt();
        this.port = in.readInt();
    }

    public static final Parcelable.Creator<ServerConfig> CREATOR = new Parcelable.Creator<ServerConfig>() {
        @Override
        public ServerConfig createFromParcel(Parcel source) {
            return new ServerConfig(source);
        }

        @Override
        public ServerConfig[] newArray(int size) {
            return new ServerConfig[size];
        }
    };
}
