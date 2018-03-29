package com.vexsoftware.votifier.net;

import com.vexsoftware.votifier.util.TokenUtil;
import io.netty.util.AttributeKey;

public class VotifierSession {
    public static final AttributeKey<VotifierSession> KEY = AttributeKey.valueOf("votifier_session");
    private ProtocolVersion version = ProtocolVersion.UNKNOWN;
    private final String challenge;
    private boolean hasCompletedVote = false;

    public VotifierSession() {
        challenge = TokenUtil.newToken();
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

    public void completeVote() {
        if (hasCompletedVote)
            throw new IllegalStateException("Protocol completed vote twice!");

        hasCompletedVote = true;
    }

    public boolean hasCompletedVote() {
        return hasCompletedVote;
    }

    public enum ProtocolVersion {
        UNKNOWN,
        ONE,
        TWO
    }
}
