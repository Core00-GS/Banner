package com.mohistmc.banner.mixin.server;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mohistmc.banner.BannerMCStart;
import com.mohistmc.banner.api.color.ColorsAPI;
import com.mohistmc.banner.bukkit.BukkitExtraConstants;
import com.mohistmc.banner.bukkit.BukkitSnapshotCaptures;
import com.mohistmc.banner.config.BannerConfig;
import com.mohistmc.banner.config.BannerConfigUtil;
import com.mohistmc.banner.fabric.BukkitRegistry;
import com.mohistmc.banner.injection.server.InjectionMinecraftServer;
import com.mohistmc.banner.paper.RollingAverage;
import com.mojang.datafixers.DataFixer;
import it.unimi.dsi.fastutil.longs.LongIterator;
import jline.console.ConsoleReader;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.minecraft.CrashReport;
import net.minecraft.ReportedException;
import net.minecraft.SystemReport;
import net.minecraft.Util;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.ChatDecorator;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import net.minecraft.network.protocol.status.ServerStatus;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.RegistryLayer;
import net.minecraft.server.Services;
import net.minecraft.server.TickTask;
import net.minecraft.server.WorldLoader;
import net.minecraft.server.WorldStem;
import net.minecraft.server.bossevents.CustomBossEvents;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.server.level.progress.ChunkProgressListener;
import net.minecraft.server.level.progress.ChunkProgressListenerFactory;
import net.minecraft.server.network.ServerConnectionListener;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.players.PlayerList;
import net.minecraft.util.ModCheck;
import net.minecraft.util.Unit;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.util.profiling.jfr.JvmProfiler;
import net.minecraft.util.thread.ReentrantBlockableEventLoop;
import net.minecraft.world.RandomSequences;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ForcedChunksSavedData;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.storage.CommandStorage;
import net.minecraft.world.level.storage.DerivedLevelData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.ServerLevelData;
import net.minecraft.world.level.storage.WorldData;
import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.craftbukkit.Main;
import org.bukkit.craftbukkit.v1_20_R3.CraftServer;
import org.bukkit.craftbukkit.v1_20_R3.scoreboard.CraftScoreboardManager;
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.event.world.WorldInitEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.plugin.PluginLoadOrder;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.Proxy;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.function.BooleanSupplier;
import java.util.logging.Level;
import java.util.logging.Logger;

// Banner - TODO fix inject method
@Mixin(MinecraftServer.class)
public abstract class MixinMinecraftServer extends ReentrantBlockableEventLoop<TickTask> implements InjectionMinecraftServer {

    // @formatter:off
    @Shadow public MinecraftServer.ReloadableResources resources;

    @Shadow public Map<ResourceKey<net.minecraft.world.level.Level>, ServerLevel> levels;
    @Shadow @Final public static org.slf4j.Logger LOGGER;
    @Shadow public abstract boolean isSpawningMonsters();
    @Shadow public abstract boolean isSpawningAnimals();
    @Shadow private int tickCount;
    @Shadow public abstract PlayerList getPlayerList();
    @Shadow public abstract boolean isStopped();
    // @formatter:on

    @Shadow public ServerConnectionListener connection;

    @Shadow public abstract ServerLevel overworld();

    @Shadow protected abstract void updateMobSpawningFlags();

    @Shadow @Final private static int TICK_STATS_SPAN;
    @Shadow private long lastServerStatus;

    @Shadow
    private static void setInitialSpawn(ServerLevel serverLevel, ServerLevelData serverLevelData, boolean bl, boolean bl2) {
    }

    @Shadow protected abstract void setupDebugLevel(WorldData worldData);

    @Shadow public WorldData worldData;

    @Shadow public abstract Set<ResourceKey<net.minecraft.world.level.Level>> levelKeys();

    @Shadow public abstract void executeIfPossible(Runnable task);

    @Shadow @Final public Executor executor;
    @Shadow public LevelStorageSource.LevelStorageAccess storageSource;

    @Shadow public abstract WorldData getWorldData();

    @Shadow public abstract RegistryAccess.Frozen registryAccess();

    @Shadow public abstract CustomBossEvents getCustomBossEvents();

    @Shadow protected abstract boolean initServer() throws IOException;

    @Shadow @Nullable private ServerStatus.Favicon statusIcon;

    @Shadow protected abstract Optional<ServerStatus.Favicon> loadStatusIcon();

    @Shadow @Nullable private ServerStatus status;

    @Shadow protected abstract ServerStatus buildServerStatus();

    @Shadow private volatile boolean running;

