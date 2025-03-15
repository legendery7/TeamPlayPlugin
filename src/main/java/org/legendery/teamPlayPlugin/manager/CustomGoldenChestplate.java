package org.legendery.teamPlayPlugin.manager;

import org.bukkit.*;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import org.legendery.teamPlayPlugin.TeamPlayPlugin;
import org.legendery.teamPlayPlugin.team.TeamManager;

import java.util.Random;

public class CustomGoldenChestplate implements Listener {
    private static final Random random = new Random();

    public CustomGoldenChestplate(){

    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        // Проверяем, является ли пострадавший игроком
        if (!(event.getEntity() instanceof Player)) return;

        Player damagedPlayer = (Player) event.getEntity();

        // Проверяем, кто нанёс урон (игрок или моб)
        boolean isDamagerPlayer = event.getDamager() instanceof Player;
        boolean isDamagerMob = event.getDamager() instanceof LivingEntity && !(event.getDamager() instanceof Player);

        // Если урон не от игрока или моба, ничего не делаем
        if (!isDamagerPlayer && !isDamagerMob) return;

        TeamPlayPlugin teamPlayPlugin = TeamPlayPlugin.getPlugin(TeamPlayPlugin.class);
        TeamManager teamManager = teamPlayPlugin.getTeamManager();

        String damagedPlayerTeam = teamManager.getPlayerTeam(damagedPlayer);

        // Если урон наносит игрок из той же команды, не даём эффекты
        if (isDamagerPlayer) {
            Player damagerPlayer = (Player) event.getDamager();
            String damagerTeam = teamManager.getPlayerTeam(damagerPlayer);

            if (damagedPlayerTeam != null && damagerTeam != null && damagedPlayerTeam.equals(damagerTeam)) {
                return; // Если игроки из одной команды, ничего не делаем
            }
        }

        // Получаем нагрудник игрока
        ItemStack chestplate = damagedPlayer.getInventory().getChestplate();
        if (chestplate == null || chestplate.getType() != Material.GOLDEN_CHESTPLATE) return;

        ItemMeta meta = chestplate.getItemMeta();
        if (meta == null) return;

        PersistentDataContainer data = meta.getPersistentDataContainer();
        if (!data.has(new NamespacedKey("customitems", "gold_chestplate"), PersistentDataType.STRING)) return;

        // Проверка на отмену урона (если урон был отменен, даем регенерацию)
        if (event.isCancelled()) {
            int regenerationLevel = random.nextInt(3) + 1;
            damagedPlayer.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 20, regenerationLevel - 1), true);
        } else {
            // Рандомно даем регенерацию, если урон не отменен
            if (random.nextDouble() < 0.5) {
                int regenerationLevel = random.nextInt(3) + 1;
                damagedPlayer.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 20, regenerationLevel - 1), true);
            }
        }

        // Уменьшаем количество частиц до 5
        int particleCount = 5;

        // Определяем местоположения для частиц
        Location backLoc = damagedPlayer.getLocation().add(0, 1, 0).add(damagedPlayer.getLocation().getDirection().multiply(-0.5)); // На спине
        Vector sideVector = damagedPlayer.getLocation().getDirection().crossProduct(new Vector(0, 1, 0)).normalize();
        Location leftShoulderLoc = damagedPlayer.getLocation().add(sideVector.multiply(0.5)).add(0, 1.0, 0);
        Location rightShoulderLoc = damagedPlayer.getLocation().subtract(sideVector.multiply(0.5)).add(0, 1.0, 0);

        // Отображаем частицы
        spawnParticles(damagedPlayer, backLoc, particleCount / 2);
        spawnParticles(damagedPlayer, leftShoulderLoc, particleCount / 4);
        spawnParticles(damagedPlayer, rightShoulderLoc, particleCount / 4);

        // Если здоровье игрока ниже 25%, даём ускорение
        if (damagedPlayer.getHealth() / damagedPlayer.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue() <= 0.25) {
            damagedPlayer.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 200, 4), true);
        }
    }

    // Вспомогательный метод для отображения частиц
    private void spawnParticles(Player player, Location location, int count) {
        for (int i = 0; i < count; i++) {
            double x = random.nextDouble() - 0.5;
            double y = random.nextDouble() - 0.5;
            double z = random.nextDouble() - 0.5;
            Location particleLoc = location.clone().add(x, y, z);
            player.getWorld().spawnParticle(Particle.REDSTONE, particleLoc, 1, new Particle.DustOptions(Color.YELLOW, 1));
        }
    }
}