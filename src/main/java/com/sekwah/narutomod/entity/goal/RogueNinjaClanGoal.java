package com.sekwah.narutomod.entity.goal;

import com.sekwah.narutomod.entity.RogueNinjaEntity;
import com.sekwah.narutomod.util.NarutoParticles;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * The bloodline technique a clan rogue brings to a fight, on top of the elemental ninjutsu
 * every missing-nin already has.
 *
 * Each one is the thing that clan is actually feared for rather than a reskinned damage
 * spell: a Hyuga shuts your chakra down, a Nara stops you moving at all, an Akimichi
 * flattens you. That is deliberate - meeting a clan rogue should change how the fight goes,
 * not just how fast your health bar drops.
 */
public class RogueNinjaClanGoal extends Goal {

    private static final double REACH = 16.0;
    /** Counted in canUse, which the goal selector reaches every other tick - so ~7 seconds. */
    private static final int COOLDOWN_TICKS = 70;

    private final RogueNinjaEntity ninja;
    private int cooldown;

    public RogueNinjaClanGoal(RogueNinjaEntity ninja) {
        this.ninja = ninja;
        this.setFlags(EnumSet.of(Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (this.ninja.getClanId() == RogueNinjaEntity.CLAN_NONE) {
            return false;
        }
        if (this.cooldown > 0) {
            this.cooldown--;
            return false;
        }
        LivingEntity target = this.ninja.getTarget();
        return target != null && target.isAlive()
                && this.ninja.distanceTo(target) <= REACH
                && this.ninja.getSensing().hasLineOfSight(target);
    }

    @Override
    public boolean canContinueToUse() {
        return false;
    }

    @Override
    public void start() {
        LivingEntity target = this.ninja.getTarget();
        if (target == null) {
            return;
        }
        this.cooldown = COOLDOWN_TICKS;
        this.ninja.getLookControl().setLookAt(target, 30f, 30f);

        switch (this.ninja.getClanId()) {
            case RogueNinjaEntity.CLAN_HYUGA -> gentleFist(target);
            case RogueNinjaEntity.CLAN_NARA -> shadowBind(target);
            case RogueNinjaEntity.CLAN_AKIMICHI -> humanBoulder(target);
            default -> { }
        }
    }

    /**
     * Hyuga: Gentle Fist. Strikes the chakra network rather than the body - modest damage,
     * but it leaves the victim unable to fight properly for a while. Ignores armour on
     * purpose, since the whole point of the technique is that it hits what armour does not
     * cover.
     */
    private void gentleFist(LivingEntity target) {
        this.ninja.getLookControl().setLookAt(target);
        // Closes the gap first - Gentle Fist is a palm strike, not a projectile.
        Vec3 toTarget = target.position().subtract(this.ninja.position()).normalize();
        this.ninja.setDeltaMovement(toTarget.scale(1.1).add(0, 0.25, 0));
        this.ninja.hurtMarked = true;

        if (this.ninja.distanceTo(target) <= 5.0) {
            target.hurt(this.ninja.damageSources().magic(), 7f);
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 8 * 20, 1, false, true));
            target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 8 * 20, 2, false, true));
        }
        playSound(SoundEvents.PLAYER_ATTACK_STRONG, 1.2f);
        if (this.ninja.level() instanceof ServerLevel serverLevel) {
            NarutoParticles.spawnRing(serverLevel, target.position().add(0, 1.0, 0), 0.9, 18,
                    NarutoParticles.ROTATION_WHITE);
        }
    }

    /**
     * Nara: Shadow Imitation. The canon version hijacks the target's movement outright,
     * which this mod has no way to express - there is no input-interception layer here.
     * Rooting them in place is the honest approximation: same tactical effect (you cannot
     * move, and the fight happens on the Nara's terms) without pretending to puppet them.
     */
    private void shadowBind(LivingEntity target) {
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 5 * 20, 6, false, true));
        // Absurd Jump amplifier is the standard "cannot leave the ground" trick - vanilla
        // has no rooting effect, and a shadow bind you could hop out of would be nothing.
        target.addEffect(new MobEffectInstance(MobEffects.JUMP, 5 * 20, 128, false, false));
        target.setDeltaMovement(Vec3.ZERO);
        target.hurtMarked = true;
        playSound(SoundEvents.SCULK_BLOCK_CHARGE, 0.8f);

        if (this.ninja.level() instanceof ServerLevel serverLevel) {
            // A line of shadow running along the ground from the Nara to their victim.
            Vec3 from = this.ninja.position();
            Vec3 to = target.position();
            int steps = (int) Math.max(6, from.distanceTo(to) * 3);
            for (int i = 0; i <= steps; i++) {
                Vec3 point = from.lerp(to, i / (double) steps);
                serverLevel.sendParticles(NarutoParticles.SHADOW_PURPLE,
                        point.x, point.y + 0.1, point.z, 2, 0.08, 0.02, 0.08, 0.0);
            }
        }
    }

    /** Akimichi: Human Boulder. Rolls straight through you and everything behind you. */
    private void humanBoulder(LivingEntity target) {
        Vec3 charge = target.position().subtract(this.ninja.position()).normalize();
        this.ninja.setDeltaMovement(charge.scale(1.8).add(0, 0.3, 0));
        this.ninja.hurtMarked = true;

        for (LivingEntity caught : this.ninja.level().getEntitiesOfClass(LivingEntity.class,
                this.ninja.getBoundingBox().inflate(3.0),
                e -> e != this.ninja && e.isAlive() && !(e instanceof RogueNinjaEntity))) {
            caught.hurt(this.ninja.damageSources().mobAttack(this.ninja), 11f);
            caught.knockback(1.6, -charge.x, -charge.z);
        }
        playSound(SoundEvents.STONE_BREAK, 0.6f);

        if (this.ninja.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.CLOUD,
                    this.ninja.getX(), this.ninja.getY() + 0.4, this.ninja.getZ(), 25, 0.6, 0.3, 0.6, 0.05);
        }
    }

    private void playSound(net.minecraft.sounds.SoundEvent sound, float pitch) {
        this.ninja.level().playSound(null, this.ninja.blockPosition(), sound, SoundSource.HOSTILE, 1.2f, pitch);
    }
}
