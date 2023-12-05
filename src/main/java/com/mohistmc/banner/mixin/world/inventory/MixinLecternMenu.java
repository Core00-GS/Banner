package com.mohistmc.banner.mixin.world.inventory;

import com.mohistmc.banner.injection.world.inventory.InjectionLecternMenu;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.LecternMenu;
import net.minecraft.world.inventory.MenuType;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_20_R3.inventory.CraftInventoryLectern;
import org.bukkit.craftbukkit.v1_20_R3.inventory.CraftInventoryView;
import org.bukkit.event.player.PlayerTakeLecternBookEvent;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LecternMenu.class)
public abstract class MixinLecternMenu extends AbstractContainerMenu implements InjectionLecternMenu {

    @Shadow @Final private Container lectern;
    private CraftInventoryView bukkitEntity;
    private Inventory playerInventory;

    protected MixinLecternMenu(@Nullable MenuType<?> menuType, int i) {
        super(menuType, i);
    }

    public void banner$constructor(int i) {
        throw new RuntimeException();
    }

    public void banner$constructor(int i, Container inventory, ContainerData intArray) {
        throw new RuntimeException();
    }

    public void banner$constructor(int i, Inventory playerInventory) {
        banner$constructor(i);
        this.playerInventory = playerInventory;
    }

    public void banner$constructor(int i, Container inventory, ContainerData intArray, Inventory playerInventory) {
        banner$constructor(i, inventory, intArray);
        this.playerInventory = playerInventory;
    }

    @Inject(method = "clickMenuButton", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/Container;removeItemNoUpdate(I)Lnet/minecraft/world/item/ItemStack;"))
    public void banner$takeBook(Player playerIn, int id, CallbackInfoReturnable<Boolean> cir) {
        PlayerTakeLecternBookEvent event = new PlayerTakeLecternBookEvent((org.bukkit.entity.Player) this.playerInventory.player.getBukkitEntity(), ((CraftInventoryLectern) getBukkitView().getTopInventory()).getHolder());
        Bukkit.getServer().getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "stillValid", cancellable = true, at = @At("HEAD"))
    public void banner$unreachable(Player playerIn, CallbackInfoReturnable<Boolean> cir) {
        if (!bridge$checkReachable()) cir.setReturnValue(true);
    }

    @Override
    public CraftInventoryView getBukkitView() {
        if (bukkitEntity != null) {
            return bukkitEntity;
        }
        CraftInventoryLectern inventory = new CraftInventoryLectern(this.lectern);
        bukkitEntity = new CraftInventoryView(this.playerInventory.player.getBukkitEntity(), inventory, (AbstractContainerMenu) (Object) this);
        return bukkitEntity;
    }

    @Override
    public void bridge$setPlayerInventory(Inventory playerInventory) {
        this.playerInventory = playerInventory;
    }
}
