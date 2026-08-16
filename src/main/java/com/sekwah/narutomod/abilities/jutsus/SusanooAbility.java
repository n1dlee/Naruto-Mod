package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import org.joml.Vector3f;

/**
 * Susanoo — Mangekyo Sharingan avatar (combo 3231, TOGGLE).
 * Requires Uchiha clan + Mangekyo awakened (Sharingan 4-tomoe).
 * Stage 1 (ribcage): Jonin+. Stage 2 (full Susanoo, arm-swipe AoE): Kage.
 * Drains chakra continuously while active; deactivates automatically when empty
 * (handleCost returning false removes it from the toggle set, which then triggers
 * handleAbilityEnded to clear NinjaData's active/stage state).
 * Effects, stage progression, and rendering are driven by NinjaData.updateSusanoo().
 */
public class SusanooAbility extends Ability implements Ability.Toggled, Ability.ToggleStartCheck, Ability.HandleEnded {

    /** Exempt from the free-hands gate: this is a transformation being worn, not a hand-cast technique. */
    @Override
    public boolean requiresFreeHands() {
        return false;
    }

    public static final float CHAKRA_COST = 8f;

    @Override
    public ActivationType activationType() {
        return ActivationType.TOGGLE;
    }

    @Override
    public long defaultCombo() {
        return 3231;
    }

    /** Baseline Mangekyo technique — the avatar answers to any awakened Mangekyo. */
    @Override
    public String requiredEye() {
        return "sharingan_ms";
    }

    @Override
    public boolean canStartToggle(Player player, INinjaData ninjaData) {
        return validateNotBroken(player, ninjaData)
                && validateAccess(player, ninjaData)
                && validateChakra(player, ninjaData);
    }

    /**
     * Refuses to raise a shell that was broken minutes ago.
     *
     * Breaking a Susanoo has to be worth doing, and it only is if the wearer cannot put
     * another one up on the next tick. The lockout is the reward for getting through it.
     */
    private boolean validateNotBroken(Player player, INinjaData ninjaData) {
        int locked = ninjaData.getSusanooBrokenTicks();
        if (locked <= 0) {
            return true;
        }
        player.displayClientMessage(Component.literal(
                        String.format("Susanoo is shattered - %ds", locked / 20))
                .withStyle(ChatFormatting.DARK_PURPLE), true);
        return false;
    }

    @Override
    public boolean handleCost(Player player, INinjaData ninjaData, int chargeAmount) {
        if (!validateAccess(player, ninjaData) || !validateChakra(player, ninjaData)) {
            return false;
        }
        ninjaData.useChakra(CHAKRA_COST, 5);
        return true;
    }

    @Override
    public void performServer(Player player, INinjaData ninjaData, int ticksActive) {
        if (!ninjaData.isSusanooActive()) {
            ninjaData.setSusanooActive(true);
            // A freshly raised shell is whole. Integrity is not carried over between
            // manifestations - dropping the technique to dodge a hit and putting it straight
            // back up would otherwise be strictly better than holding it.
            ninjaData.setSusanooDurability(ninjaData.getSusanooMaxDurability());
            player.displayClientMessage(
                    Component.literal("Susanoo manifests!").withStyle(ChatFormatting.LIGHT_PURPLE), true);
        }
    }

    @Override
    public void handleAbilityEnded(Player player, INinjaData ninjaData, int ticksActive) {
        ninjaData.setSusanooActive(false);
        ninjaData.setSusanooDurability(0f);
    }

    @Override
    public SoundEvent castingSound() {
        return SoundEvents.CONDUIT_ACTIVATE;
    }

    @Override
    public void performToggleClient(Player player, INinjaData ninjaData) {
        if (player.tickCount % 6 == 0) {
            player.level().addParticle(
                    new DustParticleOptions(new Vector3f(0.55F, 0.25F, 0.85F), 1.0F),
                    player.getX(), player.getY() + player.getBbHeight() * 0.5D, player.getZ(),
                    0.0D, 0.0D, 0.0D);
        }
    }

    private boolean validateAccess(Player player, INinjaData ninjaData) {
        // Clan + Mangekyo tier are enforced centrally via requiredEye(); only the rank
        // floor for manifesting the avatar is specific to Susanoo.
        if (ninjaData.getNinjaRank() < 3) {
            player.displayClientMessage(Component.translatable("jutsu.fail.rank.jonin",
                    Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW)), true);
            return false;
        }
        return true;
    }

    private boolean validateChakra(Player player, INinjaData ninjaData) {
        if (ninjaData.getChakra() < CHAKRA_COST) {
            player.displayClientMessage(Component.translatable("jutsu.fail.notenoughchakra",
                    Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW)), true);
            return false;
        }
        return true;
    }
}
