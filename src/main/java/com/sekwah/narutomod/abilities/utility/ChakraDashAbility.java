package com.sekwah.narutomod.abilities.utility;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.sounds.NarutoSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;

public class ChakraDashAbility extends Ability implements Ability.Toggled {

    /** Exempt from the free-hands gate: this is a movement mode, not a hand-cast technique. */
    @Override
    public boolean requiresFreeHands() {
        return false;
    }

    private static final int CHAKRA_COOLDOWN = 15;
    private static final float STAMINA_COST = 0.5F;

    @Override
    public ActivationType activationType() {
        return ActivationType.TOGGLE;
    }

    @Override
    public long defaultCombo() {
        return 2;
    }

    @Override
    public boolean handleCost(Player player, INinjaData ninjaData, int chargeAmount) {
        boolean isMoving = ninjaData.getDashStride().moving();
        float cost = isMoving ? 0.25f : 0.05f;
        if (ninjaData.getChakra() < cost) {
            player.displayClientMessage(Component.translatable("jutsu.fail.notenoughchakra",
                    Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW)), true);
            return false;
        }
        if (isMoving && ninjaData.getStamina() < STAMINA_COST) {
            player.displayClientMessage(Component.translatable("jutsu.fail.notenoughstamina",
                    Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW)), true);
            return false;
        }
        ninjaData.useChakra(cost, CHAKRA_COOLDOWN);
        if (isMoving) {
            ninjaData.useStamina(STAMINA_COST, CHAKRA_COOLDOWN);
        }
        return true;
    }

    @Override
    public void performServer(Player player, INinjaData ninjaData, int ticksActive) {
        if (ninjaData.getDashStride().starts()) {
            player.level().playSound(null, player, NarutoSounds.CHAKRA_DASH.get(), SoundSource.PLAYERS, 0.65f, 1.0f);
        }
    }

    @Override
    public void performToggleClient(Player player, INinjaData ninjaData) {
    }
}
