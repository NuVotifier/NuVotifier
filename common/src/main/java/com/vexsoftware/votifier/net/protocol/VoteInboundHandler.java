package com.vexsoftware.votifier.net.protocol;

import com.google.gson.JsonObject;
import com.vexsoftware.votifier.VoteHandler;
import com.vexsoftware.votifier.model.Vote;
import com.vexsoftware.votifier.net.VotifierSession;
import com.vexsoftware.votifier.util.GsonInst;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.util.concurrent.atomic.AtomicLong;

@ChannelHandler.Sharable
public class VoteInboundHandler extends SimpleChannelInboundHandler<Vote> {
    private final VoteHandler handler;
    private final AtomicLong lastError;
    private final AtomicLong errorsSent;

    public VoteInboundHandler(VoteHandler handler) {
        this.handler = handler;
        this.lastError = new AtomicLong();
        this.errorsSent = new AtomicLong();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, final Vote vote) throws Exception {
        VotifierSession session = ctx.channel().attr(VotifierSession.KEY).get();

        handler.onVoteReceived(vote, session.getVersion(), ctx.channel().remoteAddress().toString());
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

        String remoteAddr = ctx.channel().remoteAddress().toString();
        boolean hasCompletedVote = session.hasCompletedVote();

        if (session.getVersion() == VotifierSession.ProtocolVersion.TWO) {
            JsonObject object = new JsonObject();
            object.addProperty("status", "error");
            object.addProperty("cause", cause.getClass().getSimpleName());
            object.addProperty("error", cause.getMessage());
            ctx.writeAndFlush(GsonInst.gson.toJson(object) + "\r\n").addListener(ChannelFutureListener.CLOSE);
        } else {
            ctx.close();
        }

        if (!willThrottleErrorLogging()) {
            handler.onError(cause, hasCompletedVote, remoteAddr);
        }
    }

    private boolean willThrottleErrorLogging() {
        long lastErrorAt = this.lastError.get();
        long now = System.currentTimeMillis();

        if (lastErrorAt + 2000 >= now) {
            return this.errorsSent.incrementAndGet() >= 5;
        } else {
            this.lastError.set(now);
            this.errorsSent.set(0);
            return false;
        }
    }
}
