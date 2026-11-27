package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.events.WoodCageEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.player.Player;

public class WoodReleaseAbility extends Ability implements Ability.Cooldown {
    private static final float CHAKRA_COST = 90f;

    @Override
    public ActivationType activationType() {
        return ActivationType.INSTANT;
    }

    @Override
    public int castPoseTicks() {
        return 18;
    }

    @Override
    public long defaultCombo() {
        return 3312;
    }

    @Override
    public String requiredClan() {
        return "senju";
    }

    @Override
    public String element() {
        return "earth";
    }

    @Override
    public int elementLevelRequired() {
        return 6;
    }

    @Override
    public String secondaryElement() {
        return "water";
    }

    @Override
    public int secondaryElementLevelRequired() {
        return 6;
    }

    @Override
    public int getCooldown() {
        return 25 * 20;
    }

    @Override
    public SoundEvent castingSound() {
        return null;
    }

    @Override
    public boolean handleCost(Player player, INinjaData ninjaData, int age) {
        if (WoodCageEvents.plan(player) == null) {
            player.displayClientMessage(Component.translatable("jutsu.fail.mokuton.space"), true);
            return false;
        }
        if (ninjaData.getChakra() < CHAKRA_COST) {
            player.displayClientMessage(Component.translatable("jutsu.fail.notenoughchakra",
                    Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW)), true);
            return false;
        }
        ninjaData.useChakra(CHAKRA_COST, 40);
        return true;
    }

    @Override
    public void performServer(Player player, INinjaData ninjaData, int age) {
        var plan = WoodCageEvents.plan(player);
        if (plan != null) {
            WoodCageEvents.start(player, plan);
        }
    }
}
