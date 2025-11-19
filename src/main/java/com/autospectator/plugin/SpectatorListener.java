package com.autospectator.plugin;

import org.bukkit.Location;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;

public class SpectatorListener implements Listener {

    private final Main plugin;
    private final SpectatorManager spectatorManager;

    public SpectatorListener(Main plugin, SpectatorManager spectatorManager) {
        this.plugin = plugin;
        this.spectatorManager = spectatorManager;
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();

        if (plugin.getConfig().getBoolean("triggers.damage", true)) {
            double damage = event.getFinalDamage();
            double threshold = plugin.getConfig().getDouble("triggers.damage-threshold", 5.0);
            
            if (damage >= threshold) {
                spectatorManager.handleTrigger(player, "High Damage");
            } else if (player.getHealth() - damage <= 0) {
                 spectatorManager.handleTrigger(player, "Death");
            } else {
                // Just general damage trigger? Maybe too spammy if we trigger on every hit.
                // The user said "A user loses health" AND "A user loses X amount of health".
                // I'll assume any damage is a trigger if configured, but maybe prioritize high damage.
                spectatorManager.handleTrigger(player, "Damage");
            }
        }
    }

    @EventHandler
    public void onAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;
        Player player = (Player) event.getDamager();

        if (plugin.getConfig().getBoolean("triggers.hostile-mob-hit", true)) {
            if (event.getEntity() instanceof Monster) {
                spectatorManager.handleTrigger(player, "Fighting Hostile Mob");
            }
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (!plugin.getConfig().getBoolean("triggers.fall-damage-prediction", true)) return;
        
        Player player = event.getPlayer();
        float fallDistance = player.getFallDistance();
        double threshold = plugin.getConfig().getDouble("triggers.fall-distance-threshold", 5.0);

        if (fallDistance > threshold) {
            // Check if they are about to land? 
            // Actually, if they are falling from high, we want to watch the fall.
            // So just high fall distance is enough reason to watch.
            spectatorManager.handleTrigger(player, "Falling");
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player deadPlayer = event.getEntity();
        Location deathLocation = deadPlayer.getLocation();
        
        // Notify the spectator manager to spectate the death location
        spectatorManager.handleDeath(deadPlayer, deathLocation);
    }
}
