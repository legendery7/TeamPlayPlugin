package org.legendery.teamPlayPlugin.manager;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.inventory.meta.Damageable;
import org.legendery.teamPlayPlugin.team.TeamManager;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.UUID;

public class GoatHornListener implements Listener {

    private final TeamManager teamManager;
    private final int effectRadius = 10;
    private final int particleDurationTicks = 2 * 20;
    private final int particleUpdateRate = 5;

    private final Map<String, List<PotionEffect>> uniPositiveEffects = new HashMap<>();
    private final Map<String, List<PotionEffect>> pvpPositiveEffects = new HashMap<>();
    private final Map<String, List<PotionEffect>> pvePositiveEffects = new HashMap<>();

    private final List<PotionEffect> uniNegativeEffects = new ArrayList<>();
    private final List<PotionEffect> pvpNegativeEffects = new ArrayList<>();
    private final List<PotionEffect> pveNegativeEffects = new ArrayList<>();

    private final Map<String, Particle> tagParticles = new HashMap<>(); //Сопоставляем теги с частицами
    private final Map<String, Long> teamCooldowns = new HashMap<>(); // Отслеживаем кулдауны команд
    private final long cooldownDuration = 60 * 1000; // 1 минута в миллисекундах

    private final Map<UUID, Long> playerLastMessage = new HashMap<>(); // Предотвращение спама сообщениями
    private final long messageCooldown = 1000; // 1 секунда


    public GoatHornListener(TeamManager teamManager) {
        this.teamManager = teamManager;
     //   Bukkit.getPluginManager().registerEvents(this, Bukkit.getPluginManager().getPlugin("TeamPlayPlugin"));

        initializeEffects();
        initializeTagParticles();
    }

    private void initializeTagParticles() {
        tagParticles.put("pvp", Particle.FLAME);
        tagParticles.put("pve", Particle.VILLAGER_HAPPY);
        tagParticles.put("cap", Particle.END_ROD); //Уникальная частица
    }

    private void initializeEffects() {
        uniPositiveEffects.put("any", Arrays.asList(
                new PotionEffect(PotionEffectType.HEAL, 1, 0), // Инстант хил 2 (Уровень 1, т.к. heal это instant effect)
                new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 20 * 60, 1), //Сила 2 (1 минута)
                new PotionEffect(PotionEffectType.SPEED, 20 * 60, 1), //Скорость 2 (1 минута)
                new PotionEffect(PotionEffectType.REGENERATION, 20 * 10, 0)  //Регенерация 1 (10 секунд)
        ));
        uniNegativeEffects.add(new PotionEffect(PotionEffectType.HUNGER, 20 * 3, 0)); //Голод 1 (3 секунды)
        uniNegativeEffects.add(new PotionEffect(PotionEffectType.POISON, 20 * 3, 1)); //Отравление 2 (3 секунды)

