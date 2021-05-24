package com.vexsoftware.votifier.velocity.cmd;

import com.velocitypowered.api.command.SimpleCommand;
import com.vexsoftware.votifier.velocity.VotifierPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class NVReloadCmd implements SimpleCommand {

    private final VotifierPlugin plugin;

    public NVReloadCmd(VotifierPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(SimpleCommand.Invocation invocation) {
        invocation.source().sendMessage(Component.text("Reloading NuVotifier...", NamedTextColor.GRAY));
        if (plugin.reload()) {
            invocation.source().sendMessage(Component.text("NuVotifier has been reloaded!", NamedTextColor.DARK_GREEN));
        } else {
            invocation.source().sendMessage(Component.text("Looks like there was a problem reloading NuVotifier, check the console!", NamedTextColor.DARK_RED));
        }
    }

    @Override
    public boolean hasPermission(SimpleCommand.Invocation invocation) {
        return invocation.source().hasPermission("nuvotifier.reload");
    }
}
