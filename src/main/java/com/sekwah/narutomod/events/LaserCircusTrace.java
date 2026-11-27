package com.sekwah.narutomod.events;

import com.sekwah.narutomod.util.JutsuAim;
import com.sekwah.narutomod.util.KamuiStep;
import com.sekwah.narutomod.util.LaserCircusPath;
import com.sekwah.narutomod.util.WaterDragonFlight;
import com.sekwah.narutomod.util.WaterDragonFlight.Contact;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

final class LaserCircusTrace {
    private LaserCircusTrace() {
    }

    static Result trace(ServerLevel level, Player owner, List<Vec3> route) {
        var line = new ArrayList<Vec3>(route.size());
        line.add(route.get(0));
        for (int index = 1; index < route.size(); index++) {
            Vec3 from = route.get(index - 1);
            Vec3 to = route.get(index);
            var box = new AABB(from, to).inflate(LaserCircusPath.CONTACT);
            if (!available(level, box)) {
                if (line.size() == 1) {
                    line.add(from);
                }
                return new Result(List.copyOf(line), null, false);
            }
            var hit = contact(level, owner, box, from, to);
            if (hit != null) {
                line.add(hit.contact().point());
                return new Result(List.copyOf(line), hit.target(), hit.target() == null);
            }
            line.add(to);
        }
        return new Result(List.copyOf(line), null, false);
    }

    private static @Nullable Hit contact(ServerLevel level, Player owner, AABB box, Vec3 from, Vec3 to) {
        Contact first = null;
        LivingEntity target = null;
        for (var obstacle : level.getBlockCollisions(owner, box)) {
            for (var bounds : obstacle.toAabbs()) {
                var next = WaterDragonFlight.sweep(from, to, bounds, LaserCircusPath.CONTACT);
                if (next != null && next.precedes(first)) {
                    first = next;
                }
            }
        }
        for (var enemy : level.getEntitiesOfClass(LivingEntity.class, box,
                actor -> JutsuAim.attackable(owner, actor) && level.mayInteract(owner, actor.blockPosition()))) {
            var next = WaterDragonFlight.sweep(from, to, enemy.getBoundingBox(), LaserCircusPath.CONTACT);
            if (next != null && next.precedes(first)) {
                first = next;
                target = enemy;
            }
        }
        return first == null ? null : new Hit(first, target);
    }

    private static boolean available(ServerLevel level, AABB box) {
        var border = level.getWorldBorder();
        return KamuiStep.insideBorder(box, border.getMinX(), border.getMinZ(), border.getMaxX(), border.getMaxZ())
                && level.hasChunksAt(BlockPos.containing(box.minX, box.minY, box.minZ),
                        BlockPos.containing(box.maxX, box.maxY, box.maxZ));
    }

    record Result(List<Vec3> points, @Nullable LivingEntity target, boolean blocked) {
    }

    private record Hit(Contact contact, @Nullable LivingEntity target) {
    }
}
