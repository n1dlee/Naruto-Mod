package com.sekwah.narutomod.entity.jutsuprojectile;

import com.sekwah.narutomod.entity.NarutoEntities;
import com.sekwah.narutomod.util.NarutoParticles;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

import java.util.Optional;
import java.util.UUID;

/**
 * Chibaku Tensei - the black core thrown into the sky that everything falls toward.
 *
 * This used to be a single frame of work inside the boss AI: one impulse, one burst of
 * particles, over. As an entity it is the technique it is supposed to be - a core that hangs
 * where it was put and pulls for eight seconds, so the fight becomes about getting out from
 * under it rather than about surviving one hit.
 *
 * It does not build a moon out of the landscape. Every technique in this mod leaves terrain
 * alone, for the reason the boss AI already documents: these things spawn next to whatever
 * the player has built, and losing a base to a stray technique is not the fight anyone
 * signed up for. The pull, the crush and the collapse are all entity-side.
 */
public class ChibakuTenseiEntity extends Entity {

    /** Drawn from this, and the pull radius scales with it. */
    private static final EntityDataAccessor<Float> SIZE =
            SynchedEntityData.defineId(ChibakuTenseiEntity.class, EntityDataSerializers.FLOAT);

    private static final int RISE_TICKS = 25;
    private static final int HOLD_TICKS = 8 * 20;
    private static final double RISE_SPEED = 0.35;
    /** How far the core reaches, in blocks, at full size. */
    private static final double PULL_RADIUS = 16.0;
    /** Per tick, at the rim. Closer in it is stronger; see tickPull. */
    private static final double PULL_STRENGTH = 0.09;
    private static final float CRUSH_DAMAGE = 3.5f;
    private static final double CRUSH_RADIUS = 3.0;
    private static final float COLLAPSE_DAMAGE = 22f;

    private Optional<UUID> ownerUUID = Optional.empty();
    private int age;

    public ChibakuTenseiEntity(EntityType<ChibakuTenseiEntity> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
    }

    public ChibakuTenseiEntity(LivingEntity caster, Vec3 origin) {
        this(NarutoEntities.CHIBAKU_TENSEI.get(), caster.level());
        this.setPos(origin.x, origin.y, origin.z);
        this.ownerUUID = Optional.of(caster.getUUID());
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(SIZE, 0.2f);
    }

    public float getSize() {
        return this.entityData.get(SIZE);
    }

    @Override
    public void tick() {
        super.tick();
        this.age++;

        // Rises first, so there is a moment to read what is happening before it starts to bite.
        if (this.age <= RISE_TICKS) {
            this.setPos(this.getX(), this.getY() + RISE_SPEED, this.getZ());
        }

        float growth = Math.min(1.0f, this.age / (float) RISE_TICKS);
        this.entityData.set(SIZE, 0.2f + growth * 2.6f);

        if (this.level().isClientSide) {
            this.spawnAmbientParticles();
            return;
        }
        if (this.age > RISE_TICKS) {
            this.tickPull();
        }
        if (this.age >= RISE_TICKS + HOLD_TICKS) {
            this.collapse();
        }
    }

    /**
     * Drags everything in reach toward the core, harder the closer it already is.
     *
     * Deliberately an acceleration added to existing motion rather than a set velocity: that
     * way a player who runs, leaps or uses a movement technique is fighting the pull instead
     * of having their input overwritten by it, which is the difference between a hazard and
     * a cutscene.
     */
    private void tickPull() {
        Vec3 core = this.position();
        double radius = PULL_RADIUS;
        for (LivingEntity caught : this.level().getEntitiesOfClass(LivingEntity.class,
                new AABB(core, core).inflate(radius), this::affects)) {
            Vec3 toCore = core.subtract(caught.position());
            double distance = toCore.length();
            if (distance < 0.1) {
                continue;
            }
            double falloff = 1.0 - Math.min(1.0, distance / radius);
            Vec3 pull = toCore.normalize().scale(PULL_STRENGTH * (0.35 + falloff));
            caught.setDeltaMovement(caught.getDeltaMovement().add(pull));
            caught.hurtMarked = true;
            caught.fallDistance = 0.0f;

            // Anything that actually reaches the core is being ground against it.
            if (distance < CRUSH_RADIUS && this.age % 10 == 0) {
                caught.hurt(this.damageSource(), CRUSH_DAMAGE);
            }
        }
    }

    /** The core comes apart, and everything it gathered comes down with it. */
    private void collapse() {
        if (this.level() instanceof ServerLevel serverLevel) {
            Vec3 core = this.position();
            for (LivingEntity caught : this.level().getEntitiesOfClass(LivingEntity.class,
                    new AABB(core, core).inflate(PULL_RADIUS * 0.6), this::affects)) {
                double distance = caught.position().distanceTo(core);
                float falloff = (float) Math.max(0.0, 1.0 - distance / (PULL_RADIUS * 0.6));
                caught.hurt(this.damageSource(), COLLAPSE_DAMAGE * (0.3f + 0.7f * falloff));
                Vec3 push = caught.position().subtract(core).normalize().scale(1.2 * falloff);
                caught.setDeltaMovement(push.x, 0.4 * falloff, push.z);
                caught.hurtMarked = true;
            }
            for (double r = 2.0; r <= 10.0; r += 2.0) {
                NarutoParticles.spawnRing(serverLevel, core, r, (int) (r * 8), NarutoParticles.SHADOW_PURPLE);
            }
            serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                    core.x, core.y, core.z, 4, 2.0, 2.0, 2.0, 0.0);
            serverLevel.playSound(null, this.blockPosition(), SoundEvents.GENERIC_EXPLODE,
                    SoundSource.HOSTILE, 4.0f, 0.4f);
        }
        this.discard();
    }

    /** Never the caster, and never another core. */
    private boolean affects(LivingEntity candidate) {
        if (!candidate.isAlive() || candidate.isSpectator()) {
            return false;
        }
        return this.ownerUUID.map(id -> !id.equals(candidate.getUUID())).orElse(true);
    }

    private DamageSource damageSource() {
        Entity owner = this.ownerUUID
                .map(id -> this.level() instanceof ServerLevel serverLevel ? serverLevel.getEntity(id) : null)
                .orElse(null);
        return owner instanceof LivingEntity living
                ? this.damageSources().indirectMagic(this, living)
                : this.damageSources().magic();
    }

    private void spawnAmbientParticles() {
        double radius = this.getSize() * 1.4;
        for (int i = 0; i < 3; i++) {
            double angle = this.random.nextDouble() * Math.PI * 2;
            double height = (this.random.nextDouble() - 0.5) * radius * 2;
            this.level().addParticle(ParticleTypes.PORTAL,
                    this.getX() + Math.cos(angle) * radius,
                    this.getY() + height,
                    this.getZ() + Math.sin(angle) * radius,
                    -Math.cos(angle) * 0.4, -height * 0.1, -Math.sin(angle) * 0.4);
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.age = tag.getInt("Age");
        this.entityData.set(SIZE, tag.getFloat("Size"));
        this.ownerUUID = tag.hasUUID("OwnerUUID")
                ? Optional.of(tag.getUUID("OwnerUUID")) : Optional.empty();
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Age", this.age);
        tag.putFloat("Size", this.getSize());
        this.ownerUUID.ifPresent(uuid -> tag.putUUID("OwnerUUID", uuid));
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
