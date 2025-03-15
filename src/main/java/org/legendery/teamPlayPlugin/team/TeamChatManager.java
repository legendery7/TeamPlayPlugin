package org.legendery.teamPlayPlugin.team;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.scoreboard.Team;

public class TeamChatManager implements Listener {

    private final TeamManager teamManager;

    public TeamChatManager(TeamManager teamManager) {
        this.teamManager = teamManager;
        Bukkit.getPluginManager().registerEvents(this, Bukkit.getPluginManager().getPlugin("TeamPlayPlugin"));
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();

        if (player.getGameMode() == GameMode.SPECTATOR) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "Вы не можете писать в чат!");
            return;
        }

        String message = event.getMessage();
        String teamName = teamManager.getPlayerTeam(player);
        String tag = teamManager.getPlayerTag(player);
        ChatColor teamColor = ChatColor.WHITE;

        if (teamName != null) {
            Team team = teamManager.getTeam(teamName);
            if (team != null) {
                teamColor = team.getColor();
            }
        }

        if (message.startsWith("!")) {
            // Глобальное сообщение
            String globalPrefix = ChatColor.GRAY + "[";

            if (teamName != null) {
                globalPrefix += teamColor + teamName.toUpperCase() + ChatColor.GRAY;
            } else {
                globalPrefix += ChatColor.WHITE + "GLOBAL" + ChatColor.GRAY;
            }

            globalPrefix += "] ";
            message = ChatColor.WHITE + message.substring(1).trim();
            event.setMessage(message);

            // Если ярлык Cap — ник окрашивается в цвет команды
            ChatColor nameColor = (tag != null && tag.equalsIgnoreCase("cap")) ? teamColor : ChatColor.GRAY;
            event.setFormat(globalPrefix + nameColor + player.getDisplayName() + ChatColor.WHITE + ": %2$s");

        } else if (teamName != null) {
            // Командное сообщение
            event.setCancelled(true);
            String teamMessage;

            if (tag != null) {
                teamMessage = ChatColor.GRAY + "TC " +ChatColor.GRAY + "[" + getTagColor(tag) + getTagDisplay(tag) + ChatColor.GRAY + "] "
                        + teamColor + player.getDisplayName() + ": " + ChatColor.WHITE + message;
            } else {
                Team team = teamManager.getTeam(teamName);
                if (team != null) {
                    teamMessage = ChatColor.GRAY + "TC " +ChatColor.GRAY + "[" + team.getColor() + teamName.toUpperCase() + ChatColor.GRAY + "] "
                            + teamColor + player.getDisplayName() + ": " + ChatColor.WHITE + message;
                } else {
                    teamMessage = ChatColor.RED + "[ERROR] " + player.getDisplayName() + ": " + ChatColor.WHITE + message;
                }
            }

            for (Player teamMember : Bukkit.getOnlinePlayers()) {
                if (teamName.equals(teamManager.getPlayerTeam(teamMember))) {
                    teamMember.sendMessage(teamMessage);
                }
            }
        }
    }


    private ChatColor getTagColor(String tag) {
        switch (tag.toLowerCase()) {
            case "pve":
                return ChatColor.DARK_GREEN;
            case "pvp":
                return ChatColor.RED;
            case "cap":
                return ChatColor.GOLD;
            default:
                return ChatColor.WHITE;
        }
    }

    private String getTagDisplay(String tag) {
        if (tag != null) {
            switch (tag.toLowerCase()) {
                case "pve":
                    return "PvE";
                case "pvp":
                    return "PvP";
                case "cap":
                    return "Cap";
                default:
                    return "";
            }
        }
        return "";
    }
}