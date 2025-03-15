package org.legendery.teamPlayPlugin.upgrades;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CompassMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.legendery.teamPlayPlugin.TeamPlayPlugin;
import org.legendery.teamPlayPlugin.constructor.Upgrade;
import org.legendery.teamPlayPlugin.team.TeamManager;
import org.legendery.teamPlayPlugin.manager.util.MessageCooldownManager;
import org.legendery.teamPlayPlugin.util.ItemNameTranslator;

import java.util.*;

import static org.legendery.teamPlayPlugin.util.ItemNameTranslator.getMaterialForLevel;

public class TeamMagnetBlockManager implements Listener {

    private final TeamPlayPlugin plugin;
    private final TeamManager teamManager;
    private final UpgradeManager upgradeManager;
    private BonusUpgradesManager bonusUpgradesManager;
    private final NamespacedKey magnetKey;
    private final String magnetBlockName;
    private final Map<Location, String> magnetBlockTeams = new HashMap<>(); // Location -> Team Name
    private final Material magnetMaterial;
    private final Material grayStainedGlassPane = Material.GRAY_STAINED_GLASS_PANE;
    private final Map<String, ChatColor> teamColors = new HashMap<>();

    public void initializeTeamColors() {
        teamColors.put("red", ChatColor.RED);
        teamColors.put("purple", ChatColor.DARK_PURPLE);
        teamColors.put("gold", ChatColor.GOLD);
        teamColors.put("silver", ChatColor.GRAY);
        teamColors.put("green", ChatColor.DARK_GREEN);
        teamColors.put("aqua", ChatColor.DARK_AQUA);
    }
    public ChatColor getTeamColor(String teamName) {
        return teamColors.getOrDefault(teamName.toLowerCase(), ChatColor.WHITE); // Возвращаем белый, если цвет не найден
    }
    public TeamMagnetBlockManager(TeamPlayPlugin plugin, TeamManager teamManager, UpgradeManager upgradeManager, BonusUpgradesManager bonusUpgradesManager) {
        this.plugin = plugin;
        this.teamManager = teamManager;
        this.upgradeManager = upgradeManager;
        this.bonusUpgradesManager = bonusUpgradesManager; // Инициализируем BonusUpgradesManager
        initializeTeamColors();
        magnetKey = new NamespacedKey(plugin, "team_magnet");
        magnetBlockName = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("magnet_block.name", "&bTeam Magnet"));
        String materialString = plugin.getConfig().getString("magnet_block.material", "LODESTONE");
        Material tempMaterial = Material.getMaterial(materialString); // Используем временную переменную
        if (tempMaterial == null) {
            magnetMaterial = Material.LODESTONE;
        } else {
            magnetMaterial = tempMaterial;
        }

