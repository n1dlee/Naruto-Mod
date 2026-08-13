package com.sekwah.narutomod.entity.jutsuprojectile;

import com.sekwah.narutomod.entity.NarutoEntities;
import com.sekwah.narutomod.entity.TailedBeastVariant;
import com.sekwah.narutomod.entity.projectile.AbstractNonGlowingHurtingProjectile;
import com.sekwah.narutomod.sounds.NarutoSounds;
import com.sekwah.narutomod.util.NarutoParticles;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Bijudama - the Tailed Beast Bomb. Chakra compressed into a sphere and thrown.
 *
 * Deliberately slow. The whole shape of the fight is seeing one form, hearing it, and having
 * a second or two to not be where it lands; a fast projectile would just be unavoidable
 * damage at these numbers.
 *
 * It leaves the terrain alone, like every other technique in this mod. A boss can spawn next
 * to someone's base and losing the base to a stray Bijudama is not the fight anyone wanted.
 */
public class TailedBeastBombEntity extends AbstractNonGlowingHurtingProjectile {

    private static final EntityDataAccessor<Byte> VARIANT =
            SynchedEntityData.defineId(TailedBeastBombEntity.class, EntityDataSerializers.BYTE);
    /** Blast radius in blocks; also what the renderer scales the orb by. */
    private static final EntityDataAccessor<Float> POWER =
            SynchedEntityData.defineId(TailedBeastBombEntity.class, EntityDataSerializers.FLOAT);

    private static final float BASE_DAMAGE = 26f;
    private static final int MAX_LIFETIME = 20 * 12;

    private int lifetime;

    public TailedBeastBombEntity(EntityType<TailedBeastBombEntity> entityType, Level level) {
        super(entityType, level);
    }

    public TailedBeastBombEntity(LivingEntity shooter, Vec3 aim, TailedBeastVariant variant, float power) {
        super(NarutoEntities.TAILED_BEAST_BOMB.get(), shooter.getX(),
                shooter.getY() + shooter.getBbHeight() * 0.72D, shooter.getZ(),
                aim.x, aim.y, aim.z, shooter.level());
        this.setOwner(shooter);
        this.setRot(shooter.getYRot(), shooter.getXRot());
        this.setVariant(variant);
        this.setPower(power);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(VARIANT, (byte) 0);
        this.entityData.define(POWER, 5.0f);
    }

    public TailedBeastVariant getVariant() {
        return TailedBeastVariant.byId(this.entityData.get(VARIANT));
    }

    public void setVariant(TailedBeastVariant variant) {
        this.entityData.set(VARIANT, (byte) variant.ordinal());
    }

    public float getPower() {
        return this.entityData.get(POWER);
    }

    public void setPower(float power) {
        this.entityData.set(POWER, Math.max(1.0f, power));
    }

    /** Slower than a fireball and it keeps its speed instead of coasting to a stop. */
    @Override
    protected float getInertia() {
        return 0.99F;
    }

    @Override
    protected boolean shouldBurn() {
        return false;
    }

    @Override
    protected ParticleOptions getTrailParticle() {
        org.joml.Vector3f colour = this.getVariant().getChakraColour();
        return new DustParticleOptions(colour, 2.2F);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            return;
        }
        if (++this.lifetime > MAX_LIFETIME) {
            this.detonate();
        }
    }

    /**
     * Batting a Bijudama does not send it back the way it came.
     *
     * The base class turns a hurting projectile around toward whoever struck it, which is
     * right for a ghast fireball and very wrong here: it would let a player with a sword
     * return the beast's strongest technique to it for free.
     */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    /** Never hits the beast that made it, or another beast. */
    @Override
    protected boolean canHitEntity(Entity entity) {
        Entity owner = this.getOwner();
        if (entity == owner) {
            return false;
        }
        if (owner != null && entity.getType() == owner.getType()) {
            return false;
        }
        return super.canHitEntity(entity);
    }

    @Override
    protected void onHit(HitResult hitResult) {
        super.onHit(hitResult);
        if (!this.level().isClientSide) {
            this.detonate();
        }
    }

    private void detonate() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            this.discard();
            return;
        }
        float radius = this.getPower();
        Vec3 centre = this.position();
        Entity owner = this.getOwner();

        List<LivingEntity> caught = serverLevel.getEntitiesOfClass(LivingEntity.class,
                new AABB(centre, centre).inflate(radius),
                victim -> victim != owner && victim.isAlive()
                        && !(owner != null && victim.getType() == owner.getType()));

        for (LivingEntity victim : caught) {
            // Falls off with distance so being at the edge of the blast is survivable and
            // being under it is not.
            double distance = victim.position().distanceTo(centre);
            float falloff = (float) Math.max(0.0, 1.0 - distance / radius);
            float damage = BASE_DAMAGE * (0.35f + 0.65f * falloff) * (radius / 5.0f);
            if (damage <= 0) {
                continue;
            }
            victim.hurt(owner instanceof LivingEntity living
                    ? this.damageSources().mobProjectile(this, living)
                    : this.damageSources().explosion(this, null), damage);
            Vec3 push = victim.position().subtract(centre).normalize().scale(1.4 * falloff);
            victim.setDeltaMovement(victim.getDeltaMovement().add(push.x, 0.55 * falloff, push.z));
            victim.hurtMarked = true;
        }

        org.joml.Vector3f colour = this.getVariant().getChakraColour();
        DustParticleOptions dust = new DustParticleOptions(colour, 3.0F);
        for (double r = radius * 0.35; r <= radius; r += radius * 0.32) {
            NarutoParticles.spawnRing(serverLevel, centre, r, (int) (r * 12), dust);
        }
        NarutoParticles.spawnBurst(serverLevel, centre, 140, radius * 0.6, dust);
        serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                centre.x, centre.y, centre.z, 3, radius * 0.3, radius * 0.3, radius * 0.3, 0.0);

        this.level().playSound(null, centre.x, centre.y, centre.z,
                NarutoSounds.BIJUDAMA.get(), SoundSource.HOSTILE, 6.0f, 0.8f);
        this.discard();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putByte("Variant", this.entityData.get(VARIANT));
        tag.putFloat("Power", this.getPower());
        tag.putInt("Lifetime", this.lifetime);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.entityData.set(VARIANT, tag.getByte("Variant"));
        this.setPower(tag.getFloat("Power"));
        this.lifetime = tag.getInt("Lifetime");
    }
}
