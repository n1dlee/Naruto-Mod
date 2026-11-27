package com.sekwah.narutomod.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.SupportType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;

public final class SummoningCircle {
    public static final int INSCRIBE = 12;
    private static final List<Integer> LEVELS = List.of(0, -1, 1, -2, 2);

    private SummoningCircle() {
    }

    public static Vec3 candidate(Vec3 player, Vec3 facing, double reach, int slot) {
        double angle = switch (slot) {
            case 1 -> 0.38;
            case 2 -> -0.38;
            default -> 0;
        };
        double c = Math.cos(angle);
        double s = Math.sin(angle);
        Vec3 direction = new Vec3(facing.x * c + facing.z * s, 0, facing.z * c - facing.x * s);
        return player.add(direction.scale(reach + (slot == 3 ? 1.65 : 0)));
    }

    public static @Nullable Vec3 find(ServerLevel level, Player owner, double width, double height) {
        Vec3 axis = KamuiStep.facing(owner.getYRot());
        for (int slot = 0; slot < 4; slot++) {
            Vec3 base = candidate(owner.position(), axis, 1.8 + width * 0.68, slot);
            for (int y : LEVELS) {
                Vec3 site = new Vec3(base.x, Math.floor(base.y) + y, base.z);
                if (legal(level, owner, site, width, height)) {
                    return site;
                }
            }
        }
        return null;
    }

    public static boolean legal(ServerLevel level, Player owner, Vec3 site, double width, double height) {
        var box = box(site, width, height);
        var border = level.getWorldBorder();
        if (level.isOutsideBuildHeight(BlockPos.containing(site.subtract(0, 1, 0)))
                || level.isOutsideBuildHeight(BlockPos.containing(site.add(0, height, 0)))
                || !KamuiStep.insideBorder(box, border.getMinX(), border.getMinZ(), border.getMaxX(), border.getMaxZ())
                || !level.hasChunksAt(BlockPos.containing(box.minX, box.minY, box.minZ), BlockPos.containing(box.maxX, box.maxY, box.maxZ))
                || !level.mayInteract(owner, BlockPos.containing(site))
                || !level.noCollision(box) || owner.getBoundingBox().intersects(box)) {
            return false;
        }
        int support = 0;
        double inset = width * 0.35;
        for (double x : new double[]{-inset, inset}) {
            for (double z : new double[]{-inset, inset}) {
                BlockPos pad = BlockPos.containing(site.add(x, 0, z));
                if (!level.mayInteract(owner, pad) || !level.getBlockState(pad).getFluidState().isEmpty()) {
                    return false;
                }
                if (footing(level, pad.below())) {
                    support++;
                }
            }
        }
        Vec3 rim = site.add(0, Math.min(1.3, height / 2), 0);
        return support >= 3 && footing(level, BlockPos.containing(site).below())
                && level.clip(new ClipContext(owner.getEyePosition(), rim, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, owner)).getType() == HitResult.Type.MISS;
    }

    private static boolean footing(ServerLevel level, BlockPos block) {
        var state = level.getBlockState(block);
        return state.getFluidState().isEmpty() && state.isFaceSturdy(level, block, Direction.UP, SupportType.CENTER);
    }

    public static AABB box(Vec3 at, double width, double height) {
        double pad = width * 0.5;
        return new AABB(at.x - pad, at.y, at.z - pad, at.x + pad, at.y + height, at.z + pad);
    }

    public enum Style { RHINO, TOAD, SNAKE, SLUG, MONKEY }

    public enum Stage { PREPARE, EMERGE, FADE, CANCEL }
}
