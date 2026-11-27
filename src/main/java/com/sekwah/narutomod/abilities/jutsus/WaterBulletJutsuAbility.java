package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.entity.jutsuprojectile.WaterShotEntity;
import com.sekwah.narutomod.util.WaterShotShape;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.player.Player;

public class WaterBulletJutsuAbility extends Ability implements Ability.Cooldown {

    @Override
    public ActivationType activationType() {
        return ActivationType.INSTANT;
    }

    @Override
    public long defaultCombo() {
        return 132;
    }
    @Override
    public String element() {
        return "water";
    }

    @Override
    public int elementLevelRequired() {
        return 1;
    }

    @Override
    public float elementXpReward() {
        return 15f;
    }


    @Override
    public boolean handleCost(Player player, INinjaData ninjaData, int chargeAmount) {
        if(ninjaData.getChakra() < 30) {
            player.displayClientMessage(Component.translatable("jutsu.fail.notenoughchakra", Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW)), true);
            return false;
        }
        ninjaData.useChakra(30, 30);
        return true;
    }

    @Override
    public void performServer(Player player, INinjaData ninjaData, int ticksActive) {
        float power = ninjaData.getRankDamageMultiplier();
        var dimension = player.level().dimension();
        WaterShotEntity.launchAimed(player, power);
        for (int i = 1; i < WaterShotShape.SHOTS; i++) {
            ninjaData.scheduleDelayedTickEvent(caster -> {
                if (caster.isAlive() && caster.level().dimension().equals(dimension) && ninjaData.isNinjaModeEnabled()) {
                    WaterShotEntity.launchAimed(caster, power);
                }
            }, i * WaterShotShape.SHOT_INTERVAL);
        }
    }

    @Override
    public SoundEvent castingSound() {
        return null;
    }

    @Override
    public int getCooldown() {
        return 4 * 20;
    }
}
