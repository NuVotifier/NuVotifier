package com.vexsoftware.votifier.bungee;

import com.vexsoftware.votifier.platform.scheduler.ScheduledVotifierTask;
import com.vexsoftware.votifier.platform.scheduler.VotifierScheduler;
import net.md_5.bungee.api.scheduler.ScheduledTask;

import java.util.concurrent.TimeUnit;

class BungeeScheduler implements VotifierScheduler {
    private final NuVotifier plugin;

    public BungeeScheduler(NuVotifier plugin) {
        this.plugin = plugin;
    }

    @Override
    public ScheduledVotifierTask sync(Runnable runnable) {
        return onPool(runnable);
    }

    @Override
    public ScheduledVotifierTask onPool(Runnable runnable) {
        return new BungeeTaskWrapper(plugin.getProxy().getScheduler().runAsync(plugin, runnable));
    }

    @Override
    public ScheduledVotifierTask delayedSync(Runnable runnable, int delay, TimeUnit unit) {
        return delayedOnPool(runnable, delay, unit);
    }

    @Override
    public ScheduledVotifierTask delayedOnPool(Runnable runnable, int delay, TimeUnit unit) {
        return new BungeeTaskWrapper(plugin.getProxy().getScheduler().schedule(plugin, runnable, delay, unit));
    }

    @Override
    public ScheduledVotifierTask repeatOnPool(Runnable runnable, int delay, int repeat, TimeUnit unit) {
        return new BungeeTaskWrapper(plugin.getProxy().getScheduler().schedule(plugin, runnable, delay, repeat, unit));
    }

    private static class BungeeTaskWrapper implements ScheduledVotifierTask {
        private final ScheduledTask task;

        private BungeeTaskWrapper(ScheduledTask task) {
            this.task = task;
        }

        @Override
        public void cancel() {
            this.task.cancel();
        }
    }
}
