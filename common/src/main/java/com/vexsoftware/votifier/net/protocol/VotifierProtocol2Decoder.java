package com.vexsoftware.votifier.net.protocol;

import com.vexsoftware.votifier.VotifierPlugin;
import com.vexsoftware.votifier.model.Vote;
import com.vexsoftware.votifier.net.VotifierSession;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.CorruptedFrameException;
import io.netty.handler.codec.MessageToMessageDecoder;
import org.json.JSONObject;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Decodes protocol 2 JSON votes.
 */
public class VotifierProtocol2Decoder extends MessageToMessageDecoder<String> {
    private static final SecureRandom RANDOM = new SecureRandom();

    @Override
    protected void decode(ChannelHandlerContext ctx, String s, List<Object> list) throws Exception {
        JSONObject voteMessage = new JSONObject(s);
        VotifierSession session = ctx.channel().attr(VotifierSession.KEY).get();

        // Deserialize the payload.
        JSONObject votePayload = new JSONObject(voteMessage.getString("payload"));

        // Verify challenge.
        if (!votePayload.getString("challenge").equals(session.getChallenge())) {
            throw new CorruptedFrameException("Challenge is not valid");
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
        byte[] sigBytes = DatatypeConverter.parseBase64Binary(sigHash);

        if (!hmacEqual(sigBytes, voteMessage.getString("payload").getBytes(StandardCharsets.UTF_8), key)) {
            throw new CorruptedFrameException("Signature is not valid (invalid token?)");
        }

        // Stopgap: verify the "uuid" field is valid, if provided.
        if (votePayload.has("uuid")) {
            UUID.fromString(votePayload.getString("uuid"));
        }

        if (votePayload.getString("username").length() > 16) {
            throw new CorruptedFrameException("Username too long");
        }

        // Create the vote.
        Vote vote = new Vote(votePayload);
        list.add(vote);

        ctx.pipeline().remove(this);
    }

    private boolean hmacEqual(byte[] sig, byte[] message, Key key) throws NoSuchAlgorithmException, InvalidKeyException {
        // See https://www.nccgroup.trust/us/about-us/newsroom-and-events/blog/2011/february/double-hmac-verification/
        // This randomizes the byte order to make timing attacks more difficult.
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(key);
        byte[] calculatedSig = mac.doFinal(message);

        // Generate a random key for use in comparison
        byte[] randomKey = new byte[32];
        RANDOM.nextBytes(randomKey);

        // Then generate two HMACs for the different signatures found
        Mac mac2 = Mac.getInstance("HmacSHA256");
        mac2.init(new SecretKeySpec(randomKey, "HmacSHA256"));
        byte[] clientSig = mac2.doFinal(sig);
        mac2.reset();
        byte[] realSig = mac2.doFinal(calculatedSig);

        return MessageDigest.isEqual(clientSig, realSig);
    }
}
