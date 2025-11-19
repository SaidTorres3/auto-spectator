package com.example.plugin;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.stream.Collectors;

public class SpectatorManager {

    private final Main plugin;
    private final Map<UUID, SpectatorSession> sessions = new HashMap<>();
    private final Random random = new Random();

    public SpectatorManager(Main plugin) {
        this.plugin = plugin;
        startTasks();
    }

    private void startTasks() {
        // Movement task - runs every tick
        new BukkitRunnable() {
            @Override
            public void run() {
                for (SpectatorSession session : sessions.values()) {
                    session.updateMovement();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);

        // Logic task - runs every second to check timers/cycling
        new BukkitRunnable() {
            @Override
            public void run() {
                for (SpectatorSession session : sessions.values()) {
                    session.tick();
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    public void toggleSpectator(Player player) {
        if (sessions.containsKey(player.getUniqueId())) {
            stopSpectating(player);
            player.sendMessage("§cAuto-spectator disabled.");
        } else {
            startSpectating(player);
            player.sendMessage("§aAuto-spectator enabled.");
        }
    }

    public void startSpectating(Player player) {
        player.setGameMode(GameMode.SPECTATOR);
        SpectatorSession session = new SpectatorSession(plugin, player);
        sessions.put(player.getUniqueId(), session);
        session.findNextTarget();
    }

    public void stopSpectating(Player player) {
        sessions.remove(player.getUniqueId());
        // Optional: Reset gamemode or leave in spectator? 
        // User didn't specify, but usually staying in spectator is safer for a "camera account".
    }

    public void setSpectateTime(Player player, int seconds) {
        if (sessions.containsKey(player.getUniqueId())) {
            sessions.get(player.getUniqueId()).setDuration(seconds);
            player.sendMessage("§aSpectate duration set to " + seconds + " seconds.");
        } else {
            player.sendMessage("§cYou are not in auto-spectator mode.");
        }
    }

    public void setTarget(Player spectator, String targetName) {
        if (!sessions.containsKey(spectator.getUniqueId())) {
            startSpectating(spectator);
        }
        
        Player target = Bukkit.getPlayer(targetName);
        if (target != null) {
            sessions.get(spectator.getUniqueId()).forceSpectate(target);
            spectator.sendMessage("§aNow spectating " + target.getName());
        } else {
            spectator.sendMessage("§cPlayer not found.");
        }
    }

    public void setAutoMode(Player spectator) {
        if (sessions.containsKey(spectator.getUniqueId())) {
            sessions.get(spectator.getUniqueId()).enableAutoMode();
            spectator.sendMessage("§aResumed auto-spectating cycle.");
        } else {
            startSpectating(spectator);
        }
    }

    public void handleTrigger(Player target, String reason) {
        // If any spectator is active, force them to watch this target
        // But only if they are in auto mode or if the trigger is important enough?
        // User said: "TP-inmediately when... The idea is to be able to catch is a user is about to die"
        
        for (SpectatorSession session : sessions.values()) {
            if (session.isAutoMode()) {
                session.triggerSpectate(target, reason);
            }
        }
    }

    private class SpectatorSession {
        private final Player spectator;
        private Player currentTarget;
        private boolean autoMode = true;
        private int duration;
        private int timeRemaining;
        
        // Movement variables
        private double angle = 0;

        public SpectatorSession(Main plugin, Player spectator) {
            this.spectator = spectator;
            this.duration = plugin.getConfig().getInt("spectate-duration", 15);
            this.timeRemaining = duration;
        }

        public void setDuration(int seconds) {
            this.duration = seconds;
            // If we are currently waiting, update remaining time? 
            // Maybe not necessary, just applies to next cycle or current countdown.
        }

        public boolean isAutoMode() {
            return autoMode;
        }

        public void enableAutoMode() {
            this.autoMode = true;
            findNextTarget();
        }

        public void forceSpectate(Player target) {
            this.autoMode = false;
            setTarget(target);
        }

        public void triggerSpectate(Player target, String reason) {
            // Only switch if we aren't already watching them
            if (currentTarget != null && currentTarget.equals(target)) {
                // Reset timer to ensure we keep watching them during the event
                timeRemaining = duration; 
                return;
            }
            
            // Switch to the triggered player
            setTarget(target);
            // Reset timer
            timeRemaining = duration;
            // plugin.getLogger().info("Triggered spectate on " + target.getName() + " due to " + reason);
        }

        private void setTarget(Player target) {
            this.currentTarget = target;
            // Reset movement parameters for a smooth transition or new angle
            angle = random.nextDouble() * Math.PI * 2;
            spectator.setSpectatorTarget(null); // Unlock camera so we can move it
        }

        public void tick() {
            if (!autoMode) return; // In single player mode, we don't cycle

            if (currentTarget == null || !currentTarget.isOnline() || currentTarget.isDead()) {
                findNextTarget();
                return;
            }

            timeRemaining--;
            if (timeRemaining <= 0) {
                findNextTarget();
            }
        }

        public void findNextTarget() {
            List<Player> players = Bukkit.getOnlinePlayers().stream()
                .filter(p -> !p.equals(spectator)) // Don't spectate self
                .filter(p -> p.getGameMode() != GameMode.SPECTATOR) // Don't spectate other spectators
                .sorted(Comparator.comparing(Player::getName))
                .collect(Collectors.toList());

            if (players.isEmpty()) {
                currentTarget = null;
                return;
            }

            if (currentTarget == null) {
                setTarget(players.get(0));
            } else {
                int index = players.indexOf(currentTarget);
                if (index == -1 || index + 1 >= players.size()) {
                    setTarget(players.get(0)); // Loop back to start
                } else {
                    setTarget(players.get(index + 1));
                }
            }
            timeRemaining = duration;
        }

        public void updateMovement() {
            if (currentTarget == null || !currentTarget.isOnline()) return;

            // Cinematic movement logic with reduced rotation speed
            angle += 0.008;
            
            Location targetLoc = currentTarget.getLocation();
            
            // Check all angles and count blocked blocks
            int[] blockCounts = new int[8];
            boolean[] isBlocked = new boolean[8];
            int minBlocks = Integer.MAX_VALUE;
            int bestAngleIndex = -1;
            boolean anyAngleClear = false;
            
            for (int i = 0; i < 8; i++) {
                double testAngle = angle + (Math.PI / 4) * i;
                double testX = 5.0 * Math.cos(testAngle);
                double testZ = 5.0 * Math.sin(testAngle);
                double hoverHeight = 3.0 + Math.sin(testAngle * 0.3) * 0.5;
                
                Location testCamLoc = targetLoc.clone().add(testX, hoverHeight, testZ);
                
                int blockCount = countBlocksInLineOfSight(testCamLoc, targetLoc);
                blockCounts[i] = blockCount;
                isBlocked[i] = blockCount > 0;
                
                // Track the angle with least blocks
                if (blockCount < minBlocks) {
                    minBlocks = blockCount;
                    bestAngleIndex = i;
                    if (blockCount == 0) {
                        anyAngleClear = true;
                    }
                }
            }
            
            Location camLoc;
            
            if (anyAngleClear) {
                // Use the angle with zero blocks
                double bestAngle = angle + (Math.PI / 4) * bestAngleIndex;
                double bestX = 5.0 * Math.cos(bestAngle);
                double bestZ = 5.0 * Math.sin(bestAngle);
                double bestHoverHeight = 3.0 + Math.sin(bestAngle * 0.3) * 0.5;
                camLoc = targetLoc.clone().add(bestX, bestHoverHeight, bestZ);
                angle = bestAngle;
            } else if (minBlocks < Integer.MAX_VALUE && minBlocks > 0) {
                // All angles blocked, use the one with least blocks
                double bestAngle = angle + (Math.PI / 4) * bestAngleIndex;
                double bestX = 5.0 * Math.cos(bestAngle);
                double bestZ = 5.0 * Math.sin(bestAngle);
                double bestHoverHeight = 3.0 + Math.sin(bestAngle * 0.3) * 0.5;
                camLoc = targetLoc.clone().add(bestX, bestHoverHeight, bestZ);
                angle = bestAngle;
            } else {
                // All angles completely blocked - use aerial view
                double hoverHeight = 15.0; // Much higher for aerial view
                camLoc = targetLoc.clone().add(0, hoverHeight, 0);
                // Don't update angle for aerial view, just stay above
            }
            
            // Make camera look at the target (specifically their eyes/head)
            Location lookAt = targetLoc.clone().add(0, 1.6, 0);
            Vector direction = lookAt.toVector().subtract(camLoc.toVector());
            camLoc.setDirection(direction);

            spectator.teleport(camLoc);
        }

        private int countBlocksInLineOfSight(Location from, Location to) {
            Vector direction = to.toVector().subtract(from.toVector());
            double distance = direction.length();
            direction.normalize();
            
            int blockCount = 0;
            // Check blocks along the line from camera to target
            for (double d = 0.5; d < distance; d += 0.5) {
                Location checkLoc = from.clone().add(direction.clone().multiply(d));
                Block block = checkLoc.getBlock();
                
                // Count solid blocks that are not air or transparent
                if (block.getType().isSolid() && !block.isPassable()) {
                    blockCount++;
                }
            }
            return blockCount;
        }
    }
}
