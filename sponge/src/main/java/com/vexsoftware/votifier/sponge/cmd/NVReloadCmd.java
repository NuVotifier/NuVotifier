package com.vexsoftware.votifier.sponge.cmd;

import com.vexsoftware.votifier.sponge.NuVotifier;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

public class NVReloadCmd implements CommandExecutor {

    private final NuVotifier plugin;

    public NVReloadCmd(NuVotifier plugin) {
        this.plugin = plugin;
    }

    @Override
    public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
        src.sendMessage(Text.builder("Reloading NuVotifier...").color(TextColors.GRAY).build());
        if (plugin.reload())
            return CommandResult.success();
        else
            return CommandResult.empty();
    }
}
