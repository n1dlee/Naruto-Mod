package com.sekwah.narutomod.util;

public final class TsukuyomiScene {
    public static final int MAX_TICKS = 34;
    public static final int PLAYER_FOCUS = 7;
    public static final int BOSS_FOCUS = 11;
    public static final double RANGE = 14;

    private TsukuyomiScene() {
    }

    public static int duration(int activeTomoe) {
        return switch (activeTomoe) {
            case 0 -> MAX_TICKS;
            case 1 -> 30;
            case 2 -> 25;
            default -> 20;
        };
    }

    public static float sceneAlpha(float age, int duration, double effectsScale) {
        if (age < 0 || age >= duration || duration <= 0) {
            return 0;
        }
        float fade = Math.min(1, Math.min((age + 1) / 5, (duration - age) / 7));
        return (float) (fade * fade * (3 - 2 * fade) * Math.max(0, Math.min(1, effectsScale)));
    }

    public static boolean matches(int scene, int stop) {
        return scene == stop;
    }

    public static float echoAmount(int index, float progress) {
        float time = (progress - index * 0.14f) / 0.58f;
        if (time < 0 || time > 1) {
            return 0;
        }
        return (float) Math.sin(Math.PI * time) * (1 - index * 0.22f);
    }
}
