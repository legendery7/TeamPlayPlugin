package org.legendery.teamPlayPlugin.zapret;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.legendery.teamPlayPlugin.TeamPlayPlugin;
import org.legendery.teamPlayPlugin.constructor.TeamUpgrade;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class TeamEffectUpgrade extends TeamUpgrade {

    private final PotionEffectType effectType;
    private final int effectLevel;
    private final int effectDuration; // in ticks
    private final int checkInterval; // in ticks

    public TeamEffectUpgrade(int level, String description, PotionEffectType effectType, int effectLevel, int effectDuration, int checkInterval) {
        super(level, description);
        this.effectType = effectType;
        this.effectLevel = effectLevel;
        this.effectDuration = effectDuration;
        this.checkInterval = checkInterval;
    }

    @Override
    public void apply(String teamName) {
        TeamPlayPlugin plugin = (TeamPlayPlugin) Bukkit.getPluginManager().getPlugin("TeamPlayPlugin");
        if (plugin == null) {
            Bukkit.getLogger().warning("TeamPlayPlugin not found!");
            return;
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                Set<UUID> appliedPlayers = new HashSet<>();

                for (Player player : Bukkit.getOnlinePlayers()) {
                    String team = plugin.getDatabaseManager().getPlayerTeam(player.getUniqueId().toString());
                    if (team != null && team.equals(teamName)) {
                        PotionEffect existingEffect = player.getPotionEffect(effectType);

                        // Проверяем наличие эффекта и его уровень
                        if (existingEffect == null || existingEffect.getAmplifier() < effectLevel) {
                            // Снимаем эффект, если он есть и его уровень меньше
                            if (existingEffect != null) {
                                player.removePotionEffect(effectType);
                            }

                            // Применяем новый эффект
                            player.addPotionEffect(new PotionEffect(effectType, effectDuration, effectLevel, false, false));
                            appliedPlayers.add(player.getUniqueId());
                        }
                    }
                }

                if (plugin.getConfig().getBoolean("debug", false)) {
                    Bukkit.getLogger().info("Applied " + effectType.getName() + " " + effectLevel + " to team " + teamName + " players: " + appliedPlayers.size());
                }
            }
        }.runTaskTimer(plugin, 0L, checkInterval);
    }
}