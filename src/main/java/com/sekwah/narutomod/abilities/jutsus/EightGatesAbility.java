package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

/**
 * Eight Gates (Hachimon Tonkou) — combo 3131, INSTANT.
 * Each use opens the next gate (1→8).
 * Effects are applied in NinjaData.updateEightGates().
 * Cost: 15 chakra per gate. Cooldown: 30 seconds after all gates close.
 */
public class EightGatesAbility extends Ability {

    private static final float CHAKRA_PER_GATE = 20f;
    private static final String[] GATE_NAMES = {
        "Gate of Opening", "Gate of Healing", "Gate of Life", "Gate of Pain",
        "Gate of Limit", "Gate of View", "Gate of Wonder", "Gate of Death"
    };

    @Override
    public ActivationType activationType() {
        return ActivationType.INSTANT;
    }

    @Override
    public long defaultCombo() {
        return 3131;
    }

    @Override
    public boolean handleCost(Player player, INinjaData ninjaData, int chargeAmount) {
        int currentGates = ninjaData.getGatesOpen();
        if (currentGates >= 8) {
            player.displayClientMessage(Component.literal("All gates already open!")
                    .withStyle(ChatFormatting.RED), true);
            return false;
        }

        if (ninjaData.getChakra() < CHAKRA_PER_GATE) {
            player.displayClientMessage(Component.translatable("jutsu.fail.notenoughchakra",
                    Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW)), true);
            return false;
        }
        ninjaData.useChakra(CHAKRA_PER_GATE, 10);
        return true;
    }

    @Override
    public void performServer(Player player, INinjaData ninjaData, int ticksActive) {
        int nextGate = ninjaData.getGatesOpen() + 1;
        ninjaData.setGatesOpen(nextGate);
        ninjaData.setGatesTicks(nextGate == 8 ? 15 * 20 : 30 * 20); // Reset timer

        String gateName = GATE_NAMES[nextGate - 1];
        ChatFormatting color = nextGate <= 4 ? ChatFormatting.GREEN :
                               nextGate <= 7 ? ChatFormatting.RED : ChatFormatting.DARK_RED;

        player.displayClientMessage(
                Component.literal(gateName + " — OPEN! (Gate " + nextGate + "/8)")
                        .withStyle(color), true);
    }
}
