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
import com.vexsoftware.votifier.model.VoteListener;
import com.vexsoftware.votifier.model.listeners.BasicVoteListener;
import com.vexsoftware.votifier.net.VoteReceiver;

/**
 * The main Votifier plugin class.
 * 
 * @author Blake Beaupain
 */
public class Votifier extends JavaPlugin {

	/** The current Votifier version. */
	public static final String VERSION = "1.1";

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
			if (!config.exists()) {
				// First time run - do some initialization.
				log.info("Configuring Votifier for the first time...");

				// Initialize the configuration file.
				config.createNewFile();
				cfg.setProperty("host", "0.0.0.0");
				cfg.setProperty("port", 8192);
				cfg.save();

				// Generate the RSA key pair.
				rsaDirectory.mkdir();
				keyPair = RSAKeygen.generate(512);
				RSAIO.save(rsaDirectory, keyPair);
			} else {
				// Load configuration.
				keyPair = RSAIO.load(rsaDirectory);
				cfg.load();
			}

			// Initialize the receiver.
			String host = cfg.getString("host", "0.0.0.0");
			int port = cfg.getInt("port", 8192);
			voteReceiver = new VoteReceiver(host, port);
			voteReceiver.start();

			// Add the vote listeners.
			listeners.add(new BasicVoteListener());

			log.info("Votifier enabled.");
		} catch (Exception ex) {
			log.log(Level.SEVERE, "Unable to enable Votifier.", ex);
		}
	}

	@Override
	public void onDisable() {
		// Interrupt the vote receiver.
		if (voteReceiver != null) {
			voteReceiver.interrupt();
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
