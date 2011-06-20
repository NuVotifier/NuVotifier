package com.vexsoftware.votifier.net;

import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.ClosedByInterruptException;
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
 */
public class VoteReceiver extends Thread {

	/** The logger instance. */
	private static final Logger log = Logger.getLogger("VoteReceiver");

	/** The host to listen on. */
	private final String host;

	/** The port to listen on. */
	private final int port;

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

	@Override
	public void run() {
		ServerSocket server;
		try {
			server = new ServerSocket();
			server.bind(new InetSocketAddress(host, port));
		} catch (Exception ex) {
			log.log(Level.SEVERE, "Error initializing vote receiver", ex);
			return;
		}

		// Main loop.
		while (true) {
			try {
				Socket socket = server.accept();
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
				int opcode = block[position++];
				if (opcode != 128) {
					// Something went wrong in RSA.
					throw new Exception("Unable to decode RSA");
				}

				// Parse the block.
				String serviceName = readString(block);
				String username = readString(block);
				String address = readString(block);
				String timeStamp = readString(block);

				// Create the vote.
				Vote vote = new Vote();
				vote.setServiceName(serviceName);
				vote.setUsername(username);
				vote.setAddress(address);
				vote.setTimeStamp(timeStamp);

				// Dispatch the vote to all listeners.
				for (VoteListener listener : Votifier.getInstance().getListeners()) {
					listener.voteMade(vote);
				}

				// Clean up.
				writer.close();
				in.close();
				socket.close();
			} catch (ClosedByInterruptException ex) {
				return; // Shut down silently.
			} catch (Exception ex) {
				// Something went horribly wrong.
				log.log(Level.SEVERE, "Error in vote receiver", ex);
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
	private String readString(byte[] data) {
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < data.length; i++) {
			if (data[i] == '\n')
				break; // Delimiter reached.
			builder.append((char) data[i]);
		}
		return builder.toString();
	}

}
