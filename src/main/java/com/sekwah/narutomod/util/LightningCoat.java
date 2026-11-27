package com.sekwah.narutomod.util;

import com.sekwah.narutomod.abilities.NarutoAbilities;
import com.sekwah.narutomod.capabilities.INinjaData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class LightningCoat {
    public static final float FORM = 10;
    public static final float FADE = 5;
    public static final double SPEED_BONUS = 3.6;
    private static final double MIN_STRIDE = 0.23;
    private static final double MAX_STRIDE = 1.85;

    private LightningCoat() {
    }

    public static boolean active(Player player, INinjaData data) {
        return player.isAlive() && !player.isSpectator() && data.isNinjaModeEnabled()
                && compatible(data) && data.getToggleAbilityData().getAbilitiesHashSet().contains(NarutoAbilities.LIGHTNING_ARMOR.getId());
    }

    public static boolean compatible(INinjaData data) {
        return compatible(data.getSusanooStage(), data.getGatesOpen(), data.getKuramaTailCount(), data.isKcmActive());
    }

    public static boolean compatible(int susanooStage, int gates, int tails, boolean kcm) {
        return susanooStage <= 0 && gates == 0 && tails == 0 && !kcm;
    }

    public static float layer(float ticks, float bottom) {
        float t = Mth.clamp((ticks / FORM - bottom) / 0.44f, 0, 1);
        return t * t * (3 - 2 * t);
    }

    public static float protectedDamage(float damage) {
        return damage * 0.8f;
    }

    public static float cushioning(float multiplier) {
        return multiplier * 0.68f;
    }

    public static boolean leavesTrail(Vec3 dt, boolean sprint) {
        return sprint && dt.lengthSqr() < MAX_STRIDE * MAX_STRIDE
                && dt.horizontalDistanceSqr() >= MIN_STRIDE * MIN_STRIDE;
    }
}
