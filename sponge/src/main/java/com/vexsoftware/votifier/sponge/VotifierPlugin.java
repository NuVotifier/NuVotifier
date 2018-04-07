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
    private Logger logger;
    @Inject
    @ConfigDir(sharedRoot = false)
    private Path baseDirectory;

    @Inject
    public VotifierPlugin(Logger logger) {
        this.logger = logger;
    }

    @Listener
    public void onServerStart(GameStartedServerEvent event) {
        // Set the plugin version.
        version = this.getClass().getAnnotation(Plugin.class).version();

        // Handle configuration.
        Path config = baseDirectory.resolve("config.yml");
        File rsaDirectory = baseDirectory.resolve("rsa").toFile();

        /*
         * Use IP address from server.properties as a default for
         * configurations. Do not use InetAddress.getLocalHost() as it most
         * likely will return the main server address instead of the address
         * assigned to the server.
         */
        String hostAddr;
        Optional<InetSocketAddress> address = Sponge.getServer().getBoundAddress();
        if (address.isPresent())
            hostAddr = address.get().getAddress().getHostAddress();
        else
            hostAddr = "0.0.0.0";

        /*
         * Create configuration file if it does not exists; otherwise, load it
         */
        try {
            Files.createDirectories(baseDirectory);
        } catch (FileAlreadyExistsException ignored) {
        } catch (IOException e) {
            throw new RuntimeException("Can't create base directory", e);
        }

        if (!Files.exists(config)) {
            try {
                // First time run - do some initialization.
                logger.info("Configuring Votifier for the first time...");

                // Load and manually replace variables in the configuration.
                String cfgStr = new String(ByteStreams.toByteArray(this.getClass().getResourceAsStream("spongeConfig.yml")),
                        StandardCharsets.UTF_8);
                String token = TokenUtil.newToken();
                cfgStr = cfgStr.replace("%default_token%", token).replace("%ip%", hostAddr);
                Files.write(config, ImmutableList.<CharSequence>of(cfgStr), StandardCharsets.UTF_8);

                /*
                 * Remind hosted server admins to be sure they have the right
                 * port number.
                 */
                logger.info("------------------------------------------------------------------------------");
                logger.info("Assigning NuVotifier to listen on port 8192. If you are hosting Craftbukkit on a");
                logger.info("shared server please check with your hosting provider to verify that this port");
                logger.info("is available for your use. Chances are that your hosting provider will assign");
                logger.info("a different port, which you need to specify in config.yml");
                logger.info("------------------------------------------------------------------------------");
                logger.info("Your default NuVotifier token is " + token + ".");
                logger.info("You will need to provide this token when you submit your server to a voting");
                logger.info("list.");
                logger.info("------------------------------------------------------------------------------");
            } catch (Exception ex) {
                logger.error("Error creating configuration file", ex);
                gracefulExit();
                return;
            }
        }

        // Load configuration.
        ConfigurationLoader loader = YAMLConfigurationLoader.builder()
                .setPath(config)
                .build();
        ConfigurationNode node;
        try {
            node = loader.load();
        } catch (IOException e) {
            logger.error("Error loading configuration file", e);
            gracefulExit();
            return;
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
            logger.error("Error creating or reading RSA tokens", ex);
            gracefulExit();
            return;
        }

        debug = node.getNode("debug").getBoolean(false);

// Load Votifier tokens.
        ConfigurationNode tokenSection = node.getNode("tokens");

        if (tokenSection.hasMapChildren()) {
            for (Map.Entry<Object, ? extends ConfigurationNode> entry : tokenSection.getChildrenMap().entrySet()) {
                if (entry.getKey() instanceof String) {
                    tokens.put((String) entry.getKey(), KeyCreator.createKeyFrom(entry.getValue().getString()));
                    logger.info("Loaded token for website: " + entry.getKey());
                }
            }
        } else {
            String token = TokenUtil.newToken();
            tokenSection.setValue(ImmutableMap.of("default", token));
            tokens.put("default", KeyCreator.createKeyFrom(token));
            try {
                loader.save(node);
            } catch (IOException e) {
                logger.error("Error generating Votifier token", e);
                gracefulExit();
                return;
            }
            logger.info("------------------------------------------------------------------------------");
            logger.info("No tokens were found in your configuration, so we've generated one for you.");
            logger.info("Your default Votifier token is " + token + ".");
            logger.info("You will need to provide this token when you submit your server to a voting");
            logger.info("list.");
            logger.info("------------------------------------------------------------------------------");
        }

        // Initialize the receiver.
        final String host = node.getNode("host").getString(hostAddr);
        final int port = node.getNode("port").getInt(8192);
        if (debug)
            logger.info("DEBUG mode enabled!");

        if (port >= 0) {
            final boolean disablev1 = node.getNode("disable-v1-protocol").getBoolean(false);
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
                    .addListener(new ChannelFutureListener() {
                        @Override
                        public void operationComplete(ChannelFuture future) throws Exception {
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
                        }
                    });
        } else {
            getLogger().info("------------------------------------------------------------------------------");
            getLogger().info("Your Votifier port is less than 0, so we assume you do NOT want to start the");
            getLogger().info("votifier port server! Votifier will not listen for votes over any port, and");
            getLogger().info("will only listen for pluginMessaging forwarded votes!");
            getLogger().info("------------------------------------------------------------------------------");
        }

        ConfigurationNode forwardingConfig = node.getNode("forwarding");
        if (forwardingConfig != null) {
            String method = forwardingConfig.getNode("method").getString("none").toLowerCase(); //Default to lower case for case-insensitive searches
            if ("none".equals(method)) {
                getLogger().info("Method none selected for vote forwarding: Votes will not be received from a forwarder.");
            } else if ("pluginmessaging".equals(method)) {
                String channel = forwardingConfig.getNode("pluginMessaging").getNode("channel").getString("NuVotifier");
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
