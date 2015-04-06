package com.vexsoftware.votifier.net.protocol;

import com.vexsoftware.votifier.Votifier;
import com.vexsoftware.votifier.crypto.RSA;
import com.vexsoftware.votifier.model.Vote;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.CorruptedFrameException;

import java.util.List;

/**
 * Decodes original protocol votes.
 */
public class VotifierProtocol1Decoder extends ByteToMessageDecoder {
    private static final boolean WARNING = false;

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf buf, List<Object> list) throws Exception {
        int readable = buf.readableBytes();

        if (readable < 256) {
            return;
        }

        byte[] block = new byte[256];
        buf.getBytes(0, block);

        try {
            block = RSA.decrypt(block, Votifier.getInstance().getKeyPair()
                    .getPrivate());
        } catch (Exception e) {
            throw new CorruptedFrameException("Could not decrypt data", e);
        }
        int position = 0;

        // Perform the opcode check.
        String opcode = readString(block, position);
        position += opcode.length() + 1;
        if (!opcode.equals("VOTE")) {
            throw new CorruptedFrameException("VOTE opcode not found");
        }

        // Parse the block.
        String serviceName = readString(block, position);
        position += serviceName.length() + 1;
        String username = readString(block, position);
        position += username.length() + 1;
        String address = readString(block, position);
        position += address.length() + 1;
        String timeStamp = readString(block, position);
        position += timeStamp.length() + 1;

        // Create the vote.
        final Vote vote = new Vote();
        vote.setServiceName(serviceName);
        vote.setUsername(username);
        vote.setAddress(address);
        vote.setTimeStamp(timeStamp);

        /* Warning for using v1, when v2 is standardizing. */
        if (WARNING) {
            Votifier.getInstance().getLogger().warning(serviceName + " has sent a protocol 1 message. This version is DEPRECATED and will be removed in a future Votifier release.");
        }

        if (Votifier.getInstance().isDebug())
            Votifier.getInstance().getLogger().info("Received protocol v1 vote record -> " + vote);

        list.add(vote);

        // We are done, remove ourselves. Why? Sometimes, we will decode multiple vote messages.
        // Netty doesn't like this, so we must remove ourselves from the pipeline. With Protocol 1,
        // ending votes is a "fire and forget" operation, so this is safe.
        ctx.pipeline().remove(this);
    }

    /**
     * Reads a string from a block of data.
     *
     * @param data
     *            The data to read from
     * @return The string
     */
    private static String readString(byte[] data, int offset) {
        StringBuilder builder = new StringBuilder();
        for (int i = offset; i < data.length; i++) {
            if (data[i] == '\n')
                break; // Delimiter reached.
            builder.append((char) data[i]);
        }
        return builder.toString();
    }
}
