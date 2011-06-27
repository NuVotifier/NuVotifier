/*
 * Copyright (C) 2011 Vex Software LLC
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

import java.io.File;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.config.Configuration;

import com.vexsoftware.votifier.crypto.RSAIO;
import com.vexsoftware.votifier.crypto.RSAKeygen;
import com.vexsoftware.votifier.model.ListenerLoader;
import com.vexsoftware.votifier.model.VoteListener;
import com.vexsoftware.votifier.net.VoteReceiver;

/**
 * The main Votifier plugin class.
 * 
 * @author Blake Beaupain
 */
public class Votifier extends JavaPlugin {

	/** The current Votifier version. */
	public static final String VERSION = "1.4";

	/** The logger instance. */
	private static final Logger log = Logger.getLogger("Votifier");

	/** The Votifier instance. */
	private static Votifier instance;

	/** The vote listeners. */
	private final List<VoteListener> listeners = new ArrayList<VoteListener>();

	/** The vote receiver. */
	private VoteReceiver voteReceiver;

	/** The RSA key pair. */
	private KeyPair keyPair;

	@Override
	public void onEnable() {
		try {
			Votifier.instance = this;

			// Handle configuration.
			if (!getDataFolder().exists()) {
				getDataFolder().mkdir();
			}
			Configuration cfg = getConfiguration();
			File config = new File(getDataFolder() + "/config.yml");
			File rsaDirectory = new File(getDataFolder() + "/rsa");
			String listenerDirectory = getDataFolder() + "/listeners";
			if (!config.exists()) {
				// First time run - do some initialization.
				log.info("Configuring Votifier for the first time...");

				// Initialize the configuration file.
				config.createNewFile();
				cfg.setProperty("host", "0.0.0.0");
				cfg.setProperty("port", 8192);
				cfg.setProperty("listener_folder", listenerDirectory);
				cfg.save();

				// Generate the RSA key pair.
				rsaDirectory.mkdir();
				new File(listenerDirectory).mkdir();
				keyPair = RSAKeygen.generate(2048);
				RSAIO.save(rsaDirectory, keyPair);
			} else {
				// Load configuration.
				keyPair = RSAIO.load(rsaDirectory);
				cfg.load();
			}

			// Load the vote listeners.
			listenerDirectory = cfg.getString("listener_folder");
			listeners.addAll(ListenerLoader.load(listenerDirectory));

			// Initialize the receiver.
			String host = cfg.getString("host", "0.0.0.0");
			int port = cfg.getInt("port", 8192);
			voteReceiver = new VoteReceiver(host, port);
			voteReceiver.start();

			log.info("Votifier enabled.");
		} catch (Exception ex) {
			log.log(Level.SEVERE, "Unable to enable Votifier.", ex);
		}
	}

	@Override
	public void onDisable() {
		// Interrupt the vote receiver.
		if (voteReceiver != null) {
			voteReceiver.shutdown();
		}
		log.info("Votifier disabled.");
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
	 * Gets the listeners.
	 * 
	 * @return The listeners
	 */
	public List<VoteListener> getListeners() {
		return listeners;
	}

	/**
	 * Gets the vote receiver.
	 * 
	 * @return The vote receiver
	 */
	public VoteReceiver getVoteReceiver() {
		return voteReceiver;
	}

	/**
	 * Gets the keyPair.
	 * 
	 * @return The keyPair
	 */
	public KeyPair getKeyPair() {
		return keyPair;
	}

}
