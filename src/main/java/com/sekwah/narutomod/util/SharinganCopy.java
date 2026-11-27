package com.sekwah.narutomod.util;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.abilities.JutsuScrolls;
import com.sekwah.narutomod.capabilities.NinjaCapabilityHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Learns observed scroll techniques without unlocking natures or granting mastery.
 */
public final class SharinganCopy {

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
        if (caster == null || ability == null || path == null || !JutsuScrolls.requiresScroll(path)
                || ability.requiredEye() != null || ability.requiredClan() != null
                || ability.activationType() == Ability.ActivationType.TOGGLE) {
            return;
        }
        if (!(caster.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        for (Player watcher : serverLevel.players()) {
            if (watcher == caster || !watcher.isAlive() || watcher.isSpectator()
                    || caster.isInvisibleTo(watcher) || watcher.isAlliedTo(caster) || Faction.sameSide(watcher, caster)) {
                continue;
            }
            Vec3 offset = caster.getEyePosition().subtract(watcher.getEyePosition());
            if (!SharinganReading.watching(watcher.getLookAngle(), offset) || !watcher.hasLineOfSight(caster)) {
                continue;
            }
            tryRead(watcher, ability, path);
        }
    }

    private static void tryRead(Player watcher, Ability ability, String path) {
        watcher.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(ninjaData -> {
            if (!ninjaData.isNinjaModeEnabled() || !ninjaData.isSharinganActive() || !ninjaData.hasSharinganEye()
                    || ninjaData.getCurrentlyChanneledAbility() != null) {
                return;
            }
            int tomoe = ninjaData.isMangekyoAwakened() ? 3 : ninjaData.getSharinganTomoe();
            String element = ability.element();
            boolean nature = element == null || ninjaData.isElementUnlocked(element);
            if (ability.secondaryElement() != null && !ninjaData.isElementUnlocked(ability.secondaryElement())) {
                nature = false;
            }
            if (!SharinganReading.understands(tomoe, element != null, nature)
                    || ninjaData.isJutsuLearned(path)) {
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
        });
    }
}
