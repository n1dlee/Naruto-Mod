package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.events.WoodArmEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.player.Player;

public class WoodArmAbility extends Ability implements Ability.Cooldown {
    private static final float CHAKRA_COST = 45f;

    @Override
    public ActivationType activationType() { return ActivationType.INSTANT; }

    @Override
    public int castPoseTicks() { return 21; }

    @Override
    public long defaultCombo() { return 3213; }

    @Override
    public String requiredClan() { return "senju"; }

    @Override
    public String element() { return "earth"; }

    @Override
    public int elementLevelRequired() { return 4; }

    @Override
    public String secondaryElement() { return "water"; }

    @Override
    public int secondaryElementLevelRequired() { return 4; }

    @Override
    public int getCooldown() { return 8 * 20; }

    @Override
    public SoundEvent castingSound() { return null; }

    @Override
    public boolean handleCost(Player player, INinjaData ninjaData, int chargeAmount) {
        if (!WoodArmEvents.canStart(player)) {
            player.displayClientMessage(Component.translatable("jutsu.fail.mokuton.arm"), true);
            return false;
        }
        if (ninjaData.getChakra() < CHAKRA_COST) {
            player.displayClientMessage(Component.translatable("jutsu.fail.notenoughchakra",
                    Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW)), true);
            return false;
        }
        ninjaData.useChakra(CHAKRA_COST, 20);
        return true;
    }

    @Override
    public void performServer(Player player, INinjaData ninjaData, int ticksActive) {
        WoodArmEvents.start(player, ninjaData.getRankDamageMultiplier());
    }
}
