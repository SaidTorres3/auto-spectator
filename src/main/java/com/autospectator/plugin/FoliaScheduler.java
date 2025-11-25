package com.autospectator.plugin;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.function.Consumer;

/**
 * Utility class for scheduling tasks that works on both Paper/Spigot and Folia.
 * Folia uses regionized threading, so tasks must be scheduled on the correct region.
 */
public class FoliaScheduler {

    private static Boolean isFolia = null;
    private static Method getGlobalRegionSchedulerMethod = null;
    private static Method getRegionSchedulerMethod = null;

    /**
     * Check if the server is running Folia
     */
    public static boolean isFolia() {
        if (isFolia == null) {
            try {
                Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
                isFolia = true;
            } catch (ClassNotFoundException e) {
                isFolia = false;
            }
        }
        return isFolia;
    }

    /**
     * Run a task on the global region scheduler (Folia) or main thread (Paper/Spigot)
     */
    public static void runTask(Plugin plugin, Runnable task) {
        if (isFolia()) {
            try {
                Object scheduler = getGlobalRegionScheduler(plugin);
                Method runMethod = scheduler.getClass().getMethod("run", Plugin.class, Consumer.class);
                runMethod.invoke(scheduler, plugin, (Consumer<Object>) scheduledTask -> task.run());
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to schedule global task on Folia: " + e.getMessage());
            }
        } else {
            Bukkit.getScheduler().runTask(plugin, task);
        }
    }

    /**
     * Run a repeating task on the global region scheduler (Folia) or main thread (Paper/Spigot)
     * @param delayTicks Initial delay in ticks
     * @param periodTicks Period between executions in ticks
     */
    public static void runTaskTimer(Plugin plugin, Runnable task, long delayTicks, long periodTicks) {
        if (isFolia()) {
            try {
                Object scheduler = getGlobalRegionScheduler(plugin);
                Method runAtFixedRateMethod = scheduler.getClass().getMethod("runAtFixedRate", 
                    Plugin.class, Consumer.class, long.class, long.class);
                // Folia uses ticks, minimum 1 tick delay
                long delay = Math.max(1, delayTicks);
                long period = Math.max(1, periodTicks);
                runAtFixedRateMethod.invoke(scheduler, plugin, (Consumer<Object>) scheduledTask -> task.run(), delay, period);
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to schedule repeating task on Folia: " + e.getMessage());
            }
        } else {
            Bukkit.getScheduler().runTaskTimer(plugin, task, delayTicks, periodTicks);
        }
    }

    /**
     * Run a task for a specific entity (on their region in Folia)
     */
    public static void runEntityTask(Plugin plugin, Entity entity, Runnable task) {
        if (isFolia()) {
            try {
                Method getSchedulerMethod = entity.getClass().getMethod("getScheduler");
                Object entityScheduler = getSchedulerMethod.invoke(entity);
                Method runMethod = entityScheduler.getClass().getMethod("run", Plugin.class, Consumer.class, Runnable.class);
                runMethod.invoke(entityScheduler, plugin, (Consumer<Object>) scheduledTask -> task.run(), null);
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to schedule entity task on Folia: " + e.getMessage());
            }
        } else {
            Bukkit.getScheduler().runTask(plugin, task);
        }
    }

    /**
     * Run a task at a specific location (on that region in Folia)
     */
    public static void runAtLocation(Plugin plugin, Location location, Runnable task) {
        if (isFolia()) {
            try {
                Object scheduler = getRegionScheduler(plugin);
                Method runMethod = scheduler.getClass().getMethod("run", Plugin.class, Location.class, Consumer.class);
                runMethod.invoke(scheduler, plugin, location, (Consumer<Object>) scheduledTask -> task.run());
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to schedule location task on Folia: " + e.getMessage());
            }
        } else {
            Bukkit.getScheduler().runTask(plugin, task);
        }
    }

    /**
     * Teleport a player safely (handles Folia's async teleport)
     */
    public static void teleportAsync(Plugin plugin, Player player, Location location, Runnable onComplete) {
        if (isFolia()) {
            // In Folia, we need to use async teleport
            try {
                Method teleportAsyncMethod = player.getClass().getMethod("teleportAsync", Location.class);
                Object future = teleportAsyncMethod.invoke(player, location);
                // The future is a CompletableFuture<Boolean>
                if (future != null && onComplete != null) {
                    Method thenRunMethod = future.getClass().getMethod("thenRun", Runnable.class);
                    thenRunMethod.invoke(future, onComplete);
                }
            } catch (Exception e) {
                // Fallback to sync teleport
                runEntityTask(plugin, player, () -> {
                    player.teleport(location);
                    if (onComplete != null) onComplete.run();
                });
            }
        } else {
            player.teleport(location);
            if (onComplete != null) onComplete.run();
        }
    }

    /**
     * Teleport a player safely without callback
     */
    public static void teleportAsync(Plugin plugin, Player player, Location location) {
        teleportAsync(plugin, player, location, null);
    }

    private static Object getGlobalRegionScheduler(Plugin plugin) throws Exception {
        if (getGlobalRegionSchedulerMethod == null) {
            getGlobalRegionSchedulerMethod = Bukkit.class.getMethod("getGlobalRegionScheduler");
        }
        return getGlobalRegionSchedulerMethod.invoke(null);
    }

    private static Object getRegionScheduler(Plugin plugin) throws Exception {
        if (getRegionSchedulerMethod == null) {
            getRegionSchedulerMethod = Bukkit.class.getMethod("getRegionScheduler");
        }
        return getRegionSchedulerMethod.invoke(null);
    }
}
