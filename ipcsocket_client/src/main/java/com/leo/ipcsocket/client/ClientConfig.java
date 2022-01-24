package com.leo.ipcsocket.client;

import com.leo.ipcsocket.util.SocketParams;

public class ClientConfig {
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

    private ClientConfig() {
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

        public ClientConfig build() {
            ClientConfig config = new ClientConfig();
            config.msgEffectiveSecond = msgEffectiveSecond;
            config.maxCacheMsgCount = maxCacheMsgCount;
            config.port = port;
            return config;
        }
    }
}
