package com.vexsoftware.votifier.sponge;

import com.google.inject.Inject;
import com.vexsoftware.votifier.VoteHandler;
import com.vexsoftware.votifier.model.Vote;
import com.vexsoftware.votifier.net.VotifierServerBootstrap;
import com.vexsoftware.votifier.net.VotifierSession;
import com.vexsoftware.votifier.net.protocol.v1crypto.RSAIO;
import com.vexsoftware.votifier.net.protocol.v1crypto.RSAKeygen;
import com.vexsoftware.votifier.platform.LoggingAdapter;
import com.vexsoftware.votifier.platform.VotifierPlugin;
import com.vexsoftware.votifier.platform.scheduler.VotifierScheduler;
import com.vexsoftware.votifier.sponge.cmd.NVReloadCmd;
import com.vexsoftware.votifier.sponge.cmd.TestVoteCmd;
import com.vexsoftware.votifier.sponge.config.ConfigLoader;
import com.vexsoftware.votifier.sponge.event.VotifierEvent;
import com.vexsoftware.votifier.sponge.forwarding.SpongePluginMessagingForwardingSink;
import com.vexsoftware.votifier.support.forwarding.ForwardedVoteListener;
import com.vexsoftware.votifier.support.forwarding.ForwardingVoteSink;
import com.vexsoftware.votifier.support.forwarding.redis.*;
import com.vexsoftware.votifier.util.KeyCreator;
import org.slf4j.Logger;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.GameReloadEvent;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.event.game.state.GameStoppingServerEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.text.Text;

import java.io.File;
import java.security.Key;
import java.security.KeyPair;
import java.util.HashMap;
import java.util.Map;

@Plugin(id = "nuvotifier", name = "NuVotifier", version = "@version@", authors = "Ichbinjoe",
        description = "Safe, smart, and secure Votifier server plugin")
public class NuVotifier implements VoteHandler, VotifierPlugin, ForwardedVoteListener {

    @Inject
    public Logger logger;

    private SLF4JLogger loggerAdapter;

    @Inject
    @ConfigDir(sharedRoot = false)
    public File configDir;

    private VotifierScheduler scheduler;

