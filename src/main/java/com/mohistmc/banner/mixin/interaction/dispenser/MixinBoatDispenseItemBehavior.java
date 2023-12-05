package com.mohistmc.banner.mixin.interaction.dispenser;

import com.mohistmc.banner.bukkit.BukkitExtraConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.BoatDispenseItemBehavior;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.entity.vehicle.ChestBoat;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.phys.Vec3;
import org.bukkit.craftbukkit.v1_20_R3.inventory.CraftItemStack;
import org.bukkit.event.block.BlockDispenseEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(BoatDispenseItemBehavior.class)
public abstract class MixinBoatDispenseItemBehavior {

    @Shadow @Final private DefaultDispenseItemBehavior defaultDispenseItemBehavior;

    @Shadow @Final private boolean isChestBoat;

    @Shadow @Final private Boat.Type type;

    /**
     * @author wdog5
     * @reason bukkit event
     */
    @Overwrite
    public ItemStack execute(BlockSource isourceblock, ItemStack itemstack) {
        Direction enumdirection = (Direction) isourceblock.state().getValue(DispenserBlock.FACING);
        ServerLevel worldserver = isourceblock.level();
        Vec3 vec3 = isourceblock.center();
        double d0 = 0.5625 + (double)EntityType.BOAT.getWidth() / 2.0;
        double d1 = vec3.x() + (double)enumdirection.getStepX() * d0;
        double d2 = vec3.y() + (double)((float)enumdirection.getStepY() * 1.125F);
        double d3 = vec3.z() + (double)enumdirection.getStepZ() * d0;
        BlockPos blockposition = isourceblock.pos().relative(enumdirection);
        double d4;

        if (worldserver.getFluidState(blockposition).is(FluidTags.WATER)) {
            d4 = 1.0D;
        } else {
            if (!worldserver.getBlockState(blockposition).isAir() || !worldserver.getFluidState(blockposition.below()).is(FluidTags.WATER)) {
                return this.defaultDispenseItemBehavior.dispense(isourceblock, itemstack);
            }

            d4 = 0.0D;
        }

        // Object object = this.isChestBoat ? new ChestBoat(worldserver, d1, d2 + d4, d3) : new EntityBoat(worldserver, d1, d2 + d4, d3);
        // CraftBukkit start
        ItemStack itemstack1 = itemstack.split(1);
        org.bukkit.block.Block block = worldserver.getWorld().getBlockAt(isourceblock.pos().getX(), isourceblock.pos().getY(), isourceblock.pos().getZ());
        CraftItemStack craftItem = CraftItemStack.asCraftMirror(itemstack1);

        BlockDispenseEvent event = new BlockDispenseEvent(block, craftItem.clone(), new org.bukkit.util.Vector(d1, d2 + d4, d3));
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

        Object object = this.isChestBoat ? new ChestBoat(worldserver, event.getVelocity().getX(), event.getVelocity().getY(), event.getVelocity().getZ()) : new Boat(worldserver, event.getVelocity().getX(), event.getVelocity().getY(), event.getVelocity().getZ());
        // CraftBukkit end

        ((Boat) object).setVariant(this.type);
        ((Boat) object).setYRot(enumdirection.toYRot());
        if (!worldserver.addFreshEntity((Entity) object)) itemstack.grow(1); // CraftBukkit
        // itemstack.shrink(1); // CraftBukkit - handled during event processing
        return itemstack;
    }

}
