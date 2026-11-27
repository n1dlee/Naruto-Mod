package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.events.BodyRiteEffects;
import com.sekwah.narutomod.network.s2c.BodyRitePacket.Phase;
import com.sekwah.narutomod.util.EightGateCycle;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.player.Player;

public class EightGatesAbility extends Ability implements Ability.Cooldown, Ability.AlternateActivation {
    @Override
    public boolean requiresFreeHands() {
        return false;
    }

    @Override
    public ActivationType activationType() {
        return ActivationType.INSTANT;
    }

    @Override
    public long defaultCombo() {
        return 3131;
    }

    @Override
    public int castPoseTicks() {
        return 13;
    }

    @Override
    public int getCooldown() {
        return 5;
    }

    @Override
    public SoundEvent castingSound() {
        return null;
    }

    @Override
    public boolean handleAlternate(Player actor, INinjaData data) {
        int stage = data.getGatesOpen();
        if (stage >= EightGateCycle.LAST) {
            message(actor, "gates.locked");
            return true;
        }
        if (actor.isShiftKeyDown() && stage > 0) {
            close(actor, data, Phase.GATE_CLOSE);
            return true;
        }
        if (stage != EightGateCycle.LAST - 1) {
            return false;
        }
        return switch (data.getGateCycle().confirm(BodyRiteEffects.clock(actor))) {
            case WARN -> {
                BodyRiteEffects.emit(actor, Phase.GATE_WARNING, EightGateCycle.LAST, true);
                message(actor, "gates.confirm");
                yield true;
            }
            case WAIT -> true;
            case ACCEPT -> false;
        };
    }

    @Override
    public boolean handleCost(Player actor, INinjaData data, int age) {
        int stage = data.getGatesOpen() + 1;
        if (!actor.isAlive() || actor.isSpectator() || stage > EightGateCycle.LAST || data.isGiantForm()
                || BodyRiteEffects.clock(actor) < data.getSageStoneUntil()) {
            message(actor, "gates.blocked");
            return false;
        }
        if (data.getChakra() < EightGateCycle.profile(stage).chakra()) {
            message(actor, "jutsu.fail.notenoughchakra", Component.translatable(getTranslationKey(data)));
            return false;
        }
        if (stage < EightGateCycle.LAST && data.getStamina() < stage * 0.5f) {
            message(actor, "gates.spent");
            return false;
        }
        return true;
    }

    @Override
    public void performServer(Player actor, INinjaData data, int age) {
        int stage = data.getGatesOpen() + 1;
        if (stage > EightGateCycle.LAST) {
            return;
        }
        data.useChakra(EightGateCycle.profile(stage).chakra(), 10);
        data.getGateCycle().open(stage, BodyRiteEffects.clock(actor));
        data.setGatesOpen(stage);
        data.setGatesTicks(stage == EightGateCycle.LAST ? EightGateCycle.DEATH_TICKS : EightGateCycle.STANDARD_TICKS);
        BodyRiteEffects.emit(actor, Phase.GATE_OPEN, stage, true);
        message(actor, "gates.open", Component.translatable("gates.name." + stage), stage);
    }

    public static void close(Player actor, INinjaData data, Phase phase) {
        int stage = data.getGatesOpen();
        data.setGatesOpen(0);
        data.setGatesTicks(0);
        data.getGateCycle().clear();
        BodyRiteEffects.emit(actor, phase, stage, true);
        message(actor, switch (phase) {
            case GATE_DEATH -> "gates.death";
            case GATE_SPENT -> "gates.spent";
            default -> "gates.closed";
        });
    }

    private static void message(Player actor, String key, Object... args) {
        actor.displayClientMessage(Component.translatable(key, args).withStyle(ChatFormatting.GOLD), true);
    }
}
