package com.vexsoftware.votifier.net.protocol;

import com.vexsoftware.votifier.net.VotifierSession;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.nio.charset.StandardCharsets;

/**
 * Handles the Votifier greeting.
 */
public class VotifierGreetingHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        VotifierSession session = ctx.channel().attr(VotifierSession.KEY).get();
        String version = "VOTIFIER 2 " + session.getChallenge() + "\n";
        ByteBuf versionBuf = Unpooled.copiedBuffer(version, StandardCharsets.UTF_8);
        ctx.writeAndFlush(versionBuf);
    }
}
