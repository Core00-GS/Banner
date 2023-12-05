package com.mohistmc.banner.mixin.server.network;

import com.mohistmc.banner.config.BannerConfig;
import com.mohistmc.banner.injection.server.network.InjectionServerLoginPacketListenerImpl;
import com.mojang.authlib.GameProfile;
import net.minecraft.DefaultUncaughtExceptionHandler;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.Connection;
import net.minecraft.network.TickablePacketListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.login.ClientboundHelloPacket;
import net.minecraft.network.protocol.login.ServerLoginPacketListener;
import net.minecraft.network.protocol.login.ServerboundHelloPacket;
import net.minecraft.network.protocol.login.ServerboundKeyPacket;
import net.minecraft.network.protocol.login.ServerboundLoginAcknowledgedPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerConfigurationPacketListenerImpl;
import net.minecraft.server.network.ServerLoginPacketListenerImpl;
import net.minecraft.server.players.PlayerList;
import net.minecraft.util.Crypt;
import net.minecraft.util.CryptException;
import org.apache.commons.lang3.Validate;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_20_R3.CraftServer;
import org.bukkit.craftbukkit.v1_20_R3.util.Waitable;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerPreLoginEvent;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.naming.AuthenticationException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.security.PrivateKey;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@Mixin(ServerLoginPacketListenerImpl.class)
public abstract class MixinServerLoginPacketListenerImpl implements ServerLoginPacketListener, TickablePacketListener,InjectionServerLoginPacketListenerImpl {

    @Shadow
    public abstract void disconnect(Component reason);

    @Shadow
    @Final
    MinecraftServer server;

    @Shadow
    @Final
    public Connection connection;

    @Shadow
    private
    ServerLoginPacketListenerImpl.State state;

    @Shadow
    @Final
    static Logger LOGGER;

    @Shadow
    @Final
    private byte[] challenge;

    @Shadow
    @Final
    private static AtomicInteger UNIQUE_THREAD_ID;

    @Shadow
    abstract void startClientVerification(GameProfile gameProfile);

    @Shadow
    @Nullable
    private String requestedUsername;

    @Shadow
    protected abstract boolean isPlayerAlreadyInWorld(GameProfile gameProfile);

    @Shadow
    @Nullable
    private GameProfile authenticatedProfile;
    private static final java.util.regex.Pattern PROP_PATTERN = java.util.regex.Pattern.compile("\\w{0,16}");
    private ServerPlayer player;

    // CraftBukkit start
    @Deprecated
    @Override
    public void disconnect(String s) {
        disconnect(Component.literal(s));
    }

    private static GameProfile banner$createOfflineProfile(String name) {
        UUID uuid = UUIDUtil.createOfflinePlayerUUID(name);
        return new GameProfile(uuid, name);
    }

    // Paper start - Cache authenticator threads
    private static final AtomicInteger threadId = new AtomicInteger(0);
    private static final java.util.concurrent.ExecutorService authenticatorPool = java.util.concurrent.Executors.newCachedThreadPool(
            r -> {
                Thread ret = new Thread(r, "User Authenticator #" + threadId.incrementAndGet());

                ret.setUncaughtExceptionHandler(new DefaultUncaughtExceptionHandler(LOGGER));

                return ret;
            }
    );

    @Unique
    private int velocityLoginMessageId = -1;    // Paper - Velocity support.
    // Paper end

