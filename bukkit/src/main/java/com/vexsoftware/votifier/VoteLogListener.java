package com.vexsoftware.votifier;

import com.vexsoftware.votifier.model.Vote;
import com.vexsoftware.votifier.model.VotifierEvent;
import com.vexsoftware.votifier.platform.LoggingAdapter;
import com.vexsoftware.votifier.platform.scheduler.VotifierScheduler;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.io.BufferedWriter;
import java.io.FileWriter;

/* This code is adapted from https://dev.bukkit.org/projects/votifier
    (or a direct download: https://github.com/downloads/vexsoftware/votifier/FlatfileVoteListener.class)
    and updated to use the new VotifierEvent instead of implementing a VoteListener
 */

public class VoteLogListener implements Listener {

    private final NuVotifierBukkit plugin;
    private VotifierScheduler scheduler;
    private String file = "";
    private LoggingAdapter pluginLogger;

    VoteLogListener(NuVotifierBukkit plugin, String loggingFile){
        // Get the plugin
        this.plugin = plugin;
        // Get the plugin's logger
        this.pluginLogger = plugin.getPluginLogger();
        // Get the plugin's scheduler
        this.scheduler = plugin.getScheduler();
        // Get the file to log the votes in
        this.file = loggingFile;
    }

    @EventHandler
    public void FlatFileLogEvent(VotifierEvent event) {
        // Get the vote to log
        Vote vote = event.getVote();
        // Create an async task to run the FlatFileLog
        scheduler.onPool(() -> FlatFileLog(file, vote));
    }

    void FlatFileLog(String logFile, Vote logVote){
        try {
            // Get a BufferedFileWriter to write to the path provided
            BufferedWriter writer = new BufferedWriter(new FileWriter(logFile, true));
            // Add the vote's .toString method to the buffer
            writer.write(logVote.toString());
            // Add a new line
            writer.newLine();
            // Actually write to the log now
            writer.flush();
            // Close the file
            writer.close();
        } catch (Exception ex) {
            pluginLogger.error("Unable to log vote: " + logVote);
        }
    }
}
