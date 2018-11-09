/*
 * Copyright (C) 2012 Vex Software LLC
 * This file is part of Votifier.
 *
 * Votifier is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Votifier is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Votifier.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.vexsoftware.votifier;

import com.google.common.io.ByteStreams;
import com.google.common.io.Files;
import com.vexsoftware.votifier.forwarding.BukkitPluginMessagingForwardingSink;
import com.vexsoftware.votifier.forwarding.ForwardedVoteListener;
import com.vexsoftware.votifier.forwarding.ForwardingVoteSink;
import com.vexsoftware.votifier.model.Vote;
import com.vexsoftware.votifier.model.VotifierEvent;
import com.vexsoftware.votifier.net.VotifierServerBootstrap;
import com.vexsoftware.votifier.net.VotifierSession;
import com.vexsoftware.votifier.net.protocol.v1crypto.RSAIO;
import com.vexsoftware.votifier.net.protocol.v1crypto.RSAKeygen;
import com.vexsoftware.votifier.platform.JavaUtilLogger;
import com.vexsoftware.votifier.platform.LoggingAdapter;
import com.vexsoftware.votifier.platform.VotifierPlugin;
import com.vexsoftware.votifier.platform.scheduler.VotifierScheduler;
import com.vexsoftware.votifier.util.KeyCreator;
import com.vexsoftware.votifier.util.TokenUtil;
import io.netty.channel.Channel;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.KeyPair;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

/**
 * The main Votifier plugin class.
 *
 * @author Blake Beaupain
 * @author Kramer Campbell
 */
public class NuVotifierBukkit extends JavaPlugin implements VoteHandler, VotifierPlugin, ForwardedVoteListener {

    /**
     * The Votifier instance.
     */
    private static NuVotifierBukkit instance;

    /**
     * The current Votifier version.
     */
    private String version;

    /**
     * The server bootstrap.
     */
    private VotifierServerBootstrap bootstrap;

    /**
     * The RSA key pair.
     */
    private KeyPair keyPair;

    /**
     * Debug mode flag
     */
    private boolean debug;

    /**
     * Keys used for websites.
     */
    private Map<String, Key> tokens = new HashMap<>();

    private ForwardingVoteSink forwardingMethod;
    private VotifierScheduler scheduler;
    private LoggingAdapter pluginLogger;

