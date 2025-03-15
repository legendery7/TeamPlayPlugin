package org.legendery.teamPlayPlugin.util;

import org.bukkit.Material;

public class ItemNameTranslator {

    public static String translateMaterialName(Material material) {
        switch (material) {
            case WOODEN_PICKAXE:
                return "Деревянная кирка";
            case OAK_LOG:
                return "Дубовое бревно";
            case COBBLESTONE:
                return "Булыжник";
            case COAL:
                return "Уголь";
            case WHEAT_SEEDS:
                return "Семена пшеницы";
            case SUGAR:
                return "Сахар";
            case DIAMOND:
                return "Алмаз";
            case IRON_BLOCK:
                return "Железный блок";
            case GLOWSTONE_DUST:
                return "Светопыль";
            case EMERALD:
                return "Изумруд";
            case RAW_COPPER:
                return "Рудной меди";
            case STONE:
                return "Камень";
            case CHARCOAL:
                return "Древесный уголь";
            case IRON_INGOT:
                return "Железный слиток";
            case COPPER_INGOT:
                return "Медный слиток";
            case WHITE_WOOL:
                return "Белая шерсть";
            case SAND:
                return "Песок";
            case COD:
                return "Треска";
            case SALMON:
                return "Лосось";
            case COOKED_BEEF:
                return "Стейк";
            case GOLD_INGOT:
                return "Золотой слиток";
            case REDSTONE:
                return "Редстоун";
            case LAPIS_BLOCK:
                return "Блок лазурита";
            case OBSIDIAN:
                return "Обсидиан";
            case BOOK:
                return "Книга";
            case HONEYCOMB:
                return "Медовые соты";
            case BLAZE_POWDER:
                return "Огненный порошок";
            case MAGMA_CREAM:
                return "Сгусток магмы";
            case NETHER_WART:
                return "Незерский нарост";
            case NETHERITE_INGOT:
                return "Незеритовый слиток";
            case GHAST_TEAR:
                return "Слеза гаста";
            case ENCHANTED_GOLDEN_APPLE:
                return "Зачарованное золотое яблоко";
            case BEACON:
                return "Маяк";
            case NETHER_STAR:
                return "Звезда незера";
            case RESPAWN_ANCHOR:
                return "Якорь возрождения";
            case END_CRYSTAL:
                return "Кристалл Края";
            case SMOOTH_STONE:
                return "Гладкий камень";
            case HAY_BLOCK:
                return "Блок сена";
            case COPPER_BLOCK:
                return "Медный блок";
            case HONEY_BOTTLE:
                return "Бутылка меда";
            case REDSTONE_BLOCK:
                return "Блок редстоуна";
            case NAUTILUS_SHELL:
                return "Раковина наутилуса";
            case PUFFERFISH:
                return "Иглобрюх";
            case DIAMOND_BLOCK:
                return "Алмазный блок";
            case GOLDEN_CARROT:
                return "Золотая морковь";
            case NETHERITE_UPGRADE_SMITHING_TEMPLATE:
                return "Шаблон улучшения незерита";
            case CALCITE:
                return "Кальцит";
            case DRIED_KELP_BLOCK:
                return "Блок сушеной ламинарии";
            case CACTUS:
                return "Кактус";
            case BAMBOO:
                return "Бамбук";
            case HONEY_BLOCK:
                return "Медовый блок";
            case HONEYCOMB_BLOCK:
                return "Блок медовых сот";
            case TURTLE_HELMET:
                return "Черепаший шлем";
            case EMERALD_BLOCK:
                return "Изумрудный блок";
            case TOTEM_OF_UNDYING:
                return "Тотем бессмертия";
            case END_ROD:
                return "Стержень Края";
            case TNT:
                return "Динамит";
            case SPONGE:
                return "Губка";
            case GUNPOWDER:
                return "Порох";
            case CARROT:
                return "Морковь";
            case APPLE:
                return "Яблоко";
            case GLISTERING_MELON_SLICE:
                return "Сверкающий ломтик арбуза";
            case FERMENTED_SPIDER_EYE:
                return "Маринованный паучий глаз";
            case COOKED_PORKCHOP:
                return "Жареная свинина";
            case COOKED_CHICKEN:
                return "Жареная курятина";
            case BONE:
                return "Кость";
            case ROTTEN_FLESH:
                return "Гнилая плоть";
            case POTATO:
                return "Картофель";
            case SWEET_BERRIES:
                return "Сладкие ягоды";
            case POINTED_DRIPSTONE:
                return "Капельник";
            default:
                return material.name();
        }
    }
    public static Material getMaterialForLevel(int level) {
        switch (level) {
            case 1:
                return Material.FIRE;
            case 2:
                return Material.COBBLESTONE;
            case 3:
                return Material.RAW_COPPER;
            case 4:
                return Material.RAW_IRON;
            case 5:
                return Material.SUGAR;
            case 6:
                return Material.DIAMOND;
            case 7:
                return Material.ENCHANTING_TABLE;
            case 8:
                return Material.PUFFERFISH;
            case 9:
                return Material.ANVIL;
            case 10:
                return Material.GOLD_INGOT;
            case 11:
                return Material.MAGMA_CREAM;
            case 12:
                return Material.NETHERITE_INGOT;
            case 13:
                return Material.BLAZE_POWDER;
            case 14:
                return Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE;
            case 15:
                return Material.GOLDEN_APPLE;
            case 16:
                return Material.BEACON;
            default:
                return Material.EMERALD_BLOCK; // Default
        }
    }
}