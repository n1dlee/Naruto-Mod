package com.sekwah.narutomod.util;

import net.minecraft.world.phys.Vec3;

public final class ClayFlight {
    public static final int MOLD = 9;
    public static final int BIRD_FLIGHT = 26;
    public static final int CRAWL_FLIGHT = 22;
    public static final int ARM = 7;
    public static final int FUSE = 30;
    public static final int BURST = 13;
    public static final double RADIUS = 0.235;

    private ClayFlight() {
    }

    public static Vec3 birdLaunch(Vec3 heading) { return heading.scale(1.12).add(0, 0.18, 0); }

    public static Vec3 nextFlight(Vec3 velocity) { return velocity.multiply(0.985, 1, 0.985).add(0, -0.036, 0); }

    public static boolean armed(int restingTicks) { return restingTicks >= ARM && restingTicks < FUSE; }

    public static Vec3 settle(Vec3 contact, Vec3 normal) { return contact.add(normal.scale(RADIUS + 0.025)); }

    public static double flash(int age, float partial) {
        double total = age + partial;
        double frequency = total < 18 ? 0.64 : 1.27;
        return 0.27 + Math.pow(0.5 + 0.5 * Math.sin(total * frequency), 3) * 0.73;
    }
}
