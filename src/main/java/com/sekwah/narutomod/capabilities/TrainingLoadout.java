package com.sekwah.narutomod.capabilities;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

import javax.annotation.Nullable;
import java.util.List;

record TrainingLoadout(float previousChakra) {
    static final int HIGH_KAGE = RankLadder.MAX_INDEX - 1;
    static final float ELEMENT_XP = 20 * 20 * 25;
    static final List<String> NATURES = List.of("fire", "water", "earth", "wind", "lightning");
    private static final String KEY = "trainingLoadoutPreviousChakra";

    void save(CompoundTag tag) {
        tag.putFloat(KEY, previousChakra);
    }

    @Nullable
    static TrainingLoadout read(CompoundTag tag) {
        if (!tag.contains(KEY, Tag.TAG_FLOAT)) {
            return null;
        }
        float previous = tag.getFloat(KEY);
        return Float.isFinite(previous) && previous >= 0 ? new TrainingLoadout(previous) : null;
    }
}
