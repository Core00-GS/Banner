package com.mohistmc.banner.mixin.world.item.crafting;

import com.google.common.collect.Lists;
import java.util.Collection;
import java.util.List;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.network.protocol.game.ClientboundRecipePacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.RecipeBook;
import net.minecraft.stats.ServerRecipeBook;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.bukkit.craftbukkit.v1_20_R3.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerRecipeBook.class)
public abstract class MixinServerRecipeBook extends RecipeBook {

    @Shadow
    protected abstract void sendRecipes(ClientboundRecipePacket.State p_12802_, ServerPlayer p_12803_, List<ResourceLocation> p_12804_);

    /**
     * @author wdog5
     * @reason bukkit
     */
    @Overwrite
    public int addRecipes(Collection<RecipeHolder<?>> recipeHolders, ServerPlayer player) {
        List<ResourceLocation> list = Lists.newArrayList();
        int i = 0;

        for (RecipeHolder<?> recipeholder : recipeHolders) {
            ResourceLocation resourcelocation = recipeholder.id();
            if (!this.known.contains(resourcelocation) && !recipeholder.value().isSpecial() && CraftEventFactory.handlePlayerRecipeListUpdateEvent(player, resourcelocation)) {
                this.add(resourcelocation);
                this.addHighlight(resourcelocation);
                list.add(resourcelocation);
                CriteriaTriggers.RECIPE_UNLOCKED.trigger(player, recipeholder);
                ++i;
            }
        }

        if (list.size() > 0) {
            this.sendRecipes(ClientboundRecipePacket.State.ADD, player, list);
        }

        return i;
    }

    @Inject(method = "sendRecipes", cancellable = true, at = @At("HEAD"))
    public void banner$returnIfFail(ClientboundRecipePacket.State state, ServerPlayer player, List<ResourceLocation> recipesIn, CallbackInfo ci) {
        if (player.connection == null) {
            ci.cancel();
        }
    }
}
