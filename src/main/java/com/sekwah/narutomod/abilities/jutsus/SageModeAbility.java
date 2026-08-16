package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import org.joml.Vector3f;

/**
 * Sage Mode — CHANNELED ability (combo 1231).
 *
 * HOLD the combo keys to gather natural energy (sageCharge increases each tick).
 * RELEASE when you have enough charge (>=50) to activate Sage Mode.
 * If you hold too long past max charge (100), overcharge ticks accumulate
 * and you risk petrification.
 *
 * Cost: 1.0 chakra/tick while channeling, 80 upfront on activation.
 * Cooldown: 60 seconds (only after successful activation).
 */
public class SageModeAbility extends Ability implements Ability.Channeled, Ability.Cooldown {

    /** Exempt from the free-hands gate: this is a transformation being worn, not a hand-cast technique. */
    @Override
    public boolean requiresFreeHands() {
        return false;
    }

    private static final float CHAKRA_PER_TICK = 1.0f;
    private static final float ACTIVATION_COST = 80f;
    private static final int MIN_CHARGE_FOR_ACTIVATION = 50;
    private static final int SAGE_MAX_CHARGE = 100;
    private static final int PETRIFY_THRESHOLD = 300; // ticks spent sitting at max charge

    /**
     * Ticks held at a hundred charge. Reset the moment the gather drops below it.
     *
     * Per-ability rather than per-player because only one Sage Mode can be gathered at a time
     * and the value is meaningless outside a live channel.
     */
    private int ticksAtMaxCharge = 0;

    /**
     * Whether the last release actually turned Sage Mode on.
     *
     * Sixty seconds of cooldown used to be charged for a release that failed on insufficient
     * charge or chakra - the two most likely ways for a new player to get this wrong - so
     * fumbling the technique locked them out of it for a minute.
     */
    private boolean lastActivationSucceeded = false;

    /** Only a release that actually activated Sage Mode is a cast worth paying for. */
    @Override
    public boolean channelCommittedAt(int ticksChanneled) {
        return this.lastActivationSucceeded;
    }

    @Override
    public ActivationType activationType() {
        return ActivationType.CHANNELED;
    }

    @Override
    public long defaultCombo() {
        return 1231;
    }

    @Override
    public int getCooldown() {
        return 60 * 20;
    }

    /**
     * Allow releasing below min charge — it just won't activate sage mode,
     * but won't error either (charge is saved for next attempt).
     */
    @Override
    public boolean canActivateBelowMinCharge() {
        return true;
    }

    @Override
    public boolean useChargedMessages() {
        return false; // We send custom messages
    }

    @Override
    public boolean handleCost(Player player, INinjaData ninjaData, int chargeAmount) {
        // If sage mode already active — toggle off
        if (ninjaData.isSageModeActive()) {
            ninjaData.setSageModeActive(false);
            player.displayClientMessage(Component.translatable("sage.deactivate").withStyle(ChatFormatting.RED), true);
            return false;
        }
        if (ninjaData.getNinjaRank() < 2) {
            player.displayClientMessage(Component.translatable("jutsu.fail.rank.chunin",
                    Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW)), true);
            return false;
        }
        if (ninjaData.getChakra() < CHAKRA_PER_TICK) {
            player.displayClientMessage(Component.translatable("jutsu.fail.notenoughchakra",
                    Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW)), true);
            return false;
        }
        ninjaData.useChakra(CHAKRA_PER_TICK, 5);
        return true;
    }

    /**
     * Called every tick while holding the combo keys.
     * Gathers natural energy — increments sageCharge.
     * If overcharging past max, warns and eventually petrifies.
     */
    @Override
    public void handleChannelling(Player player, INinjaData ninjaData, int ticksChanneled) {
        int currentCharge = ninjaData.getSageCharge();

        if (currentCharge < SAGE_MAX_CHARGE) {
            // Gather natural energy: 1 charge per tick
            ninjaData.setSageCharge(currentCharge + 1);

            // Gathering particles every 5 ticks
            this.ticksAtMaxCharge = 0;
            if (ticksChanneled % 5 == 0 && player.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(
                        new DustParticleOptions(new Vector3f(1.0f, 0.7f, 0.1f), 1.2f),
                        player.getX(), player.getY() + 1.0, player.getZ(),
                        3, 0.3, 0.5, 0.3, 0.02);
                serverLevel.sendParticles(ParticleTypes.END_ROD,
                        player.getX(), player.getY() + 0.5, player.getZ(),
                        1, 0.2, 0.3, 0.2, 0.01);
            }
        } else {
            // Overcharging. The clock that matters is how long the gatherer has been sitting
            // AT full, not how long they have been channelling: a slow gather that only just
            // reached a hundred was being petrified on arrival, because the old test compared
            // total channel time against the threshold. The expression that was supposed to
            // correct for that was computed into a local and then never read.
            this.ticksAtMaxCharge++;

            if (ticksChanneled % 20 == 0) {
                player.displayClientMessage(
                        Component.translatable("sage.overcharge.warning").withStyle(ChatFormatting.GOLD), true);
            }

            // Petrification after holding too long at max
            if (this.ticksAtMaxCharge > PETRIFY_THRESHOLD && currentCharge >= SAGE_MAX_CHARGE) {
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 10 * 20, 9, false, true));
                player.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 10 * 20, 9, false, true));
                player.hurt(player.damageSources().magic(), 6.0f);
                ninjaData.setSageCharge(0);
                player.displayClientMessage(
                        Component.translatable("sage.petrify").withStyle(ChatFormatting.DARK_RED), true);
            }
        }
    }

    /**
     * Called when the player releases the combo keys.
     * If enough charge accumulated, activate Sage Mode.
     */
    @Override
    public void performServer(Player player, INinjaData ninjaData, int ticksActive) {
        this.lastActivationSucceeded = false;
        this.ticksAtMaxCharge = 0;
        int charge = ninjaData.getSageCharge();

        if (charge < MIN_CHARGE_FOR_ACTIVATION) {
            // Not enough charge — just stop gathering, keep what they have
            player.displayClientMessage(
                    Component.translatable("sage.fail.notenoughcharge").withStyle(ChatFormatting.YELLOW), true);
            return;
        }

        // Check activation chakra cost
        if (ninjaData.getChakra() < ACTIVATION_COST) {
            player.displayClientMessage(Component.translatable("jutsu.fail.notenoughchakra",
                    Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW)), true);
            return;
        }

        ninjaData.useChakra(ACTIVATION_COST, 40);

        // Convert charge to duration: charge * 12 ticks (100 charge = 60 seconds)
        int duration = charge * 12;
        ninjaData.setSageModeTicks(duration);
        ninjaData.setSageModeActive(true);
        ninjaData.setSageCharge(0);
        this.lastActivationSucceeded = true;

        player.displayClientMessage(
                Component.translatable("sage.activate",
                        Component.literal(String.valueOf(duration / 20)).withStyle(ChatFormatting.GOLD))
                        .withStyle(ChatFormatting.GREEN), true);
    }
}
