package com.sekwah.narutomod.entity.jutsuprojectile;

import com.sekwah.narutomod.config.NarutoConfig;
import com.sekwah.narutomod.entity.NarutoEntities;
import com.sekwah.narutomod.events.ClayChargeEvents;
import com.sekwah.narutomod.sounds.NarutoSounds;
import com.sekwah.narutomod.util.ClayFlight;
import com.sekwah.narutomod.util.JutsuAim;
import com.sekwah.narutomod.util.JutsuVfx;
import com.sekwah.narutomod.util.KamuiStep;
import com.sekwah.narutomod.util.ProjectileAxis;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.network.NetworkHooks;
import org.joml.Vector3f;

import javax.annotation.Nullable;

public final class ClayBombEntity extends Projectile {
    private static final Vec3 FRONT = new Vec3(0, 0, 1);
    private static final EntityDataAccessor<Vector3f> AXIS = SynchedEntityData.defineId(ClayBombEntity.class, EntityDataSerializers.VECTOR3);
    private static final EntityDataAccessor<Integer> FRAME = SynchedEntityData.defineId(ClayBombEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> CRAWLER = SynchedEntityData.defineId(ClayBombEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> GROUNDED = SynchedEntityData.defineId(ClayBombEntity.class, EntityDataSerializers.BOOLEAN);
    private static final int SHIFT = 5;
    private static final int MASK = (1 << SHIFT) - 1;
    private static final Phase[] PHASES = Phase.values();
    private Vec3 facing = FRONT;
    private Vec3 drift = Vec3.ZERO;

    public ClayBombEntity(EntityType<ClayBombEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public static boolean launch(Player owner) {
        if (!(owner.level() instanceof ServerLevel level) || !owner.isAlive() || owner.isSpectator()
                || owner.noPhysics || ClayChargeEvents.active(owner) != null) {
            return false;
        }
        Vec3 origin = hand(owner);
        Vec3 from = owner.getEyePosition();
        if (level.clip(new ClipContext(from, origin, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, owner))
                .getType() != HitResult.Type.MISS) {
            return false;
        }
        var charge = new ClayBombEntity(NarutoEntities.CLAY_CHARGE.get(), level);
        charge.setOwner(owner);
        boolean crawler = owner.isShiftKeyDown();
        charge.entityData.set(CRAWLER, crawler);
        charge.facing = crawler ? KamuiStep.facing(owner.getYRot()) : owner.getLookAngle();
        charge.aim(charge.facing);
        charge.setPos(origin);
        if (!level.addFreshEntity(charge)) {
            return false;
        }
        ClayChargeEvents.bind(owner, charge);
        charge.tone(level, NarutoSounds.CLAY_MOLD.get(), 0.63f, 0.93f);
        return true;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(AXIS, new Vector3f(0, 0, 1));
        this.entityData.define(FRAME, 0);
        this.entityData.define(CRAWLER, false);
        this.entityData.define(GROUNDED, false);
    }

    public Phase phase() { return PHASES[this.entityData.get(FRAME) >> SHIFT]; }

    public int age() { return this.entityData.get(FRAME) & MASK; }

    public boolean crawler() { return this.entityData.get(CRAWLER); }

    public boolean grounded() { return this.entityData.get(GROUNDED); }

    public Vec3 axis() { return ProjectileAxis.direction(ProjectileAxis.decode(this.entityData.get(AXIS)), FRONT); }

    public boolean pending() { return this.phase() != Phase.BURST && this.phase() != Phase.FIZZLE; }

    private void aim(Vec3 heading) {
        this.entityData.set(AXIS, ProjectileAxis.encode(ProjectileAxis.direction(heading, this.facing)));
    }

    @Override
    protected AABB makeBoundingBox() { return new AABB(this.position(), this.position()).inflate(ClayFlight.RADIUS); }

    @Override
    public AABB getBoundingBoxForCulling() { return this.getBoundingBox().inflate(this.pending() ? 1.0 : 4.1); }

    @Override
    public void tick() {
        super.tick();
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        Phase phase = this.phase();
        int age = this.age() + 1;
        if (!this.pending()) {
            if (age >= ClayFlight.BURST) {
                this.discard();
            } else {
                frame(phase, age);
            }
            return;
        }
        frame(phase, age);
        if (!(this.getOwner() instanceof Player owner) || !owner.isAlive() || owner.level() != level
                || owner.isSpectator() || owner.noPhysics || owner.distanceToSqr(this) > 54 * 54) {
            fizzle();
            return;
        }
        switch (phase) {
            case MOLD -> shape(level, owner, age);
            case OUTBOUND -> moveCharge(level, owner, age);
            case FUSE -> {
                if (age >= ClayFlight.FUSE) {
                    explode(level, owner);
                } else if (age == ClayFlight.ARM) {
                    tone(level, NarutoSounds.CLAY_ARM.get(), 0.65f, 1.14f);
                }
            }
            default -> { }
        }
    }

    private void shape(ServerLevel level, Player owner, int age) {
        Vec3 origin = hand(owner);
        if (!ProjectileVolume.visible(this, owner.getEyePosition(), origin)) {
            fizzle();
            return;
        }
        this.setPos(origin);
        if (age >= ClayFlight.MOLD) {
            this.drift = this.crawler() ? this.facing.scale(0.55).add(0, -0.22, 0) : ClayFlight.birdLaunch(this.facing);
            this.aim(this.drift);
            frame(Phase.OUTBOUND, 0);
            tone(level, NarutoSounds.CLAY_MOLD.get(), 0.77f, 1.16f);
        }
    }

    private void moveCharge(ServerLevel level, Player owner, int age) {
        boolean crawler = this.crawler();
        Vec3 from = this.position();
        if (age > (crawler ? ClayFlight.CRAWL_FLIGHT : ClayFlight.BIRD_FLIGHT)) {
            if (this.grounded()) {
                settle(level, from, Vec3.ZERO);
            } else {
                fizzle();
            }
            return;
        }
        Vec3 to = this.grounded() ? groundStep(from) : from.add(this.drift);
        if (to == null) {
            settle(level, from, Vec3.ZERO);
            return;
        }
        if (!allowed(level, owner, new AABB(from, to).inflate(ClayFlight.RADIUS), to)) {
            fizzle();
            return;
        }
        var impact = ProjectileVolume.first(this, level, from, to, ClayFlight.RADIUS,
                target -> JutsuAim.attackable(owner, target) && level.mayInteract(owner, target.blockPosition()));
        if (impact == null) {
            this.setPos(to);
            this.aim(to.subtract(from));
            if (!this.grounded()) {
                this.drift = crawler ? this.drift.add(0, -0.069, 0) : ClayFlight.nextFlight(this.drift);
            }
        } else if (ForgeEventFactory.onProjectileImpact(this, impact.result())) {
            fizzle();
        } else {
            land(level, impact, crawler);
        }
    }

    private void land(ServerLevel level, ProjectileVolume.Impact impact, boolean crawler) {
        Vec3 contact = impact.result().getLocation();
        Vec3 normal = Vec3.atLowerCornerOf(impact.face().getNormal());
        if (crawler && !this.grounded() && impact.target() == null && impact.face() == Direction.UP) {
            this.setPos(ClayFlight.settle(contact, normal));
            this.entityData.set(GROUNDED, true);
            this.aim(this.facing);
        } else {
            settle(level, contact, normal);
        }
    }

    private @Nullable Vec3 groundStep(Vec3 from) {
        Vec3 at = from.add(this.facing.scale(0.54));
        if (!this.level().hasChunkAt(BlockPos.containing(at))) {
            return null;
        }
        var floor = ProjectileVolume.clip(this, at.add(0, 0.24, 0), at.subtract(0, 0.97, 0), ClipContext.Fluid.NONE);
        if (floor.getType() != HitResult.Type.BLOCK || floor.getDirection() != Direction.UP) {
            return null;
        }
        return ClayFlight.settle(floor.getLocation(), new Vec3(0, 1, 0));
    }

    private void settle(ServerLevel level, Vec3 contact, Vec3 normal) {
        this.setPos(ClayFlight.settle(contact, normal));
        this.aim(this.facing.multiply(1, 0, 1));
        frame(Phase.FUSE, 0);
        tone(level, NarutoSounds.CLAY_ARM.get(), 0.59f, 0.82f);
    }

    private static boolean allowed(ServerLevel level, Player owner, AABB bounds, Vec3 to) {
        var border = level.getWorldBorder();
        BlockPos end = BlockPos.containing(to);
        return !level.isOutsideBuildHeight(end) && level.mayInteract(owner, end)
                && KamuiStep.insideBorder(bounds, border.getMinX(), border.getMinZ(), border.getMaxX(), border.getMaxZ())
                && level.hasChunksAt(BlockPos.containing(bounds.minX, bounds.minY, bounds.minZ),
                BlockPos.containing(bounds.maxX, bounds.maxY, bounds.maxZ));
    }

    public boolean remote(Player owner) {
        return this.level() instanceof ServerLevel level && this.getOwner() == owner
                && this.phase() == Phase.FUSE && ClayFlight.armed(this.age()) && explode(level, owner);
    }

    private boolean explode(ServerLevel level, Player owner) {
        if (!this.pending() || !owner.isAlive() || owner.level() != level || owner.distanceToSqr(this) > 54 * 54
                || !level.mayInteract(owner, this.blockPosition())) {
            fizzle();
            return false;
        }
        frame(Phase.BURST, 0);
        level.explode(this, this.getX(), this.getY(), this.getZ(), this.crawler() ? 2.75f : 3.45f,
                NarutoConfig.paperbombBlockDamage ? Level.ExplosionInteraction.TNT : Level.ExplosionInteraction.NONE);
        tone(level, NarutoSounds.CLAY_BURST.get(), 0.94f, this.crawler() ? 1.02f : 0.86f);
        return true;
    }

    public void fizzle() {
        if (this.pending() && this.level() instanceof ServerLevel level) {
            frame(Phase.FIZZLE, 0);
            tone(level, NarutoSounds.CLAY_MOLD.get(), 0.56f, 0.68f);
        }
    }

    @Override
    public boolean isPickable() { return this.phase() == Phase.FUSE; }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!this.level().isClientSide && this.isPickable() && amount > 0 && source.getEntity() != this.getOwner()) {
            fizzle();
            return true;
        }
        return false;
    }

    @Override
    public void thunderHit(ServerLevel level, LightningBolt bolt) {
        fizzle();
    }

    private void frame(Phase phase, int age) { this.entityData.set(FRAME, phase.ordinal() << SHIFT | age); }

    private static Vec3 hand(Player owner) {
        return JutsuVfx.handPosition(owner, owner.getMainArm() == HumanoidArm.RIGHT ? 1 : -1, 1);
    }

    private void tone(ServerLevel level, SoundEvent event, float volume, float pitch) {
        level.playSound(null, this.getX(), this.getY(), this.getZ(), event, SoundSource.PLAYERS, volume, pitch);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() { return NetworkHooks.getEntitySpawningPacket(this); }

    public enum Phase { MOLD, OUTBOUND, FUSE, BURST, FIZZLE }
}
