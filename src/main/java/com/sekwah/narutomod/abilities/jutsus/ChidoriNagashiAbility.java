package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.events.NagashiEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.player.Player;

/**
 * Lightning Style: Chidori Nagashi (combo 221) — Sasuke's omnidirectional release:
 * instead of concentrating the current in one hand, it's discharged through the whole
 * body, electrocuting and paralysing everything in melee range in every direction.
 *
 * Canon flow preserved: it's an EXTENSION of an active Chidori — you must have the
 * Chidori buff window running (combo 22 first), and releasing the current consumes it.
 */
public class ChidoriNagashiAbility extends Ability implements Ability.Cooldown {

    private static final float CHAKRA_COST = 35f;
    private static final double RADIUS = 3.5;
    private static final float DAMAGE = 10.0f;
    private static final int PARALYSIS_TICKS = 2 * 20;

    @Override
    public ActivationType activationType() {
        return ActivationType.INSTANT;
    }

    @Override
    public long defaultCombo() {
        return 221;
    }

    @Override
    public int castPoseTicks() {
        return NagashiEvents.BRACE_TICKS;
    }

    @Override
    public int getCooldown() {
        return 15 * 20;
    }

    @Override
    public SoundEvent castingSound() {
        return null;
    }

    @Override
    public String element() {
        return "lightning";
    }

    @Override
    public int elementLevelRequired() {
        return 12;
    }

    @Override
    public float elementXpReward() {
        return 35f;
    }


    /** The formed Chidori is what gets released — see Ability#builtOn. */
    @Override
    public HandsBusy builtOn() {
        return HandsBusy.CHIDORI;
    }

    @Override
    public boolean handleCost(Player player, INinjaData ninjaData, int chargeAmount) {
        if (!ninjaData.isChidoriActive()) {
            player.displayClientMessage(Component.translatable("jutsu.chidori_nagashi.needs_chidori")
                    .withStyle(ChatFormatting.RED), true);
            return false;
        }
        if (ninjaData.getChakra() < CHAKRA_COST) {
            player.displayClientMessage(Component.translatable("jutsu.fail.notenoughchakra",
                    Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW)), true);
            return false;
        }
        ninjaData.useChakra(CHAKRA_COST, 25);
        return true;
    }

    @Override
    public void performServer(Player player, INinjaData ninjaData, int ticksActive) {
        if (!ninjaData.isChidoriActive()) {
            return;
        }
        ninjaData.setChidoriTicks(0);
        float damage = DAMAGE * ninjaData.getRankDamageMultiplier() * ninjaData.getClanLightningDamageMultiplier();
        NagashiEvents.start(player, new NagashiEvents.Profile(RADIUS, damage, PARALYSIS_TICKS, 3, true));
    }
}
