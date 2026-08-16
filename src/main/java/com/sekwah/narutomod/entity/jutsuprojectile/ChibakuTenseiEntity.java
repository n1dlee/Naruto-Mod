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

    /** Roughly forty-five blocks up, so it clears the treeline and reads from the ground. */
    private static final int RISE_TICKS = 90;
    private static final int HOLD_TICKS = 8 * 20;
    private static final double RISE_SPEED = 0.5;

    // --- Building the moon -------------------------------------------------------------
    /** How wide a circle it tears out of the ground beneath itself. */
    private static final double EXCAVATE_RADIUS = 26.0;
    /** Ticks between excavation passes, and how many blocks each pass lifts. */
    private static final int GATHER_INTERVAL = 2;
    private static final int GATHER_PER_PASS = 22;
    /** Hard ceiling on how much earth one core can ever move. */
    private static final int MAX_GATHERED = 2600;
    /**
     * How far down a single column is stripped before the pass moves on.
     *
     * Taking only the surface block, which is what this did, scrapes the grass off a field and
     * leaves it flat. The technique in the source tears the landscape open - so each column is
     * mined downward and a real crater forms under the core.
     */
    private static final int EXCAVATE_DEPTH = 6;

    /** Where every lifted block was put, so the sphere can be taken apart when it falls. */
    private final java.util.List<net.minecraft.core.BlockPos> gathered = new java.util.ArrayList<>();
    private int gatherRing = 1;
    /**
     * How far the core reaches, in blocks. This is a regional technique, not a room-sized
     * one - at this range the whole area is being dragged toward one point, which is the
     * only scale at which the technique means what its name means.
     */
    private static final double PULL_RADIUS = 128.0;
    /**
     * Ticks between sweeps. A 256-block box every tick is real work for no benefit, so the
     * pull is applied in bursts and the impulse below is scaled to match - the acceleration
     * a caught entity feels per second is the same either way.
     */
    private static final int PULL_INTERVAL = 4;
    /** Per sweep, at the rim. Closer in it is stronger; see tickPull. */
    private static final double PULL_STRENGTH = 0.11;
    private static final float CRUSH_DAMAGE = 3.5f;
    private static final double CRUSH_RADIUS = 3.0;

    /** What the core does after it has finished gathering: it comes down. */
    private static final double FALL_ACCELERATION = 0.06;
    private static final double MAX_FALL_SPEED = 1.6;
    /** The landing. Radius, damage at the centre, and how far the shock is felt. */
    private static final double IMPACT_RADIUS = 24.0;
    private static final float IMPACT_DAMAGE = 45f;
    private static final double SHOCKWAVE_RADIUS = 48.0;

    private double fallSpeed;

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
            if (this.age % PULL_INTERVAL == 0) {
                this.tickPull();
            }
            // Only once it has stopped climbing: the sphere is built around a fixed point,
            // because blocks placed while the core was still moving would be left behind it.
            if (this.age % GATHER_INTERVAL == 0 && this.age < RISE_TICKS + HOLD_TICKS) {
                this.gatherEarth();
            }
        }
        // Once it has gathered for long enough it stops holding itself up and drops. The
        // landing is the technique's actual finish - a core this size does not fade out.
        if (this.age >= RISE_TICKS + HOLD_TICKS) {
            this.tickFall();
        }
    }

    /**
     * Tears earth out of the ground in a widening circle and packs it around the core.
     *
     * This is the part of the technique the name is actually about, so unlike everything else
     * in this mod it does move terrain. It is bounded on purpose: a twelve-block circle, a
     * hard ceiling on how much it can ever lift, and it refuses anything with a block entity
     * behind it - chests, furnaces, spawners - so a core going off next to a base takes the
     * garden and not the storage room. Bedrock and fluids are left alone too.
     *
     * Everything it lifts is remembered, because the sphere has to come apart again when the
     * core falls; otherwise the moon would simply hang there once the fight was over.
     */
    private void gatherEarth() {
        if (this.gathered.size() >= MAX_GATHERED || !(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        int lifted = 0;
        int attempts = 0;
        while (lifted < GATHER_PER_PASS && attempts < GATHER_PER_PASS * 8) {
            attempts++;
            // Sample a random point on the current ring, so the crater widens evenly rather
            // than eating one wedge at a time.
            double angle = this.random.nextDouble() * Math.PI * 2;
            double radius = Math.min(EXCAVATE_RADIUS, this.gatherRing) * Math.sqrt(this.random.nextDouble());
            int x = net.minecraft.util.Mth.floor(this.getX() + Math.cos(angle) * radius);
            int z = net.minecraft.util.Mth.floor(this.getZ() + Math.sin(angle) * radius);
            int surface = serverLevel.getHeight(
                    net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;

            // Walk down the column until something liftable is found, rather than taking only
            // whatever is on top. Once the surface has gone this is what keeps the same spot
            // producing material, which is how the hole gets deeper instead of wider forever.
            net.minecraft.core.BlockPos from = null;
            net.minecraft.world.level.block.state.BlockState state = null;
            for (int depth = 0; depth < EXCAVATE_DEPTH; depth++) {
                net.minecraft.core.BlockPos candidate =
                        new net.minecraft.core.BlockPos(x, surface - depth, z);
                net.minecraft.world.level.block.state.BlockState found = serverLevel.getBlockState(candidate);
                if (this.canLift(serverLevel, candidate, found)) {
                    from = candidate;
                    state = found;
                    break;
                }
            }
            if (from == null) {
                continue;
            }
            net.minecraft.core.BlockPos to = this.nextShellPosition();
            if (!serverLevel.getBlockState(to).isAir()) {
                continue;
            }

            serverLevel.removeBlock(from, false);
            serverLevel.setBlock(to, state, net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
            this.gathered.add(to);
            lifted++;

            NarutoParticles.spawnBolt(serverLevel,
                    Vec3.atCenterOf(from), Vec3.atCenterOf(to), 1, 0.4, NarutoParticles.SHADOW_PURPLE);
        }
        if (lifted > 0) {
            this.gatherRing = Math.min((int) EXCAVATE_RADIUS, this.gatherRing + 1);
            serverLevel.playSound(null, this.blockPosition(),
                    net.minecraft.sounds.SoundEvents.STONE_BREAK, SoundSource.HOSTILE, 2.5f, 0.4f);
        }
    }

    /** Anything that is not someone's belongings, the world floor, or liquid. */
    private boolean canLift(ServerLevel level, net.minecraft.core.BlockPos pos,
                            net.minecraft.world.level.block.state.BlockState state) {
        if (state.isAir() || !state.getFluidState().isEmpty()) {
            return false;
        }
        if (state.hasBlockEntity()) {
            return false;
        }
        // Unbreakable by design, and anything the world says cannot be destroyed at all.
        return state.getDestroySpeed(level, pos) >= 0.0f;
    }

    /**
     * The next free spot on the shell around the core, walked outward in layers so the mass
     * builds as a ball rather than a spike.
     */
    private net.minecraft.core.BlockPos nextShellPosition() {
        int index = this.gathered.size();
        // Fibonacci sphere: successive indices land far apart on the surface, which fills the
        // shell evenly without needing to track which spots are already taken.
        double golden = Math.PI * (3.0 - Math.sqrt(5.0));
        int perShell = 60;
        int shell = index / perShell;
        int within = index % perShell;
        double y = 1.0 - (within / (double) (perShell - 1)) * 2.0;
        double ringRadius = Math.sqrt(Math.max(0.0, 1.0 - y * y));
        double theta = golden * within;
        double r = 2.0 + shell * 1.6;

        return net.minecraft.core.BlockPos.containing(
                this.getX() + Math.cos(theta) * ringRadius * r,
                this.getY() + y * r,
                this.getZ() + Math.sin(theta) * ringRadius * r);
    }

    /** Lets the gathered mass go, so nothing is left floating once the core is gone. */
    private void releaseEarth() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        for (net.minecraft.core.BlockPos pos : this.gathered) {
            if (!serverLevel.getBlockState(pos).isAir()) {
                serverLevel.removeBlock(pos, false);
            }
        }
        this.gathered.clear();
    }

    /**
     * Brings the core down and detonates it the moment it meets something solid.
     *
     * Checked against the block below rather than run through the physics engine: this entity
     * has noPhysics set so that it can hang in the air during the gather, and turning that off
     * mid-flight would have it fight its own collision box on the way down.
     */
    private void tickFall() {
        // The mass it gathered lets go the moment it starts down, so the moon does not stay
        // hanging in the sky after the core has landed.
        if (!this.gathered.isEmpty()) {
            this.releaseEarth();
        }
        this.fallSpeed = Math.min(MAX_FALL_SPEED, this.fallSpeed + FALL_ACCELERATION);
        double next = this.getY() - this.fallSpeed;
        net.minecraft.core.BlockPos below = net.minecraft.core.BlockPos.containing(
                this.getX(), next - this.getSize(), this.getZ());

        if (next - this.getSize() <= this.level().getMinBuildHeight()
                || !this.level().getBlockState(below).isAir()) {
            this.setPos(this.getX(), next, this.getZ());
            this.impact();
            return;
        }
        this.setPos(this.getX(), next, this.getZ());
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
            // Scaled by the sweep interval so a burst applies what four quiet ticks would have.
            Vec3 pull = toCore.normalize().scale(PULL_STRENGTH * (0.35 + falloff) * PULL_INTERVAL);
            caught.setDeltaMovement(caught.getDeltaMovement().add(pull));
            caught.hurtMarked = true;
            caught.fallDistance = 0.0f;

            // Anything that actually reaches the core is being ground against it.
            if (distance < CRUSH_RADIUS && this.age % 10 == 0) {
                caught.hurt(this.damageSource(), CRUSH_DAMAGE);
            }
            this.smother(caught, distance);
        }
    }

    /**
     * Being sealed inside the sphere.
     *
     * The technique does not kill by impact - it encases you in the middle of a growing ball
     * of rock, which is a slower and much worse way to go. Anything dragged inside the shell
     * now runs out of air: the vanilla air supply is drained, and once it is gone the target
     * takes drowning damage exactly as it would buried in sand.
     *
     * Vanilla's own air track is used rather than a custom meter so the bubble bar on the HUD
     * empties, respiration enchantments help, and anything already immune to suffocation - a
     * player in creative, an undead that does not breathe - stays immune without a special
     * case here.
     */
    private void smother(LivingEntity caught, double distance) {
        if (distance > SMOTHER_RADIUS || !caught.canBeSeenAsEnemy()) {
            return;
        }
        int air = caught.getAirSupply();
        if (air > -20) {
            caught.setAirSupply(air - SMOTHER_AIR_PER_TICK);
        }
        if (caught.getAirSupply() <= -20) {
            caught.setAirSupply(0);
            caught.hurt(caught.damageSources().drown(), SMOTHER_DAMAGE);
        }
    }

    /** Inside this the sphere has closed over you and there is no air left. */
    private static final double SMOTHER_RADIUS = 9.0;
    private static final int SMOTHER_AIR_PER_TICK = 8;
    private static final float SMOTHER_DAMAGE = 2.0f;

    /**
     * The landing.
     *
     * Everything it gathered arrives at once, so this is the heaviest single hit in the mod:
     * lethal at the centre, still enough to throw you clear out to two dozen blocks, and felt
     * as a shove for twice that. Terrain survives - the same rule every technique here
     * follows, because these get cast next to whatever someone has built.
     */
    private void impact() {
        // Belt and braces: if the core is destroyed some other way than falling, whatever it
        // was still holding up must not be left in the air.
        this.releaseEarth();
        if (this.level() instanceof ServerLevel serverLevel) {
            Vec3 core = this.position();

            for (LivingEntity caught : this.level().getEntitiesOfClass(LivingEntity.class,
                    new AABB(core, core).inflate(SHOCKWAVE_RADIUS), this::affects)) {
                double distance = caught.position().distanceTo(core);
                Vec3 away = caught.position().subtract(core);
                Vec3 push = (away.lengthSqr() < 1.0E-4 ? new Vec3(0, 1, 0) : away.normalize());

                if (distance <= IMPACT_RADIUS) {
                    float falloff = (float) Math.max(0.0, 1.0 - distance / IMPACT_RADIUS);
                    caught.hurt(this.damageSource(), IMPACT_DAMAGE * (0.35f + 0.65f * falloff));
                    caught.setDeltaMovement(push.x * 2.2 * falloff, 0.9 * falloff, push.z * 2.2 * falloff);
                } else {
                    // Outside the crater it is a shockwave: it moves you, it does not kill you.
                    double falloff = 1.0 - (distance - IMPACT_RADIUS) / (SHOCKWAVE_RADIUS - IMPACT_RADIUS);
                    caught.setDeltaMovement(caught.getDeltaMovement()
                            .add(push.x * 0.9 * falloff, 0.35 * falloff, push.z * 0.9 * falloff));
                }
                caught.hurtMarked = true;
            }

            for (double r = 4.0; r <= IMPACT_RADIUS; r += 4.0) {
                NarutoParticles.spawnRing(serverLevel, core, r, (int) (r * 5), NarutoParticles.SHADOW_PURPLE);
            }
            serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                    core.x, core.y + 1.0, core.z, 12, 5.0, 2.0, 5.0, 0.0);
            serverLevel.sendParticles(ParticleTypes.CAMPFIRE_SIGNAL_SMOKE,
                    core.x, core.y + 1.0, core.z, 120, 8.0, 1.0, 8.0, 0.05);
            // Two layers so it reads as an impact rather than a firework: the crack, and the
            // low roll under it that carries to whoever is only watching.
            serverLevel.playSound(null, this.blockPosition(), SoundEvents.GENERIC_EXPLODE,
                    SoundSource.HOSTILE, 8.0f, 0.35f);
            serverLevel.playSound(null, this.blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER,
                    SoundSource.HOSTILE, 6.0f, 0.5f);
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
