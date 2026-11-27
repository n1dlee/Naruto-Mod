package com.sekwah.narutomod.entity.jutsuprojectile;

import com.sekwah.narutomod.entity.NarutoEntities;
import com.sekwah.narutomod.sounds.NarutoSounds;
import com.sekwah.narutomod.util.IceSpearVolley;
import com.sekwah.narutomod.util.JutsuAim;
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
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
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

public final class IceSpearEntity extends Projectile {
    private static final EntityDataAccessor<Vector3f> AXIS = SynchedEntityData.defineId(IceSpearEntity.class, EntityDataSerializers.VECTOR3);
    private static final EntityDataAccessor<Integer> FRAME = SynchedEntityData.defineId(IceSpearEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Direction> FACE = SynchedEntityData.defineId(IceSpearEntity.class, EntityDataSerializers.DIRECTION);
    private static final int SHIFT = 5;
    private static final int AGE_MASK = (1 << SHIFT) - 1;
    private static final Phase[] PHASES = Phase.values();
    private IceSpearVolley volley = new IceSpearVolley();
    private Vec3 offset = Vec3.ZERO;
    private int slot;
    private int release = 5;
    private float rank = 1f;

    public IceSpearEntity(EntityType<IceSpearEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public static boolean launch(Player caster, float rank) {
        if (!(caster.level() instanceof ServerLevel level) || caster.isSpectator() || !caster.isAlive() || caster.noPhysics) {
            return false;
        }
        Vec3 direction = caster.getLookAngle();
        var volley = new IceSpearVolley();
        boolean any = false;
        for (int slot = 0; slot < IceSpearVolley.COUNT; slot++) {
            var lane = IceSpearVolley.lane(direction, slot);
            Vec3 start = caster.getEyePosition().add(lane.offset());
            if (canStart(level, caster, start)) {
                var shard = new IceSpearEntity(NarutoEntities.ICE_SPEAR.get(), level);
                shard.setOwner(caster);
                shard.setPos(start);
                shard.slot = slot;
                shard.rank = rank;
                shard.offset = lane.offset();
                shard.release = lane.release();
                shard.volley = volley;
                shard.entityData.set(AXIS, ProjectileAxis.encode(lane.heading()));
                any |= level.addFreshEntity(shard);
            }
        }
        if (any) {
            var at = caster.getEyePosition();
            level.playSound(null, at.x, at.y, at.z, NarutoSounds.ICE_FIELD_WARN.get(), SoundSource.PLAYERS, 0.64f, 1.16f);
        }
        return any;
    }

    private static boolean canStart(ServerLevel level, Player owner, Vec3 to) {
        return level.hasChunkAt(BlockPos.containing(to)) && level.clip(new ClipContext(owner.getEyePosition(), to,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, owner)).getType() == HitResult.Type.MISS;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(AXIS, new Vector3f(0, 0, 1));
        this.entityData.define(FRAME, 0);
        this.entityData.define(FACE, Direction.UP);
    }

    public Phase phase() { return PHASES[this.entityData.get(FRAME) >> SHIFT]; }

    public int age() { return this.entityData.get(FRAME) & AGE_MASK; }

    public Vec3 axis() { return ProjectileAxis.direction(ProjectileAxis.decode(this.entityData.get(AXIS)), new Vec3(0, 0, 1)); }

    public Direction face() { return this.entityData.get(FACE); }

    @Override
    protected AABB makeBoundingBox() { return new AABB(this.position(), this.position()).inflate(IceSpearVolley.RADIUS); }

    @Override
    public AABB getBoundingBoxForCulling() { return this.getBoundingBox().inflate(1.48); }

    @Override
    public void tick() {
        super.tick();
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        Phase phase = this.phase();
        int age = this.age() + 1;
        frame(phase, age);
        if (phase == Phase.CHIP || phase == Phase.FADE) {
            if (age >= IceSpearVolley.BREAK) {
                this.discard();
            }
        } else if (!(this.getOwner() instanceof Player owner) || !owner.isAlive() || owner.level() != level
                || owner.isSpectator() || owner.noPhysics) {
            frame(Phase.FADE, 0);
        } else if (phase == Phase.GATHER) {
            gather(level, owner, age);
        } else {
            fly(level, owner, age);
        }
    }

    private void gather(ServerLevel level, Player owner, int age) {
        Vec3 at = owner.getEyePosition().add(this.offset);
        if (!canStart(level, owner, at)) {
            frame(Phase.FADE, 0);
            return;
        }
        this.setPos(at);
        if (age >= this.release) {
            frame(Phase.FLY, 0);
            if (this.slot < IceSpearVolley.COUNT / 2) {
                tone(level, false);
            }
        }
    }

    private void fly(ServerLevel level, Player owner, int age) {
        Vec3 from = this.position();
        Vec3 to = from.add(this.axis().scale(IceSpearVolley.SPEED));
        var border = level.getWorldBorder();
        var box = new AABB(from, to).inflate(IceSpearVolley.RADIUS);
        if (age > IceSpearVolley.LIFE || !KamuiStep.insideBorder(box, border.getMinX(), border.getMinZ(), border.getMaxX(), border.getMaxZ())
                || !level.hasChunksAt(BlockPos.containing(box.minX, box.minY, box.minZ), BlockPos.containing(box.maxX, box.maxY, box.maxZ))) {
            frame(Phase.FADE, 0);
            return;
        }
        var impact = ProjectileVolume.first(this, level, from, to, IceSpearVolley.RADIUS,
                target -> JutsuAim.attackable(owner, target) && level.mayInteract(owner, target.blockPosition()));
        if (impact == null) {
            this.setPos(to);
        } else if (ForgeEventFactory.onProjectileImpact(this, impact.result())) {
            frame(Phase.FADE, 0);
        } else {
            this.setPos(impact.result().getLocation());
            this.entityData.set(FACE, impact.face());
            var target = impact.target();
            if (target != null && this.volley.claim(target.getUUID())) {
                float damage = Math.min(4.1f * this.rank, target instanceof Player ? 4.4f : 8.0f);
                target.hurt(this.damageSources().thrown(this, owner), damage);
            }
            frame(Phase.CHIP, 0);
            if (this.slot % 2 == 0) {
                tone(level, true);
            }
        }
    }

    private void tone(ServerLevel level, boolean contact) {
        var sound = contact ? NarutoSounds.ICE_FIELD_MELT.get() : NarutoSounds.ICE_FIELD_RISE.get();
        level.playSound(null, this.getX(), this.getY(), this.getZ(), sound, SoundSource.PLAYERS,
                contact ? 0.48f : 0.59f, 1.19f + this.slot * 0.06f);
    }

    private void frame(Phase phase, int age) { this.entityData.set(FRAME, phase.ordinal() << SHIFT | age); }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() { return NetworkHooks.getEntitySpawningPacket(this); }

    public enum Phase { GATHER, FLY, CHIP, FADE }
}
