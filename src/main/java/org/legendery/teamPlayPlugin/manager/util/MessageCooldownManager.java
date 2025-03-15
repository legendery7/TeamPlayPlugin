package org.legendery.teamPlayPlugin.manager.util;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MessageCooldownManager {

    private final Map<UUID, Long> lastMessageTime = new HashMap<>();
    private final long cooldownMillis;

    public MessageCooldownManager(long cooldownSeconds) {
        this.cooldownMillis = cooldownSeconds * 1000;
    }

    public boolean canSendMessage(Player player) {
        UUID playerId = player.getUniqueId();
        long now = System.currentTimeMillis();

        if (lastMessageTime.containsKey(playerId) && (now - lastMessageTime.get(playerId)) < cooldownMillis) {
            return false;
        }

        lastMessageTime.put(playerId, now);
        return true;
    }
}