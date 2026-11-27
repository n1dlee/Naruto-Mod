package com.sekwah.narutomod.entity.jutsuprojectile;

import com.sekwah.narutomod.damagetypes.NarutoDamageTypes;
import com.sekwah.narutomod.entity.NarutoEntities;
import com.sekwah.narutomod.sounds.NarutoSounds;
import com.sekwah.narutomod.util.Faction;
import com.sekwah.narutomod.util.JutsuVfx;
import com.sekwah.narutomod.util.ShockArc;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.ForgeEventFactory;

public class ShockArcEntity extends Projectile {
    private static final EntityDataAccessor<Integer> AGE =
            SynchedEntityData.defineId(ShockArcEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<CompoundTag> ARC =
            SynchedEntityData.defineId(ShockArcEntity.class, EntityDataSerializers.COMPOUND_TAG);
    private float power;

    public ShockArcEntity(EntityType<ShockArcEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public static boolean cast(LivingEntity owner, Vec3 point, float power) {
        if (!(owner.level() instanceof ServerLevel level) || !owner.isAlive() || owner.isSpectator()) {
            return false;
        }
        var shock = new ShockArcEntity(NarutoEntities.SHOCK_ARC.get(), level);
        shock.setOwner(owner);
        shock.power = power;
        Vec3 start = shock.palm(owner);
        Vec3 heading = point.subtract(start).normalize();
        if (heading.lengthSqr() == 0) {
            return false;
        }
        shock.setRot((float) Math.toDegrees(Math.atan2(heading.z, heading.x)) - 90,
                (float) -Math.toDegrees(Math.atan2(heading.y, heading.horizontalDistance())));
        shock.setPos(start);
        return level.addFreshEntity(shock);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(AGE, 0);
        this.entityData.define(ARC, new CompoundTag());
    }

    public int age() {
        return this.entityData.get(AGE);
    }

    public Vec3 arc() {
        var tag = this.entityData.get(ARC);
        return new Vec3(tag.getDouble("X"), tag.getDouble("Y"), tag.getDouble("Z"));
    }

    public boolean struck() {
        return this.entityData.get(ARC).getBoolean("Hit");
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        return super.getBoundingBoxForCulling().expandTowards(this.arc()).inflate(0.5);
    }

    @Override
    public void tick() {
        super.tick();
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        int age = this.age() + 1;
        this.entityData.set(AGE, age);
        if (age <= ShockArc.PREPARE) {
            if (!(this.getOwner() instanceof LivingEntity owner) || !owner.isAlive() || owner.isSpectator()) {
                this.discard();
                return;
            }
            this.setPos(this.palm(owner));
            if (age == ShockArc.PREPARE) {
                this.release(level, owner);
            }
        } else if (age > ShockArc.PREPARE + ShockArc.FADE) {
            this.discard();
        }
    }

    private Vec3 palm(LivingEntity owner) {
        double sign = owner.getMainArm() == HumanoidArm.RIGHT ? 1 : -1;
        Vec3 shoulder = owner.position().add(0, owner.getBbHeight() * 0.72, 0);
        Vec3 raised;
        if (owner instanceof Player player) {
            raised = JutsuVfx.handPosition(player, sign, 1).add(owner.getLookAngle().scale(0.14));
        } else {
            Vec3 side = Vec3.directionFromRotation(0, owner.getYRot()).cross(new Vec3(0, 1, 0));
            raised = shoulder.subtract(side.scale(sign * 0.34)).add(owner.getLookAngle().scale(0.43));
        }
        var clip = ProjectileVolume.clip(this, shoulder, raised, ClipContext.Fluid.NONE);
        if (clip.getType() == HitResult.Type.BLOCK) {
            return clip.getLocation().subtract(raised.subtract(shoulder).normalize().scale(0.04));
        }
        return raised;
    }

    private void release(ServerLevel level, LivingEntity owner) {
        Vec3 from = this.position();
        Vec3 to = ShockArc.endpoint(from, this.getLookAngle());
        if (!level.hasChunkAt(BlockPos.containing(to)) || !level.getWorldBorder().isWithinBounds(BlockPos.containing(to))) {
            to = from.add(this.getLookAngle().scale(0.28));
        }
        boolean accepted = false;
        var impact = ProjectileVolume.first(this, level, from, to, ShockArc.RADIUS, target -> eligible(owner, target));
        if (impact != null) {
            to = impact.result().getLocation();
        }
        if (impact != null && !ForgeEventFactory.onProjectileImpact(this, impact.result())) {
            LivingEntity target = impact.target();
            if (target != null) {
                var source = NarutoDamageTypes.getDamageSource(level, NarutoDamageTypes.CHIDORI, this, owner, from);
                int stun = ShockArc.stunTicks(target.hurt(source, this.power), owner instanceof Player);
                accepted = stun > 0;
                if (accepted) {
                    target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, stun, 2, false, true));
                    level.sendParticles(ParticleTypes.ELECTRIC_SPARK, to.x, to.y, to.z, 6, 0.08, 0.1, 0.08, 0.04);
                }
            } else {
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, to.x, to.y, to.z, 3, 0.05, 0.05, 0.05, 0.03);
            }
        }
        Vec3 local = to.subtract(from);
        var tag = new CompoundTag();
        tag.putDouble("X", local.x);
        tag.putDouble("Y", local.y);
        tag.putDouble("Z", local.z);
        tag.putBoolean("Hit", accepted);
        this.entityData.set(ARC, tag);
        level.playSound(null, from.x, from.y, from.z, NarutoSounds.SHOCK_SNAP.get(),
                SoundSource.PLAYERS, accepted ? 0.69f : 0.54f, 1.05f);
    }

    private static boolean eligible(LivingEntity owner, LivingEntity target) {
        if (target == owner || !target.isAlive() || !target.isPickable() || target.isSpectator()
                || owner.isAlliedTo(target) || Faction.sameSide(owner, target)) {
            return false;
        }
        return !(target instanceof Player player)
                || (!player.isCreative() && (!(owner instanceof Player caster) || caster.canHarmPlayer(player)));
    }
}
