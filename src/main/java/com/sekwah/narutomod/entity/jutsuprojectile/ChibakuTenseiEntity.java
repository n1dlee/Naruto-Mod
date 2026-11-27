package com.sekwah.narutomod.entity.jutsuprojectile;

import com.sekwah.narutomod.entity.NarutoEntities;
import com.sekwah.narutomod.sounds.NarutoSounds;
import com.sekwah.narutomod.util.ChibakuFormation;
import com.sekwah.narutomod.util.ChibakuFormation.Phase;
import com.sekwah.narutomod.util.ChibakuFormation.Profile;
import com.sekwah.narutomod.util.ChibakuFormation.Source;
import com.sekwah.narutomod.util.JutsuAim;
import com.sekwah.narutomod.util.ProjectileAxis;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
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
import java.util.List;
import java.util.UUID;

public class ChibakuTenseiEntity extends Entity {
    private static final int SHIFT = 9;
    private static final int MASK = (1 << SHIFT) - 1;
    private static final Phase[] PHASES = Phase.values();
    private static final Vec3 UP = new Vec3(0, 1, 0);
    private static final EntityDataAccessor<Integer> FRAME = SynchedEntityData.defineId(ChibakuTenseiEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> NPC = SynchedEntityData.defineId(ChibakuTenseiEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<CompoundTag> SOURCES = SynchedEntityData.defineId(ChibakuTenseiEntity.class, EntityDataSerializers.COMPOUND_TAG);
    private static final EntityDataAccessor<Float> FINAL_SIZE = SynchedEntityData.defineId(ChibakuTenseiEntity.class, EntityDataSerializers.FLOAT);

    private @Nullable UUID owner;
    private @Nullable List<Source> cached;
    private @Nullable CompoundTag legacy;
    private double descent;
    private int missing;

    public ChibakuTenseiEntity(EntityType<ChibakuTenseiEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public ChibakuTenseiEntity(LivingEntity caster, Vec3 origin) {
        this(NarutoEntities.CHIBAKU_TENSEI.get(), caster.level());
        this.setPos(origin);
        this.owner = caster.getUUID();
        this.entityData.set(NPC, !(caster instanceof Player));
    }

    public static boolean launch(Player owner) {
        if (!(owner.level() instanceof ServerLevel level) || owned(owner) != null) {
            return false;
        }
        Vec3 at = ChibakuWorld.origin(owner);
        if (at == null) {
            return false;
        }
        var core = new ChibakuTenseiEntity(owner, at);
        if (!level.addFreshEntity(core)) {
            return false;
        }
        core.tone(level, NarutoSounds.CHIBAKU_SEED.get(), 0.95f);
        return true;
    }

    public static boolean cancel(Player owner) {
        var core = owned(owner);
        if (owner.level() instanceof ServerLevel level && core != null) {
            core.dissolve(level);
            return true;
        }
        return false;
    }

    private static @Nullable ChibakuTenseiEntity owned(Player owner) {
        if (owner.level() instanceof ServerLevel level) {
            for (var core : level.getEntities(NarutoEntities.CHIBAKU_TENSEI.get(), entity ->
                    owner.getUUID().equals(entity.owner) && entity.active())) {
                return core;
            }
        }
        return null;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(FRAME, 0);
        this.entityData.define(NPC, false);
        this.entityData.define(SOURCES, new CompoundTag());
        this.entityData.define(FINAL_SIZE, 0.24f);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (SOURCES.equals(key)) {
            this.cached = null;
        }
    }

    public Phase phase() { return PHASES[this.entityData.get(FRAME) >> SHIFT]; }

    public int age() { return this.entityData.get(FRAME) & MASK; }

    public Profile profile() { return this.entityData.get(NPC) ? ChibakuFormation.NPC : ChibakuFormation.PLAYER; }

    public double size(double partial) {
        return switch (this.phase()) {
            case RISE, LEGACY -> 0.24;
            case GATHER -> ChibakuFormation.grow(this.age() + partial, this.profile().gatherTicks());
            case FALL, IMPACT -> ChibakuFormation.grow(this.profile().gatherTicks(), this.profile().gatherTicks());
            case DISSOLVE -> this.entityData.get(FINAL_SIZE);
        };
    }

    public List<Source> sources() {
        if (this.cached == null) {
            var packed = this.entityData.get(SOURCES).getList("Sources", Tag.TAG_COMPOUND);
            var points = new ArrayList<Source>();
            for (int i = 0; i < Math.min(ChibakuFormation.MAX_SOURCES, packed.size()); i++) {
                var entry = packed.getCompound(i);
                points.add(new Source(BlockPos.of(entry.getLong("P")), entry.getInt("C")));
            }
            this.cached = List.copyOf(points);
        }
        return this.cached;
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        double radius = this.phase() == Phase.IMPACT ? this.profile().shockRange() : Math.min(32, this.profile().pullRange());
        return super.getBoundingBoxForCulling().inflate(radius, 54, radius);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) { return this.phase() != Phase.LEGACY && distance < 104 * 104; }

    @Override
    public boolean isAlive() { return super.isAlive() && this.phase() != Phase.LEGACY; }

    private boolean active() {
        return this.phase() == Phase.RISE || this.phase() == Phase.GATHER || this.phase() == Phase.FALL;
    }

    @Override
    public void tick() {
        super.tick();
        Phase phase = this.phase();
        if (!(this.level() instanceof ServerLevel level) || phase == Phase.LEGACY) {
            return;
        }
        int age = this.age() + 1;
        if (!this.active()) {
            if (age >= ChibakuFormation.SETTLE) {
                this.discard();
            } else {
                frame(phase, age);
            }
            return;
        }
        if (this.owner == null || !(level.getEntity(this.owner) instanceof LivingEntity caster) || !caster.isAlive() || caster.isSpectator()) {
            if (++this.missing > 20) {
                dissolve(level);
            }
            return;
        }
        this.missing = 0;
        frame(phase, age);
        switch (phase) {
            case RISE -> rise(level, caster, age);
            case GATHER -> gather(level, caster, age);
            case FALL -> fall(level, caster, age);
            default -> { }
        }
    }

    private void rise(ServerLevel level, LivingEntity caster, int age) {
        var rise = this.profile().rise();
        Vec3 next = this.position().add(0, rise.speed(), 0);
        if (age >= rise.ticks() || level.isOutsideBuildHeight(BlockPos.containing(next.add(0, 2.6, 0)))
                || !visible(level, this.position(), next.add(0, 0.45, 0))) {
            if (age < 5) {
                dissolve(level);
                return;
            }
            encodeSources(ChibakuWorld.samples(level, this, caster));
            frame(Phase.GATHER, 0);
            tone(level, NarutoSounds.CHIBAKU_GATHER.get(), 1);
        } else {
            this.setPos(next);
        }
    }

    private void gather(ServerLevel level, LivingEntity caster, int age) {
        Profile profile = this.profile();
        if (age >= profile.gatherTicks()) {
            frame(Phase.FALL, 0);
            return;
        }
        if ((age & 3) == 0) {
            pull(level, caster, age, profile);
        }
    }

    private void pull(ServerLevel level, LivingEntity caster, int age, Profile profile) {
        Vec3 center = this.position();
        double reach = profile.pullRange();
        var scan = new AABB(center, center).inflate(reach);
        for (var target : level.getEntitiesOfClass(LivingEntity.class, scan, other -> ChibakuWorld.target(level, caster, other))) {
            Vec3 at = JutsuAim.center(target);
            double distance = at.distanceTo(center);
            if (distance > reach || !visible(level, center, at)) {
                continue;
            }
            double pull = (target instanceof Player ? 0.42 : 0.66) * (0.17 + 0.83 * ChibakuFormation.strength(distance, reach));
            Vec3 impulse = ProjectileAxis.direction(center.subtract(at), UP).scale(pull);
            Vec3 velocity = target.getDeltaMovement().scale(0.70).add(impulse);
            target.setDeltaMovement(velocity);
            target.hurtMarked = true;
            if (distance < this.size(0) + 0.6 && age % 12 == 0) {
                target.hurt(this.damageSources().indirectMagic(this, caster), target instanceof Player ? 2f : 3.5f);
            }
        }
    }

    private void fall(ServerLevel level, LivingEntity caster, int age) {
        this.descent = Math.min(1.6, this.descent + 0.09);
        var next = this.position().subtract(0, this.descent, 0);
        double core = this.size(0);
        Vec3 bottom = next.subtract(0, core, 0);
        if (age > 105 || bottom.y <= level.getMinBuildHeight() || !ChibakuWorld.loaded(level, this.position(), bottom)) {
            dissolve(level);
            return;
        }
        var hit = level.clip(new ClipContext(this.position(), bottom, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        if (hit.getType() == HitResult.Type.BLOCK) {
            Vec3 at = hit.getLocation();
            this.setPos(at.add(0, core, 0));
            impact(level, caster, at.add(0, 0.08, 0));
        } else {
            this.setPos(next);
        }
    }

    private void impact(ServerLevel level, LivingEntity caster, Vec3 center) {
        frame(Phase.IMPACT, 0);
        tone(level, NarutoSounds.CHIBAKU_CRASH.get(), 1.36f);
        Profile profile = this.profile();
        for (var target : level.getEntitiesOfClass(LivingEntity.class, new AABB(center, center).inflate(profile.shockRange()),
                other -> ChibakuWorld.target(level, caster, other))) {
            Vec3 offset = JutsuAim.center(target).subtract(center);
            double distance = offset.length();
            double force = ChibakuFormation.strength(distance, profile.shockRange());
            if (force <= 0 || !visible(level, center, JutsuAim.center(target))) {
                continue;
            }
            double damage = profile.impactDamage() * ChibakuFormation.strength(distance, profile.impactRange());
            if (damage > 0 && !target.hurt(this.damageSources().indirectMagic(this, caster),
                    (float) Math.min(target instanceof Player && caster instanceof Player ? 9 : 45, damage))) {
                continue;
            }
            double limit = caster instanceof Player && target instanceof Player ? 0.72 : 1.21;
            Vec3 away = ProjectileAxis.direction(offset.multiply(1, 0.24, 1), UP);
            target.setDeltaMovement(target.getDeltaMovement().scale(0.66).add(away.scale(force * limit)).add(0, 0.28 * force, 0));
            target.hurtMarked = true;
        }
    }

    boolean visible(ServerLevel level, Vec3 from, Vec3 to) {
        return ChibakuWorld.loaded(level, from, to) && level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, this)).getType() == HitResult.Type.MISS;
    }

    private void dissolve(ServerLevel level) {
        this.entityData.set(FINAL_SIZE, (float) this.size(0));
        frame(Phase.DISSOLVE, 0);
        tone(level, NarutoSounds.CHIBAKU_GATHER.get(), 0.39f);
    }

    private void tone(ServerLevel level, SoundEvent sound, float volume) {
        level.playSound(null, this.getX(), this.getY(), this.getZ(), sound, SoundSource.HOSTILE, volume, 0.92f);
    }

    private void frame(Phase phase, int age) {
        this.entityData.set(FRAME, phase.ordinal() << SHIFT | Mth.clamp(age, 0, MASK));
    }

    private void encodeSources(List<Source> sources) {
        var collection = new ListTag();
        for (Source source : sources) {
            var entry = new CompoundTag();
            entry.putLong("P", source.origin().asLong());
            entry.putInt("C", source.tint());
            collection.add(entry);
        }
        var tag = new CompoundTag();
        tag.put("Sources", collection);
        this.entityData.set(SOURCES, tag);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (!tag.contains("FormationFrame", Tag.TAG_INT)) {
            this.legacy = tag.copy();
            frame(Phase.LEGACY, 0);
            return;
        }
        int raw = tag.getInt("FormationFrame");
        int phase = raw >> SHIFT;
        if (phase < 0 || phase >= PHASES.length) {
            frame(Phase.DISSOLVE, 0);
        } else {
            this.entityData.set(FRAME, raw);
        }
        this.entityData.set(NPC, tag.getBoolean("FormationNpc"));
        this.entityData.set(SOURCES, tag.getCompound("FormationSources").copy());
        this.entityData.set(FINAL_SIZE, Mth.clamp(tag.getFloat("FormationFinalSize"), 0.24f, 2.40f));
        this.owner = tag.hasUUID("OwnerUUID") ? tag.getUUID("OwnerUUID") : null;
        this.descent = Mth.clamp(tag.getDouble("Descent"), 0, 1.6);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (this.legacy != null) {
            tag.merge(this.legacy);
            return;
        }
        tag.putInt("FormationFrame", this.entityData.get(FRAME));
        tag.putBoolean("FormationNpc", this.entityData.get(NPC));
        tag.put("FormationSources", this.entityData.get(SOURCES).copy());
        tag.putFloat("FormationFinalSize", this.entityData.get(FINAL_SIZE));
        tag.putDouble("Descent", this.descent);
        if (this.owner != null) {
            tag.putUUID("OwnerUUID", this.owner);
        }
    }

    @Override
    public boolean isPickable() { return false; }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
