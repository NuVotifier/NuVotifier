package com.vexsoftware.votifier.net.protocol;

import com.vexsoftware.votifier.VotifierPlugin;
import com.vexsoftware.votifier.model.Vote;
import com.vexsoftware.votifier.net.VotifierSession;
import com.vexsoftware.votifier.net.protocol.v1crypto.RSA;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.CorruptedFrameException;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;

import static org.junit.Assert.*;

public class VotifierProtocol1DecoderTest {
    private static final VotifierSession SESSION = new VotifierSession();

    private EmbeddedChannel createChannel() {
        EmbeddedChannel channel = new EmbeddedChannel(new VotifierProtocol1Decoder());
        channel.attr(VotifierSession.KEY).set(SESSION);
        channel.attr(VotifierPlugin.KEY).set(TestVotifierPlugin.getI());
        return channel;
    }

    @Test
    public void testSuccessfulDecode() throws Exception {
        Vote votePojo = new Vote("Test", "test", "test", "test");

        // Send the vote
        EmbeddedChannel channel = createChannel();

        byte[] encrypted = VoteUtil.encodePOJOv1(votePojo);
        ByteBuf encryptedByteBuf = Unpooled.wrappedBuffer(encrypted);

        assertTrue(channel.writeInbound(encryptedByteBuf));
        Object presumedVote = channel.readInbound();
        assertEquals(votePojo, presumedVote);
        assertFalse(channel.finish());
    }

    private void verifyFailure(String bad) throws Exception {
        // Send the bad vote
        EmbeddedChannel channel = createChannel();

        byte[] encrypted = RSA.encrypt(bad.getBytes(StandardCharsets.UTF_8), TestVotifierPlugin.getI().getProtocolV1Key().getPublic());
        ByteBuf encryptedByteBuf = Unpooled.wrappedBuffer(encrypted);

        try {
            channel.writeInbound(encryptedByteBuf);
        } finally {
            channel.close();
        }
    }

    @Test(expected = CorruptedFrameException.class)
    public void testFailureDecodeMissingField() throws Exception {
        verifyFailure("VOTE\nTest\ntest\ntest"); // missing field
    }

    @Test(expected = CorruptedFrameException.class)
    public void testFailureDecodeBadOpcode() throws Exception {
        verifyFailure("TEST\nTest\ntest\ntest\ntest\n");
    }

    @Test(expected = CorruptedFrameException.class)
    public void testFailureDecodeBadRsa() throws Exception {
        // Decode our bad RSA key
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(TestVotifierPlugin.r("/bad_public.key"));
        PublicKey badPublicKey = keyFactory.generatePublic(publicKeySpec);

        // Send the bad vote
        EmbeddedChannel channel = createChannel();

        byte[] encrypted = VoteUtil.encodePOJOv1(new Vote("Test", "test", "test", "test"), badPublicKey);
        ByteBuf encryptedByteBuf = Unpooled.wrappedBuffer(encrypted);

        try {
            channel.writeInbound(encryptedByteBuf);
        } finally {
            channel.close();
        }
    }
}
