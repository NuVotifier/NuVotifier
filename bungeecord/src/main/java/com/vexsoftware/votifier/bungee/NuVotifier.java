package com.vexsoftware.votifier.bungee;

import com.google.common.collect.ImmutableMap;
import com.google.common.io.ByteStreams;
import com.google.common.io.Files;
import com.vexsoftware.votifier.VoteHandler;
import com.vexsoftware.votifier.VotifierPlugin;
import com.vexsoftware.votifier.bungee.commands.ReloadCommand;
import com.vexsoftware.votifier.bungee.events.VotifierEvent;
import com.vexsoftware.votifier.bungee.forwarding.ForwardingVoteSource;
import com.vexsoftware.votifier.bungee.forwarding.OnlineForwardPluginMessagingForwardingSource;
import com.vexsoftware.votifier.bungee.forwarding.PluginMessagingForwardingSource;
import com.vexsoftware.votifier.bungee.forwarding.cache.FileVoteCache;
import com.vexsoftware.votifier.bungee.forwarding.cache.MemoryVoteCache;
import com.vexsoftware.votifier.bungee.forwarding.cache.VoteCache;
import com.vexsoftware.votifier.bungee.forwarding.proxy.ProxyForwardingVoteSource;
import com.vexsoftware.votifier.model.Vote;
import com.vexsoftware.votifier.net.VotifierSession;
import com.vexsoftware.votifier.net.protocol.VoteInboundHandler;
import com.vexsoftware.votifier.net.protocol.VotifierGreetingHandler;
import com.vexsoftware.votifier.net.protocol.VotifierProtocolDifferentiator;
import com.vexsoftware.votifier.net.protocol.v1crypto.RSAIO;
import com.vexsoftware.votifier.net.protocol.v1crypto.RSAKeygen;
import com.vexsoftware.votifier.util.KeyCreator;
import com.vexsoftware.votifier.util.TokenUtil;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.logging.Level;

public class NuVotifier extends Plugin implements VoteHandler, VotifierPlugin {

    /**
     * The server channel.
     */
    private Channel serverChannel;

    /**
     * The event group handling the channel.
     */
    private NioEventLoopGroup serverGroup;

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

    @Override
    public void onEnable() {
        reloadConfigs();
        getProxy().getPluginManager().registerCommand(this, new ReloadCommand(this));

    }

    public boolean reloadConfigs(){
        if (serverGroup != null) {
            if (serverChannel != null) {
                serverChannel.close();
                serverChannel = null;
            }
            serverGroup.shutdownGracefully();
            serverGroup = null;
        }

        if (forwardingMethod != null) {
            forwardingMethod.halt();
            forwardingMethod = null;
        }

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
                Files.write(cfgStr, config, StandardCharsets.UTF_8);

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
        FutureTask<?> initTask = new FutureTask<>(Executors.callable(new Runnable() {
            @Override
            public void run() {
                serverGroup = new NioEventLoopGroup(2);

                new ServerBootstrap()
                        .channel(NioServerSocketChannel.class)
                        .group(serverGroup)
                        .childHandler(new ChannelInitializer<NioSocketChannel>() {
                            @Override
                            protected void initChannel(NioSocketChannel channel) throws Exception {
                                channel.attr(VotifierSession.KEY).set(new VotifierSession());
                                channel.attr(VotifierPlugin.KEY).set(NuVotifier.this);
                                channel.pipeline().addLast("greetingHandler", new VotifierGreetingHandler());
                                channel.pipeline().addLast("protocolDifferentiator", new VotifierProtocolDifferentiator(false, !disablev1));
                                channel.pipeline().addLast("voteHandler", new VoteInboundHandler(NuVotifier.this));
                            }
                        })
                        .bind(host, port)
                        .addListener(new ChannelFutureListener() {
                            @Override
                            public void operationComplete(ChannelFuture future) throws Exception {
                                if (future.isSuccess()) {
                                    serverChannel = future.channel();
                                    getLogger().info("Votifier enabled on socket "+serverChannel.localAddress()+".");
                                } else {
                                    SocketAddress socketAddress = future.channel().localAddress();
                                    if (socketAddress == null) {
                                        socketAddress = new InetSocketAddress(host, port);
                                    }
                                    getLogger().log(Level.SEVERE, "Votifier was not able to bind to " + socketAddress, future.cause());
                                }
                            }
                        });
            }
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
                    voteCache = new FileVoteCache(ProxyServer.getInstance().getServers().size(), this, new File(getDataFolder(),
                            fwdCfg.getString("pluginMessaging.file.name")));
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
                ProxyForwardingVoteSource.BackendServer server = new ProxyForwardingVoteSource.BackendServer(s,
                        new InetSocketAddress(address, section.getShort("port")),
                        KeyCreator.createKeyFrom(section.getString("token",section.getString("key"))));
                serverList.add(server);
            }

            forwardingMethod = new ProxyForwardingVoteSource(this, serverGroup, serverList, null);
            getLogger().info("Forwarding votes from this NuVotifier instance to another NuVotifier server.");
        } else {
            getLogger().severe("No vote forwarding method '" + fwdMethod + "' known. Defaulting to noop implementation.");
        }

        return true;
    }



    @Override
    public void onDisable() {
        // Shut down the network handlers.
        if (serverChannel != null)
            serverChannel.close();
        serverGroup.shutdownGracefully();

        if (forwardingMethod != null) {
            forwardingMethod.halt();
        }

        getLogger().info("Votifier disabled.");
    }


    @Override
    public void onVoteReceived(Channel channel, final Vote vote, VotifierSession.ProtocolVersion protocolVersion) throws Exception {
        if (debug) {
            if (protocolVersion == VotifierSession.ProtocolVersion.ONE) {
                getLogger().info("Got a protocol v1 vote record from " + channel.remoteAddress() + " -> " + vote);
            } else {
                getLogger().info("Got a protocol v2 vote record from " + channel.remoteAddress() + " -> " + vote);
            }
        }

        getProxy().getScheduler().runAsync(this, new Runnable() {
            @Override
            public void run() {
                getProxy().getPluginManager().callEvent(new VotifierEvent(vote));
            }
        });

        if (forwardingMethod != null) {
            getProxy().getScheduler().runAsync(this, new Runnable() {
                @Override
                public void run() {
                    forwardingMethod.forward(vote);
                }
            });
        }
    }

    @Override
    public void onError(Channel channel, Throwable throwable) {
        if (debug) {
            getLogger().log(Level.SEVERE, "Unable to process vote from " + channel.remoteAddress(), throwable);
        } else {
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

    public boolean isDebug() {
        return debug;
    }
}
