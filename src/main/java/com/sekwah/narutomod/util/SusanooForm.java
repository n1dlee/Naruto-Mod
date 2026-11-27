package com.sekwah.narutomod.util;

import net.minecraft.util.Mth;

public enum SusanooForm {
    NONE(0, 0, 1, 0),
    RIBS(1, 1, 1, 220),
    SKELETON(2, 2, 1, 480),
    SINEW(5, 2, 1.09, 610),
    AVATAR(6, 3, 1.20, 755),
    ARMORED(3, 3, 1.32, 950),
    STANDING(7, 3, 1.48, 1240),
    PERFECT(4, 4, 1, 1800);

    private static final SusanooForm[] ORDER = values();
    private static final SusanooForm[] SAVED = {NONE, RIBS, SKELETON, ARMORED, PERFECT, SINEW, AVATAR, STANDING};

    private final int id;
    private final int combatTier;
    private final double torsoScale;
    private final int integrity;

    SusanooForm(int id, int combatTier, double torsoScale, int integrity) {
        this.id = id;
        this.combatTier = combatTier;
        this.torsoScale = torsoScale;
        this.integrity = integrity;
    }

    public int id() { return id; }
    public int combatTier() { return combatTier; }
    public double torsoScale() { return torsoScale; }
    public int integrity() { return integrity; }

    public String translationKey() {
        return "susanoo.form." + name().toLowerCase(java.util.Locale.ROOT);
    }

    public static SusanooForm fromId(int id) {
        return id >= 0 && id < SAVED.length ? SAVED[id] : NONE;
    }

    public static SusanooForm forPower(float power, boolean kage) {
        int max = kage ? PERFECT.ordinal() : SKELETON.ordinal();
        return ORDER[1 + Mth.floor(Mth.clamp(power, 0, 1) * (max - 1) + .00001f)];
    }

    public static SusanooForm forBossPhase(int phase, boolean armoredLimit) {
        if (phase <= 0) return NONE;
        if (phase == 1) return SKELETON;
        if (phase == 2) return AVATAR;
        if (armoredLimit) return ARMORED;
        return phase == 3 ? STANDING : PERFECT;
    }

    public static SusanooForm limit(int id, boolean armoredLimit) {
        var form = fromId(id);
        return armoredLimit && form.ordinal() > ARMORED.ordinal() ? ARMORED : form;
    }

    public SusanooForm stepToward(SusanooForm target) {
        return ORDER[ordinal() + Integer.signum(target.ordinal() - ordinal())];
    }

    public static boolean perfect(int id) {
        return id == PERFECT.id;
    }
}
