import com.nijikokun.register.payment.Method;
import com.nijikokun.register.payment.Methods;
import com.vexsoftware.votifier.model.Vote;
import com.vexsoftware.votifier.model.VoteListener;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A VoteListener that rewards via iConomy.
 *
 * @author Blake Beaupain
 */
public class RegisterListener implements VoteListener {

    /**
     * The logger instance.
     */
    private static Logger logger = Logger.getLogger("RegisterListener");

    /**
     * The amount to reward.
     */
    private int amount = 200;

    /**
     * Instantiates a new Register listener.
     */
    public RegisterListener() {
        Properties props = new Properties();
        try {
            // Create the file if it doesn't exist.
            File configFile = new File("./plugins/Votifier/RegisterListener.ini");
            if (!configFile.exists()) {
                configFile.createNewFile();

                // Load the configuration.
                props.load(new FileReader(configFile));

                // Write the default configuration.
                props.setProperty("reward_amount", Integer.toString(amount));
                props.store(new FileWriter(configFile), "Register Listener Configuration");
            } else {
                // Load the configuration.
                props.load(new FileReader(configFile));
            }

            amount = Integer.parseInt(props.getProperty("reward_amount", "200"));
        } catch (Exception ex) {
            logger.log(Level.WARNING, String.format("Unable to load RegisterListener.ini, using default reward value of %d", amount));
        }
        if (!Methods.hasMethod()) {
            logger.log(Level.WARNING, "No payment method found!");
        } else {
            logger.log(Level.INFO, String.format("Using payment method %s", Methods.getMethod().getName()));
        }
    }

    @Override
    public void voteMade(Vote vote) {
        String username = vote.getUsername();
        if (!Methods.hasMethod()) {
            logger.log(Level.WARNING, String.format("Received vote from %s by %s but no payment method found.", vote.getServiceName(), username));
            return;
        }
        Method m = Methods.getMethod();
        if (m.hasAccount(username)) {
            if (m.getAccount(username).add(amount)) {

                // Tell the player how awesome they are.
                Player player = Bukkit.getServer().getPlayer(username);
                if (player != null) {
                    player.sendMessage("Thank you for voting on " + vote.getServiceName() + "!");
                    player.sendMessage("You have been given $" + amount + ".");
                }
                logger.log(Level.INFO, String.format("Received vote from %s by %s; paid %d by %s.", vote.getServiceName(), username, amount, m.getName()));
            } else {
                logger.log(Level.WARNING, String.format("Received vote from %s by %s but %s payment failed.", vote.getServiceName(), username, m.getName()));
            }
        } else {
            logger.log(Level.WARNING, String.format("Received vote from %s by %s but user does not have record in %s.", vote.getServiceName(), username, m.getName()));
        }
    }

}
