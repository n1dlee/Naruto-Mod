package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.util.NarutoParticles;
import com.sekwah.narutomod.world.KamuiDimension;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * Kamui: Jikukan Ido - the space-time transfer that pulls the wielder into their own
 * pocket dimension, and casting it again brings them back out.
 *
 * This is the technique's real point in the story: not a dodge, but somewhere to keep
 * things. It is the escape hatch Obito uses when a fight has gone wrong, so the return
 * trip lands you exactly where you left rather than at a spawn point.
 */
public class KamuiWarpAbility extends Ability implements Ability.Cooldown {

    private static final float CHAKRA_COST = 120f;
    private static final int COOLDOWN = 100;
    /** How far the eye can reach to take someone with it. */
    private static final double TARGET_RANGE = 24.0;
    private static final DustParticleOptions KAMUI_VIOLET =
            new DustParticleOptions(new Vector3f(0.45F, 0.15F, 0.65F), 1.6F);

    @Override
    public ActivationType activationType() {
        return ActivationType.INSTANT;
    }

    /** A short spiral out, not a performance. */
    @Override
    public int castPoseTicks() {
        return 5;
    }

    @Override
    public long defaultCombo() {
        return 1313;
    }

    @Override
    public String requiredEye() {
        return "sharingan_ms";
    }

    @Override
    public String requiredEyeForm() {
        return "obito";
    }

    @Override
    public int getCooldown() {
        return COOLDOWN;
    }

    @Override
    public SoundEvent castingSound() {
        return SoundEvents.SHULKER_TELEPORT;
    }

    @Override
    public boolean handleCost(Player player, INinjaData ninjaData, int chargeAmount) {
        // Leaving is always free. Being stuck in a black void because you spent your chakra
        // getting there would be a trap, not a technique.
        if (KamuiDimension.isKamui(player.level())) {
            return true;
        }
        if (ninjaData.getChakra() < CHAKRA_COST) {
            player.displayClientMessage(Component.translatable("jutsu.fail.notenoughchakra",
                    Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW)), true);
            return false;
        }
        ninjaData.useChakra(CHAKRA_COST, 40);
        return true;
    }

    @Override
    public void performServer(Player player, INinjaData ninjaData, int ticksActive) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        spawnVortex(serverPlayer);

        if (KamuiDimension.isKamui(serverPlayer.level())) {
            KamuiDimension.exit(serverPlayer, ninjaData);
            serverPlayer.displayClientMessage(
                    Component.translatable("jutsu.kamui.returned").withStyle(ChatFormatting.LIGHT_PURPLE), true);
            spawnVortex(serverPlayer);
            return;
        }

        // Looking at something? It goes instead of you. This is the version of the technique
        // that actually won fights - the target is not damaged, they are simply removed.
        LivingEntity victim = findTarget(serverPlayer);
        if (victim != null) {
            Vec3 lastSeen = victim.position();
            if (KamuiDimension.banish(serverPlayer, victim)) {
                spawnVortexAt(serverPlayer.serverLevel(), lastSeen, victim.getBbHeight());
                serverPlayer.displayClientMessage(Component.translatable("jutsu.kamui.banished",
                        victim.getDisplayName()).withStyle(ChatFormatting.LIGHT_PURPLE), true);
                return;
            }
        }
        if (!KamuiDimension.enter(serverPlayer, ninjaData)) {
            serverPlayer.displayClientMessage(
                    Component.translatable("jutsu.kamui.nodimension").withStyle(ChatFormatting.RED), true);
            return;
        }
        serverPlayer.displayClientMessage(
                Component.translatable("jutsu.kamui.entered").withStyle(ChatFormatting.LIGHT_PURPLE), true);
        spawnVortex(serverPlayer);
    }

    /** The nearest living thing in the crosshair, within reach of the eye. */
    private LivingEntity findTarget(ServerPlayer player) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 end = eye.add(look.scale(TARGET_RANGE));

        HitResult blockHit = player.pick(TARGET_RANGE, 0.0F, false);
        double blockDistance = blockHit.getType() == HitResult.Type.MISS
                ? Double.MAX_VALUE
                : eye.distanceToSqr(blockHit.getLocation());

        EntityHitResult hit = ProjectileUtil.getEntityHitResult(player.level(), player, eye, end,
                player.getBoundingBox().expandTowards(look.scale(TARGET_RANGE)).inflate(1.0D),
                entity -> entity != player && !entity.isSpectator() && entity.isPickable());
        if (hit != null && eye.distanceToSqr(hit.getLocation()) <= blockDistance
                && hit.getEntity() instanceof LivingEntity living) {
            return living;
        }
        return null;
    }

    /** The spiral that swallows the caster, drawn on both sides of the trip. */
    private void spawnVortex(ServerPlayer player) {
        if (player.level() instanceof ServerLevel serverLevel) {
            spawnVortexAt(serverLevel, player.position(), player.getBbHeight());
        }
    }

    private void spawnVortexAt(ServerLevel level, Vec3 origin, float height) {
        for (int i = 0; i < 40; i++) {
            double angle = (Math.PI * 2 * i) / 20.0;
            double radius = 1.6 - (i / 40.0) * 1.5;
            level.sendParticles(KAMUI_VIOLET,
                    origin.x + Math.cos(angle) * radius,
                    origin.y + (i / 40.0) * height,
                    origin.z + Math.sin(angle) * radius,
                    1, 0.0, 0.0, 0.0, 0.0);
        }
        level.sendParticles(NarutoParticles.SHARINGAN_RED,
                origin.x, origin.y + height * 0.9, origin.z, 6, 0.2, 0.2, 0.2, 0.0);
    }
}
