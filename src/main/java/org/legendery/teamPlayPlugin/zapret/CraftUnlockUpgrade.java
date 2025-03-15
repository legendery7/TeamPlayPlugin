package org.legendery.teamPlayPlugin.zapret;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.legendery.teamPlayPlugin.TeamPlayPlugin;
import org.legendery.teamPlayPlugin.constructor.TeamUpgrade;

public class CraftUnlockUpgrade extends TeamUpgrade {

    private final Material[] materialsToUnlock;

    public CraftUnlockUpgrade(int level, String description, Material... materialsToUnlock) {
        super(level, description);
        this.materialsToUnlock = materialsToUnlock;
    }

    @Override
    public void apply(String teamName) {
        TeamPlayPlugin plugin = TeamPlayPlugin.getPlugin(TeamPlayPlugin.class);
        if (plugin == null) {
       //     Bukkit.getLogger().warning("TeamPlayPlugin не найден!");
            return;
        }

        for (Material material : materialsToUnlock) {
            unlockCraftingRecipe(plugin, material, teamName);
        }
    }

    private void unlockCraftingRecipe(TeamPlayPlugin plugin, Material material, String teamName) {
        NamespacedKey key = new NamespacedKey(plugin, teamName + "_" + material.name().toLowerCase());
        ShapedRecipe recipe = createDummyRecipe(key, material); // Используем фиктивный рецепт
        Bukkit.addRecipe(recipe);
    }

    private ShapedRecipe createDummyRecipe(NamespacedKey key, Material material) {
        ItemStack item = new ItemStack(material);
        ShapedRecipe recipe = new ShapedRecipe(key, item);
        recipe.shape("ABC", "DEF", "GHI");
        recipe.setIngredient('A', Material.DIRT);
        recipe.setIngredient('B', Material.DIRT);
        recipe.setIngredient('C', Material.DIRT);
        recipe.setIngredient('D', Material.DIRT);
        recipe.setIngredient('E', Material.DIRT);
        recipe.setIngredient('F', Material.DIRT);
        recipe.setIngredient('G', Material.DIRT);
        recipe.setIngredient('H', Material.DIRT);
        recipe.setIngredient('I', Material.DIRT);
        return recipe;
    }
    private String[] getMaterialNames() {
        String[] names = new String[materialsToUnlock.length];
        for (int i = 0; i < materialsToUnlock.length; i++) {
            names[i] = materialsToUnlock[i].name();
        }
        return names;
    }
}
