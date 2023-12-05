package com.mohistmc.banner.mixin.world.level.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.CompoundContainer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.DropperBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.craftbukkit.v1_20_R3.inventory.CraftInventoryDoubleChest;
import org.bukkit.craftbukkit.v1_20_R3.inventory.CraftItemStack;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(DropperBlock.class)
public abstract class MixinDropperBlock extends DispenserBlock {

    @Shadow @Final private static DispenseItemBehavior DISPENSE_BEHAVIOUR;

    @Shadow @Final private static Logger LOGGER;

    public MixinDropperBlock(Properties properties) {
        super(properties);
    }

    /**
     * @author wdog5
     * @reason bukkit
     */
    @Overwrite
    public void dispenseFrom(ServerLevel serverLevel, BlockState blockState, BlockPos blockPos) {
        DispenserBlockEntity dispenserBlockEntity = (DispenserBlockEntity)serverLevel.getBlockEntity(blockPos, BlockEntityType.DROPPER).orElse(null);
        if (dispenserBlockEntity == null) {
            LOGGER.warn("Ignoring dispensing attempt for Dropper without matching block entity at {}", blockPos);
        } else {
            BlockSource blockSource = new BlockSource(serverLevel, blockPos, blockState, dispenserBlockEntity);
            int i = dispenserBlockEntity.getRandomSlot(serverLevel.random);
            if (i < 0) {
                serverLevel.levelEvent(1001, blockPos, 0);
            } else {
                ItemStack itemStack = dispenserBlockEntity.getItem(i);
                if (!itemStack.isEmpty()) {
                    Direction direction = (Direction)serverLevel.getBlockState(blockPos).getValue(FACING);
                    Container container = HopperBlockEntity.getContainerAt(serverLevel, blockPos.relative(direction));
                    ItemStack itemStack2;
                    if (container == null) {
                        itemStack2 = DISPENSE_BEHAVIOUR.dispense(blockSource, itemStack);
                    } else {
                        // CraftBukkit start - Fire event when pushing items into other inventories
                        CraftItemStack oitemstack = CraftItemStack.asCraftMirror(itemStack.copy().split(1));

                        org.bukkit.inventory.Inventory destinationInventory;
                        // Have to special case large chests as they work oddly
                        if (container instanceof CompoundContainer) {
                            destinationInventory = new CraftInventoryDoubleChest((CompoundContainer) container);
                        } else {
                            destinationInventory = container.getOwner().getInventory();
                        }

                        InventoryMoveItemEvent event = new InventoryMoveItemEvent(dispenserBlockEntity.getOwner().getInventory(), oitemstack.clone(), destinationInventory, true);
                        serverLevel.getCraftServer().getPluginManager().callEvent(event);
                        if (event.isCancelled()) {
                            return;
                        }

                        itemStack2 = HopperBlockEntity.addItem(dispenserBlockEntity, container, CraftItemStack.asNMSCopy(event.getItem()), direction.getOpposite());
                        if (event.getItem().equals(oitemstack) && itemStack2.isEmpty()) {
                            // CraftBukkit end
                            itemStack2 = itemStack.copy();
                            itemStack2.shrink(1);
                        } else {
                            itemStack2 = itemStack.copy();
                        }
                    }

                    dispenserBlockEntity.setItem(i, itemStack2);
                }
            }
        }
    }
}
