package io.ibj.nuvotifier.legacyserver;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.ibj.nuvotifier.legacyserver.protocol.VoteInboundHandler;
import io.ibj.nuvotifier.legacyserver.protocol.VotifierGreetingHandler;
import io.ibj.nuvotifier.legacyserver.protocol.VotifierProtocolDifferentiator;
import io.ibj.nuvotifier.platform.LoggingAdapter;
import io.ibj.nuvotifier.platform.V1KeyProvider;
import io.ibj.nuvotifier.platform.V2TokenProvider;
import io.ibj.nuvotifier.platform.VoteHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOutboundInvoker;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.ImmediateEventExecutor;
import io.netty.util.concurrent.PromiseCombiner;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

public class VotifierServer implements AutoCloseable {
    private NioEventLoopGroup bossLoopGroup;
    private NioEventLoopGroup eventLoopGroup;

    private Set<Channel> serverChannels;

    private final List<InetSocketAddress> bindAddresses;
    private final V1KeyProvider v1KeyProvider;
    private final V2TokenProvider v2TokenProvider;
    private final LoggingAdapter loggingAdapter;
    private final boolean testMode;
    private final VoteHandler voteHandler;

    VotifierServer(VotifierServerBuilder builder) {
        this.bindAddresses = builder.bindAddresses;
        this.v1KeyProvider = builder.v1KeyProvider;
        this.v2TokenProvider = builder.v2TokenProvider;
        this.loggingAdapter = builder.loggingAdapter;
        this.testMode = builder.testMode;
        this.voteHandler = builder.voteHandler;
    }

    public void start(Consumer<Throwable> error) {
        this.bossLoopGroup = new NioEventLoopGroup(1, new ThreadFactoryBuilder().setDaemon(true).setNameFormat("Votifier NIO boss").build());
        this.eventLoopGroup = new NioEventLoopGroup(1, new ThreadFactoryBuilder().setDaemon(true).setNameFormat("Votifier NIO worker").build());

        Objects.requireNonNull(error, "error");
        ServerBootstrap bootstrap = new ServerBootstrap()
                .channel(NioServerSocketChannel.class)
                .group(bossLoopGroup, eventLoopGroup)
                .childHandler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel channel) {
                        channel.attr(VotifierSession.KEY).set(new VotifierSession());
                        channel.pipeline().addLast("greetingHandler", new VotifierGreetingHandler());
                        channel.pipeline().addLast("protocolDifferentiator", new VotifierProtocolDifferentiator(v1KeyProvider, v2TokenProvider, testMode));
                        channel.pipeline().addLast("voteHandler", new VoteInboundHandler(voteHandler));
                    }
                });

        ChannelFutureListener listener = future -> {
            Channel serverChannel = future.channel();
            if (future.isSuccess()) {
                serverChannels.add(serverChannel);
                loggingAdapter.info("Votifier enabled on socket " + serverChannel.localAddress() + ".");
                error.accept(null);
            } else {
                loggingAdapter.error("Votifier was not able to bind to " + serverChannel.localAddress().toString(), future.cause());
                error.accept(future.cause());
            }
        };

        for (InetSocketAddress socketAddress : this.bindAddresses) {
            bootstrap.bind(socketAddress).addListener(listener);
        }
    }

    @Override
    public void close() throws Exception {
        if (!serverChannels.isEmpty()) {
            PromiseCombiner closePromises = new PromiseCombiner();
            serverChannels.stream().map(ChannelOutboundInvoker::close).forEach(closePromises::add);
            DefaultPromise<Void> closesDone = new DefaultPromise<>(ImmediateEventExecutor.INSTANCE);
            closePromises.finish(closesDone);
            closesDone.awaitUninterruptibly();
        }

        eventLoopGroup.shutdownGracefully();
        bossLoopGroup.shutdownGracefully();
    }
}
