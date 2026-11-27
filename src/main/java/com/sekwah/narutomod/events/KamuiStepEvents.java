package com.sekwah.narutomod.events;

import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.capabilities.NinjaCapabilityHandler;
import com.sekwah.narutomod.gameevents.NarutoGameEvents;
import com.sekwah.narutomod.network.PacketHandler;
import com.sekwah.narutomod.network.s2c.KamuiStepPacket;
import com.sekwah.narutomod.sounds.NarutoSounds;
import com.sekwah.narutomod.util.KamuiStep;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.BiConsumer;

@Mod.EventBusSubscriber(modid = NarutoMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class KamuiStepEvents {
    public static final int WINDUP = 3;
    private static final int RECOVERY = 6;
    private static final int FAILED_RETRY = 10;
    private static final Map<ServerPlayer, Shift> PENDING = new WeakHashMap<>();
    private static final Map<ServerPlayer, Long> RECOVERING = new WeakHashMap<>();
    private static final Map<ServerPlayer, Long> RETRY_AFTER = new WeakHashMap<>();

    private KamuiStepEvents() {
    }

    public static void start(ServerPlayer actor, INinjaData data, Ability ability, float cost,
                             BiConsumer<ServerPlayer, INinjaData> onSuccess) {
        ServerLevel level = actor.serverLevel();
        long now = level.getGameTime();
        if (!canShift(actor, data, ability, cost) || PENDING.containsKey(actor)
                || now < RETRY_AFTER.getOrDefault(actor, Long.MIN_VALUE)) {
            return;
        }
        Vec3 from = actor.position();
        float yaw = actor.getYRot();
        Vec3 to = KamuiStep.choose(from, yaw, candidate -> KamuiStep.safe(level, actor, candidate));
        if (to == null) {
            fail(level, actor);
            return;
        }
        boolean leftEye = "kakashi".equals(data.getMangekyoForm()) || !data.hasSignatureForm("obito");
        Shift shift = new Shift(from, to, yaw, actor.getBbHeight(), leftEye, level.random.nextInt(),
                now + WINDUP, cost, ability, onSuccess);
        PENDING.put(actor, shift);
        visual(level, actor, shift, KamuiStepPacket.Phase.PREPARE, from, from);
        level.gameEvent(actor, NarutoGameEvents.JUTSU_CASTING.get(), from.add(0, actor.getEyeHeight() * 0.7, 0));
        sound(level, from, 0.60f, 1.02f);
    }

    @SubscribeEvent
    public static void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer actor)) {
            return;
        }
        long now = actor.serverLevel().getGameTime();
        if (now >= RETRY_AFTER.getOrDefault(actor, Long.MAX_VALUE)) {
            RETRY_AFTER.remove(actor);
        }
        if (now >= RECOVERING.getOrDefault(actor, Long.MAX_VALUE)) {
            RECOVERING.remove(actor);
        }
        Shift shift = PENDING.get(actor);
        if (shift != null && now >= shift.release()) {
            PENDING.remove(actor);
            actor.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(data -> release(actor, data, shift));
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void defend(LivingAttackEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer actor)
                || actor.serverLevel().getGameTime() >= RECOVERING.getOrDefault(actor, Long.MIN_VALUE)) {
            return;
        }
        var source = event.getSource();
        if (!source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) && !source.is(DamageTypeTags.IS_FIRE)
                && (source.getEntity() != null || source.is(DamageTypeTags.IS_PROJECTILE))) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void leave(EntityLeaveLevelEvent event) {
        if (!event.getLevel().isClientSide && event.getEntity() instanceof ServerPlayer actor) {
            PENDING.remove(actor);
            RECOVERING.remove(actor);
            RETRY_AFTER.remove(actor);
        }
    }

    private static boolean canShift(ServerPlayer actor, INinjaData data, Ability ability, float cost) {
        return actor.isAlive() && !actor.isSpectator() && !actor.isPassenger() && !actor.isVehicle()
                && !actor.isSleeping() && !actor.isFallFlying() && !actor.noPhysics && !actor.isInWaterOrBubble()
                && data.isNinjaModeEnabled() && data.getChakra() >= cost && ability.hasEyeAccess(data);
    }

    private static void release(ServerPlayer actor, INinjaData data, Shift shift) {
        ServerLevel level = actor.serverLevel();
        Vec3 from = actor.position();
        Vec3 to = shift.to();
        if (!canShift(actor, data, shift.ability(), shift.cost())
                || !KamuiStep.canRelease(shift.from(), from, to, point -> KamuiStep.safe(level, actor, point))
                || !allowedTeleport(actor, to) || actor.level() != level || !KamuiStep.safe(level, actor, to)) {
            abort(level, actor, shift);
            return;
        }
        actor.connection.teleport(to.x, to.y, to.z, actor.getYRot(), actor.getXRot());
        if (actor.position().distanceToSqr(to) > 1.0e-6) {
            abort(level, actor, shift);
            return;
        }
        actor.setDeltaMovement(Vec3.ZERO);
        actor.hurtMarked = true;
        data.useChakra(shift.cost(), 40);
        RECOVERING.put(actor, level.getGameTime() + RECOVERY);
        visual(level, actor, shift, KamuiStepPacket.Phase.ARRIVED, from, actor.position());
        sound(level, to, 0.75f, 0.76f);
        shift.onSuccess().accept(actor, data);
    }

    private static boolean allowedTeleport(ServerPlayer actor, Vec3 to) {
        var event = ForgeEventFactory.onEnderTeleport(actor, to.x, to.y, to.z);
        return !event.isCanceled() && to.equals(new Vec3(event.getTargetX(), event.getTargetY(), event.getTargetZ()));
    }

    private static void abort(ServerLevel level, ServerPlayer actor, Shift shift) {
        visual(level, actor, shift, KamuiStepPacket.Phase.ABORTED, shift.from(), shift.from());
        fail(level, actor);
    }

    private static void fail(ServerLevel level, ServerPlayer actor) {
        RETRY_AFTER.put(actor, level.getGameTime() + FAILED_RETRY);
        actor.displayClientMessage(Component.translatable("jutsu.kamui.blocked").withStyle(ChatFormatting.GRAY), true);
    }

    private static void sound(ServerLevel level, Vec3 point, float volume, float pitch) {
        level.playSound(null, point.x, point.y + 1, point.z, NarutoSounds.KAMUI_FOLD.get(),
                SoundSource.PLAYERS, volume, pitch);
    }

    private static void visual(ServerLevel level, ServerPlayer actor, Shift shift, KamuiStepPacket.Phase phase,
                               Vec3 from, Vec3 to) {
        PacketHandler.sendNear(new KamuiStepPacket(level.dimension().location(), actor.getId(), phase,
                from, to, shift.yaw(), shift.height(), shift.seed(), shift.leftEye()),
                level, from.lerp(to, 0.5), 34 + from.distanceTo(to) * 0.5);
    }

    private record Shift(Vec3 from, Vec3 to, float yaw, float height, boolean leftEye, int seed, long release,
                         float cost, Ability ability, BiConsumer<ServerPlayer, INinjaData> onSuccess) {
    }
}
