package com.vexsoftware.votifier.support.forwarding.redis;

/**
 * @author AkramL
 */
public class RedisPoolConfiguration {

    private final int timeout, maxTotal, maxIdle, minIdle, minEvictableIdleTime, timeBetweenEvictionRuns,
            numTestsPerEvictionRun;
    private final boolean blockWhenExhausted;

    private RedisPoolConfiguration(int timeout,
                                  int minIdle,
                                  int maxIdle,
                                  int maxTotal,
                                  int minEvictableIdleTime,
                                  int timeBetweenEvictionRuns,
                                  int numTestsPerEvictionRun,
                                  boolean blockWhenExhausted) {
        this.timeout = timeout;
        this.minIdle = minIdle;
        this.maxIdle = maxIdle;
        this.maxTotal = maxTotal;
        this.minEvictableIdleTime = minEvictableIdleTime;
        this.timeBetweenEvictionRuns = timeBetweenEvictionRuns;
        this.numTestsPerEvictionRun = numTestsPerEvictionRun;
        this.blockWhenExhausted = blockWhenExhausted;
    }

    public boolean isBlockWhenExhausted() {
        return blockWhenExhausted;
    }

    public int getMaxIdle() {
        return maxIdle;
    }

    public int getMaxTotal() {
        return maxTotal;
    }

    public int getMinEvictableIdleTime() {
        return minEvictableIdleTime;
    }

    public int getMinIdle() {
        return minIdle;
    }

    public int getNumTestsPerEvictionRun() {
        return numTestsPerEvictionRun;
    }

    public int getTimeBetweenEvictionRuns() {
        return timeBetweenEvictionRuns;
    }

    public int getTimeout() {
        return timeout;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Builder() {
        }

        private int timeout, maxTotal, maxIdle, minIdle, minEvictableIdleTime, timeBetweenEvictionRuns,
                numTestsPerEvictionRun;
        private boolean blockWhenExhausted;

        public Builder timeout(int timeout) {
            this.timeout = timeout;
            return this;
        }

        public Builder maxTotal(int maxTotal) {
            this.maxTotal = maxTotal;
            return this;
        }

        public Builder maxIdle(int maxIdle) {
            this.maxIdle = maxIdle;
            return this;
        }

        public Builder minIdle(int minIdle) {
            this.minIdle = minIdle;
            return this;
        }

        public Builder minEvictableIdleTime(int minEvictableIdleTime) {
            this.minEvictableIdleTime = minEvictableIdleTime;
            return this;
        }

        public Builder timeBetweenEvictionRuns(int timeBetweenEvictionRuns) {
            this.timeBetweenEvictionRuns = timeBetweenEvictionRuns;
            return this;
        }

        public Builder numTestsPerEvictionRun(int numTestsPerEvictionRun) {
            this.numTestsPerEvictionRun = numTestsPerEvictionRun;
            return this;
        }

        public Builder blockWhenExhausted(boolean blockWhenExhausted) {
            this.blockWhenExhausted = blockWhenExhausted;
            return this;
        }

        public RedisPoolConfiguration build() {
            return new RedisPoolConfiguration(timeout,
                    minIdle,
                    maxIdle,
                    maxTotal,
                    minEvictableIdleTime,
                    timeBetweenEvictionRuns,
                    numTestsPerEvictionRun,
                    blockWhenExhausted);
        }
    }
}
