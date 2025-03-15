package org.legendery.teamPlayPlugin.manager;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.legendery.teamPlayPlugin.team.TeamManager;
import org.bukkit.plugin.Plugin;
import org.legendery.teamPlayPlugin.upgrades.TeamMagnetBlockManager;

public class MagnetBlockDistributor {

    private final Plugin plugin;
    private final TeamManager teamManager;
    private final TeamMagnetBlockManager magnetBlockManager;

    public MagnetBlockDistributor(Plugin plugin, TeamManager teamManager, TeamMagnetBlockManager magnetBlockManager) {
        this.plugin = plugin;
        this.teamManager = teamManager;
        this.magnetBlockManager = magnetBlockManager;
    }

    public void giveMagnetBlockToUniPlayers(CommandSender commandSender) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            int count = 0; //Счетчик выданных блоков
            for (Player player : Bukkit.getOnlinePlayers()) {
                String playerTag = teamManager.getPlayerTag(player); // Получаем тег игрока из TeamManager
                if (playerTag != null && playerTag.equalsIgnoreCase("cap")) { // Сравниваем с "cap"
                    ItemStack magnetBlock = magnetBlockManager.createMagnetBlock();
                    giveItemToPlayer(player, magnetBlock); // Используем отдельный метод

                    // Выдаем 64 изумруда
                    ItemStack emeralds = new ItemStack(Material.EMERALD, 64);
                    giveItemToPlayer(player, emeralds);

                    // Выдаем 64 хлеба
                    ItemStack bread = new ItemStack(Material.BREAD, 64);
                    giveItemToPlayer(player, bread);

                    player.sendMessage(ChatColor.WHITE + "Вы получили " + magnetBlockManager.getMagnetBlockName() +ChatColor.WHITE + ", 64 изумруда и 64 хлеба.");
                    count++; // Увеличиваем счетчик
                }
            }
            commandSender.sendMessage(ChatColor.WHITE + "Выдано " + magnetBlockManager.getMagnetBlockName() +", 64 изумруда и 64 хлеба "+ ChatColor.WHITE +" " + count + " игрокам капитанам");
        });
    }

    private void giveItemToPlayer(Player player, ItemStack item) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            player.getInventory().addItem(item);
        });
    }
}