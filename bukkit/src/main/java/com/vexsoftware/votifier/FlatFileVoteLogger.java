package com.vexsoftware.votifier;

import com.vexsoftware.votifier.model.Vote;
import com.vexsoftware.votifier.model.VotifierEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

/* This code is adapted from https://dev.bukkit.org/projects/votifier
    (or a direct download: https://github.com/downloads/vexsoftware/votifier/FlatfileVoteListener.class)
    and updated to use the new VotifierEvent instead of implementing a VoteListener
 */

public class FlatFileVoteLogger implements Listener {

    public static final String FILE = "./plugins/NuVotifier/votes.log";
    private static final Logger log = Logger.getLogger("FlatFileVoteListener");



    @EventHandler
    public void FlatFileLogEvent(VotifierEvent event){
        // Get the vote
        Vote vote = event.getVote();
        try {
            // Get a BufferedFileWriter to write to the path provided
            BufferedWriter writer = new BufferedWriter(new FileWriter(FILE, true));
            // Add the vote's .toString method to the buffer
            writer.write(vote.toString());
            // Add a new line
            writer.newLine();
            // Actually write to the log now
            writer.flush();
            // Close the file
            writer.close();
        } catch (Exception ex) {
            log.log(Level.WARNING, "Unable to log vote: " + vote);
        }
    }
}
