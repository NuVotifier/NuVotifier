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
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import com.vexsoftware.votifier.crypto.RSAIO;
import com.vexsoftware.votifier.crypto.RSAKeygen;
import com.vexsoftware.votifier.model.ListenerLoader;
import com.vexsoftware.votifier.model.VoteListener;
import com.vexsoftware.votifier.net.VoteReceiver;


/**
 * The main Votifier plugin class.
 * 
 * @author Blake Beaupain
 * @author Kramer Campbell
 */
public class Votifier extends JavaPlugin {

	/** The current Votifier version. */
	public static final String			VERSION		= "1.8";

	/** The logger instance. */
	private static final Logger			logger		= Logger.getLogger( "Votifier" );

	/** Log entry prefix */
	private static final String			logPrefix	= "[Votifier] ";

	/** The Votifier instance. */
	private static Votifier				instance;

	/** The vote listeners. */
	private final List<VoteListener>	listeners	= new ArrayList<VoteListener>();

	/** The vote receiver. */
	private VoteReceiver				voteReceiver;

	/** The RSA key pair. */
	private KeyPair						keyPair;

	/** Debug mode flag */
	private boolean						debug;


	@Override
	public void onEnable() {
		Votifier.instance = this;

		// Handle configuration.
		if ( !getDataFolder().exists() ) {
			getDataFolder().mkdir();
		}
		File config = new File( getDataFolder() + "/config.yml" );
		YamlConfiguration cfg = YamlConfiguration.loadConfiguration( config );
		File rsaDirectory = new File( getDataFolder() + "/rsa" );
		String listenerDirectory = getDataFolder() + "/listeners";

		/*
		 * Use IP address from server.properties as a default for configurations. Do not use InetAddress.getLocalHost() as it
		 * most likely will return the main server address instead of the address assigned to the server.
		 */
		String hostAddr = Bukkit.getServer().getIp();
		if ( hostAddr == null || hostAddr.length() == 0 )
			hostAddr = "0.0.0.0";

		/*
		 * Create configuration file if it does not exists; otherwise, load it
		 */
		if ( !config.exists() ) {
			try {
				// First time run - do some initialization.
				logInfo( "Configuring Votifier for the first time..." );

				// Initialize the configuration file.
				config.createNewFile();

				cfg.set( "host", hostAddr );
				cfg.set( "port", 8192 );
				cfg.set( "debug", false );

				/*
				 * Remind hosted server admins to be sure they have the right port number.
				 */
				logInfo( "------------------------------------------------------------------------------" );
				logInfo( "Assigning Votifier to listen on port 8192. If you are hosting Craftbukkit on a" );
				logInfo( "shared server please check with your hosting provider to verify that this port" );
				logInfo( "is available for your use. Chances are that your hosting provider will assign" );
				logInfo( "a different port, which you need to specify in config.yml" );
				logInfo( "------------------------------------------------------------------------------" );

				cfg.set( "listener_folder", listenerDirectory );
				cfg.save( config );
			}
			catch ( Exception ex ) {
				log( Level.SEVERE, "Error creating configuration file", ex );
				gracefulExit();
				return;
			}
		}
		else {
			cfg = YamlConfiguration.loadConfiguration( config );
		}

		/*
		 * Create RSA directory and keys if it does not exist; otherwise, read keys.
		 */
		try {
			if ( !rsaDirectory.exists() ) {
				logInfo( "Could not find RSA directory, creating directory and a new set of RSA keys!" );
				rsaDirectory.mkdir();
				new File( listenerDirectory ).mkdir();
				keyPair = RSAKeygen.generate( 2048 );
				RSAIO.save( rsaDirectory, keyPair );
			}
			else {
				keyPair = RSAIO.load( rsaDirectory );
			}
		}
		catch ( Exception ex ) {
			log( Level.SEVERE, "Error reading configuration file or RSA keys", ex );
			gracefulExit();
			return;
		}
		
		// Load the vote listeners.
		listenerDirectory = cfg.getString( "listener_folder" );
		listeners.addAll( ListenerLoader.load( listenerDirectory ) );

		// Initialize the receiver.
		String host = cfg.getString( "host", hostAddr );
		int port = cfg.getInt( "port", 8192 );
		debug = cfg.getBoolean( "debug", false );
		if ( debug )
			logDebug( "DEBUG mode enabled!" );

		try {
			voteReceiver = new VoteReceiver( this, host, port );
			voteReceiver.start();

			logInfo( "Votifier enabled." );
		}
		catch ( Exception ex ) {
			gracefulExit();
			return;
		}
	}


	@Override
	public void onDisable() {
		// Interrupt the vote receiver.
		if ( voteReceiver != null ) {
			voteReceiver.shutdown();
		}
		logger.info( "Votifier disabled." );
	}


	private void gracefulExit() {
		log( Level.SEVERE, "Votifier did not initialize properly!" );
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


	/**
	 * Convenience method for logging INFO messages. Plugin prefix is automatically added.
	 * 
	 * @param msg the INFO message to log
	 */
	public static void logInfo( String msg ) {
		logger.info( logPrefix + msg );
	}


	/**
	 * Convenience method for logging debug messages. Plugin prefix is automatically added in addition to '[DBG]' indicating
	 * a debug-specific message. Debug messages are logged at the INFO level.
	 * 
	 * @param msg the debug message to log
	 */
	public static void logDebug( String msg ) {
		logger.info( logPrefix + "[DBG] " + msg );
	}


	/**
	 * Convenience method for logging messages. Plugin prefix is automatically added. The message is logged at the given log
	 * level.
	 * 
	 * @param level log level at which to log message.
	 * @param msg message to log.
	 */
	public static void log( Level level, String msg ) {
		logger.log( level, logPrefix + msg );
	}


	/**
	 * Convenience method for logging messages with exception details. Plugin prefix is automatically added. The message is
	 * logged at the given log level.
	 * 
	 * @param level log level at which to log message.
	 * @param msg message to log.
	 * @param ex exception causing error.
	 */
	public static void log( Level level, String msg, Exception ex ) {
		logger.log( level, logPrefix + msg, ex );
	}


	public boolean isDebug() {
		return debug;
	}
}
