package com.mohistmc.banner.mixin.server;

import com.google.common.collect.ImmutableList;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mohistmc.banner.BannerMCStart;
import com.mohistmc.banner.bukkit.BukkitCaptures;
import com.mohistmc.banner.bukkit.BukkitExtraConstants;
import com.mohistmc.banner.injection.server.InjectionMinecraftServer;
import com.mojang.datafixers.DataFixer;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Lifecycle;
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
import net.minecraft.core.HolderLookup;
import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.ChatDecorator;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import net.minecraft.network.protocol.status.ServerStatus;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.RegistryLayer;
import net.minecraft.server.Services;
import net.minecraft.server.TickTask;
import net.minecraft.server.WorldLoader;
import net.minecraft.server.WorldStem;
import net.minecraft.server.bossevents.CustomBossEvents;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.dedicated.DedicatedServerProperties;
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
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.util.profiling.jfr.JvmProfiler;
import net.minecraft.util.thread.ReentrantBlockableEventLoop;
import net.minecraft.world.RandomSequences;
import net.minecraft.world.entity.ai.village.VillageSiege;
import net.minecraft.world.entity.npc.CatSpawner;
import net.minecraft.world.entity.npc.WanderingTraderSpawner;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.CustomSpawner;
import net.minecraft.world.level.ForcedChunksSavedData;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.PatrolSpawner;
import net.minecraft.world.level.levelgen.PhantomSpawner;
import net.minecraft.world.level.levelgen.WorldDimensions;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.storage.CommandStorage;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.PrimaryLevelData;
import net.minecraft.world.level.storage.ServerLevelData;
import net.minecraft.world.level.storage.WorldData;
import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.RemoteConsoleCommandSender;
import org.bukkit.craftbukkit.Main;
import org.bukkit.craftbukkit.v1_20_R1.CraftServer;
import org.bukkit.craftbukkit.v1_20_R1.SpigotTimings;
import org.bukkit.craftbukkit.v1_20_R1.util.CraftChatMessage;
import org.bukkit.craftbukkit.v1_20_R1.util.LazyPlayerSet;
import org.bukkit.event.player.AsyncPlayerChatPreviewEvent;
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.plugin.PluginLoadOrder;
import org.jetbrains.annotations.Nullable;
import org.spigotmc.WatchdogThread;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
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
import java.util.concurrent.CompletableFuture;
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
    @Shadow private long nextTickTime;
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

    @Shadow public abstract String getServerModName();

    @Shadow public abstract ModCheck getModdedStatus();

    @Shadow protected abstract void forceDifficulty();

    // CraftBukkit start
    public WorldLoader.DataLoadContext worldLoader;
    public org.bukkit.craftbukkit.v1_20_R1.CraftServer server;
    public OptionSet options;
    public org.bukkit.command.ConsoleCommandSender console;
    public org.bukkit.command.RemoteConsoleCommandSender remoteConsole;
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
    private static final int SAMPLE_INTERVAL = 100;
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
        this.worldLoader = BukkitCaptures.getDataLoadContext();
    }

    @Inject(method = "stopServer", at = @At(value = "INVOKE", remap = false, ordinal = 0, shift = At.Shift.AFTER, target = "Lorg/slf4j/Logger;info(Ljava/lang/String;)V"))
    public void banner$unloadPlugins(CallbackInfo ci) {
        if (this.server != null) {
            this.server.disablePlugins();
        }
    }

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

            Arrays.fill(recentTps, 20);
            long curTime, tickSection = Util.getMillis(), tickCount = 1;

            while (this.running) {
                long i = (curTime = Util.getMillis()) - this.nextTickTime;
                if (i > 2000L && this.nextTickTime - this.lastOverloadWarning >= 15000L) {
                    long j = i / 50L;

                    if (server.getWarnOnOverload()) {
                        LOGGER.warn("Can't keep up! Is the server overloaded? Running {}ms or {} ticks behind", i, j);
                    }

                    this.nextTickTime += j * 50L;
                    this.lastOverloadWarning = this.nextTickTime;
                }

                if (tickCount++ % SAMPLE_INTERVAL == 0) {
                    double currentTps = 1E3 / (curTime - tickSection) * SAMPLE_INTERVAL;
                    recentTps[0] = calcTps(recentTps[0], 0.92, currentTps); // 1/exp(5sec/1min)
                    recentTps[1] = calcTps(recentTps[1], 0.9835, currentTps); // 1/exp(5sec/5min)
                    recentTps[2] = calcTps(recentTps[2], 0.9945, currentTps); // 1/exp(5sec/15min)
                    tickSection = curTime;
                }

                BukkitExtraConstants.currentTick = (int) (System.currentTimeMillis() / 50);

                if (this.debugCommandProfilerDelayStart) {
                    this.debugCommandProfilerDelayStart = false;
                    this.debugCommandProfiler = new MinecraftServer.TimeProfiler(Util.getNanos(), this.tickCount);
                }

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
        } catch (Throwable throwable1) {
            LOGGER.error("Encountered an unexpected exception", throwable1);
            CrashReport crashreport = constructOrExtractCrashReport(throwable1);
            this.fillSystemReport(crashreport.getSystemReport());
            File file1 = new File(new File(this.getServerDirectory(), "crash-reports"), "crash-" + Util.getFilenameFormattedDateTime() + "-server.txt");
            if (crashreport.saveToFile(file1)) {
                LOGGER.error("This crash report has been saved to: {}", file1.getAbsolutePath());
            } else {
                LOGGER.error("We were unable to save this crash report to disk.");
            }

            this.onServerCrash(crashreport);
        } finally {
            try {
                this.stopped = true;
                this.stopServer();
            } catch (Throwable throwable) {
                LOGGER.error("Exception stopping the server", throwable);
            } finally {
                if (this.services.profileCache() != null) {
                    this.services.profileCache().clearExecutor();
                }
                WatchdogThread.doStop();
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

    @Inject(method = "stopServer", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/players/PlayerList;removeAll()V"))
    private void banner$stopThread(CallbackInfo ci) {
        try { Thread.sleep(100); } catch (InterruptedException ex) {} // CraftBukkit - SPIGOT-625 - give server at least a chance to send packets
    }

    @Inject(method = "getServerModName", at = @At(value = "HEAD"), remap = false, cancellable = true)
    private void banner$setServerModName(CallbackInfoReturnable<String> cir) {
        if (this.server != null) {
            cir.setReturnValue(server.getServer().getServerName());
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
        ServerWorldEvents.LOAD.invoker().onWorldLoad(((MinecraftServer) (Object) this), level); // Banner
        this.levels.put(level.dimension(), level);
    }

    @Override
    public void removeLevel(ServerLevel level) {
        ServerWorldEvents.UNLOAD.invoker().onWorldUnload(((MinecraftServer) (Object) this), level); // Banner
        this.levels.remove(level.dimension());
    }

    @Inject(method = "createLevels", at = @At("HEAD"), cancellable = true)
    private void banner$loadWorld0(ChunkProgressListener listener, CallbackInfo ci) {
       loadWorld0(storageSource.getLevelId());
       ci.cancel();
    }

    // Banner start - modify to bukkit like
    @Redirect(method = "loadLevel", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/storage/WorldData;setModdedInfo(Ljava/lang/String;Z)V"))
    private void banner$cancelModded(WorldData instance, String s, boolean b) { }

    @Redirect(method = "loadLevel", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/MinecraftServer;prepareLevels(Lnet/minecraft/server/level/progress/ChunkProgressListener;)V"))
    private void banner$cancelPrepareLevels(MinecraftServer instance, ChunkProgressListener listener) { }

    protected void loadWorld0(String s) {
        LevelStorageSource.LevelStorageAccess worldSession = this.storageSource;

        Registry<LevelStem> dimensions = this.registries.compositeAccess().registryOrThrow(Registries.LEVEL_STEM);
        for (LevelStem worldDimension : dimensions) {
            ResourceKey<LevelStem> dimensionKey = dimensions.getResourceKey(worldDimension).get();

            ServerLevel world;
            int dimension = 0;

            if (dimensionKey == LevelStem.NETHER) {
                if (isNetherEnabled()) {
                    dimension = -1;
                } else {
                    continue;
                }
            } else if (dimensionKey == LevelStem.END) {
                if (server.getAllowEnd()) {
                    dimension = 1;
                } else {
                    continue;
                }
            } else if (dimensionKey != LevelStem.OVERWORLD) {
                dimension = -999;
            }

            String worldType = (dimension == -999) ? dimensionKey.location().getNamespace() + "_" + dimensionKey.location().getPath() : org.bukkit.World.Environment.getEnvironment(dimension).toString().toLowerCase();
            String name = (dimensionKey == LevelStem.OVERWORLD) ? s : s + "_" + worldType;

            org.bukkit.generator.ChunkGenerator gen = this.server.getGenerator(name);
            org.bukkit.generator.BiomeProvider biomeProvider = this.server.getBiomeProvider(name);

            PrimaryLevelData worlddata;
            WorldLoader.DataLoadContext worldloader_a = this.worldLoader;
            Registry<LevelStem> iregistry = worldloader_a.datapackDimensions().registryOrThrow(Registries.LEVEL_STEM);
            DynamicOps<Tag> dynamicops = RegistryOps.create(NbtOps.INSTANCE, (HolderLookup.Provider) worldloader_a.datapackWorldgen());
            Pair<WorldData, WorldDimensions.Complete> pair = worldSession.getDataTag(dynamicops, worldloader_a.dataConfiguration(), iregistry, worldloader_a.datapackWorldgen().allRegistriesLifecycle());

            if (pair != null) {
                worlddata = (PrimaryLevelData) pair.getFirst();
            } else {
                LevelSettings worldsettings;
                WorldOptions worldoptions;
                WorldDimensions worlddimensions;

                if (this.isDemo()) {
                    worldsettings = MinecraftServer.DEMO_SETTINGS;
                    worldoptions = WorldOptions.DEMO_OPTIONS;
                    worlddimensions = WorldPresets.createNormalWorldDimensions(worldloader_a.datapackWorldgen());
                } else {
                    DedicatedServerProperties dedicatedserverproperties = ((DedicatedServer) (Object) this).getProperties();

                    worldsettings = new LevelSettings(dedicatedserverproperties.levelName, dedicatedserverproperties.gamemode, dedicatedserverproperties.hardcore, dedicatedserverproperties.difficulty, false, new GameRules(), worldloader_a.dataConfiguration());
                    worldoptions = options.has("bonusChest") ? dedicatedserverproperties.worldOptions.withBonusChest(true) : dedicatedserverproperties.worldOptions;
                    worlddimensions = dedicatedserverproperties.createDimensions(worldloader_a.datapackWorldgen());
                }

                WorldDimensions.Complete worlddimensions_b = worlddimensions.bake(iregistry);
                Lifecycle lifecycle = worlddimensions_b.lifecycle().add(worldloader_a.datapackWorldgen().allRegistriesLifecycle());

                worlddata = new PrimaryLevelData(worldsettings, worldoptions, worlddimensions_b.specialWorldProperty(), lifecycle);
            }
            worlddata.checkName(name); // CraftBukkit - Migration did not rewrite the level.dat; This forces 1.8 to take the last loaded world as respawn (in this case the end)
            if (options.has("forceUpgrade")) {
                net.minecraft.server.Main.forceUpgrade(worldSession, DataFixers.getDataFixer(), options.has("eraseCache"), () -> {
                    return true;
                }, iregistry);
            }

            PrimaryLevelData iworlddataserver = worlddata;
            boolean flag = worlddata.isDebugWorld();
            WorldOptions worldoptions = worlddata.worldGenOptions();
            long i = worldoptions.seed();
            long j = BiomeManager.obfuscateSeed(i);
            List<CustomSpawner> list = ImmutableList.of(new PhantomSpawner(), new PatrolSpawner(), new CatSpawner(), new VillageSiege(), new WanderingTraderSpawner(iworlddataserver));
            LevelStem worlddimension = (LevelStem) dimensions.get(dimensionKey);

            org.bukkit.generator.WorldInfo worldInfo = new org.bukkit.craftbukkit.v1_20_R1.generator.CraftWorldInfo(iworlddataserver, worldSession, org.bukkit.World.Environment.getEnvironment(dimension), worlddimension.type().value());
            if (biomeProvider == null && gen != null) {
                biomeProvider = gen.getDefaultBiomeProvider(worldInfo);
            }

            ResourceKey<net.minecraft.world.level.Level> worldKey = ResourceKey.create(Registries.DIMENSION, dimensionKey.location());

            if (dimensionKey == LevelStem.OVERWORLD) {
                this.worldData = worlddata;
                this.worldData.setGameType(((DedicatedServer) (Object) this).getProperties().gamemode); // From DedicatedServer.init

                ChunkProgressListener worldloadlistener = this.progressListenerFactory.create(11);

                world = new ServerLevel(((MinecraftServer) (Object) this), this.executor, worldSession, iworlddataserver, worldKey, worlddimension, worldloadlistener, flag, j, list, true, (RandomSequences) null);
                world.banner$setEnvironment(org.bukkit.World.Environment.getEnvironment(dimension));
                world.banner$setGenerator(gen);
                world.banner$setBiomeProvider(biomeProvider);
                DimensionDataStorage worldpersistentdata = world.getDataStorage();
                this.readScoreboard(worldpersistentdata);
                this.server.scoreboardManager = new org.bukkit.craftbukkit.v1_20_R1.scoreboard.CraftScoreboardManager(((MinecraftServer) (Object) this), world.getScoreboard());
                this.commandStorage = new CommandStorage(worldpersistentdata);
            } else {
                ChunkProgressListener worldloadlistener = this.progressListenerFactory.create(11);
                world = new ServerLevel(((MinecraftServer) (Object) this), this.executor, worldSession, iworlddataserver, worldKey, worlddimension, worldloadlistener, flag, j, ImmutableList.of(), true, this.overworld().getRandomSequences());
                world.banner$setEnvironment(org.bukkit.World.Environment.getEnvironment(dimension));
                world.banner$setGenerator(gen);
                world.banner$setBiomeProvider(biomeProvider);
            }

            worlddata.setModdedInfo(this.getServerModName(), this.getModdedStatus().shouldReportAsModified());
            this.initWorld(world, worlddata, worldData, worldoptions);

            this.addLevel(world);
            this.getPlayerList().addWorldborderListener(world);

            if (worlddata.getCustomBossEvents() != null) {
                this.getCustomBossEvents().load(worlddata.getCustomBossEvents());
            }
        }
        this.forceDifficulty();
        for (ServerLevel worldserver : ((MinecraftServer) (Object) this).getAllLevels()) {
            this.prepareLevels(worldserver.getChunkSource().chunkMap.progressListener, worldserver);
            worldserver.entityManager.tick(); // SPIGOT-6526: Load pending entities so they are available to the API
            this.server.getPluginManager().callEvent(new org.bukkit.event.world.WorldLoadEvent(worldserver.getWorld()));
        }

        this.server.enablePlugins(org.bukkit.plugin.PluginLoadOrder.POSTWORLD);
        this.server.getPluginManager().callEvent(new ServerLoadEvent(ServerLoadEvent.LoadType.STARTUP));
        this.connection.acceptConnections();
    }
    // CraftBukkit end

    /*
    @Inject(method = "loadLevel", at = @At("TAIL"))
    private void banner$initPlugins(CallbackInfo ci) {
        for (ServerLevel worldserver : ((MinecraftServer)(Object)this).getAllLevels()) {
            this.prepareLevels(worldserver.getChunkSource().chunkMap.progressListener, worldserver);
            worldserver.entityManager.tick(); // SPIGOT-6526: Load pending entities so they are available to the API
            this.server.getPluginManager().callEvent(new WorldLoadEvent(worldserver.getWorld()));
        }
        this.server.enablePlugins(PluginLoadOrder.POSTWORLD);
        this.server.getPluginManager().callEvent(new ServerLoadEvent(ServerLoadEvent.LoadType.STARTUP));
        this.connection.acceptConnections();
    }*/
    // Banner end

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
        boolean flag = saveData.isDebugWorld();
        if ((serverWorld.bridge$generator() != null)) {
            serverWorld.getWorld().getPopulators().addAll(
                    serverWorld.bridge$generator().getDefaultPopulators(
                            (serverWorld.getWorld())));
        }
        WorldBorder worldborder = serverWorld.getWorldBorder();
        worldborder.applySettings(worldInfo.getWorldBorder());
        this.server.getPluginManager().callEvent(new org.bukkit.event.world.WorldInitEvent(serverWorld.getWorld())); // CraftBukkit - SPIGOT-5569: Call WorldInitEvent before any chunks are generated

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

    @Override
    public void prepareLevels(ChunkProgressListener listener, ServerLevel serverWorld) {
        ServerWorldEvents.LOAD.invoker().onWorldLoad(((MinecraftServer) (Object) this), serverWorld);// Banner
        if (!serverWorld.getWorld().getKeepSpawnInMemory()) {
            return;
        }
        this.forceTicks = true;
        LOGGER.info(BannerMCStart.I18N.get("server.region.prepare"), serverWorld.dimension().location());
        BlockPos blockpos = serverWorld.getSharedSpawnPos();
        listener.updateSpawnPos(new ChunkPos(blockpos));
        ServerChunkCache serverchunkprovider = serverWorld.getChunkSource();
        this.nextTickTime = Util.getMillis();
        serverchunkprovider.addRegionTicket(TicketType.START, new ChunkPos(blockpos), 11, Unit.INSTANCE);

        while (serverchunkprovider.getTickingGenerated() < 441) {
            this.executeModerately();
        }

        this.executeModerately();

        ForcedChunksSavedData forcedchunkssavedata = serverWorld.getDataStorage().get(ForcedChunksSavedData::load, "chunks");
        if (forcedchunkssavedata != null) {
            LongIterator longiterator = forcedchunkssavedata.getChunks().iterator();

            while (longiterator.hasNext()) {
                long i = longiterator.nextLong();
                ChunkPos chunkpos = new ChunkPos(i);
                serverWorld.getChunkSource().updateChunkForced(chunkpos, true);
            }
        }

        this.executeModerately();

        listener.stop();
        // this.updateMobSpawningFlags();
        serverWorld.setSpawnSettings(this.isSpawningMonsters(), this.isSpawningAnimals());
        this.forceTicks = false;

    }

    @Inject(method = "saveAllChunks",
            at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/players/PlayerList;getSingleplayerData()Lnet/minecraft/nbt/CompoundTag;",
            shift = At.Shift.AFTER))
    private void banner$saveAllLevel(boolean suppressLog, boolean flush, boolean forced,
                                     CallbackInfoReturnable<Boolean> cir) {
        // Banner start - Save level.dat to all plugin world
        for (ServerLevel banner$level : this.levels.values()) {
            if (banner$level.bridge$convertable() != this.storageSource) {
                banner$level.bridge$serverLevelDataCB().setWorldBorder(banner$level.serverLevelData.getWorldBorder());
                banner$level.bridge$serverLevelDataCB().setCustomBossEvents(this.getCustomBossEvents().save());
                banner$level.bridge$convertable().saveDataTag(this.registryAccess(), this.worldData,
                        this.getPlayerList().getSingleplayerData());
            }
        }
        // Banner end
    }

    @Override
    public void executeModerately() {
        this.runAllTasks();
        java.util.concurrent.locks.LockSupport.parkNanos("executing tasks", 1000L);
    }

    @Inject(method = "haveTime", cancellable = true, at = @At("HEAD"))
    private void banner$forceAheadOfTime(CallbackInfoReturnable<Boolean> cir) {
        if (this.forceTicks) cir.setReturnValue(true);
    }

    @Inject(method = "tickChildren", at = @At("HEAD"))
    private void banner$processStart(BooleanSupplier hasTimeLeft, CallbackInfo ci) {
        server.getScheduler().mainThreadHeartbeat(this.tickCount);
    }

    @Inject(method = "tickServer", at = @At("HEAD"))
    private void banner$useTimings(BooleanSupplier hasTimeLeft, CallbackInfo ci) {
        SpigotTimings.serverTickTimer.startTiming(); // Spigot
        new com.destroystokyo.paper.event.server.ServerTickStartEvent(this.tickCount+1).callEvent(); // Paper
    }

    @Inject(method = "tickServer", at = @At(value = "INVOKE",
            target = "Lorg/slf4j/Logger;debug(Ljava/lang/String;)V",
            ordinal = 0,
            remap = false))
    private void banner$useTimings0(BooleanSupplier hasTimeLeft, CallbackInfo ci) {
        SpigotTimings.worldSaveTimer.startTiming(); // Spigot
    }

    @Inject(method = "tickServer", at = @At(value = "INVOKE",
            target = "Lorg/slf4j/Logger;debug(Ljava/lang/String;)V",
            ordinal = 1,
            shift = At.Shift.AFTER,
            remap = false))
    private void banner$useTimings1(BooleanSupplier hasTimeLeft, CallbackInfo ci) {
        SpigotTimings.worldSaveTimer.stopTiming(); // Spigot
    }

    @Inject(method = "tickServer", at = @At("TAIL"))
    private void banner$useTimings2(BooleanSupplier hasTimeLeft, CallbackInfo ci) {
        SpigotTimings.serverTickTimer.stopTiming(); // Spigot
        org.spigotmc.CustomTimingsHandler.tick(); // Spigot
    }

    @Inject(method = "tickChildren", at = @At("HEAD"))
    private void banner$addTimings(BooleanSupplier hasTimeLeft, CallbackInfo ci) {
        SpigotTimings.schedulerTimer.startTiming(); // Spigot
    }

    @Inject(method = "tickChildren",
            at = @At(value = "INVOKE",
            target = "Lnet/minecraft/util/profiling/ProfilerFiller;push(Ljava/lang/String;)V",
                    ordinal = 0))
    private void banner$addTimings0(BooleanSupplier hasTimeLeft, CallbackInfo ci) {
        this.server.getScheduler().mainThreadHeartbeat(this.tickCount); // CraftBukkit
        SpigotTimings.schedulerTimer.stopTiming(); // Spigot
    }

    @Inject(method = "tickChildren",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/server/ServerFunctionManager;tick()V"))
    private void banner$addTimings1(BooleanSupplier hasTimeLeft, CallbackInfo ci) {
        this.server.getScheduler().mainThreadHeartbeat(this.tickCount); // CraftBukkit
        SpigotTimings.commandFunctionsTimer.startTiming(); // Spigot
    }

    @Inject(method = "tickChildren",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/util/profiling/ProfilerFiller;popPush(Ljava/lang/String;)V",
                    ordinal = 0))
    private void banner$addTimings2(BooleanSupplier hasTimeLeft, CallbackInfo ci) {
        this.server.getScheduler().mainThreadHeartbeat(this.tickCount); // CraftBukkit
        SpigotTimings.commandFunctionsTimer.stopTiming(); // Spigot
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
        SpigotTimings.processQueueTimer.startTiming(); // Spigot
        while (!processQueue.isEmpty()) {
            processQueue.remove().run();
        }
        SpigotTimings.processQueueTimer.stopTiming(); // Spigot

        SpigotTimings.timeUpdateTimer.startTiming(); // Spigot
        // Send time updates to everyone, it will get the right time from the world the player is in.
        if (this.tickCount % 20 == 0) {
            for (int i = 0; i < this.getPlayerList().players.size(); ++i) {
                ServerPlayer entityplayer = (ServerPlayer) this.getPlayerList().players.get(i);
                entityplayer.connection.send(new ClientboundSetTimePacket(entityplayer.level().getGameTime(), entityplayer.getPlayerTime(), entityplayer.level().getGameRules().getBoolean(GameRules.RULE_DAYLIGHT))); // Add support for per player time
            }
        }
        SpigotTimings.timeUpdateTimer.stopTiming(); // Spigot
    }

    @Inject(method = "tickChildren",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerLevel;tick(Ljava/util/function/BooleanSupplier;)V"),
            locals = LocalCapture.CAPTURE_FAILHARD)
    private void banner$addTimings3(BooleanSupplier hasTimeLeft, CallbackInfo ci,
                                    Iterator var2, ServerLevel serverLevel) {
        serverLevel.bridge$timings().doTick.startTiming(); // Spigot
    }

    @Inject(method = "tickChildren",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerLevel;tick(Ljava/util/function/BooleanSupplier;)V",
                    shift = At.Shift.AFTER),
            locals = LocalCapture.CAPTURE_FAILHARD)
    private void banner$addTimings4(BooleanSupplier hasTimeLeft, CallbackInfo ci,
                                    Iterator var2, ServerLevel serverLevel) {
        serverLevel.bridge$timings().doTick.stopTiming(); // Spigot
    }

    @Inject(method = "tickChildren",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/server/network/ServerConnectionListener;tick()V"))
    private void banner$addTimings4(BooleanSupplier hasTimeLeft, CallbackInfo ci) {
        SpigotTimings.connectionTimer.startTiming(); // Spigot
    }

    @Inject(method = "tickChildren",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/util/profiling/ProfilerFiller;popPush(Ljava/lang/String;)V",
                    ordinal = 2))
    private void banner$addTimings5(BooleanSupplier hasTimeLeft, CallbackInfo ci) {
        SpigotTimings.connectionTimer.stopTiming(); // Spigot
    }

    @Inject(method = "tickChildren",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/server/players/PlayerList;tick()V"))
    private void banner$addTimings6(BooleanSupplier hasTimeLeft, CallbackInfo ci) {
        SpigotTimings.playerListTimer.startTiming(); // Spigot
    }

    @Inject(method = "tickChildren",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/server/players/PlayerList;tick()V",
                    shift = At.Shift.AFTER))
    private void banner$addTimings7(BooleanSupplier hasTimeLeft, CallbackInfo ci) {
        SpigotTimings.playerListTimer.stopTiming(); // Spigot
    }

    @Inject(method = "tickChildren",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/util/profiling/ProfilerFiller;popPush(Ljava/lang/String;)V",
                    shift = At.Shift.AFTER,
                    ordinal = 3))
    private void banner$addTimings8(BooleanSupplier hasTimeLeft, CallbackInfo ci) {
        SpigotTimings.tickablesTimer.startTiming(); // Spigot
    }

    @Inject(method = "tickChildren",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/util/profiling/ProfilerFiller;pop()V",
                    ordinal = 3))
    private void banner$addTimings9(BooleanSupplier hasTimeLeft, CallbackInfo ci) {
        SpigotTimings.tickablesTimer.stopTiming(); // Spigot
    }

    // CraftBukkit start
    public final java.util.concurrent.ExecutorService chatExecutor = java.util.concurrent.Executors.newCachedThreadPool(
            new com.google.common.util.concurrent.ThreadFactoryBuilder().setDaemon(true).setNameFormat("Async Chat Thread - #%d").build());

    @ModifyReturnValue(method = "getChatDecorator", at = @At("RETURN"))
    private ChatDecorator banner$fireChatEvent(ChatDecorator decorator) {
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
        };
    }

    @Inject(method = "method_29440", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/packs/repository/PackRepository;setSelected(Ljava/util/Collection;)V"))
    private void banner$syncCommands(Collection collection, MinecraftServer.ReloadableResources reloadableResources,
                                     CallbackInfo ci) {
        this.server.syncCommands(); // SPIGOT-5884: Lost on reload
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
    public RemoteConsoleCommandSender bridge$remoteConsole() {
        return remoteConsole;
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
    public void banner$setRemoteConsole(RemoteConsoleCommandSender remoteConsole) {
        this.remoteConsole = remoteConsole;
    }

    @Override
    public void banner$setConsole(ConsoleCommandSender console) {
        this.console = console;
    }

    // Banner end


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
}
