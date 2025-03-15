package org.legendery.teamPlayPlugin.team;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.legendery.teamPlayPlugin.DatabaseManager;
import org.legendery.teamPlayPlugin.TeamPlayPlugin;  // Импорт TeamPlayPlugin
import org.legendery.teamPlayPlugin.manager.util.MessageCooldownManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class TeamManager implements Listener {

    private final TeamPlayPlugin plugin; // Добавляем поле plugin
    private final DatabaseManager database;
    private final Scoreboard scoreboard;
    private final Map<String, Team> teams = new HashMap<>();
    private final String[] validTags = {"pvp", "pve", "cap"};
    private final MessageCooldownManager cooldownManager = new MessageCooldownManager(1); // 2 секунды

    public TeamManager(TeamPlayPlugin plugin, DatabaseManager database) {  // Изменяем конструктор
        this.plugin = plugin;
        this.database = database;
        this.scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        createTeams();
        Bukkit.getPluginManager().registerEvents(this, plugin); // Изменяем способ регистрации слушателя
    }

    private void createTeams() {
        teams.put("red", createTeam("Red", ChatColor.RED));
        teams.put("purple", createTeam("Purple", ChatColor.DARK_PURPLE));
        teams.put("gold", createTeam("Gold", ChatColor.GOLD));


        teams.put("silver", createTeam("Silver", ChatColor.GRAY));
        teams.put("green", createTeam("Green", ChatColor.DARK_GREEN));
        teams.put("aqua", createTeam("Aqua", ChatColor.DARK_AQUA));
    }


    private Team createTeam(String name, ChatColor color) {
        Team team = scoreboard.getTeam(name);
        if (team == null) {
            team = scoreboard.registerNewTeam(name);
        }
        team.setPrefix(color.toString());
        team.setColor(color);
        team.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.FOR_OWN_TEAM);
        return team;
    }
    private final Map<Player, String> playerTeams = new HashMap<>(); // <Player, TeamName>



    public void setPlayerTeam(Player player, String teamName) {
        playerTeams.put(player, teamName);
    }
    public void addPlayerToTeam(Player player, String teamName) {
        Team team = teams.get(teamName.toLowerCase());
        if (team != null) {
            removePlayerFromTeams(player);
            team.addEntry(player.getName());
            database.savePlayerTeam(player.getUniqueId().toString(), teamName);
            player.sendMessage(ChatColor.WHITE + "Вы вступили в команду " + team.getColor() + team.getName());
        }
    }

    private void removePlayerFromTeams(Player player) {
        for (Team team : scoreboard.getTeams()) {
            team.removeEntry(player.getName());
        }
        database.savePlayerTeam(player.getUniqueId().toString(), null);

    }

    public String getPlayerTeam(Player player) {
        String teamName = database.getPlayerTeam(player.getUniqueId().toString());
        return teamName;
    }

    // Методы для работы с тегами (pvp, pve, uni)
    public void setPlayerTag(Player player, String tag) {
        if (!Arrays.asList(validTags).contains(tag.toLowerCase())) {
            player.sendMessage(ChatColor.RED + "Неверный ярлык! Используйте: pvp, pve, cap");
            return;
        }
        database.savePlayerTag(player.getUniqueId().toString(), tag);
        // Также добавляем игрока в команду scoreboard для ярлыка
        addPlayerToTeam(player, tag); // Используем существующий метод для добавления в команду
        player.sendMessage(ChatColor.WHITE + "Установлен ярлык: " + tag);

    }
    public DatabaseManager getDatabase() {
        return database;
    }
    public String getPlayerTag(Player player) {
        return database.getPlayerTag(player.getUniqueId().toString());
    }
    public Team getTeam(String teamName) {
        return teams.get(teamName.toLowerCase());
    }
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {  // Изменяем получение плагина
            String teamName = getPlayerTeam(player);
            if (teamName != null) {
                addPlayerToTeam(player, teamName);
            }

            String playerTag = getPlayerTag(player);
            if (playerTag != null) {
                setPlayerTag(player, playerTag);
            }

        }, 5L); // Задержка в 5 тиков (примерно 0.25 секунды)
    }


    @EventHandler
    public void onPlayerDamage(EntityDamageByEntityEvent event) {
        // Проверяем, что и атакующий, и жертва — игроки
        if (event.getDamager() instanceof Player && event.getEntity() instanceof Player) {
            Player damager = (Player) event.getDamager();
            Player victim = (Player) event.getEntity();

            // Получаем команды атакующего и жертвы
            String damagerTeam = getPlayerTeam(damager);
            String victimTeam = getPlayerTeam(victim);

            // Проверяем, что обе команды существуют и равны
            if (damagerTeam != null && victimTeam != null && damagerTeam.equals(victimTeam)) {
                // Отменяем урон
                event.setCancelled(true);

                // Отправляем сообщение атакующему, если это необходимо
                if (cooldownManager.canSendMessage(damager)) {
                    damager.sendMessage(ChatColor.RED + "Вы не можете атаковать тимейта!");
                }
            }
        }
    }
    public String getTeamColor(String teamName) {
        Team team = teams.get(teamName.toLowerCase());
        if (team != null) {
            return team.getColor().toString();
        }
        return ChatColor.WHITE.toString();  // Если команда не найдена, возвращаем белый цвет.
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        // Проверка попадания снарядов (стрелы, трезубцы, яйца и т.п.)
        if (event.getEntity().getShooter() instanceof Player && event.getHitEntity() instanceof Player) {
            Player shooter = (Player) event.getEntity().getShooter();
            Player hitPlayer = (Player) event.getHitEntity();

            // Получаем команды атакующего и жертвы
            String shooterTeam = getPlayerTeam(shooter);
            String hitPlayerTeam = getPlayerTeam(hitPlayer);

            // Проверяем, что обе команды существуют и равны
            if (shooterTeam != null && hitPlayerTeam != null && shooterTeam.equals(hitPlayerTeam)) {
                // Отменяем попадание снаряда
                event.setCancelled(true);

                // Отправляем сообщение атакующему, если это необходимо
                if (cooldownManager.canSendMessage(shooter)) {
                    shooter.sendMessage(ChatColor.RED + "Вы не можете атаковать тимейта!");
                }
            }
        }
    }

    public String[] getValidTags() {
        return validTags;
    }

    public Set<String> getAllTeams() {
        Set<String> teams = new HashSet<>();
        String sql = "SELECT DISTINCT team FROM player_teams"; // Получаем уникальные названия команд
        try (Connection connection = database.getConnection(); // Используем метод для получения соединения
             PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                String teamName = rs.getString("team");
                teams.add(teamName);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            Bukkit.getLogger().severe("Ошибка при получении списка команд из базы данных!");
        }
        return teams;
    }
}