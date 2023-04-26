package com.mohistmc.banner.mixin.world.level;

import com.mohistmc.banner.injection.world.level.InjectionLevel;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_19_R3.CraftServer;
import org.bukkit.craftbukkit.v1_19_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_19_R3.block.CapturedBlockState;
import org.bukkit.craftbukkit.v1_19_R3.block.data.CraftBlockData;
import org.bukkit.entity.SpawnCategory;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.generator.ChunkGenerator;
import org.jetbrains.annotations.Nullable;
import org.spigotmc.SpigotWorldConfig;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Mixin(Level.class)
public abstract class MixinLevel implements LevelAccessor, AutoCloseable, InjectionLevel {

    @Shadow public abstract void setBlocksDirty(BlockPos blockPos, net.minecraft.world.level.block.state.BlockState oldState, net.minecraft.world.level.block.state.BlockState newState);

    @Shadow @Final public boolean isClientSide;

    @Shadow public abstract void sendBlockUpdated(BlockPos pos, BlockState oldState, BlockState newState, int flags);

    @Shadow public abstract void updateNeighbourForOutputSignal(BlockPos pos, Block block);

    @Shadow public abstract void onBlockStateChange(BlockPos pos, BlockState blockState, BlockState newState);

    @Shadow @Final public Thread thread;

    @Shadow public abstract LevelChunk getChunkAt(BlockPos pos);

    @Shadow @Nullable public abstract BlockEntity getBlockEntity(BlockPos pos);

    private CraftWorld world;
    public boolean pvpMode;
    public boolean keepSpawnInMemory = true;
    public org.bukkit.generator.ChunkGenerator generator;

    public boolean preventPoiUpdated = false; // CraftBukkit - SPIGOT-5710
    public boolean captureBlockStates = false;
    public boolean captureTreeGeneration = false;
    public Map<BlockPos, CapturedBlockState> capturedBlockStates = new java.util.LinkedHashMap<>();
    public Map<BlockPos, BlockEntity> capturedTileEntities = new HashMap<>();
    public List<ItemEntity> captureDrops;
    public final it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap<SpawnCategory> ticksPerSpawnCategory = new it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap<>();
    public boolean populating;
    private org.spigotmc.SpigotWorldConfig spigotConfig; // Spigot

    @Override
    public CraftWorld getWorld() {
        return this.world;
    }

    @Override
    public CraftServer getCraftServer() {
        return (CraftServer) Bukkit.getServer();
    }

    @Override
    public SpigotWorldConfig bridge$spigotConfig() {
        return spigotConfig;
    }

    @Override
    public void notifyAndUpdatePhysics(BlockPos blockposition, LevelChunk chunk, BlockState oldBlock, BlockState newBlock, BlockState actualBlock, int i, int j) {
        BlockState iblockdata = newBlock;
        BlockState iblockdata1 = oldBlock;
        BlockState iblockdata2 = actualBlock;
        if (iblockdata2 == iblockdata) {
            if (iblockdata1 != iblockdata2) {
                this.setBlocksDirty(blockposition, iblockdata1, iblockdata2);
            }

            if ((i & 2) != 0 && (!this.isClientSide || (i & 4) == 0) && (this.isClientSide || chunk == null || (chunk.getFullStatus() != null && chunk.getFullStatus().isOrAfter(ChunkHolder.FullChunkStatus.TICKING)))) { // allow chunk to be null here as chunk.isReady() is false when we send our notification during block placement
                this.sendBlockUpdated(blockposition, iblockdata1, iblockdata, i);
            }

            if ((i & 1) != 0) {
                this.blockUpdated(blockposition, iblockdata1.getBlock());
                if (!this.isClientSide && iblockdata.hasAnalogOutputSignal()) {
                    this.updateNeighbourForOutputSignal(blockposition, newBlock.getBlock());
                }
            }

            if ((i & 16) == 0 && j > 0) {
                int k = i & -34;

                iblockdata1.updateIndirectNeighbourShapes(this, blockposition, k, j - 1); // Don't call an event for the old block to limit event spam
                if (((Level) (Object) this) instanceof final ServerLevel serverLevel) {
                    CraftWorld world = serverLevel.getWorld();
                    if (world != null) {
                        BlockPhysicsEvent event = new BlockPhysicsEvent(world.getBlockAt(blockposition.getX(), blockposition.getY(), blockposition.getZ()), CraftBlockData.fromData(iblockdata));
                        this.getCraftServer().getPluginManager().callEvent(event);

                        if (event.isCancelled()) {
                            return;
                        }
                    }

                    iblockdata.updateNeighbourShapes(this, blockposition, k, j - 1);
                    iblockdata.updateIndirectNeighbourShapes(this, blockposition, k, j - 1);
                }

                if (!preventPoiUpdated) {
                    this.onBlockStateChange(blockposition, iblockdata1, iblockdata2);
                }
            }
        }
    }

