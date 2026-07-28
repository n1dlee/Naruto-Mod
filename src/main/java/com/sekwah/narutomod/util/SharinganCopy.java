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
            // Don't overwrite a stored copy the player hasn't spent yet.
            if (!ninjaData.getCopiedJutsu().isEmpty()) {
                return;
            }
            float chance = tomoe >= 3 ? CHANCE_THREE_TOMOE : CHANCE_TWO_TOMOE;
            if (watcher.getRandom().nextFloat() >= chance) {
                return;
            }

            ninjaData.setCopiedJutsu(path);
            watcher.displayClientMessage(Component.translatable("sharingan.copied",
                    Component.translatable(ability.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW))
                    .withStyle(ChatFormatting.LIGHT_PURPLE), true);

            if (watcher.level() instanceof ServerLevel serverLevel) {
                Vec3 eye = watcher.getEyePosition();
                NarutoParticles.spawnRing(serverLevel, eye, 0.5, 16, NarutoParticles.SHARINGAN_RED);
            }
        });
    }
}
