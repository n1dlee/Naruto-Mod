package com.sekwah.narutomod.entity.jutsuprojectile;

import com.sekwah.narutomod.util.ChibakuFormation;
import com.sekwah.narutomod.util.ChibakuFormation.Source;
import com.sekwah.narutomod.util.Faction;
import com.sekwah.narutomod.util.JutsuAim;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

final class ChibakuWorld {
    private ChibakuWorld() {
    }

    static @Nullable Vec3 origin(Player owner) {
        if (!(owner.level() instanceof ServerLevel level) || !owner.isAlive() || owner.isSpectator() || owner.noPhysics) {
            return null;
        }
        Vec3 eye = owner.getEyePosition();
        Vec3 limit = eye.add(owner.getLookAngle().scale(14));
        if (!loaded(level, eye, limit)) {
            return null;
        }
        var aimed = level.clip(new ClipContext(eye, limit, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, owner));
        Vec3 aim = aimed.getLocation();
        BlockPos at = BlockPos.containing(aim);
        Vec3 start = aim.add(0, 1.22, 0);
        if (eye.distanceToSqr(aim) < 2.2 * 2.2 || !level.mayInteract(owner, at) || !level.getWorldBorder().isWithinBounds(at)
                || level.isOutsideBuildHeight(BlockPos.containing(start.add(0, 11.2, 0)))
                || !level.noCollision(new AABB(start, start).inflate(0.36))) {
            return null;
        }
        return start;
    }

    static List<Source> samples(ServerLevel level, ChibakuTenseiEntity core, @Nullable LivingEntity owner) {
        final double golden = Math.PI * (3 - Math.sqrt(5));
        var used = new HashSet<BlockPos>();
        var result = new ArrayList<Source>();
        Vec3 center = core.position();
        for (int i = 0; i < ChibakuFormation.MAX_SOURCES * 2 && result.size() < ChibakuFormation.MAX_SOURCES; i++) {
            double theta = i * golden;
            double reach = 1.9 + 6.3 * Math.sqrt(i / (ChibakuFormation.MAX_SOURCES * 2d));
            BlockPos column = BlockPos.containing(center.add(Math.cos(theta) * reach, 0, Math.sin(theta) * reach));
            if (!level.hasChunkAt(column) || !level.getWorldBorder().isWithinBounds(column)) {
                continue;
            }
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column.getX(), column.getZ()) - 1;
            var source = new BlockPos(column.getX(), y, column.getZ());
            Vec3 origin = Vec3.atCenterOf(source).add(0, 0.53, 0);
            var state = level.getBlockState(source);
            if (level.isOutsideBuildHeight(source) || center.y <= origin.y || center.y - origin.y > 56
                    || state.isAir() || state.hasBlockEntity() || !state.getFluidState().isEmpty()
                    || owner instanceof Player caster && !level.mayInteract(caster, source)
                    || !used.add(source) || !core.visible(level, center, origin)) {
                continue;
            }
            result.add(new Source(source, state.getMapColor(level, source).col));
        }
        return List.copyOf(result);
    }

    static boolean target(ServerLevel level, LivingEntity owner, LivingEntity target) {
        if (owner instanceof Player caster) {
            return JutsuAim.attackable(caster, target)
                    && (!(target instanceof Player) || level.getServer().isPvpAllowed())
                    && level.mayInteract(caster, target.blockPosition());
        }
        return target != owner && target.isAlive() && !target.isSpectator() && !target.noPhysics
                && !owner.isAlliedTo(target) && !Faction.sameSide(owner, target);
    }

    static boolean loaded(ServerLevel level, Vec3 from, Vec3 to) {
        var box = new AABB(from, to);
        return level.hasChunksAt(BlockPos.containing(box.minX, box.minY, box.minZ),
                BlockPos.containing(box.maxX, box.maxY, box.maxZ));
    }
}
