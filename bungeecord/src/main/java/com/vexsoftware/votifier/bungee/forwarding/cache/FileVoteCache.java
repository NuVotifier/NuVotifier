package com.vexsoftware.votifier.bungee.forwarding.cache;

import com.google.common.io.Files;
import com.vexsoftware.votifier.model.Vote;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.scheduler.ScheduledTask;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FileVoteCache extends MemoryVoteCache {

    private final Logger l;
    private final File cacheFile;
    private final int voteTTL;
    private final ScheduledTask saveTask;

    public FileVoteCache(int initialMemorySize, final Plugin plugin, File cacheFile, int voteTTL) throws IOException {
        super(initialMemorySize);
        this.cacheFile = cacheFile;
        this.voteTTL = voteTTL;
        this.l = plugin.getLogger();

        load();

        saveTask = ProxyServer.getInstance().getScheduler().schedule(plugin, new Runnable() {
            @Override
            public void run() {
                try {
                    save();
                } catch (IOException e) {
                    l.log(Level.SEVERE, "Unable to save cached votes, votes will be lost if you restart.", e);
                }
            }
        }, 3, 3, TimeUnit.MINUTES);
    }

    private void load() throws IOException {
        // Load the cache from disk
        JSONObject object;
        try (BufferedReader reader = Files.newReader(cacheFile, StandardCharsets.UTF_8)) {
            object = new JSONObject(new JSONTokener(reader));
        } catch (FileNotFoundException e) {
            object = new JSONObject();
        }

        // Deserialize all votes contained
        for (Object server : object.keySet()) {
            JSONArray voteArray = object.getJSONArray(((String) server));
            List<Vote> votes = new ArrayList<>(voteArray.length());
            for (int i = 0; i < voteArray.length(); i++) {
                JSONObject voteObject = voteArray.getJSONObject(i);
                Vote v = new Vote(voteObject);
                if (hasTimedOut(v))
                    l.log(Level.WARNING, "Purging out of date vote.", v);
                else
                    votes.add(v);
            }
            voteCache.put(((String) server), votes);
        }

    }

    public void save() throws IOException {
        cacheLock.lock();
        JSONObject votesObject = new JSONObject();
        try {
            Iterator<Map.Entry<String, Collection<Vote>>> entryItr = voteCache.entrySet().iterator();
            while (entryItr.hasNext()) {
                Map.Entry<String, Collection<Vote>> entry = entryItr.next();
                JSONArray array = new JSONArray();
                Iterator<Vote> voteItr = entry.getValue().iterator();
                while (voteItr.hasNext()) {
                    Vote vote = voteItr.next();

                    // if the vote is no longer valid, notify and remove
                    if (hasTimedOut(vote)) {
                        l.log(Level.WARNING, "Purging out of date vote.", vote);
                        voteItr.remove();
                    } else {
                        array.put(vote.serialize());
                    }

                }

                // if, during our iteration, we TTL invalidated all of the votes
                if (entry.getValue().isEmpty())
                    entryItr.remove();

                votesObject.put(entry.getKey(), array);
            }
        } finally {
            cacheLock.unlock();
        }

        try (BufferedWriter writer = Files.newWriter(cacheFile, StandardCharsets.UTF_8)) {
            votesObject.write(writer);
        }
    }

    private boolean hasTimedOut(Vote v) {
        if (voteTTL == -1) return false;
        // scale voteTTL to milliseconds
        return v.getLocalTimestamp() + voteTTL * 24 * 60 * 60 * 1000 < System.currentTimeMillis();
    }

    public void halt() throws IOException {
        saveTask.cancel();
        save();
    }

}
