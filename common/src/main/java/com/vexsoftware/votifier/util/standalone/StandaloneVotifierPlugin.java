package com.vexsoftware.votifier.util.standalone;

import com.vexsoftware.votifier.VoteHandler;
import com.vexsoftware.votifier.net.VotifierServerBootstrap;
import com.vexsoftware.votifier.platform.VotifierPlugin;
import com.vexsoftware.votifier.model.Vote;
import com.vexsoftware.votifier.net.VotifierSession;
import com.vexsoftware.votifier.platform.scheduler.ScheduledExecutorServiceVotifierScheduler;
import com.vexsoftware.votifier.platform.scheduler.VotifierScheduler;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.security.Key;
import java.security.KeyPair;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class StandaloneVotifierPlugin implements VotifierPlugin {
    private final Map<String, Key> tokens;
    private final VoteReceiver receiver;
    private final KeyPair v1Key;
    private final InetSocketAddress bind;
    private final VotifierScheduler scheduler;
    private VotifierServerBootstrap bootstrap;

    public StandaloneVotifierPlugin(Map<String, Key> tokens, VoteReceiver receiver, KeyPair v1Key, InetSocketAddress bind) {
        this.receiver = receiver;
        this.bind = bind;
        this.tokens = Collections.unmodifiableMap(new HashMap<>(tokens));
        this.v1Key = v1Key;
        this.scheduler = new ScheduledExecutorServiceVotifierScheduler(Executors.newScheduledThreadPool(1));
    }

    public void start(Consumer<Throwable> error) {
        if (bootstrap != null) {
            bootstrap.shutdown();
        }
        this.bootstrap = new VotifierServerBootstrap(bind.getHostString(), bind.getPort(), this, v1Key == null);
        this.bootstrap.start(error);
    }

    @Override
    public Map<String, Key> getTokens() {
        return tokens;
    }

    @Override
    public KeyPair getProtocolV1Key() {
        return v1Key;
    }

    @Override
    public String getVersion() {
        return "2.3.5";
    }

    @Override
    public Logger getPluginLogger() {
        return LoggerFactory.getLogger(StandaloneVotifierPlugin.class);
    }

    @Override
    public VotifierScheduler getScheduler() {
        return scheduler;
    }

    @Override
    public void onVoteReceived(Channel channel, Vote vote, VotifierSession.ProtocolVersion protocolVersion) throws Exception {
        receiver.onVote(vote);
    }

    @Override
    public void onError(Channel channel, boolean alreadyHandledVote, Throwable throwable) {
        receiver.onException(throwable);
    }
}
