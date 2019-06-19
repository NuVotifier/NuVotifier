package com.vexsoftware.votifier.cmd;

import com.vexsoftware.votifier.NuVotifierBukkit;
import com.vexsoftware.votifier.model.Vote;
import com.vexsoftware.votifier.net.VotifierSession;
import com.vexsoftware.votifier.util.ArgsToVote;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class TestVoteCmd implements CommandExecutor {

    private final NuVotifierBukkit plugin;

    public TestVoteCmd(NuVotifierBukkit plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender.hasPermission("nuvotifier.testvote")) {
            Vote v;
            try {
                v = ArgsToVote.parse(args);
            } catch (IllegalArgumentException e) {
                sender.sendMessage(ChatColor.DARK_RED + "Error while parsing arguments to create test vote: " + e.getMessage());
                sender.sendMessage(ChatColor.GRAY + "Usage hint: /testvote [username] [serviceName=?] [username=?] [address=?] [localTimestamp=?] [timestamp=?]");
                return true;
            }

            plugin.onVoteReceived(v, VotifierSession.ProtocolVersion.TEST, "localhost.test");
            sender.sendMessage(ChatColor.GREEN + "Test vote executed: " + v.toString());
        } else {
            sender.sendMessage(ChatColor.DARK_RED + "You do not have permission to do this!");
        }
        return true;

    }
}
