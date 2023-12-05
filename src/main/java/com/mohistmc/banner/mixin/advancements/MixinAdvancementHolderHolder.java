package com.mohistmc.banner.mixin.advancements;

import com.mohistmc.banner.injection.advancements.InjectionAdvancementHolder;
import net.minecraft.advancements.AdvancementHolder;
import org.bukkit.advancement.Advancement;
import org.bukkit.craftbukkit.v1_20_R3.advancement.CraftAdvancement;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(AdvancementHolder.class)
public class MixinAdvancementHolderHolder implements InjectionAdvancementHolder {

    @Override
    public Advancement bridge$bukkit() {
        return new CraftAdvancement((AdvancementHolder) (Object) this);
    }
}
