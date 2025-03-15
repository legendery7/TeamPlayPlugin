package org.legendery.teamPlayPlugin.zapret;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachment;
import org.legendery.teamPlayPlugin.TeamPlayPlugin;  // Add this import
import org.legendery.teamPlayPlugin.constructor.TeamUpgrade;

import java.util.HashMap;
import java.util.Map;

public class PermissionUpgrade extends TeamUpgrade {
    private final String permission;
    private final Map<Player, PermissionAttachment> attachments = new HashMap<>();

    public PermissionUpgrade(int level, String description, String permission) {
        super(level, description);
        this.permission = permission;
    }

    @Override
    public void apply(String teamName) {
        TeamPlayPlugin plugin = (TeamPlayPlugin) Bukkit.getPluginManager().getPlugin("TeamPlayPlugin");
        if (plugin != null) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                String team = plugin.getDatabaseManager().getPlayerTeam(p.getUniqueId().toString());
                if (team != null && team.equals(teamName)) {
                    applyToPlayer(p, plugin); // Use applyToPlayer method
                }
            }
        }
    }

    private void applyToPlayer(Player player, TeamPlayPlugin plugin) {
        removeAttachment(player);
        PermissionAttachment attachment = player.addAttachment(plugin);
        attachment.setPermission(permission, true);
        attachments.put(player, attachment);
    }

    private void removeAttachment(Player player) {
        if (attachments.containsKey(player)) {
            PermissionAttachment attachment = attachments.get(player);
            if(attachment != null) {
                try {
                    player.removeAttachment(attachment);
                } catch (IllegalArgumentException e) {
                    Bukkit.getLogger().warning("Попытка удалить несуществующий PermissionAttachment у игрока: " + player.getName());
                }
                attachments.remove(player);
            }

        }
    }

    public String getPermission() {
        return permission;
    }
}