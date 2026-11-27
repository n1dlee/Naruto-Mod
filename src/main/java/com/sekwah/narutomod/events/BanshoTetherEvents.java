package com.sekwah.narutomod.events;

import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.network.PacketHandler;
import com.sekwah.narutomod.network.s2c.BanshoTetherPacket;
import com.sekwah.narutomod.sounds.NarutoSounds;
import com.sekwah.narutomod.util.BanshoPull;
import com.sekwah.narutomod.util.BanshoPull.Stage;
import com.sekwah.narutomod.util.JutsuAim;
import com.sekwah.narutomod.util.JutsuVfx;
import com.sekwah.narutomod.util.KamuiStep;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.BiConsumer;

@Mod.EventBusSubscriber(modid = NarutoMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class BanshoTetherEvents {
    public static final float COST = 35;
    private static final Map<ServerPlayer, Cast> ACTIVE = new WeakHashMap<>();
    private static int serial;

    private BanshoTetherEvents() {
    }

    public static boolean start(ServerPlayer owner, INinjaData data, BiConsumer<ServerPlayer, INinjaData> success) {
        if (!available(owner, data) || ACTIVE.containsKey(owner) || data.getChakra() < COST) {
            return false;
        }
        var target = JutsuAim.select(owner, BanshoPull.RANGE, 13, victim -> legal(owner, victim)
                && owner.distanceToSqr(victim) > BanshoPull.STOP * BanshoPull.STOP
                && ACTIVE.values().stream().noneMatch(tether -> tether.target.get() == victim));
        if (target == null || !clear(owner, hand(owner), JutsuAim.center(target))) {
            return false;
        }
        var cast = new Cast(owner, target, new Receipt(data, success));
        ACTIVE.put(owner, cast);
        data.useChakra(COST, 20);
        data.setCastPoseTicks(BanshoPull.FOCUS + BanshoPull.DURATION);
        emit(cast, Stage.FOCUS);
        tone(cast, NarutoSounds.BANSHO_FOCUS.get(), 0.62f);
        return true;
    }

    private static boolean available(ServerPlayer owner, INinjaData data) {
        return owner.isAlive() && !owner.noPhysics && !owner.isSpectator() && data.isNinjaModeEnabled();
    }

    private static boolean legal(ServerPlayer owner, LivingEntity target) {
        return JutsuAim.attackable(owner, target) && !target.isPassenger() && !target.isVehicle()
                && (!(target instanceof Player) || owner.serverLevel().getServer().isPvpAllowed())
                && owner.level().mayInteract(owner, target.blockPosition());
    }

    @SubscribeEvent
    public static void tick(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer owner)) {
            return;
        }
        var cast = ACTIVE.get(owner);
        if (cast == null) {
            return;
        }
        var target = cast.target.get();
        if (!available(owner, cast.receipt.data()) || cast.level != owner.level() || target == null || !legal(owner, target)
                || owner.position().distanceToSqr(cast.origin) > 3.2 * 3.2
                || owner.distanceToSqr(target) > (BanshoPull.RANGE + 1) * (BanshoPull.RANGE + 1)) {
            finish(owner, cast, Stage.BREAK);
            return;
        }
        advance(owner, target, cast);
    }

    private static void advance(ServerPlayer owner, LivingEntity target, Cast cast) {
        Vec3 source = hand(owner);
        Vec3 tip = JutsuAim.center(target);
        if (!clear(owner, source, tip)) {
            finish(owner, cast, Stage.BREAK);
            return;
        }
        cast.hand = source;
        cast.tip = tip;
        int age = (int) (cast.level.getGameTime() - cast.start) - BanshoPull.FOCUS;
        if (age < 0) {
            if ((age & 1) == 0) {
                emit(cast, Stage.FOCUS);
            }
            return;
        }
        if (age >= BanshoPull.DURATION || owner.distanceToSqr(target) <= BanshoPull.STOP * BanshoPull.STOP) {
            finish(owner, cast, Stage.RELEASE);
            return;
        }
        var motion = BanshoPull.velocity(target.position(), owner.position(), age, target instanceof Player,
                target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), target.onGround());
        if (motion.lengthSqr() <= 1.0e-8 || !canMove(cast.level, target, motion)) {
            finish(owner, cast, Stage.BREAK);
            return;
        }
        if (!cast.committed) {
            cast.committed = true;
            cast.receipt.success().accept(owner, cast.receipt.data());
            tone(cast, NarutoSounds.BANSHO_PULL.get(), 0.83f);
        }
        target.setDeltaMovement(motion);
        target.hurtMarked = true;
        emit(cast, Stage.DRAG);
    }

    private static boolean canMove(ServerLevel level, LivingEntity target, Vec3 displacement) {
        AABB sweep = target.getBoundingBox().expandTowards(displacement).deflate(1.0e-6);
        var border = level.getWorldBorder();
        return KamuiStep.insideBorder(sweep, border.getMinX(), border.getMinZ(), border.getMaxX(), border.getMaxZ())
                && level.hasChunksAt(BlockPos.containing(sweep.minX, sweep.minY, sweep.minZ),
                BlockPos.containing(sweep.maxX, sweep.maxY, sweep.maxZ))
                && level.noCollision(target, sweep);
    }

    private static boolean clear(ServerPlayer owner, Vec3 from, Vec3 to) {
        var box = new AABB(from, to);
        return owner.level().hasChunksAt(BlockPos.containing(box.minX, box.minY, box.minZ),
                BlockPos.containing(box.maxX, box.maxY, box.maxZ))
                && owner.level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, owner))
                .getType() == HitResult.Type.MISS;
    }

    private static Vec3 hand(ServerPlayer owner) {
        return JutsuVfx.handPosition(owner, owner.getMainArm() == HumanoidArm.RIGHT ? 1 : -1, 1);
    }

    private static void finish(ServerPlayer owner, Cast cast, Stage stage) {
        if (ACTIVE.remove(owner, cast)) {
            if (!cast.committed) {
                cast.receipt.data().addChakra(COST);
            }
            emit(cast, stage);
        }
    }

    private static void emit(Cast cast, Stage stage) {
        PacketHandler.sendNear(new BanshoTetherPacket(cast.level.dimension().location(), cast.id, stage, cast.hand, cast.tip),
                cast.level, cast.hand.lerp(cast.tip, 0.48), 38);
    }

    private static void tone(Cast cast, SoundEvent event, float volume) {
        cast.level.playSound(null, cast.hand.x, cast.hand.y, cast.hand.z, event, SoundSource.PLAYERS, volume, 1);
    }

    @SubscribeEvent
    public static void leave(EntityLeaveLevelEvent event) {
        if (event.getEntity() instanceof ServerPlayer owner) {
            Cast cast = ACTIVE.get(owner);
            if (cast != null) {
                finish(owner, cast, Stage.BREAK);
            }
        }
    }

    private static final class Cast {
        private final int id = ++serial;
        private final ServerLevel level;
        private final long start;
        private final Vec3 origin;
        private final WeakReference<LivingEntity> target;
        private final Receipt receipt;
        private boolean committed;
        private Vec3 hand;
        private Vec3 tip;

        private Cast(ServerPlayer owner, LivingEntity target, Receipt receipt) {
            this.level = owner.serverLevel();
            this.start = this.level.getGameTime();
            this.origin = owner.position();
            this.target = new WeakReference<>(target);
            this.receipt = receipt;
            this.hand = BanshoTetherEvents.hand(owner);
            this.tip = JutsuAim.center(target);
        }
    }

    private record Receipt(INinjaData data, BiConsumer<ServerPlayer, INinjaData> success) {
    }
}
