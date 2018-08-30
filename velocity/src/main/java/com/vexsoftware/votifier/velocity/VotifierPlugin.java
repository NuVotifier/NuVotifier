package com.vexsoftware.votifier.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import com.vexsoftware.votifier.VoteHandler;
import com.vexsoftware.votifier.model.Vote;
import com.vexsoftware.votifier.net.VotifierSession;
import com.vexsoftware.votifier.net.protocol.VoteInboundHandler;
import com.vexsoftware.votifier.net.protocol.VotifierGreetingHandler;
import com.vexsoftware.votifier.net.protocol.VotifierProtocolDifferentiator;
import com.vexsoftware.votifier.net.protocol.v1crypto.RSAIO;
import com.vexsoftware.votifier.net.protocol.v1crypto.RSAKeygen;
import com.vexsoftware.votifier.util.KeyCreator;
import com.vexsoftware.votifier.velocity.event.VotifierEvent;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.slf4j.Logger;

import java.io.File;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.security.Key;
import java.security.KeyPair;
import java.util.HashMap;
import java.util.Map;

@Plugin(id = "nuvotifier", name = "NuVotifier", version = "2.3.7", authors = "ParallelBlock LLC",
        description = "Safe, smart, and secure Votifier server plugin")
public class VotifierPlugin implements VoteHandler, com.vexsoftware.votifier.platform.VotifierPlugin {

    @Inject
    public Logger logger;

    @Inject
    @DataDirectory
    public File configDir;

    @Inject
    public ProxyServer server;

    @Subscribe
    public void onServerStart(ProxyInitializeEvent event) {
        // Load configuration.
        ConfigLoader.loadConfig(this);

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

        debug = ConfigLoader.getSpongeConfig().debug;

        // Load Votifier tokens.
        ConfigLoader.getSpongeConfig().tokens.forEach((s, s2) -> {
            tokens.put(s, KeyCreator.createKeyFrom(s2));
            logger.info("Loaded token for website: " + s);
        });

        // Initialize the receiver.
        final String host = ConfigLoader.getSpongeConfig().host;
        final int port = ConfigLoader.getSpongeConfig().port;
        if (debug)
            logger.info("DEBUG mode enabled!");

        if (port >= 0) {
            final boolean disablev1 = ConfigLoader.getSpongeConfig().disableV1Protocol;
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
                        protected void initChannel(NioSocketChannel channel) {
                            channel.attr(VotifierSession.KEY).set(new VotifierSession());
                            channel.attr(VotifierPlugin.KEY).set(VotifierPlugin.this);
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

        if (ConfigLoader.getSpongeConfig().forwarding != null) {
            String method = ConfigLoader.getSpongeConfig().forwarding.method.toLowerCase(); //Default to lower case for case-insensitive searches
            if ("none".equals(method)) {
                getLogger().info("Method none selected for vote forwarding: Votes will not be received from a forwarder.");
            } else if ("pluginmessaging".equals(method)) {
                String channel = ConfigLoader.getSpongeConfig().forwarding.pluginMessaging.channel;
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

    @Subscribe
    public void onServerStop(ProxyShutdownEvent event) {
        if (serverGroup != null) {
            try {
                if (serverChannel != null)
                    serverChannel.close().sync();
                serverGroup.shutdownGracefully().sync();
            } catch (Exception e) {
                logger.error("Unable to shut down listening port gracefully.", e);
            }
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
    public void onVoteReceived(Channel channel, final Vote vote, VotifierSession.ProtocolVersion protocolVersion) {
        if (debug) {
            if (protocolVersion == VotifierSession.ProtocolVersion.ONE) {
                logger.info("Got a protocol v1 vote record from " + channel.remoteAddress() + " -> " + vote);
            } else {
                logger.info("Got a protocol v2 vote record from " + channel.remoteAddress() + " -> " + vote);
            }
        }

        server.getEventManager().fireAndForget(new VotifierEvent(vote));
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
}