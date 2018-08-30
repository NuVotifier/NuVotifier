package com.vexsoftware.votifier;

import com.vexsoftware.votifier.platform.scheduler.ScheduledVotifierTask;
import com.vexsoftware.votifier.platform.scheduler.VotifierScheduler;
import org.bukkit.scheduler.BukkitTask;

import java.util.concurrent.TimeUnit;

class BukkitScheduler implements VotifierScheduler {
    private final NuVotifierBukkit plugin;

    public BukkitScheduler(NuVotifierBukkit plugin) {
        this.plugin = plugin;
    }

    private int toTicks(int time, TimeUnit unit) {
        return (int) (unit.toMillis(time) / 50);
    }

    @Override
    public ScheduledVotifierTask sync(Runnable runnable) {
        return new BukkitTaskWrapper(plugin.getServer().getScheduler().runTask(plugin, runnable));
    }

    @Override
    public ScheduledVotifierTask onPool(Runnable runnable) {
        return new BukkitTaskWrapper(plugin.getServer().getScheduler().runTaskAsynchronously(plugin, runnable));
    }

    @Override
    public ScheduledVotifierTask delayedSync(Runnable runnable, int delay, TimeUnit unit) {
        return new BukkitTaskWrapper(plugin.getServer().getScheduler().runTaskLater(plugin, runnable, toTicks(delay, unit)));
    }

    @Override
    public ScheduledVotifierTask delayedOnPool(Runnable runnable, int delay, TimeUnit unit) {
        return new BukkitTaskWrapper(plugin.getServer().getScheduler().runTaskLaterAsynchronously(plugin, runnable, toTicks(delay, unit)));
    }

    @Override
    public ScheduledVotifierTask repeatOnPool(Runnable runnable, int delay, int repeat, TimeUnit unit) {
        return new BukkitTaskWrapper(plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, runnable, toTicks(delay, unit), toTicks(repeat, unit)));
    }

    private static class BukkitTaskWrapper implements ScheduledVotifierTask {
        private final BukkitTask task;

        private BukkitTaskWrapper(BukkitTask task) {
            this.task = task;
        }

        @Override
        public void cancel() {
            task.cancel();
        }
    }
}
