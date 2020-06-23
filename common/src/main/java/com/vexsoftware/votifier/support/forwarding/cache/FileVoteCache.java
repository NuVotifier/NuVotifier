package com.vexsoftware.votifier.support.forwarding.cache;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.vexsoftware.votifier.platform.LoggingAdapter;
import com.vexsoftware.votifier.platform.VotifierPlugin;
import com.vexsoftware.votifier.platform.scheduler.ScheduledVotifierTask;
import com.vexsoftware.votifier.util.GsonInst;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class FileVoteCache extends MemoryVoteCache {

    private final LoggingAdapter l;
    private final File cacheFile;
    private final ScheduledVotifierTask saveTask;

    public FileVoteCache(final VotifierPlugin plugin, File cacheFile, long voteTTL) throws IOException {
        super(plugin, voteTTL);
        this.cacheFile = cacheFile;
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

    private static Set<String> keySet(JsonObject object) {
        Set<String> set = new HashSet<>();
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            set.add(entry.getKey());
        }
        return set;
    }

    private void load() throws IOException {
        // Load the cache from disk
        JsonObject object;
        try (BufferedReader reader = Files.newBufferedReader(cacheFile.toPath(), StandardCharsets.UTF_8)) {
            object = GsonInst.gson.fromJson(reader, JsonObject.class);
            if (object == null) {
                // When the input is not malformed but instead empty, this returns null. Simply assume it is empty.
                object = new JsonObject();
            }
        } catch (NoSuchFileException e) {
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

        for (String player : keySet(players)) {
            playerVoteCache.put(player, readVotes(players.getAsJsonArray(player)));
        }

        for (String server : keySet(servers)) {
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

    private Collection<VoteWithRecordedTimestamp> readVotes(JsonArray voteArray) {
        Collection<VoteWithRecordedTimestamp> votes = new HashSet<>(voteArray.size());
        for (int i = 0; i < voteArray.size(); i++) {
            JsonObject voteObject = voteArray.get(i).getAsJsonObject();
            VoteWithRecordedTimestamp v = new VoteWithRecordedTimestamp(voteObject);
            if (hasTimedOut(v))
                l.warn("Purging out of date vote.", v);
            else
                votes.add(v);
        }
        return votes;
    }

    public void save() throws IOException {
        JsonObject votesObject = new JsonObject();

        cacheLock.lock();
        try {
            votesObject.addProperty("version", 2);
            votesObject.add("players", serializeMap(playerVoteCache));
            votesObject.add("servers", serializeMap(voteCache));
        } finally {
            cacheLock.unlock();
        }

        try (BufferedWriter writer = Files.newBufferedWriter(cacheFile.toPath(), StandardCharsets.UTF_8)) {
            GsonInst.gson.toJson(votesObject, writer);
        }
    }

    public JsonObject serializeMap(Map<String, Collection<VoteWithRecordedTimestamp>> map) {
        JsonObject o = new JsonObject();

        Iterator<Map.Entry<String, Collection<VoteWithRecordedTimestamp>>> entryItr = map.entrySet().iterator();
        while (entryItr.hasNext()) {
            Map.Entry<String, Collection<VoteWithRecordedTimestamp>> entry = entryItr.next();
            JsonArray array = new JsonArray();
            Iterator<VoteWithRecordedTimestamp> voteItr = entry.getValue().iterator();
            while (voteItr.hasNext()) {
                VoteWithRecordedTimestamp vote = voteItr.next();

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


    public void halt() throws IOException {
        saveTask.cancel();
        save();
    }
}
