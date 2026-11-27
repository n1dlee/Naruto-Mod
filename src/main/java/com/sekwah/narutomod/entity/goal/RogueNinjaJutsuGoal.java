package com.sekwah.narutomod.entity.goal;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.abilities.NarutoAbilities;
import com.sekwah.narutomod.entity.RogueNinjaEntity;
import com.sekwah.narutomod.entity.jutsuprojectile.GreatFireballEntity;
import com.sekwah.narutomod.entity.jutsuprojectile.GianBeamEntity;
import com.sekwah.narutomod.entity.jutsuprojectile.GustFrontEntity;
import com.sekwah.narutomod.entity.jutsuprojectile.ShockArcEntity;
import com.sekwah.narutomod.entity.jutsuprojectile.WaterShotEntity;
import com.sekwah.narutomod.events.EarthSpikeCast;
import com.sekwah.narutomod.sounds.NarutoSounds;
import com.sekwah.narutomod.util.SharinganCopy;
import net.minecraftforge.registries.RegistryObject;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
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

    private static final int SPIKE_COUNT = 3;


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
        GreatFireballEntity.launch(this.ninja, aimVector(target), 0, FIREBALL_MULTIPLIER);
        return NarutoAbilities.FIREBALL;
    }

    /** Water Release: Water Bullet. Also the genuine projectile. */
    private RegistryObject<? extends Ability> castWaterBullet(LivingEntity target) {
        WaterShotEntity.launch(this.ninja, aimVector(target), WATER_BULLET_MULTIPLIER, false);
        return NarutoAbilities.WATER_BULLET;
    }

    private RegistryObject<? extends Ability> castEarthSpikes(LivingEntity target) {
        Vec3 toTarget = target.position().subtract(this.ninja.position());
        Vec3 forward = new Vec3(toTarget.x, 0, toTarget.z).normalize();
        if (forward.lengthSqr() < 1.0e-8 || !EarthSpikeCast.line(this.ninja, forward, SPIKE_COUNT, EARTH_SPIKE_DAMAGE)) {
            return null;
        }
        playCastSound(NarutoSounds.EARTH_SPIKE_CRACK.get(), 0.92f);
        return NarutoAbilities.EARTH_SPIKES;
    }

    /** Wind Release: Great Breakthrough - a cone that hurls everything in front away. */
    private RegistryObject<? extends Ability> castGreatBreakthrough(LivingEntity target) {
        return GustFrontEntity.launch(this.ninja, aimVector(target), 1) ? NarutoAbilities.GREAT_BREAKTHROUGH : null;
    }

    /**
     * Lightning Release. Rolls between the two techniques a level-6 lightning user knows:
     * Lightning Shock (a stunning bolt) and False Darkness (a piercing beam). Chidori sits
     * at exactly level 6 too but is excluded on purpose - it is Kakashi's signature, not
     * something every missing-nin should be throwing.
     */
    private RegistryObject<? extends Ability> castLightning(LivingEntity target) {
        boolean falseDarkness = this.ninja.getRandom().nextInt(3) == 0;
        if (falseDarkness) {
            Vec3 center = target.position().add(0, target.getBbHeight() * 0.5, 0);
            return GianBeamEntity.launch(this.ninja, center, FALSE_DARKNESS_DAMAGE, false)
                    ? NarutoAbilities.FALSE_DARKNESS : null;
        }
        Vec3 center = target.position().add(0, target.getBbHeight() * 0.5, 0);
        return ShockArcEntity.cast(this.ninja, center, LIGHTNING_SHOCK_DAMAGE) ? NarutoAbilities.LIGHTNING_SHOCK : null;
    }

    /** Leads the target slightly so a projectile at range is not trivially side-stepped. */
    private Vec3 aimVector(LivingEntity target) {
        Vec3 aimPoint = target.position().add(0, target.getBbHeight() * 0.5, 0)
                .add(target.getDeltaMovement().scale(2.0));
        return aimPoint.subtract(this.ninja.getEyePosition()).normalize();
    }

    private void playCastSound(net.minecraft.sounds.SoundEvent sound, float pitch) {
        this.ninja.level().playSound(null, this.ninja.blockPosition(), sound,
                SoundSource.HOSTILE, 1.1f, pitch);
    }
}
