package com.vexsoftware.votifier.net.protocol;

import com.vexsoftware.votifier.VotifierPlugin;
import com.vexsoftware.votifier.model.Vote;
import com.vexsoftware.votifier.net.VotifierSession;
import com.vexsoftware.votifier.net.protocol.v1crypto.RSA;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class VotifierProtocol1DecoderTest {
    @Test
    public void testSuccessfulDecode() throws Exception {
        // Encode a test vote.
        String voteString = "VOTE\nTest\ntest\ntest\ntest\n";

        // For reference, this is the same vote as a POJO:
        Vote votePojo = new Vote("Test", "test", "test", "test");

        // Send the vote
        EmbeddedChannel channel = new EmbeddedChannel(new VotifierProtocol1Decoder());
        VotifierSession session = new VotifierSession();
        channel.attr(VotifierSession.KEY).set(session);
        channel.attr(VotifierPlugin.KEY).set(TestVotifierPlugin.getI());

        byte[] encrypted = RSA.encrypt(voteString.getBytes(StandardCharsets.UTF_8), TestVotifierPlugin.getI().getProtocolV1Key().getPublic());
        ByteBuf encryptedByteBuf = Unpooled.wrappedBuffer(encrypted);

        assertTrue(channel.writeInbound(encryptedByteBuf));
        assertEquals(votePojo, channel.readInbound());
        assertFalse(channel.finish());
    }
}
