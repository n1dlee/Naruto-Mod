package com.sekwah.narutomod.entity.jutsuprojectile;

import com.sekwah.narutomod.util.NarutoParticles;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public final class MirrorTransit {
    private MirrorTransit() {
    }

    public static List<IceMirrorEntity> ringNear(LivingEntity caster, Vec3 near) {
        List<IceMirrorEntity> panes = ownedNear(caster, near);
        var anchor = panes.stream().min(Comparator.comparingDouble(pane -> pane.ringCenter().distanceToSqr(near)));
        return anchor.isEmpty() ? List.of() : panes.stream()
                .filter(pane -> pane.ringId().equals(anchor.get().ringId())).toList();
    }

    public static List<IceMirrorEntity> ring(LivingEntity caster, UUID ringId) {
        return ownedNear(caster, caster.position()).stream()
                .filter(pane -> pane.ringId().equals(ringId)).toList();
    }

    private static List<IceMirrorEntity> ownedNear(LivingEntity caster, Vec3 origin) {
        return caster.level().getEntitiesOfClass(IceMirrorEntity.class, new AABB(origin, origin).inflate(11.0),
                pane -> !pane.isRemoved() && pane.shatterTicks() == 0
                        && pane.getOwnerUUID().filter(caster.getUUID()::equals).isPresent()
                        && pane.ringCenter().distanceToSqr(origin) < 7.5 * 7.5);
    }

    @Nullable
    public static Route destination(LivingEntity caster, List<IceMirrorEntity> ring) {
        Route selected = null;
        double best = 0.74 * 0.74;
        for (IceMirrorEntity pane : ring) {
            if (!pane.available() || pane.getOwnerUUID().filter(caster.getUUID()::equals).isEmpty()) {
                continue;
            }
            Vec3 entry = pane.position().add(pane.inward().scale(0.56));
            Vec3 exit = IceMirrorEntity.safeFloor(caster.level(), caster, entry, caster.getBbHeight() + 0.07);
            if (exit == null || !caster.level().noCollision(caster, caster.getBoundingBox().move(exit.subtract(caster.position())))) {
                continue;
            }
            double distance = caster.position().distanceToSqr(exit);
            if (distance > best) {
                selected = new Route(pane, exit);
                best = distance;
            }
        }
        return selected;
    }

    public static boolean travel(LivingEntity caster, List<IceMirrorEntity> ring) {
        if (!(caster.level() instanceof ServerLevel level) || caster.isSpectator() || !caster.isAlive()
                || caster.isPassenger()) {
            return false;
        }
        Route route = destination(caster, ring);
        if (route == null) {
            return false;
        }
        Vec3 start = caster.position();
        for (IceMirrorEntity pane : ring) {
            if (pane.available() && pane.position().distanceToSqr(start) < 1.38 * 1.38) {
                pane.flash();
            }
        }
        IceMirrorEntity.move(caster, route.exit());
        route.pane().flash();
        NarutoParticles.spawnBolt(level, start.add(0, 1.0, 0), route.exit().add(0, 1.0, 0), 4, 0.19,
                NarutoParticles.ICE_PALE);
        level.playSound(null, route.exit().x, route.exit().y, route.exit().z, SoundEvents.GLASS_HIT,
                SoundSource.PLAYERS, 0.92f, 1.5f);
        return true;
    }

    public record Route(IceMirrorEntity pane, Vec3 exit) {
    }
}
