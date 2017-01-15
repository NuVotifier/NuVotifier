package com.vexsoftware.votifier.net.protocol;

import com.vexsoftware.votifier.VoteHandler;
import com.vexsoftware.votifier.model.Vote;
import com.vexsoftware.votifier.net.VotifierSession;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.json.JSONObject;

public class VoteInboundHandler extends SimpleChannelInboundHandler<Vote> {
    private final VoteHandler handler;

    public VoteInboundHandler(VoteHandler handler) {
        this.handler = handler;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, final Vote vote) throws Exception {
        VotifierSession session = ctx.channel().attr(VotifierSession.KEY).get();

        handler.onVoteReceived(ctx.channel(), vote, session.getVersion());

        if (session.getVersion() == VotifierSession.ProtocolVersion.ONE) {
            ctx.close();
        } else {
            JSONObject object = new JSONObject();
            object.put("status", "ok");
            ctx.writeAndFlush(object.toString() + "\r\n").addListener(ChannelFutureListener.CLOSE);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        VotifierSession session = ctx.channel().attr(VotifierSession.KEY).get();

        handler.onError(ctx.channel(), cause);

        if (session.getVersion() == VotifierSession.ProtocolVersion.TWO) {
            JSONObject object = new JSONObject();
            object.put("status", "error");
            object.put("cause", cause.getClass().getSimpleName());
            object.put("error", cause.getMessage());
            ctx.writeAndFlush(object.toString() + "\r\n").addListener(ChannelFutureListener.CLOSE);
        } else {
            ctx.close();
        }
    }
}
