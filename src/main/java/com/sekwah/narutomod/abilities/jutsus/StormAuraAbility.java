package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.damagetypes.NarutoDamageTypes;
import com.sekwah.narutomod.util.NarutoParticles;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Storm Release: Laser Circus - a cloud of guided lightning that hangs around the wielder
 * and lashes out at anything close.
 *
 * The 1.12.2 mod modelled this as a companion entity following the caster; here it is a
 * toggle on the caster themselves, which behaves the same from the outside and avoids an
 * entity whose only job is to sit on top of you. Steady chakra drain, and it strikes on a
 * cadence rather than every tick so it stays a threat rather than an instant-kill aura.
 */
public class StormAuraAbility extends Ability implements Ability.Toggled, Ability.ToggleStartCheck {

    private static final float CHAKRA_PER_TICK = 3.0f;
    private static final double REACH = 5.0;
    private static final float ZAP_DAMAGE = 4.5f;
    /** Ticks between discharges. */
    private static final int STRIKE_INTERVAL = 15;

    @Override
    public ActivationType activationType() {
        return ActivationType.TOGGLE;
    }

    @Override
    public long defaultCombo() {
        return 1332;
    }

    @Override
    public String element() {
        return "lightning";
    }

    @Override
    public int elementLevelRequired() {
        return 8;
    }

    @Override
    public String secondaryElement() {
        return "water";
    }

    @Override
    public int secondaryElementLevelRequired() {
        return 8;
    }

    @Override
    public SoundEvent castingSound() {
        return SoundEvents.LIGHTNING_BOLT_THUNDER;
    }

    @Override
    public boolean canStartToggle(Player player, INinjaData ninjaData) {
        return validateChakra(player, ninjaData);
    }

    @Override
    public boolean handleCost(Player player, INinjaData ninjaData, int chargeAmount) {
        if (!validateChakra(player, ninjaData)) {
            return false;
        }
        ninjaData.useChakra(CHAKRA_PER_TICK, 10);
        return true;
    }

    @Override
    public void performServer(Player player, INinjaData ninjaData, int ticksActive) {
        if (!(player.level() instanceof ServerLevel serverLevel)
                || player.tickCount % STRIKE_INTERVAL != 0) {
            return;
        }
        DamageSource source = NarutoDamageTypes.getDamageSource(
                player.level(), NarutoDamageTypes.CHIDORI, player, player);
        float damage = ZAP_DAMAGE * ninjaData.getRankDamageMultiplier();
        Vec3 origin = player.position().add(0, player.getBbHeight() * 0.6, 0);

        for (LivingEntity victim : serverLevel.getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(REACH), e -> e != player && e.isAlive())) {
            victim.hurt(source, damage);
            victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 1, false, true));
            NarutoParticles.spawnBolt(serverLevel, origin,
                    victim.position().add(0, victim.getBbHeight() * 0.5, 0), 4, 1.0,
                    NarutoParticles.CHIDORI_CYAN);
        }
    }

    @Override
    public void performToggleClient(Player player, INinjaData ninjaData) {
        if (player.tickCount % 2 == 0) {
            player.level().addParticle(NarutoParticles.CHIDORI_CYAN,
                    player.getX() + (player.getRandom().nextDouble() - 0.5) * 2.4,
                    player.getY() + player.getRandom().nextDouble() * player.getBbHeight() + 0.3,
                    player.getZ() + (player.getRandom().nextDouble() - 0.5) * 2.4,
                    0.0D, 0.0D, 0.0D);
        }
    }

    private boolean validateChakra(Player player, INinjaData ninjaData) {
        if (ninjaData.getChakra() < CHAKRA_PER_TICK) {
            player.displayClientMessage(Component.translatable("jutsu.fail.notenoughchakra",
                    Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW)), true);
            return false;
        }
        return true;
    }
}
