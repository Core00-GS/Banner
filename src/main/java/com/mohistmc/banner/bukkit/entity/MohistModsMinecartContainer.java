package com.mohistmc.banner.bukkit.entity;

import com.mohistmc.banner.api.EntityAPI;
import net.minecraft.world.entity.vehicle.AbstractMinecartContainer;
import org.bukkit.craftbukkit.v1_20_R3.CraftServer;
import org.bukkit.craftbukkit.v1_20_R3.entity.CraftMinecartContainer;

public class MohistModsMinecartContainer extends CraftMinecartContainer {

    public String entityName;

    public MohistModsMinecartContainer(CraftServer server, AbstractMinecartContainer entity) {
        super(server, entity);
        this.entityName = EntityAPI.entityName(entity);
    }

    @Override
    public AbstractMinecartContainer getHandle() {
        return (AbstractMinecartContainer) this.entity;
    }

    @Override
    public String toString() {
        return "MohistModsMinecartContainer{" + getType() + '}';
    }
}
