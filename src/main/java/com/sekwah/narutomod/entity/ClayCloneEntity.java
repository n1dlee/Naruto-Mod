package com.sekwah.narutomod.entity;

import com.sekwah.narutomod.config.NarutoConfig;
import com.sekwah.narutomod.sounds.NarutoSounds;
import com.sekwah.narutomod.util.ClayCloneState;
import com.sekwah.narutomod.util.ClayCloneState.Phase;
import com.sekwah.narutomod.util.JutsuAim;
import com.sekwah.narutomod.util.KamuiStep;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.lang.ref.WeakReference;

public final class ClayCloneEntity extends ShadowCloneEntity {
    private static final EntityDataAccessor<Integer> FRAME = SynchedEntityData.defineId(ClayCloneEntity.class, EntityDataSerializers.INT);
    private static final Phase[] PHASES = Phase.values();
    private static final int SHIFT = 7;
    private static final int MASK = (1 << SHIFT) - 1;
    private WeakReference<LivingEntity> quarry = new WeakReference<>(null);

    public ClayCloneEntity(EntityType<ClayCloneEntity> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
    }

    public static boolean launch(Player owner) {
        if (!(owner.level() instanceof ServerLevel level) || !owner.isAlive() || owner.isSpectator() || owner.noPhysics) {
            return false;
        }
        var target = JutsuAim.select(owner, 16, 24, actor -> level.mayInteract(owner, actor.blockPosition()));
        if (target == null) {
            return false;
        }
        var clone = new ClayCloneEntity(NarutoEntities.CLAY_CLONE.get(), level);
        if (!locate(level, owner, clone)) {
            return false;
        }
        clone.setOwner(owner);
        clone.quarry = new WeakReference<>(target);
        clone.setYRot(owner.getYRot());
        clone.setYBodyRot(owner.getYRot());
        clone.setYHeadRot(owner.getYHeadRot());
        if (level.addFreshEntity(clone)) {
            clone.tone(level, NarutoSounds.CLAY_MOLD.get(), 0.72f, 0.83f);
            clone.flecks(level, 12, 0.23);
            return true;
        }
        return false;
    }

    private static boolean locate(ServerLevel level, Player owner, ClayCloneEntity clone) {
        Vec3 facing = KamuiStep.facing(owner.getYRot());
        Vec3 center = owner.position().add(facing.scale(1.48));
        Vec3 side = new Vec3(facing.z, 0, -facing.x);
        for (double offset : new double[]{0, 0.7, -0.7}) {
            Vec3 at = center.add(side.scale(offset));
            clone.setPos(at);
            var foot = clone.blockPosition();
            var floor = foot.below();
            var eye = owner.getEyePosition();
            var path = new ClipContext(eye, at.add(0, 1.25, 0), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, owner);
            AABB bounds = clone.getBoundingBox();
            if (level.hasChunksAt(BlockPos.containing(bounds.minX, bounds.minY, bounds.minZ),
                    BlockPos.containing(bounds.maxX, bounds.maxY, bounds.maxZ)) && level.getWorldBorder().isWithinBounds(foot)
                    && level.mayInteract(owner, foot) && level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP)
                    && level.getBlockState(foot).getFluidState().isEmpty() && level.noCollision(clone, bounds)
                    && level.clip(path).getType() == HitResult.Type.MISS) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(FRAME, 0);
    }

    @Override
    protected void registerGoals() {
    }

    public Phase phase() { return PHASES[this.entityData.get(FRAME) >> SHIFT]; }

    public int stageAge() { return this.entityData.get(FRAME) & MASK; }

    public @Nullable Player caster() {
        return this.getOwnerUUID().map(this.level()::getPlayerByUUID).orElse(null);
    }

    @Override
    public void tick() {
        super.tick();
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        Phase phase = this.phase();
        int age = this.stageAge() + 1;
        if (phase == Phase.BURST || phase == Phase.FIZZLE) {
            if (age >= ClayCloneState.SHARDS) {
                this.discard();
            } else {
                stage(phase, age);
            }
            return;
        }
        var owner = caster();
        var target = this.quarry.get();
        if (!present(level, owner, target)) {
            collapse(level, Phase.FIZZLE);
            return;
        }
        advance(level, owner, target, phase, age);
    }

