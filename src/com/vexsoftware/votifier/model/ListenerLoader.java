package com.vexsoftware.votifier.model;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Loads vote listeners.
 * 
 * @author Blake Beaupain
 */
public class ListenerLoader {

	/** The logger instance. */
	private static Logger log = Logger.getLogger("ListenerLoader");

	/**
	 * Loads all listener class files from a directory.
	 * 
	 * @param directory
	 *            The directory
	 */
	public static List<VoteListener> load(String directory) throws Exception {
		List<VoteListener> listeners = new ArrayList<VoteListener>();
		File dir = new File(directory);

		// Load the vote listener instances.
		ClassLoader loader = new URLClassLoader(new URL[] { dir.toURI().toURL() }, VoteListener.class.getClassLoader());
		for (File file : dir.listFiles()) {
			String name = file.getName().substring(0, file.getName().lastIndexOf("."));
			Class<?> clazz = loader.loadClass(name);
			Object object = clazz.newInstance();
			if (!(object instanceof VoteListener)) {
				log.info("Not a vote listener: " + clazz.getSimpleName());
				continue;
			}
			VoteListener listener = (VoteListener) object;
			listeners.add(listener);
			log.info("Loaded vote listener: " + listener.getClass().getSimpleName());
		}
		return listeners;
	}

}