    private boolean loadAndBind() {
        scheduler = new BukkitScheduler(this);
        pluginLogger = new JavaUtilLogger(getLogger());
        if (!getDataFolder().exists()) {
            getDataFolder().mkdir();
        }

        // Handle configuration.
        File config = new File(getDataFolder(), "config.yml");

        /*
         * Use IP address from server.properties as a default for
         * configurations. Do not use InetAddress.getLocalHost() as it most
         * likely will return the main server address instead of the address
         * assigned to the server.
         */
        String hostAddr = Bukkit.getServer().getIp();
        if (hostAddr == null || hostAddr.length() == 0)
            hostAddr = "0.0.0.0";

        /*
         * Create configuration file if it does not exists; otherwise, load it
         */
        if (!config.exists()) {
            try {
                // First time run - do some initialization.
                getLogger().info("Configuring Votifier for the first time...");

                // Initialize the configuration file.
                config.createNewFile();

                // Load and manually replace variables in the configuration.
                String cfgStr = new String(ByteStreams.toByteArray(getResource("bukkitConfig.yml")), StandardCharsets.UTF_8);
                String token = TokenUtil.newToken();
                cfgStr = cfgStr.replace("%default_token%", token).replace("%ip%", hostAddr);
                Files.asCharSink(config, StandardCharsets.UTF_8).write(cfgStr);

                /*
                 * Remind hosted server admins to be sure they have the right
                 * port number.
                 */
                getLogger().info("------------------------------------------------------------------------------");
                getLogger().info("Assigning NuVotifier to listen on port 8192. If you are hosting Craftbukkit on a");
                getLogger().info("shared server please check with your hosting provider to verify that this port");
                getLogger().info("is available for your use. Chances are that your hosting provider will assign");
                getLogger().info("a different port, which you need to specify in config.yml");
                getLogger().info("------------------------------------------------------------------------------");
                getLogger().info("Your default NuVotifier token is " + token + ".");
                getLogger().info("You will need to provide this token when you submit your server to a voting");
                getLogger().info("list.");
                getLogger().info("------------------------------------------------------------------------------");
            } catch (Exception ex) {
                getLogger().log(Level.SEVERE, "Error creating configuration file", ex);
                return false;
            }
        }

        YamlConfiguration cfg;
        File rsaDirectory = new File(getDataFolder(), "rsa");

        // Load configuration.
        cfg = YamlConfiguration.loadConfiguration(config);

        /*
         * Create RSA directory and keys if it does not exist; otherwise, read
         * keys.
         */
        try {
            if (!rsaDirectory.exists()) {
                rsaDirectory.mkdir();
                keyPair = RSAKeygen.generate(2048);
                RSAIO.save(rsaDirectory, keyPair);
            } else {
                keyPair = RSAIO.load(rsaDirectory);
            }
        } catch (Exception ex) {
            getLogger().log(Level.SEVERE,
                    "Error reading configuration file or RSA tokens", ex);
            return false;
        }

        debug = cfg.getBoolean("debug", false);

        // Load Votifier tokens.
        ConfigurationSection tokenSection = cfg.getConfigurationSection("tokens");

        if (tokenSection != null) {
            Map<String, Object> websites = tokenSection.getValues(false);
            for (Map.Entry<String, Object> website : websites.entrySet()) {
                tokens.put(website.getKey(), KeyCreator.createKeyFrom(website.getValue().toString()));
                getLogger().info("Loaded token for website: " + website.getKey());
            }
        } else {
            String token = TokenUtil.newToken();
            tokenSection = cfg.createSection("tokens");
            tokenSection.set("default", token);
            tokens.put("default", KeyCreator.createKeyFrom(token));
            try {
                cfg.save(config);
            } catch (IOException e) {
                getLogger().log(Level.SEVERE,
                        "Error generating Votifier token", e);
                return false;
            }
            getLogger().info("------------------------------------------------------------------------------");
            getLogger().info("No tokens were found in your configuration, so we've generated one for you.");
            getLogger().info("Your default Votifier token is " + token + ".");
            getLogger().info("You will need to provide this token when you submit your server to a voting");
            getLogger().info("list.");
            getLogger().info("------------------------------------------------------------------------------");
        }

        // Initialize the receiver.
        final String host = cfg.getString("host", hostAddr);
        final int port = cfg.getInt("port", 8192);
        if (debug)
            getLogger().info("DEBUG mode enabled!");
        if (port >= 0) {
            final boolean disablev1 = cfg.getBoolean("disable-v1-protocol");
            if (disablev1) {
                getLogger().info("------------------------------------------------------------------------------");
                getLogger().info("Votifier protocol v1 parsing has been disabled. Most voting websites do not");
                getLogger().info("currently support the modern Votifier protocol in NuVotifier.");
                getLogger().info("------------------------------------------------------------------------------");
            }

            this.bootstrap = new VotifierServerBootstrap(host, port, this, disablev1);
            this.bootstrap.start(error -> {});
        } else {
            getLogger().info("------------------------------------------------------------------------------");
            getLogger().info("Your Votifier port is less than 0, so we assume you do NOT want to start the");
            getLogger().info("votifier port server! Votifier will not listen for votes over any port, and");
            getLogger().info("will only listen for pluginMessaging forwarded votes!");
            getLogger().info("------------------------------------------------------------------------------");
        }

        ConfigurationSection forwardingConfig = cfg.getConfigurationSection("forwarding");
        if (forwardingConfig != null) {
            String method = forwardingConfig.getString("method", "none").toLowerCase(); //Default to lower case for case-insensitive searches
            if ("none".equals(method)) {
                getLogger().info("Method none selected for vote forwarding: Votes will not be received from a forwarder.");
            } else if ("pluginmessaging".equals(method)) {
                String channel = forwardingConfig.getString("pluginMessaging.channel", "NuVotifier");
                try {
                    forwardingMethod = new BukkitPluginMessagingForwardingSink(this, channel, this);
                    getLogger().info("Receiving votes over PluginMessaging channel '" + channel + "'.");
                } catch (RuntimeException e) {
                    getLogger().log(Level.SEVERE, "NuVotifier could not set up PluginMessaging for vote forwarding!", e);
                }
            } else {
                getLogger().severe("No vote forwarding method '" + method + "' known. Defaulting to noop implementation.");
            }
        }
        return true;
    }

