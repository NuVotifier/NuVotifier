package com.vexsoftware.votifier.net.client;

import com.google.gson.JsonObject;
import com.vexsoftware.votifier.net.client.protocol.V3Hello;
import com.vexsoftware.votifier.net.client.protocol.V3HelloBack;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.util.List;
import java.util.function.Function;

public class Votifier3Greeter extends ChannelInboundHandlerAdapter {
    private final V3Hello hello;
    private final Function<String, ChannelInboundHandlerAdapter> capabilityFactory;

    public Votifier3Greeter(V3Hello hello,
                            Function<String, ChannelInboundHandlerAdapter> capabilityFactory) {
        this.hello = hello;
        this.capabilityFactory = capabilityFactory;
    }

    public void channelActive(ChannelHandlerContext ctx) {
        ctx.writeAndFlush(hello.serialize());
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        // At this stage, we can only accept a 'helloback' response
        JsonObject o = ((JsonObject) msg);
        if (!o.has("type"))
            throw new IllegalStateException("Server responded with a message without type field");

        String oType = o.get("type").getAsString();

        if (!"helloback".equals(oType))
            throw new IllegalStateException("Server responded with type '" + oType + "' but helloback was expected");

        V3HelloBack packet = V3HelloBack.decode(o);
        boolean hasCap = false;
        List<String> caps = packet.getAcceptedCapabilities();
        for (String s : hello.getCapabilities()) {
            if (caps.contains(s)) {
                ChannelInboundHandlerAdapter a = this.capabilityFactory.apply(s);
                ctx.pipeline().addLast(a);
                hasCap = true;
            }
        }

        if (hasCap)
            ctx.pipeline().remove("hello");
        else
            ctx.close();
    }
}
