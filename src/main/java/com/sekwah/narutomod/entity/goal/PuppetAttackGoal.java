package com.sekwah.narutomod.entity.goal;

import com.sekwah.narutomod.entity.PuppetEntity;
import com.sekwah.narutomod.entity.projectile.SenbonEntity;
import com.sekwah.narutomod.util.NarutoParticles;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.EnumSet;
import java.util.List;

/**
 * What each puppet in the collection actually does.
 *
 * Two complete menus split on {@link #CLOSE_RANGE}, never a chain of distance guards - these
 * run MeleeAttackGoal as well, so they close and stay closed, and anything written behind a
 * far-range guard would never fire. Same lesson as the bosses and the tailed beasts.
 *
 * Everything except Sanshouo is poisoned, because everything in Sasori's collection is.
 */
public class PuppetAttackGoal extends Goal {

    private static final double MAX_RANGE = 26.0;
    private static final double CLOSE_RANGE = 6.0;

    private static final DustParticleOptions POISON_GREEN =
            new DustParticleOptions(new Vector3f(0.35F, 0.75F, 0.20F), 1.1F);
    private static final DustParticleOptions IRON_SAND =
            new DustParticleOptions(new Vector3f(0.18F, 0.18F, 0.20F), 1.2F);

    private final PuppetEntity puppet;
    private int cooldown = 20;

