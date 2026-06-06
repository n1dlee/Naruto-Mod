package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.sounds.NarutoSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class ChidoriAbility extends Ability implements Ability.Cooldown {

    private static final float BASE_COST = 60.0F;
    private static final int ACTIVE_TICKS = 8 * 20;
    private static final DustParticleOptions CHIDORI_PARTICLE = new DustParticleOptions(new Vector3f(0.45F, 0.85F, 1.0F), 1.0F);

    @Override
    public ActivationType activationType() {
        return ActivationType.INSTANT;
    }

    @Override
    public long defaultCombo() {
        return 22; // V V — activates Chidori buff mode
    }

    @Override
    public int getCooldown() {
        return 8 * 20;
    }

    @Override
    public boolean handleCost(Player player, INinjaData ninjaData, int chargeAmount) {
        if (!"uchiha".equals(ninjaData.getClanId())) {
            player.displayClientMessage(Component.translatable("jutsu.fail.uchiha",
                    Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW)), true);
            return false;
        }
        if (ninjaData.getSharinganLevel() < 2) {
            player.displayClientMessage(Component.translatable("jutsu.fail.sharingan.two_tomoe",
                    Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW)), true);
            return false;
        }
        if (ninjaData.getChakra() < BASE_COST) {
            player.displayClientMessage(Component.translatable("jutsu.fail.notenoughchakra",
                    Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW)), true);
            return false;
        }
        ninjaData.useChakra(BASE_COST, 20);
        return true;
    }

    @Override
    public void performServer(Player player, INinjaData ninjaData, int ticksActive) {
        ninjaData.setChidoriTicks(ACTIVE_TICKS);
        if (player.level() instanceof ServerLevel serverLevel) {
            Vec3 pos = player.position().add(0.0D, player.getBbHeight() * 0.65D, 0.0D);
            serverLevel.sendParticles(CHIDORI_PARTICLE, pos.x, pos.y, pos.z, 16, 0.35D, 0.25D, 0.35D, 0.05D);
            serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, pos.x, pos.y, pos.z, 18, 0.45D, 0.35D, 0.45D, 0.08D);
        }
    }

    @Override
    public SoundEvent castingSound() {
        return NarutoSounds.CHIDORI.get();
    }
}
