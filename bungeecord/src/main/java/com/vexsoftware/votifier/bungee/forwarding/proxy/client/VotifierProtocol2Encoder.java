package com.vexsoftware.votifier.bungee.forwarding.proxy.client;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.json.JSONObject;

import javax.crypto.Mac;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Base64;

public class VotifierProtocol2Encoder extends MessageToByteEncoder<VoteRequest> {
    private static final short MAGIC = 0x733A;

    private final Key key;

    public VotifierProtocol2Encoder(Key key) {
        this.key = key;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, VoteRequest req, ByteBuf buf) throws Exception {
        JSONObject object = new JSONObject();
        JSONObject payloadObject = req.getVote().serialize();
        payloadObject.put("challenge", req.getChallenge());
        String payload = payloadObject.toString();
        object.put("payload", payload);

        // Generate the MAC
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(key);
        mac.update(payload.getBytes(StandardCharsets.UTF_8));
        String computed = Base64.getEncoder().encodeToString(mac.doFinal());
        object.put("signature", computed);

        // JSON message is ready for encoding.
        String finalMessage = object.toString();
        buf.writeShort(MAGIC);
        buf.writeShort(finalMessage.length());
        ByteBuf messageBytes = Unpooled.copiedBuffer(finalMessage, StandardCharsets.UTF_8);
        buf.writeBytes(messageBytes);
        messageBytes.release();
    }
}
