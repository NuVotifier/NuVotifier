package com.vexsoftware.votifier.net.protocol;

import com.vexsoftware.votifier.VotifierPlugin;
import com.vexsoftware.votifier.model.Vote;
import com.vexsoftware.votifier.net.VotifierSession;
import com.vexsoftware.votifier.util.KeyCreator;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.CorruptedFrameException;
import io.netty.handler.codec.DecoderException;
import org.json.JSONObject;
import org.junit.Test;

import javax.crypto.Mac;
import javax.xml.bind.DatatypeConverter;
import java.nio.charset.StandardCharsets;
import java.security.Key;

import static org.junit.Assert.*;

public class VotifierProtocol2DecoderTest {
    private static final VotifierSession SESSION = new VotifierSession();

    private EmbeddedChannel createChannel() {
        EmbeddedChannel channel = new EmbeddedChannel(new VotifierProtocol2Decoder());
        channel.attr(VotifierSession.KEY).set(SESSION);
        channel.attr(VotifierPlugin.KEY).set(TestVotifierPlugin.getI());
        return channel;
    }

    private void sendVote(Vote vote, Key key, boolean expectSuccess) throws Exception {
        // Create a well-formed request
        EmbeddedChannel channel = createChannel();

        JSONObject object = new JSONObject();
        JSONObject payload = vote.serialize();
        payload.put("challenge", SESSION.getChallenge());
        object.put("payload", payload.toString());
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(key);
        object.put("signature",
                DatatypeConverter.printBase64Binary(mac.doFinal(payload.toString().getBytes(StandardCharsets.UTF_8))));

        if (expectSuccess) {
            assertTrue(channel.writeInbound(object.toString()));
            assertEquals(vote, channel.readInbound());
            assertFalse(channel.finish());
        } else {
            try {
                channel.writeInbound(object.toString());
            } finally {
                channel.close();
            }
        }
    }

    @Test
    public void testSuccessfulDecode() throws Exception {
        sendVote(new Vote("Test", "test", "test", "0"), TestVotifierPlugin.getI().getTokens().get("default"), true);
    }

    @Test(expected = DecoderException.class)
    public void testFailureDecodeBadPacket() throws Exception {
        // Create a well-formed request
        EmbeddedChannel channel = createChannel();

        Vote vote = new Vote("Test", "test", "test", "0");
        JSONObject object = new JSONObject();
        JSONObject payload = vote.serialize();
        payload.put("challenge", SESSION.getChallenge());
        object.put("payload", payload.toString());
        // We "forget" the signature.

        try {
            channel.writeInbound(object.toString());
        } finally {
            channel.close();
        }
    }

    @Test(expected = DecoderException.class)
    public void testFailureDecodeBadVoteField() throws Exception {
        // Create a well-formed request
        EmbeddedChannel channel = createChannel();

        Vote vote = new Vote("Test", "test", "test", "0");
        JSONObject object = new JSONObject();
        JSONObject payload = vote.serialize();
        // We "forget" the challenge.
        object.put("payload", payload.toString());
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(TestVotifierPlugin.getI().getTokens().get("default"));
        object.put("signature",
                DatatypeConverter.printBase64Binary(mac.doFinal(payload.toString().getBytes(StandardCharsets.UTF_8))));

        try {
            channel.writeInbound(object.toString());
        } finally {
            channel.close();
        }
    }

    @Test(expected = DecoderException.class)
    public void testFailureDecodeBadChallenge() throws Exception {
        // Create a well-formed request
        EmbeddedChannel channel = createChannel();

        Vote vote = new Vote("Test", "test", "test", "0");
        JSONObject object = new JSONObject();
        JSONObject payload = vote.serialize();
        // We provide the wrong challenge.
        payload.put("challenge", "not a challenge for me");
        object.put("payload", payload.toString());
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(TestVotifierPlugin.getI().getTokens().get("default"));
        object.put("signature",
                DatatypeConverter.printBase64Binary(mac.doFinal(payload.toString().getBytes(StandardCharsets.UTF_8))));

        try {
            channel.writeInbound(object.toString());
        } finally {
            channel.close();
        }
    }

    @Test(expected = DecoderException.class)
    public void testFailureDecodeNonExistentKey() throws Exception {
        TestVotifierPlugin.getI().specificKeysOnly();

        Vote vote = new Vote("Bad Service", "test", "test", "0");

        try {
            sendVote(vote, TestVotifierPlugin.getI().getTokens().get("Test"), false);
        } finally {
            TestVotifierPlugin.getI().restoreDefault();
        }
    }

    @Test(expected = CorruptedFrameException.class)
    public void testFailureDecodeBadSignature() throws Exception {
        Vote vote = new Vote("Bad Service", "test", "test", "0");
        sendVote(vote, KeyCreator.createKeyFrom("BadKey"), false);
    }
}