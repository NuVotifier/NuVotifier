package io.ibj.nuvotifier.legacyserver.protocol;

import io.ibj.nuvotifier.legacyserver.protocol.v1crypto.RSA;
import io.ibj.nuvotifier.model.NuVote;
import io.ibj.nuvotifier.platform.V1KeyProvider;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.CorruptedFrameException;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Decodes original protocol votes.
 */
public class VotifierProtocol1Decoder extends ByteToMessageDecoder {

    private final V1KeyProvider keyProvider;

    public VotifierProtocol1Decoder(V1KeyProvider keyProvider) {
        this.keyProvider = keyProvider;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf buf, List<Object> list) {
        if (buf.readableBytes() < 256) {
            return;
        }

        byte[] block = new byte[buf.readableBytes()];
        buf.getBytes(0, block);
        // "Drain" the whole buffer
        buf.readerIndex(buf.readableBytes());

        try {
            block = RSA.decrypt(block, keyProvider.getPrivateKey());
        } catch (Exception e) {
            throw new CorruptedFrameException("Could not decrypt data (is your key correct?)", e);
        }

        // Parse the string we received.
        String all = new String(block, StandardCharsets.US_ASCII);
        String[] split = all.split("\n");
        if (split.length < 5) {
            throw new CorruptedFrameException("Not enough fields specified in vote.");
        }

        if (!split[0].equals("VOTE")) {
            throw new CorruptedFrameException("VOTE opcode not found");
        }

        // Create the vote.
        NuVote vote = NuVote.createLegacy(split[1], split[2], split[3], split[4]);
        list.add(vote);

        // We are done, remove ourselves. Why? Sometimes, we will decode multiple vote messages.
        // Netty doesn't like this, so we must remove ourselves from the pipeline. With Protocol 1,
        // ending votes is a "fire and forget" operation, so this is safe.
        ctx.pipeline().remove(this);
    }
}
