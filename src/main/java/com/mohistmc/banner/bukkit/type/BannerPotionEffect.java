package com.mohistmc.banner.bukkit.type;

import net.minecraft.world.effect.MobEffect;
import org.bukkit.craftbukkit.v1_20_R3.potion.CraftPotionEffectType;

public class BannerPotionEffect extends CraftPotionEffectType {

    private final String name;

    public BannerPotionEffect(MobEffect handle, String name) {
        super(handle);
        this.name = name;
    }

    @Override
    public String getName() {
        String name = super.getName();
        if (name.startsWith("UNKNOWN_EFFECT_TYPE_")) {
            return this.name;
        } else {
            return name;
        }
    }
}