    @Override
    public BlockEntity getBlockEntity(BlockPos blockposition, boolean validate) {
        if (capturedTileEntities.containsKey(blockposition)) {
            return capturedTileEntities.get(blockposition);
        }
        if (this.isOutsideBuildHeight(blockposition)) {
            return null;
        } else {
            return !this.isClientSide && Thread.currentThread() != this.thread ? null : this.getChunkAt(blockposition).getBlockEntity(blockposition, LevelChunk.EntityCreationType.IMMEDIATE);
        }
    }

    @Override
    public boolean bridge$pvpMode() {
        return this.pvpMode;
    }

    @Override
    public void banner$setPvpMode(boolean pvpMode) {
        this.pvpMode = pvpMode;
    }

    @Override
    public boolean bridge$captureBlockStates() {
        return this.captureBlockStates;
    }

    @Override
    public void banner$setCaptureBlockStates(boolean captureState) {
        this.captureBlockStates = captureState;
    }

    @Override
    public boolean bridge$captureTreeGeneration() {
        return this.captureTreeGeneration;
    }

    @Override
    public void banner$setCaptureTreeGeneration(boolean treeGeneration) {
        this.captureTreeGeneration = treeGeneration;
    }

    @Override
    public Map<BlockPos, CapturedBlockState> bridge$capturedBlockStates() {
        return this.capturedBlockStates;
    }

    @Override
    public Map<BlockPos, BlockEntity> bridge$capturedTileEntities() {
        return capturedTileEntities;
    }

    @Override
    public void banner$setCapturedTileEntities(Map<BlockPos, BlockEntity> tileEntities) {
        this.capturedTileEntities = tileEntities;
    }

    @Override
    public void banner$setCapturedBlockStates(Map<BlockPos, CapturedBlockState> capturedBlockStates) {
        this.capturedBlockStates = capturedBlockStates;
    }

    @Override
    public List<ItemEntity> bridge$captureDrops() {
        return this.captureDrops;
    }

    @Override
    public void banner$setCaptureDrops(List<ItemEntity> captureDrops) {
        this.captureDrops = captureDrops;
    }

    @Override
    public Object2LongOpenHashMap<SpawnCategory> bridge$ticksPerSpawnCategory() {
        return this.ticksPerSpawnCategory;
    }

    @Override
    public boolean bridge$populating() {
        return populating;
    }

    @Override
    public void banner$setPopulating(boolean populating) {
        this.populating = populating;
    }

    @Override
    public boolean bridge$KeepSpawnInMemory() {
        return keepSpawnInMemory;
    }

    @Override
    public void banner$setKeepSpawnInMemory(boolean keepSpawnInMemory) {
        this.keepSpawnInMemory = keepSpawnInMemory;
    }

    @Override
    public ChunkGenerator bridge$generator() {
        return generator;
    }

    @Override
    public void banner$setGenerator(ChunkGenerator generator) {
        this.generator = generator;
    }
}