    /**
     * @author wdog5
     * @reason bukkit
     */
    public void handleHello(ServerboundHelloPacket packet) {
        Validate.validState(this.state == ServerLoginPacketListenerImpl.State.HELLO, "Unexpected hello packet", new Object[0]);
        // Validate.validState(isValidUsername(packet.name()), "Invalid characters in username", new Object[0]); // Mohist Chinese and other special characters are allowed
        this.requestedUsername = packet.name();
        GameProfile gameProfile = this.server.getSingleplayerProfile();
        if (gameProfile != null && packet.name().equalsIgnoreCase(gameProfile.getName())) {
            this.startClientVerification(gameProfile);
        } else {
            if (this.server.usesAuthentication() && !this.connection.isMemoryConnection()) {
                this.state = ServerLoginPacketListenerImpl.State.KEY;
                this.connection.send(new ClientboundHelloPacket("", this.server.getKeyPair().getPublic().getEncoded(), this.challenge));
            } else {
                // Spigot start
                // Paper start - Cache authenticator threads
                authenticatorPool.execute(() -> {
                    try {
                        var banner$gameProfile = banner$createOfflineProfile(requestedUsername);
                        banner$preLogin(banner$gameProfile);
                    } catch (Exception ex) {
                        this.disconnect("Failed to verify username!");
                        LOGGER.warn("Exception verifying " + requestedUsername, ex);
                    }
                });
                // Paper end
                // Spigot end
            }

        }
    }