    @Shadow
    private static CrashReport constructOrExtractCrashReport(Throwable cause) {
        return null;
    }

    @Shadow public abstract SystemReport fillSystemReport(SystemReport systemReport);

    @Shadow public abstract File getServerDirectory();

    @Shadow public abstract void onServerCrash(CrashReport report);

    @Shadow private boolean stopped;

    @Shadow public abstract void stopServer();

    @Shadow @Final protected Services services;

    @Shadow public abstract void onServerExit();

    @Shadow private float averageTickTime;
    @Shadow private volatile boolean isReady;

    @Shadow protected abstract void endMetricsRecordingTick();

    @Shadow private ProfilerFiller profiler;

    @Shadow protected abstract void waitUntilNextTick();

    @Shadow private long delayedTasksMaxNextTickTime;
    @Shadow private boolean mayHaveDelayedTasks;

    @Shadow public abstract void tickServer(BooleanSupplier hasTimeLeft);

    @Shadow protected abstract boolean haveTime();

    @Shadow protected abstract void startMetricsRecordingTick();

    @Shadow private long lastOverloadWarning;
    @Shadow private boolean debugCommandProfilerDelayStart;
    @Shadow @Nullable private MinecraftServer.TimeProfiler debugCommandProfiler;
    @Shadow @Final private LayeredRegistryAccess<RegistryLayer> registries;

    @Shadow public abstract boolean isNetherEnabled();

    @Shadow public abstract boolean isDemo();

    @Shadow @Final public ChunkProgressListenerFactory progressListenerFactory;

    @Shadow protected abstract void readScoreboard(DimensionDataStorage dataStorage);

    @Shadow @Nullable private CommandStorage commandStorage;

    @Shadow(remap = false) public abstract String getServerModName();

    @Shadow public abstract ModCheck getModdedStatus();

    @Shadow protected abstract void forceDifficulty();

    @Shadow @Nullable public abstract ServerLevel getLevel(ResourceKey<net.minecraft.world.level.Level> dimension);

    // CraftBukkit start
    public WorldLoader.DataLoadContext worldLoader;
    public org.bukkit.craftbukkit.v1_20_R3.CraftServer server;
    public OptionSet options;
    public org.bukkit.command.ConsoleCommandSender console;
    public ConsoleReader reader;
    private static int currentTick = BukkitExtraConstants.currentTick;
    public java.util.Queue<Runnable> processQueue = BukkitExtraConstants.bridge$processQueue;
    public int autosavePeriod = BukkitExtraConstants.bridge$autosavePeriod;
    private boolean forceTicks;
    public Commands vanillaCommandDispatcher;
    private boolean hasStopped = false;
    private final Object stopLock = new Object();
    public final double[] recentTps = new double[3];
    private static final int TPS = 20;
    private static final int TICK_TIME = 1000000000 / TPS;
    private static final int SAMPLE_INTERVAL = 20; // Paper
    // CraftBukkit end

    public MixinMinecraftServer(String string) {
        super(string);
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void banner$loadOptions(Thread thread, LevelStorageSource.LevelStorageAccess levelStorageAccess, PackRepository packRepository, WorldStem worldStem, Proxy proxy, DataFixer dataFixer, Services services, ChunkProgressListenerFactory chunkProgressListenerFactory, CallbackInfo ci) {
        String[] arguments = ManagementFactory.getRuntimeMXBean().getInputArguments().toArray(new String[0]);
        OptionParser parser = new Main();
        try {
            options = parser.parse(arguments);
        } catch (Exception ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, ex.getLocalizedMessage());
        }
        Main.handleParser(parser, options);
        this.vanillaCommandDispatcher = worldStem.dataPackResources().getCommands();
        this.worldLoader = BukkitSnapshotCaptures.getDataLoadContext();
    }

    @Inject(method = "stopServer", at = @At(value = "INVOKE", remap = false, ordinal = 0, shift = At.Shift.AFTER, target = "Lorg/slf4j/Logger;info(Ljava/lang/String;)V"))
    public void banner$unloadPlugins(CallbackInfo ci) {
        if (this.server != null) {
            this.server.disablePlugins();
        }
    }

    // Paper start - Further improve server tick loop
    private static final long SEC_IN_NANO = 1000000000;
    private static final long MAX_CATCHUP_BUFFER = TICK_TIME * TPS * 60L;
    private long lastTick = 0;
    private long catchupTime = 0;
    public final RollingAverage tps1 = new RollingAverage(60, TPS, SEC_IN_NANO);
    public final RollingAverage tps5 = new RollingAverage(60 * 5, TPS, SEC_IN_NANO);
    public final RollingAverage tps15 = new RollingAverage(60 * 15, TPS, SEC_IN_NANO);
    private static final java.math.BigDecimal TPS_BASE = new java.math.BigDecimal(1E9).multiply(new java.math.BigDecimal(SAMPLE_INTERVAL));
    // Paper End

