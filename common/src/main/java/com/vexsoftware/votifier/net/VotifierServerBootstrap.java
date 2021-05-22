package com.vexsoftware.votifier.net;

import com.vexsoftware.votifier.net.protocol.VoteInboundHandler;
import com.vexsoftware.votifier.net.protocol.VotifierGreetingHandler;
import com.vexsoftware.votifier.net.protocol.VotifierProtocolDifferentiator;
import com.vexsoftware.votifier.platform.VotifierPlugin;
import com.vexsoftware.votifier.support.forwarding.cache.VoteCache;
import com.vexsoftware.votifier.support.forwarding.proxy.ProxyForwardingVoteSource;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.FastThreadLocalThread;
import io.netty.util.concurrent.GlobalEventExecutor;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class VotifierServerBootstrap {
    private static final boolean USE_EPOLL = Epoll.isAvailable();

    private final String host;
    private final int port;
    private final EventLoopGroup bossLoopGroup;
    private final EventLoopGroup eventLoopGroup;
    private final VotifierPlugin plugin;
    private final boolean v1Disable;

    private Channel serverChannel;

    public VotifierServerBootstrap(String host, int port, VotifierPlugin plugin, boolean v1Disable) {
        this.host = host;
        this.port = port;
        this.plugin = plugin;
        this.v1Disable = v1Disable;
        if (USE_EPOLL) {
            this.bossLoopGroup = new EpollEventLoopGroup(1, createThreadFactory("Votifier epoll boss"));
            this.eventLoopGroup = new EpollEventLoopGroup(3, createThreadFactory("Votifier epoll worker"));
            plugin.getPluginLogger().info("Using epoll transport to accept votes.");
        } else {
            this.bossLoopGroup = new NioEventLoopGroup(1, createThreadFactory("Votifier NIO boss"));
            this.eventLoopGroup = new NioEventLoopGroup(3, createThreadFactory("Votifier NIO worker"));
            plugin.getPluginLogger().info("Using NIO transport to accept votes.");
        }
    }

    private static ThreadFactory createThreadFactory(String name) {
        return runnable -> {
            FastThreadLocalThread thread = new FastThreadLocalThread(runnable, name);
            thread.setDaemon(true);
            return thread;
        };
    }

    public void start(Consumer<Throwable> error) {
        Objects.requireNonNull(error, "error");

        VoteInboundHandler voteInboundHandler = new VoteInboundHandler(plugin);

        new ServerBootstrap()
                .channel(USE_EPOLL ? EpollServerSocketChannel.class : NioServerSocketChannel.class)
                .group(bossLoopGroup, eventLoopGroup)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel channel) {
                        channel.attr(VotifierSession.KEY).set(new VotifierSession());
                        channel.attr(VotifierPlugin.KEY).set(plugin);
                        channel.pipeline().addLast("greetingHandler", VotifierGreetingHandler.INSTANCE);
                        channel.pipeline().addLast("protocolDifferentiator", new VotifierProtocolDifferentiator(false, !v1Disable));
                        channel.pipeline().addLast("voteHandler", voteInboundHandler);
                    }
                })
                .bind(host, port)
                .addListener((ChannelFutureListener) future -> {
                    if (future.isSuccess()) {
                        serverChannel = future.channel();
                        plugin.getPluginLogger().info("Votifier enabled on socket " + serverChannel.localAddress() + ".");
                        error.accept(null);
                    } else {
                        SocketAddress socketAddress = future.channel().localAddress();
                        if (socketAddress == null) {
                            socketAddress = new InetSocketAddress(host, port);
                        }
                        plugin.getPluginLogger().error("Votifier was not able to bind to " + socketAddress.toString(), future.cause());
                        error.accept(future.cause());
                    }
                });
    }

    private Bootstrap client() {
        return new Bootstrap()
                .channel(USE_EPOLL ? EpollSocketChannel.class : NioSocketChannel.class)
                .group(eventLoopGroup);
    }

    public ProxyForwardingVoteSource createForwardingSource(List<ProxyForwardingVoteSource.BackendServer> backendServers,
                                                            VoteCache voteCache) {
        return new ProxyForwardingVoteSource(plugin, this::client, backendServers, voteCache);
    }

    public void shutdown() {
        if (serverChannel != null) {
            try {
                serverChannel.close().syncUninterruptibly();
            } catch (Exception e) {
                plugin.getPluginLogger().error("Unable to shutdown server channel", e);
            }
        }
        eventLoopGroup.shutdownGracefully();
        bossLoopGroup.shutdownGracefully();

        try {
            bossLoopGroup.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
            eventLoopGroup.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
