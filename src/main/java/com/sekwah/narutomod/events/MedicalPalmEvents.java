package com.sekwah.narutomod.events;

import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.capabilities.NinjaCapabilityHandler;
import com.sekwah.narutomod.network.PacketHandler;
import com.sekwah.narutomod.network.s2c.MedicalPalmPacket;
import com.sekwah.narutomod.sounds.NarutoSounds;
import com.sekwah.narutomod.util.Faction;
import com.sekwah.narutomod.util.JutsuVfx;
import com.sekwah.narutomod.util.MedicalPalm;
import com.sekwah.narutomod.util.MedicalPalm.Phase;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

@Mod.EventBusSubscriber(modid = NarutoMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class MedicalPalmEvents {
    private static final Map<Player, Channel> ACTIVE = new WeakHashMap<>();
    private static final Set<INinjaData> HEALED = Collections.newSetFromMap(new WeakHashMap<>());

    private MedicalPalmEvents() {
    }

    public static void tickChannel(Player player, INinjaData data, int age) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        Channel channel = ACTIVE.get(player);
        if (age == 0) {
            HEALED.remove(data);
            channel = new Channel(level);
            ACTIVE.put(player, channel);
            show(player, channel, Phase.GATHER, Vec3.ZERO, 0);
            level.playSound(null, player.blockPosition(), NarutoSounds.MEDICAL_PALM_OPEN.get(), SoundSource.PLAYERS, 0.65f, 1);
        }
        if (channel == null || channel.level != level) {
            return;
        }
        channel.lastTick = level.getGameTime();
        if (MedicalPalm.beat(age) && heal(player, data, channel)) {
            return;
        }
        if (age > 0 && age % 6 == 0) {
            show(player, channel, Phase.HOLD, Vec3.ZERO, 0);
        }
    }

    private static boolean heal(Player player, INinjaData data, Channel channel) {
        Target target = player.isCrouching() ? new Target(player, chest(player)) : target(player, channel.level);
        if (target == null || target.entity().getHealth() >= target.entity().getMaxHealth() || data.getChakra() < MedicalPalm.COST) {
            return false;
        }
        LivingEntity patient = target.entity();
        float before = patient.getHealth();
        patient.heal(patient == player ? 1 : 1.5f);
        float after = patient.getHealth();
        if (!MedicalPalm.accepted(before, after)) {
            return false;
        }
        data.useChakra(MedicalPalm.COST, 5);
        HEALED.add(data);
        show(player, channel, Phase.HEAL, target.contact(), after - before);
        channel.level.playSound(null, target.contact().x, target.contact().y, target.contact().z,
                NarutoSounds.MEDICAL_PALM_HEAL.get(), SoundSource.PLAYERS, 0.59f, patient == player ? 0.93f : 1.06f);
        return true;
    }

    private static @Nullable Target target(Player player, ServerLevel level) {
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getLookAngle().scale(MedicalPalm.REACH));
        var block = level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        double distance = block.getType() == HitResult.Type.BLOCK ? eye.distanceTo(block.getLocation()) : MedicalPalm.REACH + 0.02;
        Target best = null;
        for (LivingEntity candidate : level.getEntitiesOfClass(LivingEntity.class, new AABB(eye, end).inflate(0.72),
                entity -> entity != player && entity.isAlive() && entity.isPickable() && !entity.isSpectator())) {
            var hit = candidate.getBoundingBox().inflate(0.06).clip(eye, end);
            if (hit.isPresent()) {
                Vec3 contact = hit.get();
                double depth = eye.distanceTo(contact);
                if (MedicalPalm.inFrontOfCover(depth, distance)) {
                    distance = depth;
                    best = new Target(candidate, contact);
                }
            }
        }
        if (best == null || !friendly(player, best.entity())) {
            return null;
        }
        Vec3 palm = palm(player);
        var occlusion = level.clip(new ClipContext(palm, best.contact(), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        return occlusion.getType() == HitResult.Type.MISS ? best : null;
    }

    private static boolean friendly(Player healer, LivingEntity entity) {
        return !entity.noPhysics && (healer.isAlliedTo(entity) || Faction.sameSide(healer, entity)
                || entity instanceof TamableAnimal tame && tame.isOwnedBy(healer));
    }

    public static boolean takeHealCredit(INinjaData data) {
        return HEALED.remove(data);
    }

    public static void end(Player player) {
        Channel channel = ACTIVE.remove(player);
        if (channel != null) {
            show(player, channel, Phase.RELEASE, Vec3.ZERO, 0);
        }
    }

    @SubscribeEvent
    public static void cleanExpired(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide) {
            return;
        }
        Channel channel = ACTIVE.get(player);
        if (channel != null && (channel.level != player.level() || !player.isAlive()
                || channel.level.getGameTime() - channel.lastTick > 2)) {
            end(player);
            player.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(HEALED::remove);
        }
    }

    @SubscribeEvent
    public static void leave(EntityLeaveLevelEvent event) {
        if (!event.getLevel().isClientSide && event.getEntity() instanceof Player player) {
            end(player);
            player.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(HEALED::remove);
        }
    }

    private static Vec3 chest(LivingEntity actor) {
        return actor.position().add(0, actor.getBbHeight() * 0.65, 0);
    }

    private static Vec3 palm(Player player) {
        return JutsuVfx.handPosition(player, player.getMainArm() == HumanoidArm.RIGHT ? 1 : -1, 1);
    }

    private static void show(Player player, Channel channel, Phase phase, Vec3 contact, float gain) {
        Vec3 hand = player.level() == channel.level ? palm(player) : channel.lastPalm;
        channel.lastPalm = hand;
        PacketHandler.sendNear(new MedicalPalmPacket(channel.level.dimension().location(), player.getId(), phase, hand,
                gain > 0 ? contact : hand, gain, player.getMainArm() == HumanoidArm.RIGHT), channel.level, hand, 32);
    }

    private record Target(LivingEntity entity, Vec3 contact) {
    }

    private static final class Channel {
        private final ServerLevel level;
        private long lastTick;
        private Vec3 lastPalm = Vec3.ZERO;

        private Channel(ServerLevel level) {
            this.level = level;
            this.lastTick = level.getGameTime();
        }
    }
}
