package com.sekwah.narutomod.entity.goal;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.abilities.NarutoAbilities;
import com.sekwah.narutomod.damagetypes.NarutoDamageTypes;
import com.sekwah.narutomod.entity.RogueNinjaEntity;
import com.sekwah.narutomod.entity.jutsuprojectile.FireballJutsuEntity;
import com.sekwah.narutomod.entity.jutsuprojectile.WaterBulletJutsuEntity;
import com.sekwah.narutomod.util.NarutoParticles;
import com.sekwah.narutomod.util.SharinganCopy;
import net.minecraftforge.registries.RegistryObject;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Rank-and-file ninja fight with ninjutsu, not just a kunai.
 *
 * Each one is born with a single random nature at mastery 6 (see RogueNinjaEntity), and
 * this goal lets it throw anything from that nature's book up to that level. Rasengan and
 * Chidori are deliberately absent - those are the techniques that mark someone out as
 * exceptional, and so are Sage Mode, the transformations and the dojutsu arts. A missing-nin
 * is dangerous because they can actually use their element, not because they are a rival
 * to the player.
 *
 * Unlike BossJutsuGoal there is no chakra budget here. The spec is that they use their
 * element freely, so cadence is governed purely by the cooldown below - an invisible
 * resource that never ran out would be dead weight in the save file.
 *
 * Fire and Water spawn the real jutsu projectiles (those entities already take a
 * LivingEntity owner, so the mob's fireball is byte-for-byte the player's fireball). Earth,
 * Wind and Lightning are reimplemented here because their player-side abilities are welded
 * to Player and INinjaData - scheduleDelayedTickEvent, playerAttack damage sources, rank
 * multipliers - and prying those apart would mean rewriting live player combat code.
 */
public class RogueNinjaJutsuGoal extends Goal {

    private static final double MIN_RANGE = 4.0;
    private static final double MAX_RANGE = 20.0;
    /**
     * Counted down inside canUse, which Mob.serverAiStep only reaches on every other tick,
     * so this is roughly six seconds of real time rather than three. Deliberately slower
     * than a boss's - these turn up several at a time.
     */
    private static final int COOLDOWN_TICKS = 60;

    // Damage sits between a vanilla melee hit and the player's own version of each jutsu.
    private static final float FIREBALL_MULTIPLIER = 0.6f;
    private static final float WATER_BULLET_MULTIPLIER = 0.7f;
    private static final float LIGHTNING_SHOCK_DAMAGE = 5.0f;
    private static final float FALSE_DARKNESS_DAMAGE = 6.5f;
    private static final float EARTH_SPIKE_DAMAGE = 6.0f;
    private static final float WIND_DAMAGE = 4.0f;

    private static final int SPIKE_COUNT = 3;
    private static final int SPIKE_HEIGHT = 2;
    private static final int SPIKE_LIFESPAN = 120;

    private static final double WIND_RANGE = 8.0;
    private static final double WIND_HALF_ANGLE_COS = 0.65;

    private final RogueNinjaEntity ninja;
    private int cooldown;

