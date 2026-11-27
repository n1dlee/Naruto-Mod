package com.sekwah.narutomod.entity.jutsuprojectile;

import com.sekwah.narutomod.damagetypes.NarutoDamageTypes;
import com.sekwah.narutomod.entity.NarutoEntities;
import com.sekwah.narutomod.entity.projectile.AbstractNonGlowingHurtingProjectile;
import com.sekwah.narutomod.sounds.NarutoSounds;
import com.sekwah.narutomod.util.NarutoParticles;
import com.sekwah.narutomod.util.ProjectileAxis;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class WaterBulletJutsuEntity extends AbstractNonGlowingHurtingProjectile {

    public int lifeSpan = 30;

    private static final double ACCELERATION = 0.52;
    private float damageMultiplier = 1.0F;

    public WaterBulletJutsuEntity(EntityType<WaterBulletJutsuEntity> entityConstructor, Level level) {
        super(entityConstructor, level);
    }

    public WaterBulletJutsuEntity(EntityType<? extends AbstractNonGlowingHurtingProjectile> entityType, double posX, double posY, double posZ, double velX, double velY, double velZ, Level p_36824_) {
        super(entityType, p_36824_);
        this.moveTo(posX, posY, posZ, this.getYRot(), this.getXRot());
        this.reapplyPosition();
        this.launchAlong(new Vec3(velX, velY, velZ));
    }

    public WaterBulletJutsuEntity(LivingEntity player, double xVel, double yVel, double zVel) {
        this(NarutoEntities.WATER_BULLET_JUTSU.get(), player.getX(), player.getEyeY() - 0.15f, player.getZ(),  xVel, yVel, zVel, player.level());
        this.setOwner(player);
    }

    private void launchAlong(Vec3 direction) {
        Vec3 axis = ProjectileAxis.direction(direction, this.getLookAngle());
        Vec3 motion = axis.scale(ACCELERATION);
        this.xPower = motion.x;
        this.yPower = motion.y;
        this.zPower = motion.z;
        this.setDeltaMovement(motion);
        var angles = ProjectileAxis.angles(axis);
        this.setRot(angles.yaw(), angles.pitch());
        this.yRotO = this.getYRot();
        this.xRotO = this.getXRot();
    }

    public Vec3 flightHeading() {
        return ProjectileAxis.direction(this.getDeltaMovement(), new Vec3(this.xPower, this.yPower, this.zPower));
    }

    public void setDamageMultiplier(float damageMultiplier) {
        this.damageMultiplier = Math.max(0.0F, damageMultiplier);
    }

    @Override
    public void tick() {
        super.tick();

        if (this.tickCount % 2 == 0) {
            this.level().addParticle(NarutoParticles.WATER_BLUE, this.getX(), this.getY(), this.getZ(), 0.0D, 0.0D, 0.0D);
        }

        if (lifeSpan-- <= 0) {

            if(this.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.SPLASH,
                        this.getX(),
                        this.getY(),
                        this.getZ(),
                        100,
                        3, 3, 3, 1);
            }
            this.playSound(NarutoSounds.WATER_BULLET_SPLASH.get(), 3f, 1.0f);
            this.discard();
        }
        if (this.isInWater()) {
            Vec3 vec3 = this.getDeltaMovement();
            this.setDeltaMovement(vec3.add(this.xPower, this.yPower, this.zPower));
        }
    }

    protected void onHitEntity(EntityHitResult p_37216_) {
        super.onHitEntity(p_37216_);
        if (!this.level().isClientSide) {
            Entity entity = p_37216_.getEntity();
            Entity entity1 = this.getOwner();
            entity.hurt(NarutoDamageTypes.getDamageSource(this.level(), NarutoDamageTypes.WATER_BULLET, this, entity1), 7.0F * this.damageMultiplier);
            if (entity1 instanceof LivingEntity) {
                this.doEnchantDamageEffects((LivingEntity)entity1, entity);
            }

        }
    }

    @Override
    protected void onHit(HitResult hitResult) {
        super.onHit(hitResult);

        if(this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.SPLASH,
                    this.getX(),
                    this.getY(),
                    this.getZ(),
                    100,
                    this.getBbWidth(), this.getBbWidth(), this.getBbHeight(), 1);

            serverLevel.sendParticles(ParticleTypes.SPLASH,
                    this.getX(),
                    this.getY(),
                    this.getZ(),
                    100,
                    3, 3, 3, 1);
        }

        if (!this.level().isClientSide) {

            int splashRadius = 8;
            this.level().getEntities(this, this.getBoundingBox().inflate(splashRadius, splashRadius, splashRadius)).forEach(entity -> {
                if(entity instanceof LivingEntity) {
                    double distance = this.position().distanceToSqr(entity.position());
                    // Remember increasing the division reduces the falloff (I keep accidentally moving it the wrong way)
                    if(entity != this.getOwner()) {
                        float damage = (float) (5f - (distance / 4f)) * this.damageMultiplier;
                        if(damage > 0) {
                            Entity entity1 = this.getOwner();
                            entity.hurt(NarutoDamageTypes.getDamageSource(this.level(), NarutoDamageTypes.WATER_BULLET, this, entity1), damage);
                            if (entity1 instanceof LivingEntity) {
                                this.doEnchantDamageEffects((LivingEntity)entity1, entity);
                            }
                        }
                    }
                    entity.clearFire();
                }
            });

            boolean flag = net.minecraftforge.event.ForgeEventFactory.getMobGriefingEvent(this.level(), this.getOwner());

            if(flag) {
                int extinguish = 4;
                for (int x = (int) this.getX() - extinguish; x < (int) this.getX() + extinguish - 1; x++) {
                    for (int y = (int) this.getY() - extinguish + 1; y < (int) this.getY() + extinguish; y++) {
                        for (int z = (int) this.getZ() - extinguish + 1; z < (int) this.getZ() + extinguish; z++) {
                            BlockPos blockPos = new BlockPos(x, y, z);
                            if (this.level().getBlockState(blockPos).getBlock() == Blocks.FIRE) {
                                this.level().setBlockAndUpdate(blockPos, Blocks.AIR.defaultBlockState());
                            }
                        }
                    }
                }
            }

            this.playSound(NarutoSounds.WATER_BULLET_SPLASH.get(), 3f, 1.0f);

            this.discard();
        }

    }

    @Override
    public boolean hurt(DamageSource p_36910_, float p_36911_) {
        return false;
    }

    @Override
    protected ParticleOptions getTrailParticle() {
        return ParticleTypes.SPLASH;
    }

    @Override
    protected double trailHeight() {
        return 0;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    protected boolean shouldBurn() {
        return false;
    }

    @Override
    public void recreateFromPacket(ClientboundAddEntityPacket packet) {
        super.recreateFromPacket(packet);
        this.launchAlong(new Vec3(packet.getXa(), packet.getYa(), packet.getZa()));
    }
}
