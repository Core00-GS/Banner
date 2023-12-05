package com.mohistmc.banner.mixin.world.entity.projectile;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractCandleBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.bukkit.craftbukkit.v1_20_R3.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.v1_20_R3.event.CraftEventFactory;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.entity.LingeringPotionSplashEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Mixin(ThrownPotion.class)
public abstract class MixinThrownPotion extends ThrowableItemProjectile {

    public MixinThrownPotion(EntityType<? extends ThrowableItemProjectile> entityType, Level level) {
        super(entityType, level);
    }

    @Redirect(method = "onHit", at = @At(value = "INVOKE", remap = false, ordinal = 1, target = "Ljava/util/List;isEmpty()Z"))
    private boolean banner$callEvent(List list) {
        return false;
    }

    /**
     * @author wdog5
     * @reason bukkit
     */
    @Overwrite
    private void applySplash(List<MobEffectInstance> list, @Nullable Entity entity) {
        AABB axisalignedbb = this.getBoundingBox().inflate(4.0, 2.0, 4.0);
        List<LivingEntity> list2 = this.level().getEntitiesOfClass(LivingEntity.class, axisalignedbb);
        Map<org.bukkit.entity.LivingEntity, Double> affected = new HashMap<>();
        if (!list2.isEmpty()) {
            for (LivingEntity entityliving : list2) {
                if (entityliving.isAffectedByPotions()) {
                    double d0 = this.distanceToSqr(entityliving);
                    if (d0 >= 16.0) {
                        continue;
                    }
                    double d2 = 1.0 - Math.sqrt(d0) / 4.0;
                    if (entityliving == entity) {
                        d2 = 1.0;
                    }
                    affected.put((org.bukkit.entity.LivingEntity) (entityliving).getBukkitEntity(), d2);
                }
            }
        }
        PotionSplashEvent event = CraftEventFactory.callPotionSplashEvent((ThrownPotion) (Object) this, affected);
        if (!event.isCancelled() && list != null && !list.isEmpty()) {
            for (org.bukkit.entity.LivingEntity victim : event.getAffectedEntities()) {
                if (!(victim instanceof CraftLivingEntity)) {
                    continue;
                }
                LivingEntity entityliving2 = ((CraftLivingEntity) victim).getHandle();
                double d2 = event.getIntensity(victim);
                for (MobEffectInstance mobeffect : list) {
                    MobEffect mobeffectlist = mobeffect.getEffect();
                    if (!this.level().bridge$pvpMode() && this.getOwner() instanceof ServerPlayer && entityliving2 instanceof ServerPlayer && entityliving2 != this.getOwner()) {
                        if (mobeffectlist == MobEffects.MOVEMENT_SLOWDOWN || mobeffectlist == MobEffects.DIG_SLOWDOWN || mobeffectlist == MobEffects.HARM || mobeffectlist == MobEffects.BLINDNESS
                                || mobeffectlist == MobEffects.HUNGER || mobeffectlist == MobEffects.WEAKNESS || mobeffectlist == MobEffects.POISON) {
                            continue;
                        }
                    }
                    if (mobeffectlist.isInstantenous()) {
                        mobeffectlist.applyInstantenousEffect((ThrownPotion) (Object) this, this.getOwner(), entityliving2, mobeffect.getAmplifier(), d2);
                    } else {
                        int i = (int) (d2 * mobeffect.getDuration() + 0.5);
                        if (i <= 20) {
                            continue;
                        }
                        entityliving2.pushEffectCause(EntityPotionEffectEvent.Cause.POTION_SPLASH);
                        entityliving2.addEffect(new MobEffectInstance(mobeffectlist, i, mobeffect.getAmplifier(), mobeffect.isAmbient(), mobeffect.isVisible()));
                    }
                }
            }
        }
    }

    @Inject(method = "makeAreaOfEffectCloud", cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
    private void banner$makeCloud(ItemStack p_190542_1_, Potion p_190542_2_, CallbackInfo ci, AreaEffectCloud entity) {
        LingeringPotionSplashEvent event = CraftEventFactory.callLingeringPotionSplashEvent((ThrownPotion) (Object) this, entity);
        if (event.isCancelled() || entity.isRemoved()) {
            ci.cancel();
            entity.discard();
        }
    }

    @Inject(method = "dowseFire", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;destroyBlock(Lnet/minecraft/core/BlockPos;ZLnet/minecraft/world/entity/Entity;)Z"))
    private void banner$entityChangeBlock(BlockPos pos, CallbackInfo ci) {
        if (!CraftEventFactory.callEntityChangeBlockEvent((ThrownPotion) (Object) this, pos, Blocks.AIR.defaultBlockState())) {
            ci.cancel();
        }
    }

    @Inject(method = "dowseFire", cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;levelEvent(Lnet/minecraft/world/entity/player/Player;ILnet/minecraft/core/BlockPos;I)V"))
    private void banner$entityChangeBlock2(BlockPos pos, CallbackInfo ci, BlockState state) {
        if (!CraftEventFactory.callEntityChangeBlockEvent((ThrownPotion) (Object) this, pos, state.setValue(CampfireBlock.LIT, false))) {
            ci.cancel();
        }
    }

    @Inject(method = "dowseFire", cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/AbstractCandleBlock;extinguish(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/core/BlockPos;)V"))
    private void banner$entityChangeBlock3(BlockPos pos, CallbackInfo ci, BlockState state) {
        if (!CraftEventFactory.callEntityChangeBlockEvent((ThrownPotion) (Object) this, pos, state.setValue(AbstractCandleBlock.LIT, false))) {
            ci.cancel();
        }
    }
}
