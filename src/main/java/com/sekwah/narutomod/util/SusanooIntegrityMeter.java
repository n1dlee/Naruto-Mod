package com.sekwah.narutomod.util;

import net.minecraft.util.Mth;

public record SusanooIntegrityMeter(float remaining, float maximum, int lockoutTicks) {
    public SusanooIntegrityMeter {
        maximum = Float.isFinite(maximum) ? Math.max(0, maximum) : 0;
        remaining = Float.isFinite(remaining) ? Mth.clamp(remaining, 0, maximum) : 0;
        lockoutTicks = Math.max(0, lockoutTicks);
    }

    public boolean broken() { return lockoutTicks > 0; }

    public float fraction() { return broken() || maximum == 0 ? 0 : remaining / maximum; }

    public int percent() { return Math.round(fraction() * 100); }

    public int seconds() { return Mth.ceil(lockoutTicks / 20.0); }

    public boolean critical() { return !broken() && maximum > 0 && fraction() <= .25f; }

    public String remainingText() { return text(broken() ? 0 : remaining); }

    public String maximumText() { return text(maximum); }

    private static String text(float value) {
        int tenths = Math.round(value * 10);
        if (value > 0 && tenths == 0) return "<0.1";
        return tenths % 10 == 0 ? Integer.toString(tenths / 10) : tenths / 10 + "." + tenths % 10;
    }
}
