package com.vexsoftware.votifier.bungee;

import com.google.common.collect.ImmutableMap;
import com.google.common.io.ByteStreams;
import com.google.common.io.Files;
import com.vexsoftware.votifier.VoteHandler;
import com.vexsoftware.votifier.net.VotifierServerBootstrap;
import com.vexsoftware.votifier.platform.BackendServer;
import com.vexsoftware.votifier.platform.ProxyVotifierPlugin;
import com.vexsoftware.votifier.bungee.events.VotifierEvent;
import com.vexsoftware.votifier.platform.scheduler.VotifierScheduler;
import com.vexsoftware.votifier.support.forwarding.ForwardingVoteSource;
import com.vexsoftware.votifier.support.forwarding.cache.FileVoteCache;
import com.vexsoftware.votifier.support.forwarding.cache.MemoryVoteCache;
import com.vexsoftware.votifier.support.forwarding.cache.VoteCache;
import com.vexsoftware.votifier.support.forwarding.proxy.ProxyForwardingVoteSource;
import com.vexsoftware.votifier.model.Vote;
import com.vexsoftware.votifier.net.VotifierSession;
import com.vexsoftware.votifier.net.protocol.v1crypto.RSAIO;
import com.vexsoftware.votifier.net.protocol.v1crypto.RSAKeygen;
import com.vexsoftware.votifier.util.KeyCreator;
import com.vexsoftware.votifier.util.TokenUtil;
import io.netty.channel.Channel;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;
import org.slf4j.Logger;
import org.slf4j.impl.DirtyTricks;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.KeyPair;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class NuVotifier extends Plugin implements VoteHandler, ProxyVotifierPlugin {

    /**
     * The server channel.
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

    /**
     * Method used to forward votes to downstream servers
     */
    private ForwardingVoteSource forwardingMethod;

    private void loadAndBind() {
        if (!getDataFolder().exists()) {
            getDataFolder().mkdir();
        }

        // Handle configuration.
        File config = new File(getDataFolder() + "/config.yml");
        File rsaDirectory = new File(getDataFolder() + "/rsa");
        Configuration configuration;

        if (!config.exists()) {
            try {
                // First time run - do some initialization.
                getLogger().info("Configuring Votifier for the first time...");

                // Initialize the configuration file.
                config.createNewFile();

                String cfgStr = new String(ByteStreams.toByteArray(getResourceAsStream("bungeeConfig.yml")), StandardCharsets.UTF_8);
                String token = TokenUtil.newToken();
                cfgStr = cfgStr.replace("%default_token%", token);
                Files.asCharSink(config, StandardCharsets.UTF_8).write(cfgStr);

                /*
                 * Remind hosted server admins to be sure they have the right
                 * port number.
                 */
                getLogger().info("------------------------------------------------------------------------------");
                getLogger().info("Assigning NuVotifier to listen on port 8192. If you are hosting BungeeCord on a");
                getLogger().info("shared server please check with your hosting provider to verify that this port");
                getLogger().info("is available for your use. Chances are that your hosting provider will assign");
                getLogger().info("a different port, which you need to specify in config.yml");
                getLogger().info("------------------------------------------------------------------------------");
                getLogger().info("Assigning NuVotifier to listen to interface 0.0.0.0. This is usually alright,");
                getLogger().info("however, if you want NuVotifier to only listen to one interface for security ");
                getLogger().info("reasons (or you use a shared host), you may change this in the config.yml.");
                getLogger().info("------------------------------------------------------------------------------");
                getLogger().info("Your default Votifier token is " + token + ".");
                getLogger().info("You will need to provide this token when you submit your server to a voting");
                getLogger().info("list.");
                getLogger().info("------------------------------------------------------------------------------");
            } catch (Exception ex) {
                throw new RuntimeException("Unable to create configuration file", ex);
            }
        }

        // Load the configuration.
        try {
            configuration = ConfigurationProvider.getProvider(YamlConfiguration.class).load(config);
        } catch (IOException e) {
            throw new RuntimeException("Unable to load configuration", e);
        }

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
            throw new RuntimeException("Error reading RSA tokens", ex);
        }

        // Load Votifier tokens.
        Configuration tokenSection = configuration.getSection("tokens");

        if (configuration.get("tokens") != null) {
            for (String s : tokenSection.getKeys()) {
                tokens.put(s, KeyCreator.createKeyFrom(tokenSection.getString(s)));
                getLogger().info("Loaded token for website: " + s);
            }
        } else {
            String token = TokenUtil.newToken();
            configuration.set("tokens", ImmutableMap.of("default", token));
            tokens.put("default", KeyCreator.createKeyFrom(token));
            try {
                ConfigurationProvider.getProvider(YamlConfiguration.class).save(configuration, config);
            } catch (IOException e) {
                throw new RuntimeException("Error generating Votifier token", e);
            }
            getLogger().info("------------------------------------------------------------------------------");
            getLogger().info("No tokens were found in your configuration, so we've generated one for you.");
            getLogger().info("Your default Votifier token is " + token + ".");
            getLogger().info("You will need to provide this token when you submit your server to a voting");
            getLogger().info("list.");
            getLogger().info("------------------------------------------------------------------------------");
        }

        // Initialize the receiver.
        final String host = configuration.getString("host", "0.0.0.0");
        final int port = configuration.getInt("port", 8192);
        debug = configuration.getBoolean("debug", false);
        if (debug)
            getLogger().info("DEBUG mode enabled!");

        final boolean disablev1 = configuration.getBoolean("disable-v1-protocol");
        if (disablev1) {
            getLogger().info("------------------------------------------------------------------------------");
            getLogger().info("Votifier protocol v1 parsing has been disabled. Most voting websites do not");
            getLogger().info("currently support the modern Votifier protocol in NuVotifier.");
            getLogger().info("------------------------------------------------------------------------------");
        }

        // Must set up server asynchronously due to BungeeCord goofiness.
        FutureTask<?> initTask = new FutureTask<>(Executors.callable(() -> {
            this.bootstrap = new VotifierServerBootstrap(host, port, NuVotifier.this, disablev1);
            this.bootstrap.start(err -> {});
        }));
        getProxy().getScheduler().runAsync(this, initTask);
        try {
            initTask.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Unable to start server", e);
        }

        Configuration fwdCfg = configuration.getSection("forwarding");
        String fwdMethod = fwdCfg.getString("method", "none").toLowerCase();
        if ("none".equals(fwdMethod)) {
            getLogger().info("Method none selected for vote forwarding: Votes will not be forwarded to backend servers.");
        } else if ("pluginmessaging".equals(fwdMethod)) {
            String channel = fwdCfg.getString("pluginMessaging.channel", "NuVotifier");
            String cacheMethod = fwdCfg.getString("pluginMessaging.cache", "file").toLowerCase();
            VoteCache voteCache = null;
            if ("none".equals(cacheMethod)) {
                getLogger().info("Vote cache none selected for caching: votes that cannot be immediately delivered will be lost.");
            } else if ("memory".equals(cacheMethod)) {
                voteCache = new MemoryVoteCache(ProxyServer.getInstance().getServers().size());
                getLogger().info("Using in-memory cache for votes that are not able to be delivered.");
            } else if ("file".equals(cacheMethod)) {
                try {
                    voteCache = new FileVoteCache(
                            ProxyServer.getInstance().getServers().size(), this,
                            new File(getDataFolder(), fwdCfg.getString("pluginMessaging.file.name")),
                            fwdCfg.getInt("pluginMessaging.file.cacheTime", -1));
                } catch (IOException e) {
                    getLogger().log(Level.SEVERE, "Unload to load file cache. Votes will be lost!", e);
                }
            }
            if (!fwdCfg.getBoolean("pluginMessaging.onlySendToJoinedServer")) {

                List<String> ignoredServers = fwdCfg.getStringList("pluginMessaging.excludedServers");

                try {
                    forwardingMethod = new PluginMessagingForwardingSource(channel, ignoredServers, this, voteCache);
                    getLogger().info("Forwarding votes over PluginMessaging channel '" + channel + "' for vote forwarding!");
                } catch (RuntimeException e) {
                    getLogger().log(Level.SEVERE, "NuVotifier could not set up PluginMessaging for vote forwarding!", e);
                }
            } else {
                try {
                    String fallbackServer = fwdCfg.getString("pluginMessaging.joinedServerFallback", null);
                    if (fallbackServer != null && fallbackServer.isEmpty()) fallbackServer = null;
                    forwardingMethod = new OnlineForwardPluginMessagingForwardingSource(channel, this, voteCache, fallbackServer);
                } catch (RuntimeException e) {
                    getLogger().log(Level.SEVERE, "NuVotifier could not set up PluginMessaging for vote forwarding!", e);
                }
            }
        } else if ("proxy".equals(fwdMethod)) {
            Configuration serverSection = fwdCfg.getSection("proxy");
            List<ProxyForwardingVoteSource.BackendServer> serverList = new ArrayList<>();
            for (String s : serverSection.getKeys()) {
                Configuration section = serverSection.getSection(s);
                InetAddress address;
                try {
                    address = InetAddress.getByName(section.getString("address"));
                } catch (UnknownHostException e) {
                    getLogger().info("Address " + section.getString("address") + " couldn't be looked up. Ignoring!");
                    continue;
                }

                Key token = null;
                try {
                    token = KeyCreator.createKeyFrom(section.getString("token", section.getString("key")));
                } catch (IllegalArgumentException e) {
                    getLogger().log(Level.SEVERE,
                            "An exception occurred while attempting to add proxy target '" + s + "' - maybe your token is wrong? " +
                                    "Votes will not be forwarded to this server!", e);
                }

                if (token != null) {
                    ProxyForwardingVoteSource.BackendServer server = new ProxyForwardingVoteSource.BackendServer(s,
                            new InetSocketAddress(address, section.getInt("port")),
                            token);
                    serverList.add(server);
                }
            }

            forwardingMethod = new ProxyForwardingVoteSource(this, bootstrap::client, serverList, null);
            getLogger().info("Forwarding votes from this NuVotifier instance to another NuVotifier server.");
        } else {
            getLogger().severe("No vote forwarding method '" + fwdMethod + "' known. Defaulting to noop implementation.");
        }
    }

    @Override
    public void onEnable() {
        loadAndBind();

        getProxy().getPluginManager().registerCommand(this, new Command("nvreload") {
            @Override
            public void execute(CommandSender sender, String[] args) {
                if (sender instanceof ProxiedPlayer)
                    sender.sendMessage(ChatColor.RED + "For security and stability, only console may run this command!");
                else
                    reload();
            }
        });
    }

    private void halt() {
        // Shut down the network handlers.
        if (bootstrap != null) {
            bootstrap.shutdown();
        }

        if (forwardingMethod != null) {
            forwardingMethod.halt();
            forwardingMethod = null;
        }
    }

    public void reload() {
        try {
            halt();
        } catch (Exception ex) {
            getLogger().log(Level.SEVERE, "On halt, an exception was thrown. This may be fine!", ex);
        }

        try {
            loadAndBind();
            getLogger().info("Reload was successful.");
        } catch (Exception ex) {
            try {
                halt();
                getLogger().log(Level.SEVERE, "On reload, there was a problem with the configuration. Votifier currently does nothing!", ex);
            } catch (Exception ex2) {
                getLogger().log(Level.SEVERE, "On reload, there was a problem loading, and we could not re-halt the server. Votifier is in an unstable state!", ex);
                getLogger().log(Level.SEVERE, "(halt exception)", ex2);
            }
        }
    }

    @Override
    public void onDisable() {
        halt();
        getLogger().info("Votifier disabled.");
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

        getProxy().getScheduler().runAsync(this, () -> getProxy().getPluginManager().callEvent(new VotifierEvent(vote)));

        if (forwardingMethod != null) {
            getProxy().getScheduler().runAsync(this, () -> forwardingMethod.forward(vote));
        }
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
    public Map<String, Key> getTokens() {
        return tokens;
    }

    @Override
    public KeyPair getProtocolV1Key() {
        return keyPair;
    }

    @Override
    public String getVersion() {
        return getDescription().getVersion();
    }

    @Override
    public Logger getPluginLogger() {
        // Well, Bungee doesn't make it any easier on us to do this. We get to do something terrible and there's nothing
        // we can do about it!
        return DirtyTricks.getLogger(getLogger());
    }

    @Override
    public VotifierScheduler getScheduler() {
        return new BungeeScheduler(this);
    }

    public boolean isDebug() {
        return debug;
    }

    @Override
    public List<BackendServer> getAllBackendServers() {
        return getProxy().getServers().values().stream()
                .map(BungeeBackendServer::new)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<BackendServer> getServer(String name) {
        ServerInfo info = getProxy().getServerInfo(name);
        return Optional.ofNullable(info).map(BungeeBackendServer::new);
    }
}
