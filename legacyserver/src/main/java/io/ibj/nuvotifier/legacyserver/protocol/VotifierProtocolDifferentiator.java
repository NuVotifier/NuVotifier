package io.ibj.nuvotifier.legacyserver.protocol;

import io.ibj.nuvotifier.legacyserver.VotifierSession;
import io.ibj.nuvotifier.platform.V1KeyProvider;
import io.ibj.nuvotifier.platform.V2TokenProvider;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.CorruptedFrameException;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Attempts to determine if original protocol or protocol v2 is being used.
 */
public class VotifierProtocolDifferentiator extends ByteToMessageDecoder {
    private static final short PROTOCOL_2_MAGIC = 0x733A;
    private final boolean testMode;

    private final V1KeyProvider v1;
    private final V2TokenProvider v2;

    public VotifierProtocolDifferentiator(V1KeyProvider v1, V2TokenProvider v2, boolean testMode) {
        this.testMode = testMode;
        this.v1 = v1;
        this.v2 = v2;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf buf, List<Object> list) {
        // Determine the number of bytes that are available.
        int readable = buf.readableBytes();

        if (readable < 2) {
            // Some retarded voting sites (PMC?) seem to send empty buffers for no good reason.
            return;
        }

        short readMagic = buf.readShort();

        // Reset reader index again
        buf.readerIndex(0);

        VotifierSession session = ctx.channel().attr(VotifierSession.KEY).get();

        if (readMagic == PROTOCOL_2_MAGIC) {
            if (v2 == null) {
                throw new CorruptedFrameException("This server only accepts v1 Votifier packets.");
            }

            // Short 0x733A + Message = Protocol v2 Vote
            session.setVersion(VotifierSession.ProtocolVersion.TWO);

            if (!testMode) {
                ctx.pipeline().addAfter("protocolDifferentiator", "protocol2LengthDecoder", new LengthFieldBasedFrameDecoder(1024, 2, 2, 0, 4));
                ctx.pipeline().addAfter("protocol2LengthDecoder", "protocol2StringDecoder", new StringDecoder(StandardCharsets.UTF_8));
                ctx.pipeline().addAfter("protocol2StringDecoder", "protocol2VoteDecoder", new VotifierProtocol2Decoder(v2));
                ctx.pipeline().addAfter("protocol2VoteDecoder", "protocol2StringEncoder", new StringEncoder(StandardCharsets.UTF_8));
                ctx.pipeline().remove(this);
            }
        } else {
            if (v1 == null) {
                throw new CorruptedFrameException("This server only accepts well-formed Votifier v2 packets.");
            }
            // Probably Protocol v1 Vote Message
            session.setVersion(VotifierSession.ProtocolVersion.ONE);
            if (!testMode) {
                ctx.pipeline().addAfter("protocolDifferentiator", "protocol1Handler", new VotifierProtocol1Decoder(v1));
                ctx.pipeline().remove(this);
            }
        }
    }
}
