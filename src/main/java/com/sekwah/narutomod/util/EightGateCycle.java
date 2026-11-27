package com.sekwah.narutomod.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

public final class EightGateCycle {
    public static final int LAST = 8;
    public static final int STANDARD_TICKS = 600;
    public static final int DEATH_TICKS = 300;
    private static final String DEATH_KEY = "gateDeathDeadline";
    private static final int CONFIRM_DELAY = 6;
    private static final int CONFIRM_WINDOW = 40;
    private static final Palette[] PROFILES = {
            new Palette(0xA4CB87, 0.86f, 0.28f, 21),
            new Palette(0x89C763, 0.92f, 0.34f, 22),
            new Palette(0x68DE59, 1.05f, 0.46f, 24),
            new Palette(0x42E880, 1.20f, 0.59f, 27),
            new Palette(0x66EDAD, 1.38f, 0.73f, 30),
            new Palette(0x5BE2D3, 1.57f, 0.84f, 34),
            new Palette(0x56B8FF, 1.82f, 1.03f, 39),
            new Palette(0xF13A35, 2.06f, 1.28f, 45)
    };
    private long armedAt = -1;
    private long deathAt = -1;

    public enum Confirm { WARN, WAIT, ACCEPT }

    public Confirm confirm(long now) {
        if (armedAt < 0 || now < armedAt || now - armedAt >= CONFIRM_WINDOW) {
            armedAt = now;
            return Confirm.WARN;
        }
        return now - armedAt >= CONFIRM_DELAY ? Confirm.ACCEPT : Confirm.WAIT;
    }

    public void open(int stage, long now) {
        armedAt = -1;
        deathAt = stage == LAST ? now + DEATH_TICKS : -1;
    }

    public void clear() {
        armedAt = -1;
        deathAt = -1;
    }

    public boolean hasDeathDeadline() {
        return deathAt >= 0;
    }

    public int deathRemaining(long now) {
        return hasDeathDeadline() ? (int) Math.max(0, Math.min(DEATH_TICKS, deathAt - now)) : 0;
    }

    public void save(CompoundTag tag) {
        if (hasDeathDeadline()) {
            tag.putLong(DEATH_KEY, deathAt);
        }
    }

    public void load(CompoundTag tag) {
        armedAt = -1;
        deathAt = tag.contains(DEATH_KEY, Tag.TAG_LONG) ? tag.getLong(DEATH_KEY) : -1;
    }

    public static Palette profile(int stage) {
        if (stage < 1 || stage > LAST) {
            throw new IllegalArgumentException("Invalid gate: " + stage);
        }
        return PROFILES[stage - 1];
    }

    public record Palette(int tint, float height, float span, int chakra) {
    }
}
