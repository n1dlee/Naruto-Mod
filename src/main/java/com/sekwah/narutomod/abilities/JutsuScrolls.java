package com.sekwah.narutomod.abilities;

import java.util.Set;

/**
 * Phase 15 C: the jutsu that must be LEARNED from a scroll before casting.
 * Everything not listed stays innate — clan kekkei genkai come with the bloodline,
 * utility movement (leap, dash, water walk...) is basic academy training.
 *
 * Elemental jutsu need scroll + their element unlocked + mastery level;
 * signature techniques (Rasengan, Shadow Clone...) need just the scroll.
 */
public final class JutsuScrolls {

    public static final Set<String> SCROLL_JUTSU = Set.of(
            // elemental
            "fireball",
            "water_bullet",
            "water_dragon",
            "earth_wall",
            "earth_spikes",
            "great_breakthrough",
            "false_darkness",
            "chidori",
            "chidori_dash",
            "chidori_nagashi",
            "rasenshuriken",
            // signature learnable techniques
            "rasengan",
            "shadow_clone",
            "multiple_shadow_clone",
            "flying_thunder_god",
            "eight_gates",
            "kuchiyose",
            "sage_mode"
    );

    public static boolean requiresScroll(String jutsuPath) {
        return SCROLL_JUTSU.contains(jutsuPath);
    }

    private JutsuScrolls() {
    }
}
