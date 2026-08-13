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
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;

/**
 * Nara Clan — Shadow Sewing Technique / Kage Nui (combo 332).
 * Unlike Shadow Imitation (which only immobilises and mirrors), Kage Nui materialises
 * the shadow into several sharp tendrils that attack and PIN multiple targets at once —
 * the shadow physically sews them in place.
 *
 * Up to 5 living targets within range are struck by a shadow tendril crawling along the
 * ground from the caster's feet: damage + a hard root (heavy Slowness + jump lock).
 */
public class ShadowSewingAbility extends Ability implements Ability.Cooldown {

    private static final float CHAKRA_COST = 45f;
    private static final double RANGE = 8.0;
    private static final int MAX_TARGETS = 5;
    private static final float DAMAGE = 6.0f;
    private static final int PIN_TICKS = 4 * 20;

    @Override
    public ActivationType activationType() {
        return ActivationType.INSTANT;
    }

    /** Shorter than possession; the shadows strike rather than hold. */
    @Override
    public int castPoseTicks() {
        return 12;
    }

    @Override
    public long defaultCombo() {
        return 332;
    }

    @Override
    public int getCooldown() {
        return 12 * 20;
    }

    @Override
    public SoundEvent castingSound() {
        return SoundEvents.SCULK_CLICKING;
    }

    @Override
    public boolean handleCost(Player player, INinjaData ninjaData, int chargeAmount) {
        if (!"nara".equals(ninjaData.getClanId())) {
            player.displayClientMessage(Component.translatable("jutsu.fail.nara",
                    Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW)), true);
            return false;
        }
        if (ninjaData.getChakra() < CHAKRA_COST) {
            player.displayClientMessage(Component.translatable("jutsu.fail.notenoughchakra",
                    Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW)), true);
            return false;
        }
        ninjaData.useChakra(CHAKRA_COST, 30);
        return true;
    }

    @Override
    public void performServer(Player player, INinjaData ninjaData, int ticksActive) {
        double range = RANGE * ninjaData.getClanJutsuRangeMultiplier();
        float damage = DAMAGE * ninjaData.getRankDamageMultiplier();

        List<LivingEntity> targets = player.level().getEntitiesOfClass(LivingEntity.class,
                        player.getBoundingBox().inflate(range, 2, range),
                        e -> e != player && e.isAlive() && !(e instanceof Player p && p.isCreative())).stream()
                .sorted(Comparator.comparingDouble(e -> e.position().distanceTo(player.position())))
                .limit(MAX_TARGETS)
                .toList();

        if (targets.isEmpty()) {
            player.displayClientMessage(Component.literal("No targets within the shadow's reach!")
                    .withStyle(ChatFormatting.GRAY), true);
            return;
        }

        Vec3 origin = player.position();
        for (LivingEntity target : targets) {
            target.hurt(player.damageSources().playerAttack(player), damage);
            // Sewn stuck: rooted in place, unable to jump free
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, PIN_TICKS, 5, false, true));
            target.addEffect(new MobEffectInstance(MobEffects.JUMP, PIN_TICKS, -10, false, false));

            // Ground-hugging tendril line from the caster's feet to each victim
            if (player.level() instanceof ServerLevel serverLevel) {
                Vec3 targetPos = target.position();
                int steps = (int) (origin.distanceTo(targetPos) * 3);
                for (int i = 0; i <= steps; i++) {
                    Vec3 pos = origin.lerp(targetPos, steps == 0 ? 0 : i / (double) steps);
                    serverLevel.sendParticles(NarutoParticles.SHADOW_PURPLE,
                            pos.x, pos.y + 0.05, pos.z, 1, 0.08, 0.01, 0.08, 0.0);
                }
                NarutoParticles.spawnBurst(serverLevel,
                        targetPos.add(0, target.getBbHeight() * 0.5, 0), 12, 0.3, NarutoParticles.SHADOW_PURPLE);
            }
        }
    }
}
