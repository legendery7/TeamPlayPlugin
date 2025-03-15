package org.legendery.teamPlayPlugin.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.legendery.teamPlayPlugin.TeamPlayPlugin;

public class CompassCommand implements CommandExecutor {

    private final TeamPlayPlugin plugin;
    private boolean compassEnabled; // Убираем инициализацию по умолчанию

    public CompassCommand(TeamPlayPlugin plugin) {
        this.plugin = plugin;
        this.compassEnabled = plugin.getConfig().getBoolean("compassEnabled", true); // Загружаем из конфига, по умолчанию true
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("compass")) {
            if (!sender.hasPermission("team.compass")) {
                sender.sendMessage(ChatColor.RED + "У вас нет прав для этой команды.");
                return true;
            }

            compassEnabled = !compassEnabled; // Переключаем состояние
            plugin.getConfig().set("compassEnabled", compassEnabled);  // Сохраняем в конфиг
            plugin.saveConfig();

            String message;
            if (compassEnabled) {
                message = ChatColor.GOLD + "На Cервере " +
                        ChatColor.GREEN + "включены " +
                        ChatColor.GOLD + "компасы";
            } else {
                message = ChatColor.GOLD + "На Cервере " +
                        ChatColor.RED + "выключены " +
                        ChatColor.GOLD + "компасы";
            }

            // Отправляем сообщение всем игрокам
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.sendMessage(message);
            }
            return true;
        }
        return false;
    }

    public boolean isCompassEnabled() {
        return compassEnabled;
    }
}