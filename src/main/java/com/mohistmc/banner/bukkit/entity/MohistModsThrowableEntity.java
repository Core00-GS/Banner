package com.mohistmc.banner.bukkit.entity;

import com.mohistmc.banner.api.EntityAPI;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import org.bukkit.craftbukkit.v1_20_R3.CraftServer;
import org.bukkit.craftbukkit.v1_20_R3.entity.CraftProjectile;

public class MohistModsThrowableEntity extends CraftProjectile {

    public String entityName;

    public MohistModsThrowableEntity(CraftServer server, ThrowableProjectile entity) {
        super(server, entity);
        this.entityName = EntityAPI.entityName(entity);
    }

    @Override
    public ThrowableProjectile getHandle() {
        return (ThrowableProjectile) entity;
    }

    @Override
    public String toString() {
        return "MohistModsThrowableEntity{" + entityName + '}';
    }
}
