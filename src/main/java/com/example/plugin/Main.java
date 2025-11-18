package com.example.plugin;

import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {

    private SpectatorManager spectatorManager;

    @Override
    public void onEnable() {
        // Load config
        saveDefaultConfig();

        // Initialize manager
        spectatorManager = new SpectatorManager(this);

        // Register commands
        getCommand("autospectate").setExecutor(new AutoSpectateCommand(this, spectatorManager));

        // Register listeners
        getServer().getPluginManager().registerEvents(new SpectatorListener(this, spectatorManager), this);

        getLogger().info("AutoSpectator has been enabled!");
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        getLogger().info("AutoSpectator has been disabled!");
    }
}
