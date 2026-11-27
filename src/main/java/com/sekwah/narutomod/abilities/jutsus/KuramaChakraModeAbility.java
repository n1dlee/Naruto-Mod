package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.events.FinalChakraEffects;
import com.sekwah.narutomod.network.s2c.FinalChakraPacket.Phase;
import com.sekwah.narutomod.network.s2c.FinalChakraPacket.Shape;
import com.sekwah.narutomod.sounds.NarutoSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.player.Player;

public class KuramaChakraModeAbility extends Ability implements Ability.Toggled, Ability.ToggleStartCheck, Ability.HandleEnded {
    public static final ResourceLocation ID = new ResourceLocation("narutomod", "kurama_chakra_mode");
    private static final float START = 10;
    private static final float MIN_BOND = 40;

    @Override
    public ActivationType activationType() {
        return ActivationType.TOGGLE;
    }

    @Override
    public long defaultCombo() {
        return 13232;
    }

    @Override
    public boolean requiresFreeHands() {
        return false;
    }

    @Override
    public boolean canStartToggle(Player actor, INinjaData data) {
        if (!eligible(actor, data)) {
            actor.displayClientMessage(Component.translatable("kcm.unavailable").withStyle(ChatFormatting.YELLOW), true);
            return false;
        }
        if (data.getKuramaBond() < MIN_BOND) {
            actor.displayClientMessage(Component.translatable("kurama.cloak.spent").withStyle(ChatFormatting.YELLOW), true);
            return false;
        }
        return true;
    }

    @Override
    public boolean handleCost(Player actor, INinjaData data, int ticks) {
        return eligible(actor, data) && data.getKuramaBond() >= (data.isKcmActive() ? 30 : MIN_BOND);
    }

    @Override
    public void performServer(Player actor, INinjaData data, int age) {
        if (!data.isKcmActive()) {
            data.useKuramaBond(START);
            data.setKcmActive(true);
            cue(actor, Phase.KCM_ENTER, NarutoSounds.KCM_KINDLE.get());
            actor.displayClientMessage(Component.translatable("kcm.open").withStyle(ChatFormatting.GOLD), true);
        }
    }

    @Override
    public void handleAbilityEnded(Player actor, INinjaData data, int ticks) {
        standDown(actor, data);
    }

    public static void standDown(Player actor, INinjaData data) {
        data.getToggleAbilityData().removeAbility(ID);
        if (data.isKcmActive()) {
            data.setKcmActive(false);
            cue(actor, Phase.KCM_EXIT, NarutoSounds.KCM_DIM.get());
        }
    }

    @Override
    public SoundEvent castingSound() {
        return null;
    }

    @Override
    public void performToggleClient(Player actor, INinjaData data) {
    }

    private static boolean eligible(Player actor, INinjaData data) {
        return actor.isAlive() && !actor.isSpectator() && data.isNinjaModeEnabled() && !data.isSusanooActive()
                && "uzumaki".equals(data.getClanId()) && data.getNinjaRank() >= 3;
    }

    private static void cue(Player actor, Phase phase, SoundEvent sound) {
        var shape = new Shape(actor.position(), actor.getLookAngle(), 1.15f, 0xFFD87A);
        FinalChakraEffects.emit(actor, phase, shape);
        FinalChakraEffects.sound(actor, shape, sound, 0.72f, 1);
    }
}
