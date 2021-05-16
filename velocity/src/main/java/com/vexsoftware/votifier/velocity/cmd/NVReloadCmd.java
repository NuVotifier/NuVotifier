package com.vexsoftware.votifier.velocity.cmd;

import com.velocitypowered.api.command.Command;
import com.velocitypowered.api.command.CommandSource;
import com.vexsoftware.votifier.velocity.VotifierPlugin;
import net.kyori.text.TextComponent;
import net.kyori.text.format.TextColor;
import org.checkerframework.checker.nullness.qual.NonNull;

public class NVReloadCmd implements Command {

    private final VotifierPlugin plugin;

    public NVReloadCmd(VotifierPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSource sender, @NonNull String[] args) {
        sender.sendMessage(TextComponent.of("Reloading NuVotifier...").color(TextColor.GRAY));
        if (plugin.reload()) {
            sender.sendMessage(TextComponent.of("NuVotifier has been reloaded!").color(TextColor.DARK_GREEN));
        } else {
            sender.sendMessage(TextComponent.of("Looks like there was a problem reloading NuVotifier, check the console!").color(TextColor.DARK_RED));
        }
    }

    @Override
    public boolean hasPermission(CommandSource sender, @NonNull String[] args) {
        return sender.hasPermission("nuvotifier.reload");
    }
}
