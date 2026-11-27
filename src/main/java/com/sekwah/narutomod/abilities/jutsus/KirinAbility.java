package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.entity.jutsuprojectile.KirinStrikeEntity;
import com.sekwah.narutomod.util.EyeTargeting;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public class KirinAbility extends Ability implements Ability.Cooldown, Ability.Channeled {
    private static final float CHARGE_TICK_COST = 1.5f;
    private static final int CHARGE_TICKS = 60;
    private static final double RANGE = 24.0;

    @Override
    public ActivationType activationType() {
        return ActivationType.CHANNELED;
    }

    @Override
    public long defaultCombo() {
        return 1121;
    }

    @Override
    public String element() {
        return "lightning";
    }

    @Override
    public int elementLevelRequired() {
        return 15;
    }

    @Override
    public int getCooldown() {
        return 45 * 20;
    }

    @Override
    public SoundEvent castingSound() {
        return null;
    }

    @Override
    public boolean canActivateBelowMinCharge() {
        return false;
    }

    @Override
    public boolean channelCommittedAt(int ticksChanneled) {
        return ticksChanneled >= CHARGE_TICKS;
    }

    @Override
    public boolean handleCost(Player player, INinjaData ninjaData, int chargeAmount) {
        if (ninjaData.getChakra() < CHARGE_TICK_COST) {
            player.displayClientMessage(Component.translatable("jutsu.fail.notenoughchakra",
                    Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW)), true);
            return false;
        }
        ninjaData.useChakra(CHARGE_TICK_COST, 40);
        return true;
    }

    @Override
    public void handleChannelling(Player player, INinjaData ninjaData, int ticksChanneled) {
        if (ticksChanneled % 6 == 0 && player.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, player.getX(), player.getEyeY(),
                    player.getZ(), 4 + Math.min(6, ticksChanneled / 10), 0.37, 0.38, 0.37, 0.055);
        }
    }

    @Override
    public void performServer(Player player, INinjaData ninjaData, int ticksActive) {
        if (ticksActive < CHARGE_TICKS) {
            player.displayClientMessage(Component.translatable("jutsu.kirin.uncharged").withStyle(ChatFormatting.GRAY), true);
            return;
        }
        LivingEntity target = EyeTargeting.raycastLiving(player, RANGE);
        if (target == null) {
            this.failure(player, ninjaData, "jutsu.fail.notarget");
            return;
        }
        float scale = ninjaData.getRankDamageMultiplier();
        if (!KirinStrikeEntity.mark(player, target, new KirinStrikeEntity.Profile(34f * scale, 10f * scale, 6))) {
            this.failure(player, ninjaData, "jutsu.fail.nosky");
        }
    }

    private void failure(Player player, INinjaData ninjaData, String key) {
        player.displayClientMessage(Component.translatable(key,
                Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW))
                .withStyle(ChatFormatting.RED), true);
    }
}
