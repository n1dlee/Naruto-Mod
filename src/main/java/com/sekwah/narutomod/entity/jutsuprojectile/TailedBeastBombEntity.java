package com.sekwah.narutomod.entity.jutsuprojectile;

import com.sekwah.narutomod.entity.NarutoEntities;
import com.sekwah.narutomod.entity.TailedBeastEntity;
import com.sekwah.narutomod.entity.TailedBeastVariant;
import com.sekwah.narutomod.events.FinalChakraEffects;
import com.sekwah.narutomod.network.s2c.FinalChakraPacket.Phase;
import com.sekwah.narutomod.network.s2c.FinalChakraPacket.Shape;
import com.sekwah.narutomod.sounds.NarutoSounds;
import com.sekwah.narutomod.util.BijudamaProfile;
import com.sekwah.narutomod.util.FinalChakraGeometry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class TailedBeastBombEntity extends FinalChakraProjectile {
    private static final EntityDataAccessor<Byte> VARIANT = SynchedEntityData.defineId(TailedBeastBombEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Float> POWER = SynchedEntityData.defineId(TailedBeastBombEntity.class, EntityDataSerializers.FLOAT);

    public TailedBeastBombEntity(EntityType<TailedBeastBombEntity> type, Level level) {
        super(type, level);
    }

    public TailedBeastBombEntity(LivingEntity owner, Vec3 mouth, Vec3 aim, TailedBeastVariant variant, float radius) {
        this(NarutoEntities.TAILED_BEAST_BOMB.get(), owner.level());
        fireFrom(owner, mouth, aim, 0.84 + variant.getTails() * 0.026);
        this.entityData.set(VARIANT, (byte) variant.ordinal());
        this.entityData.set(POWER, BijudamaProfile.limitRadius(radius));
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(VARIANT, (byte) 0);
        this.entityData.define(POWER, 5f);
    }

    public TailedBeastVariant getVariant() {
        return TailedBeastVariant.byId(this.entityData.get(VARIANT));
    }

    public float getPower() {
        return this.entityData.get(POWER);
    }

    @Override
    protected double contactRadius() {
        return BijudamaProfile.core(this.getPower(), 1);
    }

    @Override
    protected int maxLifetime() {
        return 240;
    }

    @Override
    protected boolean explodeAtTimeout() {
        return true;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    protected boolean canCollideWith(LivingEntity victim) {
        return !(victim instanceof TailedBeastEntity) && super.canCollideWith(victim);
    }

    @Override
    protected void explode(ServerLevel level, Vec3 point) {
        float radius = this.getPower();
        if (this.getOwner() instanceof LivingEntity owner) {
            applyBlast(level, point, radius, owner);
        }
        var tint = this.getVariant().getChakraColour();
        var shape = new Shape(point, this.getDeltaMovement().normalize(), radius,
                FinalChakraEffects.rgb(tint.x(), tint.y(), tint.z()));
        FinalChakraEffects.emit(this, Phase.BIJU_HIT, shape);
        FinalChakraEffects.sound(this, shape, NarutoSounds.BIJU_COLLAPSE.get(), 4.4f, 0.90f);
    }

    private void applyBlast(ServerLevel level, Vec3 point, float radius, LivingEntity owner) {
        for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, new AABB(point, point).inflate(radius),
                this::canCollideWith)) {
            double distance = FinalChakraGeometry.sphericalDistance(point, victim.getBoundingBox());
            float damage = BijudamaProfile.damage(this.getVariant().getTails(), radius, distance);
            if (damage <= 0 || !FinalChakraGeometry.clear(level, this, point,
                    FinalChakraGeometry.nearest(victim.getBoundingBox(), point))) {
                continue;
            }
            if (victim.hurt(this.damageSources().mobProjectile(this, owner), damage)) {
                Vec3 away = victim.position().subtract(point).multiply(1, 0, 1).normalize();
                victim.knockback(1.8 * (1 - distance / radius), -away.x, -away.z);
                victim.hurtMarked = true;
            }
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putByte("Variant", this.entityData.get(VARIANT));
        tag.putFloat("Power", this.getPower());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.entityData.set(VARIANT, tag.getByte("Variant"));
        this.entityData.set(POWER, BijudamaProfile.limitRadius(tag.getFloat("Power")));
    }
}
