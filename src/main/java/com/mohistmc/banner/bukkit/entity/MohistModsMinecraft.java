package com.mohistmc.banner.bukkit.entity;

import com.mohistmc.banner.api.EntityAPI;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import org.bukkit.craftbukkit.v1_20_R3.CraftServer;
import org.bukkit.craftbukkit.v1_20_R3.entity.CraftMinecart;

public class MohistModsMinecraft extends CraftMinecart {

    public String entityName;

    public MohistModsMinecraft(CraftServer server, AbstractMinecart entity) {
        super(server, entity);
        this.entityName = EntityAPI.entityName(entity);
    }

    @Override
    public AbstractMinecart getHandle() {
        return (AbstractMinecart) this.entity;
    }

    @Override
    public String toString() {
        return "MohistModsMinecraft{" + entityName + '}';
    }
}
