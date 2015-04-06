package com.vexsoftware.votifier.net.protocol;

import com.vexsoftware.votifier.Votifier;
import com.vexsoftware.votifier.net.VotifierSession;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.nio.charset.StandardCharsets;

/**
 * Handles the Votifier greeting.
 */
public class VotifierGreetingHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        /* Send the version string and challenge. */
        VotifierSession session = ctx.channel().attr(VotifierSession.KEY).get();
        String version = "VOTIFIER " + Votifier.getInstance().getVersion() + " " + session.getChallenge() + "\n";
        ctx.writeAndFlush(Unpooled.copiedBuffer(version, StandardCharsets.UTF_8));
    }
}
