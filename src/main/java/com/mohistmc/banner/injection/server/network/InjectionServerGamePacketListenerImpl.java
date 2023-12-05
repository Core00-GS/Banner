package com.mohistmc.banner.injection.server.network;

import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.world.entity.RelativeMovement;
import org.bukkit.Location;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.slf4j.Logger;

import java.util.Set;

public interface InjectionServerGamePacketListenerImpl extends InjectionServerCommonPacketListenerImpl {

    default void teleport(double d0, double d1, double d2, float f, float f1, PlayerTeleportEvent.TeleportCause cause) {
    }

    default void teleport(double d0, double d1, double d2, float f, float f1, Set<RelativeMovement> set, PlayerTeleportEvent.TeleportCause cause) { // CraftBukkit - Return event status
    }

    default void teleport(Location dest) {
    }

    default void internalTeleport(double d0, double d1, double d2, float f, float f1, Set<RelativeMovement> set) {
    }

    default void chat(String s, PlayerChatMessage original, boolean async) {
    }

    default void handleCommand(String s) {
    }

    default boolean checkLimit(long timestamp) {
        return false;
    }

    default Logger bridge$logger() {
        return null;
    }

    default void pushTeleportCause(PlayerTeleportEvent.TeleportCause cause) {

    }
}
