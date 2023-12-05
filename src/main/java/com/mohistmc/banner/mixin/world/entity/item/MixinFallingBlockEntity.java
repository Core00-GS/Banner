package com.mohistmc.banner.mixin.world.entity.item;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.craftbukkit.v1_20_R3.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(FallingBlockEntity.class)
public abstract class MixinFallingBlockEntity extends Entity {

    // @formatter:off
    @Shadow private BlockState blockState;
    // @formatter:on

    public MixinFallingBlockEntity(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    @Inject(method = "tick", cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"))
    private void banner$entityChangeBlock(CallbackInfo ci, Block block, BlockPos pos) {
        if (!CraftEventFactory.callEntityChangeBlockEvent((FallingBlockEntity) (Object) this, pos, this.blockState)) {
            ci.cancel();
        }
    }

    @Inject(method = "causeFallDamage", at = @At(value = "INVOKE", remap = false, target = "Ljava/util/List;forEach(Ljava/util/function/Consumer;)V"))
    private void banner$damageSource(float distance, float damageMultiplier, DamageSource source, CallbackInfoReturnable<Boolean> cir) {
        CraftEventFactory.entityDamage = (FallingBlockEntity) (Object) this;
    }

    @Inject(method = "causeFallDamage", at = @At(value = "INVOKE", remap = false, shift = At.Shift.AFTER, target = "Ljava/util/List;forEach(Ljava/util/function/Consumer;)V"))
    private void banner$damageSourceReset(float distance, float damageMultiplier, DamageSource source, CallbackInfoReturnable<Boolean> cir) {
        CraftEventFactory.entityDamage = null;
    }

    @Inject(method = "fall", cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"))
    private static void arclight$entityFall(Level level, BlockPos pos, BlockState state, CallbackInfoReturnable<FallingBlockEntity> cir, FallingBlockEntity entity) {
        if (!CraftEventFactory.callEntityChangeBlockEvent(entity, pos, state.getFluidState().createLegacyBlock())) {
            cir.setReturnValue(entity);
        }
    }

    private static FallingBlockEntity fall(Level level, BlockPos pos, BlockState blockState, org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason spawnReason) {
        level.pushAddEntityReason(spawnReason);
        return FallingBlockEntity.fall(level, pos, blockState);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void banner$addData(CompoundTag compoundTag, CallbackInfo ci) {
        // Paper start - Try and load origin location from the old NBT tags for backwards compatibility
        if (compoundTag.contains("SourceLoc_x")) {
            int srcX = compoundTag.getInt("SourceLoc_x");
            int srcY = compoundTag.getInt("SourceLoc_y");
            int srcZ = compoundTag.getInt("SourceLoc_z");
            this.setOrigin(new org.bukkit.Location(this.level().getWorld(), srcX, srcY, srcZ));
        }
        // Paper end
    }
}
