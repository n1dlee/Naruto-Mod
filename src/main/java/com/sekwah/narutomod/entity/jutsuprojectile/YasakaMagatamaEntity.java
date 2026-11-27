package com.sekwah.narutomod.entity.jutsuprojectile;

import com.sekwah.narutomod.entity.NarutoEntities;
import com.sekwah.narutomod.events.FinalChakraEffects;
import com.sekwah.narutomod.network.s2c.FinalChakraPacket.Phase;
import com.sekwah.narutomod.network.s2c.FinalChakraPacket.Shape;
import com.sekwah.narutomod.sounds.NarutoSounds;
import com.sekwah.narutomod.util.FinalChakraGeometry;
import com.sekwah.narutomod.util.MagatamaCluster;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public class YasakaMagatamaEntity extends FinalChakraProjectile {
    private static final EntityDataAccessor<Float> SCALE = SynchedEntityData.defineId(YasakaMagatamaEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> TINT = SynchedEntityData.defineId(YasakaMagatamaEntity.class, EntityDataSerializers.INT);
    private float damage = 20;
    private double blastRadius = 4;
    private UUID volley = UUID.randomUUID();

    public YasakaMagatamaEntity(EntityType<YasakaMagatamaEntity> type, Level level) {
        super(type, level);
    }

    public YasakaMagatamaEntity(LivingEntity owner, Vec3 from, Vec3 aim, float scale, int tint) {
        this(NarutoEntities.YASAKA_MAGATAMA.get(), owner.level());
        this.fireFrom(owner, from, aim, 1.4);
        this.entityData.set(SCALE, scale);
        this.entityData.set(TINT, tint);
    }

    public YasakaMagatamaEntity profile(UUID group, float amount, double radius) {
        this.volley = group;
        this.damage = amount;
        this.blastRadius = radius;
        return this;
    }

    public float getScale() {
        return this.entityData.get(SCALE);
    }

    public int getTint() {
        return this.entityData.get(TINT);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(SCALE, 1.5f);
        this.entityData.define(TINT, 0x8C40D9);
    }

    @Override
    protected double contactRadius() {
        return this.getScale() * 0.22;
    }

    @Override
    protected int maxLifetime() {
        return 160;
    }

    @Override
    protected void explode(ServerLevel level, Vec3 point) {
        var owner = this.getOwner();
        if (owner != null) {
            applyBlast(level, point);
        }
        var shape = new Shape(point, this.getDeltaMovement().normalize(), (float) blastRadius, this.getTint());
        FinalChakraEffects.emit(this, Phase.YASAKA_HIT, shape);
        FinalChakraEffects.sound(this, shape, NarutoSounds.MAGATAMA_HIT.get(), 1.1f, 0.94f);
    }

    private void applyBlast(ServerLevel level, Vec3 point) {
        var cluster = MagatamaCluster.of(level);
        for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, new AABB(point, point).inflate(blastRadius),
                this::canCollideWith)) {
            Vec3 contact = FinalChakraGeometry.nearest(victim.getBoundingBox(), point);
            if (contact.distanceTo(point) > blastRadius || !FinalChakraGeometry.clear(level, this, point, contact)
                    || !cluster.claim(volley, victim.getUUID(), level.getGameTime(), expiry() + 12)) {
                continue;
            }
            var owner = this.getOwner();
            if (owner instanceof LivingEntity attacker && victim.hurt(this.damageSources().mobProjectile(this, attacker), damage)) {
                Vec3 push = victim.position().subtract(point).multiply(1, 0, 1).normalize();
                victim.knockback(0.78, -push.x, -push.z);
                victim.hurtMarked = true;
            }
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("Volley")) {
            this.volley = tag.getUUID("Volley");
        }
        if (tag.contains("SealScale", Tag.TAG_FLOAT)) {
            this.entityData.set(SCALE, sane(tag.getFloat("SealScale"), 1.5f, 0.5f, 6));
            this.entityData.set(TINT, tag.getInt("SealTint") & 0xFFFFFF);
            this.damage = sane(tag.getFloat("SealDamage"), 20, 0, 1024);
            this.blastRadius = sane(tag.getFloat("SealRadius"), 4, 1, 9);
        }
    }

    private static float sane(float value, float fallback, float min, float max) {
        return Float.isFinite(value) ? Math.max(min, Math.min(value, max)) : fallback;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putUUID("Volley", volley);
        tag.putFloat("SealScale", this.getScale());
        tag.putInt("SealTint", this.getTint());
        tag.putFloat("SealDamage", damage);
        tag.putFloat("SealRadius", (float) blastRadius);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 96 * 96;
    }
}
