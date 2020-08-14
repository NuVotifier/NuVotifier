package com.vexsoftware.votifier.sponge.forwarding;

import com.vexsoftware.votifier.sponge.NuVotifier;
import com.vexsoftware.votifier.support.forwarding.AbstractPluginMessagingForwardingSink;
import com.vexsoftware.votifier.support.forwarding.ForwardedVoteListener;
import org.spongepowered.api.Platform;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.network.ChannelBinding;
import org.spongepowered.api.network.ChannelBuf;
import org.spongepowered.api.network.RawDataListener;
import org.spongepowered.api.network.RemoteConnection;

import java.util.Optional;

public class SpongePluginMessagingForwardingSink extends AbstractPluginMessagingForwardingSink implements RawDataListener {

    private final NuVotifier p;
    private final ChannelBinding.RawDataChannel channelBinding;

    public SpongePluginMessagingForwardingSink(NuVotifier p, String channel, ForwardedVoteListener listener) {
        super(listener);

        Optional<ChannelBinding> binding = Sponge.getChannelRegistrar().getChannel(channel);
        if (binding.isPresent()) {
            if (binding.get() instanceof ChannelBinding.RawDataChannel) {
                this.channelBinding = (ChannelBinding.RawDataChannel) binding.get();
            } else {
                throw new IllegalStateException("Found an indexed channel - this is a problem.");
            }
        } else {
            this.channelBinding = Sponge.getChannelRegistrar().createRawChannel(p, channel);
        }

        this.channelBinding.addListener(Platform.Type.SERVER, this);
        this.p = p;
    }

    @Override
    public void halt() {
        channelBinding.removeListener(this);
    }

    @Override
    public void handlePayload(ChannelBuf channelBuf, RemoteConnection remoteConnection, Platform.Type type) {
        byte[] msgDirBuf = channelBuf.readBytes(channelBuf.available());
        try {
            this.handlePluginMessage(msgDirBuf);
        } catch (Exception e) {
            p.getLogger().error("There was an unknown error when processing a forwarded vote.", e);
        }
    }
}
