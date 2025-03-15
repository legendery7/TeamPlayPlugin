package org.legendery.teamPlayPlugin.manager;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

public class DeathMessageSuppressor implements Listener {

    private final DeathToggleManager deathToggleManager;

    public DeathMessageSuppressor(DeathToggleManager deathToggleManager) {
        this.deathToggleManager = deathToggleManager;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!deathToggleManager.isDeathEnabled()) {
            event.setDeathMessage(null); // Отменяем сообщение о смерти
        }
    }
}