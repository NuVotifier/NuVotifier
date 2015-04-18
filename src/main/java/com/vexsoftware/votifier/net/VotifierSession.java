package com.vexsoftware.votifier.net;

import io.netty.util.AttributeKey;

import static com.vexsoftware.votifier.TokenUtil.newToken;

public class VotifierSession {
    public static final AttributeKey<VotifierSession> KEY = AttributeKey.valueOf("votifier_session");
    private ProtocolVersion version = ProtocolVersion.UNKNOWN;
    private final String challenge;

    public VotifierSession() {
        challenge = newToken();
    }

    public void setVersion(ProtocolVersion version) {
        if (this.version != ProtocolVersion.UNKNOWN)
            throw new IllegalStateException("Protocol version already switched");

        this.version = version;
    }

    public ProtocolVersion getVersion() {
        return version;
    }

    public String getChallenge() {
        return challenge;
    }

    public enum ProtocolVersion {
        UNKNOWN,
        ONE,
        TWO
    }
}
