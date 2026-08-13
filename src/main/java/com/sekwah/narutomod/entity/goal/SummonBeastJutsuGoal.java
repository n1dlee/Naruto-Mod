package com.sekwah.narutomod.entity.goal;

import com.sekwah.narutomod.entity.SummonBeastEntity;
import com.sekwah.narutomod.entity.SummonBeastVariant;
import com.sekwah.narutomod.entity.jutsuprojectile.WaterBulletJutsuEntity;
import com.sekwah.narutomod.util.NarutoParticles;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.EnumSet;
import java.util.List;

/**
 * What makes a contract beast worth a hundred chakra: its own technique, not a bigger bite.
 *
 * Built to the same shape as {@link BossJutsuGoal}, including the lesson that cost the boss
 * fights their whole kit - every rotation is written as two complete menus split on
 * {@link #CLOSE_RANGE}, never as a chain of "distance >" guards. The summons run
 * MeleeAttackGoal too, so they close and stay closed; a guarded branch at long range would
 * simply never be reached.
 *
 * Katsuyu is the exception to all of it. She is a medical summon, so her "technique" is
 * healing the ninja who called her, and her goal has to be allowed to run with no target at
 * all - otherwise she would only ever mend someone who was already winning.
 */
public class SummonBeastJutsuGoal extends Goal {

    private static final double MAX_RANGE = 28.0;
    private static final double CLOSE_RANGE = 7.0;

    private static final DustParticleOptions VENOM_GREEN =
            new DustParticleOptions(new Vector3f(0.35F, 0.75F, 0.20F), 1.3F);
    private static final DustParticleOptions ACID_WHITE =
            new DustParticleOptions(new Vector3f(0.85F, 0.9F, 0.75F), 1.2F);
    private static final DustParticleOptions HEAL_GREEN =
            new DustParticleOptions(new Vector3f(0.4F, 1.0F, 0.5F), 1.1F);

    /** Katsuyu will not spend a division on someone who has barely been scratched. */
    private static final float KATSUYU_HEAL_THRESHOLD = 0.85f;
    private static final float KATSUYU_HEAL_FRACTION = 0.18f;
    private static final double KATSUYU_HEAL_RANGE = 16.0;

    private static final double ENMA_STAFF_REACH = 14.0;

    private final SummonBeastEntity beast;
    private int cooldown;

