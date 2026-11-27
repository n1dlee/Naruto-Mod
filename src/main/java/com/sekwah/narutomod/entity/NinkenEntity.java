package com.sekwah.narutomod.entity;

import com.sekwah.narutomod.network.PacketHandler;
import com.sekwah.narutomod.network.s2c.NinkenCuePacket;
import com.sekwah.narutomod.network.s2c.NinkenCuePacket.Stage;
import com.sekwah.narutomod.sounds.NarutoSounds;
import com.sekwah.narutomod.util.JutsuAim;
import com.sekwah.narutomod.util.KamuiStep;
import com.sekwah.narutomod.util.NinkenRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.Comparator;
import java.util.UUID;

public final class NinkenEntity extends Wolf {
    private static final int OFFLINE_GRACE = 120;
    private float rank = 1;
    private int absent;
    private int drilled;
    private @Nullable UUID scent;
    private long scentUntil;

    public NinkenEntity(EntityType<? extends Wolf> type, Level level) {
        super(type, level);
    }

    public void bind(Player owner, float rank) {
        this.tame(owner);
        this.rank = Mth.clamp(rank, 0.6f, 2.1f);
        this.setCollarColor(DyeColor.CYAN);
        this.setOrderedToSit(false);
        this.setPersistenceRequired();
        this.setCustomName(Component.translatable("entity.narutomod.ninken.named", owner.getDisplayName()));
        this.applyStats(28);
    }

    private void applyStats(float healthNow) {
        var health = this.getAttribute(Attributes.MAX_HEALTH);
        var bite = this.getAttribute(Attributes.ATTACK_DAMAGE);
        var speed = this.getAttribute(Attributes.MOVEMENT_SPEED);
        if (health != null) {
            health.setBaseValue(28);
        }
        if (bite != null) {
            bite.setBaseValue(4.2 * this.rank);
        }
        if (speed != null) {
            speed.setBaseValue(0.34);
        }
        this.setHealth(Math.min(healthNow, this.getMaxHealth()));
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(3, new ScentRoute());
    }

    @Override
    public void tick() {
        if (this.isAlive() && this.level() instanceof ServerLevel server && !this.maintainOwner(server)) {
            return;
        }
        super.tick();
        if (this.drilled > 0) {
            this.drilled--;
            this.getNavigation().stop();
        }
    }

