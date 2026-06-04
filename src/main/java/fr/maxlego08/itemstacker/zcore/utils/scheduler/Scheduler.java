package fr.maxlego08.itemstacker.zcore.utils.scheduler;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

/**
 * Cross-platform scheduler that works both on Bukkit/Spigot/Paper and on Folia.
 * <p>
 * On Folia the legacy {@code Bukkit.getScheduler()} throws
 * {@link UnsupportedOperationException}, so every task must be dispatched through
 * the appropriate regionized scheduler (async, global region or per-entity).
 *
 * @author Maxlego08
 */
public final class Scheduler {

    private static final boolean FOLIA;

    static {
        boolean folia;
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            folia = true;
        } catch (ClassNotFoundException exception) {
            folia = false;
        }
        FOLIA = folia;
    }

    private Scheduler() {
    }

    /**
     * A handle to a scheduled task that can be cancelled, abstracting away the
     * Bukkit / Folia task types.
     */
    public interface Task {
        void cancel();
    }

    /**
     * @return true if the server is running Folia.
     */
    public static boolean isFolia() {
        return FOLIA;
    }

    /**
     * Runs a task asynchronously, off the main/region threads.
     *
     * @param plugin   the owning plugin.
     * @param runnable the task to run.
     */
    public static void runAsync(Plugin plugin, Runnable runnable) {
        if (FOLIA) {
            Bukkit.getAsyncScheduler().runNow(plugin, task -> runnable.run());
        } else {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, runnable);
        }
    }

    /**
     * Runs a task on the main thread (Bukkit) or on the global region thread (Folia).
     *
     * @param plugin   the owning plugin.
     * @param runnable the task to run.
     */
    public static void runGlobal(Plugin plugin, Runnable runnable) {
        if (FOLIA) {
            Bukkit.getGlobalRegionScheduler().run(plugin, task -> runnable.run());
        } else {
            Bukkit.getScheduler().runTask(plugin, runnable);
        }
    }

    /**
     * Runs a task on the thread owning the given entity after a delay, in ticks.
     * On Bukkit this falls back to the main thread scheduler.
     *
     * @param plugin     the owning plugin.
     * @param entity     the entity whose region owns the task.
     * @param runnable   the task to run.
     * @param delayTicks the delay in ticks before execution (minimum 1).
     */
    public static void runEntityLater(Plugin plugin, Entity entity, Runnable runnable, long delayTicks) {
        long delay = Math.max(1L, delayTicks);
        if (FOLIA) {
            entity.getScheduler().runDelayed(plugin, task -> runnable.run(), null, delay);
        } else {
            Bukkit.getScheduler().runTaskLater(plugin, runnable, delay);
        }
    }

    /**
     * Runs a repeating task on the main thread (Bukkit) or the global region
     * thread (Folia).
     *
     * @param plugin      the owning plugin.
     * @param runnable    the task to run on each iteration.
     * @param delayTicks  the initial delay in ticks (minimum 1 on Folia).
     * @param periodTicks the period between executions in ticks (minimum 1).
     * @return a cancellable {@link Task} handle.
     */
    public static Task runGlobalTimer(Plugin plugin, Runnable runnable, long delayTicks, long periodTicks) {
        long period = Math.max(1L, periodTicks);
        if (FOLIA) {
            long delay = Math.max(1L, delayTicks);
            var scheduledTask = Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, task -> runnable.run(), delay, period);
            return scheduledTask::cancel;
        } else {
            var bukkitTask = Bukkit.getScheduler().runTaskTimer(plugin, runnable, delayTicks, period);
            return bukkitTask::cancel;
        }
    }
}