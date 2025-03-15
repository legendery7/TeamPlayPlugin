package org.legendery.teamPlayPlugin.upgrades;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.legendery.teamPlayPlugin.TeamPlayPlugin;
import org.legendery.teamPlayPlugin.team.TeamManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TeamProtectionManager implements Listener {

    private final TeamPlayPlugin plugin;
    private final TeamManager teamManager;
    private final Map<Location, String> magnetBlockTeams = new HashMap<>();
    private final int protectionRadius;
    private final List<Material> protectedBlocks = new ArrayList<>();
    private final Material magnetMaterial;

    public TeamProtectionManager(TeamPlayPlugin plugin, TeamManager teamManager) {
        this.plugin = plugin;
        this.teamManager = teamManager;
        this.magnetMaterial = Material.getMaterial(plugin.getConfig().getString("magnet_block.material", "LODESTONE")); // Получаем материал командного блока из конфига
        if(this.magnetMaterial == null) {
            plugin.getLogger().severe("Некорректный материал командного блока в конфиге. Убедитесь, что значение корректно.");
        }

        this.protectionRadius = plugin.getConfig().getInt("protection_radius", 32);

        List<String> protectedBlockNames = plugin.getConfig().getStringList("protected_blocks");
        for (String blockName : protectedBlockNames) {
            Material material = Material.getMaterial(blockName.toUpperCase());
            if (material != null) {
                protectedBlocks.add(material);
            } else {
                plugin.getLogger().warning("Invalid protected block material: " + blockName);
            }
        }

        loadMagnetBlockLocations();
        Bukkit.getPluginManager().registerEvents(this, plugin); // Регистрируем слушателя
    }

    //  Вспомогательный метод для определения, находится ли блок в зоне защиты
    private boolean isInProtectionZone(Location location) {
        for (Location magnetBlockLocation : magnetBlockTeams.keySet()) {
            if (location.getWorld() == magnetBlockLocation.getWorld() &&
                    location.distance(magnetBlockLocation) <= protectionRadius) {
                return true;
            }
        }
        return false;
    }

    private String getTeamForLocation(Location location) {
        for (Map.Entry<Location, String> entry : magnetBlockTeams.entrySet()) {
            if (location.getWorld() == entry.getKey().getWorld() &&
                    location.distance(entry.getKey()) <= protectionRadius) {
                return entry.getValue();
            }
        }
        return null;
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        Location location = block.getLocation();

        if (magnetBlockTeams.containsKey(location)) {
            if (block.getType() == magnetMaterial) {
                event.setCancelled(true);
                return;
            }
        }


        String teamName = getTeamForLocation(location);
        if (teamName != null) {
            String playerTeam = teamManager.getPlayerTeam(player);
            if (!teamName.equals(playerTeam)) {
                if (protectedBlocks.contains(block.getType())) {
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlockPlaced();
        Location location = block.getLocation();

        if (magnetBlockTeams.containsKey(location)) {
            if (block.getType() == magnetMaterial) {
                event.setCancelled(true);
                return;
            }
        }

        String teamName = getTeamForLocation(location);
        if (teamName != null) {
            String playerTeam = teamManager.getPlayerTeam(player);
            if (!teamName.equals(playerTeam) && protectedBlocks.contains(block.getType())) {
                event.setCancelled(true);
            }
        }
    }


    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        List<Block> blocks = new ArrayList<>(event.blockList());
        for (Block block : blocks) {
            // Проверяем, является ли блок частью зоны защиты
            String teamName = getTeamForLocation(block.getLocation());
            if (teamName != null) {
                event.blockList().remove(block); // Удаляем блок из списка взрыва
            }
        }
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        Player player = (Player) event.getPlayer();

        if (event.getInventory().getLocation() == null) return; // Добавляем проверку на null

        Block block = event.getInventory().getLocation().getBlock();

        if (block == null) return;

        String teamName = getTeamForLocation(block.getLocation());
        if (teamName != null) {
            String playerTeam = teamManager.getPlayerTeam(player);
            if (!teamName.equals(playerTeam) && protectedBlocks.contains(block.getType())) {
                event.setCancelled(true);
            }
        }
    }

    public void loadMagnetBlockLocations() {
        FileConfiguration config = plugin.getConfig();
        if (!config.isConfigurationSection("magnet_blocks")) return;

        ConfigurationSection section = config.getConfigurationSection("magnet_blocks");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                try {
                    String[] parts = key.split(",");
                    if (parts.length == 4) {
                        World world = Bukkit.getWorld(parts[0]);
                        int x = Integer.parseInt(parts[1]);
                        int y = Integer.parseInt(parts[2]);
                        int z = Integer.parseInt(parts[3]);
                        Location location = new Location(world, x, y, z);
                        String teamName = section.getString(key);
                        magnetBlockTeams.put(location, teamName);
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Некорректный формат координат в magnet_blocks: " + key);
                }
            }
        }
    }

    private void saveMagnetBlockLocations() {
        FileConfiguration config = plugin.getConfig();
        config.createSection("magnet_blocks");
        ConfigurationSection section = config.getConfigurationSection("magnet_blocks");

        for (Map.Entry<Location, String> entry : magnetBlockTeams.entrySet()) {
            Location location = entry.getKey();
            String teamName = entry.getValue();
            String key = String.format("%s,%.0f,%.0f,%.0f", location.getWorld().getName(), location.getX(), location.getY(), location.getZ());
            section.set(key, teamName);
        }
        plugin.saveConfig();
    }
}