    public RogueNinjaJutsuGoal(RogueNinjaEntity ninja) {
        this.ninja = ninja;
        this.setFlags(EnumSet.of(Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (this.cooldown > 0) {
            this.cooldown--;
            return false;
        }
        LivingEntity target = this.ninja.getTarget();
        if (target == null || !target.isAlive()) {
            return false;
        }
        double distance = this.ninja.distanceTo(target);
        return distance >= MIN_RANGE && distance <= MAX_RANGE
                && this.ninja.getSensing().hasLineOfSight(target);
    }

    @Override
    public boolean canContinueToUse() {
        return false; // single-shot: fire once, then hand control back to the melee goals
    }

    @Override
    public void start() {
        LivingEntity target = this.ninja.getTarget();
        if (target == null) {
            return;
        }
        this.cooldown = COOLDOWN_TICKS;
        this.ninja.getLookControl().setLookAt(target, 30f, 30f);

        RegistryObject<? extends Ability> cast = switch (this.ninja.getElement()) {
            case "fire" -> castFireball(target);
            case "water" -> castWaterBullet(target);
            case "earth" -> castEarthSpikes(target);
            case "wind" -> castGreatBreakthrough(target);
            default -> castLightning(target);
        };
        offerToSharingan(cast);
    }

    /**
     * Exposes what was just thrown to any watching Sharingan, the same way the bosses do.
     * This matters more here than it does for the bosses: it means a player can build their
     * copy wheel off ordinary encounters instead of having to hunt down an S-rank first.
     */
    private void offerToSharingan(RegistryObject<? extends Ability> cast) {
        if (cast == null) {
            return;
        }
        SharinganCopy.onJutsuPerformed(this.ninja, cast.get(), cast.getId().getPath());
    }

    /** Fire Release: Fireball. The genuine projectile, owned by the mob. */
    private RegistryObject<? extends Ability> castFireball(LivingEntity target) {
        Vec3 aim = aimVector(target);
        FireballJutsuEntity fireball = new FireballJutsuEntity(this.ninja, aim.x, aim.y, aim.z);
        // Uncharged, and scaled below a player's: a chunin-grade fireball, not Madara's.
        fireball.setChargeAmount(0, false, FIREBALL_MULTIPLIER);
        this.ninja.level().addFreshEntity(fireball);
        playCastSound(SoundEvents.FIRECHARGE_USE, 0.9f);
        return NarutoAbilities.FIREBALL;
    }

    /** Water Release: Water Bullet. Also the genuine projectile. */
    private RegistryObject<? extends Ability> castWaterBullet(LivingEntity target) {
        Vec3 aim = aimVector(target);
        WaterBulletJutsuEntity bullet = new WaterBulletJutsuEntity(this.ninja, aim.x, aim.y, aim.z);
        bullet.setDamageMultiplier(WATER_BULLET_MULTIPLIER);
        this.ninja.level().addFreshEntity(bullet);
        playCastSound(SoundEvents.BUCKET_EMPTY, 1.1f);
        return NarutoAbilities.WATER_BULLET;
    }

    /**
     * Earth Release: Earth Spikes - a line of pillars erupting toward the target, throwing
     * anything caught into the air. The dirt is registered with the entity so it comes back
     * down; if mob griefing is off the hit still lands and only the blocks are skipped.
     */
    private RegistryObject<? extends Ability> castEarthSpikes(LivingEntity target) {
        Vec3 toTarget = target.position().subtract(this.ninja.position());
        Vec3 forward = new Vec3(toTarget.x, 0, toTarget.z).normalize();
        boolean mayPlaceBlocks = net.minecraftforge.event.ForgeEventFactory
                .getMobGriefingEvent(this.ninja.level(), this.ninja);
        // The mod has no earth damage type, and inventing one would need a data file plus a
        // datagen pass for a single mob attack. A plain mob hit reads the same in game.
        DamageSource source = this.ninja.damageSources().mobAttack(this.ninja);

        for (int i = 1; i <= SPIKE_COUNT; i++) {
            double distance = i * 2.0;
            double x = this.ninja.getX() + forward.x * distance;
            double z = this.ninja.getZ() + forward.z * distance;
            BlockPos root = groundAt(x, z);

            for (LivingEntity caught : this.ninja.level().getEntitiesOfClass(LivingEntity.class,
                    new net.minecraft.world.phys.AABB(root).inflate(0.9, SPIKE_HEIGHT, 0.9),
                    e -> e != this.ninja && e.isAlive() && !(e instanceof RogueNinjaEntity))) {
                caught.hurt(source, EARTH_SPIKE_DAMAGE);
                Vec3 velocity = caught.getDeltaMovement();
                caught.setDeltaMovement(velocity.x * 0.4, Math.min(velocity.y + 0.65, 1.1), velocity.z * 0.4);
                caught.hurtMarked = true;
            }

            if (mayPlaceBlocks && this.ninja.level() instanceof ServerLevel serverLevel) {
                for (int h = 0; h < SPIKE_HEIGHT; h++) {
                    BlockPos pos = root.above(h);
                    if (serverLevel.getBlockState(pos).isAir()) {
                        serverLevel.setBlockAndUpdate(pos, Blocks.DIRT.defaultBlockState());
                        this.ninja.trackRaisedSpike(pos, SPIKE_LIFESPAN);
                    }
                }
                BlockParticleOption debris =
                        new BlockParticleOption(ParticleTypes.BLOCK, Blocks.DIRT.defaultBlockState());
                serverLevel.sendParticles(debris, root.getX() + 0.5, root.getY() + 1.0, root.getZ() + 0.5,
                        14, 0.35, 0.4, 0.35, 0.04);
            }
        }
        playCastSound(SoundEvents.GRAVEL_BREAK, 0.7f);
        return NarutoAbilities.EARTH_SPIKES;
    }

    /** Wind Release: Great Breakthrough - a cone that hurls everything in front away. */
    private RegistryObject<? extends Ability> castGreatBreakthrough(LivingEntity target) {
        Vec3 eye = this.ninja.getEyePosition();
        Vec3 look = target.getEyePosition().subtract(eye).normalize();

        for (LivingEntity caught : this.ninja.level().getEntitiesOfClass(LivingEntity.class,
                this.ninja.getBoundingBox().expandTowards(look.scale(WIND_RANGE)).inflate(WIND_RANGE * 0.5),
                e -> e != this.ninja && e.isAlive() && !(e instanceof RogueNinjaEntity))) {
            Vec3 toCaught = caught.position().add(0, caught.getBbHeight() * 0.5, 0).subtract(eye).normalize();
            if (toCaught.dot(look) < WIND_HALF_ANGLE_COS || eye.distanceTo(caught.position()) > WIND_RANGE) {
                continue;
            }
            caught.hurt(this.ninja.damageSources().mobAttack(this.ninja), WIND_DAMAGE);
            caught.knockback(1.4, -look.x, -look.z);
            caught.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 2 * 20, 0, false, true));
        }
        playCastSound(SoundEvents.PHANTOM_FLAP, 0.8f);

        if (this.ninja.level() instanceof ServerLevel serverLevel) {
            for (int i = 1; i <= 20; i++) {
                Vec3 point = eye.add(look.scale(i / 20.0 * WIND_RANGE));
                double spread = (i / 20.0) * WIND_RANGE * 0.3;
                serverLevel.sendParticles(ParticleTypes.CLOUD,
                        point.x, point.y, point.z, 2, spread * 0.5, spread * 0.25, spread * 0.5, 0.05);
            }
        }
        return NarutoAbilities.GREAT_BREAKTHROUGH;
    }

