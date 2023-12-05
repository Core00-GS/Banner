package com.mohistmc.banner.bukkit.entity;

import com.mohistmc.banner.api.EntityAPI;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import org.bukkit.craftbukkit.v1_20_R3.CraftServer;
import org.bukkit.craftbukkit.v1_20_R3.entity.CraftThrowableProjectile;

public class MohistModsThrowableProjectile extends CraftThrowableProjectile {

    public String entityName;

    public MohistModsThrowableProjectile(CraftServer server, ThrowableItemProjectile entity) {
        super(server, entity);
        this.entityName = EntityAPI.entityName(entity);
    }

    @Override
    public ThrowableItemProjectile getHandle() {
        return (ThrowableItemProjectile) this.entity;
    }

    @Override
    public String toString() {
        return "MohistModsThrowableProjectile{" + entityName + '}';
    }
}
