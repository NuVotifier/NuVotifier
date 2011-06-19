package com.vexsoftware.votifier.model.listeners;

import java.util.logging.Logger;

import com.vexsoftware.votifier.model.Vote;
import com.vexsoftware.votifier.model.VoteListener;

/**
 * A basic vote listener for demonstration purposes.
 * 
 * @author Blake Beaupain
 */
public class BasicVoteListener implements VoteListener {

	/** The logger instance. */
	private Logger log = Logger.getLogger("BasicVoteListener");

	@Override
	public void voteMade(Vote vote) {
		log.info("Received: " + vote);
	}

}
