package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.entity.jutsuprojectile.AmaterasuFireEntity;
import com.sekwah.narutomod.util.AmaterasuFlames;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;

public class AmaterasuAbility extends Ability implements Ability.Cooldown, Ability.AlternateActivation {
    private static final float CHAKRA_COST = 70f;

    @Override
    public boolean requiresFreeHands() {
        return false;
    }

    @Override
    public ActivationType activationType() {
        return ActivationType.INSTANT;
    }

    @Override
    public int castPoseTicks() {
        return 12;
    }

    @Override
    public long defaultCombo() {
        return 113;
    }

    @Override
    public String requiredEye() {
        return "sharingan_ms";
    }

    @Override
    public String requiredEyeForm() {
        return "itachi,sasuke";
    }

    @Override
    public boolean handleCost(Player player, INinjaData data, int chargeAmount) {
        if (data.getChakra() < CHAKRA_COST) {
            player.displayClientMessage(Component.translatable("jutsu.fail.notenoughchakra",
                    Component.translatable(this.getTranslationKey(data)).withStyle(ChatFormatting.YELLOW)), true);
            return false;
        }
        data.useChakra(CHAKRA_COST, 40);
        return true;
    }

    @Override
    public boolean handleAlternate(Player player, INinjaData data) {
        if (!player.isShiftKeyDown()) {
            return false;
        }
        int count = AmaterasuFireEntity.extinguishOwned(player);
        String key = count > 0 ? "jutsu.amaterasu.extinguished" : "jutsu.amaterasu.none_owned";
        player.displayClientMessage(Component.translatable(key).withStyle(ChatFormatting.GRAY), true);
        player.playNotifySound(SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.34f, count > 0 ? 0.8f : 1.22f);
        return true;
    }

    @Override
    public void performServer(Player player, INinjaData data, int ticksActive) {
        var aim = player.getEyePosition().add(player.getLookAngle().scale(AmaterasuFlames.RANGE));
        AmaterasuFireEntity.aim(player, aim, AmaterasuFlames.power(data.getNinjaRank()));
    }

    @Override
    public int getCooldown() {
        return 30 * 20;
    }

    @Override
    public SoundEvent castingSound() {
        return null;
    }
}
