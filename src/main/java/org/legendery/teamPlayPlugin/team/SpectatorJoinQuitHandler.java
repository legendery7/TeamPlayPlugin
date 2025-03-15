package org.legendery.teamPlayPlugin.team;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

import java.util.UUID;

public class SpectatorJoinQuitHandler implements Listener {

    private final Plugin plugin;
    private final String spectatorWorldName = "spectator_world"; // Название мира для Spectator'ов
    private World spectatorWorld;

    public SpectatorJoinQuitHandler(Plugin plugin) {
        this.plugin = plugin;
     //   Bukkit.getPluginManager().registerEvents(this, plugin);
        createOrLoadSpectatorWorld();
    }

    private void createOrLoadSpectatorWorld() {
        WorldCreator worldCreator = new WorldCreator(spectatorWorldName);
        worldCreator.environment(World.Environment.NORMAL); // Или другое подходящее окружение

        spectatorWorld = Bukkit.getWorld(spectatorWorldName);
        if (spectatorWorld == null) {
            spectatorWorld = worldCreator.createWorld();
            plugin.getLogger().info("Создан новый мир для Spectator'ов: " + spectatorWorldName);
        } else {
            plugin.getLogger().info("Мир для Spectator'ов загружен: " + spectatorWorldName);
        }

        if (spectatorWorld != null) {
            spectatorWorld.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false); // Отключаем смену дня и ночи
            spectatorWorld.setTime(6000); // Устанавливаем время на день
            spectatorWorld.setDifficulty(Difficulty.PEACEFUL); // Устанавливаем мирную сложность
        } else {
            plugin.getLogger().warning("Не удалось создать или загрузить мир для Spectator'ов!");
        }
    }


    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.SPECTATOR) {
            event.setJoinMessage(null); // Отключаем сообщение о входе
            teleportToSpectatorWorld(player);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
         if (player.getGameMode() == GameMode.SPECTATOR) {
             event.setQuitMessage(null); // Отключаем сообщение о выходе
         }
    }

    public void teleportToSpectatorWorld(Player player) {
        if (spectatorWorld != null) {
            Location spawnLocation = spectatorWorld.getSpawnLocation();

            // Проверка на безопасное место телепортации (не в блоке)
            if (!isSafeLocation(spawnLocation)) {
                // Если спавн небезопасен, поднимаем на несколько блоков вверх
                spawnLocation = findSafeY(spawnLocation);
            }

            player.teleport(spawnLocation);
        } else {
            plugin.getLogger().warning("Мир для Spectator'ов не доступен. Телепортация отменена.");
            player.sendMessage(ChatColor.RED + "Мир для Spectator'ов не доступен.");
        }
    }

    private boolean isSafeLocation(Location location) {
        // Проверяем, что в ногах и над головой у игрока воздух
        Material feet = location.getBlock().getType();
        Material head = location.clone().add(0, 1, 0).getBlock().getType();
        return feet.isAir() && head.isAir();
    }

    private Location findSafeY(Location location) {
        // Ищем безопасную высоту, поднимаясь вверх от начальной позиции
        Location safeLocation = location.clone();
        for (int i = 0; i < 256; i++) { // Лимит высоты для поиска
            safeLocation.setY(location.getY() + i);
            if (isSafeLocation(safeLocation)) {
                return safeLocation;
            }
        }

        // Если безопасное место не найдено, возвращаем исходную локацию
        plugin.getLogger().warning("Не удалось найти безопасное место для телепортации в мире Spectator'ов!");
        return location;
    }
}