package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.events.IceSpikeCast;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.player.Player;

public class IceSpikesAbility extends Ability implements Ability.Cooldown {
    private static final float CHAKRA_COST = 65f;

    @Override
    public ActivationType activationType() { return ActivationType.INSTANT; }

    @Override
    public int castPoseTicks() { return 16; }

    @Override
    public long defaultCombo() { return 2311; }

    @Override
    public String element() { return "water"; }

    @Override
    public int elementLevelRequired() { return 8; }

    @Override
    public String secondaryElement() { return "wind"; }

    @Override
    public int secondaryElementLevelRequired() { return 8; }

    @Override
    public int getCooldown() { return 14 * 20; }

    @Override
    public SoundEvent castingSound() { return null; }

    @Override
    public boolean handleCost(Player player, INinjaData data, int chargeAmount) {
        if (data.getChakra() < CHAKRA_COST) {
            player.displayClientMessage(Component.translatable("jutsu.fail.notenoughchakra",
                    Component.translatable(this.getTranslationKey(data)).withStyle(ChatFormatting.YELLOW)), true);
            return false;
        }
        int mastery = Math.min(data.getElementLevel("water"), data.getElementLevel("wind"));
        if (!IceSpikeCast.start(player, mastery, Math.min(8f, 5.8f * data.getRankDamageMultiplier()))) {
            player.displayClientMessage(Component.translatable("jutsu.fail.ice_field.ground"), true);
            return false;
        }
        data.useChakra(CHAKRA_COST, 40);
        return true;
    }

    @Override
    public void performServer(Player player, INinjaData data, int ticksActive) {
    }
}
