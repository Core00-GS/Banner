package com.mohistmc.banner.bukkit.inventory.recipe;

import com.mohistmc.banner.bukkit.BukkitExtraConstants;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapedRecipe;
import org.bukkit.NamespacedKey;
import org.bukkit.craftbukkit.v1_20_R3.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_20_R3.inventory.CraftShapedRecipe;
import org.bukkit.craftbukkit.v1_20_R3.util.CraftNamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class BannerShapedRecipe extends CraftShapedRecipe {

    private final ShapedRecipe recipe;

    public BannerShapedRecipe(NamespacedKey id, ShapedRecipe recipe) {
        super(id, null);
        this.recipe = recipe;
    }

    @Override
    public @NotNull ItemStack getResult() {
        return CraftItemStack.asCraftMirror(this.recipe.getResultItem(BukkitExtraConstants.getServer().registryAccess()));
    }

    @Override
    public void addToCraftingManager() {
        BukkitExtraConstants.getServer().getRecipeManager().addRecipe((new RecipeHolder<>(CraftNamespacedKey.toMinecraft(this.getKey()), this.recipe)));
    }
}