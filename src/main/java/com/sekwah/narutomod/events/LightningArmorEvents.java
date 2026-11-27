package com.sekwah.narutomod.events;

import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.abilities.NarutoAbilities;
import com.sekwah.narutomod.capabilities.NinjaCapabilityHandler;
import com.sekwah.narutomod.util.LightningCoat;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = NarutoMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class LightningArmorEvents {
    private LightningArmorEvents() {
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void protect(LivingHurtEvent event) {
        if (event.getEntity() instanceof Player player && !player.level().isClientSide
                && event.getAmount() > 0 && coatCanBlock(event.getSource()) && active(player)) {
            event.setAmount(LightningCoat.protectedDamage(event.getAmount()));
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void flash(LivingDamageEvent event) {
        if (!(event.getEntity() instanceof Player player) || !(player.level() instanceof ServerLevel level)
                || event.getAmount() <= 0 || !coatCanBlock(event.getSource()) || !active(player)) {
            return;
        }
        Vec3 center = player.position().add(0, player.getBbHeight() * 0.64, 0);
        Vec3 incoming = event.getSource().getSourcePosition();
        Vec3 normal = incoming == null ? player.getLookAngle() : incoming.subtract(center).normalize();
        Vec3 contact = center.add(normal.scale(0.36));
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, contact.x, contact.y, contact.z, 6, 0.14, 0.19, 0.14, 0.05);
        level.playSound(null, player, SoundEvents.TRIDENT_HIT, SoundSource.PLAYERS, 0.45f, 1.55f);
    }

    @SubscribeEvent
    public static void lift(LivingEvent.LivingJumpEvent event) {
        if (event.getEntity() instanceof Player player && !player.isPassenger() && active(player)) {
            player.setDeltaMovement(player.getDeltaMovement().add(0, 0.18, 0));
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void cushion(LivingFallEvent event) {
        if (event.getEntity() instanceof Player player && !player.level().isClientSide && active(player)) {
            event.setDamageMultiplier(LightningCoat.cushioning(event.getDamageMultiplier()));
        }
    }

    @SubscribeEvent
    public static void leave(PlayerEvent.PlayerLoggedOutEvent event) {
        remove(event.getEntity());
    }

    @SubscribeEvent
    public static void arrive(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof Player player && !event.getLevel().isClientSide) {
            remove(player);
        }
    }

    @SubscribeEvent
    public static void die(LivingDeathEvent event) {
        if (event.getEntity() instanceof Player player && !player.level().isClientSide) {
            remove(player);
        }
    }

    private static boolean active(Player player) {
        return player.getCapability(NinjaCapabilityHandler.NINJA_DATA).map(data -> LightningCoat.active(player, data)).orElse(false);
    }

    private static boolean coatCanBlock(DamageSource source) {
        return !source.is(DamageTypeTags.BYPASSES_RESISTANCE) && !source.is(DamageTypeTags.IS_FALL);
    }

    private static void remove(Player player) {
        player.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(data ->
                data.getToggleAbilityData().removeAbility(NarutoAbilities.LIGHTNING_ARMOR.getId()));
    }
}
