package com.vexsoftware.votifier.support.forwarding.cache;

import com.google.common.io.Files;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.vexsoftware.votifier.model.Vote;
import com.vexsoftware.votifier.platform.VotifierPlugin;
import com.vexsoftware.votifier.platform.scheduler.ScheduledVotifierTask;
import com.vexsoftware.votifier.util.GsonInst;
import org.slf4j.Logger;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class FileVoteCache extends MemoryVoteCache {

    private final Logger l;
    private final File cacheFile;
    private final int voteTTL;
    private final ScheduledVotifierTask saveTask;

    public FileVoteCache(int initialMemorySize, final VotifierPlugin plugin, File cacheFile, int voteTTL) throws IOException {
        super(initialMemorySize);
        this.cacheFile = cacheFile;
        this.voteTTL = voteTTL;
        this.l = plugin.getPluginLogger();

        load();

        saveTask = plugin.getScheduler().repeatOnPool(() -> {
            try {
                save();
            } catch (IOException e) {
                l.error("Unable to save cached votes, votes will be lost if you restart.", e);
            }
        }, 3, 3, TimeUnit.MINUTES);
    }

    private void load() throws IOException {
        // Load the cache from disk
        JsonObject object;
        try (BufferedReader reader = Files.newReader(cacheFile, StandardCharsets.UTF_8)) {
            object = GsonInst.gson.fromJson(reader, JsonObject.class);
        } catch (FileNotFoundException e) {
            object = new JsonObject();
        }

        boolean resave = false;

        // First, lets figure out if we are converting from a pre2.3.6 cache
        if (!object.has("players") || !object.has("servers") || !object.has("version") || object.size() != 3) {
            JsonObject oldObject = object;
            object = new JsonObject();
            object.add("servers", oldObject);
            object.add("players", new JsonObject());
            object.addProperty("version", 2);
            resave = true;
        }

        if (object.get("version").getAsInt() != 2)
            throw new IllegalStateException("Could not read cache file! Unknown version '" + object.get("version").getAsInt() + "' read.");

        JsonObject players = object.getAsJsonObject("players");
        JsonObject servers = object.getAsJsonObject("servers");

        for (String player : players.keySet()) {
            playerVoteCache.put(player, readVotes(players.getAsJsonArray(player)));
        }

        for (String server : servers.keySet()) {
            voteCache.put(server, readVotes(servers.getAsJsonArray(server)));
        }

        if (resave) {
            File replacementFile;
            for (int i = 0; ; i++) {
                replacementFile = new File(cacheFile.getParentFile(), cacheFile.getName() + ".bak." + i);
                if (!replacementFile.exists())
                    break;
            }

            if (!cacheFile.renameTo(replacementFile)) {
                l.error("Backup movement failed! Will not save.");
                return;
            }

            l.warn("Saving new vote cache format to file - backup moved to " + replacementFile.getAbsolutePath());
            save();
        }
    }

    private Collection<Vote> readVotes(JsonArray voteArray) {
        List<Vote> votes = new ArrayList<>(voteArray.size());
        for (int i = 0; i < voteArray.size(); i++) {
            JsonObject voteObject = voteArray.get(i).getAsJsonObject();
            Vote v = new Vote(voteObject);
            if (hasTimedOut(v))
                l.warn("Purging out of date vote.", v);
            else
                votes.add(v);
        }
        return votes;
    }

    public void save() throws IOException {
        cacheLock.lock();
        JsonObject votesObject = new JsonObject();
        votesObject.addProperty("version", 2);
        try {
            votesObject.add("players", serializeMap(playerVoteCache));
            votesObject.add("servers", serializeMap(voteCache));
        } finally {
            cacheLock.unlock();
        }

        try (BufferedWriter writer = Files.newWriter(cacheFile, StandardCharsets.UTF_8)) {
            GsonInst.gson.toJson(votesObject, writer);
        }
    }

    public JsonObject serializeMap(Map<String, Collection<Vote>> map) {
        JsonObject o = new JsonObject();

        Iterator<Map.Entry<String, Collection<Vote>>> entryItr = map.entrySet().iterator();
        while (entryItr.hasNext()) {
            Map.Entry<String, Collection<Vote>> entry = entryItr.next();
            JsonArray array = new JsonArray();
            Iterator<Vote> voteItr = entry.getValue().iterator();
            while (voteItr.hasNext()) {
                Vote vote = voteItr.next();

                // if the vote is no longer valid, notify and remove
                if (hasTimedOut(vote)) {
                    l.warn("Purging out of date vote.", vote);
                    voteItr.remove();
                } else {
                    array.add(vote.serialize());
                }

            }

            // if, during our iteration, we TTL invalidated all of the votes
            if (entry.getValue().isEmpty())
                entryItr.remove();
            o.add(entry.getKey(), array);
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
