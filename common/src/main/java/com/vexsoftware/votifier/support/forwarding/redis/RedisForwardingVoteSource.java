package com.vexsoftware.votifier.support.forwarding.redis;

import com.google.gson.Gson;
import com.vexsoftware.votifier.model.Vote;
import com.vexsoftware.votifier.support.forwarding.ForwardingVoteSource;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.time.Duration;

/**
 * @author AkramL
 */
public class RedisForwardingVoteSource implements ForwardingVoteSource {

    private final JedisPool pool;
    private final Gson gson = new Gson();
    private final String channel;

    public RedisForwardingVoteSource(RedisCredentials credentials,
                                     RedisPoolConfiguration configuration) {

        this.channel = credentials.getChannel();

        JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();

        jedisPoolConfig.setMaxTotal(configuration.getMaxTotal());
        jedisPoolConfig.setMaxIdle(configuration.getMaxIdle());
        jedisPoolConfig.setMinIdle(configuration.getMinIdle());
        jedisPoolConfig.setMinEvictableIdleTime(Duration.ofMillis(configuration.getMinEvictableIdleTime()));
        jedisPoolConfig.setTimeBetweenEvictionRuns(Duration.ofMillis(configuration.getTimeBetweenEvictionRuns()));
        jedisPoolConfig.setBlockWhenExhausted(configuration.isBlockWhenExhausted());

        this.pool = new JedisPool(jedisPoolConfig,
                credentials.getHost(),
                credentials.getPort(),
                5000,
                credentials.getPassword());
    }

    @Override
    public void forward(Vote v) {
        try (Jedis jedis = pool.getResource()) {
            jedis.publish(channel, gson.toJson(v.serialize()));
        }
    }

    @Override
    public void halt() {

    }
}
