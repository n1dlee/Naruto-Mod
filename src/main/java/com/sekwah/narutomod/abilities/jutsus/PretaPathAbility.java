package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.events.PretaGuardEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.player.Player;

public class PretaPathAbility extends Ability implements Ability.Toggled, Ability.ToggleStartCheck, Ability.HandleEnded {
    private static final float UPKEEP = 0.65f;

    @Override
    public ActivationType activationType() { return ActivationType.TOGGLE; }

    @Override
    public long defaultCombo() { return 1232; }

    @Override
    public String requiredEye() { return "rinnegan_path:preta"; }

    @Override
    public SoundEvent castingSound() { return null; }

    @Override
    public boolean canStartToggle(Player actor, INinjaData data) {
        if (data.getChakra() < UPKEEP) {
            actor.displayClientMessage(Component.translatable("jutsu.fail.notenoughchakra",
                    Component.translatable(this.getTranslationKey(data)).withStyle(ChatFormatting.YELLOW)), true);
            return false;
        }
        if (!PretaGuardEvents.start(actor, data)) {
            actor.displayClientMessage(Component.translatable("jutsu.fail.preta.guard"), true);
            return false;
        }
        return true;
    }

    @Override
    public boolean handleCost(Player actor, INinjaData data, int chargeAmount) {
        if (data.getChakra() < UPKEEP || !PretaGuardEvents.maintain(actor)) {
            return false;
        }
        data.useChakra(UPKEEP, 5);
        return true;
    }

    @Override
    public void performServer(Player actor, INinjaData data, int ticksActive) {
    }

    @Override
    public void performToggleClient(Player actor, INinjaData data) {
    }

    @Override
    public void handleAbilityEnded(Player actor, INinjaData data, int ticksActive) {
        PretaGuardEvents.end(actor);
    }
}
