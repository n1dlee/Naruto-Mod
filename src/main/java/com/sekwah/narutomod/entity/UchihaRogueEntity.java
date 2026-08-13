package com.sekwah.narutomod.entity;

import com.sekwah.narutomod.item.NarutoItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

import javax.annotation.Nullable;

/**
 * A missing-nin of the Uchiha, and the reason a player born into any other clan can ever
 * own a Sharingan.
 *
 * The Mangekyo bosses already drop eyes, but they are S-rank set-pieces - gating the entire
 * dojutsu branch behind them meant a non-Uchiha had no realistic path in at all. These are
 * the ordinary Uchiha: rarer than a plain rogue and stingier with their eyes than a boss,
 * but findable.
 *
 * A separate entity type rather than another clan byte on RogueNinjaEntity purely so it can
 * carry its own spawn weight - clan is rolled per-spawn, which cannot express "these appear
 * less often than those".
 */
public class UchihaRogueEntity extends RogueNinjaEntity {

    /**
     * Half the 0.35 an Uchiha boss drops at. An eye should still be a find, and these are
     * far more common than a boss.
     */
    private static final float EYE_DROP_CHANCE = 0.10f;

    public UchihaRogueEntity(EntityType<? extends UchihaRogueEntity> type, Level level) {
        super(type, level);
    }

    /**
     * Uchiha rogues never carry one of the other bloodlines - they have their own, and a
     * Hyuga-Uchiha would be nonsense. Their Sharingan is expressed through the dodge below
     * rather than through a clan technique goal.
     */
    @Override
    protected void rollClan(RandomSource random) {
        this.setClanId(CLAN_NONE);
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                        MobSpawnType reason, @Nullable SpawnGroupData data,
                                        @Nullable CompoundTag tag) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, reason, data, tag);
        // The eye takes years to mature, so nobody carrying one is still a Chunin.
        this.setNinjaRank(RANK_JONIN);
        this.setCustomName(Component.translatable("entity.narutomod.uchiha_rogue"));
        return result;
    }

    /**
     * The Sharingan reads an attack before it lands. Same 60% a fully matured eye gives the
     * player, rate-limited so it thins incoming damage instead of making them untouchable.
     */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!this.level().isClientSide
                && this.sharinganDodgeCooldown <= 0
                && source.getEntity() != null
                && !source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)
                && this.getRandom().nextFloat() < 0.6f) {
            this.sharinganDodgeCooldown = 40;
            sidestep(source);
            return false;
        }
        return super.hurt(source, amount);
    }

    private int sharinganDodgeCooldown;

    /** Steps out of the line of the blow rather than teleporting - it has to look earned. */
    private void sidestep(DamageSource source) {
        if (source.getEntity() != null) {
            net.minecraft.world.phys.Vec3 away = this.position()
                    .subtract(source.getEntity().position()).normalize();
            net.minecraft.world.phys.Vec3 side = new net.minecraft.world.phys.Vec3(-away.z, 0, away.x)
                    .scale(this.getRandom().nextBoolean() ? 0.9 : -0.9);
            this.setDeltaMovement(side.x, 0.35, side.z);
            this.hurtMarked = true;
        }
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(com.sekwah.narutomod.util.NarutoParticles.SHARINGAN_RED,
                    this.getX(), this.getEyeY(), this.getZ(), 8, 0.2, 0.2, 0.2, 0.0);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide && this.sharinganDodgeCooldown > 0) {
            this.sharinganDodgeCooldown--;
        }
        // A live Sharingan glints even at rest - the tell that this one is worth killing.
        if (this.level().isClientSide && this.tickCount % 10 == 0) {
            this.level().addParticle(com.sekwah.narutomod.util.NarutoParticles.SHARINGAN_RED,
                    this.getX(), this.getEyeY(), this.getZ(), 0.0, 0.0, 0.0);
        }
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHit) {
        super.dropCustomDeathLoot(source, looting, recentlyHit);
        if (this.random.nextFloat() < EYE_DROP_CHANCE) {
            this.spawnAtLocation(new ItemStack(NarutoItems.SHARINGAN_EYE.get()));
        }
    }
}