    /**
     * @author wdog5
     * @reason bukkit
     */
    @Overwrite
    protected void runServer() {
        try {
            if (!this.initServer()) {
                throw new IllegalStateException("Failed to initialize server");
            }

            this.nextTickTime = Util.getMillis();
            this.statusIcon = this.loadStatusIcon().orElse(null);
            this.status = this.buildServerStatus();

            // Spigot start
            Arrays.fill( recentTps, 20 );
            long start = System.nanoTime(), curTime, tickSection = start; // Paper - Further improve server tick loop
            lastTick = start - TICK_TIME; // Paper
            while (this.running) {
                long i = ((curTime = System.nanoTime()) / (1000L * 1000L)) - this.nextTickTime; // Paper

                if (i > 5000L && this.nextTickTime - this.lastOverloadWarning >= 30000L) { // CraftBukkit
                    long j = i / 50L;

                    if (server.getWarnOnOverload()) // CraftBukkit
                        MinecraftServer.LOGGER.warn("Can't keep up! Is the server overloaded? Running {}ms or {} ticks behind", i, j);
                    this.nextTickTime += j * 50L;
                    this.lastOverloadWarning = this.nextTickTime;
                }

                if ( ++currentTick % SAMPLE_INTERVAL == 0 )
                {
                    final long diff = curTime - tickSection;
                    java.math.BigDecimal currentTps = TPS_BASE.divide(new java.math.BigDecimal(diff), 30, java.math.RoundingMode.HALF_UP);
                    tps1.add(currentTps, diff);
                    tps5.add(currentTps, diff);
                    tps15.add(currentTps, diff);
                    // Backwards compat with bad plugins
                    this.recentTps[0] = tps1.getAverage();
                    this.recentTps[1] = tps5.getAverage();
                    this.recentTps[2] = tps15.getAverage();
                    // Paper end
                    tickSection = curTime;
                }
                // Spigot end

                if (this.debugCommandProfilerDelayStart) {
                    this.debugCommandProfilerDelayStart = false;
                    this.debugCommandProfiler = new MinecraftServer.TimeProfiler(Util.getNanos(), this.tickCount);
                }

                // BukkitExtraConstants.currentTick = (int) (System.currentTimeMillis() / 50); // CraftBukkit  // Paper - don't overwrite current tick time
                this.nextTickTime += 50L;
                this.startMetricsRecordingTick();
                this.profiler.push("tick");
                this.tickServer(this::haveTime);
                this.profiler.popPush("nextTickWait");
                this.mayHaveDelayedTasks = true;
                this.delayedTasksMaxNextTickTime = Math.max(Util.getMillis() + 50L, this.nextTickTime);
                this.waitUntilNextTick();
                this.profiler.pop();
                this.endMetricsRecordingTick();
                this.isReady = true;
                JvmProfiler.INSTANCE.onServerTick(this.averageTickTime);
            }
        } catch (Throwable throwable) {
            MinecraftServer.LOGGER.error("Encountered an unexpected exception", throwable);
            // Spigot Start
            if ( throwable.getCause() != null )
            {
                MinecraftServer.LOGGER.error( "\tCause of unexpected exception was", throwable.getCause() );
            }
            // Spigot End
            CrashReport crashreport = constructOrExtractCrashReport(throwable);

            this.fillSystemReport(crashreport.getSystemReport());
            File file = new File(new File(this.getServerDirectory(), "crash-reports"), "crash-" + Util.getFilenameFormattedDateTime() + "-server.txt");

            if (crashreport.saveToFile(file)) {
                MinecraftServer.LOGGER.error("This crash report has been saved to: {}", file.getAbsolutePath());
            } else {
                MinecraftServer.LOGGER.error("We were unable to save this crash report to disk.");
            }

            this.onServerCrash(crashreport);
        } finally {
            try {
                this.stopped = true;
                this.stopServer();
            } catch (Throwable throwable1) {
                MinecraftServer.LOGGER.error("Exception stopping the server", throwable1);
            } finally {
                if (this.services.profileCache() != null) {
                    this.services.profileCache().clearExecutor();
                }

                //org.spigotmc.WatchdogThread.doStop(); // Spigot // Banner - disabled
                // CraftBukkit start - Restore terminal to original settings
                try {
                    reader.getTerminal().restore();
                } catch (Exception ignored) {
                }
                // CraftBukkit end
                this.onServerExit();
            }

        }

    }

