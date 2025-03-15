package org.legendery.teamPlayPlugin.manager;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class DeathToggleManager implements CommandExecutor {

    private final JavaPlugin plugin;
    private boolean deathEnabled = true; // Изначально смерть включена

    public DeathToggleManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.deathEnabled = plugin.getConfig().getBoolean("deathEnabled", true); // Загрузка из конфига
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("death")) {
            if (!sender.hasPermission("team.death")) {
                sender.sendMessage(ChatColor.RED + "У вас нет прав для этой команды.");
                return true;
            }

            deathEnabled = !deathEnabled; // Переключаем состояние
            plugin.getConfig().set("deathEnabled", deathEnabled);  // Сохраняем в конфиг
            plugin.saveConfig();

            String message;
            if (deathEnabled) {
                message = ChatColor.GOLD + "На Cервере " +
                        ChatColor.GREEN + "включена " +
                        ChatColor.GOLD + "смерть игроков";
            } else {
                message = ChatColor.GOLD + "На Cервере " +
                        ChatColor.RED + "выключена " +
                        ChatColor.GOLD + "смерть игроков";
            }

            // Отправляем сообщение всем игрокам
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.sendMessage(message);
            }
            return true;
        }
        return false;
    }

    public boolean isDeathEnabled() {
        return deathEnabled;
    }

    //Добавляем метод setDeathEnabled
    public void setDeathEnabled(boolean enabled) {
        this.deathEnabled = enabled;
        plugin.getConfig().set("deathEnabled", enabled);
        plugin.saveConfig();
    }
}