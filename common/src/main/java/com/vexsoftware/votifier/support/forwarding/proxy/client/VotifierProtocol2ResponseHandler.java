package com.vexsoftware.votifier.support.forwarding.proxy.client;

import com.google.gson.JsonObject;
import com.vexsoftware.votifier.util.GsonInst;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class VotifierProtocol2ResponseHandler extends SimpleChannelInboundHandler<String> {
    private final VotifierResponseHandler responseHandler;

    public VotifierProtocol2ResponseHandler(VotifierResponseHandler responseHandler) {
        this.responseHandler = responseHandler;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) {
        JsonObject object = GsonInst.gson.fromJson(msg, JsonObject.class);
        String status = object.get("status").getAsString();
        if (status.equals("ok")) {
            responseHandler.onSuccess();
        } else {
            responseHandler.onFailure(new Exception("Remote server error: " + object.get("cause").getAsString() +
                    ": " + object.get("error").getAsString()));
        }
        ctx.close();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        responseHandler.onFailure(cause);
        ctx.close();
    }
}
