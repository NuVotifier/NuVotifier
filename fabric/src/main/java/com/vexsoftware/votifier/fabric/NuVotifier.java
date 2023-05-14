package com.vexsoftware.votifier.fabric;

import com.vexsoftware.votifier.VoteHandler;
import com.vexsoftware.votifier.fabric.cmd.NuVotifierCommand;
import com.vexsoftware.votifier.fabric.config.ConfigLoader;
import com.vexsoftware.votifier.fabric.event.VoteListener;
import com.vexsoftware.votifier.fabric.forwarding.FabricMessagingForwardingSink;
import com.vexsoftware.votifier.model.Vote;
import com.vexsoftware.votifier.net.VotifierServerBootstrap;
import com.vexsoftware.votifier.net.VotifierSession;
import com.vexsoftware.votifier.net.protocol.v1crypto.RSAIO;
import com.vexsoftware.votifier.net.protocol.v1crypto.RSAKeygen;
import com.vexsoftware.votifier.platform.LoggingAdapter;
import com.vexsoftware.votifier.platform.VotifierPlugin;
import com.vexsoftware.votifier.platform.scheduler.ScheduledExecutorServiceVotifierScheduler;
import com.vexsoftware.votifier.platform.scheduler.VotifierScheduler;
import com.vexsoftware.votifier.support.forwarding.ForwardedVoteListener;
import com.vexsoftware.votifier.support.forwarding.ForwardingVoteSink;
import com.vexsoftware.votifier.util.KeyCreator;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.security.Key;
import java.security.KeyPair;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

public class NuVotifier implements VoteHandler, VotifierPlugin, ForwardedVoteListener, DedicatedServerModInitializer {

    public static final Logger LOGGER = LoggerFactory.getLogger("nuvotifier");

    private SLF4JLogger loggerAdapter;


    public File configDir = FabricLoader.getInstance().getConfigDir().toFile();

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
            LOGGER.error("Error creating or reading RSA tokens", ex);
            return false;
        }

        debug = ConfigLoader.getFabricConfig().debug;

        // Load Votifier tokens.
        ConfigLoader.getFabricConfig().tokens.forEach((s, s2) -> {
            tokens.put(s, KeyCreator.createKeyFrom(s2));
            LOGGER.info("Loaded token for website: " + s);
        });

        // Initialize the receiver.
        final String host = ConfigLoader.getFabricConfig().host;
        final int port = ConfigLoader.getFabricConfig().port;

        if (!debug)
            LOGGER.info("QUIET mode enabled!");

        if (port >= 0) {
            final boolean disablev1 = ConfigLoader.getFabricConfig().disableV1Protocol;
            if (disablev1) {
                LOGGER.info("------------------------------------------------------------------------------");
                LOGGER.info("Votifier protocol v1 parsing has been disabled. Most voting websites do not");
                LOGGER.info("currently support the modern Votifier protocol in NuVotifier.");
                LOGGER.info("------------------------------------------------------------------------------");
            }

            this.bootstrap = new VotifierServerBootstrap(host, port, this, disablev1);
            this.bootstrap.start(err -> {
            });
        } else {
            LOGGER.info("------------------------------------------------------------------------------");
            LOGGER.info("Your Votifier port is less than 0, so we assume you do NOT want to start the");
            LOGGER.info("votifier port server! Votifier will not listen for votes over any port, and");
            LOGGER.info("will only listen for pluginMessaging forwarded votes!");
            LOGGER.info("------------------------------------------------------------------------------");
        }

        if (ConfigLoader.getFabricConfig().forwarding != null) {
            String method = ConfigLoader.getFabricConfig().forwarding.method.toLowerCase(); //Default to lower case for case-insensitive searches
            if ("none".equals(method)) {
                LOGGER.info("Method none selected for vote forwarding: Votes will not be received from a forwarder.");
            } else if ("pluginmessaging".equals(method)) {
                String channel = ConfigLoader.getFabricConfig().forwarding.pluginMessaging.channel;
                try {
                    forwardingMethod = new FabricMessagingForwardingSink(channel, this);
                    LOGGER.info("Receiving votes over PluginMessaging channel '" + channel + "'.");
                } catch (RuntimeException e) {
                    LOGGER.error("NuVotifier could not set up PluginMessaging for vote forwarding!", e);
                }
            } else {
                LOGGER.error("No vote forwarding method '" + method + "' known. Defaulting to noop implementation.");
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
            LOGGER.error("On halt, an exception was thrown. This may be fine!", ex);
        }

        if (loadAndBind()) {
            LOGGER.info("Reload was successful.");
            return true;
        } else {
            try {
                halt();
                LOGGER.error("On reload, there was a problem with the configuration. Votifier currently does nothing!");
            } catch (Exception ex) {
                LOGGER.error("On reload, there was a problem loading, and we could not re-halt the server. Votifier is in an unstable state!", ex);
            }
            return false;
        }
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
    private final Map<String, Key> tokens = new HashMap<>();

    private ForwardingVoteSink forwardingMethod;

    private MinecraftServer server;

    @Override
    public void onInitializeServer() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> NuVotifierCommand.register(this, dispatcher));
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resourceManager, success) -> this.reload());
        ServerLifecycleEvents.SERVER_STARTING.register(this::onServerStart);
        ServerLifecycleEvents.SERVER_STOPPING.register(this::onServerStop);
    }

    private void onServerStart(MinecraftServer server) {
        this.server = server;
        this.scheduler = new ScheduledExecutorServiceVotifierScheduler(Executors.newScheduledThreadPool(1));
        this.loggerAdapter = new SLF4JLogger(LOGGER);
        if (!loadAndBind()) {
            LOGGER.error("Votifier did not initialize properly!");
        }
    }

    private void onServerStop(MinecraftServer server) {
        halt();
        LOGGER.info("Votifier disabled.");
    }

    public File getConfigDir() {
        return configDir;
    }

    @Override
    public void onVoteReceived(Vote vote, VotifierSession.ProtocolVersion protocolVersion, String remoteAddress) {
        if (debug) {
            if (protocolVersion == VotifierSession.ProtocolVersion.ONE) {
                LOGGER.info("Got a protocol v1 vote record from " + remoteAddress + " -> " + vote);
            } else {
                LOGGER.info("Got a protocol v2 vote record from " + remoteAddress + " -> " + vote);
            }
        }
        this.fireVoteEvent(vote);
    }

    @Override
    public void onError(Throwable throwable, boolean alreadyHandledVote, String remoteAddress) {
        if (debug) {
            if (alreadyHandledVote) {
                LOGGER.warn("Vote processed, however an exception " +
                        "occurred with a vote from " + remoteAddress, throwable);
            } else {
                LOGGER.warn("Unable to process vote from " + remoteAddress, throwable);
            }
        } else if (!alreadyHandledVote) {
            LOGGER.warn("Unable to process vote from " + remoteAddress);
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
    public LoggingAdapter getPluginLogger() {
        return loggerAdapter;
    }

    @Override
    public VotifierScheduler getScheduler() {
        return scheduler;
    }

    @Override
    public boolean isDebug() {
        return debug;
    }

    @Override
    public void onForward(final Vote v) {
        if (debug) {
            LOGGER.info("Got a forwarded vote -> " + v);
        }
        fireVoteEvent(v);
    }

    private void fireVoteEvent(final Vote vote) {
        server.submit(() -> VoteListener.EVENT.invoker().onVote(vote));
    }
}
