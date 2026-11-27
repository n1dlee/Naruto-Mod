package com.sekwah.narutomod.util;

import com.sekwah.narutomod.entity.projectile.HiraishinKunaiEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.List;

public final class HiraishinTargeting {
    private static final double KUNAI_RANGE = 128;
    private static final double AIM_RANGE = 48;

    private HiraishinTargeting() {
    }

    public enum Kind { KUNAI, CREATURE, GROUND }

    public record Glyph(Kind kind, Vec3 point, Direction face, int entityId) {
    }

    public record Landing(Vec3 feet, Glyph glyph) {
    }

    @Nullable
    public static HiraishinFormula.Ground markGround(ServerPlayer actor, HiraishinFormula.Anchor anchor) {
        ServerLevel level = actor.serverLevel();
        Vec3 feet = actor.position();
        BlockHitResult support = floor(actor, feet.add(0, 0.10, 0), 0.23);
        if (!actor.onGround() || support == null || !level.mayInteract(actor, support.getBlockPos())) {
            return null;
        }
        BlockPos pos = support.getBlockPos();
        var ground = new HiraishinFormula.Ground(anchor, pos, BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock()));
        return groundLanding(actor, ground) != null ? ground : null;
    }

    @Nullable
    public static Landing choose(ServerPlayer actor, HiraishinFormula.Book book) {
        List<HiraishinKunaiEntity> blades = blades(actor);
        HiraishinKunaiEntity aimed = aimedBlade(actor, blades);
        if (aimed != null) {
            return bladeLanding(actor, aimed);
        }
        for (HiraishinKunaiEntity blade : blades) {
            if (!blade.isPlanted()) {
                continue;
            }
            Landing landing = bladeLanding(actor, blade);
            if (landing != null) {
                return landing;
            }
        }
        Landing brand = brandLanding(actor, book.brand());
        return brand != null ? brand : groundLanding(actor, book.ground());
    }

    public static boolean safe(ServerPlayer actor, Vec3 feet) {
        ServerLevel level = actor.serverLevel();
        BlockHitResult support = floor(actor, feet.add(0, 0.035, 0), 0.07);
        return support != null && level.mayInteract(actor, support.getBlockPos())
                && KamuiStep.safe(level, actor, feet);
    }

    @Nullable
    public static Vec3 supportedNear(ServerPlayer actor, Vec3 sample) {
        BlockHitResult floor = floor(actor, sample.add(0, 0.68, 0), 1.36);
        return floor != null && safe(actor, floor.getLocation()) ? floor.getLocation() : null;
    }

    private static List<HiraishinKunaiEntity> blades(ServerPlayer actor) {
        return actor.serverLevel().getEntitiesOfClass(HiraishinKunaiEntity.class, actor.getBoundingBox().inflate(KUNAI_RANGE),
                        blade -> blade.isAlive() && blade.isFormulaFor(actor)
                                && blade.distanceToSqr(actor) <= KUNAI_RANGE * KUNAI_RANGE)
                .stream().sorted(Comparator.<HiraishinKunaiEntity>comparingDouble(actor::distanceToSqr)
                        .thenComparing(HiraishinKunaiEntity::getUUID)).toList();
    }

    @Nullable
    private static HiraishinKunaiEntity aimedBlade(ServerPlayer actor, List<HiraishinKunaiEntity> blades) {
        if (blades.isEmpty()) {
            return null;
        }
        Vec3 eye = actor.getEyePosition();
        Vec3 end = eye.add(actor.getLookAngle().scale(AIM_RANGE));
        Vec3 visible = visibleEnd(actor, eye, end);
        if (visible == null) {
            return null;
        }
        for (HiraishinKunaiEntity blade : blades) {
            if (blade.getBoundingBox().inflate(0.30).clip(eye, visible).isPresent()) {
                return blade;
            }
        }
        return null;
    }

    @Nullable
    private static Landing bladeLanding(ServerPlayer actor, HiraishinKunaiEntity blade) {
        Vec3 point = blade.position();
        Direction face = blade.isPlanted() ? blade.impactFace() : Direction.UP;
        Vec3 offset = Vec3.atLowerCornerOf(face.getNormal()).scale(0.66);
        return near(actor, new Glyph(Kind.KUNAI, point, face, -1), point.add(offset));
    }

    @Nullable
    private static Landing brandLanding(ServerPlayer actor, @Nullable HiraishinFormula.Brand brand) {
        if (brand == null || !brand.anchor().belongsTo(actor.getUUID(), actor.level().dimension().location())
                || !(actor.serverLevel().getEntity(brand.target()) instanceof LivingEntity target)
                || !target.isAlive() || target.isSpectator()) {
            return null;
        }
        Direction facing = target.getDirection();
        Vec3 front = Vec3.atLowerCornerOf(facing.getNormal());
        Vec3 side = Vec3.atLowerCornerOf(facing.getClockWise().getNormal());
        double clearance = (target.getBbWidth() + actor.getBbWidth()) * 0.5 + 0.36;
        Glyph glyph = new Glyph(Kind.CREATURE, target.position().add(0, target.getBbHeight() * 0.72, 0), facing, target.getId());
        return near(actor, glyph, target.position().subtract(front.scale(clearance)),
                target.position().add(side.scale(clearance)), target.position().subtract(side.scale(clearance)));
    }

    @Nullable
    private static Landing groundLanding(ServerPlayer actor, @Nullable HiraishinFormula.Ground ground) {
        if (ground == null || !ground.anchor().belongsTo(actor.getUUID(), actor.level().dimension().location())) {
            return null;
        }
        ServerLevel level = actor.serverLevel();
        BlockPos support = ground.support();
        if (!level.hasChunkAt(support) || !BuiltInRegistries.BLOCK.getKey(level.getBlockState(support).getBlock()).equals(ground.block())) {
            return null;
        }
        BlockHitResult hit = floor(actor, Vec3.atBottomCenterOf(support).add(0, 1.53, 0), 1.54);
        if (hit == null || !hit.getBlockPos().equals(support) || !safe(actor, hit.getLocation())) {
            return null;
        }
        return new Landing(hit.getLocation(), new Glyph(Kind.GROUND, hit.getLocation(), Direction.UP, -1));
    }

    @Nullable
    private static Landing near(ServerPlayer actor, Glyph glyph, Vec3... samples) {
        for (Vec3 sample : samples) {
            BlockHitResult hit = floor(actor, sample.add(0, 0.8, 0), 2.6);
            if (hit != null && safe(actor, hit.getLocation())) {
                return new Landing(hit.getLocation(), glyph);
            }
        }
        return null;
    }

    @Nullable
    private static BlockHitResult floor(ServerPlayer actor, Vec3 start, double depth) {
        ServerLevel level = actor.serverLevel();
        BlockPos column = BlockPos.containing(start);
        if (!level.hasChunkAt(column) || !level.getWorldBorder().isWithinBounds(column)) {
            return null;
        }
        var hit = level.clip(new ClipContext(start, start.subtract(0, depth, 0),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, actor));
        return hit.getType() == HitResult.Type.BLOCK && hit.getDirection() == Direction.UP ? hit : null;
    }

    @Nullable
    public static LivingEntity brandTarget(ServerPlayer actor) {
        Vec3 eye = actor.getEyePosition();
        Vec3 end = eye.add(actor.getLookAngle().scale(6));
        Vec3 limit = visibleEnd(actor, eye, end);
        if (limit == null) {
            return null;
        }
        return actor.level().getEntitiesOfClass(LivingEntity.class, new AABB(eye, limit).inflate(1),
                        target -> target != actor && target.isAlive() && !target.isSpectator()
                                && (!(target instanceof ServerPlayer player) || actor.canHarmPlayer(player)))
                .stream().filter(target -> target.getBoundingBox().inflate(0.15).clip(eye, limit).isPresent())
                .min(Comparator.comparingDouble(target -> target.distanceToSqr(eye))).orElse(null);
    }

    @Nullable
    private static Vec3 visibleEnd(ServerPlayer actor, Vec3 eye, Vec3 end) {
        var chunks = actor.serverLevel().getChunkSource();
        int minX = SectionPos.posToSectionCoord(Math.min(eye.x, end.x));
        int maxX = SectionPos.posToSectionCoord(Math.max(eye.x, end.x));
        int minZ = SectionPos.posToSectionCoord(Math.min(eye.z, end.z));
        int maxZ = SectionPos.posToSectionCoord(Math.max(eye.z, end.z));
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                if (!chunks.hasChunk(x, z)) {
                    return null;
                }
            }
        }
        var hit = actor.level().clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, actor));
        return hit.getType() == HitResult.Type.MISS ? end : hit.getLocation();
    }
}
