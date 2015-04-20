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

import java.io.*;
import java.security.Key;
import java.security.KeyPair;
import java.util.*;
import java.util.logging.*;

import com.vexsoftware.votifier.crypto.KeyCreator;
import com.vexsoftware.votifier.net.VoteInboundHandler;
import com.vexsoftware.votifier.net.VotifierSession;
import com.vexsoftware.votifier.net.protocol.VotifierGreetingHandler;
import com.vexsoftware.votifier.net.protocol.VotifierProtocolDifferentiator;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import com.vexsoftware.votifier.crypto.RSAIO;
import com.vexsoftware.votifier.crypto.RSAKeygen;
import com.vexsoftware.votifier.model.ListenerLoader;
import com.vexsoftware.votifier.model.VoteListener;

/**
 * The main Votifier plugin class.
 * 
 * @author Blake Beaupain
 * @author Kramer Campbell
 */
public class Votifier extends JavaPlugin {

	/** The logger instance. */
	private static final Logger LOG = Logger.getLogger("Votifier");

	/** Log entry prefix */
	private static final String logPrefix = "[Votifier] ";

	/** The Votifier instance. */
	private static Votifier instance;

	/** The current Votifier version. */
	private String version;

	/** The vote listeners. */
	private final List<VoteListener> listeners = new ArrayList<VoteListener>();

	/** The server channel. */
	private Channel serverChannel;

	/** The event group handling the channel. */
	private NioEventLoopGroup serverGroup;

	/** The RSA key pair. */
	private KeyPair keyPair;

	/** Debug mode flag */
	private boolean debug;

	/** Keys used for websites. */
	private Map<String, Key> tokens = new HashMap<>();

	/**
	 * Attach custom log filter to logger.
	 */
	static {
		LOG.setFilter(new LogFilter(logPrefix));
	}

	@Override
	public void onEnable() {
		Votifier.instance = this;

		// Set the plugin version.
		version = getDescription().getVersion();

		// Handle configuration.
		if (!getDataFolder().exists()) {
			getDataFolder().mkdir();
		}
		File config = new File(getDataFolder() + "/config.yml");
		YamlConfiguration cfg = YamlConfiguration.loadConfiguration(config);
		File rsaDirectory = new File(getDataFolder() + "/rsa");
		// Replace to remove a bug with Windows paths - SmilingDevil
		String listenerDirectory = getDataFolder().toString()
				.replace("\\", "/") + "/listeners";

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
				LOG.info("Configuring Votifier for the first time...");

				// Initialize the configuration file.
				config.createNewFile();

				cfg.set("host", hostAddr);
				cfg.set("port", 8192);
				cfg.set("debug", false);

				/*
				 * Remind hosted server admins to be sure they have the right
				 * port number.
				 */
				LOG.info("------------------------------------------------------------------------------");
				LOG.info("Assigning Votifier to listen on port 8192. If you are hosting Craftbukkit on a");
				LOG.info("shared server please check with your hosting provider to verify that this port");
				LOG.info("is available for your use. Chances are that your hosting provider will assign");
				LOG.info("a different port, which you need to specify in config.yml");
				LOG.info("------------------------------------------------------------------------------");

				String token = TokenUtil.newToken();
				ConfigurationSection tokenSection = cfg.createSection("tokens");
				tokenSection.set("default", token);
				LOG.info("Your default Votifier token is " + token + ".");
				LOG.info("You will need to provide this token when you submit your server to a voting");
				LOG.info("list.");
				LOG.info("------------------------------------------------------------------------------");

				cfg.set("listener_folder", listenerDirectory);
				cfg.save(config);
			} catch (Exception ex) {
				LOG.log(Level.SEVERE, "Error creating configuration file", ex);
				gracefulExit();
				return;
			}
		} else {
			// Load configuration.
			cfg = YamlConfiguration.loadConfiguration(config);
		}

		/*
		 * Create RSA directory and keys if it does not exist; otherwise, read
		 * keys.
		 */
		try {
			if (!rsaDirectory.exists()) {
				rsaDirectory.mkdir();
				new File(listenerDirectory).mkdir();
				keyPair = RSAKeygen.generate(2048);
				RSAIO.save(rsaDirectory, keyPair);
			} else {
				keyPair = RSAIO.load(rsaDirectory);
			}
		} catch (Exception ex) {
			LOG.log(Level.SEVERE,
					"Error reading configuration file or RSA tokens", ex);
			gracefulExit();
			return;
		}

		// Load the vote listeners.
		listenerDirectory = cfg.getString("listener_folder");
		listeners.addAll(ListenerLoader.load(listenerDirectory));

		// Load Votifier tokens.
		ConfigurationSection tokenSection = cfg.getConfigurationSection("tokens");

		if (tokenSection != null) {
			Map<String, Object> websites = tokenSection.getValues(false);
			for (Map.Entry<String, Object> website : websites.entrySet()) {
				tokens.put(website.getKey(), KeyCreator.createKeyFrom(website.getValue().toString()));
				LOG.info("Loaded token for website: " + website.getKey());
			}
		} else {
			LOG.warning("No websites are listed in your configuration.");
		}

		// Initialize the receiver.
		String host = cfg.getString("host", hostAddr);
		int port = cfg.getInt("port", 8192);
		debug = cfg.getBoolean("debug", false);
		if (debug)
			LOG.info("DEBUG mode enabled!");

		serverGroup = new NioEventLoopGroup(1);

		new ServerBootstrap()
				.channel(NioServerSocketChannel.class)
				.group(serverGroup)
				.childHandler(new ChannelInitializer<NioSocketChannel>() {
					@Override
					protected void initChannel(NioSocketChannel channel) throws Exception {
						channel.attr(VotifierSession.KEY).set(new VotifierSession());
						channel.pipeline().addLast("greetingHandler", new VotifierGreetingHandler());
						channel.pipeline().addLast("protocolDifferentiator", new VotifierProtocolDifferentiator());
						channel.pipeline().addLast("voteHandler", new VoteInboundHandler());
					}
				})
				.bind(host, port)
				.addListener(new ChannelFutureListener() {
					@Override
					public void operationComplete(ChannelFuture future) throws Exception {
						if (future.isSuccess()) {
							serverChannel = future.channel();
							LOG.info("Votifier enabled.");
						} else {
							LOG.log(Level.SEVERE, "Votifier was not able to bind to " + future.channel().localAddress(), future.cause());
						}
					}
				});
	}

	@Override
	public void onDisable() {
		// Shut down the network handlers.
		if (serverChannel != null)
			serverChannel.close();
		serverGroup.shutdownGracefully();
		LOG.info("Votifier disabled.");
	}

	private void gracefulExit() {
		LOG.log(Level.SEVERE, "Votifier did not initialize properly!");
	}

	/**
	 * Gets the instance.
	 * 
	 * @return The instance
	 */
	public static Votifier getInstance() {
		return instance;
	}

	/**
	 * Gets the version.
	 * 
	 * @return The version
	 */
	public String getVersion() {
		return version;
	}

	/**
	 * Gets the listeners.
	 * 
	 * @return The listeners
	 */
	public List<VoteListener> getListeners() {
		return listeners;
	}

	/**
	 * Gets the keyPair.
	 * 
	 * @return The keyPair
	 */
	public KeyPair getKeyPair() {
		return keyPair;
	}

	public boolean isDebug() {
		return debug;
	}

	public Map<String, Key> getTokens() {
		return tokens;
	}
}
