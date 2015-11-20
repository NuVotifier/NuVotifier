package com.vexsoftware.votifier.bungee.forwarding.proxy.client;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.json.JSONObject;

public class VotifierProtocol2ResponseHandler extends SimpleChannelInboundHandler<String> {
    private final VotifierResponseHandler responseHandler;

    public VotifierProtocol2ResponseHandler(VotifierResponseHandler responseHandler) {
        this.responseHandler = responseHandler;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
        JSONObject object = new JSONObject(msg);
        String status = object.getString("status");
        if (status.equals("ok")) {
            responseHandler.onSuccess();
        } else {
            responseHandler.onFailure(new Exception("Remote server error: " + object.getString("cause") + ": " + object.getString("error")));
        }
        ctx.close();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        responseHandler.onFailure(cause);
        ctx.close();
    }
}
