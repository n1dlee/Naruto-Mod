package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.events.CrowFlockEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.player.Player;

public class CrowGenjutsuAbility extends Ability implements Ability.Cooldown {

    private static final float CHAKRA_COST = 55f;

    @Override
    public ActivationType activationType() {
        return ActivationType.INSTANT;
    }

    @Override
    public int castPoseTicks() {
        return 9;
    }

    @Override
    public long defaultCombo() {
        return 1133;
    }

    @Override
    public String requiredEye() {
        return "sharingan_ms";
    }

    @Override
    public String requiredEyeForm() {
        return "itachi";
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
    public boolean handleCost(Player player, INinjaData ninjaData, int chargeAmount) {
        if (!CrowFlockEvents.hasSlot(player)) {
            return false;
        }
        if (ninjaData.getChakra() < CHAKRA_COST) {
            player.displayClientMessage(Component.translatable("jutsu.fail.notenoughchakra",
                    Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW)), true);
            return false;
        }
        ninjaData.useChakra(CHAKRA_COST, 30);
        return true;
    }

    @Override
    public void performServer(Player player, INinjaData ninjaData, int ticksActive) {
        CrowFlockEvents.start(player, player.getYRot());
    }
}
