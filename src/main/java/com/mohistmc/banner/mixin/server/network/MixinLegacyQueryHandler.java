package com.mohistmc.banner.mixin.server.network;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.server.ServerInfo;
import net.minecraft.server.network.LegacyQueryHandler;
import org.bukkit.craftbukkit.v1_20_R3.event.CraftEventFactory;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.net.SocketAddress;
import java.util.Locale;

@Mixin(LegacyQueryHandler.class)
public abstract class MixinLegacyQueryHandler {


    @Shadow @Final private static Logger LOGGER;

    @Shadow @Final private ServerInfo server;

    @Shadow
    private static String createVersion0Response(ServerInfo serverInfo) {
        return null;
    }

    @Shadow
    private static ByteBuf createLegacyDisconnectPacket(ByteBufAllocator byteBufAllocator, String string) {
        return null;
    }

    @Shadow
    private static void sendFlushAndClose(ChannelHandlerContext channelHandlerContext, ByteBuf ctx) {
    }

    @Shadow
    private static boolean readCustomPayloadPacket(ByteBuf byteBuf) {
        return false;
    }

    @Shadow
    private static String createVersion1Response(ServerInfo serverInfo) {
        return null;
    }

    /**
     * @author wdog5
     * @reason bukkit
     */
    @Overwrite
    public void channelRead(ChannelHandlerContext channelHandlerContext, Object object) {
        ByteBuf byteBuf = (ByteBuf)object;
        byteBuf.markReaderIndex();
        boolean bl = true;

        try {
            if (byteBuf.readUnsignedByte() != 254) {
                return;
            }

            SocketAddress socketAddress = channelHandlerContext.channel().remoteAddress();
            int i = byteBuf.readableBytes();
            String string;
            org.bukkit.event.server.ServerListPingEvent event = CraftEventFactory.callServerListPingEvent(socketAddress, server.getMotd(), server.getPlayerCount(), server.getMaxPlayers()); // CraftBukkit
            if (i == 0) {
                LOGGER.debug("Ping: (<1.3.x) from {}", socketAddress);
                string = createVersion0Response(this.server, event); // CraftBukkit
                sendFlushAndClose(channelHandlerContext, createLegacyDisconnectPacket(channelHandlerContext.alloc(), string));
            } else {
                if (byteBuf.readUnsignedByte() != 1) {
                    return;
                }

                if (byteBuf.isReadable()) {
                    if (!readCustomPayloadPacket(byteBuf)) {
                        return;
                    }

                    LOGGER.debug("Ping: (1.6) from {}", socketAddress);
                } else {
                    LOGGER.debug("Ping: (1.4-1.5.x) from {}", socketAddress);
                }

                string = createVersion1Response(this.server, event); // CraftBukkit
                sendFlushAndClose(channelHandlerContext, createLegacyDisconnectPacket(channelHandlerContext.alloc(), string));
            }

            byteBuf.release();
            bl = false;
        } catch (RuntimeException var11) {
        } finally {
            if (bl) {
                byteBuf.resetReaderIndex();
                channelHandlerContext.channel().pipeline().remove((LegacyQueryHandler) (Object) this);
                channelHandlerContext.fireChannelRead(object);
            }

        }

    }

    private static String createVersion0Response(ServerInfo serverinfo, org.bukkit.event.server.ServerListPingEvent event) {
        return String.format(Locale.ROOT, "%s\u00a7%d\u00a7%d", event.getMotd(), event.getNumPlayers(), event.getMaxPlayers());
    }

    private static String createVersion1Response(ServerInfo serverinfo, org.bukkit.event.server.ServerListPingEvent event) {
        return String.format(Locale.ROOT, "\u00a71\u0000%d\u0000%s\u0000%s\u0000%d\u0000%d", 127, serverinfo.getServerVersion(), event.getMotd(), event.getNumPlayers(), event.getMaxPlayers());
    }
}
