package com.sekwah.narutomod.entity.jutsuprojectile;

import com.sekwah.narutomod.damagetypes.NarutoDamageTypes;
import com.sekwah.narutomod.entity.NarutoEntities;
import com.sekwah.narutomod.util.Faction;
import com.sekwah.narutomod.util.PhoenixVolley;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.ForgeEventFactory;

import javax.annotation.Nullable;

public class FireballJutsuEntity extends Projectile {
    private static final EntityDataAccessor<Integer> FRAME =
            SynchedEntityData.defineId(FireballJutsuEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Direction> FACE =
            SynchedEntityData.defineId(FireballJutsuEntity.class, EntityDataSerializers.DIRECTION);
    private static final Phase[] PHASES = Phase.values();
    @Nullable
    private PhoenixVolley volley;
    private float power;

    public FireballJutsuEntity(EntityType<FireballJutsuEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public static void launch(Player owner, Vec3 heading, PhoenixVolley volley, float power, boolean gather) {
        if (!(owner.level() instanceof ServerLevel level) || !owner.isAlive() || owner.isSpectator()) {
            return;
        }
        var petal = new FireballJutsuEntity(NarutoEntities.FIREBALL_JUTSU.get(), level);
        petal.setOwner(owner);
        petal.volley = volley;
        petal.power = power;
        petal.setRot((float) Math.toDegrees(Math.atan2(heading.z, heading.x)) - 90,
                (float) -Math.toDegrees(Math.atan2(heading.y, heading.horizontalDistance())));
        petal.setPos(petal.mouth(owner));
        petal.frame(gather ? Phase.GATHER : Phase.FLY, 0);
        level.addFreshEntity(petal);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(FRAME, 0);
        this.entityData.define(FACE, Direction.UP);
    }

    public Phase phase() {
        return PHASES[this.entityData.get(FRAME) >> 6];
    }

    public int age() {
        return this.entityData.get(FRAME) & 63;
    }

    public Direction face() {
        return this.entityData.get(FACE);
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        return super.getBoundingBoxForCulling().inflate(1.7);
    }

    @Override
    public void tick() {
        super.tick();
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        if (this.volley == null || !(this.getOwner() instanceof Player owner) || !owner.isAlive() || owner.isSpectator()) {
            this.discard();
            return;
        }
        Phase phase = this.phase();
        int age = this.age() + 1;
        this.frame(phase, age);
        switch (phase) {
            case GATHER -> {
                this.setPos(this.mouth(owner));
                if (age >= PhoenixVolley.WINDUP) {
                    this.frame(Phase.FLY, 0);
                }
            }
            case FLY -> this.fly(level, owner, age);
            case BLOCK, VICTIM, STEAM, FADE -> {
                if (age >= PhoenixVolley.FADE_TICKS) {
                    this.discard();
                }
            }
        }
    }

    private Vec3 mouth(Player owner) {
        Vec3 origin = owner.getEyePosition().add(0, -0.17, 0);
        Vec3 ahead = origin.add(this.getLookAngle().scale(0.33));
        var collision = ProjectileVolume.clip(this, origin, ahead, ClipContext.Fluid.NONE);
        return collision.getType() == HitResult.Type.MISS ? ahead
                : collision.getLocation().subtract(this.getLookAngle().scale(0.025));
    }

    private void fly(ServerLevel level, Player owner, int age) {
        Vec3 from = this.position();
        Vec3 to = from.add(this.getLookAngle().scale(PhoenixVolley.speed(age)));
        if (age > PhoenixVolley.MAX_FLIGHT || !level.hasChunkAt(BlockPos.containing(to))) {
            this.frame(Phase.FADE, 0);
            return;
        }
        if (level.getFluidState(BlockPos.containing(from)).is(FluidTags.WATER)) {
            this.steam(level);
            return;
        }
        var wet = ProjectileVolume.clip(this, from, to, ClipContext.Fluid.WATER);
        boolean water = wet.getType() == HitResult.Type.BLOCK && level.getFluidState(wet.getBlockPos()).is(FluidTags.WATER);
        var impact = ProjectileVolume.first(this, level, from, water ? wet.getLocation() : to,
                PhoenixVolley.radius(age), target -> hittable(owner, target));
        if (impact != null && !ForgeEventFactory.onProjectileImpact(this, impact.result())) {
            this.setPos(impact.result().getLocation());
            this.entityData.set(FACE, impact.face());
            this.finish(level, owner, impact.target());
        } else if (water) {
            this.setPos(wet.getLocation());
            this.steam(level);
        } else {
            this.setPos(to);
        }
    }

    private boolean hittable(Player owner, LivingEntity target) {
        if (target == owner || !target.isAlive() || target.isSpectator() || !target.isPickable()
                || owner.isAlliedTo(target) || Faction.sameSide(owner, target)) {
            return false;
        }
        return !(target instanceof Player player) || (!player.isCreative() && owner.canHarmPlayer(player));
    }

    private void finish(ServerLevel level, Player owner, @Nullable LivingEntity victim) {
        if (victim != null && (this.volley == null || this.volley.hasHit(victim.getUUID())
                || !victim.hurt(NarutoDamageTypes.getDamageSource(level, NarutoDamageTypes.FIREBALL,
                this, owner, this.position()), this.power))) {
            this.frame(Phase.FADE, 0);
            return;
        }
        if (victim != null && this.volley != null) {
            this.volley.markHit(victim.getUUID());
            victim.setSecondsOnFire(3);
        }
        this.frame(victim == null ? Phase.BLOCK : Phase.VICTIM, 0);
        Vec3 n = Vec3.atLowerCornerOf(this.face().getNormal());
        Vec3 p = this.position().add(n.scale(0.08));
        level.sendParticles(ParticleTypes.SMOKE, p.x, p.y, p.z, 4, 0.08, 0.09, 0.08, 0.02);
        level.playSound(null, p.x, p.y, p.z, SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.22f, 1.52f);
    }

    private void steam(ServerLevel level) {
        this.frame(Phase.STEAM, 0);
        level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.FIRE_EXTINGUISH,
                SoundSource.PLAYERS, 0.26f, 1.32f);
    }

    private void frame(Phase phase, int age) {
        this.entityData.set(FRAME, phase.ordinal() << 6 | Math.min(age, 63));
    }

    public enum Phase {
        GATHER, FLY, BLOCK, VICTIM, STEAM, FADE
    }
}
