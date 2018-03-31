package com.vexsoftware.votifier.sponge.forwarding;

import com.google.gson.JsonObject;
import com.vexsoftware.votifier.model.Vote;
import com.vexsoftware.votifier.sponge.VotifierPlugin;
import com.vexsoftware.votifier.util.GsonInst;
import org.spongepowered.api.Platform;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.network.ChannelBinding;
import org.spongepowered.api.network.ChannelBuf;
import org.spongepowered.api.network.RawDataListener;
import org.spongepowered.api.network.RemoteConnection;

import java.nio.charset.StandardCharsets;

public class SpongePluginMessagingForwardingSink implements ForwardingVoteSink, RawDataListener {

    public SpongePluginMessagingForwardingSink(VotifierPlugin p, String channel, ForwardedVoteListener listener) {
        this.channelBinding = Sponge.getChannelRegistrar().createRawChannel(p, channel);
        this.channelBinding.addListener(Platform.Type.SERVER, this);
        this.listener = listener;
        this.p = p;
    }

    private final VotifierPlugin p;
    private final ForwardedVoteListener listener;
    private final ChannelBinding.RawDataChannel channelBinding;

    @Override
    public void halt() {
        channelBinding.removeListener(this);
    }

    @Override
    public void handlePayload(ChannelBuf channelBuf, RemoteConnection remoteConnection, Platform.Type type) {
        try {
            byte[] msgDirBuf = channelBuf.readBytes(channelBuf.available());
            String message = new String(msgDirBuf, StandardCharsets.UTF_8);
            JsonObject jsonObject  = GsonInst.gson.fromJson(message, JsonObject.class);
            Vote v = new Vote(jsonObject);
            listener.onForward(v);
        } catch (Exception e) {
            p.getLogger().error("There was an unknown error when processing a forwarded vote.", e);
        }
    }
}
