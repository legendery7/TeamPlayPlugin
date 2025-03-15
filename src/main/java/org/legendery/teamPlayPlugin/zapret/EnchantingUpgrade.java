package org.legendery.teamPlayPlugin.zapret;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.legendery.teamPlayPlugin.constructor.TeamUpgrade;
import org.legendery.teamPlayPlugin.team.TeamManager;
import org.bukkit.ChatColor;
import org.legendery.teamPlayPlugin.upgrades.UpgradeManager;
import org.legendery.teamPlayPlugin.manager.util.MessageCooldownManager;

public class EnchantingUpgrade extends TeamUpgrade implements Listener {

    private final MessageCooldownManager cooldownManager = new MessageCooldownManager(2); // 2 секунды
    private final TeamManager teamManager;
    private final UpgradeManager upgradeManager;

    public EnchantingUpgrade(TeamManager teamManager,int level, String description, UpgradeManager upgradeManager) {
        super(level, description);
        this.teamManager = teamManager;
        this.upgradeManager = upgradeManager;
    }

    @Override
    public void apply(String teamName) {
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (event.getInventory().getType() == InventoryType.ENCHANTING) {
            Player player = (Player) event.getPlayer();
            String teamName = teamManager.getPlayerTeam(player);
            if (teamName == null) return;
            int teamLevel = upgradeManager.getTeamLevel(teamName);
            if (teamLevel < 7) {
                event.setCancelled(true);
                if (cooldownManager.canSendMessage(player)) {
                    player.sendMessage(ChatColor.RED + "Ваша команда не достигла необходимого уровня для использования стола зачарований.");
                }
            }
        }
    }
}