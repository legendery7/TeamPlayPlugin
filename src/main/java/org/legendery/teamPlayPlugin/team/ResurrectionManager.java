package org.legendery.teamPlayPlugin.team;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.Team;
import org.legendery.teamPlayPlugin.manager.DeathToggleManager;
import org.legendery.teamPlayPlugin.manager.util.MessageCooldownManager;

import java.util.*;

public class ResurrectionManager implements Listener {

    private final JavaPlugin plugin;
    private final TeamManager teamManager;
    private final Material resurrectionAnchorMaterial;
    private final String resurrectionAnchorName;
    private final List<UUID> deadPlayers = new ArrayList<>();
    private Location resurrectionAnchorLocation;
    private final int numberOfLightnings = 5;
    private final Material temporaryAnchorBlock = Material.CRYING_OBSIDIAN;
    private final SpectatorJoinQuitHandler spectatorHandler;
    private final DeathToggleManager deathToggleManager;

    public ResurrectionManager(JavaPlugin plugin, TeamManager teamManager, String anchorMaterialName, String anchorName, SpectatorJoinQuitHandler spectatorHandler, DeathToggleManager deathToggleManager) { // Модифицируем конструктор
        this.plugin = plugin;
        this.teamManager = teamManager;
        this.resurrectionAnchorMaterial = Material.valueOf(anchorMaterialName.toUpperCase());
        this.resurrectionAnchorName = anchorName;
        this.spectatorHandler = spectatorHandler;
        this.deathToggleManager = deathToggleManager; // Инициализируем DeathToggleManager
        loadDeadPlayers();
       // Bukkit.getPluginManager().registerEvents(this, plugin);
    }


