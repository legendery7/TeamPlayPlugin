package org.legendery.teamPlayPlugin.upgrades;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;
import org.legendery.teamPlayPlugin.DatabaseManager;
import org.legendery.teamPlayPlugin.TeamPlayPlugin;
import org.legendery.teamPlayPlugin.model.TeamUpgrade;
import org.legendery.teamPlayPlugin.model.Upgrade;
import org.legendery.teamPlayPlugin.unlocks.CraftUnlockUpgrade;
import org.legendery.teamPlayPlugin.unlocks.PermissionUpgrade;
import org.legendery.teamPlayPlugin.unlocks.TeamEffectUpgrade;
import org.legendery.teamPlayPlugin.manager.util.MessageCooldownManager;

import java.util.HashMap;
import java.util.Map;

public class UpgradeManager {

    private final TeamPlayPlugin plugin;
    private final Map<Integer, Upgrade> upgrades = new HashMap<>();
    private final Map<String, ChatColor> teamColors = new HashMap<>();
    private final MessageCooldownManager cooldownManager = new MessageCooldownManager(2);
    private final Map<String, Boolean> teamSpecialUpgradeStatus = new HashMap<>();
    private final DatabaseManager databaseManager;
    public UpgradeManager(TeamPlayPlugin plugin) {
        this.plugin = plugin;
        initializeUpgrades();
        initializeTeamColors();
        this.databaseManager = plugin.getDatabaseManager();
    }

    public void initializeTeamColors() {
        teamColors.put("red", ChatColor.RED);
        teamColors.put("purple", ChatColor.DARK_PURPLE);
        teamColors.put("gold", ChatColor.GOLD);

        teamColors.put("silver", ChatColor.GRAY);
        teamColors.put("green", ChatColor.DARK_GREEN);
        teamColors.put("aqua", ChatColor.DARK_AQUA);

    }

