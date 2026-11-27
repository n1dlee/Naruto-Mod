package com.sekwah.narutomod.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.AABB;

public final class WoodHouseLayout {
    public static final int WINDUP = 22;

    private WoodHouseLayout() {
    }

    public static Site site(BlockPos caster, Direction aim, Vec3i template) {
        Rotation rotation = switch (aim) {
            case EAST -> Rotation.CLOCKWISE_90;
            case SOUTH -> Rotation.CLOCKWISE_180;
            case WEST -> Rotation.COUNTERCLOCKWISE_90;
            default -> Rotation.NONE;
        };
        boolean exchanged = aim.getAxis() == Direction.Axis.X;
        int width = exchanged ? template.getZ() : template.getX();
        int depth = exchanged ? template.getX() : template.getZ();
        boolean northSouth = aim.getAxis() == Direction.Axis.Z;
        int runway = ((northSouth ? depth : width) + 1) / 2 + 4;
        BlockPos corner = caster.relative(aim, runway).offset(-width / 2, 0, -depth / 2);
        return new Site(corner, new Vec3i(width, template.getY(), depth), rotation);
    }

    public static BlockPos rotated(BlockPos local, Vec3i template, Rotation rotation) {
        int x = local.getX();
        int z = local.getZ();
        return switch (rotation) {
            case NONE -> local;
            case CLOCKWISE_90 -> new BlockPos(template.getZ() - 1 - z, local.getY(), x);
            case CLOCKWISE_180 -> new BlockPos(template.getX() - 1 - x, local.getY(), template.getZ() - 1 - z);
            case COUNTERCLOCKWISE_90 -> new BlockPos(z, local.getY(), template.getX() - 1 - x);
        };
    }

    public static boolean allowedSize(Vec3i size) {
        return size.getX() >= 5 && size.getY() >= 4 && size.getZ() >= 5
                && size.getX() <= 24 && size.getY() <= 16 && size.getZ() <= 24;
    }

    public static double phase(double age, int begin, int duration) {
        double linear = Mth.clamp((age - begin) / duration, 0, 1);
        return linear * linear * (3 - 2 * linear);
    }

    public record Site(BlockPos corner, Vec3i size, Rotation rotation) {
        public AABB box() {
            return new AABB(this.corner, this.corner.offset(this.size));
        }
    }
}