        Bukkit.getPluginManager().registerEvents(this, plugin);
        loadMagnetBlockLocations();

    }
    public void setBonusUpgradesManager(BonusUpgradesManager bonusUpgradesManager) {
        this.bonusUpgradesManager = bonusUpgradesManager;
    }
    public ItemStack createMagnetBlock() {
        ItemStack magnet = new ItemStack(magnetMaterial);
        ItemMeta meta = magnet.getItemMeta();
        meta.setDisplayName(magnetBlockName);
        meta.getPersistentDataContainer().set(magnetKey, org.bukkit.persistence.PersistentDataType.STRING, "team_magnet_block");
        magnet.setItemMeta(meta);
        return magnet;
    }

    private Location getMagnetBlockLocationFromConfig(String teamName) {
        String path = "magnet_blocks";
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

    public void displayUpgradeEffects(String teamName) {
        Location magnetBlockLocation = getMagnetBlockLocationFromConfig(teamName);

        if (magnetBlockLocation != null) {
            World world = magnetBlockLocation.getWorld();
            if (world != null) {
                double centerX = magnetBlockLocation.getBlockX() + 0.5;
                double centerY = magnetBlockLocation.getBlockY() + 0.5;
                double centerZ = magnetBlockLocation.getBlockZ() + 0.5;
                Location centerLocation = new Location(world, centerX, centerY, centerZ);
                world.spawnParticle(Particle.VILLAGER_HAPPY, centerLocation, 50, 0.4, 0.4, 0.4, 0.1);
                Location innerLocation = new Location(world, centerX, centerY - 0.3, centerZ);
                world.spawnParticle(Particle.COMPOSTER, innerLocation, 30, 0.3, 0.3, 0.3, 0.05);
            } else {
                plugin.getLogger().warning("World is null for magnet block location of team " + teamName);
            }
        } else {
            plugin.getLogger().warning("Magnet block location not found for team " + teamName);
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlockPlaced();

        if (block.getType() != magnetMaterial) return;

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            String teamName = teamManager.getPlayerTeam(player);

            // Логирование для отладки
            plugin.getLogger().info("Игрок " + player.getName() + ", команда: " + teamName);

            if (teamName == null) {
                event.setCancelled(true);
                return;
            }

            if (magnetBlockTeams.containsValue(teamName)) {
                event.setCancelled(true);
                return;
            }

            magnetBlockTeams.put(block.getLocation(), teamName);
            saveMagnetBlockLocations();
            applyUpgrade(teamName);
        }, 10L);
    }

    public Map<Location, String> getMagnetBlockTeams() {
        return magnetBlockTeams;
    }

    public void applyUpgrade(String teamName) {

        ChatColor teamColor = teamColors.getOrDefault(teamName, ChatColor.WHITE);
        String message = ChatColor.translateAlternateColorCodes('&',
                "Команда " + teamColor + teamName + "&f построила &4Замок &fи начала изучение &3Технологий&f!");
        Bukkit.broadcastMessage(message);
    }


    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Location loc = event.getBlock().getLocation();

        if (magnetBlockTeams.containsKey(loc)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.RED + "Вы не можете сломать командный блок!");
        }
    }

    private final MessageCooldownManager cooldownManager = new MessageCooldownManager(2);

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack itemInHand = event.getItem();
        Action action = event.getAction();
        Block clickedBlock = event.getClickedBlock();

        if (action == Action.RIGHT_CLICK_BLOCK && clickedBlock != null) {
            Location blockLocation = clickedBlock.getLocation();

            if (magnetBlockTeams.containsKey(blockLocation)) {
                String teamName = magnetBlockTeams.get(blockLocation);
                String playerTeam = teamManager.getPlayerTeam(player);
                if (itemInHand != null && itemInHand.getType() == Material.COMPASS) {
                    setLodestoneTarget(itemInHand, blockLocation, player);
                    if (playerTeam != null && playerTeam.equals(teamName)) {
                        event.setCancelled(true);
                        return;
                    } else {
                        if (cooldownManager.canSendMessage(player)) {
                            player.sendMessage(ChatColor.RED + "Вы не являетесь членом команды " + teamName + ", чтобы использовать этот блок.");
                        }
                        event.setCancelled(true);
                        return;
                    }
                }
                if (playerTeam != null && playerTeam.equals(teamName)) {
                    event.setCancelled(true);
                    openUpgradeMenu(player, teamName);
                    return;
                }
                if (cooldownManager.canSendMessage(player)) {
                    player.sendMessage(ChatColor.RED + "Вы не являетесь членом команды " + teamName + ", чтобы использовать этот блок.");
                    event.setCancelled(true);
                }
            }
        }

        if (action == Action.LEFT_CLICK_BLOCK) {
            if (clickedBlock != null && isMagnetBlock(clickedBlock)) {
                event.setCancelled(true);
            }
        }

        if (action == Action.RIGHT_CLICK_BLOCK && clickedBlock != null && clickedBlock.getType() == magnetMaterial) {
            if (itemInHand != null && !itemInHand.getType().isBlock()) {
                event.setCancelled(true);
            }
        }
    }

    private void setLodestoneTarget(ItemStack compass, Location targetLocation, Player player) {
        if (compass.getItemMeta() instanceof CompassMeta) {
            CompassMeta meta = (CompassMeta) compass.getItemMeta();
            meta.setLodestone(targetLocation);
            meta.setLodestoneTracked(false);
            compass.setItemMeta(meta);
        }
    }

    private boolean isMagnetBlock(Block block) {
        return block.getType() == magnetMaterial && magnetBlockTeams.containsKey(block.getLocation());
    }

    public void openUpgradeMenu(Player player, String teamName) {
        Inventory upgradeInventory = Bukkit.createInventory(player, 5 * 9, "Прокачка Замка");

        for (int i = 0; i < upgradeInventory.getSize(); i++) {
            if (i < 9 || i >= upgradeInventory.getSize() - 9 || i % 9 == 0 || i % 9 == 8) {
                ItemStack glass = new ItemStack(grayStainedGlassPane);
                ItemMeta glassMeta = glass.getItemMeta();
                glassMeta.setDisplayName(" ");
                glass.setItemMeta(glassMeta);
                upgradeInventory.setItem(i, glass);
            }
        }

        int currentLevel = upgradeManager.getTeamLevel(teamName);
        ItemStack levelDisplay = new ItemStack(Material.EMERALD);
        ItemMeta levelMeta = levelDisplay.getItemMeta();
        levelMeta.setDisplayName(ChatColor.GOLD + "Текущий уровень: " + currentLevel);
        levelDisplay.setItemMeta(levelMeta);
        upgradeInventory.setItem(20, levelDisplay);
        Upgrade nextLevelUpgrade = upgradeManager.getUpgrade(currentLevel + 1);
        if (nextLevelUpgrade != null) {
            ItemStack upgradeButton = createUpgradeButton(currentLevel + 1);
            upgradeInventory.setItem(22, upgradeButton);
        } else {
            ItemStack maxLevelDisplay = new ItemStack(Material.BARRIER);
            ItemMeta maxLevelMeta = maxLevelDisplay.getItemMeta();
            maxLevelMeta.setDisplayName(ChatColor.RED + "Максимальный уровень достигнут!");
            maxLevelDisplay.setItemMeta(maxLevelMeta);
            upgradeInventory.setItem(22, maxLevelDisplay);
        }

        // **Замена кнопки в 24 слоте**
        if (currentLevel >= 16) {
            ItemStack bonusUpgradeButton = new ItemStack(Material.NETHER_STAR); // Оранжевая иконка
            ItemMeta bonusMeta = bonusUpgradeButton.getItemMeta();
            bonusMeta.setDisplayName(ChatColor.GOLD + "Бонусная прокачка");
            bonusUpgradeButton.setItemMeta(bonusMeta);
            upgradeInventory.setItem(24, bonusUpgradeButton);
        } else {
            ItemStack infoButton = new ItemStack(Material.CREEPER_BANNER_PATTERN);
            ItemMeta infoMeta = infoButton.getItemMeta();
            infoMeta.setDisplayName(ChatColor.DARK_RED + "Информация об улучшении");

            Upgrade currentUpgrade = upgradeManager.getUpgrade(currentLevel + 1);
            List<String> lore = new ArrayList<>();
            if (currentUpgrade != null) {
                lore.add(ChatColor.GRAY + currentUpgrade.getUpgrade().getDescription());
            } else {
                lore.add(ChatColor.GRAY + "Достигнут максимальный уровень.");
            }

            infoMeta.setLore(lore);
            infoButton.setItemMeta(infoMeta);
            upgradeInventory.setItem(24, infoButton);
        }

        player.openInventory(upgradeInventory);
    }

    private ItemStack createUpgradeButton(int level) {
        Upgrade upgrade = upgradeManager.getUpgrade(level);
        if (upgrade == null) {
            return null;
        }

        ItemStack buttonItem = new ItemStack(getMaterialForLevel(level));
        ItemMeta buttonMeta = buttonItem.getItemMeta();
        buttonMeta.setDisplayName(ChatColor.DARK_GREEN + "Улучшить до " + level + " уровня");

        List<String> lore = new ArrayList<>();

        lore.add(ChatColor.DARK_GREEN + "Стоимость:");
        for (Map.Entry<Material, Integer> entry : upgrade.getCost().entrySet()) {
            String materialName = ItemNameTranslator.translateMaterialName(entry.getKey());
            lore.add(ChatColor.GRAY + "- " + entry.getValue() + " " + materialName);
        }
        buttonMeta.setLore(lore);
        buttonItem.setItemMeta(buttonMeta);
        return buttonItem;
    }

    private void saveMagnetBlockLocations() {
        FileConfiguration config = plugin.getConfig();
        ConfigurationSection section = config.createSection("magnet_blocks");
        for (Map.Entry<Location, String> entry : magnetBlockTeams.entrySet()) {
            Location loc = entry.getKey();
            String team = entry.getValue();
            String locationString = loc.getWorld().getName() + "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
            section.set(locationString, team);
        }
        plugin.saveConfig();
    }

    public void loadMagnetBlockLocations() {
        FileConfiguration config = plugin.getConfig();
        ConfigurationSection section = config.getConfigurationSection("magnet_blocks");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                String[] parts = key.split(",");
                if (parts.length == 4) {
                    try {
                        World world = Bukkit.getWorld(parts[0]);
                        int x = Integer.parseInt(parts[1]);
                        int y = Integer.parseInt(parts[2]);
                        int z = Integer.parseInt(parts[3]);
                        Location location = new Location(world, x, y, z);
                        String team = section.getString(key);
                        magnetBlockTeams.put(location, team);
                    } catch (NumberFormatException | NullPointerException e) {
                        plugin.getLogger().warning("Не удалось загрузить командный блок из конфига: " + key);
                    }
                }
            }
        }
    }

    public String getMagnetBlockName() {
        return magnetBlockName;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        if (title.equals("Прокачка Замка") || title.equals(ChatColor.RED + "Бонусная прокачка")) {
            event.setCancelled(true);
            Player player = (Player) event.getWhoClicked();
            ItemStack clickedItem = event.getCurrentItem();

            if (clickedItem == null || !clickedItem.hasItemMeta()) return;

            String displayName = clickedItem.getItemMeta().getDisplayName();
            String teamName = teamManager.getPlayerTeam(player);
            if (teamName == null) return;
            if (title.equals("Прокачка Замка")) {
                if (displayName.equals(ChatColor.GOLD + "Бонусная прокачка")) {
                    bonusUpgradesManager.openBonusUpgradeMenu(player, teamName); // Используем BonusUpgradesManager
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                    return;
                }

                // Обработка стандартного улучшения
                if (displayName.startsWith(ChatColor.DARK_GREEN + "Улучшить до")) {
                    int level = Integer.parseInt(displayName.split(" ")[2]);
                    Upgrade upgrade = upgradeManager.getUpgrade(level);

                    if (upgrade == null) {
                        player.sendMessage(ChatColor.RED + "Не удалось получить данные об улучшении.");
                        return;
                    }

                    boolean isOp = player.isOp();

                    if (isOp) {
                        upgradeManager.applyUpgrade(teamName, level);
                        displayUpgradeEffects(teamName);
                        if (level == 22 || level == 24 || level == 26) {
                            upgradeManager.getTeamSpecialUpgradeStatus().put(teamName, true);
                        }

                        player.closeInventory();
                        player.sendMessage(ChatColor.WHITE + "Замок улучшен до " + level + " уровня бесплатно!");
                        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                        openUpgradeMenu(player, teamName);

                    } else {
                        if (upgradeManager.canUpgrade(player, teamName, level)) {
                            Map<Material, Integer> cost = upgrade.getCost();
                            for (Map.Entry<Material, Integer> entry : cost.entrySet()) {
                                removeItems(player, entry.getKey(), entry.getValue());
                            }
                            upgradeManager.applyUpgrade(teamName, level);
                            displayUpgradeEffects(teamName);
                            player.closeInventory();
                            player.sendMessage(ChatColor.WHITE + "Замок улучшен до " + level + " уровня! ");
                            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                            openUpgradeMenu(player, teamName);
                        }
                    }
                }
            }

            //Этот код больше не нужен здесь, он перемещен в BonusUpgradesManager
           /* if (title.equals(ChatColor.RED + "Бонусная прокачка")) {
                if (event.getSlot() == 44) {
                    if (teamName != null) {
                        openUpgradeMenu(player, teamName);
                        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                    }
                }
            }*/
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        InventoryView view = event.getView();
        if (view != null) {
            String title = view.getTitle();
            if (title != null && (title.equals("Прокачка Замка") || title.equals(ChatColor.RED + "Бонусная прокачка"))) {
                event.setCancelled(true);
            }
        }
    }


    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        List<Block> blocks = new ArrayList<>(event.blockList());
        for (Block block : blocks) {
            if (magnetBlockTeams.containsKey(block.getLocation())) {
                event.blockList().remove(block);
            }
        }
    }

    private void removeItems(Player player, Material material, int amount) {
        Inventory inventory = player.getInventory();
        for (ItemStack item : inventory.getContents()) {
            if (item != null && item.getType() == material) {
                int itemAmount = item.getAmount();
                if (amount <= itemAmount) {
                    item.setAmount(itemAmount - amount);
                    return;
                } else {
                    inventory.remove(item);
                    amount -= itemAmount;
                }
            }
        }
    }

    public BonusUpgradesManager getBonusUpgradesManager() {
        return bonusUpgradesManager;
    }
}