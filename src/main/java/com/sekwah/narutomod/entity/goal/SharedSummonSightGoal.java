package com.sekwah.narutomod.entity.goal;

import com.sekwah.narutomod.entity.SummonBeastEntity;
import com.sekwah.narutomod.util.Faction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

/**
 * Summons called out by the same ninja see through one another.
 *
 * The Animal Path's defining trait is that its summons share vision through the Rinnegan -
 * what one of them finds, the rest already know. The mod's version of the technique put three
 * beasts on the field and left each of them hunting independently, so the one thing that
 * distinguished it from casting an ordinary summoning three times was missing.
 *
 * Applied to every summon rather than only that ability, because a pack that ignores what its
 * packmate is currently fighting reads as broken whoever called it out.
 *
 * Deliberately one-way: this goal only ADOPTS a target from an ally that already has one, and
 * never clears or assigns one to anybody else. Two beasts each writing the other's target
 * would be a loop, and a summon that inherits a target it cannot reach should be free to give
 * up on it through the normal goal rules.
 */
public class SharedSummonSightGoal extends Goal {

    /** How far word travels. Beyond this they are fighting separate battles. */
    private static final double SIGHT_SHARING_RANGE = 32.0;

    private final SummonBeastEntity beast;

    public SharedSummonSightGoal(SummonBeastEntity beast) {
        this.beast = beast;
        // No movement or looking flags: this goal only writes the target, so claiming any
        // control flag here would block the goals that actually act on it.
        this.setFlags(java.util.EnumSet.noneOf(Goal.Flag.class));
    }

    @Override
    public boolean canUse() {
        if (this.beast.getTarget() != null) {
            return false;
        }
        LivingEntity shared = this.findAllyTarget();
        if (shared == null) {
            return false;
        }
        this.beast.setTarget(shared);
        return false; // The target is set; there is no ongoing behaviour to own.
    }

    /** A target one of the summoner's other beasts is already engaged with. */
    private LivingEntity findAllyTarget() {
        for (SummonBeastEntity ally : this.beast.level().getEntitiesOfClass(SummonBeastEntity.class,
                this.beast.getBoundingBox().inflate(SIGHT_SHARING_RANGE),
                other -> other != this.beast && other.isAlive()
                        && Faction.sameSide(this.beast, other))) {
            LivingEntity target = ally.getTarget();
            if (target != null && target.isAlive() && !Faction.sameSide(this.beast, target)) {
                return target;
            }
        }
        return null;
    }
}
