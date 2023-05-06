package com.vexsoftware.votifier.sponge;

import com.vexsoftware.votifier.platform.scheduler.ScheduledVotifierTask;
import com.vexsoftware.votifier.platform.scheduler.VotifierScheduler;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.scheduler.Task;

import java.util.concurrent.TimeUnit;

class SpongeScheduler implements VotifierScheduler {
    private final NuVotifier plugin;

    SpongeScheduler(NuVotifier plugin) {
        this.plugin = plugin;
    }

    private Task.Builder taskBuilder(Runnable runnable) {
        return Sponge.getScheduler().createTaskBuilder().execute(runnable);
    }

    @Override
    public ScheduledVotifierTask delayedOnPool(Runnable runnable, int delay, TimeUnit unit) {
        return new TaskWrapper(taskBuilder(runnable).delay(delay, unit).async().submit(plugin));
    }

    @Override
    public ScheduledVotifierTask repeatOnPool(Runnable runnable, int delay, int repeat, TimeUnit unit) {
        return new TaskWrapper(taskBuilder(runnable).delay(delay, unit).interval(repeat, unit).submit(plugin));
    }

    private static class TaskWrapper implements ScheduledVotifierTask {
        private final Task task;

        private TaskWrapper(Task task) {
            this.task = task;
        }

        @Override
        public void cancel() {
            task.cancel();
        }
    }
}
