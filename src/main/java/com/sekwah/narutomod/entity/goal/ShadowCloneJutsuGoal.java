package com.sekwah.narutomod.entity.goal;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.abilities.NarutoAbilities;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.capabilities.NinjaCapabilityHandler;
import com.sekwah.narutomod.entity.ShadowCloneEntity;
import com.sekwah.narutomod.entity.jutsuprojectile.ChakraDragonEntity;
import com.sekwah.narutomod.entity.jutsuprojectile.GianBeamEntity;
import com.sekwah.narutomod.entity.jutsuprojectile.GreatFireballEntity;
import com.sekwah.narutomod.entity.jutsuprojectile.GustFrontEntity;
import com.sekwah.narutomod.entity.jutsuprojectile.WaterShotEntity;
import com.sekwah.narutomod.events.EarthSpikeCast;
import com.sekwah.narutomod.util.Faction;
import com.sekwah.narutomod.util.ShadowCloneChakra;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.EnumSet;

public final class ShadowCloneJutsuGoal extends Goal {
    private static final int WINDUP = 8;
    private static final int COOLDOWN = 75;
    private static final Technique[] TECHNIQUES = Technique.values();
    private final ShadowCloneEntity clone;
    private Technique selected;
    private LivingEntity target;
    private int windup;
    private long nextCast;

    public ShadowCloneJutsuGoal(ShadowCloneEntity clone) {
        this.clone = clone;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        target = clone.getTarget();
        if (clone.level().getGameTime() < nextCast || !aimable(target)) {
            return false;
        }
        var owner = clone.playerOwner();
        if (owner == null || !owner.isAlive() || owner.level() != clone.level()) {
            return false;
        }
        INinjaData data = owner.getCapability(NinjaCapabilityHandler.NINJA_DATA).orElse(null);
        if (data == null || !data.isNinjaModeEnabled()) {
            return false;
        }
        var ledger = ShadowCloneChakra.of(owner.server);
        var choices = new ArrayList<Technique>();
        for (Technique tech : TECHNIQUES) {
            if (ledger.canSpend(clone.getUUID(), tech.cost) && tech.usable(data, clone.distanceTo(target))) {
                choices.add(tech);
            }
        }
        if (choices.isEmpty()) {
            nextCast = clone.level().getGameTime() + 15;
            return false;
        }
        selected = choices.get(clone.getRandom().nextInt(choices.size()));
        return true;
    }

    @Override
    public void start() {
        windup = WINDUP;
        nextCast = clone.level().getGameTime() + COOLDOWN + clone.getRandom().nextInt(21);
        clone.getNavigation().stop();
        clone.swing(InteractionHand.MAIN_HAND);
    }

    @Override
    public boolean canContinueToUse() {
        return windup > 0 && target == clone.getTarget() && aimable(target);
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (!canContinueToUse()) {
            return;
        }
        clone.getLookControl().setLookAt(target, 30f, 30f);
        if (--windup != 0) {
            return;
        }
        var owner = clone.playerOwner();
        if (owner != null && owner.isAlive() && owner.level() == clone.level()) {
            owner.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(data -> {
                var ledger = ShadowCloneChakra.of(owner.server);
                if (data.isNinjaModeEnabled() && selected.usable(data, clone.distanceTo(target))
                        && ledger.canSpend(clone.getUUID(), selected.cost) && selected.cast(clone, target)) {
                    ledger.spend(clone.getUUID(), selected.cost);
                }
            });
        }
    }

    private boolean aimable(LivingEntity candidate) {
        if (candidate == null || !candidate.isAlive() || Faction.sameSide(clone, candidate)
                || clone.isAlliedTo(candidate)) {
            return false;
        }
        double distance = clone.distanceToSqr(candidate);
        return distance >= 9 && distance <= 16 * 16 && clone.hasLineOfSight(candidate);
    }

    private enum Technique {
        FIREBALL(NarutoAbilities.FIREBALL, 30, 3, 16),
        WATER_BULLET(NarutoAbilities.WATER_BULLET, 30, 3, 16),
        WATER_DRAGON(NarutoAbilities.WATER_DRAGON, 55, 7, 16),
        EARTH_SPIKES(NarutoAbilities.EARTH_SPIKES, 30, 3, 6),
        WIND(NarutoAbilities.GREAT_BREAKTHROUGH, 35, 3, 12),
        LIGHTNING(NarutoAbilities.FALSE_DARKNESS, 45, 5, 16);

        private final RegistryObject<? extends Ability> ability;
        private final int cost;
        private final int minRange;
        private final int maxRange;

        Technique(RegistryObject<? extends Ability> ability, int cost, int minRange, int maxRange) {
            this.ability = ability;
            this.cost = cost;
            this.minRange = minRange;
            this.maxRange = maxRange;
        }

        boolean usable(INinjaData data, double distance) {
            return distance >= minRange && distance <= maxRange && data.isJutsuLearned(ability.getId().getPath())
                    && ability.get().hasElementAccess(data) && ability.get().hasClanAccess(data)
                    && ability.get().hasEyeAccess(data);
        }

        boolean cast(ShadowCloneEntity caster, LivingEntity target) {
            Vec3 center = target.getBoundingBox().getCenter();
            Vec3 axis = center.subtract(caster.getEyePosition()).normalize();
            switch (this) {
                case FIREBALL -> GreatFireballEntity.launch(caster, axis, 0, 0.55f);
                case WATER_BULLET -> WaterShotEntity.launch(caster, axis, 0.55f, false);
                case WATER_DRAGON -> ChakraDragonEntity.launchWater(caster, center, 8);
                case EARTH_SPIKES -> {
                    return EarthSpikeCast.line(caster, axis.multiply(1, 0, 1).normalize(), 3, 6);
                }
                case WIND -> {
                    return GustFrontEntity.launch(caster, axis, 0.55f);
                }
                case LIGHTNING -> {
                    return GianBeamEntity.launch(caster, center, 6, false);
                }
            }
            return true;
        }
    }
}