    private void halt() {
        // Shut down the network handlers.
        if (bootstrap != null) {
            bootstrap.shutdown();
            bootstrap = null;
        }

        if (forwardingMethod != null) {
            forwardingMethod.halt();
            forwardingMethod = null;
        }
    }

    @Override
    public void onEnable() {
        NuVotifierBukkit.instance = this;

        // Set the plugin version.
        version = getDescription().getVersion();

        if (!loadAndBind()) {
            gracefulExit();
            setEnabled(false); // safer to just bomb out
        }
    }

    @Override
    public void onDisable() {
        halt();
        getLogger().info("Votifier disabled.");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof ConsoleCommandSender) {
            reload();
        } else {
            sender.sendMessage(ChatColor.RED + "For security and stability, only console may run this command!");
        }
        return true;
    }

    private void reload() {
        try {
            halt();
        } catch (Exception ex) {
            getLogger().log(Level.SEVERE, "On halt, an exception was thrown. This may be fine!", ex);
        }

        if (loadAndBind()) {
            getLogger().info("Reload was successful.");
        } else {
            try {
                halt();
                getLogger().log(Level.SEVERE, "On reload, there was a problem with the configuration. Votifier currently does nothing!");
            } catch (Exception ex) {
                getLogger().log(Level.SEVERE, "On reload, there was a problem loading, and we could not re-halt the server. Votifier is in an unstable state!", ex);
            }
        }
    }

    private void gracefulExit() {
        getLogger().log(Level.SEVERE, "Votifier did not initialize properly!");
    }

    /**
     * Gets the instance.
     *
     * @return The instance
     */
    public static NuVotifierBukkit getInstance() {
        return instance;
    }

    @Override
    public LoggingAdapter getPluginLogger() {
        return pluginLogger;
    }

    @Override
    public VotifierScheduler getScheduler() {
        return scheduler;
    }

    public boolean isDebug() {
        return debug;
    }

    @Override
    public Map<String, Key> getTokens() {
        return tokens;
    }

    @Override
    public KeyPair getProtocolV1Key() {
        return keyPair;
    }

    @Override
    public void onVoteReceived(Channel channel, final Vote vote, VotifierSession.ProtocolVersion protocolVersion) {
        if (debug) {
            if (protocolVersion == VotifierSession.ProtocolVersion.ONE) {
                getLogger().info("Got a protocol v1 vote record from " + channel.remoteAddress() + " -> " + vote);
            } else {
                getLogger().info("Got a protocol v2 vote record from " + channel.remoteAddress() + " -> " + vote);
            }
        }
        Bukkit.getScheduler().runTask(this, () -> Bukkit.getPluginManager().callEvent(new VotifierEvent(vote)));
    }

    @Override
    public void onError(Channel channel, boolean alreadyHandledVote, Throwable throwable) {
        if (debug) {
            if (alreadyHandledVote) {
                getLogger().log(Level.SEVERE, "Vote processed, however an exception " +
                        "occurred with a vote from " + channel.remoteAddress(), throwable);
            } else {
                getLogger().log(Level.SEVERE, "Unable to process vote from " + channel.remoteAddress(), throwable);
            }
        } else if (!alreadyHandledVote) {
            getLogger().log(Level.SEVERE, "Unable to process vote from " + channel.remoteAddress());
        }
    }

    @Override
    public void onForward(final Vote v) {
        if (debug) {
            getLogger().info("Got a forwarded vote -> " + v);
        }
        Bukkit.getScheduler().runTask(this, () -> Bukkit.getPluginManager().callEvent(new VotifierEvent(v)));
    }
}
