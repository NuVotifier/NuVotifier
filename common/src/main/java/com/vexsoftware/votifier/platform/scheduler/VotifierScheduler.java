package com.vexsoftware.votifier.platform.scheduler;

import java.util.concurrent.TimeUnit;

public interface VotifierScheduler {
    ScheduledVotifierTask sync(Runnable runnable);

    ScheduledVotifierTask onPool(Runnable runnable);

    ScheduledVotifierTask delayedSync(Runnable runnable, int delay, TimeUnit unit);

    ScheduledVotifierTask delayedOnPool(Runnable runnable, int delay, TimeUnit unit);

    ScheduledVotifierTask repeatOnPool(Runnable runnable, int delay, int repeat, TimeUnit unit);
}
