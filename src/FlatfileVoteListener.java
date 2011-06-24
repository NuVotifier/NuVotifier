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

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.logging.Logger;

import com.vexsoftware.votifier.model.Vote;
import com.vexsoftware.votifier.model.VoteListener;

/**
 * A vote listener that records votes in a text file.
 * 
 * @author Blake Beaupain
 */
public class FlatfileVoteListener implements VoteListener {

	/** The logger instance. */
	private Logger log = Logger.getLogger("FlatfileVoteListener");

	/** The file to log to. */
	private final String file = "./plugins/Votifier/votes.log";

	@Override
	public void voteMade(Vote vote) {
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(file));
			writer.write(vote.toString());
			writer.newLine();
			writer.flush();
			writer.close();
		} catch (Exception ex) {
			log.info("Unable to log vote: " + vote);
		}

	}

}
