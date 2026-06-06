package com.sekwah.narutomod.events;

import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.capabilities.NinjaCapabilityHandler;
import com.sekwah.narutomod.damagetypes.NarutoDamageTypes;
import com.sekwah.narutomod.sounds.NarutoSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector3f;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = NarutoMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class PlayerEvents {

    private static final UUID NINJA_HEALTH_MODIFIER_ID = UUID.fromString("d26b89a1-8dc2-4d13-a68e-fb10c2a5e95e");
    private static final double[] HEALTH_BONUS_VALUES = new double[] {0.0D, 8.0D, 16.0D, 28.0D, 48.0D};
    private static final float[] MOB_DAMAGE_MULTIPLIERS = new float[] {1.0F, 0.9F, 0.8F, 0.65F, 0.5F};
    private static final DustParticleOptions CHIDORI_PARTICLE = new DustParticleOptions(new Vector3f(0.45F, 0.85F, 1.0F), 1.0F);

    @SubscribeEvent
    public static void onEntityUpdate(LivingEvent.LivingTickEvent event) {
        if (event.getEntity() instanceof Player player) {
            player.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(ninjaData -> {
                if (!ninjaData.isNinjaModeEnabled()) {
                    syncNinjaHealth(player, 0);
                    return;
                }
                applyRankSurvivability(player, ninjaData);
            });
        }
    }

    private static void applyRankSurvivability(Player player, INinjaData ninjaData) {
        int rank = Math.min(Math.max(ninjaData.getNinjaRank(), 0), 4);
        if (player.tickCount % 40 != 0) {
            return;
        }

        syncNinjaHealth(player, rank);
        player.removeEffect(MobEffects.HEALTH_BOOST);

        if (rank >= 3) {
            int resistanceAmplifier = rank >= 4 ? 1 : 0;
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 80, resistanceAmplifier, false, false));
        }

        float healthRatio = player.getHealth() / player.getMaxHealth();
        if (rank >= 4 && healthRatio < 0.8F) {
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 80, 1, false, false));
        } else if (rank >= 3 && healthRatio < 0.7F) {
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 80, 0, false, false));
        } else if (rank >= 2 && healthRatio < 0.5F) {
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 80, 0, false, false));
        }
    }

    private static void syncNinjaHealth(Player player, int rank) {
        AttributeInstance maxHealth = player.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth == null) {
            return;
        }

        double targetBonus = HEALTH_BONUS_VALUES[Math.min(Math.max(rank, 0), 4)];
        AttributeModifier currentModifier = maxHealth.getModifier(NINJA_HEALTH_MODIFIER_ID);
        if (targetBonus <= 0.0D) {
            if (currentModifier != null) {
                maxHealth.removeModifier(NINJA_HEALTH_MODIFIER_ID);
                player.setHealth(Math.min(player.getHealth(), player.getMaxHealth()));
            }
            return;
        }

        if (currentModifier != null && Math.abs(currentModifier.getAmount() - targetBonus) < 0.01D) {
            return;
        }

        double oldMaxHealth = player.getMaxHealth();
        if (currentModifier != null) {
            maxHealth.removeModifier(NINJA_HEALTH_MODIFIER_ID);
        }
        maxHealth.addTransientModifier(new AttributeModifier(
                NINJA_HEALTH_MODIFIER_ID,
                "Naruto ninja rank health",
                targetBonus,
                AttributeModifier.Operation.ADDITION));

        if (player.getHealth() >= oldMaxHealth - 0.01F) {
            player.setHealth(player.getMaxHealth());
        }
    }

    @SubscribeEvent
    public static void livingHurt(LivingHurtEvent event) {
        applyChidoriMeleeHit(event);
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (!(event.getSource().getEntity() instanceof Mob)) {
            return;
        }
        player.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(ninjaData -> {
            if (!ninjaData.isNinjaModeEnabled()) {
                return;
            }
            int rank = Math.min(Math.max(ninjaData.getNinjaRank(), 0), 4);
            event.setAmount(event.getAmount() * MOB_DAMAGE_MULTIPLIERS[rank]);
        });
    }

    private static void applyChidoriMeleeHit(LivingHurtEvent event) {
        if (!(event.getSource().getEntity() instanceof Player attacker)) {
            return;
        }
        LivingEntity target = event.getEntity();
        attacker.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(ninjaData -> {
            if (!ninjaData.isNinjaModeEnabled() || !ninjaData.isChidoriActive()) {
                return;
            }
            DamageSource source = NarutoDamageTypes.getDamageSource(attacker.level(), NarutoDamageTypes.CHIDORI, attacker, attacker);
            ninjaData.setChidoriTicks(0);
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
                target.hurt(source, 20.0F * damageMultiplier);
            }
            attacker.level().playSound(null, attacker, NarutoSounds.CHIDORI.get(), SoundSource.PLAYERS, 1.0F, 1.15F);
            if (attacker.level() instanceof ServerLevel serverLevel) {
                Vec3 pos = target.position().add(0.0D, target.getBbHeight() * 0.55D, 0.0D);
                serverLevel.sendParticles(CHIDORI_PARTICLE, pos.x, pos.y, pos.z, 14, 0.3D, 0.35D, 0.3D, 0.04D);
                serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, pos.x, pos.y, pos.z, 18, 0.35D, 0.4D, 0.35D, 0.08D);
            }
        });
    }

    // Handle if they have some agility perk or leaps.
    //
    @SubscribeEvent
    public static void livingFall(LivingFallEvent event) {
        if (event.getEntity() instanceof Player player){
            player.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(ninjaData -> {
                if (!ninjaData.isNinjaModeEnabled()) {
                    return;
                }
                float distance = event.getDistance();
                if(distance < 9){
                    distance *= 0.3f;
                }
                if(distance > 3) {
                    distance -= 5f;
                    distance *= 0.6f;
                }
                event.setDistance(distance);
            });
        }
    }

}
