import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import com.iConomy.iConomy;
import com.iConomy.system.Holdings;
import com.vexsoftware.votifier.model.Vote;
import com.vexsoftware.votifier.model.VoteListener;

/**
 * A VoteListener that rewards via iConomy.
 * 
 * @author Blake Beaupain
 */
public class iConomyListener implements VoteListener {

	/** The logger instance. */
	private static Logger logger = Logger.getLogger("iConomyListener");

	/** The amount to reward. */
	private int amount = 100;

	/**
	 * Instantiates a new iConomy listener.
	 */
	public iConomyListener() {
		Properties props = new Properties();
		try {
			// Create the file if it doesn't exist.
			File configFile = new File("./plugins/Votifier/iConomyListener.ini");
			if (!configFile.exists()) {
				configFile.createNewFile();

				// Load the configuration.
				props.load(new FileReader(configFile));

				// Write the default configuration.
				props.setProperty("reward_amount", Integer.toString(amount));
				props.store(new FileWriter(configFile), "iConomy Listener Configuration");
			} else {
				// Load the configuration.
				props.load(new FileReader(configFile));
			}

			amount = Integer.parseInt(props.getProperty("reward_amount", "100"));
		} catch (Exception ex) {
			logger.log(Level.WARNING, "Unable to load iConomyListener.ini, using default reward value of: " + amount);
		}
	}

	@Override
	public void voteMade(Vote vote) {
		String username = vote.getUsername();
		if (iConomy.hasAccount(username)) {
			Holdings balance = iConomy.getAccount(username).getHoldings();
			balance.add(amount);

			// Tell the player how awesome they are.
			Player player = Bukkit.getServer().getPlayer(username);
			if (player != null) {
				player.sendMessage("Thanks for voting on " + vote.getServiceName() + "!");
				player.sendMessage(amount + " has been added to your iConomy balance.");
			}
		}
	}

}
