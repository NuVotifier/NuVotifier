package com.vexsoftware.votifier.net.protocol;

import com.google.gson.JsonObject;
import com.vexsoftware.votifier.VoteHandler;
import com.vexsoftware.votifier.model.Vote;
import com.vexsoftware.votifier.net.VotifierSession;
import com.vexsoftware.votifier.util.GsonInst;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class VoteInboundHandler extends SimpleChannelInboundHandler<Vote> {
    private final VoteHandler handler;

    public VoteInboundHandler(VoteHandler handler) {
        this.handler = handler;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, final Vote vote) throws Exception {
        VotifierSession session = ctx.channel().attr(VotifierSession.KEY).get();

        handler.onVoteReceived(ctx.channel(), vote, session.getVersion());
        session.completeVote();

        if (session.getVersion() == VotifierSession.ProtocolVersion.ONE) {
            ctx.close();
        } else {
            JsonObject object = new JsonObject();
            object.addProperty("status", "ok");
            ctx.writeAndFlush(GsonInst.gson.toJson(object) + "\r\n").addListener(ChannelFutureListener.CLOSE);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        VotifierSession session = ctx.channel().attr(VotifierSession.KEY).get();

        handler.onError(ctx.channel(), session.hasCompletedVote(), cause);

        if (session.getVersion() == VotifierSession.ProtocolVersion.TWO) {
            JsonObject object = new JsonObject();
            object.addProperty("status", "error");
            object.addProperty("cause", cause.getClass().getSimpleName());
            object.addProperty("error", cause.getMessage());
            ctx.writeAndFlush(GsonInst.gson.toJson(object) + "\r\n").addListener(ChannelFutureListener.CLOSE);
        } else {
            ctx.close();
        }
    }
}
