package org.legendery.teamPlayPlugin.manager;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class PvPManager implements CommandExecutor, Listener {

    private final JavaPlugin plugin;
    private boolean pvpEnabled = false; // По умолчанию PvP выключено

    public PvPManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.pvpEnabled = plugin.getConfig().getBoolean("pvpEnabled", false); // Загружаем из конфига
    }
    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Player && event.getDamager() instanceof Player) {
            Player damager = (Player) event.getDamager();

            // Проверяем, включено ли PvP и отключен ли DeathToggle
            if (!isPvpEnabled()) {
                event.setCancelled(true);
            }
        }
    }
    public boolean isPvpEnabled() {
        return pvpEnabled;
    }

    public void setPvpEnabled(boolean enabled) {
        pvpEnabled = enabled;
        plugin.getConfig().set("pvpEnabled", enabled);  // Сохраняем в конфиг
        plugin.saveConfig();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("pvp")) {

            if (!sender.hasPermission("team.pvp.toggle")) {
                sender.sendMessage(ChatColor.RED + "У вас нет прав для этой команды.");
                return true;
            }

            setPvpEnabled(!pvpEnabled); // Переключаем состояние PvP и сохраняем
            String status = pvpEnabled ? ChatColor.GREEN + "включено" : ChatColor.RED + "выключено";

            Bukkit.broadcastMessage(ChatColor.GOLD + "На Сервере " + (pvpEnabled ? ChatColor.GREEN +"включено" : ChatColor.RED+"выключено") + ChatColor.GOLD+" PvP");

            return true;
        }
        return false;
    }
}