package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.entity.jutsuprojectile.ChibakuTenseiEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.player.Player;

public class ChibakuTenseiAbility extends Ability implements Ability.Cooldown, Ability.AlternateActivation {
    private static final float CHAKRA_COST = 220f;

    @Override
    public ActivationType activationType() { return ActivationType.INSTANT; }

    @Override
    public int castPoseTicks() { return 27; }

    @Override
    public long defaultCombo() { return 1323; }

    @Override
    public String requiredEye() { return "rinnegan"; }

    @Override
    public int getCooldown() { return 60 * 20; }

    @Override
    public SoundEvent castingSound() { return null; }

    @Override
    public boolean handleAlternate(Player player, INinjaData data) { return ChibakuTenseiEntity.cancel(player); }

    @Override
    public boolean handleCost(Player player, INinjaData data, int chargeAmount) {
        if (data.getChakra() < CHAKRA_COST) {
            player.displayClientMessage(Component.translatable("jutsu.fail.notenoughchakra",
                    Component.translatable(this.getTranslationKey(data)).withStyle(ChatFormatting.YELLOW)), true);
            return false;
        }
        if (!ChibakuTenseiEntity.launch(player)) {
            player.displayClientMessage(Component.translatable("jutsu.fail.chibaku.space"), true);
            return false;
        }
        data.useChakra(CHAKRA_COST, 60);
        return true;
    }

    @Override
    public void performServer(Player player, INinjaData data, int ticksActive) {
    }
}
