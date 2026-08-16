package com.sekwah.narutomod.entity.goal;

import com.sekwah.narutomod.entity.TailedBeastEntity;
import com.sekwah.narutomod.entity.TailedBeastVariant;
import com.sekwah.narutomod.entity.jutsuprojectile.TailedBeastBombEntity;
import com.sekwah.narutomod.sounds.NarutoSounds;
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
 * How a tailed beast fights.
 *
 * Same two-menu shape as {@link BossJutsuGoal}, for the same hard-won reason: these run
 * MeleeAttackGoal as well, so any rotation written as a chain of "distance >" guards would
 * collapse onto its last branch the moment the beast closed. Each species gets one complete
 * close menu and one complete ranged menu.
 *
 * On top of the species kit sits the Bijudama, which every beast has. It is the only thing
 * here that plays out over time: the beast plants itself, the sphere gathers in front of its
 * mouth for two seconds, and then it goes. That wind-up is the counterplay - it is the window
 * to break line of sight or to close and interrupt the fight on your terms.
 */
public class TailedBeastJutsuGoal extends Goal {

    private static final double MAX_RANGE = 40.0;
    private static final double CLOSE_RANGE = 10.0;

    /** Ticks the sphere gathers before it is thrown. */
    private static final int BIJUDAMA_CHARGE = 40;
    /** Rough one-in-N chance the beast opens with a Bijudama rather than its species kit. */
    private static final int BIJUDAMA_ODDS = 4;

    private static final DustParticleOptions SAND_TAN =
            new DustParticleOptions(new Vector3f(0.80F, 0.72F, 0.45F), 1.3F);
    private static final DustParticleOptions BLUE_FLAME =
            new DustParticleOptions(new Vector3f(0.25F, 0.55F, 1.0F), 1.4F);
    private static final DustParticleOptions LAVA_ORANGE =
            new DustParticleOptions(new Vector3f(1.0F, 0.35F, 0.05F), 1.4F);
    private static final DustParticleOptions STEAM_WHITE =
            new DustParticleOptions(new Vector3f(0.92F, 0.92F, 0.95F), 1.5F);
    private static final DustParticleOptions SLIME_GREEN =
            new DustParticleOptions(new Vector3f(0.70F, 0.95F, 0.50F), 1.3F);
    private static final DustParticleOptions SCALE_GOLD =
            new DustParticleOptions(new Vector3f(0.85F, 0.80F, 0.35F), 1.1F);
    private static final DustParticleOptions INK_BLACK =
            new DustParticleOptions(new Vector3f(0.10F, 0.10F, 0.16F), 1.4F);

    private final TailedBeastEntity beast;
    private int cooldown = 40;

    private boolean charging;
    private int chargeTicks;
    /** Snapshot of the beast's hurt clock, so a hit landing mid-charge is detectable. */
    private int hurtStampAtChargeStart;

