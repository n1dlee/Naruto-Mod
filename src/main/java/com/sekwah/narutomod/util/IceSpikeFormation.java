package com.sekwah.narutomod.util;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.properties.DripstoneThickness;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

public final class IceSpikeFormation {
    public static final int WAIT = 8;
    public static final int LIFESPAN = 128;
    public static final int MELT = 15;
    public static final int WARNING = 6;
    private static final int ARMS = 6;

    private IceSpikeFormation() {
    }

    public static List<BlockPos> roots(Vec3 feet, float yaw, int mastery) {
        var roots = new LinkedHashSet<BlockPos>();
        int rings = Mth.clamp(3 + (mastery - 8) / 4, 3, 6);
        double heading = Math.atan2(KamuiStep.facing(yaw).z, KamuiStep.facing(yaw).x);
        for (int ring = 0; ring < rings; ring++) {
            double radius = 2.04 + ring * 1.56;
            for (int arm = 0; arm < ARMS; arm++) {
                double bearing = heading + arm * Math.PI / 3 + (ring % 2 == 0 ? 0 : 0.19);
                roots.add(BlockPos.containing(feet.x + Math.cos(bearing) * radius, feet.y, feet.z + Math.sin(bearing) * radius));
                if (ring >= 2 && arm % 2 == 0) {
                    roots.add(BlockPos.containing(feet.x + Math.cos(bearing + 0.32) * radius,
                            feet.y, feet.z + Math.sin(bearing + 0.32) * radius));
                }
            }
        }
        return List.copyOf(new ArrayList<>(roots));
    }

    public static int delay(Vec3 feet, BlockPos root) {
        double x = root.getX() + 0.5 - feet.x;
        double z = root.getZ() + 0.5 - feet.z;
        return WARNING + (int) (Math.sqrt(x * x + z * z) * 1.27);
    }

    public static List<DripstoneThickness> layers(int height) {
        return switch (height) {
            case 1 -> List.of(DripstoneThickness.TIP);
            case 2 -> List.of(DripstoneThickness.BASE, DripstoneThickness.TIP);
            case 3 -> List.of(DripstoneThickness.BASE, DripstoneThickness.MIDDLE, DripstoneThickness.TIP);
            default -> List.of(DripstoneThickness.BASE, DripstoneThickness.FRUSTUM, DripstoneThickness.MIDDLE, DripstoneThickness.TIP);
        };
    }

    public static double radius(DripstoneThickness shape) {
        return switch (shape) {
            case BASE -> 5 / 16d;
            case FRUSTUM -> 4 / 16d;
            case MIDDLE -> 3 / 16d;
            default -> 2 / 16d;
        };
    }

    public static double height(DripstoneThickness shape) {
        return shape == DripstoneThickness.TIP || shape == DripstoneThickness.TIP_MERGE ? 11 / 16d : 1;
    }

    public static int chill(int current, int required, boolean canFreeze) {
        return canFreeze ? Math.max(current, Math.min(current + 31, required + 4)) : current;
    }
}
