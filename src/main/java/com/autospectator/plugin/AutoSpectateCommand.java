package com.autospectator.plugin;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class AutoSpectateCommand implements CommandExecutor {

    private final SpectatorManager spectatorManager;

    public AutoSpectateCommand(Main plugin, SpectatorManager spectatorManager) {
        this.spectatorManager = spectatorManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            spectatorManager.toggleSpectator(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "time":
                if (args.length < 2) {
                    player.sendMessage("§cUsage: /autospectate time <seconds>");
                    return true;
                }
                try {
                    int seconds = Integer.parseInt(args[1]);
                    spectatorManager.setSpectateTime(player, seconds);
                } catch (NumberFormatException e) {
                    player.sendMessage("§cInvalid number.");
                }
                break;
            case "auto":
                spectatorManager.setAutoMode(player);
                break;
            case "perspective":
                if (args.length < 2) {
                    player.sendMessage("§cUsage: /autospectate perspective <cinematic|followup>");
                    return true;
                }
                String modeStr = args[1].toLowerCase();
                if (modeStr.equals("cinematic")) {
                    spectatorManager.setPerspective(player, SpectatorManager.PerspectiveMode.CINEMATIC);
                } else if (modeStr.equals("followup")) {
                    spectatorManager.setPerspective(player, SpectatorManager.PerspectiveMode.FOLLOWUP);
                } else {
                    player.sendMessage("§cInvalid perspective. Use 'cinematic' or 'followup'.");
                }
                break;
            default:
                // Assume it's a player name
                spectatorManager.setTarget(player, args[0]);
                break;
        }

        return true;
    }
}
