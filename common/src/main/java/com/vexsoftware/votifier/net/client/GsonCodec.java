package com.vexsoftware.votifier.net.client;

import com.google.gson.JsonObject;
import com.vexsoftware.votifier.util.GsonInst;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageCodec;

import java.nio.charset.StandardCharsets;
import java.util.List;

public class GsonCodec extends ByteToMessageCodec<JsonObject> {
    @Override
    protected void encode(ChannelHandlerContext ctx, JsonObject msg, ByteBuf out) {
        String s = GsonInst.gson.toJson(msg);
        out.writeBytes(s.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        byte[] b = new byte[in.readableBytes()];
        in.readBytes(b);
        out.add(GsonInst.gson.fromJson(new String(b, StandardCharsets.UTF_8), JsonObject.class));
    }
}
