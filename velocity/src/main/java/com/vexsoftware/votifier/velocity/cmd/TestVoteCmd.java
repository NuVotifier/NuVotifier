package com.vexsoftware.votifier.velocity.cmd;

import com.velocitypowered.api.command.Command;
import com.velocitypowered.api.command.CommandSource;
import com.vexsoftware.votifier.model.Vote;
import com.vexsoftware.votifier.net.VotifierSession;
import com.vexsoftware.votifier.util.ArgsToVote;
import com.vexsoftware.votifier.velocity.VotifierPlugin;
import net.kyori.text.TextComponent;
import net.kyori.text.format.TextColor;
import org.checkerframework.checker.nullness.qual.NonNull;

public class TestVoteCmd implements Command {

    private final VotifierPlugin plugin;

    public TestVoteCmd(VotifierPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSource sender, @NonNull String[] args) {
        Vote v;
        try {
            v = ArgsToVote.parse(args);
        } catch (IllegalArgumentException e) {
            sender.sendMessage(TextComponent.of("Error while parsing arguments to create test vote: " + e.getMessage()).color(TextColor.DARK_RED));
            sender.sendMessage(TextComponent.of("Usage hint: /testvote [username] [serviceName=?] [username=?] [address=?] [localTimestamp=?] [timestamp=?]").color(TextColor.GRAY));
            return;
        }

        plugin.onVoteReceived(v, VotifierSession.ProtocolVersion.TEST, "localhost.test");
        sender.sendMessage(TextComponent.of("Test vote executed: " + v.toString()).color(TextColor.GREEN));
    }

    @Override
    public boolean hasPermission(CommandSource source, @NonNull String[] args) {
        return source.hasPermission("nuvotifier.testvote");
    }
}