    public PuppetAttackGoal(PuppetEntity puppet) {
        this.puppet = puppet;
        this.setFlags(EnumSet.of(Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (this.cooldown > 0) {
            this.cooldown--;
            return false;
        }
        LivingEntity target = this.puppet.getTarget();
        if (target == null || !target.isAlive()) {
            return false;
        }
        double distance = this.puppet.distanceTo(target);
        if (distance > MAX_RANGE) {
            return false;
        }
        return distance < CLOSE_RANGE || this.puppet.getSensing().hasLineOfSight(target);
    }

    @Override
    public boolean canContinueToUse() {
        return false;
    }

    @Override
    public void start() {
        LivingEntity target = this.puppet.getTarget();
        if (target == null) {
            return;
        }
        this.puppet.getLookControl().setLookAt(target, 30f, 30f);
        double distance = this.puppet.distanceTo(target);

        this.cooldown = switch (this.puppet.getVariant()) {
            case HIRUKO -> castHiruko(target, distance);
            case KARASU -> castKarasu(target, distance);
            case SANSHOUO -> castSanshouo(target, distance);
            case THIRD_KAZEKAGE -> castThirdKazekage(target, distance);
            case HUNDRED -> castHundred(target, distance);
        };
    }

    // ------------------------------------------------------------------ per puppet

    /** Hiruko: the tail. Whether it reaches you is the only question in the fight. */
    private int castHiruko(LivingEntity target, double distance) {
        if (distance > CLOSE_RANGE) {
            stingerLunge(target);
            return 45;
        }
        if (this.puppet.getRandom().nextBoolean()) {
            tailSweep();
            return 50;
        }
        stingerLunge(target);
        return 45;
    }

    /** Karasu: senbon at range, and blades from the arms when it cannot get away. */
    private int castKarasu(LivingEntity target, double distance) {
        if (distance > CLOSE_RANGE) {
            senbonVolley(target, 5);
            return 40;
        }
        if (this.puppet.getRandom().nextBoolean()) {
            bladeStrike(target);
            return 30;
        }
        senbonVolley(target, 3);
        return 35;
    }

    /** Sanshouo: no weapon. It puts its shell between an attacker and everything behind it. */
    private int castSanshouo(LivingEntity target, double distance) {
        guardAllies();
        if (distance <= CLOSE_RANGE) {
            shellSlam();
            return 60;
        }
        return 40;
    }

    /** The Third Kazekage: Iron Sand. Heavier than anything else in the collection. */
    private int castThirdKazekage(LivingEntity target, double distance) {
        if (distance > CLOSE_RANGE) {
            ironSandVolley(target);
            return 55;
        }
        if (this.puppet.getRandom().nextBoolean()) {
            ironSandSpikes();
            return 60;
        }
        ironSandVolley(target);
        return 55;
    }

    /** One of the Hundred: a blade, and not much else. The threat is the count. */
    private int castHundred(LivingEntity target, double distance) {
        if (distance > CLOSE_RANGE) {
            senbonVolley(target, 2);
            return 45;
        }
        bladeStrike(target);
        return 30;
    }

    // ------------------------------------------------------------------ techniques

    /** The scorpion tail, driven forward. Long reach, and it leaves poison behind. */
    private void stingerLunge(LivingEntity target) {
        Vec3 origin = this.puppet.position().add(0, this.puppet.getBbHeight() * 0.6, 0);
        Vec3 direction = target.position().add(0, target.getBbHeight() * 0.5, 0)
                .subtract(origin).normalize();
        for (double step = 1.5; step <= 9.0; step += 1.5) {
            Vec3 point = origin.add(direction.scale(step));
            for (LivingEntity victim : enemiesNear(point, 1.4)) {
                if (victim.hurt(this.puppet.damageSources().mobAttack(this.puppet), this.attack() * 1.2f)) {
                    poison(victim, 160, 1);
                }
            }
        }
        if (this.puppet.level() instanceof ServerLevel serverLevel) {
            NarutoParticles.spawnBolt(serverLevel, origin, origin.add(direction.scale(9.0)),
                    2, 0.2, POISON_GREEN);
        }
        this.puppet.level().playSound(null, this.puppet.blockPosition(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 1.0f, 1.3f);
    }

    /** The tail swept in a circle: everything around Hiruko, and all of it poisoned. */
    private void tailSweep() {
        Vec3 centre = this.puppet.position();
        for (LivingEntity victim : enemiesNear(centre, 5.5)) {
            if (victim.hurt(this.puppet.damageSources().mobAttack(this.puppet), this.attack())) {
                poison(victim, 120, 0);
                Vec3 push = victim.position().subtract(centre).normalize().scale(0.7);
                victim.setDeltaMovement(push.x, 0.35, push.z);
                victim.hurtMarked = true;
            }
        }
        if (this.puppet.level() instanceof ServerLevel serverLevel) {
            NarutoParticles.spawnRing(serverLevel, centre.add(0, 0.6, 0), 5.0, 30, POISON_GREEN);
        }
        this.puppet.level().playSound(null, this.puppet.blockPosition(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 1.2f, 0.8f);
    }

    /** Karasu firing off its own limbs. Reuses the real senbon so the poison behaves. */
    private void senbonVolley(LivingEntity target, int count) {
        Vec3 origin = this.puppet.position().add(0, this.puppet.getBbHeight() * 0.7, 0);
        Vec3 aim = target.position().add(0, target.getBbHeight() * 0.5, 0).subtract(origin);
        for (int i = 0; i < count; i++) {
            SenbonEntity senbon = new SenbonEntity(this.puppet.level(), this.puppet);
            senbon.setPos(origin.x, origin.y, origin.z);
            double spread = (i - (count - 1) / 2.0) * 0.09;
            senbon.shoot(aim.x + spread * aim.z, aim.y + spread * 0.5, aim.z - spread * aim.x, 1.6f, 2.0f);
            this.puppet.level().addFreshEntity(senbon);
        }
        this.puppet.level().playSound(null, this.puppet.blockPosition(),
                SoundEvents.ARROW_SHOOT, SoundSource.HOSTILE, 0.9f, 1.5f);
    }

    /** A hidden blade. Short, quick, poisoned. */
    private void bladeStrike(LivingEntity target) {
        if (target.hurt(this.puppet.damageSources().mobAttack(this.puppet), this.attack() * 1.3f)) {
            poison(target, 140, 0);
        }
        if (this.puppet.level() instanceof ServerLevel serverLevel) {
            NarutoParticles.spawnBurst(serverLevel,
                    target.position().add(0, target.getBbHeight() * 0.6, 0), 18, 0.8,
                    NarutoParticles.METAL_GRAY);
        }
        this.puppet.level().playSound(null, this.puppet.blockPosition(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 0.9f, 1.4f);
    }

    /**
     * Sanshouo's whole purpose: it stands over whatever is behind it and takes the hit.
     *
     * Implemented as Resistance on the puppeteer's own side rather than by intercepting
     * damage, so it reads on the health bar and cannot double up with itself.
     */
    private void guardAllies() {
        Vec3 centre = this.puppet.position();
        List<LivingEntity> guarded = this.puppet.level().getEntitiesOfClass(LivingEntity.class,
                new AABB(centre, centre).inflate(7.0), this::isFriendly);
        for (LivingEntity ally : guarded) {
            ally.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, 1, false, false));
        }
        if (this.puppet.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.CRIT,
                    centre.x, centre.y + this.puppet.getBbHeight() * 0.5, centre.z,
                    10, 1.2, 0.8, 1.2, 0.02);
        }
    }

    /** The shell dropped on whatever is standing in front of it. */
    private void shellSlam() {
        Vec3 centre = this.puppet.position();
        for (LivingEntity victim : enemiesNear(centre, 4.5)) {
            if (victim.hurt(this.puppet.damageSources().mobAttack(this.puppet), this.attack() * 1.4f)) {
                Vec3 push = victim.position().subtract(centre).normalize().scale(0.9);
                victim.setDeltaMovement(push.x, 0.45, push.z);
                victim.hurtMarked = true;
            }
        }
        if (this.puppet.level() instanceof ServerLevel serverLevel) {
            NarutoParticles.spawnRing(serverLevel, centre, 4.5, 26, NarutoParticles.LOG_BROWN);
        }
        this.puppet.level().playSound(null, this.puppet.blockPosition(),
                SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 0.8f, 0.6f);
    }

    /** Iron Sand fired as a spread of hardened shot. */
    private void ironSandVolley(LivingEntity target) {
        Vec3 impact = target.position();
        for (LivingEntity victim : enemiesNear(impact, 3.0)) {
            victim.hurt(this.puppet.damageSources().mobAttack(this.puppet), this.attack() * 1.1f);
            victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 1));
        }
        if (this.puppet.level() instanceof ServerLevel serverLevel) {
            NarutoParticles.spawnBolt(serverLevel,
                    this.puppet.position().add(0, this.puppet.getBbHeight() * 0.7, 0),
                    impact.add(0, 1.0, 0), 3, 0.4, IRON_SAND);
            NarutoParticles.spawnBurst(serverLevel, impact.add(0, 0.8, 0), 40, 2.0, IRON_SAND);
        }
        this.puppet.level().playSound(null, this.puppet.blockPosition(),
                SoundEvents.SAND_BREAK, SoundSource.HOSTILE, 1.4f, 0.5f);
    }