    private void loadDeadPlayers() {
        List<String> deadPlayerUUIDs = teamManager.getDatabase().getDeadPlayers();
        for (String uuidString : deadPlayerUUIDs) {
            UUID uuid = UUID.fromString(uuidString);
            deadPlayers.add(uuid);
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.getGameMode() != GameMode.SPECTATOR) {
                player.setGameMode(GameMode.SPECTATOR);
            }
        }
    }
    private final Map<String, Boolean> teamLossAnnounced = new HashMap<>();
    private void checkForTeamLoss(String teamName, Player lastDeadPlayer) {
        if (isTeamDefeated(teamName)) {
            if (!teamLossAnnounced.getOrDefault(teamName, false)) {
                Bukkit.broadcastMessage(ChatColor.RED + "Команда " + teamManager.getTeamColor(teamName) + teamName + ChatColor.RED + " проиграла! Последний выживший: " + lastDeadPlayer.getName());
                teamLossAnnounced.put(teamName, true);
            }
        }
    }
    private boolean isTeamDefeated(String teamName) {
        List<String> teamMembersUUIDs = teamManager.getDatabase().getTeamMembers(teamName);

        if (teamMembersUUIDs.isEmpty()) {
            return true;
        }

        // Проверяем, что все члены команды находятся в списке мертвых
        for (String playerUUIDString : teamMembersUUIDs) {
            try {
                UUID playerUUID = UUID.fromString(playerUUIDString);
                if (!deadPlayers.contains(playerUUID)) {
                    // Если хотя бы один игрок не в списке мертвых, команда еще не проиграла
                    return false;
                }
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Неверный UUID игрока: " + playerUUIDString);
            }
        }

        // Если все игроки в команде находятся в списке мертвых, команда проиграла
        return true;
    }
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (teamManager.getPlayerTeam(player) != null && deathToggleManager.isDeathEnabled()) { // Добавляем проверку DeathToggleManager
            UUID playerUUID = player.getUniqueId();
            deadPlayers.add(playerUUID);
            teamManager.getDatabase().saveDeadPlayer(playerUUID.toString());
            player.setGameMode(GameMode.SPECTATOR);
            player.sendMessage(ChatColor.RED + "Вы умерли! Ваши тимейты должны использовать якорь возрождения.");
            spectatorHandler.teleportToSpectatorWorld(player);

            // Проверяем, проиграла ли команда после смерти игрока
            String teamName = teamManager.getPlayerTeam(player);
            if (teamName != null) {
                checkForTeamLoss(teamName, player); // Передаем игрока в checkForTeamLoss
            }
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Material clickedBlockMaterial = event.getClickedBlock() != null ? event.getClickedBlock().getType() : null;
        Material itemInHand = player.getInventory().getItemInMainHand().getType();

        // Проверяем, что игрок кликнул по якорю и в руке святящийся камень
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK
                && clickedBlockMaterial == resurrectionAnchorMaterial) {

            // Если в руке святящийся камень, разрешаем клик
            if (itemInHand == Material.GLOWSTONE) {
                return; // Не отменяем событие
            }

            // Отменяем событие, если игрок находится в измерениях Nether или End
            if (player.getWorld().getEnvironment() == World.Environment.NETHER ||
                    player.getWorld().getEnvironment() == World.Environment.THE_END) {
                if (cooldownManager.canSendMessage(player)) {
                    player.sendMessage(ChatColor.RED + "Нельзя использовать якорь воскрешения в этом измерении!");
                }
                event.setCancelled(true);
                return;
            }

            // В остальных случаях отменяем событие
            event.setCancelled(true);
            resurrectionAnchorLocation = event.getClickedBlock().getLocation();
            openResurrectionMenu(player);
        }
    }

    private void openResurrectionMenu(Player player) {
        String teamName = teamManager.getPlayerTeam(player);
        if (teamName == null) {
            player.sendMessage(ChatColor.RED + "Вы не состоите в команде!");
            return;
        }

        List<Player> availableResurrections = new ArrayList<>();
        for (UUID deadPlayerUUID : deadPlayers) {
            Player deadPlayer = Bukkit.getPlayer(deadPlayerUUID);
            if (deadPlayer != null) {
                String deadPlayerTeam = teamManager.getPlayerTeam(deadPlayer);
                if (teamName.equals(deadPlayerTeam)) {
                    availableResurrections.add(deadPlayer);
                }
            }
        }

        if (availableResurrections.isEmpty()) {
            return;
        }

        int inventorySize = Math.min(54, (int) (Math.ceil((double) availableResurrections.size() / 9) * 9));
        Inventory resurrectionMenu = Bukkit.createInventory(null, inventorySize, ChatColor.DARK_GRAY + resurrectionAnchorName);

        for (Player deadPlayer : availableResurrections) {
            ItemStack skull = createPlayerSkull(deadPlayer);
            resurrectionMenu.addItem(skull);
        }
        player.openInventory(resurrectionMenu);
    }

    private ItemStack createPlayerSkull(Player player) {
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();
        meta.setOwningPlayer(player);

        String tag = teamManager.getPlayerTag(player);
        String teamName = teamManager.getPlayerTeam(player);

        ChatColor teamColor = ChatColor.WHITE;
        if (teamName != null) {
            Team team = teamManager.getTeam(teamName);
            if (team != null) {
                teamColor = team.getColor();
            }
        }

        String displayName = teamColor + player.getName() + " " + getTagDisplay(tag);
        meta.setDisplayName(displayName);

        List<ItemStack> costItems = getResurrectionCost(player);
        meta.setLore(getPriceLore(costItems));
        skull.setItemMeta(meta);
        return skull;
    }

    private String getTagDisplay(String tag) {
        if (tag != null) {
            switch (tag.toLowerCase()) {
                case "pve":
                    return ChatColor.DARK_GREEN + " (PvE)";
                case "pvp":
                    return ChatColor.RED + " (PvP)";
                case "cap":
                    return ChatColor.GOLD + " (Cap)";
                default:
                    return "";
            }
        }
        return "";
    }

    private String pluralize(int count, String one, String few, String many) {
        count = Math.abs(count); // Для отрицательных чисел тоже
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

    private String getFriendlyName(Material material, int amount) {
        switch (material) {
            case DIAMOND:
                return pluralize(amount, "Алмаз", "Алмаза", "Алмазов");
            case GOLD_INGOT:
                return pluralize(amount, "Золотой слиток", "Золотых слитка", "Золотых слитков");
            case COPPER_BLOCK:
                return pluralize(amount, "Медный блок", "Медных блока", "Медных блоков");
            case TNT:
                return pluralize(amount, "Динамит", "Динамита", "Динамитов");
            case EMERALD_BLOCK:
                return pluralize(amount, "Изумрудный блок", "Изурудных блока", "Изумрудных блоков");
            case PITCHER_PLANT:
                return pluralize(amount, "Кувшинница", "Кувшинницы", "Кувшинниц");
            case DRAGON_BREATH:
                return pluralize(amount, "Драконье дыхание", "Драконьего дыхания", "Драконьих дыханий");
            case ENCHANTED_GOLDEN_APPLE:
                return pluralize(amount, "Зачарованное золотое яблоко", "Зачарованных золотых яблока", "Зачарованных золотых яблок");
            case BEACON:
                return pluralize(amount, "Маяк", "Маяка", "Маяков");
            case HONEY_BLOCK:
                return pluralize(amount, "Блок меда", "Блока меда", "Блоков меда");
            case WITHER_ROSE:
                return pluralize(amount, "Роза визера", "Розы визера", "Роз визера");
            case TOTEM_OF_UNDYING:
                return pluralize(amount, "Тотем бессмертия", "Тотема бессмертия", "Тотемов бессмертия");
            case OBSIDIAN:
                return pluralize(amount, "Обсидиан", "Обсидиана", "Обсидианов");
            case NETHERITE_SWORD:
                return pluralize(amount, "Незеритовый меч", "Незеритовых меча", "Незеритовых мечей");
            case ELYTRA:
                return pluralize(amount, "Элитры", "Элитры", "Элитр");
            case NETHERITE_HOE:
                return pluralize(amount, "Незеритовая мотыга", "Незеритовых мотыги", "Незеритовых мотыг");
            case GOLDEN_APPLE:
                return pluralize(amount, "Золотое яблоко", "Золотых яблока", "Золотых яблок");
            case HAY_BLOCK:
                return pluralize(amount, "Сноп сена", "Снопа сена", "Снопов сена");
            default:
                return material.toString();
        }
    }

    private final MessageCooldownManager cooldownManager = new MessageCooldownManager(2);

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        ItemStack clickedItem = event.getCurrentItem();

        if (event.getView().getTitle().equals(ChatColor.DARK_GRAY + resurrectionAnchorName)) {
            event.setCancelled(true);

            if (clickedItem != null && clickedItem.getType() == Material.PLAYER_HEAD && clickedItem.hasItemMeta()) {
                SkullMeta meta = (SkullMeta) clickedItem.getItemMeta();
                OfflinePlayer target = meta.getOwningPlayer();

                if (target != null && target.isOnline()) {
                    Player targetPlayer = target.getPlayer();
                    List<ItemStack> costItems = getResurrectionCost(targetPlayer);
                    if (player.isOp()) {
                        player.sendMessage(ChatColor.GREEN + "Вы воскресили " + targetPlayer.getName() + " бесплатно.");
                        resurrectPlayer(player, targetPlayer);
                        player.closeInventory();
                        return;
                    }
                    // Проверяем хватает ли ресурсов
                    boolean hasAll = true;
                    StringBuilder missingResourcesMessage = new StringBuilder(ChatColor.RED + "Недостаточно ресурсов для воскрешения:\n");
                    for (ItemStack item : costItems) {
                        int requiredAmount = item.getAmount();
                        int playerHas = 0;

                        // Считаем сколько у игрока нужного ресурса
                        for (ItemStack invItem : player.getInventory().getStorageContents()) {
                            if (invItem != null && invItem.getType() == item.getType()) {
                                playerHas += invItem.getAmount();
                            }
                        }

                        // Если не хватает, то добавляем в сообщение
                        int missingAmount = requiredAmount - playerHas;
                        if (missingAmount > 0) {
                            hasAll = false;
                            missingResourcesMessage.append(ChatColor.RED)
                                    .append("- ")
                                    .append(missingAmount)
                                    .append(" ")
                                    .append(pluralize(missingAmount, getOneForm(item.getType()), getFewForm(item.getType()), getManyForm(item.getType())))
                                    .append("\n");
                        }
                    }

                    if (hasAll) {
                        removeAllItems(player.getInventory(), costItems);
                        player.sendMessage(ChatColor.WHITE + "Вы заплатили за воскрешение " + targetPlayer.getName());
                        resurrectPlayer(player, targetPlayer);
                    } else {
                        player.sendMessage(missingResourcesMessage.toString());
                    }
                }
                player.closeInventory();
            }
        }
    }

    private String getOneForm(Material material) {
        switch (material) {
            case DIAMOND:
                return "Алмаз";
            case GOLD_INGOT:
                return "Золотой слиток";
            case COPPER_BLOCK:
                return "Медный блок";
            case TNT:
                return "Динамит";
            case EMERALD_BLOCK:
                return "Изумрудный блок";
            case PITCHER_PLANT:
                return "Кувшинница";
            case DRAGON_BREATH:
                return "Драконье дыхание";
            case ENCHANTED_GOLDEN_APPLE:
                return "Зачарованное золотое яблоко";
            case BEACON:
                return "Маяк";
            case HONEY_BLOCK:
                return "Блок меда";
            case WITHER_ROSE:
                return "Роза визера";
            case TOTEM_OF_UNDYING:
                return "Тотем бессмертия";
            case OBSIDIAN:
                return "Обсидиан";
            case NETHERITE_SWORD:
                return "Незеритовый меч";
            case ELYTRA:
                return "Элитры";
            case NETHERITE_HOE:
                return "Незеритовая мотыга";
            case GOLDEN_APPLE:
                return "Золотое яблоко";
            case HAY_BLOCK:
                return "Сноп сена";
            default:
                return material.toString(); // Если не нашли, оставляем как есть
        }
    }

    private String getFewForm(Material material) {
        switch (material) {
            case DIAMOND:
                return "Алмаза";
            case GOLD_INGOT:
                return "Золотых слитка";
            case COPPER_BLOCK:
                return "Медных блока";
            case TNT:
                return "Динамита";
            case EMERALD_BLOCK:
                return "Изумрудных блока";
            case PITCHER_PLANT:
                return "Кувшиницы";
            case DRAGON_BREATH:
                return "Драконьего дыхания";
            case ENCHANTED_GOLDEN_APPLE:
                return "Зачарованных золотых яблока";
            case BEACON:
                return "Маяка";
            case HONEY_BLOCK:
                return "Блока меда";
            case WITHER_ROSE:
                return "Розы визера";
            case TOTEM_OF_UNDYING:
                return "Тотема бессмертия";
            case OBSIDIAN:
                return "Обсидиана";
            case NETHERITE_SWORD:
                return "Незеритовых меча";
            case ELYTRA:
                return "Элитры";
            case NETHERITE_HOE:
                return "Незеритовых мотыги";
            case GOLDEN_APPLE:
                return "Золотых яблока";
            case HAY_BLOCK:
                return "Снопа сена";
            default:
                return material.toString(); // Если не нашли, оставляем как есть
        }
    }

    private String getManyForm(Material material) {
        switch (material) {
            case DIAMOND:
                return "Алмазов";
            case GOLD_INGOT:
                return "Золотых слитков";
            case COPPER_BLOCK:
                return "Медных блоков";
            case TNT:
                return "Динамитов";
            case EMERALD_BLOCK:
                return "Изумрудных блоков";
            case PITCHER_PLANT:
                return "Кувшинниц";
            case DRAGON_BREATH:
                return "Драконьих дыханий";
            case ENCHANTED_GOLDEN_APPLE:
                return "Зачарованных золотых яблок";
            case BEACON:
                return "Маяков";
            case HONEY_BLOCK:
                return "Блоков меда";
            case WITHER_ROSE:
                return "Роз визера";
            case TOTEM_OF_UNDYING:
                return "Тотемов бессмертия";
            case OBSIDIAN:
                return "Обсидиана";
            case NETHERITE_SWORD:
                return "Незеритовых мечей";
            case ELYTRA:
                return "Элитр";
            case NETHERITE_HOE:
                return "Незеритовых мотыг";
            case GOLDEN_APPLE:
                return "Золотых яблок";
            case HAY_BLOCK:
                return "Снопов сена";
            default:
                return material.toString(); // Если не нашли, оставляем как есть
        }
    }

    private List<String> getPriceLore(List<ItemStack> costItems) {
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Цена:");
        if (costItems.isEmpty()) {
            lore.add(ChatColor.GREEN + "Бесплатно!");
        } else {
            for (ItemStack item : costItems) {
                lore.add(getFriendlyNameForPrice(item));
            }
        }
        return lore;
    }

    private String getFriendlyNameForPrice(ItemStack item) {
        int amount = item.getAmount();
        String costString = ChatColor.GRAY + "- ";
        String resourceName = getFriendlyName(item.getType(), amount);

        if (amount > 64) {
            int numStacks = amount / 64;
            int remainder = amount % 64;

            if (amount >= 192 && amount % 64 == 0) { //Если больше 192 и делится на 64
                costString += numStacks + "x64";
            } else if (amount >= 128 && amount % 64 == 0) { //Если больше 128 и делится на 64
                costString += numStacks + "x64";
            } else if (amount >= 64 && amount % 64 == 0) { //Если больше 64 и делится на 64
                costString += numStacks + "x64";
            } else { //Все остальные случаи
                costString += numStacks + "x64";
                if (remainder > 0) {
                    costString += " + " + remainder; //Добавляем остаток, если он есть
                }
            }

        } else {
            costString = ChatColor.GRAY + "- " + amount; //Выводим количество
        }

        costString += " " + resourceName; //Выводим название
        return costString;
    }

    private void resurrectPlayer(Player ressurector, Player target) {
        if (resurrectionAnchorLocation == null) {
            return;
        }

        World world = resurrectionAnchorLocation.getWorld();
        Block anchorBlock = resurrectionAnchorLocation.getBlock();
        Material originalAnchorMaterial = anchorBlock.getType();
        Location centerLocation = resurrectionAnchorLocation.clone().add(0.5, 0, 0.5); // Центр блока

        world.setStorm(true);
        world.playSound(centerLocation, Sound.WEATHER_RAIN, 1, 1);

        // Анимация: Волна огня по кругу из центра
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (int step = 0; step <= 10; step++) {
                final int currentStep = step;
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    double radius = currentStep;
                    for (int angle = 0; angle < 360; angle += 15) {
                        double radians = Math.toRadians(angle);
                        double x = radius * Math.cos(radians);
                        double z = radius * Math.sin(radians);
                        Location fireLocation = centerLocation.clone().add(x, 0.5, z);
                        world.spawnParticle(Particle.FLAME, fireLocation, 8, 0.1, 0.1, 0.1, 0.05);
                    }
                    world.playSound(centerLocation, Sound.BLOCK_FIRE_AMBIENT, 0.8f, 1.2f);
                }, currentStep * 5L);
            }
        }, 60L);

        // Анимация: Мелкие частицы притяжения
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (int i = 0; i < 150; i++) {
                double angle = Math.random() * Math.PI * 2;
                double radius = Math.random() * 4;
                double x = radius * Math.cos(angle);
                double z = radius * Math.sin(angle);
                Location particleLocation = centerLocation.clone().add(x, Math.random(), z);
                world.spawnParticle(Particle.END_ROD, particleLocation, 2, 0, 0, 0, 0.01);
            }
            target.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 150, 5));

            world.playSound(centerLocation, Sound.BLOCK_BEACON_POWER_SELECT, 1, 1.5f);
        }, 100L);

        // Основная часть воскрешения
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
        //    anchorBlock.setType(temporaryAnchorBlock);
            world.playSound(centerLocation, Sound.BLOCK_PORTAL_AMBIENT, 1, 1);
            world.spawnParticle(Particle.PORTAL, centerLocation, 70, 0.5, 0.5, 0.5, 0.3);

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                for (int i = 0; i < numberOfLightnings; i++) {
                    final int index = i;
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        world.strikeLightningEffect(centerLocation);

                        anchorBlock.setType(temporaryAnchorBlock);
                        world.playSound(centerLocation, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 1, 1);

                      //  anchorBlock.setType(originalAnchorMaterial);
                        world.spawnParticle(Particle.SMOKE_LARGE, centerLocation, 30, 0.5, 0.5, 0.5, 0.1);
                    }, index * 15L);
                }

                // Финальный взрыв синхронизирован с появлением игрока
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    // Финальный взрыв синхронизирован с появлением игрока
                    world.spawnParticle(Particle.EXPLOSION_HUGE, centerLocation, 1, 0, 0, 0, 0);
                    world.spawnParticle(Particle.FLAME, centerLocation, 150, 2, 2, 2, 0.05);
                    world.spawnParticle(Particle.SMOKE_LARGE, centerLocation, 100, 1, 1, 1, 0.1);
                    world.spawnParticle(Particle.CAMPFIRE_SIGNAL_SMOKE, centerLocation, 20, 0.5, 2, 0.5, 0.02);
                    world.spawnParticle(Particle.FIREWORKS_SPARK, centerLocation, 120, 1.5, 1.5, 1.5, 0.1);

                    for (int angle = 0; angle < 360; angle += 10) {
                        double radians = Math.toRadians(angle);
                        double x = Math.cos(radians) * 6;
                        double z = Math.sin(radians) * 6;
                        Location waveLocation = centerLocation.clone().add(x, 0.5, z);
                        world.spawnParticle(Particle.CLOUD, waveLocation, 10, 0.3, 0.3, 0.3, 0.05);
                    }

                    world.playSound(centerLocation, Sound.ENTITY_GENERIC_EXPLODE, 3, 0.5f);
                    world.playSound(centerLocation, Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, 2, 0.7f);
                    world.playSound(centerLocation, Sound.BLOCK_END_PORTAL_SPAWN, 1.5f, 0.9f);

                    // Появление игрока с магическими частицами
                    target.getWorld().spawnParticle(Particle.CRIT_MAGIC, target.getLocation(), 60, 0.5, 1, 0.5, 0.3);
                    target.setGlowing(true);

                   // anchorBlock.setType(originalAnchorMaterial);
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {

                        target.setGlowing(false);
                    }, 20L);

                    // Телепортация игрока в центр блока
                    world.setStorm(false);
                    Location teleportLocation = centerLocation.clone().add(0, 1, 0);

                  //  anchorBlock.setType(temporaryAnchorBlock);
                    // Направление игрока лицом к воскресителю
                    double deltaX = ressurector.getLocation().getX() - teleportLocation.getX();
                    double deltaZ = ressurector.getLocation().getZ() - teleportLocation.getZ();
                    double yaw = Math.toDegrees(Math.atan2(deltaZ, deltaX)) - 90;
                    teleportLocation.setYaw((float) yaw);

                    target.teleport(teleportLocation);
                    target.setGameMode(GameMode.SURVIVAL);
                    target.sendMessage(ChatColor.GOLD + "Вас воскресил " + ressurector.getName());

                    UUID playerUUID = target.getUniqueId();
                    deadPlayers.remove(playerUUID);
                    teamManager.getDatabase().removeDeadPlayer(playerUUID.toString());

                }, numberOfLightnings * 15L);
            }, 40L);
        }, 60L);
    }

    private List<ItemStack> getResurrectionCost(Player player) {
        String tag = teamManager.getPlayerTag(player);
        List<ItemStack> costItems = new ArrayList<>();
        Set<Material> addedMaterials = new HashSet<>(); // Set для отслеживания добавленных материалов

        switch (tag != null ? tag.toLowerCase() : "") {
            case "pve":
              //  if (addedMaterials.add(Material.COPPER_BLOCK)) {
            //        costItems.add(new ItemStack(Material.COPPER_BLOCK, 64));
             //   }
                if (addedMaterials.add(Material.DIAMOND)) {
                    costItems.add(new ItemStack(Material.DIAMOND, 16));
                }
           //     if (addedMaterials.add(Material.HAY_BLOCK)) {
              //      costItems.add(new ItemStack(Material.HAY_BLOCK, 64));
              //  }

                if (addedMaterials.add(Material.GOLDEN_APPLE)) {
                    costItems.add(new ItemStack(Material.GOLDEN_APPLE, 12));
                }
                if (addedMaterials.add(Material.EMERALD_BLOCK)) {
                    costItems.add(new ItemStack(Material.EMERALD_BLOCK, 8));
                }
                if (addedMaterials.add(Material.NETHERITE_HOE)) {
                    costItems.add(new ItemStack(Material.NETHERITE_HOE, 1));
                }
                if (addedMaterials.add(Material.TOTEM_OF_UNDYING)) {
                    costItems.add(new ItemStack(Material.TOTEM_OF_UNDYING, 1));
                }
                break;
            case "pvp":
             //   if (addedMaterials.add(Material.COPPER_BLOCK)) {
             //       costItems.add(new ItemStack(Material.COPPER_BLOCK, 64)); // 2 стака меди
             //   }
                if (addedMaterials.add(Material.GOLDEN_APPLE)) {
                    costItems.add(new ItemStack(Material.GOLDEN_APPLE, 24));
                }
                if (addedMaterials.add(Material.OBSIDIAN)) {
                    costItems.add(new ItemStack(Material.OBSIDIAN, 16));
                }
                if (addedMaterials.add(Material.EMERALD_BLOCK)) {
                    costItems.add(new ItemStack(Material.EMERALD_BLOCK, 12));
                }

                if (addedMaterials.add(Material.NETHERITE_SWORD)) {
                    costItems.add(new ItemStack(Material.NETHERITE_SWORD, 1));
                }
                if (addedMaterials.add(Material.TOTEM_OF_UNDYING)) {
                    costItems.add(new ItemStack(Material.TOTEM_OF_UNDYING, 1));
                }
             //   if (addedMaterials.add(Material.WITHER_ROSE)) {
              //      costItems.add(new ItemStack(Material.WITHER_ROSE, 1));
              //  }
                break;
            case "cap":
                if (addedMaterials.add(Material.COPPER_BLOCK)) {
                    costItems.add(new ItemStack(Material.COPPER_BLOCK, 24)); // 3 стака медных блоков
                }

                if (addedMaterials.add(Material.EMERALD_BLOCK)) {
                    costItems.add(new ItemStack(Material.EMERALD_BLOCK, 16));
                }
                if (addedMaterials.add(Material.TNT)) {
                    costItems.add(new ItemStack(Material.TNT, 4));
                }
              //  if (addedMaterials.add(Material.DRAGON_BREATH)) {
               //     costItems.add(new ItemStack(Material.DRAGON_BREATH, 4));
              //  }
                if (addedMaterials.add(Material.ENCHANTED_GOLDEN_APPLE)) {
                    costItems.add(new ItemStack(Material.ENCHANTED_GOLDEN_APPLE, 1));
                }
                if (addedMaterials.add(Material.TOTEM_OF_UNDYING)) {
                    costItems.add(new ItemStack(Material.TOTEM_OF_UNDYING, 1));
                }
              //  if (addedMaterials.add(Material.BEACON)) {
            //        costItems.add(new ItemStack(Material.BEACON, 1));
              //  }
                break;
            default:
                break;
        }
        return costItems;
    }

    private void removeAllItems(Inventory inventory, List<ItemStack> costItems) {
        for (ItemStack item : costItems) {
            int amount = item.getAmount();
            Material type = item.getType();

            for (ItemStack invItem : inventory.getStorageContents()) {
                if (invItem != null && invItem.getType() == type) {
                    int invAmount = invItem.getAmount();
                    if (invAmount >= amount) {
                        invItem.setAmount(invAmount - amount);
                        break;
                    } else {
                        amount -= invAmount;
                        invItem.setAmount(0);
                    }
                }
            }
        }
    }
}