    public boolean hasActiveBonusUpgrade() {
        // Проверяем, есть ли активный бонусный апгрейд в базе данных
        return databaseManager.hasTeamUpgrade("totem") ||
                databaseManager.hasTeamUpgrade("resistance") ||
                databaseManager.hasTeamUpgrade("invisibility");
    }
    private void initializeUpgrades() {
        upgrades.put(1, new Upgrade(1, Map.of(
                Material.WOODEN_PICKAXE, 1
        ), new PermissionUpgrade(1, "Позволяет добывать дерево.", "team.mine.wood")));

        upgrades.put(2, new Upgrade(2, Map.of(
                Material.COBBLESTONE, 4 * 64,
                Material.OAK_LOG, 64,
                Material.COAL, 32,
                Material.WHEAT_SEEDS, 32,
                Material.SWEET_BERRIES, 64
        ), new PermissionUpgrade(2, "Разрешает крафт каменных предметов.", "team.craft.copper")));

        upgrades.put(3, new Upgrade(3, Map.of(
                Material.RAW_COPPER, 64,
                Material.CHARCOAL, 64,
                Material.SWEET_BERRIES, 64
        ), new PermissionUpgrade(3, "Разрешает крафт медных предметов.\nДоступ к медной броне.", "team.craft.copper")));

        upgrades.put(4, new Upgrade(4, Map.of(
                Material.IRON_INGOT, 128,
                Material.SMOOTH_STONE, 4 * 64,
                Material.SAND, 4 * 64,
                Material.APPLE, 32,
                Material.COD, 64,
                Material.SALMON, 64,
                Material.COOKED_BEEF, 32
        ), new PermissionUpgrade(4, "Разрешает крафт железных предметов.\nДаёт доступ к новым рецептам.\nОптимизация добычи.", "team.craft.iron_tools")));

        upgrades.put(5, new Upgrade(5, Map.of(
                Material.COPPER_INGOT, 4 * 64,
                Material.SUGAR, 2 * 64,
                Material.HAY_BLOCK, 64,
                Material.IRON_INGOT, 64,
                Material.CHARCOAL, 2 * 64,
                Material.DIAMOND, 16,
                Material.BONE, 32,
                Material.NAUTILUS_SHELL, 24
        ), new TeamEffectUpgrade(5, "Дает эффект скорости I.\nУскоряет передвижение команды.", PotionEffectType.SPEED, 0, 200, 200)));

        upgrades.put(6, new Upgrade(6, Map.of(
                Material.DIAMOND, 64,
                Material.WHITE_WOOL, 64,
                Material.SALMON, 64,
                Material.COD, 64,
                Material.CARROT, 64,
                Material.POTATO, 64,
                Material.PUFFERFISH, 16
        ), new PermissionUpgrade(6, "Разрешает крафт алмазных предметов.\nОткрывает доступ к новым возможностям.", "team.craft.diamond_tools")));

        upgrades.put(7, new Upgrade(7, Map.of(
                Material.REDSTONE, 8 * 64,
                Material.BOOK, 2 * 64,
                Material.OBSIDIAN, 64,
                Material.DIAMOND, 64,
                Material.COOKED_CHICKEN, 64,
                Material.FERMENTED_SPIDER_EYE, 64,
                Material.HONEYCOMB, 16
        ), new PermissionUpgrade(7, "Разрешает использование стола зачарований.\nДает возможность улучшать предметы.", "team.enchant")));

        upgrades.put(8, new Upgrade(8, Map.of(
                Material.WHITE_WOOL, 4 * 64,
                Material.SMOOTH_STONE, 8 * 64,
                Material.COPPER_BLOCK, 64,
                Material.NAUTILUS_SHELL, 32,
                Material.ROTTEN_FLESH, 2*64,
                Material.PUFFERFISH, 32,
                Material.HONEY_BOTTLE, 16
        ), new TeamEffectUpgrade(8, "Дает эффект подводного дыхания I.\nПозволяет дольше находиться под водой.", PotionEffectType.WATER_BREATHING, 0, 200, 200)));

        upgrades.put(9, new Upgrade(9, Map.of(
                Material.IRON_BLOCK, 2 * 64,
                Material.REDSTONE_BLOCK, 4 * 64,
                Material.OAK_LOG, 4 * 64,
                Material.POINTED_DRIPSTONE, 64,
                Material.HAY_BLOCK, 2 * 64
        ), new PermissionUpgrade(9, "Разрешает использование наковальни.\nПозволяет чинить и переименовывать предметы.", "team.anvil")));

        upgrades.put(10, new Upgrade(10, Map.of(
                Material.GOLD_INGOT, 5 * 64,
                Material.REDSTONE_BLOCK, 64,
                Material.LAPIS_BLOCK, 2 * 64,
                Material.SAND, 18 * 64,
                Material.OBSIDIAN, 64

        ), new PermissionUpgrade(10, "Разрешает крафт золотых предметов.\nОткрывает новые возможности.", "team.craft.gold_items")));

        upgrades.put(11, new Upgrade(11, Map.of(
                Material.BLAZE_POWDER,  64,
                Material.MAGMA_CREAM, 32,
                Material.NETHER_WART, 3 * 64,
                Material.GLOWSTONE_DUST, 4 * 64,
                Material.GUNPOWDER, 3 * 64
        ), new TeamEffectUpgrade(11, "Дает эффект огнестойкости I.\nКоманда не получает урон от огня.", PotionEffectType.FIRE_RESISTANCE, 0, 200, 200)));

        upgrades.put(12, new Upgrade(12, Map.of(
                Material.DIAMOND_BLOCK, 32,
                Material.NETHERITE_INGOT, 12,
                Material.ENCHANTED_GOLDEN_APPLE, 4,
                Material.GOLDEN_CARROT, 64,
                Material.COOKED_PORKCHOP, 64
        ), new PermissionUpgrade(12, "Разрешает использование стола кузнеца.\nОткрывает доступ к улучшениям незерита.", "team.stol")));

        upgrades.put(13, new Upgrade(13, Map.of(
                Material.BLAZE_POWDER, 15 * 64,
                Material.COPPER_BLOCK, 4 * 64,
                Material.EMERALD, 64,
                Material.ENCHANTED_GOLDEN_APPLE, 16,
                Material.GUNPOWDER, 3 * 64,
                Material.GLISTERING_MELON_SLICE, 2*64,
                Material.CALCITE, 4 * 64

        ), new TeamEffectUpgrade(13, "Дает эффект силы I.\nУвеличивает урон команды.", PotionEffectType.INCREASE_DAMAGE, 0, 200, 200)));

        upgrades.put(14, new Upgrade(14, Map.of(
                Material.DRIED_KELP_BLOCK, 2*64,
                Material.CACTUS, 2*64,
                Material.BAMBOO, 2*64,
                Material.HONEY_BLOCK, 32,
                Material.HONEYCOMB_BLOCK, 32,
                Material.TURTLE_HELMET, 4
        ), new PermissionUpgrade(14, "Разрешает крафт шаблонов.\nОткрывает доступ к кастомизации брони.", "team.craft.templates")));

        upgrades.put(15, new Upgrade(15, Map.of(
                Material.REDSTONE_BLOCK, 4 * 64,
                Material.DIAMOND_BLOCK, 48,
                Material.EMERALD_BLOCK, 64,
                Material.GHAST_TEAR, 32,
                Material.ENCHANTED_GOLDEN_APPLE, 16,
                Material.TOTEM_OF_UNDYING, 5,
                Material.NETHER_STAR, 1
        ), new TeamEffectUpgrade(15, "Дает эффект регенерации I.\nКоманда быстрее восстанавливает здоровье.", PotionEffectType.REGENERATION, 0, 200, 200)));

        upgrades.put(16, new Upgrade(16, Map.of(
                Material.OAK_LOG, 8 * 64,
                Material.END_ROD, 4 * 64,
                Material.TNT, 2 * 64,
                Material.GOLD_INGOT,  9*64,
                Material.SPONGE, 8
        ), new CraftUnlockUpgrade(16, "Открывает все оставшиеся крафты и доступ к бонусным улучшениям.\nДаёт полный доступ ко всем рецептам.",
                Material.BEACON, Material.RESPAWN_ANCHOR, Material.END_CRYSTAL)));
    }



