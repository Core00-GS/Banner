package com.mohistmc.banner.injection.server;

import jline.console.ConsoleReader;
import joptsimple.OptionSet;
import net.minecraft.commands.Commands;
import net.minecraft.server.WorldLoader;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.progress.ChunkProgressListener;
import net.minecraft.server.rcon.RconConsoleSource;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.storage.ServerLevelData;
import net.minecraft.world.level.storage.WorldData;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.craftbukkit.v1_20_R3.CraftServer;

public interface InjectionMinecraftServer {

    default void bridge$drainQueuedTasks() {

    }

    default Commands bridge$getVanillaCommands() {
        return null;
    }

    default java.util.concurrent.ExecutorService bridge$chatExecutor() {
        return null;
    }

    default void bridge$queuedProcess(Runnable runnable) {

    }

    default java.util.Queue<Runnable> bridge$processQueue() {
        return null;
    }

    default void banner$setProcessQueue(java.util.Queue<Runnable> processQueue) {
    }

    default WorldLoader.DataLoadContext bridge$worldLoader() {
        return null;
    }

    default CraftServer bridge$server() {
        return null;
    }

    default void banner$setServer(CraftServer server) {
    }

    default OptionSet bridge$options() {
        return null;
    }

    default ConsoleCommandSender bridge$console() {
        return null;
    }

    default void banner$setConsole(ConsoleCommandSender console) {

    }

    default ConsoleReader bridge$reader() {
        return null;
    }

    default boolean bridge$forceTicks() {
        return false;
    }

    default boolean isDebugging() {
        return false;
    }

    default boolean hasStopped() {
        return false;
    }

    default void initWorld(ServerLevel serverWorld, ServerLevelData worldInfo, WorldData saveData, WorldOptions worldOptions) {
    }

    default void prepareLevels(ChunkProgressListener listener, ServerLevel serverWorld) {
    }

    default void addLevel(ServerLevel level) {
    }

    default void removeLevel(ServerLevel level) {
    }

    default void executeModerately() {
    }

    default double[] getTPS() {
        return new double[0];
    }

    default void banner$setRconConsoleSource(RconConsoleSource source) {

    }
}
