package com.sekwah.narutomod.entity.goal;

import com.sekwah.narutomod.util.NarutoParticles;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * The chakra-assisted bound the player has on Leap, given to the mobs that should have it.
 *
 * A ninja closes a gap by jumping it. Without this the rogues and bosses walk the whole way
 * like any other mob, which is most of why a fight against them felt like a fight against a
 * reskinned zombie no matter what techniques they threw once they arrived.
 *
 * Deliberately not a teleport and not a charge: it is a real arc, so it can be read coming,
 * it can be sidestepped, and it can overshoot.
 */
public class NinjaLeapGoal extends Goal {

    /** Closer than this and there is nothing to jump over. */
    private static final double MIN_RANGE = 5.0;
    private static final double MAX_RANGE = 18.0;
    /** Polled every other tick by the goal selector, so this is roughly six seconds. */
    private static final int COOLDOWN_TICKS = 60;

    private final Mob mob;
    private final double horizontalPower;
    private final double verticalPower;
    private int cooldown = COOLDOWN_TICKS;

    public NinjaLeapGoal(Mob mob, double horizontalPower, double verticalPower) {
        this.mob = mob;
        this.horizontalPower = horizontalPower;
        this.verticalPower = verticalPower;
        // MOVE only: the jump is a single impulse, so holding LOOK would pointlessly block
        // whatever else wants to aim this tick.
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (this.cooldown > 0) {
            this.cooldown--;
            return false;
        }
        LivingEntity target = this.mob.getTarget();
        if (target == null || !target.isAlive()) {
            return false;
        }
        // Needs its feet under it. Leaping out of a fall just wastes the cooldown.
        if (!this.mob.onGround()) {
            return false;
        }
        double distance = this.mob.distanceTo(target);
        return distance >= MIN_RANGE && distance <= MAX_RANGE
                && this.mob.getSensing().hasLineOfSight(target);
    }

    @Override
    public boolean canContinueToUse() {
        return false; // one impulse, then control goes straight back
    }

    @Override
    public void start() {
        LivingEntity target = this.mob.getTarget();
        if (target == null) {
            return;
        }
        this.cooldown = COOLDOWN_TICKS;

        Vec3 toTarget = target.position().subtract(this.mob.position());
        Vec3 flat = new Vec3(toTarget.x, 0, toTarget.z);
        if (flat.lengthSqr() < 1.0E-4) {
            return;
        }
        Vec3 direction = flat.normalize();

        // Lift scales a little with the gap so a long jump actually clears the ground
        // between, rather than skimming into the first hill in the way.
        double lift = this.verticalPower + Math.min(flat.length() * 0.02, 0.25);
        this.mob.setDeltaMovement(direction.x * this.horizontalPower, lift,
                direction.z * this.horizontalPower);
        this.mob.hurtMarked = true;
        this.mob.getLookControl().setLookAt(target, 30f, 30f);

        this.mob.level().playSound(null, this.mob.blockPosition(),
                com.sekwah.narutomod.sounds.NarutoSounds.LEAP.get(), SoundSource.HOSTILE, 0.8f, 1.0f);
        if (this.mob.level() instanceof ServerLevel serverLevel) {
            NarutoParticles.spawnRing(serverLevel, this.mob.position(), 0.9, 16,
                    NarutoParticles.ROTATION_WHITE);
        }
    }
}
