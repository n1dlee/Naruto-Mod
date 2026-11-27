package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.events.KamuiPhaseEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.player.Player;

public class KamuiPhaseAbility extends Ability implements Ability.Toggled, Ability.ToggleStartCheck {
    private static final float COST = 10f;

    @Override
    public boolean requiresFreeHands() { return false; }

    @Override
    public ActivationType activationType() { return ActivationType.TOGGLE; }

    @Override
    public long defaultCombo() { return 1212; }

    @Override
    public String requiredEye() { return "sharingan_ms"; }

    @Override
    public String requiredEyeForm() { return "obito"; }

    @Override
    public SoundEvent castingSound() { return null; }

    @Override
    public boolean canStartToggle(Player player, INinjaData data) {
        if (!hasChakra(player, data)) {
            return false;
        }
        if (data.isWallWalkAttached() || data.getCurrentlyChanneledAbility() != null
                || data.getToggleAbilityData().getAbilitiesHashSet().contains(KamuiPhaseEvents.SCOUT)
                || !KamuiPhaseEvents.canStart(player)) {
            player.displayClientMessage(Component.translatable("jutsu.kamui.phase_not_clear").withStyle(ChatFormatting.GRAY), true);
            return false;
        }
        return true;
    }

    @Override
    public boolean handleCost(Player player, INinjaData data, int chargeAmount) {
        if (!hasChakra(player, data) || !this.hasEyeAccess(data)) {
            return false;
        }
        data.useChakra(COST, 5);
        return true;
    }

    @Override
    public void performServer(Player player, INinjaData data, int ticksActive) {
    }

    @Override
    public void performToggleClient(Player player, INinjaData data) {
    }

    private boolean hasChakra(Player player, INinjaData data) {
        if (data.getChakra() >= COST) {
            return true;
        }
        player.displayClientMessage(Component.translatable("jutsu.fail.notenoughchakra",
                Component.translatable(this.getTranslationKey(data)).withStyle(ChatFormatting.YELLOW)), true);
        return false;
    }
}
