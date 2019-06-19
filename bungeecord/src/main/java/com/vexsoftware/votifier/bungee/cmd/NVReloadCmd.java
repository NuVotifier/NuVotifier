package com.vexsoftware.votifier.bungee.cmd;

import com.vexsoftware.votifier.bungee.NuVotifier;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.plugin.Command;

public class NVReloadCmd extends Command {

    private final NuVotifier plugin;

    private static final BaseComponent reloadingNuVotifier = new TextComponent("Reloading NuVotifier...");
    private static final BaseComponent nuvotifierHasBeenReloaded = new TextComponent("NuVotifier has been reloaded!");
    private static final BaseComponent problem = new TextComponent("Looks like there was a problem reloading NuVotifier, check the console!");
    private static final BaseComponent permission = new TextComponent("You do not have permission to do this!");

    static {
        reloadingNuVotifier.setColor(ChatColor.GRAY);
        nuvotifierHasBeenReloaded.setColor(ChatColor.DARK_GREEN);
        problem.setColor(ChatColor.DARK_RED);
        permission.setColor(ChatColor.DARK_RED);
    }

    public NVReloadCmd(NuVotifier plugin) {
        super("pnvreload", "nuvotifier.reload");
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (sender.hasPermission("nuvotifier.reload")) {
            sender.sendMessage(reloadingNuVotifier);
            if (plugin.reload()) {
                sender.sendMessage(nuvotifierHasBeenReloaded);
            } else {
                sender.sendMessage(problem);
            }
        } else {
            sender.sendMessage(permission);
        }
    }
}
