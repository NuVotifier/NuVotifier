package com.vexsoftware.votifier.net;

import com.vexsoftware.votifier.platform.LoggingAdapter;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GlobalEventExecutor;

import java.util.concurrent.TimeUnit;

final class NettyShutdownVerifier {
    private final LoggingAdapter logger;

    NettyShutdownVerifier(LoggingAdapter logger) {
        this.logger = logger;
    }

    void awaitTermination(Future<?> channelCloseFuture, Future<?> bossShutdownFuture,
                          Future<?> workerShutdownFuture, long timeout, TimeUnit unit) {
        long deadline = System.nanoTime() + unit.toNanos(timeout);

        if (channelCloseFuture != null) {
            awaitFuture("server channel", channelCloseFuture, deadline);
        }
        boolean bossTerminated = awaitFuture("boss event loop", bossShutdownFuture, deadline);
        boolean workerTerminated = awaitFuture("worker event loop", workerShutdownFuture, deadline);

        if (bossTerminated && workerTerminated) {
            awaitGlobalEventExecutor(deadline);
        }
    }

    private boolean awaitFuture(String resource, Future<?> future, long deadline) {
        if (!future.isDone()) {
            long remaining = deadline - System.nanoTime();
            if (remaining > 0) {
                future.awaitUninterruptibly(remaining, TimeUnit.NANOSECONDS);
            }
        }

        if (!future.isDone()) {
            logger.warn("Timed out waiting for Votifier " + resource + " to shut down.");
            return false;
        }

        if (!future.isSuccess()) {
            logger.error("Unable to shutdown Votifier " + resource, future.cause());
            return false;
        }

        return true;
    }

    private void awaitGlobalEventExecutor(long deadline) {
        long remainingMillis = TimeUnit.NANOSECONDS.toMillis(deadline - System.nanoTime());
        if (remainingMillis <= 0) {
            logger.warn("Timed out waiting for the Votifier global event executor to shut down.");
            return;
        }

        try {
            if (!GlobalEventExecutor.INSTANCE.awaitInactivity(remainingMillis, TimeUnit.MILLISECONDS)) {
                logger.warn("Timed out waiting for the Votifier global event executor to shut down.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("Interrupted while waiting for the Votifier global event executor to shut down.");
        } catch (IllegalStateException ignored) {
            // The executor had no work and never started.
        }
    }
}