    /** Iron Sand World Method, in miniature: spikes driven up around the puppet. */
    private void ironSandSpikes() {
        Vec3 centre = this.puppet.position();
        for (LivingEntity victim : enemiesNear(centre, 6.0)) {
            if (victim.hurt(this.puppet.damageSources().mobAttack(this.puppet), this.attack() * 1.5f)) {
                victim.setDeltaMovement(victim.getDeltaMovement().add(0, 0.5, 0));
                victim.hurtMarked = true;
            }
        }
        if (this.puppet.level() instanceof ServerLevel serverLevel) {
            NarutoParticles.spawnRing(serverLevel, centre, 5.5, 34, IRON_SAND);
            NarutoParticles.spawnBurst(serverLevel, centre.add(0, 1.0, 0), 50, 2.5, IRON_SAND);
        }
        this.puppet.level().playSound(null, this.puppet.blockPosition(),
                SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 0.9f, 0.8f);
    }

    // ------------------------------------------------------------------ helpers

    private void poison(LivingEntity victim, int ticks, int amplifier) {
        if (this.puppet.getVariant().isPoisoned()) {
            victim.addEffect(new MobEffectInstance(MobEffects.POISON, ticks, amplifier));
        }
    }

    private float attack() {
        return (float) this.puppet.getAttributeValue(Attributes.ATTACK_DAMAGE);
    }

    private List<LivingEntity> enemiesNear(Vec3 centre, double radius) {
        return this.puppet.level().getEntitiesOfClass(LivingEntity.class,
                new AABB(centre, centre).inflate(radius),
                candidate -> candidate != this.puppet && candidate.isAlive() && !isFriendly(candidate));
    }

    /** The puppeteer and the rest of their collection. */
    private boolean isFriendly(LivingEntity candidate) {
        java.util.UUID owner = this.puppet.getOwnerUUID().orElse(null);
        if (owner == null) {
            return candidate == this.puppet;
        }
        if (owner.equals(candidate.getUUID())) {
            return true;
        }
        return candidate instanceof PuppetEntity other
                && owner.equals(other.getOwnerUUID().orElse(null));
    }
}
