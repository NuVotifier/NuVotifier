package com.vexsoftware.votifier.forwarding;

import com.google.gson.JsonObject;
import com.vexsoftware.votifier.NuVotifierBukkit;
import com.vexsoftware.votifier.model.Vote;
import com.vexsoftware.votifier.util.GsonInst;
import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.nio.charset.StandardCharsets;
import java.util.logging.Level;

/**
 * Created by Joe Hirschfeld on 10/20/2015.
 */
public class BukkitPluginMessagingForwardingSink implements ForwardingVoteSink, PluginMessageListener {

    public BukkitPluginMessagingForwardingSink(Plugin p, String channel, ForwardedVoteListener listener) {
        Validate.notNull(channel, "Channel cannot be null.");
        this.channel = channel;
        Bukkit.getMessenger().registerIncomingPluginChannel(p, channel, this);
        this.listener = listener;
        this.p = p;
    }

    private final Plugin p;
    private final String channel;
    private final ForwardedVoteListener listener;

    @Override
    public void halt() {
        Bukkit.getMessenger().unregisterIncomingPluginChannel(p, channel, this);
    }

    @Override
    public void onPluginMessageReceived(String s, Player player, byte[] bytes) {
        try {
            String message = new String(bytes, StandardCharsets.UTF_8);
            JsonObject jsonObject = GsonInst.gson.fromJson(message, JsonObject.class);
            Vote v = new Vote(jsonObject);
            listener.onForward(v);
        } catch (Exception e) {
            NuVotifierBukkit.getInstance().getLogger().log(Level.SEVERE, "There was an unknown error when processing a forwarded vote.", e);
        }
    }
}
