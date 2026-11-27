package com.sekwah.narutomod.entity;

import com.sekwah.narutomod.events.SummonRitualEvents;
import com.sekwah.narutomod.sounds.NarutoSounds;
import com.sekwah.narutomod.util.AnimalPathDrive;
import com.sekwah.narutomod.util.AnimalPathDrive.Stage;
import com.sekwah.narutomod.util.Faction;
import com.sekwah.narutomod.util.JutsuAim;
import com.sekwah.narutomod.util.KamuiStep;
import com.sekwah.narutomod.util.SummoningCircle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class AnimalPathRhinoEntity extends PathfinderMob {
    private static final EntityDataAccessor<Optional<UUID>> OWNER = SynchedEntityData.defineId(AnimalPathRhinoEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Integer> FRAME = SynchedEntityData.defineId(AnimalPathRhinoEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> LOCK = SynchedEntityData.defineId(AnimalPathRhinoEntity.class, EntityDataSerializers.INT);
    private static final Stage[] STAGES = Stage.values();
    private static final int SHIFT = 11;
    private static final int MASK = (1 << SHIFT) - 1;
    private final Set<UUID> struck = new HashSet<>();
    private long expires;
    private long commandExpires;
    private float chargeYaw;
    private Vec3 lastStep = Vec3.ZERO;

    public AnimalPathRhinoEntity(EntityType<AnimalPathRhinoEntity> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
        this.setMaxUpStep(0.85f);
    }

    public static @Nullable AnimalPathRhinoEntity owned(ServerPlayer actor) {
        for (var beast : actor.serverLevel().getEntities(NarutoEntities.ANIMAL_RHINO.get(), rhino ->
                rhino.isAlive() && rhino.stage() != Stage.FADE && rhino.ownerId().filter(actor.getUUID()::equals).isPresent())) {
            return beast;
        }
        return null;
    }

    public void bond(ServerPlayer actor) {
        this.entityData.set(OWNER, Optional.of(actor.getUUID()));
        this.expires = actor.serverLevel().getGameTime() + AnimalPathDrive.LIFE;
    }

    public boolean mark(ServerPlayer owner) {
        if (this.level() != owner.level() || this.ownerId().filter(owner.getUUID()::equals).isEmpty()) {
            return false;
        }
        LivingEntity target = JutsuAim.select(owner, 23, 18, actor -> this.validTarget(owner, actor));
        if (target == null) {
            return false;
        }
        this.commandExpires = owner.serverLevel().getGameTime() + 100;
        lock(target);
        return true;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(OWNER, Optional.empty());
        this.entityData.define(FRAME, 0);
        this.entityData.define(LOCK, 0);
    }

    public Optional<UUID> ownerId() { return this.entityData.get(OWNER); }

    public Stage stage() { return STAGES[this.entityData.get(FRAME) >> SHIFT]; }

    public int age() { return this.entityData.get(FRAME) & MASK; }

    public int markPulse() { return this.entityData.get(LOCK); }

    public static AttributeSupplier.Builder createAttributes() {
        return createMobAttributes().add(Attributes.MAX_HEALTH, 105).add(Attributes.MOVEMENT_SPEED, 0.27)
                .add(Attributes.FOLLOW_RANGE, 29).add(Attributes.KNOCKBACK_RESISTANCE, 0.76)
                .add(Attributes.ATTACK_DAMAGE, 11);
    }

    @Override
    public void tick() {
        super.tick();
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        Stage stage = this.stage();
        int age = this.age() + 1;
        frame(stage, age);
        int pulse = this.markPulse();
        if (pulse > 0) {
            this.entityData.set(LOCK, pulse - 1);
        }
        if (stage == Stage.FADE) {
            if (age >= AnimalPathDrive.FADE) {
                this.discard();
            }
            return;
        }
        if (!this.isAlive() || this.isInWaterOrBubble()
                || !(this.ownerId().map(level::getPlayerByUUID).orElse(null) instanceof ServerPlayer owner)
                || !AnimalPathDrive.liveBond(owner.isAlive() && !owner.isSpectator(), owner.level() == level,
                level.getGameTime(), this.expires) || owner.distanceToSqr(this) > 46 * 46) {
            dissolve();
            return;
        }
        switch (stage) {
            case BIRTH -> {
                if (age >= AnimalPathDrive.BIRTH) {
                    frame(Stage.PROWL, 0);
                }
            }
            case PROWL -> prowl(level, owner, age);
            case BRACE -> brace(level, owner, age);
            case RAM -> ram(level, owner, age);
            case REST -> {
                if (age >= AnimalPathDrive.REST) {
                    frame(Stage.PROWL, 0);
                }
            }
            default -> { }
        }
    }

    private void prowl(ServerLevel level, ServerPlayer owner, int age) {
        if (age % 7 != 1) {
            return;
        }
        var target = this.getTarget();
        if (target == null || !validTarget(owner, target)
                || !(target instanceof Enemy) && level.getGameTime() >= this.commandExpires) {
            target = level.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(15),
                            entry -> entry instanceof Enemy && validTarget(owner, entry) && this.hasLineOfSight(entry))
                    .stream().min(Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
            if (target != null) {
                lock(target);
            } else {
                this.setTarget(null);
            }
        }
        if (target == null) {
            if (this.distanceToSqr(owner) > 5.5 * 5.5) {
                this.getNavigation().moveTo(owner, 0.83);
            }
            return;
        }
        Vec3 offset = target.position().subtract(this.position());
        if (this.onGround() && AnimalPathDrive.canBrace(offset, this.hasLineOfSight(target))) {
            this.getNavigation().stop();
            this.chargeYaw = (float) Math.toDegrees(Math.atan2(-offset.x, offset.z));
            turn();
            this.struck.clear();
            frame(Stage.BRACE, 0);
            blocks(level, this.position(), 6);
            level.playSound(null, this, NarutoSounds.ANIMAL_RAM.get(), SoundSource.PLAYERS, 0.58f, 0.68f);
        } else {
            this.getLookControl().setLookAt(target, 32, 24);
            this.getNavigation().moveTo(target, 0.96);
        }
    }

    private void brace(ServerLevel level, ServerPlayer owner, int age) {
        this.getNavigation().stop();
        turn();
        var target = this.getTarget();
        if (!this.onGround() || target == null || !validTarget(owner, target)) {
            frame(Stage.REST, 0);
        } else if (age >= AnimalPathDrive.BRACE) {
            this.lastStep = this.position();
            frame(Stage.RAM, 0);
            advance(0);
            blocks(level, this.position(), 8);
        }
    }

    private void ram(ServerLevel level, ServerPlayer owner, int age) {
        turn();
        Vec3 from = this.lastStep;
        Vec3 to = this.position();
        if (!this.onGround() || this.horizontalCollision || age >= AnimalPathDrive.RAM
                || from.distanceToSqr(to) > 1.6) {
            frame(Stage.REST, 0);
            this.setDeltaMovement(0, this.getDeltaMovement().y, 0);
            if (this.horizontalCollision) {
                blocks(level, to, 8);
            }
            return;
        }
        this.lastStep = to;
        contact(level, owner, from, to);
        if ((age & 1) == 0) {
            blocks(level, to, 4);
        }
        advance(age);
    }

    private void contact(ServerLevel level, ServerPlayer owner, Vec3 from, Vec3 to) {
        var axis = KamuiStep.facing(this.chargeYaw);
        Vec3 begin = from.add(axis.scale(0.84)).add(0, 1.12, 0);
        Vec3 end = to.add(axis.scale(0.84)).add(0, 1.12, 0);
        var scan = new AABB(begin, end).inflate(0.70);
        for (var victim : level.getEntitiesOfClass(LivingEntity.class, scan,
                target -> validTarget(owner, target) && (target instanceof Enemy || this.getTarget() == target)
                        && this.hasLineOfSight(target))) {
            if (com.sekwah.narutomod.util.BoulderRoll.contact(begin, end, victim.getBoundingBox(), 0.58) != null
                    && this.struck.add(victim.getUUID()) && victim.hurt(this.damageSources().mobAttack(this), victim instanceof Player ? 7 : 11)) {
                victim.push(axis.x * 0.51, 0.18, axis.z * 0.51);
                victim.hurtMarked = true;
            }
        }
    }

    private void advance(int age) {
        Vec3 direction = KamuiStep.facing(this.chargeYaw).scale(AnimalPathDrive.tempo(age));
        this.setDeltaMovement(direction.x, this.getDeltaMovement().y, direction.z);
    }

    private void turn() {
        this.setYRot(this.chargeYaw);
        this.setYBodyRot(this.chargeYaw);
        this.setYHeadRot(this.chargeYaw);
    }

    private void lock(LivingEntity target) {
        if (target != this.getTarget()) {
            this.setTarget(target);
            this.entityData.set(LOCK, 9);
        }
    }

    private boolean validTarget(ServerPlayer owner, LivingEntity target) {
        return target != this && !Faction.sameSide(this, target) && JutsuAim.attackable(owner, target)
                && (!(target instanceof Player) || owner.serverLevel().getServer().isPvpAllowed())
                && owner.level().mayInteract(owner, target.blockPosition()) && this.distanceToSqr(target) < 29 * 29;
    }

    public void dissolve() {
        if (this.level() instanceof ServerLevel level && this.stage() != Stage.FADE) {
            this.getNavigation().stop();
            this.setTarget(null);
            this.setDeltaMovement(Vec3.ZERO);
            this.setNoAi(true);
            frame(Stage.FADE, 0);
            this.ownerId().map(level::getPlayerByUUID).filter(ServerPlayer.class::isInstance).map(ServerPlayer.class::cast)
                    .ifPresent(owner -> SummonRitualEvents.publish(owner, level, this.position(), SummoningCircle.Style.RHINO,
                            SummoningCircle.Stage.FADE, AnimalPathDrive.WIDTH));
        }
    }

    private void blocks(ServerLevel level, Vec3 position, int amount) {
        BlockPos ground = BlockPos.containing(position).below();
        if (level.hasChunkAt(ground) && !level.isEmptyBlock(ground)) {
            var material = new BlockParticleOption(ParticleTypes.BLOCK, level.getBlockState(ground));
            level.sendParticles(material, position.x, position.y + 0.08, position.z, amount, 0.57, 0.13, 0.57, 0.07);
        }
    }

    private void frame(Stage stage, int age) { this.entityData.set(FRAME, stage.ordinal() << SHIFT | Math.min(age, MASK)); }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.getEntity() != null && Faction.sameSide(this, source.getEntity())) {
            return false;
        }
        return super.hurt(source, amount);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() { return NetworkHooks.getEntitySpawningPacket(this); }
}
