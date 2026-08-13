package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.damagetypes.NarutoDamageTypes;
import com.sekwah.narutomod.util.EyeTargeting;
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
 * Lightning Release: Shock (Raiton: Jibashi) — the entry-level lightning technique.
 *
 * Every other nature had a Lv1 jutsu to train on; lightning's cheapest was False Darkness
 * at mastery 3, which made the nature impossible to level — you could not cast anything to
 * earn the XP that unlocks casting. This fills that hole: short range, small damage, brief
 * stun, cheap enough to spam while learning.
 */
public class LightningShockAbility extends Ability implements Ability.Cooldown {

    private static final float CHAKRA_COST = 12f;
    private static final double RANGE = 6.0;
    private static final float DAMAGE = 4.0f;
    private static final int STUN_TICKS = 30;

    @Override
    public ActivationType activationType() {
        return ActivationType.INSTANT;
    }

    /** Quick discharge. */
    @Override
    public int castPoseTicks() {
        return 10;
    }

    @Override
    public long defaultCombo() {
        return 222; // V V V — sits next to Chidori's own 22 in the lightning family
    }

    @Override
    public String element() {
        return "lightning";
    }

    @Override
    public int elementLevelRequired() {
        return 1;
    }

    @Override
    public float elementXpReward() {
        return 12f;
    }

    @Override
    public int getCooldown() {
        return 3 * 20;
    }

    @Override
    public SoundEvent castingSound() {
        return SoundEvents.LIGHTNING_BOLT_IMPACT;
    }

    @Override
    public boolean handleCost(Player player, INinjaData ninjaData, int chargeAmount) {
        if (ninjaData.getChakra() < CHAKRA_COST) {
            player.displayClientMessage(Component.translatable("jutsu.fail.notenoughchakra",
                    Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW)), true);
            return false;
        }
        ninjaData.useChakra(CHAKRA_COST, 15);
        return true;
    }

    @Override
    public void performServer(Player player, INinjaData ninjaData, int ticksActive) {
        LivingEntity target = EyeTargeting.raycastLiving(player, RANGE);
        DamageSource source = NarutoDamageTypes.getDamageSource(
                player.level(), NarutoDamageTypes.CHIDORI, player, player);

        if (target != null) {
            target.hurt(source, DAMAGE * ninjaData.getRankDamageMultiplier());
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, STUN_TICKS, 2, false, true));
        }

        if (player.level() instanceof ServerLevel serverLevel) {
            Vec3 from = player.getEyePosition();
            Vec3 to = target != null
                    ? target.position().add(0, target.getBbHeight() * 0.5, 0)
                    : from.add(player.getLookAngle().scale(RANGE));
            int steps = Math.max(6, (int) (from.distanceTo(to) * 4));
            for (int i = 0; i <= steps; i++) {
                Vec3 point = from.lerp(to, i / (double) steps);
                serverLevel.sendParticles(NarutoParticles.CHIDORI_CYAN,
                        point.x, point.y, point.z, 1, 0.05, 0.05, 0.05, 0.0);
            }
        }
    }
}
