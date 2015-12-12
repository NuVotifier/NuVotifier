package com.vexsoftware.votifier.net.protocol;

import com.vexsoftware.votifier.VotifierPlugin;
import com.vexsoftware.votifier.model.Vote;
import com.vexsoftware.votifier.net.VotifierSession;
import io.netty.channel.embedded.EmbeddedChannel;
import org.json.JSONObject;
import org.junit.Test;

import javax.crypto.Mac;
import javax.xml.bind.DatatypeConverter;

import java.nio.charset.StandardCharsets;

import static org.junit.Assert.*;

public class VotifierProtocol2DecoderTest {
    @Test
    public void testSuccessfulDecode() throws Exception {
        // Create a well-formed request
        EmbeddedChannel channel = new EmbeddedChannel(new VotifierProtocol2Decoder());
        VotifierSession session = new VotifierSession();
        channel.attr(VotifierSession.KEY).set(session);
        channel.attr(VotifierPlugin.KEY).set(TestVotifierPlugin.getI());

        Vote vote = new Vote("Test", "test", "test", "0");
        JSONObject object = new JSONObject();
        JSONObject payload = vote.serialize();
        payload.put("challenge", session.getChallenge());
        object.put("payload", payload.toString());
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(TestVotifierPlugin.getI().getTokens().get("default"));
        object.put("signature",
                DatatypeConverter.printBase64Binary(mac.doFinal(payload.toString().getBytes(StandardCharsets.UTF_8))));

        assertTrue(channel.writeInbound(object.toString()));
        assertEquals(vote, channel.readInbound());
        assertFalse(channel.finish());
    }
}