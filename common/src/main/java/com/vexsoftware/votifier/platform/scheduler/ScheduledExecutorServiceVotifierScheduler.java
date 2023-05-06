package com.vexsoftware.votifier.platform.scheduler;

import java.util.Objects;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ScheduledExecutorServiceVotifierScheduler implements VotifierScheduler {
    private final ScheduledExecutorService service;

    public ScheduledExecutorServiceVotifierScheduler(ScheduledExecutorService service) {
        this.service = Objects.requireNonNull(service, "service");
    }

    @Override
    public ScheduledVotifierTask delayedOnPool(Runnable runnable, int delay, TimeUnit unit) {
        return new ScheduledVotifierTaskWrapper(service.schedule(runnable, delay, unit));
    }

    @Override
    public ScheduledVotifierTask repeatOnPool(Runnable runnable, int delay, int repeat, TimeUnit unit) {
        return new ScheduledVotifierTaskWrapper(service.scheduleAtFixedRate(runnable, delay, repeat, unit));
    }

    private static class ScheduledVotifierTaskWrapper implements ScheduledVotifierTask {
        private final Future<?> future;

        private ScheduledVotifierTaskWrapper(Future<?> future) {
            this.future = future;
        }

        @Override
        public void cancel() {
            future.cancel(false);
        }
    }
}
