package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.damagetypes.NarutoDamageTypes;
import com.sekwah.narutomod.sounds.NarutoSounds;
import com.sekwah.narutomod.util.NarutoParticles;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;

public class ChidoriDashAbility extends Ability implements Ability.Cooldown {

    private static final float CHAKRA_COST = 40.0F;
    private static final double DASH_DISTANCE = 10.0D;
    private static final double HIT_RADIUS = 1.25D;
    private static final int WINDUP_TICKS = 4;

    @Override
    public ActivationType activationType() {
        return ActivationType.INSTANT;
    }

    /** The dash carries the read; the arm has no time to pose. */
    @Override
    public int castPoseTicks() {
        return 4;
    }

    @Override
    public long defaultCombo() {
        return 2121;
    }

    @Override
    public int getCooldown() {
        return 12 * 20;
    }
    // --- Phase 15: Nature Release ---
    @Override
    public String element() {
        return "lightning";
    }

    @Override
    public int elementLevelRequired() {
        return 9;
    }

    @Override
    public float elementXpReward() {
        return 30f;
    }


    /**
     * The dash you make WITH a Chidori, so a live one must not refuse it.
     *
     * Unlike Nagashi this does not require one — the lunge stands on its own — but casting it
     * with the lightning already lit is the normal way it happens, and that was blocked.
     */
    @Override
    public HandsBusy builtOn() {
        return HandsBusy.CHIDORI;
    }

    @Override
    public boolean handleCost(Player player, INinjaData ninjaData, int chargeAmount) {
        // Phase 15: lightning-nature mastery gates this now (was Uchiha + Sharingan only)
        if (ninjaData.getChakra() < CHAKRA_COST) {
            player.displayClientMessage(Component.translatable("jutsu.fail.notenoughchakra",
                    Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW)), true);
            return false;
        }
        ninjaData.useChakra(CHAKRA_COST, 20);
        return true;
    }

    @Override
    public void performServer(Player player, INinjaData ninjaData, int ticksActive) {
        ninjaData.setChidoriTicks(0);
        Vec3 look = player.getLookAngle().normalize();

        // Brief brace before the charge — crackling hand-flash sells "committing to the dash"
        // instead of an instant zero-tell lunge.
        if (player.level() instanceof ServerLevel serverLevel) {
            Vec3 bracePos = player.position().add(0, player.getBbHeight() * 0.65, 0).add(look.scale(0.4));
            serverLevel.sendParticles(NarutoParticles.CHIDORI_CYAN, bracePos.x, bracePos.y, bracePos.z, 10, 0.15, 0.15, 0.15, 0.02);
            serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, bracePos.x, bracePos.y, bracePos.z, 10, 0.2, 0.2, 0.2, 0.05);
        }

        ninjaData.scheduleDelayedTickEvent(p -> launchDash(p, ninjaData, look), WINDUP_TICKS);
    }

    private void launchDash(Player player, INinjaData ninjaData, Vec3 look) {
        Vec3 start = player.position().add(0.0D, player.getBbHeight() * 0.5D, 0.0D);
        Vec3 end = start.add(look.scale(DASH_DISTANCE));
        BlockHitResult blockHit = player.level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 destination = blockHit.getType() == HitResult.Type.MISS ? end : blockHit.getLocation().subtract(look.scale(0.45D));
        double distance = start.distanceTo(destination);

        List<LivingEntity> targets = findTargets(player, start, look, distance);
        DamageSource source = NarutoDamageTypes.getDamageSource(player.level(), NarutoDamageTypes.CHIDORI, player, player);
        for (LivingEntity target : targets) {
            damageTarget(target, source, ninjaData);
        }

        player.setDeltaMovement(look.x * 2.4D, Math.max(look.y * 0.5D, 0.08D), look.z * 2.4D);
        player.hurtMarked = true;

        if (player.level() instanceof ServerLevel serverLevel) {
            spawnDashParticles(serverLevel, start, destination);
        }
    }

    @Override
    public SoundEvent castingSound() {
        return NarutoSounds.CHIDORI.get();
    }

    private List<LivingEntity> findTargets(Player player, Vec3 start, Vec3 look, double distance) {
        Vec3 path = look.scale(distance);
        AABB searchBox = player.getBoundingBox().expandTowards(path).inflate(HIT_RADIUS);
        return player.level().getEntitiesOfClass(LivingEntity.class, searchBox, entity -> {
                    if (entity == player || entity.isSpectator() || !entity.isAlive()) {
                        return false;
                    }
                    Vec3 relative = entity.position().add(0.0D, entity.getBbHeight() * 0.5D, 0.0D).subtract(start);
                    double projection = relative.dot(look);
                    if (projection < 0.0D || projection > distance) {
                        return false;
                    }
                    Vec3 closest = start.add(look.scale(projection));
                    Vec3 targetCenter = entity.position().add(0.0D, entity.getBbHeight() * 0.5D, 0.0D);
                    return targetCenter.distanceTo(closest) <= HIT_RADIUS;
                }).stream()
                .sorted(Comparator.comparingDouble(entity -> entity.position().subtract(start).dot(look)))
                .toList();
    }

    private void damageTarget(LivingEntity target, DamageSource source, INinjaData ninjaData) {
        float damageMultiplier = ninjaData.getRankDamageMultiplier() * ninjaData.getClanLightningDamageMultiplier();
        if (target instanceof Player targetPlayer) {
            float damage = 16.0F * damageMultiplier;
            if (ninjaData.getNinjaRank() < 4) {
                damage = Math.min(damage, targetPlayer.getHealth() - 1.0F);
            }
            if (damage > 0.0F) {
                target.hurt(source, damage);
            }
        } else {
            target.hurt(source, 22.0F * damageMultiplier);
        }
    }

    private void spawnDashParticles(ServerLevel serverLevel, Vec3 start, Vec3 end) {
        Vec3 diff = end.subtract(start);
        int steps = Math.max(12, (int) (diff.length() * 2.0D));
        for (int i = 0; i <= steps; i++) {
            Vec3 pos = start.add(diff.scale(i / (double) steps));
            serverLevel.sendParticles(NarutoParticles.CHIDORI_CYAN, pos.x, pos.y, pos.z, 1, 0.08D, 0.08D, 0.08D, 0.02D);
            serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, pos.x, pos.y, pos.z, 2, 0.08D, 0.08D, 0.08D, 0.04D);
        }
    }
}
