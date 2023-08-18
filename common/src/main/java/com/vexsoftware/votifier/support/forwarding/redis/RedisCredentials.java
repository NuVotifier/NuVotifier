package com.vexsoftware.votifier.support.forwarding.redis;

/**
 * @author AkramL
 */
public class RedisCredentials {

    private final String host, password, channel;
    private final int port;

    private RedisCredentials(String host,
                             int port,
                             String password,
                             String channel) {
        this.host = host;
        this.port = port;
        this.password = password;
        this.channel = channel;
    }

    public String getHost() {
        return host;
    }

    public String getPassword() {
        return password;
    }

    public int getPort() {
        return port;
    }

    public String getChannel() {
        return channel;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private String host, password, channel;
        private int port;

        private Builder() {
        }

        public Builder host(String host) {
            this.host = host;
            return this;
        }

        public Builder port(int port) {
            this.port = port;
            return this;
        }

        public Builder password(String password) {
            this.password = password;
            return this;
        }

        public Builder channel(String channel) {
            this.channel = channel;
            return this;
        }

        public RedisCredentials build() {
            return new RedisCredentials(host, port, password, channel);
        }

    }

}