        pvpPositiveEffects.put("any", Arrays.asList(
                new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 20 * 60, 1),  // Сила 2 (1 минута)
                new PotionEffect(PotionEffectType.FAST_DIGGING, 20 * 60, 0),  // Спешка 1 (1 минута)
                new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 20 * 30, 0) //Сопротивление 1 (30 секунд)
        ));
        pvpNegativeEffects.add(new PotionEffect(PotionEffectType.HUNGER, 20 * 5, 0)); // Голод 1 (5 секунд)

        pvePositiveEffects.put("any", Arrays.asList(
                new PotionEffect(PotionEffectType.SATURATION, 20 * 10, 0),  // Насыщение 1 (10 секунд)
                new PotionEffect(PotionEffectType.SPEED, 20 * 60, 1),  // Скорость 2 (1 минута)
                new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 20 * 60, 0)  // Сила 1 (1 минута)
        ));
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        // Проверяем, что это использование левой кнопки мыши (удар)
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {

            if (item != null && item.getType() == Material.GOAT_HORN) {

                String playerTeam = teamManager.getPlayerTeam(player);
                if (playerTeam == null) {
                    sendMessage(player, ChatColor.RED + "Вы не состоите в команде!");
                    return;
                }

                if (isTeamOnCooldown(playerTeam)) {
                    long timeLeft = getTeamCooldownRemaining(playerTeam);
                    if (timeLeft <= 55000) {
                        sendMessage(player, ChatColor.RED + "Подождите перед использованием Воодушевления. Осталось: " + timeLeft / 1000 + " сек.");
                    }
                    event.setCancelled(true); // Предотвращаем обычное использование рога
                    return;
                }

                // Берем damageValue
                Damageable damageable = (Damageable) item.getItemMeta();
                int damage = damageable.getDamage();

                // Применяем эффекты
                applyEffects(player);

                // Запускаем частицы
                startParticleEffect(player);

                // Устанавливаем кулдаун для команды
                setTeamCooldown(playerTeam);
            }
        }
    }


    private void applyEffects(Player user) {
        String playerTag = teamManager.getPlayerTag(user);

        Location center = user.getLocation();
        Collection<Entity> nearbyEntities = user.getWorld().getNearbyEntities(center, effectRadius, effectRadius, effectRadius);

        for (Entity entity : nearbyEntities) {
            if (entity instanceof LivingEntity) {
                LivingEntity target = (LivingEntity) entity;

                if (target instanceof Player) {
                    Player targetPlayer = (Player) target;

                    if (isTeammate(user, targetPlayer)) {
                        //Эффекты для союзников, в зависимости от Tag
                        if (playerTag != null) {
                            applyPositiveEffects(targetPlayer, playerTag);
                        }

                    } else {
                        if (playerTag != null) {
                            applyNegativeEffects(target, playerTag);
                        }
                    }
                } else {
                    if (playerTag != null) {
                        applyNegativeEffects(target, playerTag);
                    }
                }
            }
        }
    }

    private void applyPositiveEffects(Player target, String tag) {
        Map<String, List<PotionEffect>> effectsMap;

        switch (tag.toLowerCase()) {
            case "cap":
                effectsMap = uniPositiveEffects;
                break;
            case "pvp":
                effectsMap = pvpPositiveEffects;
                break;
            case "pve":
                effectsMap = pvePositiveEffects;
                break;
            default:
                return; //Никаких эффектов, если тег не найден
        }

        List<PotionEffect> effects = effectsMap.get("any");
        if (effects != null) {
            for (PotionEffect effect : effects) {
                target.addPotionEffect(effect);
            }
        }
    }

    private void applyNegativeEffects(LivingEntity target, String tag) {
        List<PotionEffect> negativeEffects;

        switch (tag.toLowerCase()) {
            case "cap":
                negativeEffects = uniNegativeEffects;
                break;
            case "pvp":
                negativeEffects = pvpNegativeEffects;
                break;
            case "pve":
                negativeEffects = pveNegativeEffects;
                break;
            default:
                return; //Никаких эффектов, если тег не найден
        }

        for (PotionEffect effect : negativeEffects) {
            target.addPotionEffect(effect);
        }
    }

    private boolean isTeammate(Player user, Player target) {
        String userTeam = teamManager.getPlayerTeam(user);
        String targetTeam = teamManager.getPlayerTeam(target);

        if (userTeam == null) return false;

        return userTeam.equals(targetTeam);
    }

    private void startParticleEffect(Player player) {
        String playerTag = teamManager.getPlayerTag(player);
        Particle particle = tagParticles.getOrDefault(playerTag.toLowerCase(), Particle.FLAME);

        final BukkitRunnable particleTask = new BukkitRunnable() {
            int ticks = 0;
            Player trackingPlayer = player;

            @Override
            public void run() {
                if (trackingPlayer == null || !trackingPlayer.isOnline() || ticks >= particleDurationTicks) {
                    this.cancel();
                    return;
                }

                Location center = trackingPlayer.getLocation();
                spawnParticles(center, effectRadius, particle);
                applyEffectsInRadius(center, playerTag, trackingPlayer);  // Применяем эффекты внутри радиуса
                ticks += particleUpdateRate;
            }
        };

        particleTask.runTaskTimer(Bukkit.getPluginManager().getPlugin("TeamPlayPlugin"), 0, particleUpdateRate);
    }
    private void applyEffectsInRadius(Location center, String playerTag, Player user) {
        Collection<Entity> nearbyEntities = user.getWorld().getNearbyEntities(center, effectRadius, effectRadius, effectRadius);

        for (Entity entity : nearbyEntities) {
            if (entity instanceof LivingEntity) {
                LivingEntity target = (LivingEntity) entity;

                if (target instanceof Player) {
                    Player targetPlayer = (Player) target;

                    if (isTeammate(user, targetPlayer)) {
                        if (playerTag != null) {
                            applyPositiveEffects(targetPlayer, playerTag);
                        }
                    } else {
                        if (playerTag != null) {
                            applyNegativeEffects(target, playerTag);
                        }
                    }
                } else {
                    if (playerTag != null) {
                        applyNegativeEffects(target, playerTag);
                    }
                }
            }
        }
    }
    private void spawnParticles(Location center, double radius, Particle particle) {
        double y = center.getY();

        for (double a = 0; a < Math.PI * 2; a += Math.PI / 120) {
            double x = radius * Math.cos(a) + center.getX();
            double z = radius * Math.sin(a) + center.getZ();

            center.getWorld().spawnParticle(particle, new Location(center.getWorld(), x, y, z), 1, 0, 0, 0, 0); //Используем переданную частицу
        }
    }

    private boolean isTeamOnCooldown(String teamName) {
        return teamCooldowns.containsKey(teamName) && (System.currentTimeMillis() - teamCooldowns.get(teamName)) < cooldownDuration;
    }

    private long getTeamCooldownRemaining(String teamName) {
        if (!teamCooldowns.containsKey(teamName)) {
            return 0;
        }
        return cooldownDuration - (System.currentTimeMillis() - teamCooldowns.get(teamName));
    }


    private void setTeamCooldown(String teamName) {
        teamCooldowns.put(teamName, System.currentTimeMillis());

        // Убираем кулдаун автоматически через 1 минуту
        new BukkitRunnable() {
            @Override
            public void run() {
                teamCooldowns.remove(teamName);
            }
        }.runTaskLater(Bukkit.getPluginManager().getPlugin("TeamPlayPlugin"), cooldownDuration / 50); // Переводим в тики (20 тиков/секунда)
    }


    private void sendMessage(Player player, String message) {
        UUID playerId = player.getUniqueId();
        long now = System.currentTimeMillis();

        if (!playerLastMessage.containsKey(playerId) || (now - playerLastMessage.get(playerId) > messageCooldown)) {
            player.sendMessage(message);
            playerLastMessage.put(playerId, now);
        }
    }


    // Вспомогательный класс для хранения эффектов рога (Больше не используется, но оставляю на всякий случай)
    private static class HornEffect {
        PotionEffectType positiveEffect;
        PotionEffectType negativeEffect;
        int positiveAmplifier;
        int negativeAmplifier;
        int positiveDuration;
        int negativeDuration;

        public HornEffect(PotionEffectType positiveEffect, PotionEffectType negativeEffect, int positiveAmplifier, int negativeAmplifier, int positiveDuration, int negativeDuration) {
            this.positiveEffect = positiveEffect;
            this.negativeEffect = negativeEffect;
            this.positiveAmplifier = positiveAmplifier;
            this.negativeAmplifier = negativeAmplifier;
            this.positiveDuration = positiveDuration;
            this.negativeDuration = negativeDuration;
        }
    }

}