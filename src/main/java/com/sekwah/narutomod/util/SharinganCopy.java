package com.sekwah.narutomod.util;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.NinjaCapabilityHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * The Sharingan's copy-wheel: watching an enemy perform a technique is enough to steal it.
 *
 * Whenever anything in the world casts a jutsu, this gets called; every player nearby with
 * an open Sharingan rolls to read it. A successful read stores ONE technique, which they
 * can then throw back with its normal hand-seal combo — the stored copy waives the scroll
 * and nature requirements for that single cast (see Ability.checkLearnedRequirement /
 * checkElementRequirement), then is spent.
 *
 * Tomoe gate the reading, matching how the eye matures in canon:
 *   1 tomoe  - can follow movement, but cannot read a technique at all
 *   2 tomoe  - can copy elemental ninjutsu
 *   3 tomoe  - can copy essentially anything
 */
public final class SharinganCopy {

    /** How far the eye can read a technique being performed. */
    private static final double READ_RANGE = 24.0D;
    private static final float CHANCE_TWO_TOMOE = 0.40f;
    private static final float CHANCE_THREE_TOMOE = 0.75f;

    private SharinganCopy() {
    }

    /**
     * @param caster the entity performing the technique (never copies from itself)
     * @param ability the technique being performed
     * @param path its registry path, used as the stored copy key
     */
    public static void onJutsuPerformed(LivingEntity caster, Ability ability, String path) {
        if (caster == null || ability == null || path == null || path.isEmpty()) {
            return;
        }
        if (!(caster.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        for (Player watcher : serverLevel.players()) {
            if (watcher == caster || !watcher.isAlive()) {
                continue;
            }
            if (watcher.distanceToSqr(caster) > READ_RANGE * READ_RANGE) {
                continue;
            }
            // Has to actually be watching - no copying what happens behind your back.
            if (!watcher.hasLineOfSight(caster)) {
                continue;
            }
            tryRead(watcher, caster, ability, path);
        }
    }

    private static void tryRead(Player watcher, LivingEntity caster, Ability ability, String path) {
        watcher.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(ninjaData -> {
            if (!ninjaData.isNinjaModeEnabled() || !ninjaData.isSharinganActive()) {
                return;
            }
            int tomoe = ninjaData.isMangekyoAwakened() ? 3 : ninjaData.getSharinganTomoe();
            if (tomoe < 2) {
                return; // a single tomoe reads motion, not technique
            }
            // Two tomoe can only lift elemental ninjutsu; three can take almost anything.
            if (tomoe == 2 && ability.element() == null) {
                return;
            }
            float chance = tomoe >= 3 ? CHANCE_THREE_TOMOE : CHANCE_TWO_TOMOE;
            if (watcher.getRandom().nextFloat() >= chance) {
                return;
            }
            // Nothing to gain from reading a technique already committed to memory.
            if (ninjaData.isJutsuLearned(path)) {
                return;
            }

            // Learned outright, not stored as a single throw. The old one-shot slot is what
            // broke this: it refused to overwrite an unspent copy, and it was only cleared
            // by casting that exact technique - so copying anything whose hand seals the
            // player never used left the slot jammed and the eye stopped reading forever.
            ninjaData.learnJutsu(path);
            ninjaData.setCopiedJutsu(path);
            watcher.displayClientMessage(Component.translatable("sharingan.copied",
                    Component.translatable(ability.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW))
                    .withStyle(ChatFormatting.LIGHT_PURPLE), true);

            grantStudyXp(watcher, ninjaData, ability);

            if (watcher.level() instanceof ServerLevel serverLevel) {
                Vec3 eye = watcher.getEyePosition();
                NarutoParticles.spawnRing(serverLevel, eye, 0.5, 16, NarutoParticles.SHARINGAN_RED);
            }
        });
    }

    /**
     * Reading a technique teaches you about its nature, so a copy advances that element's
     * mastery. Worth several casts' worth of practice - watching someone who has already
     * mastered it is a shortcut, which is the whole reason the eye is feared.
     *
     * It will NOT hand you a nature you never awakened: the Sharingan copies the form of a
     * technique, not the chakra to fuel it. Kakashi still needed his own lightning affinity.
     */
    private static void grantStudyXp(Player watcher, com.sekwah.narutomod.capabilities.INinjaData ninjaData,
                                     Ability ability) {
        String element = ability.element();
        if (element == null) {
            return;
        }
        if (!ninjaData.isElementUnlocked(element)) {
            watcher.displayClientMessage(Component.translatable("sharingan.copied.nonature",
                    Component.translatable("element.narutomod." + element).withStyle(ChatFormatting.YELLOW))
                    .withStyle(ChatFormatting.GRAY), true);
            return;
        }
        int before = ninjaData.getElementLevel(element);
        ninjaData.addElementXp(element, ability.elementXpReward() * STUDY_XP_MULTIPLIER);
        int after = ninjaData.getElementLevel(element);
        if (after > before) {
            watcher.displayClientMessage(Component.translatable("sharingan.copied.mastery",
                    Component.translatable("element.narutomod." + element).withStyle(ChatFormatting.YELLOW),
                    Component.literal(String.valueOf(after)).withStyle(ChatFormatting.YELLOW))
                    .withStyle(ChatFormatting.GREEN), false);
        }
    }

    /** A read is worth this many ordinary casts of study. */
    private static final float STUDY_XP_MULTIPLIER = 5f;
}
