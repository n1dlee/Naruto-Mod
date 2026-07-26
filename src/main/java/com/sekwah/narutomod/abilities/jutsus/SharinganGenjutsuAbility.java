package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.util.NarutoParticles;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Sharingan — Genjutsu: Illusion (combo 1123).
 * The player looks at a target (raycast 10 blocks) and catches them in an illusion.
 * Requires active Sharingan.
 * Target effects (4 seconds):
 *   - Blindness II
 *   - Weakness II
 *   - Mining Fatigue II
 * Players hit also receive a brief nausea effect.
 * Red spinning dust particles appear at the target's eye level.
 * Cost: 60 chakra. Cooldown: 30 seconds.
 */
public class SharinganGenjutsuAbility extends Ability implements Ability.Cooldown {

    private static final float CHAKRA_COST = 60f;
    private static final double RANGE = 10.0;
    private static final int EFFECT_TICKS = 4 * 20;

    @Override
    public ActivationType activationType() {
        return ActivationType.INSTANT;
    }

    @Override
    public long defaultCombo() {
        return 1123;
    }

    @Override
    public int getCooldown() {
        return 30 * 20;
    }

    @Override
    public SoundEvent castingSound() {
        return SoundEvents.EVOKER_CAST_SPELL;
    }

    /** Canon: casting genjutsu through the eye needs a Sharingan matured to two tomoe. */
    @Override
    public String requiredEye() {
        return "sharingan_tomoe2";
    }

    @Override
    public boolean handleCost(Player player, INinjaData ninjaData, int chargeAmount) {
        if (!ninjaData.isSharinganActive()) {
            player.displayClientMessage(Component.translatable("jutsu.fail.sharingan.active",
                    Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW)), true);
            return false;
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
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        Vec3 end = eye.add(look.scale(RANGE));

        // Block clip
        Vec3 blockEnd = end;
        var blockHit = player.level().clip(
                new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (blockHit.getType() != HitResult.Type.MISS) {
            blockEnd = blockHit.getLocation();
        }

        // Entity raycast
        double maxDist = eye.distanceTo(blockEnd);
        AABB searchBox = new AABB(
                Math.min(eye.x, blockEnd.x) - 1, eye.y - 2, Math.min(eye.z, blockEnd.z) - 1,
                Math.max(eye.x, blockEnd.x) + 1, eye.y + 4, Math.max(eye.z, blockEnd.z) + 1);

        LivingEntity target = player.level().getEntitiesOfClass(LivingEntity.class, searchBox,
                        e -> e != player && e.isAlive()).stream()
                .filter(e -> {
                    Vec3 toE = e.position().add(0, e.getBbHeight() * 0.5, 0).subtract(eye);
                    double proj = toE.dot(look);
                    if (proj < 0 || proj > maxDist) return false;
                    return e.position().add(0, e.getBbHeight() * 0.5, 0)
                            .distanceTo(eye.add(look.scale(proj))) <= 1.2;
                })
                .min(java.util.Comparator.comparingDouble(e -> e.position().distanceTo(eye)))
                .orElse(null);

        if (target == null) return;

        // Apply genjutsu effects
        target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, EFFECT_TICKS, 1, false, true));
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, EFFECT_TICKS, 1, false, true));
        target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, EFFECT_TICKS, 1, false, false));
        if (target instanceof Player) {
            target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, EFFECT_TICKS, 0, false, false));
        }

        // Red Sharingan particles swirling at target's eyes
        if (player.level() instanceof ServerLevel serverLevel) {
            Vec3 targetEye = target.getEyePosition();
            for (int i = 0; i < 20; i++) {
                double angle = Math.toRadians(i * 18.0);
                double px = targetEye.x + 0.5 * Math.cos(angle);
                double pz = targetEye.z + 0.5 * Math.sin(angle);
                serverLevel.sendParticles(NarutoParticles.GENJUTSU_RED, px, targetEye.y, pz, 1, 0.04, 0.04, 0.04, 0.0);
            }
        }
    }
}
