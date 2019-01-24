package com.vexsoftware.votifier.net;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.vexsoftware.votifier.net.protocol.VoteInboundHandler;
import com.vexsoftware.votifier.net.protocol.VotifierGreetingHandler;
import com.vexsoftware.votifier.net.protocol.VotifierProtocolDifferentiator;
import com.vexsoftware.votifier.platform.VotifierPlugin;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Objects;
import java.util.function.Consumer;

public class VotifierServerBootstrap {
    private final String host;
    private final int port;
    private final NioEventLoopGroup bossLoopGroup;
    private final NioEventLoopGroup eventLoopGroup;
    private final VotifierPlugin plugin;
    private final boolean v1Disable;

    private Channel serverChannel;

    public VotifierServerBootstrap(String host, int port, VotifierPlugin plugin, boolean v1Disable) {
        this.host = host;
        this.port = port;
        this.plugin = plugin;
        this.v1Disable = v1Disable;
        this.bossLoopGroup = new NioEventLoopGroup(1, new ThreadFactoryBuilder().setDaemon(true).setNameFormat("Votifier NIO boss").build());
        this.eventLoopGroup = new NioEventLoopGroup(1, new ThreadFactoryBuilder().setDaemon(true).setNameFormat("Votifier NIO worker").build());
    }

    public void start(Consumer<Throwable> error) {
        Objects.requireNonNull(error, "error");
        new ServerBootstrap()
                .channel(NioServerSocketChannel.class)
                .group(bossLoopGroup, eventLoopGroup)
                .childHandler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel channel) {
                        channel.attr(VotifierSession.KEY).set(new VotifierSession());
                        channel.attr(VotifierPlugin.KEY).set(plugin);
                        channel.pipeline().addLast("greetingHandler", new VotifierGreetingHandler());
                        channel.pipeline().addLast("protocolDifferentiator", new VotifierProtocolDifferentiator(false, !v1Disable));
                        channel.pipeline().addLast("voteHandler", new VoteInboundHandler(plugin));
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

    public Bootstrap client() {
        return new Bootstrap()
                .channel(NioSocketChannel.class)
                .group(eventLoopGroup);
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
    }
}
