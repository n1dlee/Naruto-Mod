package com.sekwah.narutomod.util;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;

/**
 * Shared "who am I looking at" raycast for the dojutsu techniques — genjutsu and the
 * Mangekyo signature jutsu all need the same thing: the nearest living entity along the
 * caster's line of sight, stopped by solid blocks. Extracted from SharinganGenjutsuAbility
 * so every eye technique resolves targets identically.
 */
public final class EyeTargeting {

    /** Half-width of the "beam" an entity must fall inside to count as looked-at. */
    private static final double AIM_TOLERANCE = 1.2D;

    private EyeTargeting() {
    }

    public static LivingEntity raycastLiving(Player player, double range) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        Vec3 end = eye.add(look.scale(range));

        // Walls block line of sight — you cannot catch what you cannot see.
        var blockHit = player.level().clip(
                new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 blockEnd = blockHit.getType() == HitResult.Type.MISS ? end : blockHit.getLocation();

        double maxDist = eye.distanceTo(blockEnd);
        AABB searchBox = new AABB(
                Math.min(eye.x, blockEnd.x) - 1, eye.y - 2, Math.min(eye.z, blockEnd.z) - 1,
                Math.max(eye.x, blockEnd.x) + 1, eye.y + 4, Math.max(eye.z, blockEnd.z) + 1);

        return player.level().getEntitiesOfClass(LivingEntity.class, searchBox,
                        e -> e != player && e.isAlive()).stream()
                .filter(e -> isOnAim(e, eye, look, maxDist))
                .min(Comparator.comparingDouble(e -> e.position().distanceTo(eye)))
                .orElse(null);
    }

    /** Every living entity inside a sphere around the caster, excluding the caster. */
    public static List<LivingEntity> livingAround(Player player, double radius) {
        return player.level().getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(radius), e -> e != player && e.isAlive());
    }

    private static boolean isOnAim(LivingEntity entity, Vec3 eye, Vec3 look, double maxDist) {
        Vec3 center = entity.position().add(0, entity.getBbHeight() * 0.5, 0);
        double projection = center.subtract(eye).dot(look);
        if (projection < 0 || projection > maxDist) {
            return false;
        }
        return center.distanceTo(eye.add(look.scale(projection))) <= AIM_TOLERANCE;
    }
}
