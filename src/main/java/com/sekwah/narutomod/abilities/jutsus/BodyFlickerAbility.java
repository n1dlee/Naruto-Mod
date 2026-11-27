package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.events.FlickerEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public class BodyFlickerAbility extends Ability implements Ability.Cooldown {
    private static final float CHAKRA_COST = 15f;
    private static final float STAMINA_COST = 30f;
    private static final double[] RANK_RANGES = {8, 12, 15, 25, 40};

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
        return 0;
    }

    @Override
    public long defaultCombo() {
        return 21;
    }

    @Override
    public int getCooldown() {
        return 8 * 20;
    }

    @Override
    public boolean handleCost(Player player, INinjaData data, int chargeAmount) {
        if (data.getChakra() < CHAKRA_COST) {
            return reject(player, data, "jutsu.fail.notenoughchakra");
        }
        if (data.getStamina() < STAMINA_COST) {
            return reject(player, data, "jutsu.fail.notenoughstamina");
        }
        if (!FlickerEvents.canStart(player, path(player, data))) {
            player.displayClientMessage(Component.translatable("jutsu.flicker.path_blocked"), true);
            return false;
        }
        data.useChakra(CHAKRA_COST, 20);
        data.useStamina(STAMINA_COST, 20);
        return true;
    }

    private boolean reject(Player player, INinjaData data, String key) {
        player.displayClientMessage(Component.translatable(key,
                Component.translatable(this.getTranslationKey(data)).withStyle(ChatFormatting.YELLOW)), true);
        return false;
    }

    @Override
    public void performServer(Player player, INinjaData data, int ticksActive) {
        FlickerEvents.start(player, path(player, data), null);
    }

    private static Vec3 path(Player player, INinjaData data) {
        double distance = RANK_RANGES[Math.max(0, Math.min(data.getNinjaRank(), RANK_RANGES.length - 1))];
        return player.getLookAngle().normalize().scale(distance);
    }
}
