package com.vexsoftware.votifier.bungee.forwarding.proxy.client;

import com.vexsoftware.votifier.bungee.NuVotifier;
import com.vexsoftware.votifier.model.Vote;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.CorruptedFrameException;

public class VotifierProtocol2HandshakeHandler extends SimpleChannelInboundHandler<String> {
    private final Vote toSend;
    private final VotifierResponseHandler responseHandler;
    private final NuVotifier nuVotifier;

    public VotifierProtocol2HandshakeHandler(Vote toSend, VotifierResponseHandler responseHandler, NuVotifier nuVotifier) {
        this.toSend = toSend;
        this.responseHandler = responseHandler;
        this.nuVotifier = nuVotifier;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String s) {
        String[] handshakeContents = s.split(" ");
        if (handshakeContents.length != 3) {
            throw new CorruptedFrameException("Handshake is not valid.");
        }

        VoteRequest request = new VoteRequest(handshakeContents[2], toSend);
        if (nuVotifier.isDebug()) {
            nuVotifier.getLogger().info("Sent request: " + request.toString());
        }
        ctx.writeAndFlush(request);
        ctx.pipeline().addLast(new VotifierProtocol2ResponseHandler(responseHandler));
        ctx.pipeline().remove(this);
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        responseHandler.onFailure(cause);
        ctx.close();
    }
}
