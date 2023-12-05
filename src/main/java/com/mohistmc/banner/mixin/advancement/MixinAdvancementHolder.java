package com.mohistmc.banner.mixin.advancement;

import com.mohistmc.banner.injection.advancements.InjectionAdvancementHolder;
import net.minecraft.advancements.AdvancementHolder;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(AdvancementHolder.class)
public class MixinAdvancementHolder implements InjectionAdvancementHolder {

    public final org.bukkit.advancement.Advancement bukkit =
            new org.bukkit.craftbukkit.v1_20_R3.advancement.CraftAdvancement(((AdvancementHolder) (Object) this)); // CraftBukkit

    @Override
    public org.bukkit.advancement.Advancement bridge$bukkit() {
        return bukkit;
    }
}