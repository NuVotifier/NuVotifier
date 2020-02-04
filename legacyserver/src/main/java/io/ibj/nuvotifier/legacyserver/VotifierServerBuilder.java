package io.ibj.nuvotifier.legacyserver;

import io.ibj.nuvotifier.platform.LoggingAdapter;
import io.ibj.nuvotifier.platform.V1KeyProvider;
import io.ibj.nuvotifier.platform.V2TokenProvider;
import io.ibj.nuvotifier.platform.VoteHandler;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

public class VotifierServerBuilder {
    // Which addresses to bind to? We support multiple now!
    List<InetSocketAddress> bindAddresses = new ArrayList<>();

    // Provides the server the ability to decode v1 keys. Defaults to
    V1KeyProvider v1KeyProvider = null;

    // Provides the server the ability to validate v2 token payloads
    V2TokenProvider v2TokenProvider = null;

    // Logging adapter which will be used to communicate various logs
    LoggingAdapter loggingAdapter = null;

    // Whether not the server is in 'test mode'. Spoiler: unless you are testing, its not.
    boolean testMode = false;

    // Handler to invoke when a vote is sent.
    VoteHandler voteHandler = null;

    public VotifierServerBuilder addBindAddress(InetSocketAddress bindAddress) {
        this.bindAddresses.add(bindAddress);
        return this;
    }

    public VotifierServerBuilder setV1KeyProvider(V1KeyProvider v1KeyProvider) {
        this.v1KeyProvider = v1KeyProvider;
        return this;
    }

    public VotifierServerBuilder setV2TokenProvider(V2TokenProvider v2TokenProvider) {
        this.v2TokenProvider = v2TokenProvider;
        return this;
    }

    public VotifierServerBuilder setLoggingAdapter(LoggingAdapter loggingAdapter) {
        this.loggingAdapter = loggingAdapter;
        return this;
    }

    public VotifierServerBuilder setTestMode(boolean testMode) {
        this.testMode = testMode;
        return this;
    }

    public VotifierServerBuilder setVoteHandler(VoteHandler voteHandler) {
        this.voteHandler = voteHandler;
        return this;
    }


    public VotifierServer build() {
        return new VotifierServer(this);
    }
}