    public SummonBeastJutsuGoal(SummonBeastEntity beast) {
        this.beast = beast;
        this.setFlags(EnumSet.of(Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (this.cooldown > 0) {
            this.cooldown--;
            return false;
        }
        // Katsuyu answers a wound, not an enemy. Checked before the target gate on purpose.
        if (this.beast.getVariant() == SummonBeastVariant.KATSUYU) {
            return this.hasSomeoneToMend();
        }
        LivingEntity target = this.beast.getTarget();
        if (target == null || !target.isAlive()) {
            return false;
        }
        double distance = this.beast.distanceTo(target);
        if (distance > MAX_RANGE) {
            return false;
        }
        return distance < 6.0 || this.beast.getSensing().hasLineOfSight(target);
    }

    @Override
    public boolean canContinueToUse() {
        return false; // every technique here resolves in the frame it starts
    }

    @Override
    public void start() {
        LivingEntity target = this.beast.getTarget();
        if (target != null) {
            this.beast.getLookControl().setLookAt(target, 30f, 30f);
        }
        double distance = target == null ? Double.MAX_VALUE : this.beast.distanceTo(target);
        this.cooldown = switch (this.beast.getVariant()) {
            case GAMABUNTA -> castGamabunta(target, distance);
            case MANDA -> castManda(target, distance);
            case KATSUYU -> castKatsuyu();
            case ENMA -> castEnma(target, distance);
        };
    }

    // ------------------------------------------------------------------ contracts

    /** Gamabunta: water bullets at range, the tanto and his own weight up close. */
    private int castGamabunta(LivingEntity target, double distance) {
        if (target == null) {
            return 40;
        }
        boolean coinFlip = this.beast.getRandom().nextBoolean();
        if (distance > CLOSE_RANGE) {
            if (coinFlip) {
                waterBulletVolley(target, 3);
                return 60;
            }
            groundSlam(6.0, 8f, 1.1);
            return 50;
        }
        if (coinFlip) {
            tantoSlash(target);
            return 45;
        }
        groundSlam(7.0, 12f, 1.4);
        return 55;
    }

    /** Manda: venom at range, a constricting bite in close. Both poison. */
    private int castManda(LivingEntity target, double distance) {
        if (target == null) {
            return 40;
        }
        boolean coinFlip = this.beast.getRandom().nextBoolean();
        if (distance > CLOSE_RANGE) {
            venomSpit(target);
            return coinFlip ? 45 : 55;
        }
        if (coinFlip) {
            constrictingBite(target);
            return 50;
        }
        venomSpit(target);
        return 40;
    }

    /** Katsuyu: divide, cover the wounded, mend them. */
    private int castKatsuyu() {
        Player owner = this.beast.getOwner();
        Vec3 centre = owner != null ? owner.position() : this.beast.position();
        List<LivingEntity> mended = this.beast.level().getEntitiesOfClass(LivingEntity.class,
                new AABB(centre, centre).inflate(KATSUYU_HEAL_RANGE), this::isFriendly);

        for (LivingEntity ally : mended) {
            if (ally.getHealth() >= ally.getMaxHealth()) {
                continue;
            }
            ally.heal(ally.getMaxHealth() * KATSUYU_HEAL_FRACTION);
            ally.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 0, false, true));
            if (this.beast.level() instanceof ServerLevel serverLevel) {
                NarutoParticles.spawnRing(serverLevel, ally.position().add(0, 0.2, 0), 0.8, 14, HEAL_GREEN);
                serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                        ally.getX(), ally.getY() + ally.getBbHeight() * 0.6, ally.getZ(),
                        12, 0.5, 0.6, 0.5, 0.02);
            }
        }
        this.beast.level().playSound(null, this.beast.blockPosition(),
                SoundEvents.SLIME_SQUISH, SoundSource.NEUTRAL, 0.9f, 0.7f);

