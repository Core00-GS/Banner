package com.mohistmc.banner.mixin.interaction.dispenser;

import com.mohistmc.banner.bukkit.BukkitExtraConstants;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.core.dispenser.AbstractProjectileDispenseBehavior;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import org.bukkit.craftbukkit.v1_20_R3.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_20_R3.projectiles.CraftBlockProjectileSource;
import org.bukkit.event.block.BlockDispenseEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(AbstractProjectileDispenseBehavior.class)
public abstract class MixinAbstractProjectileDispenseBehavior {

    @Shadow protected abstract Projectile getProjectile(Level level, Position position, ItemStack stack);

    @Shadow protected abstract float getPower();

    @Shadow protected abstract float getUncertainty();

    /**
     * @author wdog5
     * @reason
     */
    @Overwrite
    public ItemStack execute(BlockSource isourceblock, ItemStack itemstack) {
        ServerLevel worldserver = isourceblock.level();
        Position iposition = DispenserBlock.getDispensePosition(isourceblock);
        Direction enumdirection = (Direction) isourceblock.state().getValue(DispenserBlock.FACING);
        Projectile iprojectile = this.getProjectile(worldserver, iposition, itemstack);

        // CraftBukkit start
        // iprojectile.shoot((double) enumdirection.getStepX(), (double) ((float) enumdirection.getStepY() + 0.1F), (double) enumdirection.getStepZ(), this.getPower(), this.getUncertainty());
        ItemStack itemstack1 = itemstack.split(1);
        org.bukkit.block.Block block = worldserver.getWorld().getBlockAt(isourceblock.pos().getX(), isourceblock.pos().getY(), isourceblock.pos().getZ());
        CraftItemStack craftItem = CraftItemStack.asCraftMirror(itemstack1);

        BlockDispenseEvent event = new BlockDispenseEvent(block, craftItem.clone(), new org.bukkit.util.Vector((double) enumdirection.getStepX(), (double) ((float) enumdirection.getStepY() + 0.1F), (double) enumdirection.getStepZ()));
        if (!BukkitExtraConstants.dispenser_eventFired) {
            worldserver.getCraftServer().getPluginManager().callEvent(event);
        }

        if (event.isCancelled()) {
            itemstack.grow(1);
            return itemstack;
        }

        if (!event.getItem().equals(craftItem)) {
            itemstack.grow(1);
            // Chain to handler for new item
            ItemStack eventStack = CraftItemStack.asNMSCopy(event.getItem());
            DispenseItemBehavior idispensebehavior = (DispenseItemBehavior) DispenserBlock.DISPENSER_REGISTRY.get(eventStack.getItem());
            if (idispensebehavior != DispenseItemBehavior.NOOP && idispensebehavior != this) {
                idispensebehavior.dispense(isourceblock, eventStack);
                return itemstack;
            }
        }

        iprojectile.shoot(event.getVelocity().getX(), event.getVelocity().getY(), event.getVelocity().getZ(), this.getPower(), this.getUncertainty());
        ((Entity) iprojectile).banner$setProjectileSource(new CraftBlockProjectileSource((DispenserBlockEntity) isourceblock.blockEntity()));
        // CraftBukkit end
        worldserver.addFreshEntity(iprojectile);
        // itemstack.shrink(1); // CraftBukkit - Handled during event processing
        return itemstack;
    }

}
