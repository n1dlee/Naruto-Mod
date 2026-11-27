package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.entity.jutsuprojectile.GustFrontEntity;
import com.sekwah.narutomod.item.weapons.GunbaiItem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.player.Player;

public class GunbaiWindAbility extends Ability implements Ability.Cooldown {
    private static final float CHAKRA_COST = 70f;

    @Override
    public ActivationType activationType() { return ActivationType.INSTANT; }

    @Override
    public boolean requiresFreeHands() { return false; }

    @Override
    public int castPoseTicks() { return 0; }

    @Override
    public long defaultCombo() { return 1122; }

    @Override
    public String requiredEye() { return "sharingan_ems"; }

    @Override
    public String requiredEyeForm() { return "madara"; }

    @Override
    public int getCooldown() { return 20 * 20; }

    @Override
    public SoundEvent castingSound() { return null; }

    @Override
    public boolean handleCost(Player player, INinjaData data, int chargeAmount) {
        if (GunbaiItem.heldHand(player) == null) {
            player.displayClientMessage(Component.translatable("jutsu.gunbai.needed").withStyle(ChatFormatting.GRAY), true);
            return false;
        }
        if (data.getChakra() < CHAKRA_COST) {
            player.displayClientMessage(Component.translatable("jutsu.fail.notenoughchakra",
                    Component.translatable(this.getTranslationKey(data)).withStyle(ChatFormatting.YELLOW)), true);
            return false;
        }
        data.useChakra(CHAKRA_COST, 30);
        return true;
    }

    @Override
    public void performServer(Player player, INinjaData data, int ticksActive) {
        var hand = GunbaiItem.heldHand(player);
        if (hand != null) {
            GustFrontEntity.fan(player, player.getLookAngle(), hand, data.getRankDamageMultiplier(), true);
        }
    }
}
