package com.sekwah.narutomod.capabilities;

/**
 * Display side of the rank ladder: names, colours and XP thresholds for the 0-13 index
 * produced by {@link INinjaData#getRankIndex()}.
 *
 * Kept out of NinjaData so the HUD and the progression screen share one table instead of
 * each carrying their own copy of the rank names - which is how they previously drifted out
 * of step with the data layer.
 *
 * Strings stay ASCII-only: with Embeddium/Oculus installed, non-ASCII glyphs in HUD text
 * render as garbage.
 */
public final class RankLadder {

    /** Indexed by getRankIndex(). Academy has no grades; Six Paths sits past Kage. */
    public static final String[] NAMES = {
            "Academy",
            "Low Genin", "Mid Genin", "High Genin",
            "Low Chunin", "Mid Chunin", "High Chunin",
            "Low Jonin", "Mid Jonin", "High Jonin",
            "Low Kage", "Mid Kage", "High Kage",
            "Six Paths"
    };

    /** One hue per base rank, brightening across its three tiers. */
    public static final int[] COLORS = {
            0xAAAAAA,
            0x338833, 0x44BB44, 0x55FF55,
            0x3333AA, 0x4444CC, 0x5555FF,
            0xBB7700, 0xDD9100, 0xFFAA00,
            0xBB3333, 0xDD4444, 0xFF5555,
            0xFFD75E
    };

    /** Mirrors NinjaData's own table so the screen can draw a progress bar to the next step. */
    public static final float[] XP_THRESHOLDS = {
            0,
            1000, 2000, 3400,
            5000, 8000, 11000,
            15000, 24000, 35000,
            50000, 80000, 120000,
            Float.MAX_VALUE
    };

    public static final int MAX_INDEX = NAMES.length - 1;

    private RankLadder() {
    }

    public static String name(int index) {
        return NAMES[clamp(index)];
    }

    public static int color(int index) {
        return COLORS[clamp(index)];
    }

    /**
     * XP needed for the next step, or -1 at the end of the XP ladder. High Kage returns -1
     * as well: Six Paths is earned by felling Mangekyo bosses, not by accumulating XP, so
     * showing a target number there would be a lie.
     */
    public static float nextThreshold(int index) {
        int clamped = clamp(index);
        if (clamped >= MAX_INDEX - 1) {
            return -1f;
        }
        return XP_THRESHOLDS[clamped + 1];
    }

    private static int clamp(int index) {
        return Math.min(Math.max(index, 0), MAX_INDEX);
    }
}
