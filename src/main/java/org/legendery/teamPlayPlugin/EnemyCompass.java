package org.legendery.teamPlayPlugin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CompassMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.legendery.teamPlayPlugin.commands.CompassCommand;
import org.legendery.teamPlayPlugin.team.TeamManager;

public class EnemyCompass extends BukkitRunnable {

    private final TeamPlayPlugin plugin;
    private final TeamManager teamManager;
    private final CompassCommand compassCommand; // Добавляем ссылку на CompassCommand

    public EnemyCompass(TeamPlayPlugin plugin, TeamManager teamManager, CompassCommand compassCommand) { // Изменяем конструктор
        this.plugin = plugin;
        this.teamManager = teamManager;
        this.compassCommand = compassCommand;
    }

    @Override
    public void run() {
        // Проверяем, включены ли компасы
        if (!compassCommand.isCompassEnabled()) {
            // Если компасы выключены, очищаем ActionBar у всех игроков и выходим
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.sendActionBar("");
            }
            return;
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            String playerTeam = teamManager.getPlayerTeam(player);

            if (playerTeam == null) {
                // Игрок не в команде, компас не работает
                continue;
            }

            // Получаем предмет в основной руке игрока
            ItemStack mainHandItem = player.getInventory().getItemInMainHand();
            ItemStack offHandItem = player.getInventory().getItemInOffHand();
            Player nearestEnemy = findNearestEnemy(player, playerTeam);

            // Проверяем, есть ли компас в основной руке
            if (mainHandItem != null && mainHandItem.getType() == Material.COMPASS && mainHandItem.getItemMeta() instanceof CompassMeta) {
                updateCompass(player, mainHandItem, nearestEnemy);
            }
            // Проверяем, есть ли компас в левой руке
            else if (offHandItem != null && offHandItem.getType() == Material.COMPASS && offHandItem.getItemMeta() instanceof CompassMeta) {
                updateCompass(player, offHandItem, nearestEnemy);
            } else {
                // Если в руке не компас, очищаем ActionBar
                player.sendActionBar("");
            }
        }
    }

    private void updateCompass(Player player, ItemStack compass, Player nearestEnemy) {
        if (nearestEnemy != null) {
            // Устанавливаем точку компаса на врага
            CompassMeta compassMeta = (CompassMeta) compass.getItemMeta();
            compassMeta.setLodestone(nearestEnemy.getLocation());
            compassMeta.setLodestoneTracked(false);
            compass.setItemMeta(compassMeta);

            Location playerLocation = player.getLocation();
            Location enemyLocation = nearestEnemy.getLocation();
            double distance = playerLocation.distance(enemyLocation);
            int distanceInt = (int) distance; // Преобразуем в целое число

            // Формируем сообщение для ActionBar с правильным склонением
            String actionBarMessage = ChatColor.RED + "Враг: " + nearestEnemy.getName() +
                    " " + distanceInt + " " + pluralize(distanceInt, "блок", "блока", "блоков") + "";

            // Отправляем сообщение в ActionBar
            player.sendActionBar(actionBarMessage); // Используем sendActionBar напрямую
        } else {
            // Врагов нет
            player.sendActionBar(ChatColor.GREEN + "Врагов поблизости нет!");
        }
    }

    private Player findNearestEnemy(Player player, String playerTeam) {
        Player nearestEnemy = null;
        double minDistance = Double.MAX_VALUE;

        for (Player otherPlayer : Bukkit.getOnlinePlayers()) {
            if (otherPlayer.equals(player)) {
                continue; // Не проверяем самого себя
            }

            String otherPlayerTeam = teamManager.getPlayerTeam(otherPlayer);

            if (otherPlayerTeam == null || playerTeam.equals(otherPlayerTeam)) {
                continue; // Игрок без команды или из той же команды
            }

            //Проверяем, является ли игрок в спектрах
            if (otherPlayer.getGameMode() == org.bukkit.GameMode.SPECTATOR){
                continue;
            }

            double distance = player.getLocation().distance(otherPlayer.getLocation());

            if (distance < minDistance) {
                minDistance = distance;
                nearestEnemy = otherPlayer;
            }
        }

        return nearestEnemy;
    }

    // Метод для склонения слова "блок"
    private String pluralize(int count, String one, String few, String many) {
        count = Math.abs(count);
        int mod10 = count % 10;
        int mod100 = count % 100;

        if (mod10 == 1 && mod100 != 11) {
            return one;
        } else if (mod10 >= 2 && mod10 <= 4 && (mod100 < 10 || mod100 >= 20)) {
            return few;
        } else {
            return many;
        }
    }
}