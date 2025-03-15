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

public class StoneCraftRestriction implements Listener {

    private final TeamManager teamManager;
    private final UpgradeManager upgradeManager;
    private final MessageCooldownManager cooldownManager = new MessageCooldownManager(2); // 2 секунды

    public StoneCraftRestriction(TeamManager teamManager, UpgradeManager upgradeManager) {
        this.teamManager = teamManager;
        this.upgradeManager = upgradeManager;
    }
    private static final Set<Material> LEVEL_16_RESTRICTED_ITEMS = Set.of(
            Material.BEACON, // Маяк
           // Material.CRAFTER,
          //  Material.MACE,
            Material.SHULKER_BOX,
            Material.END_CRYSTAL,
            Material.EXPERIENCE_BOTTLE,
            Material.RESPAWN_ANCHOR // Якорь возрождения
    );
    private static final Set<Material> STONE_ITEMS = Set.of(
            Material.STONE_SWORD, Material.STONE_AXE, Material.STONE_PICKAXE,
            Material.STONE_SHOVEL, Material.STONE_HOE, Material.FURNACE
    );
    private static final Set<Material> SMITHING_TEMPLATES = Set.of(
            Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE, Material.SENTRY_ARMOR_TRIM_SMITHING_TEMPLATE,
            Material.DUNE_ARMOR_TRIM_SMITHING_TEMPLATE, Material.COAST_ARMOR_TRIM_SMITHING_TEMPLATE,
            Material.WILD_ARMOR_TRIM_SMITHING_TEMPLATE, Material.WARD_ARMOR_TRIM_SMITHING_TEMPLATE,
            Material.VEX_ARMOR_TRIM_SMITHING_TEMPLATE, Material.TIDE_ARMOR_TRIM_SMITHING_TEMPLATE,
            Material.SNOUT_ARMOR_TRIM_SMITHING_TEMPLATE, Material.RAISER_ARMOR_TRIM_SMITHING_TEMPLATE,
            //Material.BOLT_ARMOR_TRIM_SMITHING_TEMPLATE,
            //Material.FLOW_ARMOR_TRIM_SMITHING_TEMPLATE,
            Material.HOST_ARMOR_TRIM_SMITHING_TEMPLATE, Material.EYE_ARMOR_TRIM_SMITHING_TEMPLATE,
            Material.RIB_ARMOR_TRIM_SMITHING_TEMPLATE, Material.SHAPER_ARMOR_TRIM_SMITHING_TEMPLATE,
            Material.WAYFINDER_ARMOR_TRIM_SMITHING_TEMPLATE, Material.SILENCE_ARMOR_TRIM_SMITHING_TEMPLATE,
            Material.SPIRE_ARMOR_TRIM_SMITHING_TEMPLATE
    );

    @EventHandler
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        if (event.getView().getPlayer() instanceof Player player) {
            String teamName = teamManager.getPlayerTeam(player);
            //if (teamName == null) return;

            int teamLevel = upgradeManager.getTeamLevel(teamName);
            ItemStack result = event.getRecipe() != null ? event.getRecipe().getResult() : null;
            if (result == null) return;

            Material itemType = result.getType();

            if (STONE_ITEMS.contains(itemType) && teamLevel < 2) {
                event.getInventory().setResult(null);
                if (cooldownManager.canSendMessage(player)) {
                    player.sendMessage(ChatColor.RED + "Ваша команда должна достичь 2 уровня для крафта каменных предметов!");
                }
            }

            if (SMITHING_TEMPLATES.contains(itemType) && teamLevel < 14) {
                event.getInventory().setResult(null);
                if (cooldownManager.canSendMessage(player)) {
                player.sendMessage(ChatColor.RED + "Ваша команда должна достичь 14 уровня для крафта кузнечных шаблонов!");
            }}
            if (LEVEL_16_RESTRICTED_ITEMS.contains(itemType) && teamLevel < 16) {
                event.getInventory().setResult(null);
                if (cooldownManager.canSendMessage(player)) {
                player.sendMessage(ChatColor.RED + "Ваша команда должна достичь 16 уровня для крафта этого предмета!");
            }}
        }
    }
}