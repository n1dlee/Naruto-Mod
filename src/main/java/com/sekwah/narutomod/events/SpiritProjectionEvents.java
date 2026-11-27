package com.sekwah.narutomod.events;

import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.capabilities.NinjaCapabilityHandler;
import com.sekwah.narutomod.network.PacketHandler;
import com.sekwah.narutomod.network.s2c.SpiritProjectionPacket;
import com.sekwah.narutomod.network.s2c.SpiritProjectionPacket.State;
import com.sekwah.narutomod.sounds.NarutoSounds;
import com.sekwah.narutomod.util.SpiritProjectionPath;
import com.sekwah.narutomod.util.SpiritProjectionPath.Input;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.WeakHashMap;

@Mod.EventBusSubscriber(modid = NarutoMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SpiritProjectionEvents {
    private static final Map<ServerPlayer, Focus> ACTIVE = new WeakHashMap<>();
    private static final Map<ServerPlayer, Long> RECOVERY = new WeakHashMap<>();
    private static int sequence;

    private SpiritProjectionEvents() {
    }

    public static boolean canStart(Player caster) {
        return caster instanceof ServerPlayer player && usable(player) && player.onGround()
                && !ACTIVE.containsKey(player) && !KamuiPhaseEvents.actionLocked(player)
                && player.level().getGameTime() >= RECOVERY.getOrDefault(player, Long.MIN_VALUE);
    }

    public static boolean actionLocked(Player caster) {
        return caster instanceof ServerPlayer player && ACTIVE.containsKey(player);
    }

    public static void input(ServerPlayer player, int session, Input input) {
        Focus focus = ACTIVE.get(player);
        if (focus != null && focus.id == session && focus.level == player.level()) {
            long clock = focus.level.getGameTime();
            if (focus.lastInput != clock) {
                focus.command = input;
                focus.lastInput = clock;
            }
        }
    }

    @SubscribeEvent
    public static void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) {
            return;
        }
        long now = player.level().getGameTime();
        if (now >= RECOVERY.getOrDefault(player, Long.MAX_VALUE)) {
            RECOVERY.remove(player);
        }
        boolean requested = player.getCapability(NinjaCapabilityHandler.NINJA_DATA).map(SpiritProjectionEvents::requested).orElse(false);
        Focus focus = ACTIVE.get(player);
        if (focus == null) {
            if (requested && canStart(player)) {
                open(player, now);
            } else if (requested) {
                removeToggle(player);
            }
        } else {
            advance(player, focus, now, requested);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void hurt(LivingDamageEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getAmount() > 0 && ACTIVE.containsKey(player)) {
            close(player);
        }
    }

    @SubscribeEvent
    public static void attack(LivingAttackEvent event) {
        if (event.getSource().getDirectEntity() instanceof Player player && actionLocked(player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void swing(AttackEntityEvent event) {
        if (actionLocked(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void use(PlayerInteractEvent event) {
        if (event.isCancelable() && actionLocked(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void leave(EntityLeaveLevelEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            close(player);
            RECOVERY.remove(player);
        }
    }

    private static boolean requested(INinjaData data) {
        return data.isNinjaModeEnabled() && data.getByakuganLevel() >= 2
                && data.getToggleAbilityData().getAbilitiesHashSet().contains(KamuiPhaseEvents.SCOUT);
    }

    private static boolean usable(ServerPlayer player) {
        return player.isAlive() && !player.isSpectator() && !player.isPassenger() && !player.isVehicle()
                && !player.isSleeping() && !player.isInWaterOrBubble() && !player.isFallFlying() && !player.noPhysics;
    }

    private static void open(ServerPlayer player, long now) {
        Focus focus = new Focus(++sequence, player, now);
        ACTIVE.put(player, focus);
        signal(player, focus, State.GATHER);
        focus.level.playSound(null, player.blockPosition(), NarutoSounds.SPIRIT_OPEN.get(), SoundSource.PLAYERS, 0.66f, 1f);
    }

    private static void advance(ServerPlayer player, Focus focus, long now, boolean requested) {
        int age = (int) (now - focus.started);
        if (!requested || player.level() != focus.level || !usable(player)
                || !SpiritProjectionPath.keepsBody(focus.body, player.position(), age)) {
            close(player);
            return;
        }
        if (age < SpiritProjectionPath.OPEN) {
            return;
        }
        if (now - focus.lastInput <= 8) {
            focus.eye = SpiritProjectionPath.step(focus.initialEye, focus.eye, focus.command, point -> loaded(focus.level, point));
        }
        if (age == SpiritProjectionPath.OPEN || age % 2 == 0) {
            signal(player, focus, State.DRIFT);
        }
    }

    private static boolean loaded(ServerLevel level, Vec3 point) {
        BlockPos pos = BlockPos.containing(point);
        return pos.getY() > level.getMinBuildHeight() && pos.getY() < level.getMaxBuildHeight()
                && level.hasChunkAt(pos) && level.getWorldBorder().isWithinBounds(pos);
    }

    private static void close(ServerPlayer player) {
        Focus focus = ACTIVE.remove(player);
        if (focus == null) {
            return;
        }
        removeToggle(player);
        RECOVERY.put(player, focus.level.getGameTime() + 24);
        signal(player, focus, State.RELEASE);
        focus.level.playSound(null, BlockPos.containing(focus.body), NarutoSounds.SPIRIT_RETURN.get(), SoundSource.PLAYERS, 0.65f, 1f);
    }

    private static void removeToggle(ServerPlayer player) {
        player.getCapability(NinjaCapabilityHandler.NINJA_DATA)
                .ifPresent(data -> data.getToggleAbilityData().removeAbility(KamuiPhaseEvents.SCOUT));
    }

    private static void signal(ServerPlayer player, Focus focus, State state) {
        PacketHandler.sendNear(new SpiritProjectionPacket(focus.level.dimension().location(), player.getId(), focus.id,
                state, focus.body, focus.eye, focus.command.yaw()), focus.level, focus.body, 42);
    }

    private static final class Focus {
        private final int id;
        private final ServerLevel level;
        private final long started;
        private final Vec3 body;
        private final Vec3 initialEye;
        private Vec3 eye;
        private Input command;
        private long lastInput;

        private Focus(int id, ServerPlayer player, long now) {
            this.id = id;
            this.level = player.serverLevel();
            this.started = now;
            this.body = player.position();
            this.initialEye = player.getEyePosition();
            this.eye = this.initialEye;
            this.command = new Input(player.getYRot(), (byte) 0, (byte) 0, (byte) 0);
            this.lastInput = now;
        }
    }
}
