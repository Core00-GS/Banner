package com.mohistmc.banner.mixin.server.players;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.players.StoredUserList;
import net.minecraft.server.players.UserWhiteList;
import net.minecraft.server.players.UserWhiteListEntry;
import org.bukkit.craftbukkit.v1_20_R3.profile.CraftPlayerProfile;
import org.spongepowered.asm.mixin.Mixin;

import java.io.File;

@Mixin(UserWhiteList.class)
public abstract class MixinUserWhiteList extends StoredUserList<GameProfile, UserWhiteListEntry> {

    public MixinUserWhiteList(File file) {
        super(file);
    }

    // Paper start - Add whitelist events
    @Override
    public void add(UserWhiteListEntry entry) {
        if (!new io.papermc.paper.event.server.WhitelistStateUpdateEvent(new CraftPlayerProfile(entry.getUser().getId(), entry.getUser().getName()), io.papermc.paper.event.server.WhitelistStateUpdateEvent.WhitelistStatus.ADDED).callEvent()) {
            return;
        }

        super.add(entry);
    }

    @Override
    public void remove(GameProfile profile) {
        if (!new io.papermc.paper.event.server.WhitelistStateUpdateEvent(new CraftPlayerProfile(profile.getId(), profile.getName()), io.papermc.paper.event.server.WhitelistStateUpdateEvent.WhitelistStatus.REMOVED).callEvent()) {
            return;
        }

        super.remove(profile);
    }
    // Paper end
}