    public Upgrade getUpgrade(int level) {
        return upgrades.get(level);
    }

    public int getTeamLevel(String teamName) {
        DatabaseManager databaseManager = plugin.getDatabaseManager();
        return databaseManager.getTeamLevel(teamName);
    }

    public boolean canUpgrade(Player player, String teamName, int nextLevel) {
        Upgrade upgrade = getUpgrade(nextLevel);
        if (upgrade == null) {
            player.sendMessage(ChatColor.RED + "Нет доступных улучшений на этот уровень!");
            return false;
        }

        // Проверяем, можно ли команде улучшаться (только один из 3 специальных апгрейдов)
        if (nextLevel == 22 || nextLevel == 24 || nextLevel == 26) {
            if (teamSpecialUpgradeStatus.containsKey(teamName) && teamSpecialUpgradeStatus.get(teamName)) {
                player.sendMessage(ChatColor.RED + "Ваша команда уже выбрала одно из этих улучшений!");
                return false;
            }
        }

        Map<Material, Integer> cost = upgrade.getCost();
        Map<Material, Integer> missing = new HashMap<>();
        boolean canAfford = true;

        for (Map.Entry<Material, Integer> entry : cost.entrySet()) {
            Material material = entry.getKey();
            int requiredAmount = entry.getValue();
            int playerHas = getItemAmount(player, material);

            if (playerHas < requiredAmount) {
                missing.put(material, requiredAmount - playerHas);
                canAfford = false;
            }
        }

        if (!canAfford) {
            if (cooldownManager.canSendMessage(player)) {
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                player.closeInventory();
                StringBuilder message = new StringBuilder(ChatColor.RED + "Недостаточно ресурсов для улучшения:\n");
                for (Map.Entry<Material, Integer> entry : missing.entrySet()) {
                    Material material = entry.getKey();
                    int missingAmount = entry.getValue();
                    message.append(ChatColor.RED).append("- ")
                            .append(missingAmount).append(" ")
                            .append(capitalizeFirstLetter(getDeclensionForm(material, missingAmount)))
                            .append("\n");
                }
                player.sendMessage(message.toString());
            }
            return false;
        }
        return true;
    }

