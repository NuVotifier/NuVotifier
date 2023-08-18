package com.vexsoftware.votifier.support.forwarding.redis;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.vexsoftware.votifier.model.Vote;
import com.vexsoftware.votifier.support.forwarding.ForwardedVoteListener;
import com.vexsoftware.votifier.support.forwarding.ForwardingVoteSink;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisPubSub;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * @author AkramL
 */
public class RedisForwardingSink extends JedisPubSub implements ForwardingVoteSink {

    public RedisForwardingSink(RedisCredentials credentials,
                               RedisPoolConfiguration poolConfiguration,
                               ForwardedVoteListener listener) {

        this.channel = credentials.getChannel();
        this.listener = listener;

        JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();

        jedisPoolConfig.setMaxTotal(poolConfiguration.getMaxTotal());
        jedisPoolConfig.setMaxIdle(poolConfiguration.getMaxIdle());
        jedisPoolConfig.setMinIdle(poolConfiguration.getMinIdle());
        jedisPoolConfig.setMinEvictableIdleTime(Duration.ofMillis(poolConfiguration.getMinEvictableIdleTime()));
        jedisPoolConfig.setTimeBetweenEvictionRuns(Duration.ofMillis(poolConfiguration.getTimeBetweenEvictionRuns()));
        jedisPoolConfig.setBlockWhenExhausted(poolConfiguration.isBlockWhenExhausted());

        this.pool = new JedisPool(jedisPoolConfig,
                credentials.getHost(),
                credentials.getPort(),
                5000,
                credentials.getPassword());

        CompletableFuture.runAsync(() -> {
            try (Jedis jedis = pool.getResource()) {
                jedis.subscribe(this, channel);
            }
        }).exceptionally(exception -> {
            exception.printStackTrace();
            return null;
        });
    }

    private final ForwardedVoteListener listener;
    private final String channel;
    private final JedisPool pool;
    private final Gson gson = new Gson();

    public void handleMessage(String message) {
        JsonObject object = gson.fromJson(message, JsonObject.class);
        Vote vote = new Vote(object);
        listener.onForward(vote);
    }

    @Override
    public void onMessage(String channel, String message) {
        if (channel.equals(this.channel)) {
            // Using try-catch block to avoid channel break on exceptions.
            try {
                handleMessage(message);
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        }
    }

    @Override
    public void halt() {
    }
}
