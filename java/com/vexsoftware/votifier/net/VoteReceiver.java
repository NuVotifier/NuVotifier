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

package com.vexsoftware.votifier.net;

import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.vexsoftware.votifier.Votifier;
import com.vexsoftware.votifier.crypto.RSA;
import com.vexsoftware.votifier.model.Vote;
import com.vexsoftware.votifier.model.VoteListener;

/**
 * The vote receiving server.
 * 
 * @author Blake Beaupain
 * @author Kramer Campbell
 */
public class VoteReceiver extends Thread {

	/** The logger instance. */
	private static final Logger log = Logger.getLogger("VoteReceiver");

	/** The host to listen on. */
	private final String host;

	/** The port to listen on. */
	private final int port;

	/** The server socket. */
	private ServerSocket server;

	/** The running flag. */
	private boolean running = true;

	/**
	 * Instantiates a new vote receiver.
	 * 
	 * @param host
	 *            The host to listen on
	 * @param port
	 *            The port to listen on
	 */
	public VoteReceiver(String host, int port) {
		this.host = host;
		this.port = port;
	}

	/**
	 * Shuts the vote receiver down cleanly.
	 */
	public void shutdown() {
		running = false;
		if (server == null)
			return;
		try {
			server.close();
		} catch (Exception ex) {
			log.log(Level.WARNING, "Unable to shut down vote receiver cleanly.");
		}
	}

	@Override
	public void run() {
		try {
			server = new ServerSocket();
			server.bind(new InetSocketAddress(host, port));
		} catch (Exception ex) {
			log.log(Level.SEVERE, "Error initializing vote receiver");
			return;
		}

		// Main loop.
		while (running) {
			try {
				Socket socket = server.accept();
				socket.setSoTimeout(5000); // Don't hang on slow connections.
				BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
				InputStream in = socket.getInputStream();

				// Send them our version.
				writer.write("VOTIFIER " + Votifier.VERSION);
				writer.newLine();
				writer.flush();

				// Read the 256 byte block.
				byte[] block = new byte[256];
				in.read(block, 0, block.length);

				// Decrypt the block.
				block = RSA.decrypt(block, Votifier.getInstance().getKeyPair().getPrivate());
				int position = 0;

				// Perform the opcode check.
				String opcode = readString(block, position);
				position += opcode.length() + 1;
				if (!opcode.equals("VOTE")) {
					// Something went wrong in RSA.
					throw new Exception("Unable to decode RSA");
				}

				// Parse the block.
				String serviceName = readString(block, position);
				position += serviceName.length() + 1;
				String username = readString(block, position);
				position += username.length() + 1;
				String address = readString(block, position);
				position += address.length() + 1;
				String timeStamp = readString(block, position);
				position += timeStamp.length() + 1;

				// Create the vote.
				Vote vote = new Vote();
				vote.setServiceName(serviceName);
				vote.setUsername(username);
				vote.setAddress(address);
				vote.setTimeStamp(timeStamp);

				// Dispatch the vote to all listeners.
				for (VoteListener listener : Votifier.getInstance().getListeners()) {
					try {
						listener.voteMade(vote);
					} catch (Exception ex) {
						log.log(Level.WARNING, "Exception caught while sending the vote notification to a listener", ex);
					}
				}

				// Clean up.
				writer.close();
				in.close();
				socket.close();
			} catch (Exception ex) {
				log.log(Level.WARNING, "Exception caught while receiving a vote notification", ex);
			}
		}
	}

	/**
	 * Reads a string from a block of data.
	 * 
	 * @param data
	 *            The data to read from
	 * @return The string
	 */
	private String readString(byte[] data, int offset) {
		StringBuilder builder = new StringBuilder();
		for (int i = offset; i < data.length; i++) {
			if (data[i] == '\n')
				break; // Delimiter reached.
			builder.append((char) data[i]);
		}
		return builder.toString();
	}

}
