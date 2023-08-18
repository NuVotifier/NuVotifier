package com.vexsoftware.votifier.fabric.forwarding;

import com.vexsoftware.votifier.fabric.NuVotifier;
import com.vexsoftware.votifier.support.forwarding.AbstractPluginMessagingForwardingSink;
import com.vexsoftware.votifier.support.forwarding.ForwardedVoteListener;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;

public class FabricMessagingForwardingSink extends AbstractPluginMessagingForwardingSink implements ServerPlayNetworking.PlayChannelHandler {

    private final String channel;

    public FabricMessagingForwardingSink(String channel, ForwardedVoteListener listener) {
        super(listener);
        this.channel = channel;
        ServerPlayNetworking.registerGlobalReceiver(new ResourceLocation(channel), this);
    }

    @Override
    public void halt() {
        ServerPlayNetworking.unregisterGlobalReceiver(new ResourceLocation(channel));
    }

    @Override
    public void receive(MinecraftServer server, ServerPlayer player, ServerGamePacketListenerImpl handler, FriendlyByteBuf buf, PacketSender responseSender) {
        byte[] data = new byte[buf.readableBytes()];
        buf.readBytes(data);
        try {
            this.handlePluginMessage(data);
        } catch (Exception e) {
            NuVotifier.LOGGER.error("There was an unknown error when processing a forwarded vote.", e);
        }
    }
}