    public TailedBeastJutsuGoal(TailedBeastEntity beast) {
        this.beast = beast;
        this.setFlags(EnumSet.of(Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (this.cooldown > 0) {
            this.cooldown--;
            return false;
        }
        LivingEntity target = this.beast.getTarget();
        if (target == null || !target.isAlive()) {
            return false;
        }
        double distance = this.beast.distanceTo(target);
        if (distance > MAX_RANGE) {
            return false;
        }
        return distance < CLOSE_RANGE || this.beast.getSensing().hasLineOfSight(target);
    }

    /**
     * The wind-up is the counterplay, so it has to be possible to actually take it away.
     *
     * Only checking that the target is alive meant neither advertised window existed: you
     * could break line of sight, run behind a hill, close to point blank or hit the beast in
     * the mouth and the sphere still went off. Three things end a charge now - losing sight
     * of the target, the target getting outside the beast's reach, and the beast being hurt
     * while it gathers. The last one is what makes hitting it during the wind-up mean
     * something rather than being a worse choice than backing off.
     */
    @Override
    public boolean canContinueToUse() {
        if (!this.charging) {
            return false;
        }
        LivingEntity target = this.beast.getTarget();
        if (target == null || !target.isAlive()) {
            return false;
        }
        if (this.beast.getHurtCount() != this.hurtStampAtChargeStart) {
            interrupt("hit");
            return false;
        }
        if (this.beast.distanceTo(target) > MAX_RANGE) {
            interrupt("range");
            return false;
        }
        if (!this.beast.getSensing().hasLineOfSight(target)) {
            interrupt("sight");
            return false;
        }
        return true;
    }

    /** Drops the gathered sphere. Short cooldown - the beast has spent nothing but time. */
    private void interrupt(String reason) {
        this.charging = false;
        this.chargeTicks = 0;
        this.cooldown = 40;
        if (this.beast.level() instanceof ServerLevel serverLevel) {
            Vec3 mouth = this.mouthPosition();
            serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE,
                    mouth.x, mouth.y, mouth.z, 30, 0.6, 0.6, 0.6, 0.03);
        }
        this.beast.level().playSound(null, this.beast.blockPosition(),
                SoundEvents.FIRE_EXTINGUISH, SoundSource.HOSTILE, 2.0f, 0.6f);
    }

    @Override
    public void start() {
        LivingEntity target = this.beast.getTarget();
        if (target == null) {
            return;
        }
        this.beast.getLookControl().setLookAt(target, 30f, 30f);
        this.charging = false;
        this.chargeTicks = 0;

        double distance = this.beast.distanceTo(target);
        // A wounded beast reaches for the Bijudama more often, and throws a bigger one.
        //
        // Rolled at every range on purpose. Gating it on "distance > CLOSE_RANGE" is the exact
        // mistake that made half the boss kits unreachable: these beasts run MeleeAttackGoal
        // too, so they close and stay closed, and a technique behind a far-range guard is a
        // technique that never happens. Point blank is fine here - the two-second wind-up is
        // the counterplay, not the distance.
        int odds = Math.max(2, BIJUDAMA_ODDS - this.beast.getRage());
        if (this.beast.getRandom().nextInt(odds) == 0) {
            this.beginBijudama();
            return;
        }
        this.cooldown = switch (this.beast.getVariant()) {
            case SHUKAKU -> castShukaku(target, distance);
            case MATATABI -> castMatatabi(target, distance);
            case ISOBU -> castIsobu(target, distance);
            case SON_GOKU -> castSonGoku(target, distance);
            case KOKUO -> castKokuo(target, distance);
            case SAIKEN -> castSaiken(target, distance);
            case CHOMEI -> castChomei(target, distance);
            case GYUKI -> castGyuki(target, distance);
        };
    }

    @Override
    public void tick() {
        if (!this.charging) {
            return;
        }
        LivingEntity target = this.beast.getTarget();
        if (target == null) {
            this.charging = false;
            return;
        }
        this.beast.getLookControl().setLookAt(target, 30f, 30f);
        // Planted while charging: a beast that walked through its own wind-up would make the
        // tell unreadable and the dodge unlearnable.
        this.beast.getNavigation().stop();

        this.chargeTicks++;
        // Drives the rear-back pose in TailedBeastRenderer. The charge is the tell.
        this.beast.setBijudamaCharge((float) this.chargeTicks / BIJUDAMA_CHARGE);
        if (this.beast.level() instanceof ServerLevel serverLevel) {
            Vec3 mouth = this.mouthPosition();
            float progress = (float) this.chargeTicks / BIJUDAMA_CHARGE;
            DustParticleOptions dust =
                    new DustParticleOptions(this.beast.getVariant().getChakraColour(), 2.5F);
            NarutoParticles.spawnRing(serverLevel, mouth,
                    0.5 + 2.5 * (1.0 - progress), 18, dust);
            serverLevel.sendParticles(ParticleTypes.SMOKE,
                    mouth.x, mouth.y, mouth.z, 4, 0.4, 0.4, 0.4, 0.01);
        }

        if (this.chargeTicks >= BIJUDAMA_CHARGE) {
            this.fireBijudama(target);
            this.charging = false;
            this.beast.setBijudamaCharge(0f);
        }
    }

    @Override
    public void stop() {
        this.charging = false;
        // Cleared here too: a charge interrupted by losing the target would otherwise leave
        // the beast frozen mid-rear for as long as it lived.
        this.beast.setBijudamaCharge(0f);
    }

    // ------------------------------------------------------------------ Bijudama

    private void beginBijudama() {
        this.charging = true;
        this.chargeTicks = 0;
        this.beast.setBijudamaCharge(0f);
        this.hurtStampAtChargeStart = this.beast.getHurtCount();
        this.beast.playSound(this.beast.getVariant().getRoar(), 5.0f, 0.8f);
    }

    private void fireBijudama(LivingEntity target) {
        TailedBeastVariant variant = this.beast.getVariant();
        // Bigger beasts throw bigger bombs, and a cornered one throws bigger still.
        float power = 4.0f + variant.getTails() * 0.45f + this.beast.getRage() * 0.8f;

        Vec3 mouth = this.mouthPosition();
        Vec3 aim = target.position().add(0, target.getBbHeight() * 0.5, 0).subtract(mouth);
        TailedBeastBombEntity bomb = new TailedBeastBombEntity(this.beast, aim, variant, power);
        bomb.setPos(mouth.x, mouth.y, mouth.z);
        this.beast.level().addFreshEntity(bomb);

        this.beast.level().playSound(null, this.beast.blockPosition(),
                NarutoSounds.BIJUDAMA.get(), SoundSource.HOSTILE, 4.0f, 1.2f);
        this.cooldown = 150 - this.beast.getRage() * 20;
    }

    private Vec3 mouthPosition() {
        Vec3 look = this.beast.getLookAngle();
        double reach = this.beast.getBbWidth() * 0.6 + 1.5;
        return this.beast.position()
                .add(0, this.beast.getBbHeight() * 0.78, 0)
                .add(look.x * reach, 0, look.z * reach);
    }

    // ------------------------------------------------------------------ species kits

    /** Shukaku: Wind Release at range, a sandstorm slam in close. */
    private int castShukaku(LivingEntity target, double distance) {
        if (distance > CLOSE_RANGE) {
            drillingAirBullet(target);
            return 70;
        }
        if (this.beast.getRandom().nextBoolean()) {
            slam(8.0, 1.0f, SAND_TAN);
            blindNearby(8.0, 100);
            return 60;
        }
        drillingAirBullet(target);
        return 70;
    }

    /** Matatabi: blue fire down a line, claws in close. */
    private int castMatatabi(LivingEntity target, double distance) {
        if (distance > CLOSE_RANGE) {
            breathLine(target, BLUE_FLAME, 1.0f, 6);
            return 65;
        }
        if (this.beast.getRandom().nextBoolean()) {
            clawRake(target, 1.7f);
            return 40;
        }
        breathLine(target, BLUE_FLAME, 0.9f, 5);
        return 55;
    }

    /** Isobu: water pressure at range, a shell spin that clears the ring in close. */
    private int castIsobu(LivingEntity target, double distance) {
        if (distance > CLOSE_RANGE) {
            breathLine(target, NarutoParticles.WATER_BLUE, 0.9f, 0);
            return 60;
        }
        if (this.beast.getRandom().nextBoolean()) {
            slam(9.0, 1.2f, NarutoParticles.WATER_BLUE);
            return 70;
        }
        clawRake(target, 1.4f);
        return 40;
    }

    /** Son Goku: Lava Release. Everything he touches keeps burning. */
    private int castSonGoku(LivingEntity target, double distance) {
        if (distance > CLOSE_RANGE) {
            breathLine(target, LAVA_ORANGE, 1.1f, 8);
            return 70;
        }
        if (this.beast.getRandom().nextBoolean()) {
            slam(8.0, 1.1f, LAVA_ORANGE);
            igniteNearby(8.0, 6);
            return 65;
        }
        clawRake(target, 1.6f);
        return 40;
    }

    /** Kokuo: Boil Release at range, a goring charge in close. */
    private int castKokuo(LivingEntity target, double distance) {
        if (distance > CLOSE_RANGE) {
            steamCloud(target);
            return 65;
        }
        if (this.beast.getRandom().nextBoolean()) {
            gore(target);
            return 55;
        }
        steamCloud(target);
        return 65;
    }

    /** Saiken: corrosion. No knockback anywhere in the kit - it dissolves rather than hits. */
    private int castSaiken(LivingEntity target, double distance) {
        if (distance > CLOSE_RANGE) {
            corrosiveSpray(target);
            return 60;
        }
        if (this.beast.getRandom().nextBoolean()) {
            corrosiveBurst();
            return 70;
        }
        corrosiveSpray(target);
        return 60;
    }

    /** Chomei: scale dust that takes your eyes, and a horn ram when it dives. */
    private int castChomei(LivingEntity target, double distance) {
        if (distance > CLOSE_RANGE) {
            scaleDust(target);
            return 55;
        }
        if (this.beast.getRandom().nextBoolean()) {
            gore(target);
            return 50;
        }
        scaleDust(target);
        return 55;
    }

    /** Gyuki: ink at range, and eight tentacles worth of sweep in close. */
    private int castGyuki(LivingEntity target, double distance) {
        if (distance > CLOSE_RANGE) {
            inkSpray(target);
            return 60;
        }
        if (this.beast.getRandom().nextBoolean()) {
            tentacleSweep();
            return 55;
        }
        inkSpray(target);
        return 60;
    }

    // ------------------------------------------------------------------ techniques

    /** A drilling cone of wind. Hits hard and throws whatever it catches. */
    private void drillingAirBullet(LivingEntity target) {
        Vec3 origin = this.mouthPosition();
        Vec3 direction = target.position().add(0, target.getBbHeight() * 0.5, 0)
                .subtract(origin).normalize();
        for (double step = 2.0; step <= 24.0; step += 2.0) {
            Vec3 point = origin.add(direction.scale(step));
            for (LivingEntity victim : enemiesNear(point, 2.2)) {
                if (victim.hurt(this.beast.damageSources().mobAttack(this.beast), this.attack() * 0.8f)) {
                    victim.setDeltaMovement(direction.x * 1.6, 0.5, direction.z * 1.6);
                    victim.hurtMarked = true;
                }
            }
            if (this.beast.level() instanceof ServerLevel serverLevel) {
                NarutoParticles.spawnRing(serverLevel, point, 1.8, 12, SAND_TAN);
            }
        }
        this.beast.level().playSound(null, this.beast.blockPosition(),
                SoundEvents.ENDER_DRAGON_FLAP, SoundSource.HOSTILE, 2.5f, 0.6f);
    }

    /** A breath weapon: a line of damage, optionally setting fire to what it passes through. */
    private void breathLine(LivingEntity target, net.minecraft.core.particles.ParticleOptions dust,
                            float damageScale, int burnSeconds) {
        Vec3 origin = this.mouthPosition();
        Vec3 direction = target.position().add(0, target.getBbHeight() * 0.5, 0)
                .subtract(origin).normalize();
        for (double step = 1.5; step <= 22.0; step += 1.5) {
            Vec3 point = origin.add(direction.scale(step));
            for (LivingEntity victim : enemiesNear(point, 2.0)) {
                victim.hurt(this.beast.damageSources().mobAttack(this.beast), this.attack() * damageScale);
                if (burnSeconds > 0) {
                    victim.setSecondsOnFire(burnSeconds);
                }
            }
        }
        if (this.beast.level() instanceof ServerLevel serverLevel) {
            NarutoParticles.spawnBolt(serverLevel, origin,
                    origin.add(direction.scale(22.0)), 3, 0.6, dust);
        }
        this.beast.level().playSound(null, this.beast.blockPosition(),
                SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 2.5f, 0.5f);
    }

    /** Dropping the beast's own weight. Radius scales with how big it actually is. */
    private void slam(double radius, float damageScale, DustParticleOptions dust) {
        Vec3 centre = this.beast.position();
        double reach = radius + this.beast.getBbWidth() * 0.5;
        for (LivingEntity victim : enemiesNear(centre, reach)) {
            if (victim.hurt(this.beast.damageSources().mobAttack(this.beast), this.attack() * damageScale)) {
                Vec3 push = victim.position().subtract(centre).normalize().scale(1.1);
                victim.setDeltaMovement(push.x, 0.6, push.z);
                victim.hurtMarked = true;
            }
        }
        if (this.beast.level() instanceof ServerLevel serverLevel) {
            NarutoParticles.spawnRing(serverLevel, centre, reach, (int) (reach * 6), dust);
            serverLevel.sendParticles(ParticleTypes.EXPLOSION,
                    centre.x, centre.y, centre.z, 6, reach * 0.35, 0.3, reach * 0.35, 0.0);
        }
        this.beast.level().playSound(null, this.beast.blockPosition(),
                SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 3.0f, 0.4f);
    }

    private void clawRake(LivingEntity target, float damageScale) {
        target.hurt(this.beast.damageSources().mobAttack(this.beast), this.attack() * damageScale);
        if (this.beast.level() instanceof ServerLevel serverLevel) {
            NarutoParticles.spawnBurst(serverLevel,
                    target.position().add(0, target.getBbHeight() * 0.6, 0), 30, 1.2,
                    NarutoParticles.METAL_GRAY);
        }
        this.beast.level().playSound(null, this.beast.blockPosition(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 2.0f, 0.5f);
    }

    /** Boil Release: a lingering scald that saps strength rather than throwing anyone. */
    private void steamCloud(LivingEntity target) {
        Vec3 centre = target.position();
        for (LivingEntity victim : enemiesNear(centre, 5.0)) {
            victim.hurt(this.beast.damageSources().mobAttack(this.beast), this.attack() * 0.7f);
            victim.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 160, 1));
            victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 1));
        }
        if (this.beast.level() instanceof ServerLevel serverLevel) {
            NarutoParticles.spawnBurst(serverLevel, centre.add(0, 1.0, 0), 90, 3.5, STEAM_WHITE);
            serverLevel.sendParticles(ParticleTypes.CLOUD,
                    centre.x, centre.y + 1.0, centre.z, 60, 2.5, 1.5, 2.5, 0.02);
        }
        this.beast.level().playSound(null, this.beast.blockPosition(),
                SoundEvents.LAVA_EXTINGUISH, SoundSource.HOSTILE, 3.0f, 0.6f);
    }

    /** A running gore. Single target, heavy, and it moves the beast onto them. */
    private void gore(LivingEntity target) {
        Vec3 toTarget = target.position().subtract(this.beast.position());
        Vec3 flat = new Vec3(toTarget.x, 0, toTarget.z);
        if (flat.lengthSqr() > 1.0E-4) {
            Vec3 direction = flat.normalize();
            this.beast.setDeltaMovement(direction.x * 0.9, 0.25, direction.z * 0.9);
            this.beast.hurtMarked = true;
        }
        if (target.hurt(this.beast.damageSources().mobAttack(this.beast), this.attack() * 1.8f)) {
            target.setDeltaMovement(target.getDeltaMovement().add(0, 0.6, 0));
            target.hurtMarked = true;
        }
        this.beast.level().playSound(null, this.beast.blockPosition(),
                SoundEvents.RAVAGER_ATTACK, SoundSource.HOSTILE, 2.5f, 0.5f);
    }

    /** Saiken's slime. Poison and rot, no knockback - you stay in it. */
    private void corrosiveSpray(LivingEntity target) {
        Vec3 centre = target.position();
        for (LivingEntity victim : enemiesNear(centre, 4.0)) {
            victim.hurt(this.beast.damageSources().mobAttack(this.beast), this.attack() * 0.6f);
            victim.addEffect(new MobEffectInstance(MobEffects.POISON, 180, 1));
            victim.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 180, 0));
        }
        if (this.beast.level() instanceof ServerLevel serverLevel) {
            NarutoParticles.spawnBolt(serverLevel, this.mouthPosition(),
                    centre.add(0, 1.0, 0), 3, 0.5, SLIME_GREEN);
            NarutoParticles.spawnRing(serverLevel, centre.add(0, 0.2, 0), 3.5, 28, SLIME_GREEN);
        }
        this.beast.level().playSound(null, this.beast.blockPosition(),
                SoundEvents.SLIME_ATTACK, SoundSource.HOSTILE, 2.5f, 0.5f);
    }

    private void corrosiveBurst() {
        Vec3 centre = this.beast.position();
        double reach = 9.0;
        for (LivingEntity victim : enemiesNear(centre, reach)) {
            victim.hurt(this.beast.damageSources().mobAttack(this.beast), this.attack() * 0.9f);
            victim.addEffect(new MobEffectInstance(MobEffects.POISON, 240, 2));
        }
        if (this.beast.level() instanceof ServerLevel serverLevel) {
            NarutoParticles.spawnRing(serverLevel, centre, reach, 50, SLIME_GREEN);
            NarutoParticles.spawnBurst(serverLevel, centre.add(0, 2.0, 0), 100, 4.0, SLIME_GREEN);
        }
        this.beast.level().playSound(null, this.beast.blockPosition(),
                SoundEvents.SLIME_SQUISH, SoundSource.HOSTILE, 3.0f, 0.4f);
    }

    /** Chomei's scales. It is a blinding technique first and damage second. */
    private void scaleDust(LivingEntity target) {
        Vec3 centre = target.position();
        for (LivingEntity victim : enemiesNear(centre, 6.0)) {
            victim.hurt(this.beast.damageSources().mobAttack(this.beast), this.attack() * 0.45f);
            victim.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 120, 0));
            victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 120, 1));
        }
        if (this.beast.level() instanceof ServerLevel serverLevel) {
            NarutoParticles.spawnBurst(serverLevel, centre.add(0, 1.5, 0), 110, 4.5, SCALE_GOLD);
        }
        this.beast.level().playSound(null, this.beast.blockPosition(),
                SoundEvents.BEE_LOOP_AGGRESSIVE, SoundSource.HOSTILE, 3.0f, 0.7f);
    }

    private void inkSpray(LivingEntity target) {
        Vec3 centre = target.position();
        for (LivingEntity victim : enemiesNear(centre, 5.5)) {
            victim.hurt(this.beast.damageSources().mobAttack(this.beast), this.attack() * 0.6f);
            victim.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 160, 0));
        }
        if (this.beast.level() instanceof ServerLevel serverLevel) {
            NarutoParticles.spawnBolt(serverLevel, this.mouthPosition(),
                    centre.add(0, 1.0, 0), 2, 0.4, INK_BLACK);
            NarutoParticles.spawnBurst(serverLevel, centre.add(0, 1.2, 0), 90, 4.0, INK_BLACK);
        }
        this.beast.level().playSound(null, this.beast.blockPosition(),
                SoundEvents.SQUID_SQUIRT, SoundSource.HOSTILE, 3.0f, 0.5f);
    }

    /** Eight tentacles, three sweeps. Wide, repeated, and it moves people. */
    private void tentacleSweep() {
        Vec3 centre = this.beast.position();
        double reach = 11.0;
        for (LivingEntity victim : enemiesNear(centre, reach)) {
            for (int hit = 0; hit < 3; hit++) {
                victim.invulnerableTime = 0; // three distinct sweeps, not one with a big number
                victim.hurt(this.beast.damageSources().mobAttack(this.beast), this.attack() * 0.5f);
            }
            Vec3 push = victim.position().subtract(centre).normalize().scale(1.3);
            victim.setDeltaMovement(push.x, 0.5, push.z);
            victim.hurtMarked = true;
        }
        if (this.beast.level() instanceof ServerLevel serverLevel) {
            NarutoParticles.spawnRing(serverLevel, centre.add(0, 2.0, 0), reach, 60, INK_BLACK);
        }
        this.beast.level().playSound(null, this.beast.blockPosition(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 3.0f, 0.4f);
    }

    private void blindNearby(double radius, int ticks) {
        for (LivingEntity victim : enemiesNear(this.beast.position(), radius)) {
            victim.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, ticks, 0));
        }
    }

    private void igniteNearby(double radius, int seconds) {
        for (LivingEntity victim : enemiesNear(this.beast.position(), radius)) {
            victim.setSecondsOnFire(seconds);
        }
    }

    // ------------------------------------------------------------------ helpers

    private float attack() {
        return (float) this.beast.getAttributeValue(Attributes.ATTACK_DAMAGE);
    }

    /** Everything in range that is not the beast itself and not another tailed beast. */
    private List<LivingEntity> enemiesNear(Vec3 centre, double radius) {
        return this.beast.level().getEntitiesOfClass(LivingEntity.class,
                new AABB(centre, centre).inflate(radius),
                candidate -> candidate != this.beast && candidate.isAlive()
                        && !(candidate instanceof TailedBeastEntity));
    }
}
