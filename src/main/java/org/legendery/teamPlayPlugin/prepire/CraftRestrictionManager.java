package org.legendery.teamPlayPlugin.prepire;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.ItemStack;
import org.legendery.teamPlayPlugin.team.TeamManager;
import org.legendery.teamPlayPlugin.upgrades.UpgradeManager;
import org.legendery.teamPlayPlugin.manager.util.MessageCooldownManager;

import java.util.Set;

public class CraftRestrictionManager implements Listener {

    private final TeamManager teamManager;
    private final UpgradeManager upgradeManager;
    private final MessageCooldownManager cooldownManager = new MessageCooldownManager(2); // 2 секунды

    public CraftRestrictionManager(TeamManager teamManager, UpgradeManager upgradeManager) {
        this.teamManager = teamManager;
        this.upgradeManager = upgradeManager;
    }

    private static final Set<Material> IRON_ITEMS = Set.of(
            Material.IRON_INGOT, Material.IRON_BLOCK, Material.IRON_NUGGET,
            Material.IRON_SWORD, Material.IRON_PICKAXE, Material.IRON_AXE,
            Material.IRON_SHOVEL, Material.IRON_HOE, Material.IRON_HELMET,
            Material.IRON_CHESTPLATE, Material.IRON_LEGGINGS, Material.IRON_BOOTS,
            Material.ANVIL, Material.HOPPER, Material.CAULDRON,
            Material.IRON_BARS, Material.COMPASS, Material.HOPPER_MINECART,
            Material.FLINT_AND_STEEL, Material.MINECART, Material.BUCKET,
            Material.SHEARS, Material.IRON_TRAPDOOR, Material.IRON_DOOR, Material.HEAVY_WEIGHTED_PRESSURE_PLATE
    );

    private static final Set<Material> GOLD_ITEMS = Set.of(
            Material.GOLD_INGOT, Material.GOLD_BLOCK, Material.GOLD_NUGGET,Material.GOLDEN_CARROT,
            Material.GOLDEN_SWORD, Material.GOLDEN_PICKAXE, Material.GOLDEN_AXE,
            Material.GOLDEN_SHOVEL, Material.GOLDEN_HOE, Material.GOLDEN_HELMET,
            Material.GOLDEN_CHESTPLATE, Material.GOLDEN_LEGGINGS, Material.GOLDEN_BOOTS,
            Material.POWERED_RAIL, Material.CLOCK, Material.GOLDEN_APPLE, Material.ENCHANTED_GOLDEN_APPLE
    );

    private static final Set<Material> DIAMOND_ITEMS = Set.of(
            Material.DIAMOND, Material.DIAMOND_BLOCK,
            Material.DIAMOND_SWORD, Material.DIAMOND_PICKAXE, Material.DIAMOND_AXE,
            Material.DIAMOND_SHOVEL, Material.DIAMOND_HOE, Material.DIAMOND_HELMET,
            Material.DIAMOND_CHESTPLATE, Material.DIAMOND_LEGGINGS, Material.DIAMOND_BOOTS,
            Material.ENCHANTING_TABLE, Material.JUKEBOX
    );

    private static final Set<Material> COPPER_ITEMS = Set.of(
            Material.COPPER_INGOT, Material.COPPER_BLOCK, Material.CUT_COPPER,
            Material.CUT_COPPER_STAIRS, Material.CUT_COPPER_SLAB,
            Material.EXPOSED_COPPER, Material.OXIDIZED_COPPER, Material.WAXED_COPPER_BLOCK,
            Material.SPYGLASS, Material.LIGHTNING_ROD
    );

    @EventHandler
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        if (event.getView().getPlayer() instanceof Player player) {
            String teamName = teamManager.getPlayerTeam(player);
          //  if (teamName == null) return;

            int teamLevel = upgradeManager.getTeamLevel(teamName);
            ItemStack result = event.getRecipe() != null ? event.getRecipe().getResult() : null;
            if (result == null) return;

            Material itemType = result.getType();

            if (IRON_ITEMS.contains(itemType) && teamLevel < 4) {
                event.getInventory().setResult(null);
                if (cooldownManager.canSendMessage(player)) {
                    player.sendMessage(ChatColor.RED + "Ваша команда должна достичь 4 уровня для крафта железных предметов!");
                }} else if (GOLD_ITEMS.contains(itemType) && teamLevel < 10) {
                event.getInventory().setResult(null);
                if (cooldownManager.canSendMessage(player)) {
                player.sendMessage(ChatColor.RED + "Ваша команда должна достичь 10 уровня для крафта золотых предметов!");
            }} else if (DIAMOND_ITEMS.contains(itemType) && teamLevel < 6) {
                event.getInventory().setResult(null);
                if (cooldownManager.canSendMessage(player)) {
                    player.sendMessage(ChatColor.RED + "Ваша команда должна достичь 6 уровня для крафта алмазных предметов!");
                }} else if (COPPER_ITEMS.contains(itemType) && teamLevel < 3) {
                event.getInventory().setResult(null);
                if (cooldownManager.canSendMessage(player)) {
                player.sendMessage(ChatColor.RED + "Ваша команда должна достичь 3 уровня для крафта медных предметов!");
            }}
        }
    }
}