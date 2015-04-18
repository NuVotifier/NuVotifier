package com.vexsoftware.votifier.net.protocol;

import com.vexsoftware.votifier.Votifier;
import com.vexsoftware.votifier.model.Vote;
import com.vexsoftware.votifier.net.VotifierSession;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import org.json.JSONObject;

import javax.xml.bind.DatatypeConverter;
import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.security.Signature;
import java.util.List;

/**
 * Decodes protocol 2 JSON votes.
 */
public class VotifierProtocol2VoteDecoder extends MessageToMessageDecoder<String> {
    @Override
    protected void decode(ChannelHandlerContext ctx, String s, List<Object> list) throws Exception {
        JSONObject voteMessage = new JSONObject(s);
        VotifierSession session = ctx.channel().attr(VotifierSession.KEY).get();

        // Verify challenge.
        if (!voteMessage.getString("challenge").equals(session.getChallenge())) {
            throw new RuntimeException("Challenge is not valid");
        }

        // Verify that the service exists.
        PublicKey key = Votifier.getInstance().getKeys().get(voteMessage.getString("serviceName"));

        if (key == null) {
            throw new RuntimeException("Unknown service '" + voteMessage.getString("serviceName") + "'");
        }

        // Verify that the service got a token from us.
        String token = voteMessage.getString("token");

        if (token == null || !token.equals(Votifier.getInstance().getToken())) {
            throw new RuntimeException("Got invalid token " + token + ", wanted " + Votifier.getInstance().getToken());
        }

        // Verify signature.
        String sigHash = voteMessage.getString("signature");
        byte[] sigArray = DatatypeConverter.parseBase64Binary(sigHash);
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initVerify(key);
        signature.update(voteMessage.getString("payload").getBytes(StandardCharsets.UTF_8));

        if (!signature.verify(sigArray)) {
            throw new RuntimeException("Signature is not valid");
        }

        //
        JSONObject votePayload = new JSONObject(voteMessage.getString("payload"));

        Vote vote = new Vote();
        vote.setServiceName(voteMessage.getString("serviceName"));
        vote.setUsername(votePayload.getString("username"));
        vote.setAddress(votePayload.getString("address"));
        vote.setTimeStamp(votePayload.getString("timestamp"));

        if (Votifier.getInstance().isDebug())
            Votifier.getInstance().getLogger().info("Received protocol v2 vote record -> " + vote);

        list.add(vote);

        ctx.pipeline().remove(this);
    }
}
