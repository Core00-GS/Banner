package com.mohistmc.banner.mixin.world.entity.decoration;

import com.mohistmc.banner.injection.world.entity.decoration.InjectionItemFrame;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import org.bukkit.craftbukkit.v1_20_R3.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemFrame.class)
public abstract class MixinItemFrame extends HangingEntity implements InjectionItemFrame {

    protected MixinItemFrame(EntityType<? extends HangingEntity> entityType, Level level) {
        super(entityType, level);
    }

    @Shadow protected abstract void onItemChanged(ItemStack item);

    @Shadow @Final
    private static EntityDataAccessor<ItemStack> DATA_ITEM;

    @Inject(method = "hurt", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/decoration/ItemFrame;dropItem(Lnet/minecraft/world/entity/Entity;Z)V"))
    private void banner$damageNonLiving(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (CraftEventFactory.handleNonLivingEntityDamageEvent((ItemFrame) (Object) this, source, amount, false) || this.isRemoved()) {
            cir.setReturnValue(true);
        }
    }

    @Override
    public void setItem(ItemStack itemstack, final boolean flag, final boolean playSound) {
        if (!itemstack.isEmpty()) {
            itemstack = itemstack.copy();
            itemstack.setCount(1);
        }
        this.onItemChanged(itemstack);
        this.getEntityData().set(DATA_ITEM, itemstack);
        if (!itemstack.isEmpty() && playSound) {
            this.playSound(SoundEvents.ITEM_FRAME_ADD_ITEM, 1.0f, 1.0f);
        }
        if (flag && this.pos != null) {
            this.level().updateNeighbourForOutputSignal(this.pos, Blocks.AIR);
        }
    }

    private static AABB calculateBoundingBox(Entity entity, BlockPos blockPosition, Direction direction, int width, int height) {
        double d0 = 0.46875;
        double locX = blockPosition.getX() + 0.5 - direction.getStepX() * 0.46875;
        double locY = blockPosition.getY() + 0.5 - direction.getStepY() * 0.46875;
        double locZ = blockPosition.getZ() + 0.5 - direction.getStepZ() * 0.46875;
        if (entity != null) {
            entity.setPosRaw(locX, locY, locZ);
        }
        double d2 = width;
        double d3 = height;
        double d4 = width;
        Direction.Axis enumdirection_enumaxis = direction.getAxis();
        switch (enumdirection_enumaxis) {
            case X -> d2 = 1.0;
            case Y -> d3 = 1.0;
            case Z -> d4 = 1.0;
        }
        d2 /= 32.0;
        d3 /= 32.0;
        d4 /= 32.0;
        return new AABB(locX - d2, locY - d3, locZ - d4, locX + d2, locY + d3, locZ + d4);
    }
}
