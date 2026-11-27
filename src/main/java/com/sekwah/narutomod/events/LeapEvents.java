package com.sekwah.narutomod.events;

import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.capabilities.NinjaCapabilityHandler;
import com.sekwah.narutomod.gameevents.NarutoGameEvents;
import com.sekwah.narutomod.network.PacketHandler;
import com.sekwah.narutomod.network.s2c.LeapFxPacket;
import com.sekwah.narutomod.network.s2c.LeapFxPacket.Phase;
import com.sekwah.narutomod.sounds.NarutoSounds;
import com.sekwah.narutomod.util.ChidoriDashPath;
import com.sekwah.narutomod.util.LeapMotion;
import com.sekwah.sekclib.util.PlayerUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

@Mod.EventBusSubscriber(modid = NarutoMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class LeapEvents {
    public static final float STAMINA = 10;
    private static final Map<LivingEntity, Flight> ACTIVE = new WeakHashMap<>();

    private LeapEvents() {
    }

    public static boolean canStart(LivingEntity actor) {
        return !ACTIVE.containsKey(actor) && ready(actor);
    }

    private static boolean ready(LivingEntity actor) {
        return actor.isAlive() && !actor.isSpectator() && !actor.isPassenger() && !actor.isFallFlying()
                && !actor.isNoGravity() && actor.onGround() && support(actor) != null
                && actor.level().noCollision(actor, actor.getBoundingBox().move(0, 0.20, 0));
    }

    public static void start(LivingEntity actor, Vec3 impulse) {
        if (!(actor.level() instanceof ServerLevel level) || !canStart(actor)) {
            return;
        }
        Flight flight = new Flight(impulse, ChidoriDashPath.groundHeading(impulse, actor.getYRot()));
        ACTIVE.put(actor, flight);
        send(level, actor, flight, Phase.BRACE, new Contact(actor.position(), Blocks.AIR.defaultBlockState(), false), 0);
    }

    @SubscribeEvent
    public static void tick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level)) {
            return;
        }
        for (LivingEntity actor : List.copyOf(ACTIVE.keySet())) {
            Flight flight = ACTIVE.get(actor);
            if (flight != null && actor.level() == level && !advance(level, actor, flight)) {
                ACTIVE.remove(actor, flight);
            }
        }
    }

    @SubscribeEvent
    public static void leave(EntityLeaveLevelEvent event) {
        if (!event.getLevel().isClientSide && event.getEntity() instanceof LivingEntity actor) {
            ACTIVE.remove(actor);
        }
    }

    private static boolean advance(ServerLevel level, LivingEntity actor, Flight flight) {
        if (!actor.isAlive() || actor.isRemoved() || actor.isSpectator()) {
            return false;
        }
        int tick = ++flight.ticks;
        if (tick <= LeapMotion.WINDUP) {
            if (!ready(actor)) {
                return cancel(level, actor, flight);
            }
            if (actor instanceof Mob mob) {
                mob.getNavigation().stop();
            }
            return tick < LeapMotion.WINDUP || launch(level, actor, flight);
        }
        if (actor.isPassenger() || actor.isFallFlying() || actor.isNoGravity() || tick > 80) {
            return cancel(level, actor, flight);
        }
        return trackArc(level, actor, flight);
    }

    private static boolean launch(ServerLevel level, LivingEntity actor, Flight flight) {
        Contact support = support(actor);
        if (support == null) {
            return cancel(level, actor, flight);
        }
        Vec3 impulse = flight.impulse;
        if (actor instanceof Player player) {
            var capability = player.getCapability(NinjaCapabilityHandler.NINJA_DATA).resolve();
            if (capability.isEmpty() || capability.get().getStamina() < STAMINA) {
                return cancel(level, actor, flight);
            }
            capability.get().useStamina(STAMINA, 40);
            PlayerUtil.setVelocity(player, impulse.x, impulse.y, impulse.z, true);
        } else {
            actor.setDeltaMovement(impulse);
            actor.hurtMarked = true;
        }
        flight.origin = support;
        flight.baseY = actor.getY();
        flight.peak = flight.baseY;
        flight.lastY = flight.baseY;
        return true;
    }

    private static boolean trackArc(ServerLevel level, LivingEntity actor, Flight flight) {
        double y = actor.getY();
        flight.peak = Math.max(flight.peak, y);
        double drop = flight.lastY - y;
        flight.lastY = y;
        if (!flight.airborne) {
            if (LeapMotion.airborne(flight.baseY, y, actor.onGround())) {
                flight.airborne = true;
                send(level, actor, flight, Phase.TAKEOFF, flight.origin, 0.53f);
                actor.playSound(NarutoSounds.LEAP.get(), 0.66f, 1.0f);
                actor.level().gameEvent(actor, NarutoGameEvents.LEAP.get(), actor.position());
            } else if (flight.ticks - LeapMotion.WINDUP > 5) {
                return cancel(level, actor, flight);
            }
            return true;
        }
        if (drop > 0) {
            flight.speedDown = Math.max(drop, flight.speedDown);
        }
        Contact floor = support(actor);
        if (floor != null && (floor.water() || actor.onGround()) && drop >= -0.02) {
            land(level, actor, flight, floor, y);
            return false;
        }
        return true;
    }

    private static void land(ServerLevel level, LivingEntity actor, Flight flight, Contact floor, double y) {
        float strength = LeapMotion.impact(flight.peak - y, flight.speedDown);
        send(level, actor, flight, floor.water() ? Phase.WATER : Phase.LAND, floor, strength);
        Vec3 point = floor.point();
        var sound = floor.water() ? SoundEvents.PLAYER_SPLASH
                : strength >= 0.63f ? SoundEvents.GENERIC_BIG_FALL
                : floor.state().getSoundType(level, BlockPos.containing(point.add(0, -0.07, 0)), actor).getFallSound();
        level.playSound(null, point.x, point.y, point.z, sound, SoundSource.PLAYERS, 0.33f + strength * 0.45f, 0.93f);
    }

    private static boolean cancel(ServerLevel level, LivingEntity actor, Flight flight) {
        send(level, actor, flight, Phase.CANCEL, new Contact(actor.position(), Blocks.AIR.defaultBlockState(), false), 0);
        return false;
    }

    @Nullable
    private static Contact support(LivingEntity actor) {
        var level = actor.level();
        Vec3 feet = actor.position();
        BlockPos belowFeet = BlockPos.containing(feet.add(0, -0.12, 0));
        var fluid = level.getFluidState(belowFeet);
        if (fluid.is(FluidTags.WATER)) {
            double surface = belowFeet.getY() + fluid.getHeight(level, belowFeet);
            if (Math.abs(surface - feet.y) <= 0.19 || actor.isInWaterOrBubble()) {
                return new Contact(new Vec3(feet.x, surface + 0.015, feet.z), Blocks.WATER.defaultBlockState(), true);
            }
        }
        var floor = level.clip(new ClipContext(feet.add(0, 0.12, 0), feet.add(0, -0.33, 0),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, actor));
        if (floor.getType() == HitResult.Type.MISS) {
            return null;
        }
        return new Contact(floor.getLocation().add(0, 0.019, 0), level.getBlockState(floor.getBlockPos()), false);
    }

    private static void send(ServerLevel level, LivingEntity actor, Flight flight, Phase phase, Contact contact, float strength) {
        PacketHandler.sendNear(new LeapFxPacket(level.dimension().location(), actor.getId(), phase,
                contact.point(), flight.forward, Block.getId(contact.state()), strength), level, contact.point(), 48.0);
    }

    private record Contact(Vec3 point, BlockState state, boolean water) {
    }

    private static final class Flight {
        private final Vec3 impulse;
        private final Vec3 forward;
        private Contact origin;
        private int ticks;
        private boolean airborne;
        private double baseY;
        private double lastY;
        private double peak;
        private double speedDown;

        private Flight(Vec3 impulse, Vec3 forward) {
            this.impulse = impulse;
            this.forward = forward;
        }
    }
}
