package com.vexsoftware.votifier.util.standalone;

import com.vexsoftware.votifier.VoteHandler;
import com.vexsoftware.votifier.VotifierPlugin;
import com.vexsoftware.votifier.model.Vote;
import com.vexsoftware.votifier.net.VotifierSession;
import com.vexsoftware.votifier.net.protocol.VoteInboundHandler;
import com.vexsoftware.votifier.net.protocol.VotifierGreetingHandler;
import com.vexsoftware.votifier.net.protocol.VotifierProtocolDifferentiator;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.net.InetSocketAddress;
import java.security.Key;
import java.security.KeyPair;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class StandaloneVotifierPlugin implements VotifierPlugin, VoteHandler {
    private final Map<String, Key> tokens;
    private final VoteReceiver receiver;
    private final KeyPair v1Key;
    private final InetSocketAddress bind;
    private NioEventLoopGroup group;

    public StandaloneVotifierPlugin(Map<String, Key> tokens, VoteReceiver receiver, KeyPair v1Key, InetSocketAddress bind) {
        this.receiver = receiver;
        this.bind = bind;
        this.tokens = Collections.unmodifiableMap(new HashMap<>(tokens));
        this.v1Key = v1Key;
    }

    public Throwable start() throws Throwable {
        group = new NioEventLoopGroup(2);

        ChannelFuture future = new ServerBootstrap()
                .channel(NioServerSocketChannel.class)
                .group(group)
                .childHandler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel channel) throws Exception {
                        channel.attr(VotifierSession.KEY).set(new VotifierSession());
                        channel.attr(VotifierPlugin.KEY).set(StandaloneVotifierPlugin.this);
                        channel.pipeline().addLast("greetingHandler", new VotifierGreetingHandler());
                        channel.pipeline().addLast("protocolDifferentiator", new VotifierProtocolDifferentiator(false, v1Key != null));
                        channel.pipeline().addLast("voteHandler", new VoteInboundHandler(StandaloneVotifierPlugin.this));
                    }
                })
                .bind(bind)
                .syncUninterruptibly();

        if (!future.isSuccess()) {
            return future.cause();
        } else {
            return null;
        }
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
    public void onVoteReceived(Channel channel, Vote vote, VotifierSession.ProtocolVersion protocolVersion) throws Exception {
        receiver.onVote(vote);
    }

    @Override
    public void onError(Channel channel, Throwable throwable) {
        receiver.onException(throwable);
    }
}
