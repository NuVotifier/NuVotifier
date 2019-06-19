package com.vexsoftware.votifier.bungee.cmd;

import com.vexsoftware.votifier.bungee.NuVotifier;
import com.vexsoftware.votifier.model.Vote;
import com.vexsoftware.votifier.net.VotifierSession;
import com.vexsoftware.votifier.util.ArgsToVote;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.plugin.Command;

public class TestVoteCmd extends Command {

    private final NuVotifier plugin;

    private static final BaseComponent permission = new TextComponent("You do not have permission to do this!");
    private static final BaseComponent usage = new TextComponent("Usage hint: /ptestvote [username] [serviceName=?] [username=?] [address=?] [localTimestamp=?] [timestamp=?]");

    static {
        usage.setColor(ChatColor.GRAY);
    }

    public TestVoteCmd(NuVotifier plugin) {
        super("ptestvote", "nuvotifier.testvote");
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (sender.hasPermission("nuvotifier.testvote")) {
            Vote v;
            try {
                v = ArgsToVote.parse(args);
            } catch (IllegalArgumentException e) {
                TextComponent c = new TextComponent("Error while parsing arguments to create test vote: " + e.getMessage());
                c.setColor(ChatColor.DARK_RED);
                sender.sendMessage(c);
                sender.sendMessage(usage);
                return;
            }

            plugin.onVoteReceived(v, VotifierSession.ProtocolVersion.TEST, "localhost.test");
            TextComponent c = new TextComponent("Test vote executed: " + v.toString());
            c.setColor(ChatColor.GREEN);
            sender.sendMessage(c);
        } else {
            sender.sendMessage(permission);
        }
    }
}
