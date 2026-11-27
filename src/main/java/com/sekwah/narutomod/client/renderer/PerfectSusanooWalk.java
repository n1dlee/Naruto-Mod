package com.sekwah.narutomod.client.renderer;

import com.sekwah.narutomod.util.PerfectSusanooPose;
import com.sekwah.narutomod.util.PerfectSusanooPose.Stance;
import com.sekwah.narutomod.util.SusanooPose;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.WeakHashMap;

final class PerfectSusanooWalk {
    private static final Map<LivingEntity, Track> TRACKS = new WeakHashMap<>();

    private PerfectSusanooWalk() {
    }

    static Stance sample(LivingEntity actor, float yaw, int age, float partial, double scale) {
        var track = TRACKS.get(actor);
        if (track == null || track.scale != scale) {
            track = new Track(scale);
            TRACKS.put(actor, track);
        }
        if (track.tick != actor.tickCount || age < track.age) {
            track.update(actor, yaw, age);
        }
        return track.before.interpolate(track.after, partial);
    }

    private static Vec3 surface(LivingEntity actor, Vec3 point) {
        var trace = actor.level().clip(new ClipContext(point.add(0, 1.8, 0), point.add(0, -2.5, 0),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, actor));
        return trace.getType() == HitResult.Type.BLOCK ? trace.getLocation() : point;
    }

    private static final class Track {
        private final double scale;
        private final PerfectSusanooPose.Gait gait;
        private int tick = -1;
        private int age;
        private Vec3 root = Vec3.ZERO;
        private Stance before;
        private Stance after;

        private Track(double scale) {
            this.scale = scale;
            this.gait = new PerfectSusanooPose.Gait(scale);
        }

        private void update(LivingEntity actor, float yaw, int newAge) {
            Vec3 position = actor.position();
            Vec3 travel = position.subtract(root);
            var frame = SusanooPose.frame(position, yaw);
            boolean grounded = actor.onGround();
            if (after == null || actor.tickCount - tick > 3 || travel.lengthSqr() > 20 || newAge < age || !grounded) {
                after = gait.reset(frame, point -> grounded ? surface(actor, point) : point);
                before = after;
            } else {
                before = after;
                after = gait.update(frame, travel, point -> surface(actor, point));
            }
            root = position;
            tick = actor.tickCount;
            age = newAge;
        }
    }
}
