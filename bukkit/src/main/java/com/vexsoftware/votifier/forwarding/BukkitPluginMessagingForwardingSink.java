package com.vexsoftware.votifier.forwarding;

import com.vexsoftware.votifier.NuVotifierBukkit;
import com.vexsoftware.votifier.support.forwarding.AbstractPluginMessagingForwardingSink;
import com.vexsoftware.votifier.support.forwarding.ForwardedVoteListener;
import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.util.logging.Level;

/**
 * Created by Joe Hirschfeld on 10/20/2015.
 */
public class BukkitPluginMessagingForwardingSink extends AbstractPluginMessagingForwardingSink implements PluginMessageListener {

    public BukkitPluginMessagingForwardingSink(Plugin p, String channel, ForwardedVoteListener listener) {
        super(listener);
        Validate.notNull(channel, "Channel cannot be null.");
        this.channel = channel;
        Bukkit.getMessenger().registerIncomingPluginChannel(p, channel, this);
        this.p = p;
    }

    private final Plugin p;
    private final String channel;

    @Override
    public void halt() {
        Bukkit.getMessenger().unregisterIncomingPluginChannel(p, channel, this);
    }

    @Override
    public void onPluginMessageReceived(String s, Player player, byte[] bytes) {
        try {
            this.handlePluginMessage(bytes);
        } catch (Exception e) {
            NuVotifierBukkit.getInstance().getLogger().log(Level.SEVERE, "There was an unknown error when processing a forwarded vote.", e);
        }
    }
}
