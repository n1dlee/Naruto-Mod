package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.abilities.NarutoAbilities;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.events.BodyRiteEffects;
import com.sekwah.narutomod.network.s2c.BodyRitePacket.Phase;
import com.sekwah.narutomod.util.EightGateCycle;
import com.sekwah.narutomod.util.SageFocus;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;

public class SageModeAbility extends Ability implements Ability.Channeled, Ability.Cooldown, Ability.AlternateActivation {
    private static final float GATHER_COST = 1;
    private static final float ACTIVATION_COST = 80;

    @Override
    public boolean requiresFreeHands() {
        return false;
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
        return 1200;
    }

    @Override
    public SoundEvent castingSound() {
        return null;
    }

    @Override
    public boolean canActivateBelowMinCharge() {
        return false;
    }

    @Override
    public boolean hideChannelMessages() {
        return true;
    }

    @Override
    public boolean handleAlternate(Player player, INinjaData data) {
        if (!data.isSageModeActive()) {
            return false;
        }
        endActive(player, data);
        return true;
    }

    @Override
    public boolean handleCost(Player player, INinjaData data, int ticksActive) {
        if (!compatible(player, data)) {
            interrupt(player, data);
            player.displayClientMessage(Component.translatable("sage.fail.unavailable").withStyle(ChatFormatting.YELLOW), true);
            return false;
        }
        if (data.getNinjaRank() < 2 || data.getChakra() < GATHER_COST) {
            interrupt(player, data);
            String key = data.getNinjaRank() < 2 ? "jutsu.fail.rank.chunin" : "jutsu.fail.notenoughchakra";
            player.displayClientMessage(Component.translatable(key, Component.translatable(getTranslationKey(data)))
                    .withStyle(ChatFormatting.YELLOW), true);
            return false;
        }
        data.useChakra(GATHER_COST, 5);
        return true;
    }

    @Override
    public void handleChannelling(Player player, INinjaData data, int age) {
        var focus = data.getSageFocus();
        if (age == 0) {
            focus.reset();
        }
        var sample = focus.advance(data.getSageCharge(), player.position(), stable(player, data));
        data.setSageCharge(sample.charge());
        if (sample.beat() == SageFocus.Beat.PETRIFY) {
            petrify(player, data);
            return;
        }
        if (focus.publish(sample.beat(), age)) {
            drawFocus(player, sample, age);
        }
    }

    private static void drawFocus(Player actor, SageFocus.Sample sample, int age) {
        Phase phase = switch (sample.beat()) {
            case DRAW -> Phase.SAGE_DRAW;
            case BALANCED -> Phase.SAGE_BALANCE;
            case OVERFLOW -> Phase.SAGE_OVERFLOW;
            case FRACTURE -> Phase.SAGE_BREAK;
            case PETRIFY -> Phase.SAGE_STONE;
        };
        boolean accent = age == 0 || age % 40 == 0;
        BodyRiteEffects.emit(actor, phase, sample.intensity(), accent);
        if (accent && phase == Phase.SAGE_OVERFLOW) {
            actor.displayClientMessage(Component.translatable("sage.overcharge.warning").withStyle(ChatFormatting.GOLD), true);
        }
    }

    @Override
    public void performServer(Player player, INinjaData data, int age) {
        if (age <= 0 || !compatible(player, data)
                || !data.getSageFocus().canRelease(data.getSageCharge(), player.position(), stable(player, data))) {
            BodyRiteEffects.emit(player, Phase.SAGE_BREAK, 0, true);
            player.displayClientMessage(Component.translatable(data.getSageCharge() < SageFocus.MINIMUM
                    ? "sage.fail.notenoughcharge" : "sage.fail.moving").withStyle(ChatFormatting.YELLOW), true);
            return;
        }
        if (data.getChakra() < ACTIVATION_COST) {
            BodyRiteEffects.emit(player, Phase.SAGE_BREAK, 0, true);
            player.displayClientMessage(Component.translatable("jutsu.fail.notenoughchakra",
                    Component.translatable(getTranslationKey(data))).withStyle(ChatFormatting.YELLOW), true);
            return;
        }
        int charge = data.getSageCharge();
        int duration = SageFocus.duration(charge);
        data.useChakra(ACTIVATION_COST, 40);
        data.setSageModeTicks(duration);
        data.setSageModeActive(true);
        data.setSageCharge(0);
        data.getSageFocus().commit();
        BodyRiteEffects.emit(player, Phase.SAGE_ENTER, charge, true);
        player.displayClientMessage(Component.translatable("sage.activate", duration / 20).withStyle(ChatFormatting.GREEN), true);
    }

    @Override
    public boolean channelCommittedAt(INinjaData data, int age) {
        return data.getSageFocus().committed();
    }

    public static boolean compatible(Player actor, INinjaData data) {
        return actor.isAlive() && !actor.isSpectator() && data.isNinjaModeEnabled() && !data.isGiantForm()
                && data.getGatesOpen() < EightGateCycle.LAST && BodyRiteEffects.clock(actor) >= data.getSageStoneUntil();
    }

    public static void endActive(Player actor, INinjaData data) {
        data.setSageModeActive(false);
        BodyRiteEffects.emit(actor, Phase.SAGE_EXIT, 0, true);
        actor.displayClientMessage(Component.translatable("sage.deactivate").withStyle(ChatFormatting.GRAY), true);
    }

    public static void interrupt(Player actor, INinjaData data) {
        if (NarutoAbilities.SAGE_MODE.getId().equals(data.getCurrentlyChanneledAbility())) {
            data.setCurrentlyChanneledAbility(actor, null);
            BodyRiteEffects.emit(actor, Phase.SAGE_BREAK, 0, true);
        }
        data.getSageFocus().reset();
    }

    private static boolean stable(Player actor, INinjaData data) {
        return actor.onGround() && !actor.isSprinting() && !actor.isPassenger() && !actor.isFallFlying()
                && !actor.isInWaterOrBubble() && !data.isWallWalkAttached();
    }

    private static void petrify(Player actor, INinjaData data) {
        data.setCurrentlyChanneledAbility(actor, null);
        data.getSageFocus().reset();
        data.setSageStoneUntil(BodyRiteEffects.clock(actor) + SageFocus.STONE_TICKS);
        actor.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, SageFocus.STONE_TICKS, 9, false, false));
        actor.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, SageFocus.STONE_TICKS, 9, false, false));
        actor.hurt(actor.damageSources().magic(), 6.0f);
        BodyRiteEffects.emit(actor, Phase.SAGE_STONE, 100, true);
        actor.displayClientMessage(Component.translatable("sage.petrify").withStyle(ChatFormatting.DARK_RED), true);
    }
}
