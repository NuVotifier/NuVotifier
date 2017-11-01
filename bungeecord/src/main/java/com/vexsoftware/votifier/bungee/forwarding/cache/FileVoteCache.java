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

        boolean resave = false;

        // First, lets figure out if we are converting from a pre2.3.6 cache
        if (!object.has("players") || !object.has("servers") || !object.has("version") || object.length() != 3) {
            JSONObject oldObject = object;
            object = new JSONObject();
            object.put("servers", oldObject);
            object.put("players", new JSONObject());
            object.put("version", 2);
            resave = true;
        }

        if (object.getInt("version") != 2)
            throw new IllegalStateException("Could not read cache file! Unknown version '" + object.getInt("version") + "' read.");

        JSONObject players = object.getJSONObject("players");
        JSONObject servers = object.getJSONObject("servers");

        for (Object player : players.keySet()) {
            playerVoteCache.put(((String) player), readVotes(players.getJSONArray((String) player)));
        }

        for (Object server : servers.keySet()) {
            voteCache.put(((String) server), readVotes(players.getJSONArray((String) server)));
        }

        if (resave) {
            File replacementFile;
            for (int i = 0; ; i++) {
                replacementFile = new File(cacheFile.getParentFile(), cacheFile.getName() + ".bak." + i);
                if (!replacementFile.exists())
                    break;
            }

            if (!cacheFile.renameTo(replacementFile)) {
                l.log(Level.SEVERE, "Backup movement failed! Will not save.");
                return;
            }

            l.log(Level.WARNING, "Saving new vote cache format to file - backup moved to " + replacementFile.getAbsolutePath());
            save();
        }
    }

    private Collection<Vote> readVotes(JSONArray voteArray) {
        List<Vote> votes = new ArrayList<>(voteArray.length());
        for (int i = 0; i < voteArray.length(); i++) {
            JSONObject voteObject = voteArray.getJSONObject(i);
            Vote v = new Vote(voteObject);
            if (hasTimedOut(v))
                l.log(Level.WARNING, "Purging out of date vote.", v);
            else
                votes.add(v);
        }
        return votes;
    }

    public void save() throws IOException {
        cacheLock.lock();
        JSONObject votesObject = new JSONObject();
        votesObject.put("version", 2);
        try {
            votesObject.put("players", serializeMap(playerVoteCache));
            votesObject.put("servers", serializeMap(voteCache));
        } finally {
            cacheLock.unlock();
        }

        try (BufferedWriter writer = Files.newWriter(cacheFile, StandardCharsets.UTF_8)) {
            votesObject.write(writer);
        }
    }

    public JSONObject serializeMap(Map<String, Collection<Vote>> map) {
        JSONObject o = new JSONObject();

        Iterator<Map.Entry<String, Collection<Vote>>> entryItr = map.entrySet().iterator();
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
            o.put(entry.getKey(), o);
        }
        return o;
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
