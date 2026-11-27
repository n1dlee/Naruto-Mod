package com.sekwah.narutomod.util;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.function.Predicate;

public final class JutsuAim {
    private JutsuAim() {
    }

    public static @Nullable LivingEntity select(Player caster, double range, double halfAngle, Predicate<LivingEntity> filter) {
        Vec3 eye = caster.getEyePosition();
        Vec3 facing = caster.getLookAngle();
        double cosine = Math.cos(Math.toRadians(halfAngle));
        return caster.level().getEntitiesOfClass(LivingEntity.class, caster.getBoundingBox().inflate(range),
                        target -> attackable(caster, target) && filter.test(target)
                                && aimed(eye, facing, center(target), range, cosine) && caster.hasLineOfSight(target))
                .stream().min(Comparator
                        .comparingDouble((LivingEntity target) -> -center(target).subtract(eye).normalize().dot(facing))
                        .thenComparingDouble(caster::distanceToSqr)
                        .thenComparing(LivingEntity::getUUID)).orElse(null);
    }

    public static Vec3 center(LivingEntity target) {
        return target.position().add(0, target.getBbHeight() * 0.64, 0);
    }

    public static boolean aimed(Vec3 eyes, Vec3 direction, Vec3 target, double range, double coneCosine) {
        Vec3 offset = target.subtract(eyes);
        double length = offset.lengthSqr();
        return length > 0.018 && length <= range * range && offset.dot(direction) >= Math.sqrt(length) * coneCosine;
    }

    public static boolean attackable(Player caster, LivingEntity target) {
        return target != caster && target.level() == caster.level() && target.isAlive() && !target.isSpectator()
                && target.isPickable() && !target.noPhysics && !caster.isAlliedTo(target) && !Faction.sameSide(caster, target)
                && !(target instanceof TamableAnimal tame && tame.isOwnedBy(caster))
                && (!(target instanceof Player other) || !other.isCreative() && caster.canHarmPlayer(other));
    }
}
