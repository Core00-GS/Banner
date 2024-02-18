/**
 * Automatically generated file, changes will be lost.
 */
package org.bukkit.craftbukkit.v1_20_R3.block.impl;

public final class CraftPiston extends org.bukkit.craftbukkit.v1_20_R3.block.data.CraftBlockData implements org.bukkit.block.data.type.Piston, org.bukkit.block.data.Directional {

    public CraftPiston() {
        super();
    }

    public CraftPiston(net.minecraft.world.level.block.state.BlockState state) {
        super(state);
    }

    // org.bukkit.craftbukkit.v1_20_R2.block.data.type.CraftPiston

    private static final net.minecraft.world.level.block.state.properties.BooleanProperty EXTENDED = getBoolean(net.minecraft.world.level.block.piston.PistonBaseBlock.class, "extended");

    @Override
    public boolean isExtended() {
        return get(EXTENDED);
    }

    @Override
    public void setExtended(boolean extended) {
        set(EXTENDED, extended);
    }

    // org.bukkit.craftbukkit.v1_20_R2.block.data.CraftDirectional

    private static final net.minecraft.world.level.block.state.properties.EnumProperty<?> FACING = getEnum(net.minecraft.world.level.block.piston.PistonBaseBlock.class, "facing");

    @Override
    public org.bukkit.block.BlockFace getFacing() {
        return get(FACING, org.bukkit.block.BlockFace.class);
    }

    @Override
    public void setFacing(org.bukkit.block.BlockFace facing) {
        set(FACING, facing);
    }

    @Override
    public java.util.Set<org.bukkit.block.BlockFace> getFaces() {
        return getValues(FACING, org.bukkit.block.BlockFace.class);
    }
}
