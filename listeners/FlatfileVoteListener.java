import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.vexsoftware.votifier.model.Vote;
import com.vexsoftware.votifier.model.VoteListener;

/**
 * A vote listener that logs to a file.
 * 
 * @author Blake Beaupain
 */
public class FlatfileVoteListener implements VoteListener {

	/** The log file. */
	public static final String FILE = "./plugins/Votifier/votes.log";

	/** The logger instance. */
	private static final Logger log = Logger.getLogger("FlatfileVoteListener");

	@Override
	public void voteMade(Vote vote) {
		try {
			// Open a buffered writer in append mode.
			BufferedWriter writer = new BufferedWriter(new FileWriter(FILE, true));

			// Append the vote to the file.
			writer.write(vote.toString());
			writer.newLine();
			writer.flush();

			// All done.
			writer.close();
		} catch (Exception ex) {
			log.log(Level.WARNING, "Unable to log vote: " + vote);
		}
	}

}
