package org.legendery.teamPlayPlugin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffectType;
import org.legendery.teamPlayPlugin.commands.CompassCommand;
import org.legendery.teamPlayPlugin.commands.TeamInfoCommand;
import org.legendery.teamPlayPlugin.commands.TeamInfoTabCompleter;
import org.legendery.teamPlayPlugin.manager.*;
import org.legendery.teamPlayPlugin.prepire.CraftRestrictionManager;
import org.legendery.teamPlayPlugin.prepire.StoneCraftRestriction;
import org.legendery.teamPlayPlugin.team.*;
import org.legendery.teamPlayPlugin.upgrades.*;
import org.legendery.teamPlayPlugin.zapret.AnvilUpgrade;
import org.legendery.teamPlayPlugin.zapret.EnchantingUpgrade;
import org.legendery.teamPlayPlugin.zapret.SmithingTableUpgrade;
import org.legendery.teamPlayPlugin.zapret.TeamEffectUpgrade;

public final class TeamPlayPlugin extends JavaPlugin implements Listener {

    private DatabaseManager databaseManager;
    private TeamManager teamManager;
    private TeamChatManager teamChatManager;
    private UpgradeManager upgradeManager;
    private SpectatorJoinQuitHandler spectatorHandler;
    private TeamMagnetBlockManager magnetBlockManager;
    private MagnetBlockDistributor magnetBlockDistributor;
    private PvPManager pvpManager;
    private DeathToggleManager deathToggleManager;
    private ResurrectionManager resurrectionManager;
    private TeamProtectionManager teamProtectionManager;
    private BonusUpgradesManager bonusUpgradesManager;


    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.databaseManager = new DatabaseManager(this);
        teamManager = new TeamManager(this, databaseManager);
        upgradeManager = new UpgradeManager(this);

        getCommand("teams").setExecutor(new TeamCommand(teamManager));
        getCommand("tm").setExecutor(new TeamInfoCommand(this, teamManager, databaseManager));
        getCommand("tm").setTabCompleter(new TeamInfoTabCompleter(databaseManager));

        magnetBlockManager = new TeamMagnetBlockManager(this, teamManager, upgradeManager,null); // Инициализируйте здесь с null, чтобы избежать зацикливания
        bonusUpgradesManager = new BonusUpgradesManager(this, teamManager, magnetBlockManager); //Перенесите инициализацию сюда
        magnetBlockManager.setBonusUpgradesManager(bonusUpgradesManager); //Затем установите ссылку друг на друга

        teamChatManager = new TeamChatManager(teamManager);

        spectatorHandler = new SpectatorJoinQuitHandler(this);

        // В onEnable() методе TeamPlayPlugin:
        CompassCommand compassCommand = new CompassCommand(this);
        getCommand("compass").setExecutor(compassCommand);

        EnemyCompass enemyCompass = new EnemyCompass(this, teamManager, compassCommand);
        enemyCompass.runTaskTimer(this, 0L, 20L); // Обновление каждые 20 тиков (1 секунда)


        getConfig().options().copyDefaults(true);
        saveConfig();
        GoatHornListener goatHornListener = new GoatHornListener(teamManager);
        Bukkit.getPluginManager().registerEvents(goatHornListener, this);
        String anchorMaterial = getConfig().getString("resurrection_anchor.material", "ANVIL");
        String anchorName = getConfig().getString("resurrection_anchor.name", "Якорь возрождения");
        CraftRestrictionManager craftRestrictionManager = new CraftRestrictionManager(teamManager, upgradeManager);
        Bukkit.getPluginManager().registerEvents(craftRestrictionManager, this);
        Bukkit.getPluginManager().registerEvents(new StoneCraftRestriction(teamManager, upgradeManager), this);
        Bukkit.getPluginManager().registerEvents(new CustomGoldenChestplate(), this);

        teamProtectionManager = new TeamProtectionManager(this, teamManager);
        deathToggleManager = new DeathToggleManager(this);
        resurrectionManager = new ResurrectionManager(this, teamManager, anchorMaterial, anchorName, spectatorHandler, deathToggleManager);
        Bukkit.getPluginManager().registerEvents(new DeathMessageSuppressor(deathToggleManager), this);
        Bukkit.getPluginManager().registerEvents(resurrectionManager, this);

        getServer().getPluginManager().registerEvents(new SmithingTableUpgrade(teamManager, 12, "UpgradeName", upgradeManager), this);