    private boolean loadAndBind() {
        // Load configuration.
        ConfigLoader.loadConfig(this);

        /*
         * Create RSA directory and keys if it does not exist; otherwise, read
         * keys.
         */
        File rsaDirectory = new File(configDir, "rsa");
        try {
            if (!rsaDirectory.exists()) {
                if (!rsaDirectory.mkdir()) {
                    throw new RuntimeException("Unable to create the RSA key folder " + rsaDirectory);
                }
                keyPair = RSAKeygen.generate(2048);
                RSAIO.save(rsaDirectory, keyPair);
            } else {
                keyPair = RSAIO.load(rsaDirectory);
            }
        } catch (Exception ex) {
            logger.error("Error creating or reading RSA tokens", ex);
            return false;
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
            this.bootstrap.start(err -> {
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
            } else if ("redis".equals(method)) {

                // Load redis credentials
                RedisCredentials redisCredentials = RedisCredentials.builder()
                        .host(ConfigLoader.getSpongeConfig().forwarding.redis.address)
                        .port(ConfigLoader.getSpongeConfig().forwarding.redis.port)
                        .password(ConfigLoader.getSpongeConfig().forwarding.redis.password)
                        .channel(ConfigLoader.getSpongeConfig().forwarding.redis.channel)
                        .build();

                // Load redis pool parameters
                RedisPoolConfiguration redisPoolConfiguration = RedisPoolConfiguration.builder()
                        .timeout(ConfigLoader.getSpongeConfig().forwarding.redis.poolSettings.timeout)
                        .maxTotal(ConfigLoader.getSpongeConfig().forwarding.redis.poolSettings.maxTotal)
                        .maxIdle(ConfigLoader.getSpongeConfig().forwarding.redis.poolSettings.maxIdle)
                        .minIdle(ConfigLoader.getSpongeConfig().forwarding.redis.poolSettings.minIdle)
                        .minEvictableIdleTime(ConfigLoader.getSpongeConfig().forwarding.redis.poolSettings.minEvictableIdleTime)
                        .timeBetweenEvictionRuns(ConfigLoader.getSpongeConfig().forwarding.redis.poolSettings.timeBetweenEvictionRuns)
                        .numTestsPerEvictionRun(ConfigLoader.getSpongeConfig().forwarding.redis.poolSettings.numTestsPerEvictionRun)
                        .blockWhenExhausted(ConfigLoader.getSpongeConfig().forwarding.redis.poolSettings.blockWhenExhausted)
                        .build();

                forwardingMethod = new RedisForwardingSink(redisCredentials, redisPoolConfiguration, this);
            } else {
                logger.error("No vote forwarding method '" + method + "' known. Defaulting to noop implementation.");
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

    public boolean reload() {
        try {
            halt();
        } catch (Exception ex) {
            logger.error("On halt, an exception was thrown. This may be fine!", ex);
        }

        if (loadAndBind()) {
            logger.info("Reload was successful.");
            return true;
        } else {
            try {
                halt();
                logger.error("On reload, there was a problem with the configuration. Votifier currently does nothing!");
            } catch (Exception ex) {
                logger.error("On reload, there was a problem loading, and we could not re-halt the server. Votifier is in an unstable state!", ex);
            }
            return false;
        }
    }

    @Listener
    public void onServerStart(GameStartedServerEvent event) {
        this.scheduler = new SpongeScheduler(this);
        this.loggerAdapter = new SLF4JLogger(logger);

        CommandSpec nvreloadSpec = CommandSpec.builder()
                .description(Text.of("Reloads NuVotifier"))
                .permission("nuvotifier.reload")
                .executor(new NVReloadCmd(this)).build();

        Sponge.getCommandManager().register(this, nvreloadSpec, "nvreload");

        CommandSpec testvoteSpec = CommandSpec.builder()
                .arguments(GenericArguments.allOf(GenericArguments.string(Text.of("args"))))
                .description(Text.of("Sends a test vote to the server's listeners"))
                .permission("nuvotifier.testvote")
                .executor(new TestVoteCmd(this)).build();

        Sponge.getCommandManager().register(this, testvoteSpec, "testvote");

        if (!loadAndBind()) {
            gracefulExit();
        }
    }

    @Listener
    public void onGameReload(GameReloadEvent event) {
        this.reload();
    }

    @Listener
    public void onServerStop(GameStoppingServerEvent event) {
        halt();
        logger.info("Votifier disabled.");
    }

    public Logger getLogger() {
        return logger;
    }

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
    public void onVoteReceived(final Vote vote, VotifierSession.ProtocolVersion protocolVersion, String remoteAddress) {
        if (debug) {
            if (protocolVersion == VotifierSession.ProtocolVersion.ONE) {
                logger.info("Got a protocol v1 vote record from " + remoteAddress + " -> " + vote);
            } else {
                logger.info("Got a protocol v2 vote record from " + remoteAddress + " -> " + vote);
            }
        }
        this.fireVoteEvent(vote);
    }

    @Override
    public void onError(Throwable throwable, boolean alreadyHandledVote, String remoteAddress) {
        if (debug) {
            if (alreadyHandledVote) {
                logger.warn("Vote processed, however an exception " +
                        "occurred with a vote from " + remoteAddress, throwable);
            } else {
                logger.warn("Unable to process vote from " + remoteAddress, throwable);
            }
        } else if (!alreadyHandledVote) {
            logger.warn("Unable to process vote from " + remoteAddress);
        }
    }

    @Override
    public void onForward(final Vote v) {
        if (debug) {
            logger.info("Got a forwarded vote -> " + v);
        }
        fireVoteEvent(v);
    }

    private void fireVoteEvent(final Vote vote) {
        Sponge.getScheduler().createTaskBuilder()
                .execute(() -> {
                    VotifierEvent event = new VotifierEvent(vote, Sponge.getCauseStackManager().getCurrentCause());
                    Sponge.getEventManager().post(event);
                })
                .submit(this);
    }
}