    private static double calcTps(double avg, double exp, double tps) {
        return (avg * exp) + (tps * (1 - exp));
    }

    @Inject(method = "stopServer", at = @At("HEAD"), cancellable = true)
    private void banner$stop(CallbackInfo ci) {
        synchronized(stopLock) {
            if (hasStopped) ci.cancel();
            hasStopped = true;
        }
    }

    /**
     * @author wdog5
     * @reason colorful motd
     */
    @Overwrite
    public String getMotd() {
        return ColorsAPI.of(BannerConfigUtil.motdFirstLine() + "\n" + BannerConfigUtil.motdSecondLine());
    }

    @Inject(method = "stopServer", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/players/PlayerList;removeAll()V"))
    private void banner$stopThread(CallbackInfo ci) {
        try { Thread.sleep(100); } catch (InterruptedException ex) {} // CraftBukkit - SPIGOT-625 - give server at least a chance to send packets
    }

    @Inject(method = "loadLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;prepareLevels(Lnet/minecraft/server/level/progress/ChunkProgressListener;)V",
            shift = At.Shift.AFTER))
    private void banner$loadLevel(CallbackInfo ci) {
        if (!BannerConfig.skipOtherWorldPreparing) {
            for (ServerLevel worldserver : ((MinecraftServer)(Object)this).getAllLevels()) {
                if (worldserver != overworld()) {
                    if (banner$isNether(worldserver) && isNetherEnabled()) {
                        banner$prepareWorld(worldserver);
                    }else if (banner$isEnd(worldserver) && this.server.getAllowEnd()) {
                        banner$prepareWorld(worldserver);
                    }
                    if (banner$isNotNetherAndEnd(worldserver)) {
                        banner$prepareWorld(worldserver);
                    }
                }
            }
        }
    }

    private boolean banner$isNotNetherAndEnd(ServerLevel worldserver) {
        return !banner$isNether(worldserver) && !banner$isEnd(worldserver);
    }

    private boolean banner$isNether(ServerLevel worldserver) {
        return worldserver == this.getLevel(net.minecraft.world.level.Level.NETHER);
    }

    private boolean banner$isEnd(ServerLevel worldserver) {
        return worldserver == this.getLevel(net.minecraft.world.level.Level.END);
    }

    private void banner$prepareWorld(ServerLevel worldserver) {
        this.prepareLevels(worldserver.getChunkSource().chunkMap.progressListener, worldserver);
        worldserver.entityManager.tick(); // SPIGOT-6526: Load pending entities so they are available to the API
        this.server.getPluginManager().callEvent(new WorldLoadEvent(worldserver.getWorld()));
    }

    @Inject(method = "loadLevel", at = @At("RETURN"))
    public void banner$enablePlugins(CallbackInfo ci) {
        this.server.enablePlugins(PluginLoadOrder.POSTWORLD);
        this.server.getPluginManager().callEvent(new ServerLoadEvent(ServerLoadEvent.LoadType.STARTUP));
        this.connection.acceptConnections();
    }

