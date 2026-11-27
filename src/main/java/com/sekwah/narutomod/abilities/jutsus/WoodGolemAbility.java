package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.events.WoodGolemSummons;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.player.Player;

public class WoodGolemAbility extends Ability implements Ability.Toggled, Ability.ToggleStartCheck, Ability.HandleEnded {
    private static final float SUMMON_COST = 200f;
    private static final float UPKEEP_PER_TICK = 2.5f;

    @Override
    public ActivationType activationType() { return ActivationType.TOGGLE; }

    @Override
    public long defaultCombo() { return 3222; }

    @Override
    public String requiredClan() { return "senju"; }

    @Override
    public String element() { return "earth"; }

    @Override
    public int elementLevelRequired() { return 12; }

    @Override
    public String secondaryElement() { return "water"; }

    @Override
    public int secondaryElementLevelRequired() { return 12; }

    @Override
    public SoundEvent castingSound() { return null; }

    @Override
    public boolean canStartToggle(Player player, INinjaData data) {
        if (data.getChakra() < SUMMON_COST) {
            player.displayClientMessage(Component.translatable("jutsu.fail.notenoughchakra",
                    Component.translatable(this.getTranslationKey(data)).withStyle(ChatFormatting.YELLOW)), true);
            return false;
        }
        if (!WoodGolemSummons.summon(player)) {
            player.displayClientMessage(Component.translatable("jutsu.fail.mokuton.golem_space"), true);
            return false;
        }
        data.useChakra(SUMMON_COST, 60);
        return true;
    }

    @Override
    public boolean handleCost(Player player, INinjaData data, int chargeAmount) {
        if (data.getChakra() < UPKEEP_PER_TICK || !WoodGolemSummons.refresh(player)) {
            return false;
        }
        data.useChakra(UPKEEP_PER_TICK, 10);
        return true;
    }

    @Override
    public void performServer(Player player, INinjaData data, int ticksActive) {
    }

    @Override
    public void performToggleClient(Player player, INinjaData data) {
    }

    @Override
    public void handleAbilityEnded(Player player, INinjaData data, int ticksActive) {
        WoodGolemSummons.end(player);
    }
}
