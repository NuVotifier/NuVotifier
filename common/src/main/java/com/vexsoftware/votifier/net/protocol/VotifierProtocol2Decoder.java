package com.vexsoftware.votifier.net.protocol;

import com.vexsoftware.votifier.VotifierPlugin;
import com.vexsoftware.votifier.model.Vote;
import com.vexsoftware.votifier.net.VotifierSession;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import org.json.JSONObject;

import javax.crypto.Mac;
import javax.xml.bind.DatatypeConverter;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.List;

/**
 * Decodes protocol 2 JSON votes.
 */
public class VotifierProtocol2Decoder extends MessageToMessageDecoder<String> {
    @Override
    protected void decode(ChannelHandlerContext ctx, String s, List<Object> list) throws Exception {
        JSONObject voteMessage = new JSONObject(s);
        VotifierSession session = ctx.channel().attr(VotifierSession.KEY).get();

        // Deserialize the payload.
        JSONObject votePayload = new JSONObject(voteMessage.getString("payload"));

        // Verify challenge.
        if (!votePayload.getString("challenge").equals(session.getChallenge())) {
            throw new RuntimeException("Challenge is not valid");
        }

        // Verify that we have keys available.
        VotifierPlugin plugin = ctx.channel().attr(VotifierPlugin.KEY).get();
        Key key = plugin.getTokens().get(votePayload.getString("serviceName"));

        if (key == null) {
            key = plugin.getTokens().get("default");
            if (key == null) {
                throw new RuntimeException("Unknown service '" + votePayload.getString("serviceName") + "'");
            }
        }

        // Verify signature.
        String sigHash = voteMessage.getString("signature");
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(key);
        mac.update(voteMessage.getString("payload").getBytes(StandardCharsets.UTF_8));
        String computed = DatatypeConverter.printBase64Binary(mac.doFinal());

        if (!sigHash.equals(computed)) {
            throw new RuntimeException("Signature is not valid (invalid token?)");
        }

        // Create the vote.
        Vote vote = new Vote(votePayload);
        list.add(vote);

        ctx.pipeline().remove(this);
    }
}
