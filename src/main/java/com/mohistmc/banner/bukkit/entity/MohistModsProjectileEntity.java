package com.mohistmc.banner.bukkit.entity;

import com.mohistmc.banner.api.EntityAPI;
import net.minecraft.world.entity.projectile.Projectile;
import org.bukkit.craftbukkit.v1_20_R3.CraftServer;
import org.bukkit.craftbukkit.v1_20_R3.entity.CraftProjectile;

public class MohistModsProjectileEntity extends CraftProjectile {

    public String entityName;

    public MohistModsProjectileEntity(CraftServer server, Projectile entity) {
        super(server, entity);
        this.entityName = EntityAPI.entityName(entity);
    }

    @Override
    public Projectile getHandle() {
        return (Projectile) this.entity;
    }

    @Override
    public String toString() {
        return "MohistModsProjectileEntity{" + entityName + '}';
    }
}

