package com.vexsoftware.votifier.model;

/**
 * A listener for votes.
 * 
 * @author Blake Beaupain
 */
public interface VoteListener {

	/**
	 * Called when a vote is made.
	 * 
	 * @param vote
	 *            The vote that was made
	 */
	public void voteMade(Vote vote);

}
