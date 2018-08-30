package com.vexsoftware.votifier.platform.scheduler;

import com.google.common.base.Preconditions;

import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ScheduledExecutorServiceVotifierScheduler implements VotifierScheduler {
    private final ScheduledExecutorService service;

    public ScheduledExecutorServiceVotifierScheduler(ScheduledExecutorService service) {
        this.service = Preconditions.checkNotNull(service, "service");
    }

    @Override
    public ScheduledVotifierTask sync(Runnable runnable) {
        return new ScheduledVotifierTaskWrapper(service.submit(runnable));
    }

    @Override
    public ScheduledVotifierTask onPool(Runnable runnable) {
        return new ScheduledVotifierTaskWrapper(service.submit(runnable));
    }

    @Override
    public ScheduledVotifierTask delayedSync(Runnable runnable, int delay, TimeUnit unit) {
        return new ScheduledVotifierTaskWrapper(service.schedule(runnable, delay, unit));
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
