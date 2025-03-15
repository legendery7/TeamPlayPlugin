package org.legendery.teamPlayPlugin.commands;

import org.bukkit.*;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.legendery.teamPlayPlugin.DatabaseManager;
import org.legendery.teamPlayPlugin.TeamPlayPlugin;
import org.legendery.teamPlayPlugin.team.TeamManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class TeamInfoCommand implements CommandExecutor {

    private final Map<Player, BukkitTask> teleportCooldowns = new HashMap<>();
    private final TeamPlayPlugin plugin;
    private final TeamManager teamManager;
    private final DatabaseManager databaseManager;
    private final String freeTpPermission = "teamplayplugin.freetp"; //  Добавлено:  Permission для бесплатного телепорта

    public TeamInfoCommand(TeamPlayPlugin plugin, TeamManager teamManager, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.teamManager = teamManager;
        this.databaseManager = databaseManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Эта команда доступна только игрокам");
            return true;
        }
        Player player = (Player) sender;
        String teamName = teamManager.getPlayerTeam(player);
        if (teamName == null) {
            player.sendMessage(ChatColor.RED + "Вы не состоите в команде");
            return true;
        }
        if (args.length >= 1) {
            switch (args[0].toLowerCase()) {
                case "set":
                    handleSetCommand(player, teamName, args);
                    return true;
                case "del":
                    handleDelCommand(player, teamName, args);
                    return true;
                case "list":
                    handleListCommand(player, teamName);
                    return true;
                case "tp":
                    handleTpCommand(player, teamName);
                    return true;
            }
        }
        displayTeamInfo(player, teamName);
        return true;
    }

    private void displayTeamInfo(Player player, String teamName) {
        int teamLevel = databaseManager.getTeamLevel(teamName);
        int pointsCount = databaseManager.getTeamPointCount(teamName);
        Location magnetBlockLocation = getMagnetBlockLocationFromConfig(teamName);

        List<String> teamMembers = databaseManager.getTeamMembers(teamName);
        StringBuilder members = new StringBuilder();
        if (teamMembers != null && !teamMembers.isEmpty()) {
            for (String memberUUID : teamMembers) {

                UUID uuid = UUID.fromString(memberUUID);
                OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
                String memberName = (offlinePlayer.getName() != null) ? offlinePlayer.getName() : "Неизвестный игрок";
                members.append(memberName).append(", ");

            }
        } else {
            members.append("Нет участников");
        }
        player.sendMessage(ChatColor.GOLD + "--- Информация о команде " + teamName + " ---");
        player.sendMessage(ChatColor.WHITE + "Уровень команды: " + ChatColor.GOLD + teamLevel);
        if (magnetBlockLocation != null) {
            player.sendMessage(ChatColor.WHITE + "Координаты Замка: " + ChatColor.GOLD +
                    magnetBlockLocation.getBlockX() + ", " +
                    magnetBlockLocation.getBlockY() + ", " +
                    magnetBlockLocation.getBlockZ());
        } else {
            player.sendMessage(ChatColor.WHITE + "Место телепорта не установлено");
        }
        player.sendMessage(ChatColor.WHITE + "Участники команды: " + ChatColor.GOLD + members.toString());
        player.sendMessage(ChatColor.WHITE + "Сохраненные точки: " + ChatColor.GOLD + pointsCount + "/9");
    }

    private void handleTpCommand(Player player, String teamName) {
        if (teleportCooldowns.containsKey(player)) {
            player.sendMessage(ChatColor.RED + "Вы уже готовитесь к телепортации");
            return;
        }
        Location magnetLocation = getMagnetBlockLocationFromConfig(teamName);
        if (magnetLocation == null) {
            player.sendMessage(ChatColor.RED + "Место для телепорта не найдено");
            return;
        }
        float yaw = player.getLocation().getYaw();
        float pitch = player.getLocation().getPitch();
        final Location targetLocation = new Location(
                magnetLocation.getWorld(),
                magnetLocation.getBlockX() + 0.5,
                magnetLocation.getBlockY() + 1,
                magnetLocation.getBlockZ() + 0.5,
                yaw, pitch
        );

        boolean hasFreeTp = player.hasPermission(freeTpPermission);  //  Добавлено:  Проверка на разрешение

        int teamLevel = databaseManager.getTeamLevel(teamName);
        int teleportCost = getTeleportCost(teamLevel);

        if (!hasFreeTp) { // Добавлено:  Если нет права, то взимаем плату
            if (!removeEmeralds(player, teleportCost)) {
                player.sendMessage(ChatColor.RED + "Для телепортации требуется " + teleportCost + " " + getEmeraldDeclension(teleportCost));
                return;
            }
        }


        Location startLocation = player.getLocation();
        BossBar bossBar = Bukkit.createBossBar(ChatColor.GREEN + "Телепортация через 5 секунд...", BarColor.BLUE, BarStyle.SEGMENTED_20);
        bossBar.addPlayer(player);
        final int[] emeraldsRemoved = {teleportCost};
        BukkitTask task = new BukkitRunnable() {
            int secondsLeft = 5;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    if(!hasFreeTp)  giveEmeralds(player, emeraldsRemoved[0]);  //  Добавлено: Возврат изумрудов, если телепорт прерван
                    cancelTeleport(player, bossBar);
                    cancel();
                    return;
                }
                if (player.getLocation().distanceSquared(startLocation) > 0.1) {
                    player.sendMessage(ChatColor.RED + "Телепортация отменена! Вы двигались.");
                    if(!hasFreeTp) giveEmeralds(player, emeraldsRemoved[0]); //  Добавлено:  Возврат изумрудов, если телепорт отменен движением
                    cancelTeleport(player, bossBar);
                    cancel();
                    return;
                }
                if (secondsLeft <= 0) {
                    player.teleport(targetLocation);
                    player.sendMessage(ChatColor.GOLD + "Вы телепортированы!");
                    player.getWorld().playSound(targetLocation, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
                    spawnTeleportParticles(targetLocation);
                    bossBar.removeAll();
                    teleportCooldowns.remove(player);
                    cancel();
                    return;
                }
                bossBar.setTitle(ChatColor.GREEN + "Телепортация через " + secondsLeft + " секунд...");
                bossBar.setProgress(secondsLeft / 5.0);
                secondsLeft--;
            }
        }.runTaskTimer(plugin, 0, 20);
        teleportCooldowns.put(player, task);
    }

    private Location getMagnetBlockLocationFromConfig(String teamName) {
        String path = "magnet_blocks"; // Путь к вашей секции в config.yml
        FileConfiguration config = plugin.getConfig();

        if (config.getConfigurationSection(path) == null) {
            plugin.getLogger().warning("Section 'magnet_blocks' not found in config.yml");
            return null;
        }

        String locationString = null;
        for (String key : config.getConfigurationSection(path).getKeys(false)) {
            if (config.getString(path + "." + key).equals(teamName)) {
                locationString = key;
                break;
            }
        }

        if (locationString == null) {
            //plugin.getLogger().warning("No location found for team " + teamName + " in config.yml");
            return null;
        }

        String[] parts = locationString.split(",");
        if (parts.length != 4) {
            plugin.getLogger().warning("Invalid location format: " + locationString);
            return null;
        }

        try {
            World world = Bukkit.getWorld(parts[0]);
            double x = Double.parseDouble(parts[1]);
            double y = Double.parseDouble(parts[2]);
            double z = Double.parseDouble(parts[3]);

            if (world == null) {
                plugin.getLogger().warning("World not found: " + parts[0]);
                return null;
            }

            return new Location(world, x, y, z);
        } catch (NumberFormatException e) {
            plugin.getLogger().warning("Invalid number format in location: " + locationString);
            return null;
        }
    }

    private void spawnTeleportParticles(Location location) {
        World world = location.getWorld();
        if (world != null) {
            world.spawnParticle(Particle.PORTAL, location, 50, 0.5, 1, 0.5, 0.1);
            world.spawnParticle(Particle.ENCHANTMENT_TABLE, location, 30, 0.3, 0.5, 0.3, 0.05);
        }
    }

    private int getTeleportCost(int level) {
        if (level == 16) return 8;
        if (level <= 3) return 1;
        if (level <= 4) return 2;
        if (level <= 7) return 4;
        if (level <= 11) return 8;
        if (level <= 15) return 12;
        return 1;
    }

    private boolean removeEmeralds(Player player, int amount) {
        Inventory inventory = player.getInventory();
        int emeraldsFound = 0;
        for (ItemStack item : inventory.getContents()) {
            if (item != null && item.getType() == Material.EMERALD) {
                emeraldsFound += item.getAmount();
                if (emeraldsFound >= amount) {
                    break;
                }
            }
        }
        if (emeraldsFound < amount) return false;
        int remaining = amount;
        for (ItemStack item : inventory.getContents()) {
            if (item != null && item.getType() == Material.EMERALD) {
                int toRemove = Math.min(item.getAmount(), remaining);
                item.setAmount(item.getAmount() - toRemove);
                remaining -= toRemove;
                if (remaining == 0) break;
            }
        }
        player.updateInventory();
        return true;
    }

    private void giveEmeralds(Player player, int amount) {
        ItemStack emeralds = new ItemStack(Material.EMERALD, amount);
        Inventory inventory = player.getInventory();
        inventory.addItem(emeralds);
        player.updateInventory();
    }

    private void cancelTeleport(Player player, BossBar bossBar) {
        if (teleportCooldowns.containsKey(player)) {
            teleportCooldowns.get(player).cancel();
            teleportCooldowns.remove(player);
        }
        bossBar.removeAll();
    }

    private String getEmeraldDeclension(int count) {
        if (count % 100 >= 11 && count % 100 <= 14) {
            return "изумрудов";
        }
        switch (count % 10) {
            case 1:
                return "изумруд";
            case 2:
            case 3:
            case 4:
                return "изумруда";
            default:
                return "изумрудов";
        }
    }

    private void handleListCommand(Player player, String teamName) {
        Map<String, Location> points = databaseManager.getTeamPoints(teamName);
        if (points.isEmpty()) {
            player.sendMessage(ChatColor.GOLD + "У команды нет сохраненных точек");
        } else {
            player.sendMessage(ChatColor.GOLD + "--- Сохраненные точки ---");
            for (Map.Entry<String, Location> entry : points.entrySet()) {
                Location loc = entry.getValue();
                player.sendMessage(ChatColor.WHITE + entry.getKey() + ": " + ChatColor.GOLD +
                        loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ());
            }
        }
    }

    private void handleDelCommand(Player player, String teamName, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Использование: /tm del <название>");
            return;
        }
        if (databaseManager.removeTeamPoint(teamName, args[1])) {
            player.sendMessage(ChatColor.GOLD + "Точка " + args[1] + " удалена");
        } else {
            player.sendMessage(ChatColor.RED + "Точка не найдена");
        }
    }

    private void handleSetCommand(Player player, String teamName, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Использование: /tm set <название>");
            return;
        }
        String pointName = args[1];
        if (databaseManager.addTeamPoint(teamName, pointName, player.getLocation())) {
            player.sendMessage(ChatColor.GOLD + "Точка " + pointName + " сохранена.");
        } else {
            player.sendMessage(ChatColor.RED + "У вашей команды уже максимум точек");
        }
    }
}