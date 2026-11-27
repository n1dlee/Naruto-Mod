package com.sekwah.narutomod.events;

import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.capabilities.NinjaCapabilityHandler;
import com.sekwah.narutomod.util.ChakraFlow;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector3f;

import java.util.Map;
import java.util.WeakHashMap;

@Mod.EventBusSubscriber(modid = NarutoMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ChakraFlowEvents {
    private static final DustParticleOptions SHARD = new DustParticleOptions(new Vector3f(0.48f, 0.93f, 0.81f), 0.43f);
    private static final Map<LivingEntity, Hit> PENDING = new WeakHashMap<>();

    private ChakraFlowEvents() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void amplify(LivingHurtEvent event) {
        if (event.getEntity().level().isClientSide || event.getAmount() <= 0
                || !event.getSource().is(DamageTypes.PLAYER_ATTACK)
                || !(event.getSource().getDirectEntity() instanceof Player attacker)
                || ChakraFlow.edge(attacker.getMainHandItem()) == null) {
            return;
        }
        attacker.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(data -> {
            if (ChakraFlow.active(data) && data.getChakra() >= ChakraFlow.HIT_COST) {
                event.setAmount(event.getAmount() + ChakraFlow.bonus(data.getRankDamageMultiplier()));
                PENDING.put(event.getEntity(), new Hit(event.getSource(), attacker.level().getGameTime()));
            }
        });
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void confirm(LivingDamageEvent event) {
        LivingEntity target = event.getEntity();
        if (!(target.level() instanceof ServerLevel level)) {
            return;
        }
        Hit hit = PENDING.get(target);
        if (hit == null || hit.source() != event.getSource()) {
            return;
        }
        PENDING.remove(target);
        if (event.isCanceled() || event.getAmount() <= 0 || hit.tick() != level.getGameTime()
                || !(hit.source().getDirectEntity() instanceof Player attacker)) {
            return;
        }
        attacker.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(data -> {
            data.useChakra(ChakraFlow.HIT_COST, 10);
            emitContact(level, attacker, target);
        });
    }

    private static void emitContact(ServerLevel level, Player attacker, LivingEntity target) {
        var bounds = target.getBoundingBox();
        Vec3 pos = new Vec3(Mth.clamp(attacker.getX(), bounds.minX, bounds.maxX),
                target.getY(0.61), Mth.clamp(attacker.getZ(), bounds.minZ, bounds.maxZ));
        level.sendParticles(SHARD, pos.x, pos.y, pos.z, 6, 0.13, 0.11, 0.13, 0.02);
        boolean armored = target.getArmorValue() > 0;
        level.playSound(null, pos.x, pos.y, pos.z,
                armored ? SoundEvents.CHAIN_HIT : SoundEvents.PLAYER_ATTACK_STRONG,
                SoundSource.PLAYERS, 0.44f, armored ? 1.31f : 1.17f);
    }

    private record Hit(DamageSource source, long tick) {
    }
}
