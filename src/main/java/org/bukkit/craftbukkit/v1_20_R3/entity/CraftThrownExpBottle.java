package org.bukkit.craftbukkit.v1_20_R3.entity;

import org.bukkit.craftbukkit.v1_20_R3.CraftServer;
import org.bukkit.entity.ThrownExpBottle;

public class CraftThrownExpBottle extends CraftThrowableProjectile implements ThrownExpBottle {
    public CraftThrownExpBottle(CraftServer server, net.minecraft.world.entity.projectile.ThrownExperienceBottle entity) {
        super(server, entity);
    }

    @Override
    public net.minecraft.world.entity.projectile.ThrownExperienceBottle getHandle() {
        return (net.minecraft.world.entity.projectile.ThrownExperienceBottle) entity;
    }

    @Override
    public String toString() {
        return "net.minecraft.world.entity.projectile.ThrownExperienceBottle";
    }
}