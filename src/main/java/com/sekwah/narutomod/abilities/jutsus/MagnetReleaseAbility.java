package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.entity.jutsuprojectile.IronSandEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.player.Player;

public class MagnetReleaseAbility extends Ability implements Ability.Cooldown {
    private static final float CHAKRA_COST = 40f;

    @Override
    public ActivationType activationType() { return ActivationType.INSTANT; }

    @Override
    public long defaultCombo() { return 3321; }

    @Override
    public int getCooldown() { return 12 * 20; }

    @Override
    public int castPoseTicks() { return 12; }

    @Override
    public SoundEvent castingSound() { return null; }

    @Override
    public boolean handleCost(Player player, INinjaData data, int chargeAmount) {
        if (data.getChakra() < CHAKRA_COST) {
            player.displayClientMessage(Component.translatable("jutsu.fail.notenoughchakra",
                    Component.translatable(this.getTranslationKey(data)).withStyle(ChatFormatting.YELLOW)), true);
            return false;
        }
        if (!IronSandEntity.canLaunch(player)) {
            player.displayClientMessage(Component.translatable("jutsu.fail.iron_sand.space"), true);
            return false;
        }
        return true;
    }

    @Override
    public void performServer(Player player, INinjaData data, int ticksActive) {
        if (data.getChakra() >= CHAKRA_COST && IronSandEntity.launch(player, data.getRankDamageMultiplier())) {
            data.useChakra(CHAKRA_COST, 15);
        }
    }
}
