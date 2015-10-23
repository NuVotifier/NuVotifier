package com.vexsoftware.votifier.bungee.forwarding.cache;

import com.google.common.io.Files;
import com.vexsoftware.votifier.model.Vote;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class FileVoteCache extends MemoryVoteCache {

    private final File cacheFile;

    public FileVoteCache(int initialMemorySize, File cacheFile) throws IOException {
        super(initialMemorySize);
        this.cacheFile = cacheFile;
        load();
    }

    private void load() throws IOException {
        if (cacheFile.exists()) {
            // Load the cache from disk
            JSONObject object = new JSONObject(Files.toString(cacheFile, StandardCharsets.UTF_8));

            // Deserialize all votes contained
            for (String server : object.keySet()) {
                JSONArray voteArray = object.optJSONArray(server);
                if (voteArray == null) continue;
                Set<Vote> votes = new LinkedHashSet<>(voteArray.length());
                for (int i = 0; i < voteArray.length(); i++) {
                    votes.add(new Vote(voteArray.getJSONObject(i)));
                }
                voteCache.put(server, votes);
            }
        } else {
            cacheFile.createNewFile();
        }
    }

    public void save() throws IOException {
        cacheLock.lock();
        // Create a copy of the votes.
        JSONObject votesObject = new JSONObject();
        for (Map.Entry<String, Collection<Vote>> entry : voteCache.entrySet()) {
            JSONArray array = new JSONArray();
            for (Vote vote : entry.getValue()) {
                array.put(vote.serialize());
            }
            votesObject.put(entry.getKey(), array);
        }
        cacheLock.unlock();

        try (BufferedWriter writer = Files.newWriter(cacheFile, StandardCharsets.UTF_8)) {
            votesObject.write(writer);
        }
    }

}
