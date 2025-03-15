package org.legendery.teamPlayPlugin;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.*;
import java.util.*;

public class DatabaseManager {
    private final String databasePath;
    private volatile Connection connection;
    private final JavaPlugin plugin;
    private final Object connectionLock = new Object();

    public DatabaseManager(TeamPlayPlugin plugin) {
        this.plugin = plugin;
        databasePath = new File(plugin.getDataFolder(), "teams.db").getAbsolutePath();
        connect();
        createTables();
    }

    private void connect() {
        synchronized (connectionLock) {
            try {
                Class.forName("org.sqlite.JDBC");
                connection = DriverManager.getConnection("jdbc:sqlite:" + databasePath);
                Bukkit.getLogger().info("[TeamPlayPlugin] Подключено к SQLite!");
            } catch (Exception e) {
                Bukkit.getLogger().severe("[TeamPlayPlugin] Ошибка подключения к SQLite!");
                e.printStackTrace();
            }
        }
    }

    private void createTables() {
        createTeamTable();
        createTagTable();
        createDeadPlayerTable();
        createTeamUpgradesTable();
        createTeamPointsTable();
        createGlobalUpgradesTable();
    }

    private void createTeamTable() {
        String sql = "CREATE TABLE IF NOT EXISTS player_teams (" +
                "uuid TEXT PRIMARY KEY, " +
                "team TEXT NOT NULL)";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void createTagTable() {
        String sql = "CREATE TABLE IF NOT EXISTS player_tags (" +
                "uuid TEXT PRIMARY KEY, " +
                "tag TEXT NOT NULL)";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void createDeadPlayerTable() {
        String sql = "CREATE TABLE IF NOT EXISTS dead_players (" +
                "uuid TEXT PRIMARY KEY)";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void createTeamUpgradesTable() {
        String sql = "CREATE TABLE IF NOT EXISTS team_upgrades (" +
                "team_name TEXT PRIMARY KEY, " +
                "level INTEGER NOT NULL DEFAULT 1)";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void createGlobalUpgradesTable() {
        String sql = "CREATE TABLE IF NOT EXISTS global_upgrades (" +
                "upgrade_name TEXT PRIMARY KEY, " +
                "team_name TEXT, " +
                "is_active INTEGER NOT NULL DEFAULT 0)";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        insertUpgradeIfNotExist("totem");
        insertUpgradeIfNotExist("resistance");
        insertUpgradeIfNotExist("invisibility");
    }

    private void createTeamPointsTable() {
        String sql = "CREATE TABLE IF NOT EXISTS team_points (" +
                "team_name TEXT NOT NULL, " +
                "point_name TEXT NOT NULL, " +
                "x INTEGER, y INTEGER, z INTEGER, world TEXT, " +
                "PRIMARY KEY (team_name, point_name))";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void insertUpgradeIfNotExist(String upgradeName) {
        String checkSql = "SELECT upgrade_name FROM global_upgrades WHERE upgrade_name = ?";
        String insertSql = "INSERT INTO global_upgrades (upgrade_name, team_name, is_active) SELECT ?, NULL, 0 WHERE NOT EXISTS (SELECT 1 FROM global_upgrades WHERE upgrade_name = ?)";
        try (Connection conn = getConnection();
             PreparedStatement checkStmt = conn.prepareStatement(checkSql);
             PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {

            checkStmt.setString(1, upgradeName);
            insertStmt.setString(1, upgradeName);
            insertStmt.setString(2, upgradeName);

            insertStmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            plugin.getLogger().severe("Ошибка при добавлении апгрейда " + upgradeName + " в базу данных!");
        }
    }

    public void savePlayerTeam(String uuid, String team) {
        String sql = "REPLACE INTO player_teams (uuid, team) VALUES (?, ?)";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setString(1, uuid);
            stmt.setString(2, team);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public String getPlayerTeam(String uuid) {
        String sql = "SELECT team FROM player_teams WHERE uuid = ?";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setString(1, uuid);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getString("team");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void savePlayerTag(String uuid, String tag) {
        String sql = "REPLACE INTO player_tags (uuid, tag) VALUES (?, ?)";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setString(1, uuid);
            stmt.setString(2, tag);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public String getPlayerTag(String uuid) {
        String sql = "SELECT tag FROM player_tags WHERE uuid = ?";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setString(1, uuid);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getString("tag");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void saveDeadPlayer(String uuid) {
        String sql = "INSERT OR REPLACE INTO dead_players (uuid) VALUES (?)";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setString(1, uuid);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void removeDeadPlayer(String uuid) {
        String sql = "DELETE FROM dead_players WHERE uuid = ?";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setString(1, uuid);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<String> getDeadPlayers() {
        List<String> deadPlayers = new ArrayList<>();
        String sql = "SELECT uuid FROM dead_players";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                deadPlayers.add(rs.getString("uuid"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return deadPlayers;
    }

    public int getTeamLevel(String teamName) {
        String sql = "SELECT level FROM team_upgrades WHERE team_name = ?";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setString(1, teamName);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("level");
            } else {
                createTeamEntry(teamName);
                return 1;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return 1;
        }
    }

    public void setTeamLevel(String teamName, int level) {
        String sql = "UPDATE team_upgrades SET level = ? WHERE team_name = ?";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setInt(1, level);
            stmt.setString(2, teamName);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void createTeamEntry(String teamName) {
        String sql = "INSERT INTO team_upgrades (team_name, level) VALUES (?, ?)";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setString(1, teamName);
            stmt.setInt(2, 1);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public boolean hasTeamUpgrade(String upgradeName) {
        String sql = "SELECT team_name FROM global_upgrades WHERE upgrade_name = ? AND team_name IS NOT NULL";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setString(1, upgradeName);
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public String getTeamForUpgrade(String upgradeName) {
        String sql = "SELECT team_name FROM global_upgrades WHERE upgrade_name = ?";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setString(1, upgradeName);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getString("team_name");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void setTeamUpgrade(String upgradeName, String teamName) {
        String sql = "UPDATE global_upgrades SET team_name = ? WHERE upgrade_name = ?";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setString(1, teamName);
            stmt.setString(2, upgradeName);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Map<String, Location> getTeamPoints(String teamName) {
        Map<String, Location> points = new HashMap<>();
        String sql = "SELECT point_name, x, y, z, world FROM team_points WHERE team_name = ?";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setString(1, teamName);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                String name = rs.getString("point_name");
                int x = rs.getInt("x");
                int y = rs.getInt("y");
                int z = rs.getInt("z");
                String world = rs.getString("world");
                Location location = new Location(Bukkit.getWorld(world), x, y, z);
                points.put(name, location);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return points;
    }

    public boolean addTeamPoint(String teamName, String pointName, Location location) {
        if (getTeamPointCount(teamName) >= 9) return false;

        String sql = "INSERT INTO team_points (team_name, point_name, x, y, z, world) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setString(1, teamName);
            stmt.setString(2, pointName);
            stmt.setInt(3, location.getBlockX());
            stmt.setInt(4, location.getBlockY());
            stmt.setInt(5, location.getBlockZ());
            stmt.setString(6, location.getWorld().getName());
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean removeTeamPoint(String teamName, String pointName) {
        String sql = "DELETE FROM team_points WHERE team_name = ? AND point_name = ?";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setString(1, teamName);
            stmt.setString(2, pointName);
            int rows = stmt.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public int getTeamPointCount(String teamName) {
        String sql = "SELECT COUNT(*) FROM team_points WHERE team_name = ?";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setString(1, teamName);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public List<String> getTeamMembers(String teamName) {
        List<String> members = new ArrayList<>();
        String sql = "SELECT uuid FROM player_teams WHERE team = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, teamName);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                String uuidString = rs.getString("uuid");
                if (uuidString != null && !uuidString.isEmpty()) {
                    members.add(uuidString);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            plugin.getLogger().severe("Ошибка при получении списка участников команды из базы данных!");
        }
        return members;
    }

    public Connection getConnection() {
        synchronized (connectionLock) {
            try {
                if (connection == null || connection.isClosed()) {
                    connect();
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Не удалось проверить или переподключиться к базе данных: " + e.getMessage());
                e.printStackTrace();
                return null;
            }
            return connection;
        }
    }

    public void close() {
        synchronized (connectionLock) {
            try {
                if (connection != null && !connection.isClosed()) {
                    connection.close();
                    Bukkit.getLogger().info("[TeamPlayPlugin] Соединение с SQLite закрыто!");
                }
            } catch (SQLException e) {
                Bukkit.getLogger().severe("Ошибка закрытия соединения с SQLite!");
                e.printStackTrace();
            } finally {
                connection = null;
            }
        }
    }
}