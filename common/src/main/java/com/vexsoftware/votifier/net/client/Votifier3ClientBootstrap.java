package com.vexsoftware.votifier.net.client;

import com.google.common.collect.Lists;
import com.vexsoftware.votifier.net.client.protocol.V3Hello;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.json.JsonObjectDecoder;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;

import javax.net.ssl.SSLException;
import java.net.SocketAddress;

public class Votifier3ClientBootstrap {

    private final SocketAddress socketAddress;

    public Votifier3ClientBootstrap(SocketAddress address) {
        this.socketAddress = address;
    }

    public ChannelFuture start() throws SSLException {
        final SslContext ctx = SslContextBuilder.forClient().build();

        return new Bootstrap().channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(Channel ch) throws Exception {
                        ch.pipeline().addLast(ctx.newHandler(ch.alloc()));
                        ch.pipeline().addLast(new JsonObjectDecoder());
                        ch.pipeline().addLast(new GsonCodec());
                        ch.pipeline().addLast("hello", new Votifier3Greeter(
                                hook,
                                new V3Hello(token, 1, Lists.newArrayList("nuvotifier.3")),
                                key -> "nuvotifier.1".equals(key) ? null : null
                                )
                        );
                    }
                }).connect(socketAddress);
    }
}
