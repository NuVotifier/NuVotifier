package com.vexsoftware.votifier.net;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.ClosedByInterruptException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.vexsoftware.votifier.Votifier;
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
		try {
			ServerSocket server = new ServerSocket();
			server.bind(new InetSocketAddress(host, port));
			while (true) {
				Socket socket = server.accept();
				BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
				BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

				// Send them our version.
				writer.write("VERSION " + Votifier.VERSION);
				writer.newLine();

				// Read the vote information.
				String serviceName = reader.readLine();
				String username = reader.readLine();
				String address = reader.readLine();
				String timeStamp = reader.readLine();

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
			}
		} catch (ClosedByInterruptException ex) {
			// Shut down silently.
		} catch (IOException ex) {
			// Something went wrong.
			log.log(Level.SEVERE, "I/O error in VoteReceiver", ex);
		}
	}

}
