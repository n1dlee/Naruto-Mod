package com.sekwah.narutomod.entity.goal;

import com.sekwah.narutomod.entity.SummonBeastEntity;
import com.sekwah.narutomod.entity.SummonBeastVariant;
import com.sekwah.narutomod.entity.jutsuprojectile.WaterShotEntity;
import com.sekwah.narutomod.util.Faction;
import com.sekwah.narutomod.util.JutsuAim;
import com.sekwah.narutomod.util.NarutoParticles;
import com.sekwah.narutomod.util.ProjectileAxis;
import com.sekwah.narutomod.util.WaterDragonFlight;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.EnumSet;

public final class SummonBeastJutsuGoal extends Goal {
    private static final int LOAD = 8;
    private static final Vec3 UP = new Vec3(0, 1, 0);
    private final SummonBeastEntity beast;
    private int reuse;
    private int remaining;
    private @Nullable LivingEntity target;

    public SummonBeastJutsuGoal(SummonBeastEntity beast) {
        this.beast = beast;
        this.setFlags(EnumSet.of(Flag.LOOK, Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (this.reuse > 0) {
            this.reuse--;
            return false;
        }
        if (!(this.beast.level() instanceof ServerLevel level) || !this.beast.canUseSignature()) {
            return false;
        }
        this.target = this.beast.getVariant() == SummonBeastVariant.KATSUYU ? patient(level) : this.beast.getTarget();
        return this.target != null && eligible(level, this.target);
    }

    @Override
    public void start() {
        this.remaining = adjustedTickDelay(LOAD);
        this.beast.signal(LOAD);
        this.beast.getNavigation().stop();
        if (this.level() instanceof ServerLevel level) {
            Vec3 mark = mouth();
            level.sendParticles(ParticleTypes.POOF, mark.x, mark.y, mark.z, 7, 0.14, 0.12, 0.16, 0.025);
            level.playSound(null, this.beast, cue(this.beast.getVariant()), SoundSource.NEUTRAL, 0.67f, 0.76f);
        }
    }

    @Override
    public boolean canContinueToUse() {
        return this.remaining > 0 && this.level() instanceof ServerLevel level
                && this.target != null && this.beast.canUseSignature() && eligible(level, this.target);
    }

    @Override
    public void tick() {
        if (this.target == null) {
            return;
        }
        this.beast.getLookControl().setLookAt(this.target, 26, 24);
        if (--this.remaining == 0 && this.level() instanceof ServerLevel level && eligible(level, this.target)) {
            this.beast.signal(-7);
            fire(level, this.target);
        }
    }

    @Override
    public void stop() {
        this.reuse = reducedTickDelay(this.remaining > 0 ? 28 : this.beast.getVariant().isSupport() ? 104 : 52);
        if (this.remaining > 0) {
            this.beast.signal(0);
        }
        this.target = null;
        this.remaining = 0;
    }

    private void fire(ServerLevel level, LivingEntity actor) {
        switch (this.beast.getVariant()) {
            case GAMABUNTA -> water(actor);
            case MANDA -> venom(level, actor);
            case KATSUYU -> mend(level, actor);
            case ENMA -> staff(level, actor);
        }
    }

    private void water(LivingEntity target) {
        Vec3 origin = this.beast.position().add(0, this.beast.getBbHeight() * 0.80, 0);
        Vec3 direction = ProjectileAxis.direction(JutsuAim.center(target).subtract(origin), this.beast.getLookAngle());
        Vec3 side = UP.cross(direction).normalize();
        for (int i = -1; i <= 1; i++) {
            WaterShotEntity.launch(this.beast, direction.add(side.scale(i * 0.064)), 1.12f, true);
        }
    }

    private void venom(ServerLevel level, LivingEntity victim) {
        if (victim.hurt(this.beast.damageSources().mobAttack(this.beast), 12f)) {
            victim.addEffect(new MobEffectInstance(MobEffects.POISON, 80, 0));
            Vec3 at = JutsuAim.center(victim);
            NarutoParticles.spawnBurst(level, at, 10, 0.31, NarutoParticles.LOG_BROWN);
            level.playSound(null, this.beast, SoundEvents.RAVAGER_ATTACK, SoundSource.NEUTRAL, 0.73f, 0.72f);
        }
    }

    private void mend(ServerLevel level, LivingEntity victim) {
        float previous = victim.getHealth();
        victim.heal(Math.min(10f, (victim.getMaxHealth() - previous) * 0.52f));
        if (victim.getHealth() > previous) {
            Vec3 at = JutsuAim.center(victim);
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, at.x, at.y, at.z, 12, 0.28, 0.35, 0.28, 0.02);
            NarutoParticles.spawnRing(level, victim.position().add(0, 0.14, 0), 0.90, 17, NarutoParticles.SAGE_GOLD);
        }
    }

    private void staff(ServerLevel level, LivingEntity aim) {
        Vec3 origin = mouth();
        Vec3 axis = ProjectileAxis.direction(JutsuAim.center(aim).subtract(origin), this.beast.getLookAngle());
        Vec3 limit = origin.add(axis.scale(12.8));
        if (!level.hasChunksAt(BlockPos.containing(origin), BlockPos.containing(limit))) {
            return;
        }
        var obstacle = level.clip(new ClipContext(origin, limit, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this.beast));
        Vec3 stop = obstacle.getLocation();
        var volume = new AABB(origin, stop).inflate(0.27);
        var hit = level.getEntitiesOfClass(LivingEntity.class, volume, target -> enemy(level, target) && this.beast.hasLineOfSight(target))
                .stream().filter(target -> WaterDragonFlight.sweep(origin, stop, target.getBoundingBox(), 0.22) != null)
                .min(Comparator.comparingDouble(target -> JutsuAim.center(target).distanceToSqr(origin))).orElse(null);
        if (hit != null && hit.hurt(this.beast.damageSources().mobAttack(this.beast), 15f)) {
            hit.push(axis.x * 0.62, 0.19, axis.z * 0.62);
            hit.hurtMarked = true;
        }
        NarutoParticles.spawnBolt(level, origin, stop, 2, 0.10, NarutoParticles.METAL_GRAY);
    }

    private boolean eligible(ServerLevel level, LivingEntity actor) {
        var variant = this.beast.getVariant();
        double range = switch (variant) {
            case GAMABUNTA -> 23;
            case MANDA -> 5.8;
            case KATSUYU -> 9;
            case ENMA -> 12.8;
        };
        return actor.level() == level && actor.isAlive() && this.beast.distanceToSqr(actor) <= range * range
                && this.beast.hasLineOfSight(actor) && (variant.isSupport() ? friendly(actor) && wounded(actor) : enemy(level, actor));
    }

    private boolean enemy(ServerLevel level, LivingEntity actor) {
        Player owner = this.beast.getOwner();
        return actor != this.beast && !friendly(actor) && !actor.isSpectator() && !actor.noPhysics
                && (actor instanceof Enemy || actor == this.beast.getTarget())
                && (owner == null ? !(actor instanceof Player) : JutsuAim.attackable(owner, actor)
                && (!(actor instanceof Player) || level.getServer().isPvpAllowed()) && level.mayInteract(owner, actor.blockPosition()));
    }

    private boolean friendly(LivingEntity target) {
        Player owner = this.beast.getOwner();
        return Faction.sameSide(this.beast, target) || owner != null && owner.isAlliedTo(target);
    }

    private @Nullable LivingEntity patient(ServerLevel level) {
        return level.getEntitiesOfClass(LivingEntity.class, this.beast.getBoundingBox().inflate(9),
                        ally -> ally != this.beast && eligible(level, ally))
                .stream().min(Comparator.comparingDouble(ally -> ally.getHealth() / ally.getMaxHealth())).orElse(null);
    }

    private static boolean wounded(LivingEntity actor) { return actor.getHealth() < actor.getMaxHealth() * 0.88f; }

    private static net.minecraft.sounds.SoundEvent cue(SummonBeastVariant variant) {
        return switch (variant) {
            case GAMABUNTA -> SoundEvents.FROG_AMBIENT;
            case MANDA -> SoundEvents.SPIDER_AMBIENT;
            case KATSUYU -> SoundEvents.SLIME_SQUISH;
            case ENMA -> SoundEvents.ANVIL_HIT;
        };
    }

    private net.minecraft.world.level.Level level() { return this.beast.level(); }

    private Vec3 mouth() { return this.beast.position().add(0, this.beast.getBbHeight() * 0.67, 0); }
}
