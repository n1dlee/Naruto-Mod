package com.sekwah.narutomod.entity.jutsuprojectile;

import com.sekwah.narutomod.damagetypes.NarutoDamageTypes;
import com.sekwah.narutomod.entity.NarutoEntities;
import com.sekwah.narutomod.sounds.NarutoSounds;
import com.sekwah.narutomod.util.AmaterasuFlames;
import com.sekwah.narutomod.util.Faction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.ForgeEventFactory;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class AmaterasuFireEntity extends Projectile {
    private static final EntityDataAccessor<Byte> PHASE =
            SynchedEntityData.defineId(AmaterasuFireEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> AGE =
            SynchedEntityData.defineId(AmaterasuFireEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Direction> FACE =
            SynchedEntityData.defineId(AmaterasuFireEntity.class, EntityDataSerializers.DIRECTION);
    private static final EntityDataAccessor<Boolean> BODY =
            SynchedEntityData.defineId(AmaterasuFireEntity.class, EntityDataSerializers.BOOLEAN);
    private static final Phase[] PHASES = Phase.values();
    private final Set<UUID> exposed = new HashSet<>();
    @Nullable
    private UUID author;
    @Nullable
    private UUID carrier;
    private Vec3 attachment = Vec3.ZERO;
    private float power = 1;
    private long ends;
    private int missing;

    public AmaterasuFireEntity(EntityType<AmaterasuFireEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public static boolean aim(LivingEntity owner, Vec3 point, float power) {
        if (!(owner.level() instanceof ServerLevel level) || !owner.isAlive() || owner.isSpectator()) {
            return false;
        }
        var flame = new AmaterasuFireEntity(NarutoEntities.AMATERASU_FIRE.get(), level);
        flame.owner(owner, power);
        Vec3 eye = owner.getEyePosition();
        Vec3 direction = point.subtract(eye).normalize();
        if (direction.lengthSqr() == 0) {
            return false;
        }
        flame.setRot((float) Math.toDegrees(Math.atan2(direction.z, direction.x)) - 90,
                (float) -Math.toDegrees(Math.atan2(direction.y, direction.horizontalDistance())));
        flame.setPos(eye);
        return level.addFreshEntity(flame);
    }

    public static int extinguishOwned(Player player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return 0;
        }
        double range = AmaterasuFlames.RANGE;
        var flames = level.getEntitiesOfClass(AmaterasuFireEntity.class, player.getBoundingBox().inflate(range),
                flame -> AmaterasuFlames.ownedBy(flame.author, player.getUUID()) && flame.phase() != Phase.FADE
                        && flame.distanceToSqr(player) <= range * range);
        for (var flame : flames) {
            flame.phase(Phase.FADE);
        }
        return flames.size();
    }

    private void owner(LivingEntity owner, float power) {
        this.setOwner(owner);
        this.author = owner.getUUID();
        this.power = power;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(PHASE, (byte) Phase.FOCUS.ordinal());
        this.entityData.define(AGE, 0);
        this.entityData.define(FACE, Direction.UP);
        this.entityData.define(BODY, false);
    }

    public Phase phase() {
        return PHASES[this.entityData.get(PHASE)];
    }

    public int age() {
        return this.entityData.get(AGE);
    }

    public Direction face() {
        return this.entityData.get(FACE);
    }

    public boolean body() {
        return this.entityData.get(BODY);
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        return super.getBoundingBoxForCulling().inflate(1.7);
    }

    @Override
    public void tick() {
        super.tick();
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        int age = this.age() + 1;
        this.entityData.set(AGE, age);
        if (this.author == null || (this.phase() == Phase.FADE && age > AmaterasuFlames.FADE)) {
            this.discard();
            return;
        }
        var owner = this.getOwner() instanceof LivingEntity living && living.isAlive() && living.level() == level
                && !living.isSpectator() ? living : null;
        Phase phase = this.phase();
        if (phase == Phase.FOCUS) {
            if (owner == null) {
                this.discard();
            } else {
                this.setPos(owner.getEyePosition());
                if (age >= AmaterasuFlames.FOCUS) {
                    this.release(level, owner);
                }
            }
        } else if (phase == Phase.CARRIER || phase == Phase.SURFACE) {
            if (level.getGameTime() >= this.ends) {
                this.phase(Phase.FADE);
            } else if (phase == Phase.CARRIER) {
                this.cling(level, owner, age);
            } else if (owner != null && age % AmaterasuFlames.PULSE == 0) {
                this.catchFromSurface(level, owner);
            }
        }
    }

    private void release(ServerLevel level, LivingEntity owner) {
        Vec3 from = this.position();
        Vec3 end = from;
        for (int i = 1; i <= AmaterasuFlames.RANGE; i++) {
            Vec3 next = from.add(this.getLookAngle().scale(i));
            if (!level.hasChunkAt(BlockPos.containing(next)) || !level.getWorldBorder().isWithinBounds(BlockPos.containing(next))) {
                break;
            }
            end = next;
        }
        var hit = ProjectileVolume.first(this, level, from, end, 0.025, target -> eligible(owner, target));
        if (hit == null || ForgeEventFactory.onProjectileImpact(this, hit.result())) {
            this.discard();
            return;
        }
        this.setPos(hit.result().getLocation());
        if (hit.target() != null) {
            if (this.existing(level, hit.target(), owner) || !this.burn(level, owner, hit.target())) {
                this.discard();
                return;
            }
            this.attach(level, hit.target(), this.position());
        } else {
            this.entityData.set(FACE, hit.face());
            Vec3 normal = Vec3.atLowerCornerOf(hit.face().getNormal());
            this.setPos(this.position().add(normal.scale(0.035)));
            this.ends = AmaterasuFlames.expiry(level.getGameTime(), false);
            this.phase(Phase.SURFACE);
        }
        level.playSound(null, this.getX(), this.getY(), this.getZ(), NarutoSounds.AMATERASU_IGNITE.get(),
                SoundSource.HOSTILE, 1.5f, 0.98f);
    }

    private boolean existing(ServerLevel level, LivingEntity target, LivingEntity owner) {
        return !level.getEntitiesOfClass(AmaterasuFireEntity.class, target.getBoundingBox().inflate(3),
                flame -> flame != this && flame.phase() == Phase.CARRIER && target.getUUID().equals(flame.carrier)
                        && AmaterasuFlames.ownedBy(flame.author, owner.getUUID())).isEmpty();
    }

    private void attach(ServerLevel level, LivingEntity target, Vec3 contact) {
        this.carrier = target.getUUID();
        this.entityData.set(BODY, true);
        this.attachment = contact.subtract(target.position()).yRot(target.getYRot() * Mth.DEG_TO_RAD);
        this.ends = AmaterasuFlames.expiry(level.getGameTime(), true);
        this.phase(Phase.CARRIER);
        this.setPos(contact);
    }

    private void cling(ServerLevel level, @Nullable LivingEntity owner, int age) {
        var linked = this.carrier == null ? null : level.getEntity(this.carrier);
        if (!(linked instanceof LivingEntity target)) {
            if (++this.missing > 25) {
                this.phase(Phase.FADE);
            }
            return;
        }
        this.missing = 0;
        if (!target.isAlive()) {
            this.phase(Phase.FADE);
            return;
        }
        this.setPos(target.position().add(this.attachment.yRot(-target.getYRot() * Mth.DEG_TO_RAD)));
        if (owner != null && age % AmaterasuFlames.PULSE == 0) {
            if (eligible(owner, target)) {
                this.burn(level, owner, target);
            } else {
                this.phase(Phase.FADE);
            }
        }
    }

    private void catchFromSurface(ServerLevel level, LivingEntity owner) {
        if (this.exposed.size() >= AmaterasuFlames.MAX_CARRIERS) {
            return;
        }
        AABB patch = AmaterasuFlames.patch(this.position(), this.face());
        var targets = level.getEntitiesOfClass(LivingEntity.class, patch, target -> eligible(owner, target)
                && !this.exposed.contains(target.getUUID()));
        for (LivingEntity target : targets) {
            if (this.exposed.size() >= AmaterasuFlames.MAX_CARRIERS) {
                break;
            }
            Vec3 contact = com.sekwah.narutomod.util.WaterDragonFlight.nearestPoint(target.getBoundingBox(), this.position());
            if (this.existing(level, target, owner) || !ProjectileVolume.visible(this, this.position(), contact)
                    || !this.burn(level, owner, target)) {
                continue;
            }
            var child = new AmaterasuFireEntity(NarutoEntities.AMATERASU_FIRE.get(), level);
            child.owner(owner, this.power);
            child.attach(level, target, contact);
            if (level.addFreshEntity(child)) {
                this.exposed.add(target.getUUID());
            }
        }
    }

    private boolean burn(ServerLevel level, LivingEntity owner, LivingEntity target) {
        var damage = NarutoDamageTypes.getDamageSource(level, NarutoDamageTypes.AMATERASU, this, owner, this.position());
        return !target.fireImmune() && target.hurt(damage, 2.4f * this.power);
    }

    private static boolean eligible(LivingEntity owner, LivingEntity target) {
        if (owner == target || !target.isAlive() || !target.isPickable() || target.isSpectator()
                || Faction.sameSide(owner, target) || owner.isAlliedTo(target)) {
            return false;
        }
        return !(target instanceof Player player)
                || (!player.isCreative() && (!(owner instanceof Player caster) || caster.canHarmPlayer(player)));
    }

    private void phase(Phase phase) {
        this.entityData.set(PHASE, (byte) phase.ordinal());
        this.entityData.set(AGE, 0);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (this.author != null && this.phase() != Phase.FOCUS) {
            tag.putUUID("ArsonAuthor", this.author);
        }
        if (this.carrier != null) {
            tag.putUUID("ArsonCarrier", this.carrier);
        }
        tag.putLong("ArsonEnds", this.ends);
        tag.putInt("ArsonAge", this.age());
        tag.putByte("ArsonPhase", (byte) this.phase().ordinal());
        tag.putByte("ArsonFace", (byte) this.face().get3DDataValue());
        tag.putFloat("ArsonPower", this.power);
        tag.put("ArsonOffset", this.newDoubleList(this.attachment.x, this.attachment.y, this.attachment.z));
        var consumed = new ListTag();
        this.exposed.forEach(id -> consumed.add(NbtUtils.createUUID(id)));
        tag.put("ArsonSeen", consumed);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.author = tag.hasUUID("ArsonAuthor") ? tag.getUUID("ArsonAuthor") : null;
        this.carrier = tag.hasUUID("ArsonCarrier") ? tag.getUUID("ArsonCarrier") : null;
        this.entityData.set(BODY, this.carrier != null);
        this.ends = tag.getLong("ArsonEnds");
        this.power = tag.getFloat("ArsonPower");
        this.entityData.set(AGE, Mth.clamp(tag.getInt("ArsonAge"), 0, 240));
        this.entityData.set(PHASE, (byte) Mth.clamp(tag.getByte("ArsonPhase"), 0, Phase.FADE.ordinal()));
        this.entityData.set(FACE, Direction.from3DDataValue(tag.getByte("ArsonFace")));
        var offset = tag.getList("ArsonOffset", Tag.TAG_DOUBLE);
        this.attachment = offset.size() == 3 ? new Vec3(offset.getDouble(0), offset.getDouble(1), offset.getDouble(2)) : Vec3.ZERO;
        var consumed = tag.getList("ArsonSeen", Tag.TAG_INT_ARRAY);
        for (int i = 0; i < Math.min(consumed.size(), AmaterasuFlames.MAX_CARRIERS); i++) {
            this.exposed.add(NbtUtils.loadUUID(consumed.get(i)));
        }
    }

    public enum Phase {
        FOCUS, SURFACE, CARRIER, FADE
    }
}
