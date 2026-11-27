package com.sekwah.narutomod.util;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class FinalChakraGeometry {
    private FinalChakraGeometry() {
    }

    public static Vec3 nearest(AABB box, Vec3 point) {
        return new Vec3(Mth.clamp(point.x, box.minX, box.maxX), Mth.clamp(point.y, box.minY, box.maxY),
                Mth.clamp(point.z, box.minZ, box.maxZ));
    }

    public static double sphericalDistance(Vec3 origin, AABB box) {
        return origin.distanceTo(nearest(box, origin));
    }

    public static boolean enemy(Entity owner, LivingEntity target) {
        return target.isAlive() && !target.isSpectator()
                && (owner == null || !Faction.sameSide(owner, target) && !owner.isAlliedTo(target))
                && (!(owner instanceof Player attacker) || !(target instanceof Player victim) || attacker.canHarmPlayer(victim));
    }

    public static boolean loaded(ServerLevel level, Vec3 from, Vec3 to) {
        return level.hasChunksAt(BlockPos.containing(Math.min(from.x, to.x), Math.min(from.y, to.y), Math.min(from.z, to.z)),
                BlockPos.containing(Math.max(from.x, to.x), Math.max(from.y, to.y), Math.max(from.z, to.z)));
    }

    public static boolean clear(ServerLevel level, Entity shape, Vec3 from, Vec3 to) {
        return loaded(level, from, to) && level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, shape)).getType() == HitResult.Type.MISS;
    }
}