    @Inject(method = "createLevels", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerLevel;<init>(Lnet/minecraft/server/MinecraftServer;Ljava/util/concurrent/Executor;Lnet/minecraft/world/level/storage/LevelStorageSource$LevelStorageAccess;Lnet/minecraft/world/level/storage/ServerLevelData;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/world/level/dimension/LevelStem;Lnet/minecraft/server/level/progress/ChunkProgressListener;ZJLjava/util/List;ZLnet/minecraft/world/RandomSequences;)V",
            ordinal = 0))
    private void banner$registerEnv(ChunkProgressListener p_240787_1_, CallbackInfo ci) {
        BukkitRegistry.registerEnvironments(this.registryAccess().registryOrThrow(Registries.LEVEL_STEM));
    }

    @Inject(method = "createLevels", at = @At(value = "INVOKE", remap = false,
            target = "Ljava/util/Map;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", ordinal = 0),
            locals = LocalCapture.CAPTURE_FAILHARD)
    private void banner$worldInit(ChunkProgressListener listener, CallbackInfo ci, ServerLevelData serverLevelData,
                                    boolean bl, Registry registry, WorldOptions worldOptions, long l, long m,
                                    List list, LevelStem levelStem, ServerLevel serverLevel) {
        banner$initLevel(serverLevel);
    }

    @Redirect(method = "createLevels",
            at = @At(value = "NEW", args = "class=net/minecraft/server/level/ServerLevel", ordinal = 1))
    private ServerLevel banner$resetListener(MinecraftServer server, Executor dispatcher,
                                             LevelStorageSource.LevelStorageAccess levelStorageAccess,
                                             ServerLevelData serverLevelData, ResourceKey dimension,
                                             LevelStem levelStem, ChunkProgressListener progressListener,
                                             boolean isDebug, long biomeZoomSeed, List customSpawners, boolean tickTime,
                                             RandomSequences randomSequences) {
        ChunkProgressListener listener = this.progressListenerFactory.create(11);
        return new ServerLevel(server, dispatcher, levelStorageAccess, serverLevelData,
                dimension, levelStem, listener, isDebug, biomeZoomSeed, customSpawners, tickTime, randomSequences);
    }

    @Inject(method = "createLevels",
            at = @At(value = "INVOKE",
                    target = "Ljava/util/Map;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", ordinal = 1),
            locals = LocalCapture.CAPTURE_FAILHARD)
    private void banner$initWorld(ChunkProgressListener chunkProgressListener, CallbackInfo ci,
                                   ServerLevelData serverLevelData, boolean bl, Registry registry,
                                   WorldOptions worldOptions, long l, long m, List list, LevelStem
                                           levelStem, ServerLevel serverLevel, DimensionDataStorage
                                           dimensionDataStorage, WorldBorder worldBorder,
                                   RandomSequences randomSequences, Iterator var16, Map.Entry entry,
                                   ResourceKey resourceKey, ResourceKey resourceKey2, DerivedLevelData derivedLevelData,
                                   ServerLevel serverLevel2) {
        banner$initLevel(serverLevel2);
        banner$initializedLevel(serverLevel2, derivedLevelData, worldData, worldOptions);
    }

    @Inject(method = "getServerModName", at = @At(value = "HEAD"), remap = false, cancellable = true)
    private void banner$setServerModName(CallbackInfoReturnable<String> cir) {
        if (this.server != null) {
            cir.setReturnValue(server.getName());
        }
    }

    @Override
    public boolean hasStopped() {
        synchronized (stopLock) {
            return hasStopped;
        }
    }

    @Override
    public void banner$setServer(CraftServer server) {
        this.server = server;
    }

    private static MinecraftServer getServer() {
        return Bukkit.getServer() instanceof CraftServer ? ((CraftServer) Bukkit.getServer()).getServer() : null;
    }

    @Override
    public void addLevel(ServerLevel level) {
        this.levels.put(level.dimension(), level);
    }

    @Override
    public void removeLevel(ServerLevel level) {
        ServerWorldEvents.UNLOAD.invoker().onWorldUnload(((MinecraftServer) (Object) this), level); // Banner
        this.levels.remove(level.dimension());
    }

    @Inject(method = "setInitialSpawn", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerChunkCache;getGenerator()Lnet/minecraft/world/level/chunk/ChunkGenerator;", shift = At.Shift.BEFORE), cancellable = true)
    private static void banner$spawnInit(ServerLevel level, ServerLevelData levelData, boolean generateBonusChest, boolean debug, CallbackInfo ci) {
        // CraftBukkit start
        if (level.bridge$generator() != null) {
            Random rand = new Random(level.getSeed());
            org.bukkit.Location spawn = level.bridge$generator().getFixedSpawnLocation(level.getWorld(), rand);

            if (spawn != null) {
                if (spawn.getWorld() != level.getWorld()) {
                    throw new IllegalStateException("Cannot set spawn point for " + levelData.getLevelName() + " to be in another world (" + spawn.getWorld().getName() + ")");
                } else {
                    levelData.setSpawn(new BlockPos(spawn.getBlockX(), spawn.getBlockY(), spawn.getBlockZ()), spawn.getYaw());
                    ci.cancel();
                }
            }
        }
    }

    @Override
    public void initWorld(ServerLevel serverWorld, ServerLevelData worldInfo, WorldData saveData, WorldOptions worldOptions) {
        banner$initLevel(serverWorld);
        WorldBorder worldborder = serverWorld.getWorldBorder();
        worldborder.applySettings(worldInfo.getWorldBorder());
        banner$initializedLevel(serverWorld, worldInfo, saveData, worldOptions);
    }

    private void banner$initLevel(ServerLevel serverWorld) {
        this.server.scoreboardManager = new CraftScoreboardManager((MinecraftServer) (Object) this, serverWorld.getScoreboard());

        if (serverWorld.bridge$generator() != null) {
            serverWorld.getWorld().getPopulators().addAll(
                    serverWorld.bridge$generator().getDefaultPopulators(
                            serverWorld.getWorld()));
        }
        Bukkit.getPluginManager().callEvent(new WorldInitEvent(serverWorld.getWorld()));
    }

    private void banner$initializedLevel(ServerLevel serverWorld, ServerLevelData worldInfo, WorldData saveData, WorldOptions worldOptions) {
        boolean flag = saveData.isDebugWorld();

        if (!worldInfo.isInitialized()) {
            try {
                setInitialSpawn(serverWorld, worldInfo, worldOptions.generateBonusChest(), flag);
                worldInfo.setInitialized(true);
                if (flag) {
                    this.setupDebugLevel(this.worldData);
                }
            } catch (Throwable throwable) {
                CrashReport crashreport = CrashReport.forThrowable(throwable, "Exception initializing level");
                try {
                    serverWorld.fillReportDetails(crashreport);
                } catch (Throwable throwable2) {
                    // empty catch block
                }
                throw new ReportedException(crashreport);
            }
            worldInfo.setInitialized(true);
        }
    }

    /**
     * @author wdog5
     * @reason bukkit
     */
    @Overwrite
    public final void prepareLevels(ChunkProgressListener listener) {
        ServerLevel worldserver = this.overworld();
        this.forceTicks = true;
        // CraftBukkit end

        LOGGER.info(BannerMCStart.I18N.as("server.region.prepare"), worldserver.dimension().location());
        BlockPos blockposition = worldserver.getSharedSpawnPos();

        listener.updateSpawnPos(new ChunkPos(blockposition));
        ServerChunkCache chunkproviderserver = worldserver.getChunkSource();

        this.nextTickTime = Util.getMillis();
        // CraftBukkit start
        if (worldserver.getWorld().getKeepSpawnInMemory()) {
            chunkproviderserver.addRegionTicket(TicketType.START, new ChunkPos(blockposition), 11, Unit.INSTANCE);

            while (chunkproviderserver.getTickingGenerated() != 441) {
                // this.nextTickTime = SystemUtils.getMillis() + 10L;
                this.executeModerately();
            }
        }

        // this.nextTickTime = SystemUtils.getMillis() + 10L;
        this.executeModerately();
        // Iterator iterator = this.levels.values().iterator();
        Iterator var5 = this.levels.values().iterator();

        while(true) {
            ServerLevel serverLevel2;
            ForcedChunksSavedData forcedChunksSavedData;
            do {
                if (!var5.hasNext()) {
                    // CraftBukkit start
                    // this.nextTickTime = SystemUtils.getMillis() + 10L;
                    this.executeModerately();
                    // CraftBukkit end
                    listener.stop();
                    // CraftBukkit start
                    // this.updateMobSpawningFlags();
                    worldserver.setSpawnSettings(this.isSpawningMonsters(), this.isSpawningAnimals());

                    this.forceTicks = false;
                    // CraftBukkit end
                    return;
                }

                serverLevel2 = (ServerLevel)var5.next();
                forcedChunksSavedData = (ForcedChunksSavedData)serverLevel2.getDataStorage().get(ForcedChunksSavedData.factory(), "chunks");
            } while(forcedChunksSavedData == null);

            LongIterator longIterator = forcedChunksSavedData.getChunks().iterator();

            while(longIterator.hasNext()) {
                long l = longIterator.nextLong();
                ChunkPos chunkPos = new ChunkPos(l);
                serverLevel2.getChunkSource().updateChunkForced(chunkPos, true);
            }

            worldserver.entityManager.tick(); // SPIGOT-6526: Load pending entities so they are available to the API // Banner - tick to sync chunks
            Bukkit.getPluginManager().callEvent(new WorldLoadEvent(serverLevel2.getWorld()));
        }
    }

    @Override
    public void prepareLevels(ChunkProgressListener listener, ServerLevel worldserver) {
        ServerWorldEvents.LOAD.invoker().onWorldLoad(((MinecraftServer) (Object) this), worldserver);// Banner
        // WorldServer worldserver = this.overworld();
        this.forceTicks = true;
        // CraftBukkit end

        LOGGER.info(BannerMCStart.I18N.as("server.region.prepare"), worldserver.dimension().location());
        BlockPos blockposition = worldserver.getSharedSpawnPos();

        listener.updateSpawnPos(new ChunkPos(blockposition));
        ServerChunkCache chunkproviderserver = worldserver.getChunkSource();

        this.nextTickTime = Util.getMillis();
        // CraftBukkit start
        if (worldserver.getWorld().getKeepSpawnInMemory()) {
            chunkproviderserver.addRegionTicket(TicketType.START, new ChunkPos(blockposition), 11, Unit.INSTANCE);

            while (chunkproviderserver.getTickingGenerated() != 441) {
                // this.nextTickTime = SystemUtils.getMillis() + 10L;
                this.executeModerately();
            }
        }

        // this.nextTickTime = SystemUtils.getMillis() + 10L;
        this.executeModerately();
        // Iterator iterator = this.levels.values().iterator();

        if (true) {
            ServerLevel worldserver1 = worldserver;
            // CraftBukkit end
            ForcedChunksSavedData forcedchunk = (ForcedChunksSavedData) worldserver1.getDataStorage().get(ForcedChunksSavedData.factory(), "chunks");

            if (forcedchunk != null) {
                LongIterator longiterator = forcedchunk.getChunks().iterator();

                while (longiterator.hasNext()) {
                    long i = longiterator.nextLong();
                    ChunkPos chunkcoordintpair = new ChunkPos(i);

                    worldserver1.getChunkSource().updateChunkForced(chunkcoordintpair, true);
                }
            }
        }

        // CraftBukkit start
        // this.nextTickTime = SystemUtils.getMillis() + 10L;
        this.executeModerately();
        // CraftBukkit end
        listener.stop();
        // CraftBukkit start
        // this.updateMobSpawningFlags();
        worldserver.setSpawnSettings(this.isSpawningMonsters(), this.isSpawningAnimals());

        this.forceTicks = false;
        // CraftBukkit end
    }

    @Override
    public void executeModerately() {
        this.runAllTasks();
        this.bridge$drainQueuedTasks();
        java.util.concurrent.locks.LockSupport.parkNanos("executing tasks", 1000L);
    }

    @Inject(method = "haveTime", cancellable = true, at = @At("HEAD"))
    private void banner$forceAheadOfTime(CallbackInfoReturnable<Boolean> cir) {
        if (this.forceTicks) cir.setReturnValue(true);
    }

    @Inject(method = "tickChildren", at = @At("HEAD"))
    private void banner$processStart(BooleanSupplier hasTimeLeft, CallbackInfo ci) {
        BukkitExtraConstants.currentTick = (int) (System.currentTimeMillis() / 50);
        server.getScheduler().mainThreadHeartbeat(this.tickCount);
        this.bridge$drainQueuedTasks();
    }

    @Inject(method = "tickServer", at = @At("HEAD"))
    private void banner$serverTickStartEvent(BooleanSupplier hasTimeLeft, CallbackInfo ci) {
        new com.destroystokyo.paper.event.server.ServerTickStartEvent(this.tickCount+1).callEvent(); // Paper
    }

    @Inject(method = "tickChildren",
            at = @At(value = "INVOKE",
            target = "Lnet/minecraft/util/profiling/ProfilerFiller;push(Ljava/lang/String;)V",
                    ordinal = 0))
    private void banner$mainThreadHeartbeat0(BooleanSupplier hasTimeLeft, CallbackInfo ci) {
        this.server.getScheduler().mainThreadHeartbeat(this.tickCount); // CraftBukkit
    }

    @Inject(method = "tickChildren",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/server/ServerFunctionManager;tick()V"))
    private void banner$mainThreadHeartbeat1(BooleanSupplier hasTimeLeft, CallbackInfo ci) {
        this.server.getScheduler().mainThreadHeartbeat(this.tickCount); // CraftBukkit
    }

    @Inject(method = "tickChildren",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/util/profiling/ProfilerFiller;popPush(Ljava/lang/String;)V",
                    ordinal = 0))
    private void banner$mainThreadHeartbeat2(BooleanSupplier hasTimeLeft, CallbackInfo ci) {
        this.server.getScheduler().mainThreadHeartbeat(this.tickCount); // CraftBukkit
    }

    @Inject(method = "tickServer",
            at = @At(value = "INVOKE",
            target = "Lnet/minecraft/util/profiling/ProfilerFiller;push(Ljava/lang/String;)V",
            ordinal = 1))
    private void banner$tickEndEvent(BooleanSupplier hasTimeLeft, CallbackInfo ci) {
        // Paper start
        long endTime = System.nanoTime();
        long remaining = (TICK_STATS_SPAN - (endTime - lastServerStatus)) - tickCount;
        new com.destroystokyo.paper.event.server.ServerTickEndEvent(this.tickCount, ((double)(endTime - lastServerStatus) / 1000000D), remaining).callEvent();
        // Paper end
    }

    @Inject(method = "tickChildren", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;getAllLevels()Ljava/lang/Iterable;"))
    private void banner$checkHeart(BooleanSupplier hasTimeLeft, CallbackInfo ci) {
        // CraftBukkit start
        // Run tasks that are waiting on processing
        while (!processQueue.isEmpty()) {
            processQueue.remove().run();
        }

        // Send time updates to everyone, it will get the right time from the world the player is in.
        if (this.tickCount % 20 == 0) {
            for (int i = 0; i < this.getPlayerList().players.size(); ++i) {
                ServerPlayer entityplayer = (ServerPlayer) this.getPlayerList().players.get(i);
                entityplayer.connection.send(new ClientboundSetTimePacket(entityplayer.level().getGameTime(), entityplayer.getPlayerTime(), entityplayer.level().getGameRules().getBoolean(GameRules.RULE_DAYLIGHT))); // Add support for per player time
            }
        }
    }

    // CraftBukkit start
    public final java.util.concurrent.ExecutorService chatExecutor = java.util.concurrent.Executors.newCachedThreadPool(
            new com.google.common.util.concurrent.ThreadFactoryBuilder().setDaemon(true).setNameFormat("Async Chat Thread - #%d").build());

    @ModifyReturnValue(method = "getChatDecorator", at = @At("RETURN"))
    private ChatDecorator banner$fireChatEvent(ChatDecorator decorator) {
        /*
        return (entityplayer, ichatbasecomponent) -> {
            // SPIGOT-7127: Console /say and similar
            if (entityplayer == null) {
                return CompletableFuture.completedFuture(ichatbasecomponent);
            }

            return CompletableFuture.supplyAsync(() -> {
                AsyncPlayerChatPreviewEvent event = new AsyncPlayerChatPreviewEvent(true, entityplayer.getBukkitEntity(), CraftChatMessage.fromComponent(ichatbasecomponent), new LazyPlayerSet(((MinecraftServer) (Object) this)));
                String originalFormat = event.getFormat(), originalMessage = event.getMessage();
                this.server.getPluginManager().callEvent(event);

                if (originalFormat.equals(event.getFormat()) && originalMessage.equals(event.getMessage()) && event.getPlayer().getName().equalsIgnoreCase(event.getPlayer().getDisplayName())) {
                    return ichatbasecomponent;
                }
                return CraftChatMessage.fromStringOrNull(String.format(event.getFormat(), event.getPlayer().getDisplayName(), event.getMessage()));
            }, chatExecutor);
        };*/
        return decorator;
    }

    @Inject(method = "method_29440", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/packs/repository/PackRepository;setSelected(Ljava/util/Collection;)V"))
    private void banner$syncCommands(Collection collection, MinecraftServer.ReloadableResources reloadableResources,
                                     CallbackInfo ci) {
        this.server.syncCommands(); // SPIGOT-5884: Lost on reload
    }

    @ModifyConstant(method = "spin", constant = @Constant(intValue = 8))
    private static int banner$configurePriority(int constant) {
        return BannerConfigUtil.serverThread();
    }

    // Banner start
    @Override
    public WorldLoader.DataLoadContext bridge$worldLoader() {
        return worldLoader;
    }

    @Override
    public CraftServer bridge$server() {
        return server;
    }

    @Override
    public OptionSet bridge$options() {
        return options;
    }

    @Override
    public ConsoleCommandSender bridge$console() {
        return console;
    }

    @Override
    public ConsoleReader bridge$reader() {
        return reader;
    }

    @Override
    public boolean bridge$forceTicks() {
        return forceTicks;
    }

    @Override
    public boolean isDebugging() {
        return false;
    }

    @Override
    public void banner$setConsole(ConsoleCommandSender console) {
        this.console = console;
    }

    // Banner end

    @Override
    public void bridge$drainQueuedTasks() {
        while (!processQueue.isEmpty()) {
            processQueue.remove().run();
        }
    }

    @Override
    public void bridge$queuedProcess(Runnable runnable) {
        processQueue.add(runnable);
    }

    @Override
    public Queue<Runnable> bridge$processQueue() {
        return processQueue;
    }

    @Override
    public void banner$setProcessQueue(Queue<Runnable> processQueue) {
        this.processQueue = processQueue;
    }


    @Override
    public Commands bridge$getVanillaCommands() {
        return this.vanillaCommandDispatcher;
    }

    @Override
    public java.util.concurrent.ExecutorService bridge$chatExecutor() {
        return chatExecutor;
    }

    @Override
    public boolean isSameThread() {
        return super.isSameThread() || this.isStopped(); // CraftBukkit - MC-142590
    }

    @Override
    public double[] getTPS() {
        return new double[] {
                tps1.getAverage(),
                tps5.getAverage(),
                tps15.getAverage()
        };
    }
}
