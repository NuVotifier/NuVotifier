package com.vexsoftware.votifier.net.protocol;

import com.vexsoftware.votifier.model.Vote;
import com.vexsoftware.votifier.net.VotifierSession;
import com.vexsoftware.votifier.util.QuietException;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.CorruptedFrameException;
import io.netty.handler.codec.DecoderException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class VotifierProtocolDifferentiatorTest {
    @Test
    public void v1Test() {
        EmbeddedChannel channel = new EmbeddedChannel(new VotifierProtocolDifferentiator(true, true));

        VotifierSession session = new VotifierSession();
        channel.attr(VotifierSession.KEY).set(session);

        ByteBuf test = Unpooled.buffer(256);
        for (int i = 0; i < 256; i++) {
            test.writeByte(0);
        }
        channel.writeInbound(test);

        assertEquals(VotifierSession.ProtocolVersion.ONE, session.getVersion());
        channel.close();
    }

    @Test
    public void v2Test() {
        EmbeddedChannel channel = new EmbeddedChannel(new VotifierProtocolDifferentiator(true, false));

        VotifierSession session = new VotifierSession();
        channel.attr(VotifierSession.KEY).set(session);

        ByteBuf test = Unpooled.buffer();
        test.writeShort(0x733A);
        channel.writeInbound(test);

        assertEquals(VotifierSession.ProtocolVersion.TWO, session.getVersion());
        channel.close();
    }

    @Test
    public void failIfv1NotSupported() {
        EmbeddedChannel channel = new EmbeddedChannel(new VotifierProtocolDifferentiator(true, false));

        VotifierSession session = new VotifierSession();
        channel.attr(VotifierSession.KEY).set(session);

        ByteBuf test = Unpooled.buffer(256);
        for (int i = 0; i < 256; i++) {
            test.writeByte(0);
        }
        assertThrows(DecoderException.class, () -> channel.writeInbound(test));
        channel.close();
    }

    @Test
    public void failOnBadPacketTest() {
        EmbeddedChannel channel = new EmbeddedChannel(new VotifierProtocolDifferentiator(true, false));

        ByteBuf buf = Unpooled.buffer();
        for (int i = 0; i < 3; i++) {
            buf.writeByte(0);
        }

        assertThrows(DecoderException.class, () -> channel.writeInbound(buf));
        channel.close();
    }

    @Test
    public void tryIdentifyRealVotev1() throws Exception {
        EmbeddedChannel channel = new EmbeddedChannel(new VotifierProtocolDifferentiator(true, true));

        VotifierSession session = new VotifierSession();
        channel.attr(VotifierSession.KEY).set(session);

        Vote votePojo = new Vote("Test", "test", "test", "test");
        byte[] encrypted = VoteUtil.encodePOJOv1(votePojo);
        ByteBuf encryptedByteBuf = Unpooled.wrappedBuffer(encrypted);

        channel.writeInbound(encryptedByteBuf);

        assertEquals(VotifierSession.ProtocolVersion.ONE, session.getVersion());
        channel.close();
    }
}
