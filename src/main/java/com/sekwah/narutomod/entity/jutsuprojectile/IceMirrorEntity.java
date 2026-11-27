package com.sekwah.narutomod.entity.jutsuprojectile;

import com.sekwah.narutomod.entity.NarutoEntities;
import com.sekwah.narutomod.util.Faction;
import com.sekwah.narutomod.util.MirrorRing;
import com.sekwah.narutomod.util.NarutoParticles;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class IceMirrorEntity extends Entity {
    public static final double HEIGHT = 2.5;
    public static final float MAX_HEALTH = 12f;
    public static final int RING_SIZE = MirrorRing.SECTIONS;
    public static final double RING_RADIUS = MirrorRing.RADIUS;
    private static final int LIFESPAN = 400;
    private static final EntityDataAccessor<Float> HEALTH =
            SynchedEntityData.defineId(IceMirrorEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> FACING =
            SynchedEntityData.defineId(IceMirrorEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> RISE =
            SynchedEntityData.defineId(IceMirrorEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> SHATTER =
            SynchedEntityData.defineId(IceMirrorEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> REFLECTION =
            SynchedEntityData.defineId(IceMirrorEntity.class, EntityDataSerializers.INT);

    private Optional<UUID> owner = Optional.empty();
    private UUID ringId = UUID.randomUUID();
    private Vec3 center = Vec3.ZERO;
    private int age;
    private float boundFacing;
    private Map<UUID, Vec3> nearby = new HashMap<>();

    public IceMirrorEntity(EntityType<IceMirrorEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public static List<IceMirrorEntity> raiseRing(LivingEntity caster, Vec3 center) {
        if (!(caster.level() instanceof ServerLevel level)) {
            return List.of();
        }
        UUID ring = UUID.randomUUID();
        List<IceMirrorEntity> panes = new ArrayList<>();
        for (int slot = 0; slot < RING_SIZE; slot++) {
            Vec3 outward = MirrorRing.outward(slot);
            Vec3 point = center.add(outward.scale(RING_RADIUS));
            Vec3 footing = safeFloor(level, caster, point, IceMirrorEntity.HEIGHT);
            if (footing == null) {
                continue;
            }
            var pane = new IceMirrorEntity(NarutoEntities.ICE_MIRROR.get(), level);
            pane.ringId = ring;
            pane.center = center;
            pane.owner = Optional.of(caster.getUUID());
            pane.entityData.set(FACING, (float) Math.toDegrees(Math.atan2(outward.z, outward.x)) + 90f);
            pane.entityData.set(RISE, -MirrorRing.delay(slot));
            pane.setPos(footing);
            if (level.addFreshEntity(pane)) {
                panes.add(pane);
            }
        }
        return panes;
    }

    @Nullable
    static Vec3 safeFloor(Level level, LivingEntity caster, Vec3 point, double clearance) {
        Vec3 start = point.add(0, 0.8, 0);
        var floor = level.clip(new ClipContext(start, point.add(0, -1.50, 0),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster));
        if (floor.getType() != HitResult.Type.BLOCK || floor.getDirection() != net.minecraft.core.Direction.UP) {
            return null;
        }
        Vec3 feet = new Vec3(point.x, floor.getLocation().y + 0.035, point.z);
        AABB box = new AABB(feet.x - 0.34, feet.y, feet.z - 0.34, feet.x + 0.34, feet.y + clearance, feet.z + 0.34);
        return level.noCollision(caster, box) && level.getFluidState(net.minecraft.core.BlockPos.containing(feet)).isEmpty()
                ? feet : null;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(HEALTH, MAX_HEALTH);
        this.entityData.define(FACING, 0f);
        this.entityData.define(RISE, 0);
        this.entityData.define(SHATTER, 0);
        this.entityData.define(REFLECTION, 0);
    }

    public float getMirrorHealth() {
        return this.entityData.get(HEALTH);
    }

    public float getFacing() {
        return this.entityData.get(FACING);
    }

    public int riseTicks() {
        return this.entityData.get(RISE);
    }

    public int shatterTicks() {
        return this.entityData.get(SHATTER);
    }

    public int reflectionTicks() {
        return this.entityData.get(REFLECTION);
    }

    public Optional<UUID> getOwnerUUID() {
        return this.owner;
    }

    public UUID ringId() {
        return this.ringId;
    }

    public Vec3 ringCenter() {
        return this.center;
    }

    public Vec3 inward() {
        return Vec3.directionFromRotation(0, this.getFacing());
    }

    public boolean available() {
        return !this.isRemoved() && this.shatterTicks() == 0 && this.riseTicks() >= MirrorRing.RISE_TICKS;
    }

    public void flash() {
        if (this.available()) {
            this.entityData.set(REFLECTION, 7);
        }
    }

    @Override
    protected AABB makeBoundingBox() {
        return this.paneBounds();
    }

    private AABB paneBounds() {
        Vec3 normal = Vec3.directionFromRotation(0, this.boundFacing);
        double x = Math.abs(normal.z) * MirrorRing.WIDTH * 0.5 + 0.11;
        double z = Math.abs(normal.x) * MirrorRing.WIDTH * 0.5 + 0.11;
        return new AABB(this.getX() - x, this.getY(), this.getZ() - z,
                this.getX() + x, this.getY() + HEIGHT, this.getZ() + z);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (FACING.equals(key)) {
            this.boundFacing = this.getFacing();
            this.setBoundingBox(this.paneBounds());
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        if (this.shatterTicks() > 0) {
            if (this.shatterTicks() >= 6) {
                this.discard();
            } else {
                this.entityData.set(SHATTER, this.shatterTicks() + 1);
            }
            return;
        }
        if (++this.age >= LIFESPAN) {
            this.shatter();
            return;
        }
        int rise = this.riseTicks();
        if (rise < MirrorRing.RISE_TICKS) {
            this.entityData.set(RISE, rise + 1);
            if (rise + 1 == MirrorRing.RISE_TICKS) {
                level.playSound(null, this.blockPosition(), SoundEvents.GLASS_PLACE, SoundSource.HOSTILE, 0.48f, 1.15f);
            }
        }
        if (this.reflectionTicks() > 0) {
            this.entityData.set(REFLECTION, this.reflectionTicks() - 1);
        }
        if (this.available()) {
            this.blockCrossing(level);
        }
    }

    private void blockCrossing(ServerLevel level) {
        Vec3 outward = this.inward().reverse();
        Map<UUID, Vec3> observed = new HashMap<>();
        Entity caster = this.owner.map(level::getEntity).orElse(null);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class,
                this.getBoundingBox().inflate(2.8, 0.2, 2.8),
                actor -> this.blocks(caster, actor))) {
            Vec3 current = entity.position();
            Vec3 before = this.nearby.getOrDefault(entity.getUUID(), new Vec3(entity.xOld, entity.yOld, entity.zOld));
            Vec3 hit = MirrorRing.clipOutward(before, current, this.position(), outward, entity.getBbWidth() * 0.44, HEIGHT);
            if (hit != null && level.noCollision(entity, entity.getBoundingBox().move(hit.subtract(current)))) {
                move(entity, hit);
                Vec3 velocity = entity.getDeltaMovement();
                entity.setDeltaMovement(velocity.subtract(outward.scale(Math.max(0, velocity.dot(outward)))));
                entity.hurtMarked = true;
                current = hit;
            }
            observed.put(entity.getUUID(), current);
        }
        this.nearby = observed;
    }

    private boolean blocks(@Nullable Entity caster, LivingEntity actor) {
        if (!actor.isAlive() || actor.isSpectator() || this.owner.filter(actor.getUUID()::equals).isPresent()
                || Faction.sameSide(caster, actor)) {
            return false;
        }
        if (actor instanceof Player player) {
            return !player.isCreative() && (caster != null || this.owner.isEmpty())
                    && (!(caster instanceof Player source) || source.canHarmPlayer(player));
        }
        return true;
    }

    public static void move(LivingEntity entity, Vec3 point) {
        if (entity instanceof ServerPlayer player) {
            player.connection.teleport(point.x, point.y, point.z, player.getYRot(), player.getXRot());
        } else {
            entity.teleportTo(point.x, point.y, point.z);
        }
        entity.resetFallDistance();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!(this.level() instanceof ServerLevel level) || !this.available() || amount <= 0
                || (source.getEntity() != null && this.owner.filter(source.getEntity().getUUID()::equals).isPresent())) {
            return false;
        }
        float health = Math.max(0, this.getMirrorHealth() - amount);
        this.entityData.set(HEALTH, health);
        if (health == 0) {
            this.shatter();
        } else {
            level.playSound(null, this.blockPosition(), SoundEvents.GLASS_HIT, SoundSource.HOSTILE, 0.9f, 1.12f);
        }
        return true;
    }

    public void shatter() {
        if (this.shatterTicks() != 0 || !(this.level() instanceof ServerLevel level)) {
            return;
        }
        this.entityData.set(SHATTER, 1);
        NarutoParticles.spawnBurst(level, this.position().add(0, 1.0, 0), 18, 0.9, NarutoParticles.ICE_PALE);
        level.playSound(null, this.blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 1.12f, 0.94f);
    }

    @Override
    public boolean isPickable() {
        return this.available();
    }

    @Override
    public boolean isAttackable() {
        return this.available();
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.age = tag.getInt("Age");
        this.entityData.set(HEALTH, tag.getFloat("MirrorHealth"));
        this.entityData.set(FACING, tag.getFloat("Facing"));
        this.entityData.set(RISE, tag.getInt("MirrorRise"));
        this.entityData.set(SHATTER, tag.getInt("MirrorShatter"));
        this.owner = tag.hasUUID("OwnerUUID") ? Optional.of(tag.getUUID("OwnerUUID")) : Optional.empty();
        if (!tag.hasUUID("MirrorRing")) {
            this.age = LIFESPAN;
            return;
        }
        this.ringId = tag.getUUID("MirrorRing");
        this.center = new Vec3(tag.getDouble("RingX"), tag.getDouble("RingY"), tag.getDouble("RingZ"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Age", this.age);
        tag.putFloat("MirrorHealth", this.getMirrorHealth());
        tag.putFloat("Facing", this.getFacing());
        tag.putInt("MirrorRise", this.riseTicks());
        tag.putInt("MirrorShatter", this.shatterTicks());
        this.owner.ifPresent(uuid -> tag.putUUID("OwnerUUID", uuid));
        tag.putUUID("MirrorRing", this.ringId);
        tag.putDouble("RingX", this.center.x);
        tag.putDouble("RingY", this.center.y);
        tag.putDouble("RingZ", this.center.z);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
