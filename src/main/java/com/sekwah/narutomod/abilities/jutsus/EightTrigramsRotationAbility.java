package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.events.KaitenEvents;
import com.sekwah.narutomod.util.KaitenDome;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

public class EightTrigramsRotationAbility extends Ability implements Ability.Channeled, Ability.Cooldown {
    private static final float CHAKRA_BASE = 45f;
    private static final float CHAKRA_PER_TICK = 2f;

    @Override
    public ActivationType activationType() {
        return ActivationType.CHANNELED;
    }

    @Override
    public long defaultCombo() {
        return 1111;
    }

    @Override
    public int getCooldown() {
        return 20 * 20;
    }

    @Override
    public boolean canActivateBelowMinCharge() {
        return false;
    }

    @Override
    public boolean useChargedMessages() {
        return false;
    }

    @Override
    public boolean handleCost(Player player, INinjaData ninjaData, int chargeAmount) {
        if (!"hyuga".equals(ninjaData.getClanId())) {
            player.displayClientMessage(Component.translatable("jutsu.fail.hyuga",
                    Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW)), true);
            return false;
        }
        boolean opening = chargeAmount == 0;
        if (opening ? !KaitenEvents.canStart(player) : !KaitenEvents.spinning(player)) {
            return false;
        }
        float cost = opening ? CHAKRA_BASE : CHAKRA_PER_TICK;
        if (ninjaData.getChakra() < cost) {
            if (opening) {
                player.displayClientMessage(Component.translatable("jutsu.fail.notenoughchakra",
                        Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW)), true);
            }
            return false;
        }
        ninjaData.useChakra(cost, opening ? 5 : 2);
        return true;
    }

    @Override
    public void handleChannelling(Player player, INinjaData ninjaData, int ticksActive) {
        if (ticksActive == 0) {
            KaitenEvents.start(player, 6 * ninjaData.getRankDamageMultiplier(), KaitenDome.MAX_ACTIVE);
        } else {
            KaitenEvents.maintain(player);
        }
    }

    @Override
    public void performServer(Player player, INinjaData ninjaData, int ticksActive) {
        KaitenEvents.end(player);
    }
}
