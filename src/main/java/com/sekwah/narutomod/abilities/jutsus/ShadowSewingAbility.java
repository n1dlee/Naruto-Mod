package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.events.ShadowSewingEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.player.Player;

public class ShadowSewingAbility extends Ability implements Ability.Cooldown {
    private static final float COST = 45;

    @Override
    public ActivationType activationType() { return ActivationType.INSTANT; }

    @Override
    public int castPoseTicks() { return 12; }

    @Override
    public long defaultCombo() { return 332; }

    @Override
    public int getCooldown() { return 12 * 20; }

    @Override
    public SoundEvent castingSound() { return null; }

    @Override
    public boolean handleCost(Player player, INinjaData data, int chargeAmount) {
        if (!"nara".equals(data.getClanId())) {
            return failure(player, data, "jutsu.fail.nara");
        }
        if (!ShadowSewingEvents.canStart(player)) {
            return failure(player, data, "jutsu.shadow.ground");
        }
        if (data.getChakra() < COST) {
            return failure(player, data, "jutsu.fail.notenoughchakra");
        }
        data.useChakra(COST, 30);
        return true;
    }

    private boolean failure(Player player, INinjaData data, String key) {
        player.displayClientMessage(Component.translatable(key,
                Component.translatable(this.getTranslationKey(data)).withStyle(ChatFormatting.YELLOW)), true);
        return false;
    }

    @Override
    public void performServer(Player player, INinjaData data, int ticksActive) {
        ShadowSewingEvents.start(player, 8 * data.getClanJutsuRangeMultiplier(), 6f * data.getRankDamageMultiplier(), null);
    }
}
