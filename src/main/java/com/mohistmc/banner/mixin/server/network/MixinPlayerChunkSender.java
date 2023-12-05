package com.mohistmc.banner.mixin.server.network;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.PlayerChunkSender;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import org.bukkit.craftbukkit.v1_20_R3.CraftChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerChunkSender.class)
public class MixinPlayerChunkSender {

    @Inject(method = "dropChunk",
            at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;send(Lnet/minecraft/network/protocol/Packet;)V",
            shift = At.Shift.AFTER))
    private void banner$chunkUnload(ServerPlayer serverPlayer, ChunkPos chunkPos, CallbackInfo ci) {
        // Paper start
        if(io.papermc.paper.event.packet.PlayerChunkUnloadEvent.getHandlerList().getRegisteredListeners().length > 0){
            new io.papermc.paper.event.packet.PlayerChunkUnloadEvent(serverPlayer.getBukkitEntity().getWorld().getChunkAt(chunkPos.x, chunkPos.z), serverPlayer.getBukkitEntity()).callEvent();
        }
        // Paper end
    }

    @Inject(method = "sendChunk",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;send(Lnet/minecraft/network/protocol/Packet;)V",
                    shift = At.Shift.AFTER))
    private static void banner$chunkLoad(ServerGamePacketListenerImpl serverGamePacketListenerImpl, ServerLevel serverLevel, LevelChunk levelChunk, CallbackInfo ci) {
        // Paper start
        if(io.papermc.paper.event.packet.PlayerChunkLoadEvent.getHandlerList().getRegisteredListeners().length > 0){
            new io.papermc.paper.event.packet.PlayerChunkLoadEvent(new CraftChunk(levelChunk), serverGamePacketListenerImpl.getPlayer().getBukkitEntity()).callEvent();
        }
        // Paper end
    }
}
