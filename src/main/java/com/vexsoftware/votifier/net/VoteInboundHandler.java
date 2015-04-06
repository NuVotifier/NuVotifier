package com.vexsoftware.votifier.net;

import com.vexsoftware.votifier.Votifier;
import com.vexsoftware.votifier.model.Vote;
import com.vexsoftware.votifier.model.VoteListener;
import com.vexsoftware.votifier.model.VotifierEvent;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.bukkit.Bukkit;
import org.json.JSONObject;

import java.util.logging.Level;

public class VoteInboundHandler extends SimpleChannelInboundHandler<Vote> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, final Vote vote) throws Exception {
        // Fire a synchronous task and close the connection.
        Votifier.getInstance().getServer().getScheduler().scheduleSyncDelayedTask(Votifier.getInstance(), new Runnable() {
            @Override
            public void run() {
                for (VoteListener listener : Votifier.getInstance().getListeners()) {
                    try {
                        listener.voteMade(vote);
                    } catch (Exception ex) {
                        String vlName = listener.getClass().getSimpleName();
                        Votifier.getInstance().getLogger().log(Level.WARNING,
                                "Exception caught while sending the vote notification to the '"
                                        + vlName + "' listener", ex);
                    }
                }
                Bukkit.getServer().getPluginManager().callEvent(new VotifierEvent(vote));
            }
        });

        VotifierSession session = ctx.channel().attr(VotifierSession.KEY).get();

        if (session.getVersion() == VotifierSession.ProtocolVersion.ONE) {
            ctx.close();
        } else {
            JSONObject object = new JSONObject();
            object.put("status", "ok");
            ctx.writeAndFlush(object.toString()).addListener(ChannelFutureListener.CLOSE);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        VotifierSession session = ctx.channel().attr(VotifierSession.KEY).get();

        Votifier.getInstance().getLogger().log(Level.SEVERE, "Exception while processing vote from " + ctx.channel().remoteAddress(), cause);

        if (session.getVersion() == VotifierSession.ProtocolVersion.TWO) {
            JSONObject object = new JSONObject();
            object.put("status", "error");
            object.put("cause", cause.getClass().getSimpleName());
            object.put("error", cause.getMessage());
            ctx.writeAndFlush(object.toString()).addListener(ChannelFutureListener.CLOSE);
        } else {
            ctx.close();
        }
    }
}
