package com.autospectator.plugin;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
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

    public enum PerspectiveMode {
        FOLLOWUP,
        CINEMATIC
    }

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

    public void setPerspective(Player player, PerspectiveMode mode) {
        if (sessions.containsKey(player.getUniqueId())) {
            sessions.get(player.getUniqueId()).setPerspective(mode);
            player.sendMessage("§aPerspective set to " + mode.name().toLowerCase() + ".");
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

    public void handleDeath(Player deadPlayer, Location deathLocation) {
        // If any spectator is in auto mode, force them to watch the death location
        for (SpectatorSession session : sessions.values()) {
            if (session.isAutoMode()) {
                session.spectateLocation(deathLocation, deadPlayer.getName(), 15);
            }
        }
    }

    private class SpectatorSession {
        private final Player spectator;
        private Player currentTarget;
        private Location currentLocationTarget;
        private String locationTargetName;
        private boolean autoMode = true;
        private int duration;
        private int timeRemaining;
        private int locationSpectationTimeRemaining = 0;
        
        private PerspectiveMode perspective = PerspectiveMode.FOLLOWUP;
        private Location cinematicLocation;
        private long lastCinematicSwitch;

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

        public void spectateLocation(Location location, String playerName, int durationSeconds) {
            // Set a location target that the spectator will watch for the specified duration
            currentLocationTarget = location.clone();
            locationTargetName = playerName;
            locationSpectationTimeRemaining = durationSeconds; // Store seconds directly
            currentTarget = null; // Clear player target
        }

        private void setTarget(Player target) {
            this.currentTarget = target;
            // Reset movement parameters for a smooth transition or new angle
            angle = random.nextDouble() * Math.PI * 2;
            spectator.setSpectatorTarget(null); // Unlock camera so we can move it
        }

        public void setPerspective(PerspectiveMode mode) {
            this.perspective = mode;
            this.cinematicLocation = null; // Reset cinematic location
        }

        public void tick() {
            if (!autoMode) return; // In single player mode, we don't cycle

            // Handle location spectation (death location watching)
            if (locationSpectationTimeRemaining > 0) {
                locationSpectationTimeRemaining--;
                if (locationSpectationTimeRemaining <= 0) {
                    // Time to stop watching the location and go back to auto mode
                    currentLocationTarget = null;
                    locationTargetName = null;
                    findNextTarget();
                }
                return; // Don't process player target logic while watching a location
            }

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
            if (currentTarget == null && currentLocationTarget == null) return;

            // Handle location spectation (stationary camera at death location)
            if (currentLocationTarget != null && locationSpectationTimeRemaining > 0) {
                updateDeathLocationSpectation();
                return;
            }

            // Handle player target spectation (normal mode)
            if (currentTarget == null || !currentTarget.isOnline()) return;

            if (perspective == PerspectiveMode.CINEMATIC) {
                updateCinematicMovement();
            } else {
                updateFollowupMovement();
            }
        }

        private void updateDeathLocationSpectation() {
            Location targetLoc = currentLocationTarget;
            
            // Cinematic movement around the death location
            angle += 0.008;
            
            // Check all angles and count blocked blocks
            int[] blockCounts = new int[8];
            boolean[] canPlaceCamera = new boolean[8];
            int minBlocks = Integer.MAX_VALUE;
            int bestAngleIndex = -1;
            boolean anyAngleClear = false;
            boolean anyValidAngle = false;
            
            for (int i = 0; i < 8; i++) {
                double testAngle = angle + (Math.PI / 4) * i;
                double testX = 5.0 * Math.cos(testAngle);
                double testZ = 5.0 * Math.sin(testAngle);
                double hoverHeight = 3.0 + Math.sin(testAngle * 0.3) * 0.5;
                
                Location testCamLoc = targetLoc.clone().add(testX, hoverHeight, testZ);
                
                // Check if camera position is not inside a block
                boolean cameraInBlock = isCameraInBlock(testCamLoc);
                canPlaceCamera[i] = !cameraInBlock;
                
                if (!cameraInBlock) {
                    anyValidAngle = true;
                    int blockCount = countBlocksInLineOfSight(testCamLoc, targetLoc);
                    blockCounts[i] = blockCount;
                    
                    // Track the angle with least blocks
                    if (blockCount < minBlocks) {
                        minBlocks = blockCount;
                        bestAngleIndex = i;
                        if (blockCount == 0) {
                            anyAngleClear = true;
                        }
                    }
                }
            }
            
            Location camLoc;
            
            if (anyAngleClear && canPlaceCamera[bestAngleIndex]) {
                // Use the angle with zero blocks and valid camera position
                double bestAngle = angle + (Math.PI / 4) * bestAngleIndex;
                double bestX = 5.0 * Math.cos(bestAngle);
                double bestZ = 5.0 * Math.sin(bestAngle);
                double bestHoverHeight = 3.0 + Math.sin(bestAngle * 0.3) * 0.5;
                camLoc = targetLoc.clone().add(bestX, bestHoverHeight, bestZ);
                
                // Make camera look at the death location
                Location lookAt = targetLoc.clone().add(0, 1.0, 0);
                Vector direction = lookAt.toVector().subtract(camLoc.toVector());
                camLoc.setDirection(direction);
                
                angle = bestAngle;
                spectator.teleport(camLoc);
                showDeathLocationActionBar();
            } else if (anyValidAngle && minBlocks < Integer.MAX_VALUE && minBlocks > 0) {
                // Use the best valid angle with least blocks
                double bestAngle = angle + (Math.PI / 4) * bestAngleIndex;
                double bestX = 5.0 * Math.cos(bestAngle);
                double bestZ = 5.0 * Math.sin(bestAngle);
                double bestHoverHeight = 3.0 + Math.sin(bestAngle * 0.3) * 0.5;
                camLoc = targetLoc.clone().add(bestX, bestHoverHeight, bestZ);
                
                // Make camera look at the death location
                Location lookAt = targetLoc.clone().add(0, 1.0, 0);
                Vector direction = lookAt.toVector().subtract(camLoc.toVector());
                camLoc.setDirection(direction);
                
                angle = bestAngle;
                spectator.teleport(camLoc);
                showDeathLocationActionBar();
            } else {
                // Stay at death location first-person view
                spectator.teleport(targetLoc.clone().add(0, 1.6, 0));
                showDeathLocationActionBar();
            }
        }

        private void updateCinematicMovement() {
            Location targetLoc = currentTarget.getLocation();
            long currentTime = System.currentTimeMillis();

            // Check if we need to switch position (every 8 seconds or if view is blocked)
            boolean needsSwitch = cinematicLocation == null || 
                                  (currentTime - lastCinematicSwitch > 8000) || // Switch every 8 seconds
                                  countBlocksInLineOfSight(cinematicLocation, targetLoc.clone().add(0, 1.6, 0)) > 0 ||
                                  cinematicLocation.distance(targetLoc) > 25; // Too far

            if (needsSwitch) {
                // Find a new spot
                List<Location> candidates = new ArrayList<>();
                for (int i = 0; i < 15; i++) {
                    // Random angle and distance
                    double angle = random.nextDouble() * Math.PI * 2;
                    double distance = 6 + random.nextDouble() * 14; // 6 to 20 blocks away
                    double height = -2 + random.nextDouble() * 8; // -2 to +6 height relative to player

                    Location candidate = targetLoc.clone().add(
                        distance * Math.cos(angle),
                        height,
                        distance * Math.sin(angle)
                    );

                    // Check if valid
                    if (!isCameraInBlock(candidate) && countBlocksInLineOfSight(candidate, targetLoc.clone().add(0, 1.6, 0)) == 0) {
                        candidates.add(candidate);
                    }
                }

                if (!candidates.isEmpty()) {
                    cinematicLocation = candidates.get(random.nextInt(candidates.size()));
                    lastCinematicSwitch = currentTime;
                } else if (cinematicLocation == null) {
                    // Fallback if no good spot found: just go up
                    cinematicLocation = targetLoc.clone().add(0, 5, 0);
                }
            }

            // Always look at the player
            if (cinematicLocation != null) {
                Location lookAt = targetLoc.clone().add(0, 1.6, 0); // Look at eyes
                Vector direction = lookAt.toVector().subtract(cinematicLocation.toVector());
                cinematicLocation.setDirection(direction);
                spectator.teleport(cinematicLocation);
                showPlayerNameActionBar();
            }
        }

        private void updateFollowupMovement() {
            Location targetLoc = currentTarget.getLocation();
            
            // Check if the target is in a very tight space (like a 2x1 tunnel)
            if (isInTightSpace(targetLoc)) {
                // Switch to first-person view by teleporting spectator to target location
                spectator.setSpectatorTarget(currentTarget);
                showPlayerNameActionBar();
                return;
            }
            
            // If not in tight space, revert spectator target if it was set
            spectator.setSpectatorTarget(null);

            // Cinematic movement logic with reduced rotation speed
            angle += 0.008;
            
            // Check all angles and count blocked blocks
            int[] blockCounts = new int[8];
            boolean[] canPlaceCamera = new boolean[8];
            int minBlocks = Integer.MAX_VALUE;
            int bestAngleIndex = -1;
            boolean anyAngleClear = false;
            boolean anyValidAngle = false;
            
            for (int i = 0; i < 8; i++) {
                double testAngle = angle + (Math.PI / 4) * i;
                double testX = 5.0 * Math.cos(testAngle);
                double testZ = 5.0 * Math.sin(testAngle);
                double hoverHeight = 3.0 + Math.sin(testAngle * 0.3) * 0.5;
                
                Location testCamLoc = targetLoc.clone().add(testX, hoverHeight, testZ);
                
                // Check if camera position is not inside a block
                boolean cameraInBlock = isCameraInBlock(testCamLoc);
                canPlaceCamera[i] = !cameraInBlock;
                
                if (!cameraInBlock) {
                    anyValidAngle = true;
                    int blockCount = countBlocksInLineOfSight(testCamLoc, targetLoc);
                    blockCounts[i] = blockCount;
                    
                    // Track the angle with least blocks
                    if (blockCount < minBlocks) {
                        minBlocks = blockCount;
                        bestAngleIndex = i;
                        if (blockCount == 0) {
                            anyAngleClear = true;
                        }
                    }
                }
            }
            
            Location camLoc;
            
            if (anyAngleClear && canPlaceCamera[bestAngleIndex]) {
                // Use the angle with zero blocks and valid camera position
                double bestAngle = angle + (Math.PI / 4) * bestAngleIndex;
                double bestX = 5.0 * Math.cos(bestAngle);
                double bestZ = 5.0 * Math.sin(bestAngle);
                double bestHoverHeight = 3.0 + Math.sin(bestAngle * 0.3) * 0.5;
                camLoc = targetLoc.clone().add(bestX, bestHoverHeight, bestZ);
                
                // Make camera look at the target (specifically their eyes/head)
                Location lookAt = targetLoc.clone().add(0, 1.6, 0);
                Vector direction = lookAt.toVector().subtract(camLoc.toVector());
                camLoc.setDirection(direction);
                
                angle = bestAngle;
                spectator.teleport(camLoc);
                showPlayerNameActionBar();
            } else if (anyValidAngle && minBlocks < Integer.MAX_VALUE && minBlocks > 0) {
                // Use the best valid angle with least blocks
                double bestAngle = angle + (Math.PI / 4) * bestAngleIndex;
                double bestX = 5.0 * Math.cos(bestAngle);
                double bestZ = 5.0 * Math.sin(bestAngle);
                double bestHoverHeight = 3.0 + Math.sin(bestAngle * 0.3) * 0.5;
                camLoc = targetLoc.clone().add(bestX, bestHoverHeight, bestZ);
                
                // Make camera look at the target (specifically their eyes/head)
                Location lookAt = targetLoc.clone().add(0, 1.6, 0);
                Vector direction = lookAt.toVector().subtract(camLoc.toVector());
                camLoc.setDirection(direction);
                
                angle = bestAngle;
                spectator.teleport(camLoc);
                showPlayerNameActionBar();
            } else {
                // No valid angle found at normal distance - switch to first person
                spectator.setSpectatorTarget(currentTarget);
                showPlayerNameActionBar();
            }
        }
        
        private boolean isCameraInBlock(Location loc) {
            // Check feet, mid-body, and head/eye level
            if (isBlockSolid(loc) || 
                isBlockSolid(loc.clone().add(0, 1.0, 0)) || 
                isBlockSolid(loc.clone().add(0, 1.7, 0))) {
                return true;
            }
            
            // Check width at feet level
            if (isBlockSolid(loc.clone().add(0.35, 0, 0)) ||
                isBlockSolid(loc.clone().add(-0.35, 0, 0)) ||
                isBlockSolid(loc.clone().add(0, 0, 0.35)) ||
                isBlockSolid(loc.clone().add(0, 0, -0.35))) {
                return true;
            }

            // Check width at head level (to prevent head clipping into walls)
            Location headLoc = loc.clone().add(0, 1.6, 0);
            if (isBlockSolid(headLoc.clone().add(0.35, 0, 0)) ||
                isBlockSolid(headLoc.clone().add(-0.35, 0, 0)) ||
                isBlockSolid(headLoc.clone().add(0, 0, 0.35)) ||
                isBlockSolid(headLoc.clone().add(0, 0, -0.35))) {
                return true;
            }
            
            return false;
        }
        
        private boolean isBlockSolid(Location loc) {
            Block block = loc.getBlock();
            return block.getType().isSolid() && !block.isPassable();
        }
        
        private void showPlayerNameActionBar() {
            // Show player name in ActionBar (subtitle bar)
            String playerName = currentTarget.getName();
            int health = (int) Math.ceil(currentTarget.getHealth());
            AttributeInstance maxHealthAttribute = currentTarget.getAttribute(Attribute.MAX_HEALTH);
            double maxHealthValue = maxHealthAttribute != null ? maxHealthAttribute.getValue() : currentTarget.getHealth();
            int maxHealth = (int) Math.ceil(maxHealthValue);
            String actionBarMessage = "§eSpectating: §a" + playerName + " §c❤ " + health + "/" + maxHealth;
            
            // Using spigot API to send action bar
            net.md_5.bungee.api.chat.TextComponent component = new net.md_5.bungee.api.chat.TextComponent(actionBarMessage);
            spectator.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR, component);
        }

        private void showDeathLocationActionBar() {
            // Show death location spectation info in ActionBar
            String actionBarMessage = "§cSpectating death of §e" + locationTargetName + " §c(" + locationSpectationTimeRemaining + "s)";
            
            // Using spigot API to send action bar
            net.md_5.bungee.api.chat.TextComponent component = new net.md_5.bungee.api.chat.TextComponent(actionBarMessage);
            spectator.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR, component);
        }
        
        private boolean isInTightSpace(Location loc) {
            // Check if the target is in a very tight space (like a 2x1 tunnel)
            // We check a 3x3x3 area around the player for open space
            
            Block centerBlock = loc.getBlock();
            
            // Count air/transparent blocks in a 3x3x3 area around the target
            int emptyBlocks = 0;
            int totalBlocks = 0;
            
            for (int x = -1; x <= 1; x++) {
                for (int y = 0; y <= 2; y++) {
                    for (int z = -1; z <= 1; z++) {
                        Block block = centerBlock.getRelative(x, y, z);
                        totalBlocks++;
                        
                        // Count air/transparent/passable blocks
                        if (!block.getType().isSolid() || block.isPassable()) {
                            emptyBlocks++;
                        }
                    }
                }
            }
            
            // If less than 50% of the space is empty, it's a tight space
            // A 2x1 tunnel would have about 2/3 empty (the player's column and above)
            // A normal open area would have much more empty space
            return emptyBlocks < (totalBlocks * 0.5);
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
