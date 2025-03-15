package org.legendery.teamPlayPlugin.upgrades;

import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.legendery.teamPlayPlugin.TeamPlayPlugin;
import org.legendery.teamPlayPlugin.team.TeamManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryView;
import org.bukkit.event.inventory.InventoryDragEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class BonusUpgradesManager implements Listener {

    private final TeamPlayPlugin plugin;
    private final TeamManager teamManager;
    private final TeamMagnetBlockManager magnetBlockManager;
    private final Material grayStainedGlassPane = Material.GRAY_STAINED_GLASS_PANE;

    // Конфигурация улучшений (цены, названия, описания, эффекты)
    private final Map<String, UpgradeConfig> upgradeConfigs = new HashMap<>();

    // Команда, которая купила улучшение (ключ - id улучшения, значение - название команды)
    private final Map<String, String> purchasedUpgrades = new ConcurrentHashMap<>();

    private final Map<String, Long> invisibilityCooldowns = new ConcurrentHashMap<>();

    // Русификатор для названий предметов
    private final Map<Material, String> russianItemNames = new HashMap<>();

    public BonusUpgradesManager(TeamPlayPlugin plugin, TeamManager teamManager, TeamMagnetBlockManager magnetBlockManager) {
        this.plugin = plugin;
        this.teamManager = teamManager;
        this.magnetBlockManager = magnetBlockManager;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);

        // Инициализация русификатора
        initializeRussianItemNames();
        loadConfig();
        scheduleTasks();
    }

    private void initializeRussianItemNames() {
        russianItemNames.put(Material.EMERALD_BLOCK, "Изумрудный блок");
        russianItemNames.put(Material.GOLD_BLOCK, "Золотых блока");
        russianItemNames.put(Material.TURTLE_HELMET, "Черепаший шлемов");
        russianItemNames.put(Material.DIAMOND, "Алмаз");
        russianItemNames.put(Material.IRON_BLOCK, "Железных блока");
        russianItemNames.put(Material.NETHERITE_INGOT, "Незеритовых слитов");
        russianItemNames.put(Material.DIAMOND_BLOCK, "Алмазных блока");
        russianItemNames.put(Material.GOLDEN_PICKAXE, "Золотая кирка");
        russianItemNames.put(Material.EMERALD, "Изумруда");
        // Добавьте остальные предметы по мере необходимости
    }


    // Загрузка конфигурации из файла (теперь только эффекты и purchased)
    private void loadConfig() {
        FileConfiguration config = plugin.getConfig();
        ConfigurationSection upgradesSection = config.getConfigurationSection("upgrades");

        if (upgradesSection == null) {
            plugin.getLogger().warning("Секция 'upgrades' не найдена в config.yml. Создаю секцию с дефолтными настройками.");
            // Создаем дефолтную секцию upgrades
            upgradesSection = config.createSection("upgrades");
            createDefaultUpgrades(upgradesSection);
            plugin.saveConfig();
        }

        // Определяем улучшения здесь, в коде
        Map<String, UpgradeData> upgradeDataMap = new HashMap<>();
        upgradeDataMap.put("resistance", new UpgradeData("Сопротивление", "Сопротивление I", Material.TURTLE_HELMET, Arrays.asList("turtle_helmet:5", "iron_block:64"), Arrays.asList("resistance:1:10:60")));
        upgradeDataMap.put("haste", new UpgradeData("Спешка", "Спешка II", Material.GOLDEN_PICKAXE, Arrays.asList("diamond_block:64", "emerald:64"), Arrays.asList("speed:2:10:10")));
        upgradeDataMap.put("strength_speed", new UpgradeData("Сила и Скорость", "Сила II, Скорость II", Material.NETHERITE_SWORD, Arrays.asList("netherite_ingot:16", "gold_block:64"), Arrays.asList("strength:2:10:10", "speed:2:10:10")));

        for (Map.Entry<String, UpgradeData> entry : upgradeDataMap.entrySet()) {
            String upgradeId = entry.getKey();
            UpgradeData data = entry.getValue();

            // Парсим цены
            Map<Material, Integer> cost = parseCost(data.costStrings);

            // Создаем UpgradeConfig
            UpgradeConfig upgradeConfig = new UpgradeConfig(upgradeId, data.name, data.description, cost, data.effects, data.icon);
            upgradeConfigs.put(upgradeId, upgradeConfig);

            String purchasedBy = config.getString("purchased." + upgradeId);
            if (purchasedBy != null) {
                purchasedUpgrades.put(upgradeId, purchasedBy);
            }
        }
    }

    // Парсинг цен из config.yml
    private Map<Material, Integer> parseCost(List<String> costStrings) {
        Map<Material, Integer> cost = new HashMap<>();
        for (String costString : costStrings) {
            String[] parts = costString.split(":");
            if (parts.length == 2) {
                try {
                    Material material = Material.valueOf(parts[0].toUpperCase());
                    int amount = Integer.parseInt(parts[1]);
                    cost.put(material, amount);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Неверный формат цены: " + costString);
                }
            } else {
                plugin.getLogger().warning("Неверный формат цены: " + costString);
            }
        }
        return cost;
    }

    private void createDefaultUpgrades(ConfigurationSection upgradesSection) {
        // Пример: Сохранение купленных улучшений в config.yml
        // В данном случае, нужно только сохранить секцию purchased
    }


    // Сохранение конфигурации в файл
    private void saveConfig() {
        FileConfiguration config = plugin.getConfig();

        for (Map.Entry<String, String> entry : purchasedUpgrades.entrySet()) {
            config.set("purchased." + entry.getKey(), entry.getValue());
        }

        plugin.saveConfig();
    }

    // Запуск задач для работы улучшений
    private void scheduleTasks() {
        upgradeConfigs.forEach((upgradeId, config) -> {
            config.getEffects().forEach(effectString -> {
                String[] parts = effectString.split(":");
                if (parts.length != 4) {
                    plugin.getLogger().warning("Неверный формат эффекта: " + effectString);
                    return;
                }

                String type = parts[0];
                int level = Integer.parseInt(parts[1]);
                int duration = Integer.parseInt(parts[2]);
                int interval = Integer.parseInt(parts[3]);

                PotionEffectType potionEffectType = PotionEffectType.getByName(type.toUpperCase());
                if (potionEffectType == null) {
                    plugin.getLogger().warning("Неизвестный тип эффекта: " + type);
                    return;
                }

                new BukkitRunnable() {
                    @Override
                    public void run() {
                        String owningTeam = purchasedUpgrades.get(upgradeId);
                        if (owningTeam != null) {
                            applyPotionEffectToTeam(owningTeam, potionEffectType, level, duration);
                        }
                    }
                }.runTaskTimer(plugin, 0L, interval * 20L); // interval в секундах -> тики
            });
        });
    }


    // Применение эффекта зелья к игрокам команды
    private void applyPotionEffectToTeam(PotionEffectType type, int level, int duration, String teamName) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (teamManager.getPlayerTeam(player) != null && teamManager.getPlayerTeam(player).equals(teamName)) {
                player.addPotionEffect(new PotionEffect(type, duration * 20, level - 1));
            }
        }
    }
    private void applyPotionEffectToTeam(String teamName, PotionEffectType type, int level, int duration) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (teamManager.getPlayerTeam(player) != null && teamManager.getPlayerTeam(player).equals(teamName)) {
                player.addPotionEffect(new PotionEffect(type, duration * 20, level - 1)); // duration в секундах -> тики
            }
        }
    }

    private Inventory createBonusUpgradeMenu(Player player, String teamName) {
        Inventory bonusMenu = Bukkit.createInventory(null, 45, ChatColor.RED + "Бонусная прокачка");

        // Заполняем стеклами (серый цвет)
        ItemStack glass = new ItemStack(grayStainedGlassPane);
        ItemMeta glassMeta = glass.getItemMeta();
        glassMeta.setDisplayName(" ");
        glass.setItemMeta(glassMeta);

        for (int i = 0; i < 9; i++) {
            bonusMenu.setItem(i, glass);
        }
        bonusMenu.setItem(9, glass);
        bonusMenu.setItem(17, glass);
        bonusMenu.setItem(18, glass);
        bonusMenu.setItem(26, glass);
        bonusMenu.setItem(27, glass);
        bonusMenu.setItem(35, glass);
        for (int i = 36; i < 44; i++) {
            bonusMenu.setItem(i, glass);
        }



        // Создаем кнопки для каждого улучшения на клетку ниже +2
        int slot = 20; // Клетка ниже +2
        for (UpgradeConfig upgradeConfig : upgradeConfigs.values()) {
            ItemStack upgradeButton = createUpgradeButton(upgradeConfig, teamName);
            bonusMenu.setItem(slot, upgradeButton);
            slot += 2; // Размещаем кнопки через 1 слот
            if (slot == 22 || slot == 24 || slot == 31 || slot == 33 || slot == 40 || slot == 42) {
                slot += 0; // Перейти на следующую строку, если достигнут край инвентаря
            }
        }

        return bonusMenu;
    }

    private ItemStack createUpgradeButton(UpgradeConfig config, String teamName) {
        // Используем материал из конфига (или дефолтный, если не указан)
        Material material = config.getIcon();
        String displayName = ChatColor.GOLD + config.getName(); // Делаем название оранжевым по умолчанию

        // Добавляем (Куплено TeamName) серым цветом, если улучшение куплено
        if (purchasedUpgrades.containsKey(config.getId())) {
            String purchasedBy = purchasedUpgrades.get(config.getId());
            if (purchasedBy != null) {
                displayName = ChatColor.GOLD + config.getName() + ChatColor.GRAY + " (Куплено " + purchasedBy + ")";
            }
        }
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + config.getDescription());
        lore.add(""); // Пустая строка для разделения
        lore.add(ChatColor.DARK_GREEN + "Стоимость:");
        if (config.getCost().isEmpty()) {
            lore.add(ChatColor.RED + "Нет стоимости"); // Если стоимость пустая
        } else {
            for (Map.Entry<Material, Integer> costEntry : config.getCost().entrySet()) {
                lore.add(ChatColor.GRAY + " - " + costEntry.getValue() + " " + getRussianItemName(costEntry.getKey()));
            }
        }
        return createButtonItem(material, displayName, lore);
    }

    // Вспомогательный метод для получения русского названия предмета
    private String getRussianItemName(Material material) {
        return russianItemNames.getOrDefault(material, formatMaterialName(material));
    }

    // Вспомогательный метод для форматирования названий материалов
    private String formatMaterialName(Material material) {
        String name = material.toString().toLowerCase().replace("_", " ");
        return name.substring(0, 1).toUpperCase() + name.substring(1);
    }


    // Универсальный метод для создания кнопок
    private ItemStack createButtonItem(Material material, String displayName, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(displayName);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }


    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        InventoryView view = event.getView();
        if (view == null) return;

        String title = view.getTitle();
        if (title.equals(ChatColor.RED + "Бонусная прокачка")) {
            event.setCancelled(true);
            Player player = (Player) event.getWhoClicked();
            ItemStack clickedItem = event.getCurrentItem();

            if (clickedItem == null || !clickedItem.hasItemMeta()) return;

            String displayName = clickedItem.getItemMeta().getDisplayName();
            String teamName = teamManager.getPlayerTeam(player);
            if (teamName == null) return;


            // Получаем ID улучшения по названию кнопки (обрезаем "(Куплено TeamName)")
            String upgradeName = ChatColor.stripColor(displayName).replaceFirst(" \\(Куплено .+?\\)", "").trim();

            String upgradeId = upgradeConfigs.entrySet().stream()
                    .filter(entry -> entry.getValue().getName().equals(upgradeName))
                    .map(Map.Entry::getKey)
                    .findFirst()
                    .orElse(null);

            if (upgradeId == null) {
                return;
            }

            purchaseUpgrade(player, teamName, upgradeId);
            player.closeInventory();
            if (magnetBlockManager != null) {
                magnetBlockManager.openUpgradeMenu(player, teamName);
            }

        }
    }

    private void purchaseUpgrade(Player player, String teamName, String upgradeId) {
        if (purchasedUpgrades.values().stream().anyMatch(teamName::equals)) {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            player.sendMessage(ChatColor.RED + "Ваша команда уже приобрела бонусное улучшение.");
            return;
        }

        if (purchasedUpgrades.containsKey(upgradeId)) {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            player.sendMessage(ChatColor.RED + "Это улучшение уже куплено другой командой.");
            return;
        }

        UpgradeConfig config = upgradeConfigs.get(upgradeId);
        if (config == null) {
            return;
        }
        // **********ВСТАВЬТЕ КОД НИЖЕ***********
        teamName = teamManager.getPlayerTeam(player);
        plugin.getLogger().info("TeamName для игрока " + player.getName() + ": " + teamName); // Добавлено логирование
        ChatColor teamColor = magnetBlockManager.getTeamColor(teamName);
        // **********ВСТАВЬТЕ КОД ВЫШЕ***********

        // Проверка наличия ресурсов
        if (!hasEnoughItems(player, config.getCost())) {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            player.sendMessage(ChatColor.RED + "Недостаточно ресурсов для покупки улучшения.");
            return;
        }

        // Снятие ресурсов
        removeItems(config.getCost(), player);

        // Покупка улучшения
        purchasedUpgrades.put(upgradeId, teamName);
        saveConfig();
        if (magnetBlockManager != null) {
            teamColor = magnetBlockManager.getTeamColor(teamName);

            if (teamColor == null) {
                plugin.getLogger().warning("Не удалось получить цвет команды для: " + teamName + ". Сообщение в чат не будет отправлено с цветом.");
                String message =  "Команда "  + teamName +
                        ChatColor.WHITE + " купила уникальное улучшение для замка " +
                        ChatColor.GOLD + config.getName() + ChatColor.WHITE + "!";

                Bukkit.getServer().broadcastMessage(message);
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_TRADE, 1.0f, 1.0f); //звук оставляем
                return;
            }


            // Формируем сообщение для общего чата
            String message =  "Команда " + teamColor + teamName +
                    ChatColor.WHITE + " купила уникальное улучшение для замка " +
                    ChatColor.GOLD + config.getName() + ChatColor.WHITE + "!";

            Bukkit.getServer().broadcastMessage(message);
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_TRADE, 1.0f, 1.0f); //звук оставляем

        }
        openBonusUpgradeMenu(player, teamName);
    }

    // Проверка, есть ли у игрока достаточное количество предметов (новая версия)
    private boolean hasEnoughItems(Player player, Map<Material, Integer> cost) {
        for (Map.Entry<Material, Integer> costEntry : cost.entrySet()) {
            Material material = costEntry.getKey();
            int amount = costEntry.getValue();
            int count = 0;
            for (ItemStack item : player.getInventory().getContents()) {
                if (item != null && item.getType() == material) {
                    count += item.getAmount();
                }
            }
            if (count < amount) {
                return false;
            }
        }
        return true;
    }

    // Удаление предметов из инвентаря игрока (новая версия)
    private void removeItems(Map<Material, Integer> cost, Player player) {
        Inventory inventory = player.getInventory();
        for (Map.Entry<Material, Integer> costEntry : cost.entrySet()) {
            Material material = costEntry.getKey();
            int amount = costEntry.getValue();
            if (amount <= 0) continue;

            for (int i = 0; i < inventory.getSize(); i++) {
                ItemStack item = inventory.getItem(i);
                if (item != null && item.getType() == material) {
                    int itemAmount = item.getAmount();
                    if (amount <= itemAmount) {
                        item.setAmount(itemAmount - amount);
                        inventory.setItem(i, item);
                        break;
                    } else {
                        inventory.clear(i);
                        amount -= itemAmount;
                        if (amount <= 0) break;
                    }
                }
            }
        }
    }


    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        InventoryView view = event.getView();
        if (view != null) {
            String title = view.getTitle();
            if (title != null && (title.equals(ChatColor.RED + "Бонусная прокачка"))) {
                event.setCancelled(true);
            }
        }
    }

    public void openBonusUpgradeMenu(Player player, String teamName) {
        Inventory bonusMenu = createBonusUpgradeMenu(player, teamName);
        player.openInventory(bonusMenu);
    }

    private static class UpgradeData {
        private final String name;
        private final String description;
        private final Material icon;
        private final List<String> costStrings;
        private final List<String> effects;

        public UpgradeData(String name, String description, Material icon, List<String> costStrings, List<String> effects) {
            this.name = name;
            this.description = description;
            this.icon = icon;
            this.costStrings = costStrings;
            this.effects = effects;
        }
    }

    // Класс для хранения конфигурации улучшения
    private static class UpgradeConfig {
        private final String id;
        private final String name;
        private final String description;
        private final Map<Material, Integer> cost;
        private final List<String> effects;
        private final Material icon;

        public UpgradeConfig(String id, String name, String description, Map<Material, Integer> cost, List<String> effects, Material icon) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.cost = cost;
            this.effects = effects;
            this.icon = icon;
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        public Map<Material, Integer> getCost() {
            return cost;
        }

        public List<String> getEffects() {
            return effects;
        }

        public Material getIcon() {
            return icon;
        }
    }
}