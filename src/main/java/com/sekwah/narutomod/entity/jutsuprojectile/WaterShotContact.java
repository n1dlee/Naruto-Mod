package com.sekwah.narutomod.entity.jutsuprojectile;

import com.sekwah.narutomod.damagetypes.NarutoDamageTypes;
import com.sekwah.narutomod.util.WaterDragonFlight;
import com.sekwah.narutomod.util.WaterShotShape;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

final class WaterShotContact {
    private WaterShotContact() {
    }

    static boolean damage(WaterShotEntity shot, ServerLevel level, @Nullable LivingEntity owner,
                          LivingEntity target, float amount) {
        if (owner == null || amount <= 0 || !target.hurt(NarutoDamageTypes.getDamageSource(level,
                NarutoDamageTypes.WATER_BULLET, shot, owner, shot.position()), amount)) {
            return false;
        }
        target.clearFire();
        return true;
    }

    static void splash(WaterShotEntity shot, ServerLevel level, @Nullable LivingEntity owner,
                       @Nullable LivingEntity direct, float power) {
        if (owner == null) {
            return;
        }
        Vec3 center = shot.position();
        Vec3 from = center.add(Vec3.atLowerCornerOf(shot.face().getNormal()).scale(0.04));
        var scan = new AABB(center, center).inflate(WaterShotShape.splashRadius(shot.heavy()));
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, scan,
                candidate -> candidate != direct && shot.hittable(owner, candidate))) {
            Vec3 point = WaterDragonFlight.nearestPoint(target.getBoundingBox(), from);
            float amount = WaterShotShape.splashDamage(center.distanceTo(point), shot.heavy()) * power;
            if (amount > 0 && ProjectileVolume.visible(shot, from, point)) {
                damage(shot, level, owner, target, amount);
            }
        }
    }

}
