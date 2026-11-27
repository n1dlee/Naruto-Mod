package com.sekwah.narutomod.util;

import com.sekwah.narutomod.capabilities.INinjaData;
import net.minecraft.util.Mth;

public final class DojutsuVisual {
    public static final float REVEAL_TICKS = 4;
    private static final Eye[] EYES = Eye.values();

    private DojutsuVisual() {
    }

    public static Command select(String clan, boolean sharingan, boolean byakugan, boolean secondary) {
        if (!sharingan) {
            return byakugan ? Command.BYAKUGAN : Command.NONE;
        }
        if (!byakugan) {
            return Command.SHARINGAN;
        }
        boolean preferByakugan = "hyuga".equals(clan) ^ secondary;
        return preferByakugan ? Command.BYAKUGAN : Command.SHARINGAN;
    }

    public static int resolve(INinjaData data) {
        if (!data.isNinjaModeEnabled()) {
            return 0;
        }
        String clan = data.getClanId();
        int marks = data.hasSharinganEye() && data.isSharinganActive() ? data.getSharinganLevel() : 0;
        var context = new State(data.isRinneganAwakened(), data.isSageModeActive(),
                marks,
                data.isTransplantedSharingan() && !"uchiha".equals(clan), data.isMangekyoAwakened(),
                data.isByakuganActive(), "hyuga".equals(clan), data.getByakuganLevel() > 0);
        return resolve(context);
    }

    public static int resolve(State state) {
        Eye left = state.rinnegan() ? Eye.RINNEGAN : state.sage() ? Eye.SAGE : Eye.NONE;
        Eye right = left;
        if (state.nativeByakugan() && state.byakuganActive()) {
            left = Eye.BYAKUGAN_FOCUS;
            right = Eye.BYAKUGAN_FOCUS;
        } else if (!state.nativeByakugan() && state.hasByakuganLevel()) {
            left = state.byakuganActive() ? Eye.BYAKUGAN_FOCUS : Eye.BYAKUGAN;
        }
        if (state.marks() > 0) {
            Eye red = state.mangekyo() ? Eye.MANGEKYO : switch (state.marks()) {
                case 1 -> Eye.TOMOE_ONE;
                case 2 -> Eye.TOMOE_TWO;
                default -> Eye.TOMOE_THREE;
            };
            right = red;
            if (!state.transplantSharingan()) {
                left = red;
            }
        }
        return right.ordinal() | (left.ordinal() << 4);
    }

    public static Eye eye(int state, boolean right) {
        int channel = (right ? state : state >>> 4) & 15;
        return channel < EYES.length ? EYES[channel] : Eye.NONE;
    }

    public static float reveal(float age) {
        float t = Mth.clamp(age / REVEAL_TICKS, 0, 1);
        return t * t * (3 - 2 * t);
    }

    public static int textureU(boolean right) {
        return right ? 9 : 13;
    }

    public enum Eye {
        NONE, TOMOE_ONE, MANGEKYO, BYAKUGAN, RINNEGAN, SAGE, TOMOE_TWO, TOMOE_THREE, BYAKUGAN_FOCUS
    }

    public enum Command {
        NONE, SHARINGAN, BYAKUGAN
    }

    public record State(boolean rinnegan, boolean sage, int marks, boolean transplantSharingan,
                        boolean mangekyo, boolean byakuganActive, boolean nativeByakugan, boolean hasByakuganLevel) {
    }
}
