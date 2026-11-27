package com.sekwah.narutomod.events;

import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.entity.NarutoEntities;
import com.sekwah.narutomod.entity.NinkenEntity;
import com.sekwah.narutomod.network.PacketHandler;
import com.sekwah.narutomod.network.s2c.NinkenCuePacket;
import com.sekwah.narutomod.network.s2c.NinkenCuePacket.Stage;
import com.sekwah.narutomod.sounds.NarutoSounds;
import com.sekwah.narutomod.util.KamuiStep;
import com.sekwah.narutomod.util.NinkenRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityDimensions;
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
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

@Mod.EventBusSubscriber(modid = NarutoMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class NinkenEvents {
    public static final int SEAL_TICKS = 5;
    private static final EntityDimensions BODY = EntityDimensions.scalable(0.68f, 0.86f);
    private static final Map<Player, Summon> CALLS = new WeakHashMap<>();

    private NinkenEvents() {
    }

    public static boolean hasPartner(Player player) {
        return player.level() instanceof ServerLevel level
                && NinkenRegistry.of(level).active(player.getUUID(), NinkenRegistry.time(level)) != null;
    }

    public static @Nullable NinkenEntity partner(Player player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return null;
        }
        UUID partner = NinkenRegistry.of(level).active(player.getUUID(), NinkenRegistry.time(level));
        return partner != null && level.getEntity(partner) instanceof NinkenEntity dog && dog.isOwnedBy(player) ? dog : null;
    }

    public static @Nullable Vec3 placement(Player player) {
        if (!(player.level() instanceof ServerLevel level) || !player.isAlive() || player.isSpectator() || player.noPhysics
                || CALLS.containsKey(player)) {
            return null;
        }
        Vec3 forward = KamuiStep.facing(player.getYRot());
        Vec3 side = new Vec3(forward.z, 0, -forward.x);
        for (double distance : new double[]{1.35, 2.0, 2.6}) {
            for (double offset : new double[]{0, 0.82, -0.82}) {
                Vec3 flat = player.position().add(forward.scale(distance)).add(side.scale(offset));
                for (double step : new double[]{0, 0.5, -0.5, 1, -1}) {
                    Vec3 feet = flat.add(0, step, 0);
                    if (safe(level, player, feet)) {
                        return feet;
                    }
                }
            }
        }
        return null;
    }

    public static void start(Player player, Vec3 feet, float rank) {
        if (!(player.level() instanceof ServerLevel level) || CALLS.containsKey(player) || hasPartner(player)) {
            return;
        }
        var summon = new Summon(level, player.position(), feet, KamuiStep.facing(player.getYRot()), level.getGameTime() + SEAL_TICKS, rank);
        CALLS.put(player, summon);
        send(summon, Stage.SEAL);
    }

    private static boolean safe(ServerLevel level, Player player, Vec3 feet) {
        AABB box = BODY.makeBoundingBox(feet);
        var border = level.getWorldBorder();
        if (!KamuiStep.insideBorder(box, border.getMinX(), border.getMinZ(), border.getMaxX(), border.getMaxZ())
                || !level.hasChunkAt(BlockPos.containing(box.minX, box.minY, box.minZ))
                || !level.hasChunkAt(BlockPos.containing(box.maxX, box.minY, box.maxZ))
                || !level.noCollision(player, box.deflate(0.002)) || level.containsAnyLiquid(box)
                || !level.getBlockCollisions(player, box.move(0, -0.17, 0)).iterator().hasNext()) {
            return false;
        }
        return level.clip(new ClipContext(player.getEyePosition(), feet.add(0, 0.43, 0), ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, player)).getType() == HitResult.Type.MISS;
    }

    @SubscribeEvent
    public static void tick(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide) {
            return;
        }
        Summon summon = CALLS.get(player);
        if (summon == null || summon.level().getGameTime() < summon.release()) {
            return;
        }
        CALLS.remove(player);
        if (!player.isAlive() || player.level() != summon.level() || player.isSpectator() || player.noPhysics
                || player.position().distanceToSqr(summon.origin()) > 1.65 * 1.65
                || hasPartner(player) || !safe(summon.level(), player, summon.feet())) {
            player.displayClientMessage(Component.translatable("jutsu.fail.ninken.space"), true);
            return;
        }
        arrive(player, summon);
    }

    private static void arrive(Player player, Summon summon) {
        ServerLevel level = summon.level();
        NinkenEntity dog = NarutoEntities.NINKEN.get().create(level);
        if (dog == null) {
            return;
        }
        Vec3 at = summon.feet();
        dog.moveTo(at.x, at.y, at.z, player.getYRot(), 0);
        dog.bind(player, summon.rank());
        var ledger = NinkenRegistry.of(level);
        if (!ledger.claim(player.getUUID(), dog.getUUID(), NinkenRegistry.time(level))) {
            return;
        }
        if (!level.addFreshEntity(dog)) {
            ledger.release(player.getUUID(), dog.getUUID());
            return;
        }
        send(summon, Stage.ARRIVE);
        level.playSound(null, BlockPos.containing(at), NarutoSounds.NINKEN_ARRIVE.get(), SoundSource.NEUTRAL, 0.87f, 1);
        dog.playSound(net.minecraft.sounds.SoundEvents.WOLF_GROWL, 0.46f, 0.86f);
    }

    private static void send(Summon summon, Stage stage) {
        int seed = Long.hashCode(summon.release() ^ Double.doubleToLongBits(summon.feet().x));
        PacketHandler.sendNear(new NinkenCuePacket(summon.level().dimension().location(), seed, stage,
                summon.feet(), summon.facing()), summon.level(), summon.feet(), 30);
    }

    @SubscribeEvent
    public static void leave(EntityLeaveLevelEvent event) {
        if (!event.getLevel().isClientSide && event.getEntity() instanceof Player player) {
            CALLS.remove(player);
        }
    }

    private record Summon(ServerLevel level, Vec3 origin, Vec3 feet, Vec3 facing, long release, float rank) {
    }
}
