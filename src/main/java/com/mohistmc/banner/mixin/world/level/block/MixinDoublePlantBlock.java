package com.mohistmc.banner.mixin.world.level.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.craftbukkit.v1_20_R3.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DoublePlantBlock.class)
public class MixinDoublePlantBlock {

    @Inject(method = "playerWillDestroy", cancellable = true, at = @At("HEAD"))
    public void banner$blockPhysics(Level worldIn, BlockPos pos, BlockState state, Player player, CallbackInfo ci) {
        if (CraftEventFactory.callBlockPhysicsEvent(worldIn, pos).isCancelled()) {
            ci.cancel();
        }
    }
}
