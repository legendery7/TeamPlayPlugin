package org.legendery.teamPlayPlugin.constructor;

import org.bukkit.Material;

import java.util.Map;

public class Upgrade {

    private final int level;
    private final Map<Material, Integer> cost;
    private final TeamUpgrade upgrade;

    public Upgrade(int level, Map<Material, Integer> cost, TeamUpgrade upgrade) {
        this.level = level;
        this.cost = cost;
        this.upgrade = upgrade;
    }

    public int getLevel() {
        return level;
    }

    public Map<Material, Integer> getCost() {
        return cost;
    }

    public TeamUpgrade getUpgrade() {
        return upgrade;
    }
}