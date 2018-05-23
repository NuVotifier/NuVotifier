package com.vexsoftware.votifier.sponge;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.ByteStreams;
import com.google.inject.Inject;
import com.vexsoftware.votifier.VoteHandler;
import com.vexsoftware.votifier.model.Vote;
import com.vexsoftware.votifier.net.VotifierSession;
import com.vexsoftware.votifier.net.protocol.VoteInboundHandler;
import com.vexsoftware.votifier.net.protocol.VotifierGreetingHandler;
import com.vexsoftware.votifier.net.protocol.VotifierProtocolDifferentiator;
import com.vexsoftware.votifier.net.protocol.v1crypto.RSAIO;
import com.vexsoftware.votifier.net.protocol.v1crypto.RSAKeygen;
import com.vexsoftware.votifier.sponge.config.ConfigLoader;
import com.vexsoftware.votifier.sponge.config.SpongeConfig;
import com.vexsoftware.votifier.sponge.event.VotifierEvent;
import com.vexsoftware.votifier.sponge.forwarding.ForwardedVoteListener;
import com.vexsoftware.votifier.sponge.forwarding.ForwardingVoteSink;
import com.vexsoftware.votifier.sponge.forwarding.SpongePluginMessagingForwardingSink;
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
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import ninja.leaping.configurate.yaml.YAMLConfigurationLoader;
import org.slf4j.Logger;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GamePostInitializationEvent;
import org.spongepowered.api.event.game.state.GamePreInitializationEvent;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.event.game.state.GameStoppingServerEvent;
import org.spongepowered.api.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Key;
import java.security.KeyPair;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Plugin(id = "nuvotifier", name = "NuVotifier", version = "2.3.7", authors = "ParallelBlock LLC",
        description = "Safe, smart, and secure Votifier server plugin")
public class VotifierPlugin implements VoteHandler, com.vexsoftware.votifier.VotifierPlugin, ForwardedVoteListener {

    @Inject
    private Logger logger;

    @Inject
    @ConfigDir(sharedRoot = false)
    private File configDir;

    @Listener
    public void onServerStart(GamePreInitializationEvent event) {
        // Handle configuration.
        ConfigLoader cfgLoader = new ConfigLoader(this);

        // Load configuration.
        cfgLoader.loadConfig();

        /*
         * Create RSA directory and keys if it does not exist; otherwise, read
         * keys.
         */
        File rsaDirectory = new File(configDir, "rsa");
        try {
            if (!rsaDirectory.exists()) {
                rsaDirectory.mkdir();
                keyPair = RSAKeygen.generate(2048);
                RSAIO.save(rsaDirectory, keyPair);
            } else {
                keyPair = RSAIO.load(rsaDirectory);
            }
        } catch (Exception ex) {
            logger.error("Error creating or reading RSA tokens", ex);
            gracefulExit();
            return;
        }

        debug = cfgLoader.getSpongeConfig().debug;

        // Load Votifier tokens.
        cfgLoader.getSpongeConfig().tokens.forEach((s, s2) -> {
            tokens.put(s, KeyCreator.createKeyFrom(s2));
            logger.info("Loaded token for website: " + s);
        });

        // Initialize the receiver.
        final String host = cfgLoader.getSpongeConfig().host;
        final int port = cfgLoader.getSpongeConfig().port;
        if (debug)
            logger.info("DEBUG mode enabled!");

        if (port >= 0) {
            final boolean disablev1 = cfgLoader.getSpongeConfig().disableV1Protocol;
            if (disablev1) {
                logger.info("------------------------------------------------------------------------------");
                logger.info("Votifier protocol v1 parsing has been disabled. Most voting websites do not");
                logger.info("currently support the modern Votifier protocol in NuVotifier.");
                logger.info("------------------------------------------------------------------------------");
            }

            serverGroup = new NioEventLoopGroup(1);

            new ServerBootstrap()
                    .channel(NioServerSocketChannel.class)
                    .group(serverGroup)
                    .childHandler(new ChannelInitializer<NioSocketChannel>() {
                        @Override
                        protected void initChannel(NioSocketChannel channel) throws Exception {
                            channel.attr(VotifierSession.KEY).set(new VotifierSession());
                            channel.attr(com.vexsoftware.votifier.VotifierPlugin.KEY).set(VotifierPlugin.this);
                            channel.pipeline().addLast("greetingHandler", new VotifierGreetingHandler());
                            channel.pipeline().addLast("protocolDifferentiator", new VotifierProtocolDifferentiator(false, !disablev1));
                            channel.pipeline().addLast("voteHandler", new VoteInboundHandler(VotifierPlugin.this));
                        }
                    })
                    .bind(host, port)
                    .addListener((ChannelFutureListener) future -> {
                        if (future.isSuccess()) {
                            serverChannel = future.channel();
                            logger.info("Votifier enabled on socket " + serverChannel.localAddress() + ".");
                        } else {
                            SocketAddress socketAddress = future.channel().localAddress();
                            if (socketAddress == null) {
                                socketAddress = new InetSocketAddress(host, port);
                            }
                            logger.error("Votifier was not able to bind to " + socketAddress, future.cause());
                        }
                    });
        } else {
            getLogger().info("------------------------------------------------------------------------------");
            getLogger().info("Your Votifier port is less than 0, so we assume you do NOT want to start the");
            getLogger().info("votifier port server! Votifier will not listen for votes over any port, and");
            getLogger().info("will only listen for pluginMessaging forwarded votes!");
            getLogger().info("------------------------------------------------------------------------------");
        }

        if (cfgLoader.getSpongeConfig().forwarding != null) {
            String method = cfgLoader.getSpongeConfig().forwarding.method.toLowerCase(); //Default to lower case for case-insensitive searches
            if ("none".equals(method)) {
                getLogger().info("Method none selected for vote forwarding: Votes will not be received from a forwarder.");
            } else if ("pluginmessaging".equals(method)) {
                String channel = cfgLoader.getSpongeConfig().forwarding.pluginMessaging.channel;
                try {
                    forwardingMethod = new SpongePluginMessagingForwardingSink(this, channel, this);
                    getLogger().info("Receiving votes over PluginMessaging channel '" + channel + "'.");
                } catch (RuntimeException e) {
                    logger.error("NuVotifier could not set up PluginMessaging for vote forwarding!", e);
                }
            } else {
                logger.error("No vote forwarding method '" + method + "' known. Defaulting to noop implementation.");
            }
        }
    }

