package org.legendery.teamPlayPlugin.team;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class TeamCommand implements CommandExecutor, TabCompleter {

    private final TeamManager teamManager;
    private final List<String> teamNames = Arrays.asList("red", "purple", "gold", "pvp", "pve", "cap",
            "silver", "green", "aqua");

    public TeamCommand(TeamManager teamManager) {
        this.teamManager = teamManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("team.admin")) {
            sender.sendMessage(ChatColor.RED + "У вас нет прав для использования этой команды!");
            return true;
        }

        if (args.length != 2) { // <---- ИЗМЕНЕНИЕ: Теперь нужно только 2 аргумента
            sender.sendMessage(ChatColor.YELLOW + "Использование: /teams <ник> <red/purple/gold/pvp/pve/cap...>"); // <---- ИЗМЕНЕНИЕ: Убрали упоминание settag
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Игрок не найден!");
            return true;
        }

        String teamName = args[1].toLowerCase();
        if (!teamNames.contains(teamName)) {
            sender.sendMessage(ChatColor.RED + "Неверное название команды! Используйте: red, purple, gold, pvp, pve, cap...");
            return true;
        }

        if (teamName.equalsIgnoreCase("pvp") || teamName.equalsIgnoreCase("pve") || teamName.equalsIgnoreCase("cap")) {
            teamManager.setPlayerTag(target, teamName);
            sender.sendMessage(ChatColor.WHITE + "Игроку " + target.getName() + " установлен тег " + teamName + "!");
            return true;
        }

        teamManager.addPlayerToTeam(target, teamName);
        sender.sendMessage(ChatColor.WHITE + "Игрок " + target.getName() + " добавлен в команду " + teamName + "!");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("team.admin")) return new ArrayList<>();

        if (args.length == 1) {
            // Возвращаем список онлайн игроков для автодополнения ника
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        } else if (args.length == 2) {
            // Возвращаем список команд (red, purple, gold, pvp, pve, uni)
            return teamNames.stream()
                    .filter(team -> team.startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }

        return new ArrayList<>();
    }
}