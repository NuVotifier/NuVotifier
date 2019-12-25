package com.vexsoftware.votifier.sponge;

import com.google.inject.Inject;
import com.vexsoftware.votifier.VoteHandler;
import com.vexsoftware.votifier.model.Vote;
import com.vexsoftware.votifier.net.VotifierServerBootstrap;
import com.vexsoftware.votifier.net.VotifierSession;
import com.vexsoftware.votifier.net.protocol.v1crypto.RSAIO;
import com.vexsoftware.votifier.net.protocol.v1crypto.RSAKeygen;
import com.vexsoftware.votifier.platform.LoggingAdapter;
import com.vexsoftware.votifier.platform.scheduler.VotifierScheduler;
import com.vexsoftware.votifier.sponge.config.ConfigLoader;
import com.vexsoftware.votifier.sponge.event.VotifierEvent;
import com.vexsoftware.votifier.sponge.forwarding.SpongePluginMessagingForwardingSink;
import com.vexsoftware.votifier.support.forwarding.ForwardedVoteListener;
import com.vexsoftware.votifier.support.forwarding.ForwardingVoteSink;
import com.vexsoftware.votifier.util.KeyCreator;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.event.game.state.GameStoppingServerEvent;
import org.spongepowered.api.plugin.Plugin;

import java.io.File;
import java.security.Key;
import java.security.KeyPair;
import java.util.HashMap;
import java.util.Map;

@Plugin(id = "nuvotifier", name = "NuVotifier", version="SOMEONE_FUCKED_UP_THE_BUILD", authors = "Ichbinjoe",
        description = "Safe, smart, and secure Votifier server plugin")
public class VotifierPlugin implements VoteHandler, com.vexsoftware.votifier.platform.VotifierPlugin, ForwardedVoteListener {

    @Inject
    public Logger logger;

    private SLF4JLogger loggerAdapter;

    @Inject
    @ConfigDir(sharedRoot = false)
    public File configDir;

    private VotifierScheduler scheduler;

    @Listener
    public void onServerStart(GameStartedServerEvent event) {
        this.scheduler = new SpongeScheduler(this);
        this.loggerAdapter = new SLF4JLogger(logger);

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

        if (!debug)
            logger.info("QUIET mode enabled!");

        if (port >= 0) {
            final boolean disablev1 = ConfigLoader.getSpongeConfig().disableV1Protocol;
            if (disablev1) {
                logger.info("------------------------------------------------------------------------------");
                logger.info("Votifier protocol v1 parsing has been disabled. Most voting websites do not");
                logger.info("currently support the modern Votifier protocol in NuVotifier.");
                logger.info("------------------------------------------------------------------------------");
            }

            this.bootstrap = new VotifierServerBootstrap(host, port, this, disablev1);
            this.bootstrap.start(err -> {});
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

    @Listener
    public void onServerStop(GameStoppingServerEvent event) {
        if (bootstrap != null) {
            bootstrap.shutdown();
            bootstrap = null;
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

    private void gracefulExit() {
        logger.error("Votifier did not initialize properly!");
    }

    @Override
    public LoggingAdapter getPluginLogger() {
        return loggerAdapter;
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
        Sponge.getScheduler().createTaskBuilder()
                .execute(() -> {
                    VotifierEvent event = new VotifierEvent(vote, Sponge.getCauseStackManager().getCurrentCause());
                    Sponge.getEventManager().post(event);
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
                .execute(() -> {
                    VotifierEvent event = new VotifierEvent(v, Sponge.getCauseStackManager().getCurrentCause());
                    Sponge.getEventManager().post(event);
                })
                .submit(this);
    }
}
