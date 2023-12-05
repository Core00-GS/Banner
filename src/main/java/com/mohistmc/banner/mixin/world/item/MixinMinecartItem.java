package com.mohistmc.banner.mixin.world.item;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MinecartItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;
import org.bukkit.craftbukkit.v1_20_R3.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(MinecartItem.class)
public abstract class MixinMinecartItem {

    @Redirect(method = "useOn", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
    private boolean banner$cancelAddEntity(Level instance, Entity entity) {
        return false;
    }

    @Inject(method = "useOn", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"),
            locals = LocalCapture.CAPTURE_FAILHARD, cancellable = true)
    private void banner$entityPlace(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir, Level level,
                                    BlockPos blockPos, BlockState blockState,
                                    ItemStack itemStack, RailShape railShape, double d,
                                    AbstractMinecart abstractMinecart) {
        // CraftBukkit start
        if (CraftEventFactory.callEntityPlaceEvent(context, abstractMinecart).isCancelled()) {
            cir.setReturnValue(InteractionResult.FAIL);
        }
        // CraftBukkit end
        if (!level.addFreshEntity(abstractMinecart)) cir.setReturnValue(InteractionResult.PASS); // CraftBukkit
    }
}
