package com.vexsoftware.votifier.net.protocol;

import com.vexsoftware.votifier.VotifierPlugin;
import com.vexsoftware.votifier.model.Vote;
import com.vexsoftware.votifier.net.protocol.v1crypto.RSA;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.CorruptedFrameException;

import java.util.List;

/**
 * Decodes original protocol votes.
 */
public class VotifierProtocol1Decoder extends ByteToMessageDecoder {
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf buf, List<Object> list) throws Exception {
        int readable = buf.readableBytes();

        if (readable != 256) {
            throw new CorruptedFrameException("Not a full 256-byte v1 block");
        }

        byte[] block = new byte[256];
        buf.getBytes(0, block);
        // "Drain" the whole buffer
        buf.readerIndex(buf.capacity());

        VotifierPlugin plugin = ctx.channel().attr(VotifierPlugin.KEY).get();

        try {
            block = RSA.decrypt(block, plugin.getProtocolV1Key().getPrivate());
        } catch (Exception e) {
            throw new CorruptedFrameException("Could not decrypt data (is your key correct?)", e);
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

        // Create the vote.
        Vote vote = new Vote(serviceName, username, address, timeStamp);
        list.add(vote);

        // We are done, remove ourselves. Why? Sometimes, we will decode multiple vote messages.
        // Netty doesn't like this, so we must remove ourselves from the pipeline. With Protocol 1,
        // ending votes is a "fire and forget" operation, so this is safe.
        ctx.pipeline().remove(this);
    }

    /**
     * Reads a string from a block of data.
     *
     * @param data The data to read from
     * @return The string
     */
    private static String readString(byte[] data, int offset) {
        if (offset >= data.length) {
            throw new CorruptedFrameException("Tried to read more data than expected.");
        }

        StringBuilder builder = new StringBuilder();
        for (int i = offset; i < data.length; i++) {
            if (data[i] == '\n')
                break; // Delimiter reached.
            builder.append((char) data[i]);
        }
        return builder.toString();
    }
}