        // She still spits acid at whatever is standing over the person she just healed.
        LivingEntity target = this.beast.getTarget();
        if (target != null && this.beast.distanceTo(target) <= MAX_RANGE) {
            acidSpray(target);
        }
        return 120;
    }

    /** Enma: the Adamantine Staff, which is the whole reason he outranges his size. */
    private int castEnma(LivingEntity target, double distance) {
        if (target == null) {
            return 40;
        }
        if (distance > CLOSE_RANGE) {
            staffExtension(target);
            return 50;
        }
        if (this.beast.getRandom().nextBoolean()) {
            staffSpin();
            return 45;
        }
        staffExtension(target);
        return 55;
    }

    // ------------------------------------------------------------------ techniques

    /** Suiton: Teppodama - a burst of compressed water bullets. */
    private void waterBulletVolley(LivingEntity target, int shots) {
        Vec3 mouth = this.beast.position().add(0, this.beast.getBbHeight() * 0.8, 0);
        for (int i = 0; i < shots; i++) {
            Vec3 aim = target.position().add(0, target.getBbHeight() * 0.5, 0).subtract(mouth);
            double spread = (i - (shots - 1) / 2.0) * 0.12;
            WaterBulletJutsuEntity bullet = new WaterBulletJutsuEntity(this.beast,
                    aim.x + spread * aim.z, aim.y, aim.z - spread * aim.x);
            bullet.setPos(mouth.x, mouth.y, mouth.z);
            bullet.setDamageMultiplier(1.4f);
            this.beast.level().addFreshEntity(bullet);
        }
        this.beast.level().playSound(null, this.beast.blockPosition(),
                SoundEvents.GENERIC_SPLASH, SoundSource.HOSTILE, 1.2f, 0.6f);
    }

    /** Dropping the full weight of a mountain toad. Everything nearby is thrown clear. */
    private void groundSlam(double radius, float damage, double lift) {
        Vec3 centre = this.beast.position();
        for (LivingEntity victim : nearbyEnemies(centre, radius)) {
            victim.hurt(this.beast.damageSources().mobAttack(this.beast), damage);
            Vec3 push = victim.position().subtract(centre).normalize().scale(0.8);
            victim.setDeltaMovement(push.x, lift * 0.5, push.z);
            victim.hurtMarked = true;
        }
        if (this.beast.level() instanceof ServerLevel serverLevel) {
            NarutoParticles.spawnRing(serverLevel, centre, radius, 40, NarutoParticles.LOG_BROWN);
            serverLevel.sendParticles(ParticleTypes.EXPLOSION, centre.x, centre.y, centre.z,
                    4, radius * 0.4, 0.2, radius * 0.4, 0.0);
        }
        this.beast.level().playSound(null, this.beast.blockPosition(),
                SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 0.9f, 0.5f);
    }

    /** The tanto Gamabunta carries at his hip, which the imported model already has. */
    private void tantoSlash(LivingEntity target) {
        target.hurt(this.beast.damageSources().mobAttack(this.beast),
                (float) this.beast.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE) * 1.6f);
        if (this.beast.level() instanceof ServerLevel serverLevel) {
            NarutoParticles.spawnBurst(serverLevel,
                    target.position().add(0, target.getBbHeight() * 0.6, 0), 26, 1.0,
                    NarutoParticles.METAL_GRAY);
        }
        this.beast.level().playSound(null, this.beast.blockPosition(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 1.3f, 0.5f);
    }

    /** A gout of venom over the target's footing. */
    private void venomSpit(LivingEntity target) {
        Vec3 impact = target.position();
        for (LivingEntity victim : nearbyEnemies(impact, 3.5)) {
            victim.hurt(this.beast.damageSources().mobAttack(this.beast), 7f);
            victim.addEffect(new MobEffectInstance(MobEffects.POISON, 140, 1));
        }
        if (this.beast.level() instanceof ServerLevel serverLevel) {
            NarutoParticles.spawnRing(serverLevel, impact.add(0, 0.2, 0), 3.0, 34, VENOM_GREEN);
            NarutoParticles.spawnBurst(serverLevel, impact.add(0, 0.8, 0), 30, 1.6, VENOM_GREEN);
        }
        this.beast.level().playSound(null, this.beast.blockPosition(),
                SoundEvents.SPIDER_HURT, SoundSource.HOSTILE, 1.1f, 0.5f);
    }

    /** Coils, then bites. Slow and heavy rather than fast and repeated. */
    private void constrictingBite(LivingEntity target) {
        target.hurt(this.beast.damageSources().mobAttack(this.beast),
                (float) this.beast.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE) * 1.5f);
        target.addEffect(new MobEffectInstance(MobEffects.POISON, 200, 2));
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2));
        if (this.beast.level() instanceof ServerLevel serverLevel) {
            NarutoParticles.spawnBurst(serverLevel,
                    target.position().add(0, target.getBbHeight() * 0.5, 0), 30, 1.1, VENOM_GREEN);
        }
        this.beast.level().playSound(null, this.beast.blockPosition(),
                SoundEvents.RAVAGER_ATTACK, SoundSource.HOSTILE, 1.2f, 0.6f);
    }

    /** Katsuyu's acid. Corrosive rather than concussive, so it does not throw anyone. */
    private void acidSpray(LivingEntity target) {
        Vec3 from = this.beast.position().add(0, this.beast.getBbHeight() * 0.7, 0);
        Vec3 to = target.position().add(0, target.getBbHeight() * 0.5, 0);
        for (LivingEntity victim : nearbyEnemies(to, 2.5)) {
            victim.hurt(this.beast.damageSources().mobAttack(this.beast), 6f);
            victim.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 0));
        }
        if (this.beast.level() instanceof ServerLevel serverLevel) {
            NarutoParticles.spawnBolt(serverLevel, from, to, 3, 0.35, ACID_WHITE);
        }
        this.beast.level().playSound(null, this.beast.blockPosition(),
                SoundEvents.SLIME_ATTACK, SoundSource.NEUTRAL, 1.0f, 0.8f);
    }

    /**
     * Kongonyoi - the staff shoots out in a straight line and everything standing in it is
     * driven back along that line. This is what a summon Enma's size is for.
     */
    private void staffExtension(LivingEntity target) {
        Vec3 origin = this.beast.position().add(0, this.beast.getBbHeight() * 0.6, 0);
        Vec3 direction = target.position().add(0, target.getBbHeight() * 0.5, 0)
                .subtract(origin).normalize();
        Vec3 tip = origin.add(direction.scale(ENMA_STAFF_REACH));

        float damage = (float) this.beast.getAttributeValue(
                net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE) * 1.3f;
        // Sample along the shaft rather than one box at the tip, so standing halfway down it
        // is not a safe place to be.
        for (double step = 2.0; step <= ENMA_STAFF_REACH; step += 2.0) {
            Vec3 point = origin.add(direction.scale(step));
            for (LivingEntity victim : nearbyEnemies(point, 1.6)) {
                if (victim.hurt(this.beast.damageSources().mobAttack(this.beast), damage)) {
                    victim.setDeltaMovement(direction.x * 1.1, 0.35, direction.z * 1.1);
                    victim.hurtMarked = true;
                }
            }
        }
        if (this.beast.level() instanceof ServerLevel serverLevel) {
            NarutoParticles.spawnBolt(serverLevel, origin, tip, 2, 0.12, NarutoParticles.METAL_GRAY);
        }
        this.beast.level().playSound(null, this.beast.blockPosition(),
                SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 0.8f, 1.4f);
    }

    /** The staff swept in a full circle. Short, but it clears everyone off him at once. */
    private void staffSpin() {
        Vec3 centre = this.beast.position();
        float damage = (float) this.beast.getAttributeValue(
                net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
        for (LivingEntity victim : nearbyEnemies(centre, 5.0)) {
            if (victim.hurt(this.beast.damageSources().mobAttack(this.beast), damage)) {
                Vec3 push = victim.position().subtract(centre).normalize().scale(1.0);
                victim.setDeltaMovement(push.x, 0.4, push.z);
                victim.hurtMarked = true;
            }
        }
        if (this.beast.level() instanceof ServerLevel serverLevel) {
            NarutoParticles.spawnRing(serverLevel, centre.add(0, 1.0, 0), 4.5, 36,
                    NarutoParticles.METAL_GRAY);
        }
        this.beast.level().playSound(null, this.beast.blockPosition(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 1.2f, 0.7f);
    }

    // ------------------------------------------------------------------ helpers

    /** Everything in range that this summon is willing to hit. */
    private List<LivingEntity> nearbyEnemies(Vec3 centre, double radius) {
        return this.beast.level().getEntitiesOfClass(LivingEntity.class,
                new AABB(centre, centre).inflate(radius),
                candidate -> candidate != this.beast && candidate.isAlive() && !isFriendly(candidate));
    }

    /** The summoner and anything else they called out. Never a valid target. */
    private boolean isFriendly(LivingEntity candidate) {
        java.util.UUID owner = this.beast.getOwnerUUID().orElse(null);
        if (owner == null) {
            return candidate == this.beast;
        }
        if (owner.equals(candidate.getUUID())) {
            return true;
        }
        return candidate instanceof SummonBeastEntity other
                && owner.equals(other.getOwnerUUID().orElse(null));
    }

    private boolean hasSomeoneToMend() {
        Player owner = this.beast.getOwner();
        Vec3 centre = owner != null ? owner.position() : this.beast.position();
        return this.beast.level().getEntitiesOfClass(LivingEntity.class,
                new AABB(centre, centre).inflate(KATSUYU_HEAL_RANGE),
                ally -> isFriendly(ally) && ally.getHealth() < ally.getMaxHealth() * KATSUYU_HEAL_THRESHOLD)
                .stream().findAny().isPresent();
    }
}