    @Listener
    public void onServerStop(GameStoppingServerEvent event) {
        if (serverGroup != null) {
            if (serverChannel != null)
                serverChannel.close();
            serverGroup.shutdownGracefully();
        }
        if (forwardingMethod != null)
            forwardingMethod.halt();

        logger.info("Votifier disabled.");
    }

    public Logger getLogger() {
        return logger;
    }

    /**
     * The current Votifier version.
     */
    private String version;

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

    private ForwardingVoteSink forwardingMethod;

    private void gracefulExit() {
        logger.error("Votifier did not initialize properly!");
    }

    /**
     * Gets the version.
     *
     * @return The version
     */
    public String getVersion() {
        return version;
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

    public File getConfigDir() {
        return configDir;
    }

    @Override
    public void onVoteReceived(Channel channel, final Vote vote, VotifierSession.ProtocolVersion protocolVersion) throws Exception {
        if (debug) {
            if (protocolVersion == VotifierSession.ProtocolVersion.ONE) {
                logger.info("Got a protocol v1 vote record from " + channel.remoteAddress() + " -> " + vote);
            } else {
                logger.info("Got a protocol v2 vote record from " + channel.remoteAddress() + " -> " + vote);
            }
        }
        Sponge.getScheduler().createTaskBuilder()
                .execute(new Runnable() {
                    @Override
                    public void run() {
                        VotifierEvent event = new VotifierEvent(vote, Sponge.getCauseStackManager().getCurrentCause());
                        Sponge.getEventManager().post(event);
                    }
                })
                .submit(this);
    }

    @Override
    public void onError(Channel channel, boolean alreadyHandledVote, Throwable throwable) {
        if (debug) {
            if (alreadyHandledVote) {
                logger.error("Vote processed, however an exception " +
                        "occurred with a vote from " + channel.remoteAddress(), throwable);
            } else {
                logger.error("Unable to process vote from " + channel.remoteAddress(), throwable);
            }
        } else if (!alreadyHandledVote) {
            logger.error("Unable to process vote from " + channel.remoteAddress());
        }
    }

    @Override
    public void onForward(final Vote v) {
        if (debug) {
            logger.info("Got a forwarded vote -> " + v);
        }
        Sponge.getScheduler().createTaskBuilder()
                .execute(new Runnable() {
                    @Override
                    public void run() {
                        VotifierEvent event = new VotifierEvent(v, Sponge.getCauseStackManager().getCurrentCause());
                        Sponge.getEventManager().post(event);
                    }
                })
                .submit(this);
    }
}
