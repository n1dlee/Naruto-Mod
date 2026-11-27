package com.sekwah.narutomod.entity.jutsuprojectile;

import com.sekwah.narutomod.entity.NarutoEntities;
import com.sekwah.narutomod.util.Faction;
import com.sekwah.narutomod.util.KirinTimeline;
import com.sekwah.narutomod.util.WaterDragonFlight;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.UUID;

public class KirinStrikeEntity extends Entity {
    private static final EntityDataAccessor<Integer> AGE =
            SynchedEntityData.defineId(KirinStrikeEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> CLOUD =
            SynchedEntityData.defineId(KirinStrikeEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> RADIUS =
            SynchedEntityData.defineId(KirinStrikeEntity.class, EntityDataSerializers.FLOAT);
    private static final double DIRECT_RADIUS = 1.35;

    @Nullable
    private UUID ownerId;
    private float directDamage = 34f;
    private float sideDamage = 10f;

    public KirinStrikeEntity(EntityType<KirinStrikeEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public static boolean mark(LivingEntity caster, LivingEntity target, Profile profile) {
        if (!(caster.level() instanceof ServerLevel level)) {
            return false;
        }
        Vec3 feet = target.position().add(0, 0.10, 0);
        var floor = clip(level, caster, feet, feet.add(0, -6.0, 0));
        Vec3 point = floor.getType() == HitResult.Type.BLOCK ? floor.getLocation().add(0, 0.045, 0) : feet;
        if (!openColumn(level, caster, point) || KirinTimeline.cloudHeight(point.y, level.getMaxBuildHeight()) < 3.0) {
            return false;
        }
        KirinStrikeEntity strike = new KirinStrikeEntity(NarutoEntities.KIRIN_STRIKE.get(), level);
        strike.ownerId = caster.getUUID();
        strike.directDamage = profile.direct();
        strike.sideDamage = profile.splash();
        strike.entityData.set(RADIUS, profile.radius());
        strike.entityData.set(CLOUD, (float) KirinTimeline.cloudHeight(point.y, level.getMaxBuildHeight()));
        strike.setPos(point);
        if (!level.addFreshEntity(strike)) {
            return false;
        }
        level.playSound(null, point.x, point.y, point.z, SoundEvents.LIGHTNING_BOLT_THUNDER,
                SoundSource.WEATHER, 0.85f, 0.51f);
        return true;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(AGE, 0);
        this.entityData.define(CLOUD, 34f);
        this.entityData.define(RADIUS, 6f);
    }

    public int age() {
        return this.entityData.get(AGE);
    }

    public float cloudHeight() {
        return this.entityData.get(CLOUD);
    }

    public float radius() {
        return this.entityData.get(RADIUS);
    }

    @Override
    public void tick() {
        super.tick();
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        int age = this.age() + 1;
        this.entityData.set(AGE, age);
        if (age >= KirinTimeline.CONTACT + KirinTimeline.RESIDUE) {
            this.discard();
            return;
        }
        if (KirinTimeline.checkSkyAt(age) && !openColumn(level, this, this.position())) {
            this.discard();
            return;
        }
        if (age == KirinTimeline.CONTACT) {
            this.strike(level);
        }
    }

    private void strike(ServerLevel level) {
        Vec3 center = this.position();
        Entity owner = this.ownerId == null ? null : level.getEntity(this.ownerId);
        var source = owner == null ? this.damageSources().magic() : this.damageSources().indirectMagic(this, owner);
        double radius = this.radius();
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, new AABB(center, center).inflate(radius),
                victim -> this.canStrike(owner, victim))) {
            Vec3 point = WaterDragonFlight.nearestPoint(target.getBoundingBox(), center);
            double distance = point.distanceToSqr(center);
            if (distance <= radius * radius && openColumn(level, this, target.position().add(0, 0.2, 0))
                    && (distance < 1.0e-8 || clip(level, this, center, point).getType() == HitResult.Type.MISS)) {
                target.hurt(source, distance <= DIRECT_RADIUS * DIRECT_RADIUS ? this.directDamage : this.sideDamage);
            }
        }
        level.playSound(null, center.x, center.y, center.z, SoundEvents.LIGHTNING_BOLT_THUNDER,
                SoundSource.WEATHER, 2.6f, 0.67f);
    }

    private boolean canStrike(@Nullable Entity owner, LivingEntity target) {
        if (!target.isAlive() || !target.isPickable() || target.isSpectator()
                || target.getUUID().equals(this.ownerId) || Faction.sameSide(owner, target)) {
            return false;
        }
        if (target instanceof Player victim) {
            return !victim.isCreative() && owner != null
                    && (!(owner instanceof Player player) || player.canHarmPlayer(victim));
        }
        return owner == null || !owner.isAlliedTo(target);
    }

    private static boolean openColumn(Level level, Entity context, Vec3 point) {
        return level.canSeeSky(BlockPos.containing(point))
                && clip(level, context, point, new Vec3(point.x, level.getMaxBuildHeight() - 0.01, point.z))
                .getType() == HitResult.Type.MISS;
    }

    private static HitResult clip(Level level, Entity context, Vec3 start, Vec3 end) {
        return level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, context));
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        return super.getBoundingBoxForCulling().expandTowards(0, this.cloudHeight(), 0).inflate(this.radius() + 4);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("KirinAge", this.age());
        tag.putFloat("KirinCloud", this.cloudHeight());
        tag.putFloat("KirinRadius", this.radius());
        tag.putFloat("KirinDirect", this.directDamage);
        tag.putFloat("KirinSplash", this.sideDamage);
        if (this.ownerId != null) {
            tag.putUUID("KirinOwner", this.ownerId);
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.entityData.set(AGE, tag.getInt("KirinAge"));
        this.entityData.set(CLOUD, tag.getFloat("KirinCloud"));
        this.entityData.set(RADIUS, tag.getFloat("KirinRadius"));
        this.directDamage = tag.getFloat("KirinDirect");
        this.sideDamage = tag.getFloat("KirinSplash");
        this.ownerId = tag.hasUUID("KirinOwner") ? tag.getUUID("KirinOwner") : null;
    }

    public record Profile(float direct, float splash, float radius) {
    }
}
