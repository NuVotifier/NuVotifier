package com.vexsoftware.votifier.bungee.forwarding.proxy.client;

import com.vexsoftware.votifier.model.Vote;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.CorruptedFrameException;

public class VotifierProtocol2HandshakeHandler extends SimpleChannelInboundHandler<String> {
    private final Vote toSend;
    private final VotifierResponseHandler responseHandler;

    public VotifierProtocol2HandshakeHandler(Vote toSend, VotifierResponseHandler responseHandler) {
        this.toSend = toSend;
        this.responseHandler = responseHandler;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String s) throws Exception {
        String[] handshakeContents = s.split(" ");
        if (handshakeContents.length != 3) {
            throw new CorruptedFrameException("Handshake is not valid.");
        }

        VoteRequest request = new VoteRequest(handshakeContents[2], toSend);
        System.out.println(request);
        ctx.writeAndFlush(request);
        ctx.pipeline().addLast(new VotifierProtocol2ResponseHandler(responseHandler));
        ctx.pipeline().remove(this);
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        responseHandler.onFailure(cause);
        ctx.close();
    }
}