    private boolean maintainOwner(ServerLevel level) {
        UUID owner = this.getOwnerUUID();
        var registry = NinkenRegistry.of(level);
        if (owner == null || !this.getUUID().equals(registry.active(owner, NinkenRegistry.time(level)))) {
            this.sendHome(level);
            return false;
        }
        var player = level.getServer().getPlayerList().getPlayer(owner);
        if (player == null) {
            this.scent = null;
            this.setTarget(null);
            this.getNavigation().stop();
            if (++this.absent >= OFFLINE_GRACE) {
                this.sendHome(level);
                return false;
            }
        } else if (player.level() != level || !player.isAlive()) {
            this.sendHome(level);
            return false;
        } else {
            this.absent = 0;
        }
        return true;
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (this.isOwnedBy(player) && player.isCrouching() && player.getItemInHand(hand).isEmpty()) {
            if (player instanceof ServerPlayer server && this.level() instanceof ServerLevel level) {
                this.askScent(server, level);
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }
        return super.mobInteract(player, hand);
    }

    private void askScent(ServerPlayer player, ServerLevel level) {
        LivingEntity target = this.drilled > 0 ? null : this.closestScent(player);
        if (target == null) {
            player.displayClientMessage(Component.translatable("jutsu.ninken.no_scent"), true);
            return;
        }
        this.scent = target.getUUID();
        this.scentUntil = level.getGameTime() + 76;
        this.setOrderedToSit(false);
        this.getNavigation().stop();
        level.playSound(null, this.blockPosition(), NarutoSounds.NINKEN_TRACK.get(), SoundSource.NEUTRAL, 0.49f, 1);
    }

    private @Nullable LivingEntity closestScent(ServerPlayer player) {
        var facing = KamuiStep.facing(player.getYRot());
        var eye = player.position().add(0, 0.65, 0);
        return this.level().getEntitiesOfClass(Monster.class, this.getBoundingBox().inflate(12),
                candidate -> JutsuAim.attackable(player, candidate)
                        && JutsuAim.aimed(eye, facing, candidate.position().add(0, 0.65, 0), 12, 0.62)
                        && this.distanceToSqr(candidate) <= 12 * 12 && this.hasLineOfSight(candidate))
                .stream().min(Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
    }

    public void braceForDrill(int ticks) {
        this.drilled = ticks;
        this.scent = null;
        this.getNavigation().stop();
        this.setTarget(null);
    }

    public boolean readyForDrill() {
        return this.isAlive() && !this.isOrderedToSit() && !this.isInWaterOrBubble() && !this.isPassenger() && this.drilled <= 0;
    }

    @Override
    public boolean canMate(Animal partner) {
        return false;
    }

    private void sendHome(ServerLevel level) {
        this.release(level);
        PacketHandler.sendNear(new NinkenCuePacket(level.dimension().location(), this.random.nextInt(),
                Stage.SEND_HOME, this.position(), KamuiStep.facing(this.getYRot())), level, this.position(), 30);
        level.playSound(null, this.blockPosition(), NarutoSounds.NINKEN_DISMISS.get(), SoundSource.NEUTRAL, 0.61f, 1);
        this.discard();
    }

    private void release(ServerLevel level) {
        UUID owner = this.getOwnerUUID();
        if (owner != null) {
            NinkenRegistry.of(level).release(owner, this.getUUID());
        }
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (this.level() instanceof ServerLevel level) {
            this.release(level);
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("NinkenRank", this.rank);
        tag.putInt("NinkenAbsent", this.absent);
        if (this.scent != null) {
            tag.putUUID("NinkenScent", this.scent);
            tag.putLong("NinkenScentUntil", this.scentUntil);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.rank = tag.contains("NinkenRank") ? Mth.clamp(tag.getFloat("NinkenRank"), 0.6f, 2.1f) : 1;
        this.absent = Mth.clamp(tag.getInt("NinkenAbsent"), 0, OFFLINE_GRACE);
        this.scent = tag.hasUUID("NinkenScent") ? tag.getUUID("NinkenScent") : null;
        this.scentUntil = tag.getLong("NinkenScentUntil");
        this.applyStats(tag.contains("Health") ? tag.getFloat("Health") : 28);
    }

    private final class ScentRoute extends Goal {
        private ScentRoute() {
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return this.target() != null;
        }

        private @Nullable LivingEntity target() {
            if (!(level() instanceof ServerLevel level) || scent == null || level.getGameTime() >= scentUntil
                    || drilled > 0 || getTarget() != null || isOrderedToSit()
                    || !(getOwner() instanceof ServerPlayer owner) || owner.distanceToSqr(NinkenEntity.this) > 14 * 14) {
                return null;
            }
            return level.getEntity(scent) instanceof Monster mob && JutsuAim.attackable(owner, mob) ? mob : null;
        }

        @Override
        public void tick() {
            LivingEntity target = this.target();
            if (target == null) {
                return;
            }
            getLookControl().setLookAt(target, 30, 25);
            if (tickCount % 8 == 0 && getOwner() instanceof ServerPlayer player) {
                boolean path = getNavigation().moveTo(target, 1.12);
                if (path && hasLineOfSight(target)) {
                    var cue = new NinkenCuePacket(level().dimension().location(), random.nextInt(), Stage.SCENT,
                            position(), target.position().subtract(position()).normalize());
                    PacketHandler.sendToPlayer(cue, player);
                }
            }
            if (distanceToSqr(target) < 4.5) {
                scent = null;
                getNavigation().stop();
            }
        }

        @Override
        public void stop() {
            getNavigation().stop();
        }
    }
}
