package org.legendery.teamPlayPlugin.model;

public abstract class TeamUpgrade {
    private final int level;
    private final String description;

    public TeamUpgrade(int level, String description) {
        this.level = level;
        this.description = description;
    }

    public int getLevel() {
        return level;
    }

    public String getDescription() {
        return description;
    }

    public abstract void apply(String teamName);
}