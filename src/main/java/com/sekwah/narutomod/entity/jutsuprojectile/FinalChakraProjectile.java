package com.sekwah.narutomod.entity.jutsuprojectile;

import com.sekwah.narutomod.util.FinalChakraGeometry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.network.NetworkHooks;

abstract class FinalChakraProjectile extends Projectile {
    private long expiry = -1;
    private boolean resolved;

    protected FinalChakraProjectile(EntityType<? extends FinalChakraProjectile> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    protected final void fireFrom(LivingEntity owner, Vec3 from, Vec3 aim, double speed) {
        this.setOwner(owner);
        this.setPos(from.x, from.y, from.z);
        this.setDeltaMovement(aim.normalize().scale(speed));
        this.expiry = owner.level().getGameTime() + maxLifetime();
    }

    @Override
    public final void tick() {
        super.tick();
        if (this.isRemoved()) {
            return;
        }
        Vec3 from = this.position();
        Vec3 to = from.add(this.getDeltaMovement());
        if (this.level() instanceof ServerLevel level) {
            if (expiry < 0) {
                expiry = level.getGameTime() + maxLifetime();
            }
            if (level.getGameTime() >= expiry) {
                if (explodeAtTimeout()) {
                    resolve(from);
                } else {
                    this.discard();
                }
                return;
            }
            if (!FinalChakraGeometry.loaded(level, from, to)) {
                this.discard();
                return;
            }
            HitResult hit = firstImpact(level, from, to);
            if (hit.getType() != HitResult.Type.MISS) {
                boolean canceled = ForgeEventFactory.onProjectileImpact(this, hit);
                if (this.isRemoved()) {
                    return;
                }
                if (!canceled) {
                    Vec3 point = hit.getLocation();
                    if (hit instanceof BlockHitResult block) {
                        point = point.add(Vec3.atLowerCornerOf(block.getDirection().getNormal()).scale(0.045));
                    }
                    resolve(point);
                    return;
                }
            }
        }
        this.setPos(to.x, to.y, to.z);
    }

    private HitResult firstImpact(ServerLevel level, Vec3 from, Vec3 to) {
        HitResult nearest = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        double gate = nearest.getType() == HitResult.Type.MISS ? from.distanceToSqr(to) + 1.0e-6
                : from.distanceToSqr(nearest.getLocation());
        var scope = new AABB(from, to).inflate(contactRadius() + 0.1);
        for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, scope, this::canCollideWith)) {
            var bounds = victim.getBoundingBox().inflate(contactRadius());
            Vec3 hit = bounds.contains(from) ? from : bounds.clip(from, to).orElse(null);
            if (hit != null && from.distanceToSqr(hit) < gate && FinalChakraGeometry.clear(level, this, hit, victim.getBoundingBox().getCenter())) {
                gate = from.distanceToSqr(hit);
                nearest = new EntityHitResult(victim, hit);
            }
        }
        return nearest;
    }

    protected boolean canCollideWith(LivingEntity victim) {
        return FinalChakraGeometry.enemy(this.getOwner(), victim);
    }

    protected abstract double contactRadius();
    protected abstract int maxLifetime();
    protected abstract void explode(ServerLevel level, Vec3 point);

    protected boolean explodeAtTimeout() {
        return false;
    }

    private void resolve(Vec3 point) {
        if (resolved || !(this.level() instanceof ServerLevel level)) {
            return;
        }
        resolved = true;
        this.setPos(point.x, point.y, point.z);
        this.discard();
        explode(level, point);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putLong("FlightExpiry", expiry);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        expiry = tag.contains("FlightExpiry", Tag.TAG_LONG) ? tag.getLong("FlightExpiry") : -1;
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    protected long expiry() {
        return expiry;
    }
}
