package com.vexsoftware.votifier.sponge.forwarding;

import com.vexsoftware.votifier.sponge.VotifierPlugin;
import com.vexsoftware.votifier.support.forwarding.AbstractPluginMessagingForwardingSink;
import com.vexsoftware.votifier.support.forwarding.ForwardedVoteListener;
import org.spongepowered.api.Platform;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.network.ChannelBinding;
import org.spongepowered.api.network.ChannelBuf;
import org.spongepowered.api.network.RawDataListener;
import org.spongepowered.api.network.RemoteConnection;

public class SpongePluginMessagingForwardingSink extends AbstractPluginMessagingForwardingSink implements RawDataListener {

    public SpongePluginMessagingForwardingSink(VotifierPlugin p, String channel, ForwardedVoteListener listener) {
        super(listener);
        this.channelBinding = Sponge.getChannelRegistrar().createRawChannel(p, channel);
        this.channelBinding.addListener(Platform.Type.SERVER, this);
        this.p = p;
    }

    private final VotifierPlugin p;
    private final ChannelBinding.RawDataChannel channelBinding;

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
