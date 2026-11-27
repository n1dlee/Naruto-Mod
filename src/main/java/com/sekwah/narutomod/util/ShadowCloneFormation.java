package com.sekwah.narutomod.util;

import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public final class ShadowCloneFormation {
    public static final int OWNER_CAP = 20;
    public static final int LIFETIME = 600;

    private ShadowCloneFormation() {
    }

    public enum Profile {
        SQUAD(20, 3, 4, 3),
        MASS(80, OWNER_CAP, 8, 5);

        public final int cost;
        public final int count;
        public final int windup;
        public final int wave;

        Profile(int cost, int count, int windup, int wave) {
            this.cost = cost;
            this.count = count;
            this.windup = windup;
            this.wave = wave;
        }
    }

    public static List<Vec3> candidates(Vec3 origin, float yaw, Profile profile) {
        Vec3 forward = KamuiStep.facing(yaw);
        Vec3 right = new Vec3(forward.z, 0, -forward.x);
        List<Vec3> sites = new ArrayList<>();
        addRow(sites, origin, forward, right, 0, 3);
        addRow(sites, origin, forward, right, 1, profile == Profile.MASS ? 7 : 3);
        addRow(sites, origin, forward, right, -1, profile == Profile.MASS ? 7 : 3);
        if (profile == Profile.MASS) {
            addRow(sites, origin, forward, right, 2, 7);
            addRow(sites, origin, forward, right, -2, 7);
            addRow(sites, origin, forward, right, 3, 7);
        }
        return List.copyOf(sites);
    }

    private static void addRow(List<Vec3> sites, Vec3 from, Vec3 forward, Vec3 side, int row, int width) {
        for (int column = -(width / 2); column <= width / 2; column++) {
            if (column != 0 || row != 0) {
                sites.add(from.add(forward.scale(row * 1.27)).add(side.scale(column * 1.27)));
            }
        }
    }

}
