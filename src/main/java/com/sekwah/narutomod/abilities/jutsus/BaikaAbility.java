package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;

/**
 * Akimichi Clan — Multi-Size Technique / Baika no Jutsu (combo 213, TOGGLE).
 * The Akimichi convert calories into chakra and inflate their body to giant size:
 * while active the player is rendered enlarged (see RenderEvents), hits like a truck
 * (Strength II), shrugs off blows (Resistance I) but lumbers (Slowness I). Burns
 * chakra AND hunger continuously — calories are literally the fuel.
 */
public class BaikaAbility extends Ability implements Ability.Toggled, Ability.ToggleStartCheck {

    private static final float CHAKRA_PER_TICK = 1.5f;

    @Override
    public ActivationType activationType() {
        return ActivationType.TOGGLE;
    }

    @Override
    public long defaultCombo() {
        return 213;
    }

    @Override
    public boolean canStartToggle(Player player, INinjaData ninjaData) {
        return validateAccess(player, ninjaData);
    }

    @Override
    public boolean handleCost(Player player, INinjaData ninjaData, int chargeAmount) {
        if (!validateAccess(player, ninjaData)) {
            return false;
        }
        ninjaData.useChakra(CHAKRA_PER_TICK, 10);
        return true;
    }

    @Override
    public void performServer(Player player, INinjaData ninjaData, int ticksActive) {
        if (player.tickCount % 20 == 0) {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 30, 1, false, false));
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 30, 0, false, false));
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 0, false, false));
            // Calories are the fuel — giant form makes you HUNGRY
            player.causeFoodExhaustion(1.5f);
        }
    }

    @Override
    public void performToggleClient(Player player, INinjaData ninjaData) {
        if (player.tickCount % 6 == 0) {
            player.level().addParticle(ParticleTypes.POOF,
                    player.getX() + (player.getRandom().nextDouble() - 0.5) * 1.5,
                    player.getY() + player.getRandom().nextDouble() * 2.5,
                    player.getZ() + (player.getRandom().nextDouble() - 0.5) * 1.5,
                    0, 0.02, 0);
        }
    }

    @Override
    public SoundEvent castingSound() {
        return SoundEvents.PUFFER_FISH_BLOW_UP;
    }

    private boolean validateAccess(Player player, INinjaData ninjaData) {
        if (!"akimichi".equals(ninjaData.getClanId())) {
            player.displayClientMessage(Component.translatable("jutsu.fail.akimichi",
                    Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW)), true);
            return false;
        }
        if (ninjaData.getChakra() < CHAKRA_PER_TICK) {
            player.displayClientMessage(Component.translatable("jutsu.fail.notenoughchakra",
                    Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW)), true);
            return false;
        }
        return true;
    }
}