        magnetBlockDistributor = new MagnetBlockDistributor(this, teamManager, magnetBlockManager);

        Bukkit.getPluginManager().registerEvents(teamProtectionManager, this);
        pvpManager = new PvPManager(this);



        getCommand("magnetblock").setExecutor((sender, command, label, args) -> {
            if (!sender.hasPermission("team.magnetblock.giveall")) {
                sender.sendMessage(ChatColor.RED + "У вас нет прав для этой команды.");
                return true;
            }

            if (sender instanceof Player) {
                Player player = (Player) sender;
                if (!player.hasPermission("team.magnetblock")) {
                    player.sendMessage(ChatColor.RED + "У вас нет прав для этой команды.");
                    return true;
                }

                magnetBlockDistributor.giveMagnetBlockToUniPlayers(sender);
                return true;
            } else {
                magnetBlockDistributor.giveMagnetBlockToUniPlayers(sender);
                sender.sendMessage(ChatColor.GREEN + "Магнитные блоки выданы всем uni игрокам.");
                return true;
            }
        });

        EnchantingUpgrade enchantingUpgrade = new EnchantingUpgrade(teamManager, 7, "UpgradeName", upgradeManager);
        Bukkit.getPluginManager().registerEvents(enchantingUpgrade, this);
        AnvilUpgrade anvilUpgrade = new AnvilUpgrade(teamManager, 9, "UpgradeName", upgradeManager);

        Bukkit.getPluginManager().registerEvents(anvilUpgrade, this);
        getCommand("pvp").setExecutor(pvpManager);
        Bukkit.getPluginManager().registerEvents(pvpManager, this);
        getCommand("death").setExecutor(deathToggleManager);
        restartEffectTasks();
    }


    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public TeamProtectionManager getTeamProtectionManager() {
        return teamProtectionManager;
    }
    @Override
    public void onDisable() {
        databaseManager.close();
        getLogger().info("TeamPlayPlugin отключен.");
    }

    private void restartEffectTasks() {
        for (String teamName : teamManager.getAllTeams()) {
            int teamLevel = databaseManager.getTeamLevel(teamName);
            if (teamLevel >= 15) {
                applyEffectTask(teamName, PotionEffectType.SPEED, 0, 200, 200);
                applyEffectTask(teamName, PotionEffectType.WATER_BREATHING, 0, 200, 200);
                applyEffectTask(teamName, PotionEffectType.FIRE_RESISTANCE, 0, 200, 200);
                applyEffectTask(teamName, PotionEffectType.INCREASE_DAMAGE, 0, 200, 200);
                applyEffectTask(teamName, PotionEffectType.REGENERATION, 0, 200, 200);
                continue;
            }
            if (teamLevel >= 13) {
                applyEffectTask(teamName, PotionEffectType.SPEED, 0, 200, 200);
                applyEffectTask(teamName, PotionEffectType.WATER_BREATHING, 0, 200, 200);
                applyEffectTask(teamName, PotionEffectType.FIRE_RESISTANCE, 0, 200, 200);
                applyEffectTask(teamName, PotionEffectType.INCREASE_DAMAGE, 0, 200, 200);
                continue;
            }
            if (teamLevel >= 11) {
                applyEffectTask(teamName, PotionEffectType.SPEED, 0, 200, 200);
                applyEffectTask(teamName, PotionEffectType.WATER_BREATHING, 0, 200, 200);
                applyEffectTask(teamName, PotionEffectType.FIRE_RESISTANCE, 0, 200, 200);
                continue;
            }

            if (teamLevel >= 8) {
                applyEffectTask(teamName, PotionEffectType.SPEED, 0, 200, 200);
                applyEffectTask(teamName, PotionEffectType.WATER_BREATHING, 0, 200, 200);
                continue;
            }
            if (teamLevel >= 5) {
                applyEffectTask(teamName, PotionEffectType.SPEED, 0, 200, 200);
                continue;
            }
        }
    }

    private void applyEffectTask(String teamName, PotionEffectType effectType, int effectLevel, int effectDuration, int checkInterval) {
        new TeamEffectUpgrade(0, "Перезапуск эффекта", effectType, effectLevel, effectDuration, checkInterval).apply(teamName);
    }
    public TeamManager getTeamManager() {
        return teamManager;
    }

    public TeamMagnetBlockManager getMagnetBlockManager() {
        return magnetBlockManager;
    }
}