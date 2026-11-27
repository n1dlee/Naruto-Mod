package com.sekwah.narutomod.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.Mth;

public final class BossChakraReserve {
    public static final int NINJA_CAST_FLOOR = 70;
    public static final int BEAST_CAST_FLOOR = 30;
    private static final int RECOVERY_PAUSE = 100;
    private static final int RECOVERY_TARGET = 135;
    private static final float COMBAT_RECOVERY = 0.5f;
    private static final float REST_RECOVERY = 1f;

    private final int floor;
    private float capacity;
    private float remaining;
    private boolean exhausted;
    private int recoveryDelay;

    public BossChakraReserve(float capacity, int floor) {
        this.floor = floor;
        reset(capacity);
    }

    public void reset(float capacity) {
        this.capacity = capacity;
        this.remaining = capacity;
        this.exhausted = false;
        this.recoveryDelay = 0;
    }

    public float remaining() {
        return remaining;
    }

    public boolean exhausted() {
        return exhausted;
    }

    public boolean canSpend(float cost) {
        return !exhausted && Float.isFinite(cost) && cost > 0 && remaining >= cost;
    }

    public boolean spend(float cost) {
        if (!canSpend(cost)) {
            return false;
        }
        remaining -= cost;
        recoveryDelay = RECOVERY_PAUSE;
        exhausted = remaining < floor;
        return true;
    }

    public void tick(boolean fighting) {
        if (recoveryDelay > 0) {
            recoveryDelay--;
            return;
        }
        if (!fighting || exhausted) {
            remaining = Math.min(capacity, remaining + (fighting ? COMBAT_RECOVERY : REST_RECOVERY));
            if (remaining >= Math.min(capacity, RECOVERY_TARGET)) {
                exhausted = false;
            }
        }
    }

    public void save(CompoundTag tag) {
        tag.putFloat("BossChakra", remaining);
        tag.putBoolean("BossChakraExhausted", exhausted);
        tag.putInt("BossChakraRecovery", recoveryDelay);
    }

    public void load(CompoundTag tag) {
        float saved = tag.contains("BossChakra", Tag.TAG_ANY_NUMERIC) ? tag.getFloat("BossChakra") : capacity;
        remaining = Float.isFinite(saved) ? Mth.clamp(saved, 0, capacity) : capacity;
        exhausted = remaining < floor || tag.getBoolean("BossChakraExhausted");
        recoveryDelay = tag.contains("BossChakraRecovery", Tag.TAG_ANY_NUMERIC)
                ? Mth.clamp(tag.getInt("BossChakraRecovery"), 0, RECOVERY_PAUSE)
                : exhausted ? RECOVERY_PAUSE : 0;
    }
}
