package com.mohistmc.banner.bukkit.entity;

import com.mohistmc.banner.api.EntityAPI;
import net.minecraft.world.entity.monster.Monster;
import org.bukkit.craftbukkit.v1_20_R3.CraftServer;
import org.bukkit.craftbukkit.v1_20_R3.entity.CraftMonster;

/**
 * Mohist
 *
 * @author Malcolm - m1lc0lm
 * @Created  at 20.02.2022 - 21:02 GMT+1
 * © Copyright 2021 / 2022 - M1lcolm
 */
public class MohistModsMonster extends CraftMonster {

    public String entityName;

    public MohistModsMonster(CraftServer server, Monster entity) {
        super(server, entity);
        this.entityName = EntityAPI.entityName(entity);
    }


    @Override
    public Monster getHandle() {
        return (Monster) entity;
    }

    @Override
    public String toString() {
        return "MohistModsMonster{" + entityName + '}';
    }
}