    private boolean present(ServerLevel level, @Nullable Player owner, @Nullable LivingEntity target) {
        return this.isAlive() && owner != null && owner.isAlive() && !owner.isSpectator() && !owner.noPhysics
                && owner.level() == level && owner.distanceToSqr(this) < 42 * 42
                && target != null && JutsuAim.attackable(owner, target) && !this.isInWaterOrBubble()
                && level.mayInteract(owner, this.blockPosition());
    }

    private void advance(ServerLevel level, Player owner, LivingEntity target, Phase phase, int age) {
        stage(phase, age);
        if (phase == Phase.FORM) {
            if (age >= ClayCloneState.FORM) {
                stage(Phase.PURSUE, 0);
            }
        } else if (phase == Phase.PURSUE) {
            pursue(level, target, age);
        } else if (phase == Phase.ARMED) {
            double distance = this.distanceToSqr(target);
            Phase next = ClayCloneState.armed(age, distance, this.hasLineOfSight(target));
            if (next == Phase.BURST) {
                detonate(level, owner);
            } else if (next == Phase.FIZZLE) {
                collapse(level, next);
            }
        }
    }

    private void pursue(ServerLevel level, LivingEntity target, int age) {
        double distance = this.distanceToSqr(target);
        Phase next = ClayCloneState.pursue(age, distance, this.hasLineOfSight(target));
        if (next == Phase.FIZZLE) {
            collapse(level, next);
        } else if (next == Phase.ARMED) {
            this.getNavigation().stop();
            Vec3 movement = this.getDeltaMovement();
            this.setDeltaMovement(0, movement.y, 0);
            stage(next, 0);
            tone(level, NarutoSounds.CLAY_ARM.get(), 0.85f, 0.76f);
        } else if (age % 5 == 1) {
            this.getLookControl().setLookAt(target, 45, 30);
            this.getNavigation().moveTo(target, 1.23);
        }
    }

    private void detonate(ServerLevel level, Player owner) {
        collapse(level, Phase.BURST);
        Vec3 at = this.position().add(0, this.getBbHeight() * 0.49, 0);
        level.explode(this, level.damageSources().explosion(this, owner), null, at, 3.65f, false,
                NarutoConfig.paperbombBlockDamage ? Level.ExplosionInteraction.TNT : Level.ExplosionInteraction.NONE);
        tone(level, NarutoSounds.CLAY_BURST.get(), 1.12f, 0.71f);
    }

    private void collapse(ServerLevel level, Phase end) {
        if (this.phase() == Phase.FIZZLE || this.phase() == Phase.BURST) {
            return;
        }
        this.getNavigation().stop();
        this.setDeltaMovement(Vec3.ZERO);
        this.setNoAi(true);
        this.setInvulnerable(true);
        stage(end, 0);
        flecks(level, end == Phase.BURST ? 22 : 10, end == Phase.BURST ? 0.55 : 0.31);
        if (end == Phase.FIZZLE) {
            tone(level, NarutoSounds.CLAY_MOLD.get(), 0.59f, 0.62f);
        }
    }

    @Override
    public void die(DamageSource source) {
        if (this.level() instanceof ServerLevel level) {
            collapse(level, Phase.FIZZLE);
        }
        super.die(source);
    }

    @Override
    public void thunderHit(ServerLevel level, LightningBolt bolt) { collapse(level, Phase.FIZZLE); }

    private void stage(Phase phase, int age) { this.entityData.set(FRAME, phase.ordinal() << SHIFT | age); }

    private void tone(ServerLevel level, SoundEvent sound, float volume, float pitch) {
        level.playSound(null, this.getX(), this.getY() + 0.65, this.getZ(), sound, SoundSource.PLAYERS, volume, pitch);
    }

    private void flecks(ServerLevel level, int count, double width) {
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.WHITE_TERRACOTTA.defaultBlockState()),
                this.getX(), this.getY() + 0.81, this.getZ(), count, width, 0.47, width, 0.044);
    }
}
