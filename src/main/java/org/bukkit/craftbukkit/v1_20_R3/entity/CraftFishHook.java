package org.bukkit.craftbukkit.v1_20_R3.entity;

import com.google.common.base.Preconditions;
import net.minecraft.core.BlockPos;
import org.bukkit.craftbukkit.v1_20_R3.CraftServer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FishHook;

public class CraftFishHook extends CraftProjectile implements FishHook {
    private double biteChance = -1;

    public CraftFishHook(CraftServer server, net.minecraft.world.entity.projectile.FishingHook entity) {
        super(server, entity);
    }

    @Override
    public net.minecraft.world.entity.projectile.FishingHook getHandle() {
        return (net.minecraft.world.entity.projectile.FishingHook) entity;
    }

    @Override
    public String toString() {
        return "CraftFishingHook";
    }

    @Override
    public int getMinWaitTime() {
        return getHandle().bridge$minWaitTime();
    }

    @Override
    public void setMinWaitTime(int minWaitTime) {
        Preconditions.checkArgument(minWaitTime >= 0 && minWaitTime <= this.getMaxWaitTime(), "The minimum wait time should be between %s and %s (the maximum wait time)", 0, this.getMaxWaitTime());
        net.minecraft.world.entity.projectile.FishingHook hook = getHandle();
        hook.banner$setMinWaitTime(minWaitTime);
    }

    @Override
    public int getMaxWaitTime() {
        return getHandle().bridge$maxWaitTime();
    }

    @Override
    public void setMaxWaitTime(int maxWaitTime) {
        Preconditions.checkArgument(maxWaitTime >= 0 && maxWaitTime >= this.getMinWaitTime(), "The maximum wait time should be between %s and %s (the minimum wait time)", 0, this.getMinWaitTime());
        net.minecraft.world.entity.projectile.FishingHook hook = getHandle();
        hook.banner$setMaxWaitTime(maxWaitTime);
    }

    @Override
    public void setWaitTime(int min, int max) {
        Preconditions.checkArgument(min >= 0 && max >= 0 && min <= max, "The minimum/maximum wait time should be higher than or equal to 0 and the minimum wait time");
        getHandle().banner$setMinWaitTime(min);
        getHandle().banner$setMaxWaitTime(max);
    }

    @Override
    public int getMinLureTime() {
        return getHandle().bridge$minLureTime();
    }

    @Override
    public void setMinLureTime(int minLureTime) {
        Preconditions.checkArgument(minLureTime >= 0 && minLureTime <= this.getMaxLureTime(), "The minimum lure time (%s) should be between 0 and %s (the maximum wait time)", minLureTime, this.getMaxLureTime());
        getHandle().banner$setMinLureTime(minLureTime);
    }

    @Override
    public int getMaxLureTime() {
        return getHandle().bridge$maxLureTime();
    }

    @Override
    public void setMaxLureTime(int maxLureTime) {
        Preconditions.checkArgument(maxLureTime >= 0 && maxLureTime >= this.getMinLureTime(), "The maximum lure time (%s) should be higher than or equal to 0 and %s (the minimum wait time)", maxLureTime, this.getMinLureTime());
        getHandle().banner$setMaxLureTime(maxLureTime);
    }

    @Override
    public void setLureTime(int min, int max) {
        Preconditions.checkArgument(min >= 0 && max >= 0 && min <= max, "The minimum/maximum lure time should be higher than or equal to 0 and the minimum wait time.");        getHandle().banner$setMinLureTime(min);
        getHandle().banner$setMaxLureTime(max);
    }

    @Override
    public float getMinLureAngle() {
        return getHandle().bridge$minLureAngle();
    }

    @Override
    public void setMinLureAngle(float minLureAngle) {
        Preconditions.checkArgument(minLureAngle <= this.getMaxLureAngle(), "The minimum lure angle (%s) should be less than %s (the maximum lure angle)", minLureAngle, this.getMaxLureAngle());
        getHandle().banner$setMinLureAnglee(minLureAngle);
    }

    @Override
    public float getMaxLureAngle() {
        return getHandle().bridge$maxLureAngle();
    }

    @Override
    public void setMaxLureAngle(float maxLureAngle) {
        Preconditions.checkArgument(maxLureAngle >= this.getMinLureAngle(), "The minimum lure angle (%s) should be less than %s (the maximum lure angle)", maxLureAngle, this.getMinLureAngle());
        getHandle().banner$setMaxLureAnglee(maxLureAngle);
    }

    @Override
    public void setLureAngle(float min, float max) {
        Preconditions.checkArgument(min <= max, "The minimum lure (%s) angle should be less than the maximum lure angle (%s)", min, max);
        getHandle().banner$setMinLureAnglee(min);
        getHandle().banner$setMaxLureAnglee(max);
    }

    @Override
    public boolean isSkyInfluenced() {
        return getHandle().bridge$skyInfluenced();
    }

    @Override
    public void setSkyInfluenced(boolean skyInfluenced) {
        getHandle().banner$setSkyInfluenced(skyInfluenced);
    }

    @Override
    public boolean isRainInfluenced() {
        return getHandle().bridge$rainInfluenced();
    }

    @Override
    public void setRainInfluenced(boolean rainInfluenced) {
        getHandle().banner$setRainInfluenced(rainInfluenced);
    }

    @Override
    public boolean getApplyLure() {
        return getHandle().bridge$applyLure();
    }

    @Override
    public void setApplyLure(boolean applyLure) {
        getHandle().banner$setApplyLure(applyLure);
    }

    @Override
    public double getBiteChance() {
        net.minecraft.world.entity.projectile.FishingHook hook = getHandle();

        if (this.biteChance == -1) {
            if (hook.level().isRainingAt(BlockPos.containing(hook.position()).offset(0, 1, 0))) {
                return 1 / 300.0;
            }
            return 1 / 500.0;
        }
        return this.biteChance;
    }

    @Override
    public void setBiteChance(double chance) {
        Preconditions.checkArgument(chance >= 0 && chance <= 1, "The bite chance must be between 0 and 1");
        this.biteChance = chance;
    }

    @Override
    public boolean isInOpenWater() {
        return this.getHandle().outOfWaterTime < 10 && this.getHandle().calculateOpenWater(this.getHandle().blockPosition()); // Paper - isOpenWaterFishing is only calculated when a "fish" is approaching the hook
    }

    @Override
    public Entity getHookedEntity() {
        net.minecraft.world.entity.Entity hooked = getHandle().hookedIn;
        return (hooked != null) ? hooked.getBukkitEntity() : null;
    }

    @Override
    public void setHookedEntity(Entity entity) {
        net.minecraft.world.entity.projectile.FishingHook hook = getHandle();

        hook.hookedIn = (entity != null) ? ((CraftEntity) entity).getHandle() : null;
        hook.getEntityData().set(net.minecraft.world.entity.projectile.FishingHook.DATA_HOOKED_ENTITY, hook.hookedIn != null ? hook.hookedIn.getId() + 1 : 0);
    }

    @Override
    public boolean pullHookedEntity() {
        net.minecraft.world.entity.projectile.FishingHook hook = getHandle();
        if (hook.hookedIn == null) {
            return false;
        }

        hook.pullEntity(hook.hookedIn);
        return true;
    }

    @Override
    public HookState getState() {
        return HookState.values()[getHandle().currentState.ordinal()];
    }
}
