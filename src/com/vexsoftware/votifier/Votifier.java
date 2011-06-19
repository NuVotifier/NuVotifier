package com.vexsoftware.votifier;

import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.plugin.java.JavaPlugin;

import com.vexsoftware.votifier.model.VoteListener;
import com.vexsoftware.votifier.model.listeners.BasicVoteListener;
import com.vexsoftware.votifier.net.VoteReceiver;

/**
 * The main Votifier plugin class.
 * 
 * @author Blake Beaupain
 */
public class Votifier extends JavaPlugin {

	/** The configuration file. */
	public static final String CONFIG_FILE = "server.properties";

	/** The current Votifier version. */
	public static final String VERSION = "1.0";

	/** The logger instance. */
	private static final Logger log = Logger.getLogger("Votifier");

	/** The Votifier instance. */
	private static Votifier instance;

	/** The vote listeners. */
	private final List<VoteListener> listeners = new ArrayList<VoteListener>();

	/** The vote receiver. */
	private VoteReceiver voteReceiver;

	@Override
	public void onEnable() {
		try {
			Votifier.instance = this;
			Properties props = new Properties();
			props.load(new FileReader(CONFIG_FILE));

			// Start up the vote receiver.
			String host = props.getProperty("votifier_host");
			int port = Integer.parseInt(props.getProperty("votifier_port"));
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

}