    private int getItemAmount(Player player, Material material) {
        int amount = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == material) {
                amount += item.getAmount();
            }
        }
        return amount;
    }

    private String getDeclensionForm(Material material, int amount) {
        switch (material) {
            case WOODEN_PICKAXE:
                return declension(amount, "деревянная кирка", "деревянные кирки", "деревянных кирок");
            case OAK_LOG:
                return declension(amount, "дубовое бревно", "дубовых бревна", "дубовых бревен");
            case COBBLESTONE:
                return declension(amount, "булыжник", "булыжника", "булыжников");
            case GLOWSTONE_DUST:
                return declension(amount, "светопыль", "светопыли", "светопыли");
            case EMERALD:
                return declension(amount, "изумруд", "изумруда", "изумрудов");
            case IRON_BLOCK:
                return declension(amount, "железный блок", "железных блока", "железных блоков");
            case SUGAR:
                return declension(amount, "сахар", "сахара", "сахара");
            case COAL:
                return declension(amount, "уголь", "угля", "угля");
            case WHEAT_SEEDS:
                return declension(amount, "семена пшеницы", "семени пшеницы", "семян пшеницы");
            case RAW_COPPER:
                return declension(amount, "рудная медь", "рудной меди", "рудной меди");
            case STONE:
                return declension(amount, "камень", "камня", "камней");
            case CHARCOAL:
                return declension(amount, "древесный уголь", "древесного угля", "древесного угля");
            case IRON_INGOT:
                return declension(amount, "железный слиток", "железных слитка", "железных слитков");
            case COPPER_INGOT:
                return declension(amount, "медный слиток", "медных слитка", "медных слитков");
            case WHITE_WOOL:
                return declension(amount, "белая шерсть", "белой шерсти", "белой шерсти");
            case SAND:
                return declension(amount, "песок", "песка", "песка");
            case COD:
                return declension(amount, "треска", "трески", "тресок");
            case SALMON:
                return declension(amount, "лосось", "лосося", "лососей");
            case COOKED_BEEF:
                return declension(amount, "стейк", "стейка", "стейков");
            case GOLD_INGOT:
                return declension(amount, "золотой слиток", "золотых слитка", "золотых слитков");
            case REDSTONE:
                return declension(amount, "редстоун", "редстоуна", "редстоунов");
            case LAPIS_BLOCK:
                return declension(amount, "блок лазурита", "блока лазурита", "блоков лазурита");
            case OBSIDIAN:
                return declension(amount, "обсидиан", "обсидиана", "обсидиана");
            case BOOK:
                return declension(amount, "книга", "книги", "книг");
            case HONEYCOMB:
                return declension(amount, "медовые соты", "медовых сот", "медовых сот");
            case BLAZE_POWDER:
                return declension(amount, "огненный порошок", "огненного порошка", "огненного порошка");
            case MAGMA_CREAM:
                return declension(amount, "сгусток магмы", "сгустка магмы", "сгустков магмы");
            case NETHER_WART:
                return declension(amount, "незерский нарост", "незерского нароста", "незерских наростов");
            case NETHERITE_INGOT:
                return declension(amount, "незеритовый слиток", "незеритовых слитка", "незеритовых слитков");
            case GHAST_TEAR:
                return declension(amount, "слеза гаста", "слезы гаста", "слез гаста");
            case ENCHANTED_GOLDEN_APPLE:
                return declension(amount, "зачарованное золотое яблоко", "зачарованных золотых яблока", "зачарованных золотых яблок");
            case BEACON:
                return declension(amount, "маяк", "маяка", "маяков");
            case NETHER_STAR:
                return declension(amount, "звезда незера", "звезды незера", "звезд незера");
            case RESPAWN_ANCHOR:
                return declension(amount, "якорь возрождения", "якоря возрождения", "якорей возрождения");
            case END_CRYSTAL:
                return declension(amount, "кристалл Края", "кристалла Края", "кристаллов Края");
            case SMOOTH_STONE:
                return declension(amount, "гладкий камень", "гладкого камня", "гладких камней");
            case HAY_BLOCK:
                return declension(amount, "блок сена", "блока сена", "блоков сена");
            case COPPER_BLOCK:
                return declension(amount, "медный блок", "медных блока", "медных блоков");
            case HONEY_BOTTLE:
                return declension(amount, "бутылка меда", "бутылки меда", "бутылок меда");
            case REDSTONE_BLOCK:
                return declension(amount, "блок редстоуна", "блока редстоуна", "блоков редстоуна");
            case NAUTILUS_SHELL:
                return declension(amount, "раковина наутилуса", "раковины наутилуса", "раковин наутилуса");
            case PUFFERFISH:
                return declension(amount, "иглобрюх", "иглобрюха", "иглобрюхов");
            case DIAMOND_BLOCK:
                return declension(amount, "алмазный блок", "алмазного блока", "алмазных блоков");
            case DIAMOND:
                return declension(amount, "алмаз", "алмаза", "алмазов");
            case GOLDEN_CARROT:
                return declension(amount, "золотая морковь", "золотой моркови", "золотых морковок");
            case NETHERITE_UPGRADE_SMITHING_TEMPLATE:
                return declension(amount, "Незеритовое улучшение", "Незеритовых улучшения", "Незеритовых улучшений");
            case CALCITE:
                return declension(amount, "кальцит", "кальцита", "кальцита");
            case DRIED_KELP_BLOCK:
                return declension(amount, "блок сушеной ламинарии", "блока сушеной ламинарии", "блоков сушеной ламинарии");
            case CACTUS:
                return declension(amount, "кактус", "кактуса", "кактусов");
            case BAMBOO:
                return declension(amount, "бамбук", "бамбука", "бамбука");
            case HONEY_BLOCK:
                return declension(amount, "медовый блок", "медового блока", "медовых блоков");
            case HONEYCOMB_BLOCK:
                return declension(amount, "блок медовых сот", "блока медовых сот", "блоков медовых сот");
            case TURTLE_HELMET:
                return declension(amount, "черепаший шлем", "черепашьего шлема", "черепашьих шлемов");
            case EMERALD_BLOCK:
                return declension(amount, "изумрудный блок", "изумрудного блока", "изумрудных блоков");
            case TOTEM_OF_UNDYING:
                return declension(amount, "тотем бессмертия", "тотема бессмертия", "тотемов бессмертия");
            case END_ROD:
                return declension(amount, "стержень Края", "стержня Края", "стержней Края");
            case TNT:
                return declension(amount, "динамит", "динамита", "динамита");
            case SPONGE:
                return declension(amount, "губка", "губки", "губок");
            case GUNPOWDER:
                return declension(amount, "порох", "пороха", "пороха");
            case CARROT:
                return declension(amount, "морковь", "моркови", "морковок");
            case APPLE:
                return declension(amount, "яблоко", "яблока", "яблок");
            case GLISTERING_MELON_SLICE:
                return declension(amount, "сверкающий ломтик арбуза", "сверкающих ломтика арбуза", "сверкающих ломтиков арбуза");
            case FERMENTED_SPIDER_EYE:
                return declension(amount, "маринованный паучий глаз", "маринованных паучьих глаза", "маринованных паучьих глаз");
            case COOKED_PORKCHOP:
                return declension(amount, "жареная свинина", "жареной свинины", "жареной свинины");
            case COOKED_CHICKEN:
                return declension(amount, "жареная курятина", "жареной курятины", "жареной курятины");
            case BONE:
                return declension(amount, "кость", "кости", "костей");
            case ROTTEN_FLESH:
                return declension(amount, "гнилая плоть", "гнилой плоти", "гнилой плоти");
            case POTATO:
                return declension(amount, "картофель", "картофеля", "картофелей");
            case SWEET_BERRIES:
                return declension(amount, "сладкие ягоды", "сладких ягод", "сладких ягод");
            case POINTED_DRIPSTONE:
                return declension(amount, "капельник", "капельника", "капельников");

            default:
                return translateMaterialName(material);


        }
    }

    private static String declension(int amount, String form1, String form2, String form5) {
        amount = Math.abs(amount);
        if (amount % 100 >= 11 && amount % 100 <= 14) {
            return form5;
        }
        int lastDigit = amount % 10;
        if (lastDigit == 1) {
            return form1;
        }
        if (lastDigit >= 2 && lastDigit <= 4) {
            return form2;
        }
        return form5;
    }

    private String translateMaterialName(Material material) {
        switch (material) {
            case WOODEN_PICKAXE:
                return "деревянная кирка";
            case OAK_LOG:
                return "дубовое бревно";
            case SUGAR:
                return "сахар";
            case COBBLESTONE:
                return "булыжник";
            case COAL:
                return "уголь";
            case WHEAT_SEEDS:
                return "семена пшеницы";
            case RAW_COPPER:
                return "рудная медь";
            case STONE:
                return "камень";
            case CHARCOAL:
                return "древесный уголь";
            case IRON_INGOT:
                return "железный слиток";
            case COPPER_INGOT:
                return "медный слиток";
            case WHITE_WOOL:
                return "белая шерсть";
            case SAND:
                return "песок";
            case COD:
                return "треска";
            case SALMON:
                return "лосось";
            case COOKED_BEEF:
                return "стейк";
            case GOLD_INGOT:
                return "золотой слиток";
            case REDSTONE:
                return "редстоун";
            case LAPIS_BLOCK:
                return "блок лазурита";
            case OBSIDIAN:
                return "обсидиан";
            case BOOK:
                return "книга";
            case HONEYCOMB:
                return "медовые соты";
            case BLAZE_POWDER:
                return "огненный порошок";
            case MAGMA_CREAM:
                return "сгусток магмы";
            case NETHER_WART:
                return "незерский нарост";
            case NETHERITE_INGOT:
                return "незеритовый слиток";
            case GHAST_TEAR:
                return "слеза гаста";
            case ENCHANTED_GOLDEN_APPLE:
                return "зачарованное золотое яблоко";
            case BEACON:
                return "маяк";
            case NETHER_STAR:
                return "звезда незера";
            case RESPAWN_ANCHOR:
                return "якорь возрождения";
            case END_CRYSTAL:
                return "кристалл Края";
            case SMOOTH_STONE:
                return "гладкий камень";
            case HAY_BLOCK:
                return "блок сена";
            case COPPER_BLOCK:
                return "медный блок";
            case HONEY_BOTTLE:
                return "бутылка меда";
            case REDSTONE_BLOCK:
                return "редстоуновый блок";
            case NAUTILUS_SHELL:
                return "раковина наутилуса";
            case PUFFERFISH:
                return "иглобрюх";
            case DIAMOND_BLOCK:
                return "алмазный блок";
            case GOLDEN_CARROT:
                return "золотая морковь";
            case NETHERITE_UPGRADE_SMITHING_TEMPLATE:
                return "незеритовое улучшение";
            case CALCITE:
                return "кальцит";
            case DRIED_KELP_BLOCK:
                return "блок сушеной ламинарии";
            case CACTUS:
                return "кактус";
            case BAMBOO:
                return "бамбук";
            case HONEY_BLOCK:
                return "медовый блок";
            case HONEYCOMB_BLOCK:
                return "блок медовых сот";
            case TURTLE_HELMET:
                return "черепаший шлем";
            case EMERALD_BLOCK:
                return "изумрудный блок";
            case TOTEM_OF_UNDYING:
                return "тотем бессмертия";
            case END_ROD:
                return "стержень Края";
            case TNT:
                return "динамит";
            case SPONGE:
                return "губка";
            case GUNPOWDER:
                return "порох";
            case CARROT:
                return "морковь";
            case APPLE:
                return "яблоко";
            case GLISTERING_MELON_SLICE:
                return "сверкающий ломтик арбуза";
            case FERMENTED_SPIDER_EYE:
                return "маринованный паучий глаз";
            case COOKED_PORKCHOP:
                return "жареная свинина";
            case COOKED_CHICKEN:
                return "жареная курятина";
            case BONE:
                return "кость";
            case ROTTEN_FLESH:
                return "гнилая плоть";
            case POTATO:
                return "картофель";
            case SWEET_BERRIES:
                return "сладкие ягоды";
            case POINTED_DRIPSTONE:
                return "капельник";
            default:
                return material.name();
        }
    }

    private String capitalizeFirstLetter(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        return text.substring(0, 1).toUpperCase() + text.substring(1);
    }

    public void applyUpgrade(String teamName, int level) {
        Upgrade upgradeData = getUpgrade(level);
        if (upgradeData == null) {
            Bukkit.getLogger().warning("Попытка применить несуществующее улучшение для уровня: " + level);
            return;
        }

        TeamUpgrade upgrade = upgradeData.getUpgrade();
        DatabaseManager databaseManager = plugin.getDatabaseManager();
        upgrade.apply(teamName);
        databaseManager.setTeamLevel(teamName, level);

        ChatColor teamColor = teamColors.getOrDefault(teamName, ChatColor.WHITE);

        String message = ChatColor.translateAlternateColorCodes('&',
                "Команда " + teamColor + teamName + "&f улучшила замок до &6" + level + "&f уровня!");
        Bukkit.broadcastMessage(message);
    }
    public Map<String, Boolean> getTeamSpecialUpgradeStatus() {
        return teamSpecialUpgradeStatus;
    }
}