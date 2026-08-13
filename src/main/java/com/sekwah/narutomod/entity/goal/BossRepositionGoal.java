package com.sekwah.narutomod.entity.goal;

import com.sekwah.narutomod.entity.MangekyoBossEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Periodically walks a boss back out to throwing distance.
 *
 * Without this, every ranged technique in {@link BossJutsuGoal} is dead code. The bosses
 * run MeleeAttackGoal, which closes the gap and then holds it, so the caster spends the
 * entire fight inside six blocks - and every rotation branch guarded by "distance > 6" or
 * "distance > 8" can never be reached. Kirin, Great Fireball, Kamui's pull, Kisame's shark
 * volley and Sasori's senbon were all written, registered and unreachable.
 *
 * So the fight now has a rhythm instead of one range band: the boss presses for a while,
 * then disengages to stand-off distance and works from there, then closes again. Taking the
 * MOVE flag is what makes it work - MeleeAttackGoal wants the same flag and this goal sits
 * above it, so the melee chase genuinely stops for the duration rather than fighting it.
 */
public class BossRepositionGoal extends Goal {

    /** Ticks of melee pressure between stand-offs (polled every other tick, so ~9s). */
    private static final int PRESSURE_TICKS = 90;
    /** How long a stand-off lasts once reached. */
    private static final int STANDOFF_TICKS = 100;
    /** The range the boss backs out to - past every "distance >" gate in the rotations. */
    private static final double STANDOFF_RANGE = 13.0;
    /** Only bother disengaging if the target is inside this. */
    private static final double CROWDED_RANGE = 9.0;
    private static final int REPATH_INTERVAL = 10;

    private final MangekyoBossEntity boss;
    private int cooldown = PRESSURE_TICKS;
    private int remaining;
    private int repathIn;

    public BossRepositionGoal(MangekyoBossEntity boss) {
        this.boss = boss;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (this.cooldown > 0) {
            this.cooldown--;
            return false;
        }
        LivingEntity target = this.boss.getTarget();
        if (target == null || !target.isAlive()) {
            return false;
        }
        // A giant does not skip backwards; it stands and stamps. Asks isGiant() rather than
        // the stage number, or every non-giant wielder would also stop repositioning for the
        // last twelve percent of the fight purely because they share the counter.
        if (this.boss.isGiant()) {
            return false;
        }
        return this.boss.distanceTo(target) < CROWDED_RANGE;
    }

    @Override
    public void start() {
        this.remaining = STANDOFF_TICKS;
        this.repathIn = 0;
    }

    @Override
    public boolean canContinueToUse() {
        LivingEntity target = this.boss.getTarget();
        return this.remaining > 0 && target != null && target.isAlive();
    }

    @Override
    public void tick() {
        this.remaining--;
        LivingEntity target = this.boss.getTarget();
        if (target == null) {
            return;
        }
        // Keep facing the target the whole way out - a boss that turns its back to retreat
        // reads as fleeing, and it also cannot aim anything while it does.
        this.boss.getLookControl().setLookAt(target, 30f, 30f);

        if (this.boss.distanceTo(target) >= STANDOFF_RANGE) {
            this.boss.getNavigation().stop();
            return;
        }
        if (this.repathIn-- > 0) {
            return;
        }
        this.repathIn = REPATH_INTERVAL;

        Vec3 away = DefaultRandomPos.getPosAway(this.boss, 14, 7, target.position());
        if (away == null) {
            // Cornered: no pathable retreat exists, so give the stand-off up rather than
            // grinding into a wall for five seconds doing nothing.
            this.remaining = 0;
            return;
        }
        this.boss.getNavigation().moveTo(away.x, away.y, away.z, 1.15D);
    }

    @Override
    public void stop() {
        this.cooldown = PRESSURE_TICKS;
        this.boss.getNavigation().stop();
    }
}
