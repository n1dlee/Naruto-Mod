package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.entity.jutsuprojectile.FireballJutsuEntity;
import com.sekwah.narutomod.sounds.NarutoSounds;
import com.sekwah.narutomod.util.PhoenixVolley;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public class PhoenixFlowerAbility extends Ability implements Ability.Cooldown {

    private static final float CHAKRA_COST = 35f;

    @Override
    public ActivationType activationType() {
        return ActivationType.INSTANT;
    }

    @Override
    public int castPoseTicks() {
        return 20;
    }

    @Override
    public long defaultCombo() {
        return 3111;
    }

    @Override
    public int getCooldown() {
        return 10 * 20;
    }

    @Override
    public SoundEvent castingSound() {
        return NarutoSounds.PHOENIX_VOLLEY.get();
    }

    @Override
    public String element() {
        return "fire";
    }

    @Override
    public int elementLevelRequired() {
        return 6;
    }

    @Override
    public float elementXpReward() {
        return 25f;
    }

    @Override
    public boolean handleCost(Player player, INinjaData ninjaData, int chargeAmount) {
        if (ninjaData.getChakra() < CHAKRA_COST) {
            player.displayClientMessage(Component.translatable("jutsu.fail.notenoughchakra",
                    Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW)), true);
            return false;
        }
        ninjaData.useChakra(CHAKRA_COST, 30);
        return true;
    }

    @Override
    public void performServer(Player player, INinjaData data, int ticksActive) {
        float power = 6.2f * data.getRankDamageMultiplier() * ("uchiha".equals(data.getClanId()) ? 1.25f : 1);
        Vec3 aim = player.getLookAngle();
        Vec3 horizontal = Vec3.directionFromRotation(0, player.getYRot());
        var dimension = player.level().dimension();
        var volley = new PhoenixVolley();
        FireballJutsuEntity.launch(player, PhoenixVolley.heading(aim, horizontal, 0), volley, power, true);
        for (int i = 1; i < PhoenixVolley.PETALS; i++) {
            Vec3 heading = PhoenixVolley.heading(aim, horizontal, i);
            data.scheduleDelayedTickEvent(caster -> {
                if (caster.level().dimension().equals(dimension) && data.isNinjaModeEnabled()) {
                    FireballJutsuEntity.launch(caster, heading, volley, power, false);
                }
            }, PhoenixVolley.WINDUP + i * PhoenixVolley.INTERVAL);
        }
    }
}
