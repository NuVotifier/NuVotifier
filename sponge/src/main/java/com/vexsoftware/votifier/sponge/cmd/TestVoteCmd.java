package com.vexsoftware.votifier.sponge.cmd;

import com.vexsoftware.votifier.model.Vote;
import com.vexsoftware.votifier.net.VotifierSession;
import com.vexsoftware.votifier.sponge.NuVotifier;
import com.vexsoftware.votifier.util.ArgsToVote;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

import java.util.Collection;

public class TestVoteCmd implements CommandExecutor {

    private final NuVotifier plugin;

    public TestVoteCmd(NuVotifier plugin) {
        this.plugin = plugin;
    }

    @Override
    public CommandResult execute(CommandSource sender, CommandContext args) throws CommandException {
        Vote v;
        try {
            Collection<String> a = args.getAll("args");
            v = ArgsToVote.parse(a.toArray(new String[0]));
        } catch (IllegalArgumentException e) {
            sender.sendMessage(Text.builder("Error while parsing arguments to create test vote: " + e.getMessage()).color(TextColors.DARK_RED).build());
            sender.sendMessage(Text.builder("Usage hint: /testvote [username] [serviceName=?] [username=?] [address=?] [localTimestamp=?] [timestamp=?]").color(TextColors.GRAY).build());
            return CommandResult.empty();
        }

        plugin.onVoteReceived(v, VotifierSession.ProtocolVersion.TEST, "localhost.test");
        return CommandResult.success();
    }
}
