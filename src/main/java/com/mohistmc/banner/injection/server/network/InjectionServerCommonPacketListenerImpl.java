package com.mohistmc.banner.injection.server.network;

import net.minecraft.server.level.ServerPlayer;
import org.bukkit.craftbukkit.v1_20_R3.entity.CraftPlayer;

public interface InjectionServerCommonPacketListenerImpl {

    default void disconnect(String s) {
    }

    default CraftPlayer getCraftPlayer() {
        return null;
    }

    default boolean bridge$isDisconnected(){
        return false;
    }

    default boolean bridge$processedDisconnect() {
        return false;
    }

    default void banner$setProcessedDisconnect(boolean processedDisconnect) {
    }

    default void banner$setPlayer(ServerPlayer player) {

    }
}