    @Redirect(method = "verifyLoginAndFinishConnectionSetup", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/players/PlayerList;canPlayerLogin(Ljava/net/SocketAddress;Lcom/mojang/authlib/GameProfile;)Lnet/minecraft/network/chat/Component;"))
    private Component banner$canLogin(PlayerList instance, SocketAddress socketAddress, GameProfile gameProfile) {
        this.player = instance.canPlayerLogin((ServerLoginPacketListenerImpl) (Object) this, gameProfile);
        return null;
    }

    @Inject(method = "verifyLoginAndFinishConnectionSetup", cancellable = true, at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/server/players/PlayerList;canPlayerLogin(Ljava/net/SocketAddress;Lcom/mojang/authlib/GameProfile;)Lnet/minecraft/network/chat/Component;"))
    private void banner$returnIfFail(GameProfile gameProfile, CallbackInfo ci) {
        if (this.player == null) {
            ci.cancel();
        }
    }

    @Redirect(method = "verifyLoginAndFinishConnectionSetup", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/players/PlayerList;disconnectAllPlayersWithProfile(Lcom/mojang/authlib/GameProfile;)Z"))
    private boolean bannerskipKick(PlayerList instance, GameProfile gameProfile) {
        return this.isPlayerAlreadyInWorld(Objects.requireNonNull(this.authenticatedProfile));
    }

    @Inject(method = "handleLoginAcknowledgement", locals = LocalCapture.CAPTURE_FAILHARD, at = @At(value = "INVOKE", target = "Lnet/minecraft/network/Connection;setListener(Lnet/minecraft/network/PacketListener;)V"))
    private void banner$setPlayer(ServerboundLoginAcknowledgedPacket p_298815_, CallbackInfo ci, CommonListenerCookie cookie, ServerConfigurationPacketListenerImpl listener) {
        listener.banner$setPlayer(this.player);
    }

    /**
     * @author wdog5
     * @reason bukkit
     */
    @Overwrite
    public void handleKey(ServerboundKeyPacket packetIn) {
        Validate.validState(this.state == ServerLoginPacketListenerImpl.State.KEY, "Unexpected key packet");

        final String s;
        try {
            PrivateKey privatekey = this.server.getKeyPair().getPrivate();
            if (!packetIn.isChallengeValid(this.challenge, privatekey)) {
                throw new IllegalStateException("Protocol error");
            }

            SecretKey secretKey = packetIn.getSecretKey(privatekey);
            Cipher cipher = Crypt.getCipher(2, secretKey);
            Cipher cipher1 = Crypt.getCipher(1, secretKey);
            s = (new BigInteger(Crypt.digestData("", this.server.getKeyPair().getPublic(), secretKey))).toString(16);
            this.state = ServerLoginPacketListenerImpl.State.AUTHENTICATING;
            this.connection.setEncryptionKey(cipher, cipher1);
        } catch (CryptException cryptexception) {
            throw new IllegalStateException("Protocol error", cryptexception);
        }

        class Handler extends Thread {

            Handler(int i) {
                super("User Authenticator #" + i);
            }

            public void run() {
                String name = Objects.requireNonNull(requestedUsername, "Player name not initialized");

                try {
                    var profileResult = server.getSessionService().hasJoinedServer(name, s, this.getAddress());
                    if (profileResult != null) {
                        var gameProfile = profileResult.profile();
                        if (!connection.isConnected()) {
                            return;
                        }
                        banner$preLogin(gameProfile);
                    } else if (server.isSingleplayer()) {
                        LOGGER.warn("Failed to verify username but will let them in anyway!");
                        startClientVerification(banner$createOfflineProfile(name));
                    } else {
                        disconnect(Component.translatable("multiplayer.disconnect.unverified_username"));
                        LOGGER.error("Username '{}' tried to join with an invalid session", name);
                    }
                } catch (AuthenticationException var3) {
                    if (server.isSingleplayer()) {
                        LOGGER.warn("Authentication servers are down but will let them in anyway!");
                        startClientVerification(banner$createOfflineProfile(name));
                    } else {
                        disconnect(Component.translatable("multiplayer.disconnect.authservers_down"));
                        LOGGER.error("Couldn't verify username because servers are unavailable");
                    }
                } catch (Exception e) {
                    disconnect("Failed to verify username!");
                    LOGGER.error("Exception verifying " + name, e);
                }
            }

            @Nullable
            private InetAddress getAddress() {
                SocketAddress socketaddress = connection.getRemoteAddress();
                return server.getPreventProxyConnections() && socketaddress instanceof InetSocketAddress ? ((InetSocketAddress) socketaddress).getAddress() : null;
            }
        }
        Thread thread = new Handler(UNIQUE_THREAD_ID.incrementAndGet());
        thread.setUncaughtExceptionHandler(new DefaultUncaughtExceptionHandler(LOGGER));
        thread.start();
    }

    void banner$preLogin(GameProfile gameProfile) throws Exception {
        if (velocityLoginMessageId == -1 && BannerConfig.velocityEnabled) {
            disconnect("This server requires you to connect with Velocity.");
            return;
        }

        String playerName = gameProfile.getName();
        InetAddress address = ((InetSocketAddress) connection.getRemoteAddress()).getAddress();
        UUID uniqueId = gameProfile.getId();
        CraftServer craftServer = (CraftServer) Bukkit.getServer();
        AsyncPlayerPreLoginEvent asyncEvent = new AsyncPlayerPreLoginEvent(playerName, address, uniqueId);
        craftServer.getPluginManager().callEvent(asyncEvent);
        if (PlayerPreLoginEvent.getHandlerList().getRegisteredListeners().length != 0) {
            PlayerPreLoginEvent event = new PlayerPreLoginEvent(playerName, address, uniqueId);
            if (asyncEvent.getResult() != PlayerPreLoginEvent.Result.ALLOWED) {
                event.disallow(asyncEvent.getResult(), asyncEvent.getKickMessage());
            }
            class SyncPreLogin extends Waitable<PlayerPreLoginEvent.Result> {

                @Override
                protected PlayerPreLoginEvent.Result evaluate() {
                    craftServer.getPluginManager().callEvent(event);
                    return event.getResult();
                }
            }
            Waitable<PlayerPreLoginEvent.Result> waitable = new SyncPreLogin();
            server.bridge$queuedProcess(waitable);
            if (waitable.get() != PlayerPreLoginEvent.Result.ALLOWED) {
                disconnect(event.getKickMessage());
                return;
            }
        } else if (asyncEvent.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED) {
            disconnect(asyncEvent.getKickMessage());
            return;
        }
        LOGGER.info("UUID of player {} is {}", gameProfile.getName(), gameProfile.getId());
        this.startClientVerification(gameProfile);
    }
}