    /**
     * Lightning Release. Rolls between the two techniques a level-6 lightning user knows:
     * Lightning Shock (a stunning bolt) and False Darkness (a piercing beam). Chidori sits
     * at exactly level 6 too but is excluded on purpose - it is Kakashi's signature, not
     * something every missing-nin should be throwing.
     */
    private RegistryObject<? extends Ability> castLightning(LivingEntity target) {
        boolean falseDarkness = this.ninja.getRandom().nextInt(3) == 0;
        DamageSource source = NarutoDamageTypes.getDamageSource(
                this.ninja.level(), NarutoDamageTypes.CHIDORI, this.ninja, this.ninja);

        if (falseDarkness) {
            // A beam: everything on the line between them takes it, not just the target.
            Vec3 eye = this.ninja.getEyePosition();
            Vec3 look = target.getEyePosition().subtract(eye).normalize();
            for (LivingEntity caught : this.ninja.level().getEntitiesOfClass(LivingEntity.class,
                    this.ninja.getBoundingBox().expandTowards(look.scale(MAX_RANGE)).inflate(1.5),
                    e -> e != this.ninja && e.isAlive() && !(e instanceof RogueNinjaEntity))) {
                Vec3 toCaught = caught.getEyePosition().subtract(eye).normalize();
                if (toCaught.dot(look) > 0.94) {
                    caught.hurt(source, FALSE_DARKNESS_DAMAGE);
                }
            }
            drawBolt(eye, eye.add(look.scale(MAX_RANGE)));
            playCastSound(SoundEvents.LIGHTNING_BOLT_THUNDER, 1.4f);
            return NarutoAbilities.FALSE_DARKNESS;
        }

        target.hurt(source, LIGHTNING_SHOCK_DAMAGE);
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 2 * 20, 2, false, true));
        drawBolt(this.ninja.getEyePosition(), target.position().add(0, target.getBbHeight() * 0.5, 0));
        playCastSound(SoundEvents.LIGHTNING_BOLT_IMPACT, 1.5f);
        return NarutoAbilities.LIGHTNING_SHOCK;
    }

    private void drawBolt(Vec3 from, Vec3 to) {
        if (!(this.ninja.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        int steps = Math.max(6, (int) (from.distanceTo(to) * 4));
        for (int i = 0; i <= steps; i++) {
            Vec3 point = from.lerp(to, i / (double) steps);
            serverLevel.sendParticles(NarutoParticles.CHIDORI_CYAN,
                    point.x, point.y, point.z, 1, 0.05, 0.05, 0.05, 0.0);
        }
    }

    /** Leads the target slightly so a projectile at range is not trivially side-stepped. */
    private Vec3 aimVector(LivingEntity target) {
        Vec3 aimPoint = target.position().add(0, target.getBbHeight() * 0.5, 0)
                .add(target.getDeltaMovement().scale(2.0));
        return aimPoint.subtract(this.ninja.getEyePosition()).normalize();
    }

    private BlockPos groundAt(double x, double z) {
        BlockPos pos = BlockPos.containing(x, this.ninja.getY(), z);
        int minY = this.ninja.level().getMinBuildHeight();
        while (pos.getY() > minY && this.ninja.level().getBlockState(pos.below()).isAir()) {
            pos = pos.below();
        }
        return pos;
    }

    private void playCastSound(net.minecraft.sounds.SoundEvent sound, float pitch) {
        this.ninja.level().playSound(null, this.ninja.blockPosition(), sound,
                SoundSource.HOSTILE, 1.1f, pitch);
    }
}
