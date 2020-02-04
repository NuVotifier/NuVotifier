package io.ibj.nuvotifier.legacyserver.protocol;

import com.google.gson.JsonObject;
import io.ibj.nuvotifier.legacyserver.GsonInst;
import io.ibj.nuvotifier.legacyserver.VotifierSession;
import io.ibj.nuvotifier.model.NuVote;
import io.ibj.nuvotifier.platform.VoteHandler;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class VoteInboundHandler extends SimpleChannelInboundHandler<NuVote> {
    private final VoteHandler handler;

    public VoteInboundHandler(VoteHandler handler) {
        this.handler = handler;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, final NuVote vote) throws Exception {
        VotifierSession session = ctx.channel().attr(VotifierSession.KEY).get();

        handler.onVote(vote);
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

        handler.onError(cause, session.hasCompletedVote(), ctx.channel().remoteAddress().toString());

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
