package com.sekwah.narutomod.entity.jutsuprojectile;

import com.sekwah.narutomod.entity.NarutoEntities;
import com.sekwah.narutomod.sounds.NarutoSounds;
import com.sekwah.narutomod.util.IronSandFlight;
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
import net.minecraft.sounds.SoundEvent;
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

public final class IronSandEntity extends Projectile {
    private static final EntityDataAccessor<Vector3f> AXIS = SynchedEntityData.defineId(IronSandEntity.class, EntityDataSerializers.VECTOR3);
    private static final EntityDataAccessor<Integer> FRAME = SynchedEntityData.defineId(IronSandEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Direction> FACE = SynchedEntityData.defineId(IronSandEntity.class, EntityDataSerializers.DIRECTION);
    private static final int PHASE_SHIFT = 5;
    private static final int AGE_BITS = (1 << PHASE_SHIFT) - 1;
    private static final Stage[] STAGES = Stage.values();
    private float rank = 1;

    public IronSandEntity(EntityType<IronSandEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public static boolean canLaunch(Player caster) {
        return caster.level() instanceof ServerLevel level && caster.isAlive() && !caster.isSpectator()
                && !caster.noPhysics && resolveMuzzle(caster, caster.getLookAngle(), level);
    }

    public static boolean launch(Player caster, float rank) {
        if (canLaunch(caster) && caster.level() instanceof ServerLevel level) {
            var sand = new IronSandEntity(NarutoEntities.IRON_SAND.get(), level);
            Vec3 direction = ProjectileAxis.direction(caster.getLookAngle(), new Vec3(0, 0, 1));
            sand.setOwner(caster);
            sand.entityData.set(AXIS, ProjectileAxis.encode(direction));
            sand.rank = rank;
            sand.setPos(muzzle(caster, direction));
            if (level.addFreshEntity(sand)) {
                sand.tone(level, NarutoSounds.IRON_SAND_GATHER.get(), 0.69f, 0.89f);
                return true;
            }
        }
        return false;
    }

    private static boolean resolveMuzzle(Player owner, Vec3 axis, ServerLevel level) {
        Vec3 eye = owner.getEyePosition();
        Vec3 muzzle = muzzle(owner, axis);
        return level.hasChunkAt(BlockPos.containing(muzzle))
                && level.clip(new ClipContext(eye, muzzle, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, owner))
                .getType() == HitResult.Type.MISS;
    }

    private static Vec3 muzzle(Player owner, Vec3 axis) { return owner.getEyePosition().add(axis.scale(0.69)); }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(AXIS, new Vector3f(0, 0, 1));
        this.entityData.define(FRAME, 0);
        this.entityData.define(FACE, Direction.UP);
    }

    public Stage stage() { return STAGES[this.entityData.get(FRAME) >> PHASE_SHIFT]; }

    public int age() { return this.entityData.get(FRAME) & AGE_BITS; }

    public Vec3 axis() { return ProjectileAxis.direction(ProjectileAxis.decode(this.entityData.get(AXIS)), new Vec3(0, 0, 1)); }

    public Direction face() { return this.entityData.get(FACE); }

    @Override
    protected AABB makeBoundingBox() { return new AABB(this.position(), this.position()).inflate(IronSandFlight.RADIUS); }

    @Override
    public AABB getBoundingBoxForCulling() { return super.getBoundingBoxForCulling().inflate(2.1); }

    @Override
    public void tick() {
        super.tick();
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        Stage stage = this.stage();
        int time = this.age() + 1;
        frame(stage, time);
        if (stage == Stage.BLOCK || stage == Stage.HIT || stage == Stage.FALL) {
            if (time >= IronSandFlight.SCATTER) {
                this.discard();
            }
        } else if (!(this.getOwner() instanceof Player caster) || !caster.isAlive() || caster.level() != level
                || caster.isSpectator() || caster.noPhysics) {
            frame(Stage.FALL, 0);
        } else if (stage == Stage.GATHER) {
            gather(level, caster, time);
        } else {
            fly(level, caster, time);
        }
    }

    private void gather(ServerLevel level, Player caster, int age) {
        if (!resolveMuzzle(caster, this.axis(), level)) {
            frame(Stage.FALL, 0);
            return;
        }
        this.setPos(muzzle(caster, this.axis()));
        if (age >= IronSandFlight.GATHER) {
            frame(Stage.FLY, 0);
            tone(level, NarutoSounds.IRON_SAND_GATHER.get(), 0.82f, 1.24f);
        }
    }

    private void fly(ServerLevel level, Player caster, int age) {
        Vec3 from = this.position();
        Vec3 to = IronSandFlight.step(from, this.axis(), age);
        if (age > IronSandFlight.FLIGHT || !available(level, new AABB(from, to).inflate(IronSandFlight.RADIUS))) {
            frame(Stage.FALL, 0);
            return;
        }
        var impact = ProjectileVolume.first(this, level, from, to, IronSandFlight.RADIUS,
                actor -> JutsuAim.attackable(caster, actor) && level.mayInteract(caster, actor.blockPosition()));
        if (impact == null) {
            this.setPos(to);
        } else if (ForgeEventFactory.onProjectileImpact(this, impact.result())) {
            frame(Stage.FALL, 0);
        } else {
            contact(level, caster, impact);
        }
    }

    private void contact(ServerLevel level, Player caster, ProjectileVolume.Impact impact) {
        this.setPos(impact.result().getLocation());
        this.entityData.set(FACE, impact.face());
        var target = impact.target();
        if (target == null) {
            frame(Stage.BLOCK, 0);
        } else {
            float damage = Math.min(8.1f * this.rank, target instanceof Player ? 8.5f : 17f);
            boolean accepted = target.hurt(this.damageSources().thrown(this, caster), damage);
            frame(accepted ? Stage.HIT : Stage.FALL, 0);
        }
        tone(level, NarutoSounds.IRON_SAND_BREAK.get(), target == null ? 0.97f : 0.70f, target == null ? 0.76f : 0.93f);
    }

    private static boolean available(ServerLevel level, AABB box) {
        var border = level.getWorldBorder();
        return KamuiStep.insideBorder(box, border.getMinX(), border.getMinZ(), border.getMaxX(), border.getMaxZ())
                && level.hasChunkAt(BlockPos.containing(box.minX, box.minY, box.minZ))
                && level.hasChunkAt(BlockPos.containing(box.minX, box.minY, box.maxZ))
                && level.hasChunkAt(BlockPos.containing(box.maxX, box.minY, box.minZ))
                && level.hasChunkAt(BlockPos.containing(box.maxX, box.minY, box.maxZ));
    }

    private void frame(Stage stage, int age) { this.entityData.set(FRAME, stage.ordinal() << PHASE_SHIFT | age); }

    private void tone(ServerLevel level, SoundEvent event, float volume, float pitch) {
        level.playSound(null, this.getX(), this.getY(), this.getZ(), event, SoundSource.PLAYERS, volume, pitch);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() { return NetworkHooks.getEntitySpawningPacket(this); }

    public enum Stage { GATHER, FLY, BLOCK, HIT, FALL }
}
