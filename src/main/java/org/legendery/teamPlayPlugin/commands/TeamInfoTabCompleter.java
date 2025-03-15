package org.legendery.teamPlayPlugin.commands;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.legendery.teamPlayPlugin.DatabaseManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TeamInfoTabCompleter implements TabCompleter {
    private final DatabaseManager databaseManager;

    public TeamInfoTabCompleter(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player)) return null;
        Player player = (Player) sender;
        String teamName = databaseManager.getPlayerTeam(player.getUniqueId().toString());
        if (teamName == null) return null;

        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.add("set");
            completions.add("del");
            completions.add("list");
            completions.add("tp");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("del")) {
            Map<String, Location> points = databaseManager.getTeamPoints(teamName);
            completions.addAll(points.keySet());
        }

        return completions;
    }
}
