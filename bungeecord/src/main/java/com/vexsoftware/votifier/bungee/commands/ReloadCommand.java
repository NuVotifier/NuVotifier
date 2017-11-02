package com.vexsoftware.votifier.bungee.commands;

import com.vexsoftware.votifier.bungee.NuVotifier;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.plugin.Command;

/**
 * Created by curscascis on 8/20/2017.
 */
public class ReloadCommand extends Command{
    private NuVotifier plugin;

    public ReloadCommand(NuVotifier plugin) {
        super("nvreload");
        this.plugin = plugin;
    }


    @Override
    public void execute(CommandSender commandSender, String[] strings) {
        if(commandSender.hasPermission("nuvotifier.reload")){
            if ((plugin.reloadConfigs())) {
                commandSender.sendMessage(new ComponentBuilder("nuvotifier has been reloaded").color(ChatColor.GREEN).create());
            } else {
                commandSender.sendMessage(new ComponentBuilder("nuvotifier has failed while reloading. Check your config.").color(ChatColor.RED).create());
            }
        }
    }


}
