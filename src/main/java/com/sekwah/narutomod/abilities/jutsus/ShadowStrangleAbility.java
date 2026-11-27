package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.events.ShadowGripEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.player.Player;

public class ShadowStrangleAbility extends Ability implements Ability.Cooldown {
    private static final float COST = 30;

    @Override
    public ActivationType activationType() { return ActivationType.INSTANT; }

    @Override
    public int castPoseTicks() { return 9; }

    @Override
    public long defaultCombo() { return 333; }

    @Override
    public int getCooldown() { return 10 * 20; }

    @Override
    public SoundEvent castingSound() { return null; }

    @Override
    public boolean handleCost(Player player, INinjaData data, int chargeAmount) {
        if (!"nara".equals(data.getClanId())) {
            return failure(player, data, "jutsu.fail.nara");
        }
        if (!ShadowGripEvents.canStart(player)) {
            return failure(player, data, "jutsu.shadow.no_tether");
        }
        if (data.getChakra() < COST) {
            return failure(player, data, "jutsu.fail.notenoughchakra");
        }
        data.useChakra(COST, 20);
        return true;
    }

    private boolean failure(Player player, INinjaData data, String key) {
        player.displayClientMessage(Component.translatable(key,
                Component.translatable(this.getTranslationKey(data)).withStyle(ChatFormatting.YELLOW)), true);
        return false;
    }

    @Override
    public void performServer(Player player, INinjaData data, int ticksActive) {
        ShadowGripEvents.start(player, 3.0f * data.getRankDamageMultiplier());
    }
}
