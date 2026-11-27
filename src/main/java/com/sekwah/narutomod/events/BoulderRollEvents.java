package com.sekwah.narutomod.events;

import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.capabilities.NinjaCapabilityHandler;
import com.sekwah.narutomod.network.PacketHandler;
import com.sekwah.narutomod.network.s2c.BoulderRollPacket;
import com.sekwah.narutomod.sounds.NarutoSounds;
import com.sekwah.narutomod.util.BaikaScale;
import com.sekwah.narutomod.util.BoulderRoll;
import com.sekwah.narutomod.util.BoulderRoll.Stage;
import com.sekwah.narutomod.util.Faction;
import com.sekwah.narutomod.util.KamuiStep;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

@Mod.EventBusSubscriber(modid = NarutoMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class BoulderRollEvents {
    private static final Map<Player, Roll> ROLLING = new WeakHashMap<>();

    private BoulderRollEvents() {
    }

    public static boolean canStart(Player player) {
        return player.level() instanceof ServerLevel level && suitable(player) && grounded(player, level) && !ROLLING.containsKey(player)
                && standFits(level, player, player.position())
                && (BaikaScale.current(player) == 1 || BaikaEvents.full(player));
    }

    public static boolean actionLocked(Player player) {
        if (player.level().isClientSide) {
            return false;
        }
        Roll roll = ROLLING.get(player);
        return roll != null && !roll.restoring;
    }

    public static void start(Player player, float rank) {
        if (!(player.level() instanceof ServerLevel level) || !canStart(player)) {
            return;
        }
        var session = new Roll(level, player, KamuiStep.facing(player.getYRot()), rank, BaikaEvents.full(player));
        ROLLING.put(player, session);
        publish(player, session, Stage.TUCK);
        tone(player, session, Stage.TUCK);
    }

    @SubscribeEvent
    public static void tick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level) || ROLLING.isEmpty()) {
            return;
        }
        for (Player player : List.copyOf(ROLLING.keySet())) {
            Roll roll = ROLLING.get(player);
            if (roll == null || roll.level != level) {
                continue;
            }
            if (!player.isAlive() || player.level() != level) {
                clear(player, roll);
            } else if (roll.restoring) {
                restore(player, roll, false);
            } else if (!suitable(player) || !grounded(player, level) || player.position().distanceToSqr(roll.lastFeet) > 1.2 * 1.2) {
                restore(player, roll, false);
            } else if (level.getGameTime() - roll.start >= BoulderRoll.WINDUP) {
                advance(player, roll);
            }
        }
    }

    private static void advance(Player player, Roll roll) {
        Vec3 start = player.position();
        if (roll.index == 0) {
            BoulderRoll.apply(player, roll.radius);
            tone(player, roll, Stage.ROLL);
        }
        Vec3 desired = roll.direction.scale(BoulderRoll.step(roll.index++));
        Vec3 allowed = safeStep(player, roll, start, desired);
        if (allowed.horizontalDistanceSqr() > 0.0001) {
            player.move(MoverType.SELF, allowed);
        }
        Vec3 end = player.position();
        roll.lastFeet = end;
        double distance = start.subtract(end).horizontalDistance();
        if (distance > 0.025) {
            roll.distance += distance;
            if (standFits(roll.level, player, end)) {
                roll.lastSafe = end;
            }
            hit(player, roll, start.add(0, roll.radius, 0), end.add(0, roll.radius, 0));
            sync(player);
            if (roll.index % 2 == 0 && grounded(player, roll.level)) {
                dust(player, roll, 5);
            }
            publish(player, roll, Stage.ROLL);
        }
        if (distance < desired.horizontalDistance() - 0.10) {
            restore(player, roll, true);
        } else if (roll.index >= BoulderRoll.segments()) {
            restore(player, roll, false);
        }
    }

    private static Vec3 safeStep(Player player, Roll roll, Vec3 feet, Vec3 movement) {
        int samples = Math.max(1, (int) Math.ceil(movement.horizontalDistance() / 0.12));
        for (int i = 1; i <= samples; i++) {
            Vec3 probe = feet.add(movement.scale(i / (double) samples));
            if (!standFits(roll.level, player, probe) && !standFits(roll.level, player, probe.add(0, 0.58, 0))) {
                return movement.scale((i - 1) / (double) samples);
            }
        }
        return movement;
    }

    private static boolean standFits(ServerLevel level, Player player, Vec3 feet) {
        float scale = BaikaScale.current(player);
        double half = 0.3 * scale;
        AABB box = new AABB(feet.x - half, feet.y, feet.z - half, feet.x + half, feet.y + 1.8 * scale, feet.z + half);
        var border = level.getWorldBorder();
        if (!KamuiStep.insideBorder(box, border.getMinX(), border.getMinZ(), border.getMaxX(), border.getMaxZ())) {
            return false;
        }
        for (double x : new double[]{box.minX, box.maxX}) {
            for (double z : new double[]{box.minZ, box.maxZ}) {
                if (!level.hasChunkAt(BlockPos.containing(x, box.minY, z))) {
                    return false;
                }
            }
        }
        return !level.getBlockCollisions(player, box.deflate(0.002)).iterator().hasNext();
    }

    private static boolean grounded(Player player, ServerLevel level) {
        return player.onGround()
                || level.getBlockCollisions(player, player.getBoundingBox().deflate(0.01).move(0, -0.13, 0)).iterator().hasNext();
    }

    private static void hit(Player actor, Roll roll, Vec3 from, Vec3 end) {
        var bounds = new AABB(from, end).inflate(roll.radius);
        for (LivingEntity target : roll.level.getEntitiesOfClass(LivingEntity.class, bounds,
                enemy -> !roll.hit.contains(enemy.getUUID()) && eligible(actor, enemy))) {
            Vec3 point = BoulderRoll.contact(from, end, target.getBoundingBox(), roll.radius);
            if (point == null || roll.level.clip(new ClipContext(end, point, ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE, actor)).getType() != HitResult.Type.MISS) {
                continue;
            }
            roll.hit.add(target.getUUID());
            if (target.hurt(actor.damageSources().playerAttack(actor), Math.min(roll.damage, target instanceof Player ? 7f : 16f))) {
                target.knockback(roll.radius > 0.7f ? 1.63 : 1.16, -roll.direction.x, -roll.direction.z);
                target.hurtMarked = true;
            }
        }
    }

    private static void restore(Player player, Roll roll, boolean bump) {
        if (!roll.restoring && bump) {
            dust(player, roll, 13);
            tone(player, roll, Stage.BUMP);
        }
        roll.restoring = true;
        if (!standFits(roll.level, player, player.position())) {
            if (player.position().distanceToSqr(roll.lastSafe) <= 2.25 && standFits(roll.level, player, roll.lastSafe)) {
                player.setPos(roll.lastSafe);
                sync(player);
            } else {
                if (roll.level.getGameTime() % 5 == 0) {
                    publish(player, roll, Stage.HOLD);
                }
                return;
            }
        }
        BoulderRoll.apply(player, 0);
        publish(player, roll, bump ? Stage.BUMP : Stage.SETTLE);
        ROLLING.remove(player);
    }

    private static void clear(Player player, Roll roll) {
        BoulderRoll.apply(player, 0);
        publish(player, roll, Stage.SETTLE);
        ROLLING.remove(player);
    }

    @SubscribeEvent
    public static void leave(EntityLeaveLevelEvent event) {
        if (!event.getLevel().isClientSide && event.getEntity() instanceof Player player) {
            Roll roll = ROLLING.get(player);
            if (roll != null) {
                clear(player, roll);
            }
        }
    }

    @SubscribeEvent
    public static void blockNormalAttack(AttackEntityEvent event) {
        if (actionLocked(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    private static boolean suitable(Player player) {
        return player.isAlive() && !player.isSpectator() && !player.noPhysics && !player.isPassenger() && !player.isSleeping()
                && !player.isInWaterOrBubble() && !player.isFallFlying() && !player.onClimbable()
                && !AdamantineChainEvents.sealed(player) && !KaitenEvents.spinning(player)
                && player.getCapability(NinjaCapabilityHandler.NINJA_DATA).map(data -> !data.isGiantForm()).orElse(false);
    }

    private static boolean eligible(Player player, LivingEntity target) {
        return target != player && target.isAlive() && target.isPickable() && !target.isSpectator() && !target.noPhysics
                && !player.isAlliedTo(target) && !Faction.sameSide(player, target)
                && !(target instanceof TamableAnimal tame && tame.isOwnedBy(player))
                && (!(target instanceof Player other) || !other.isCreative() && player.canHarmPlayer(other));
    }

    private static void sync(Player player) {
        player.setDeltaMovement(0, player.getDeltaMovement().y, 0);
        if (player instanceof ServerPlayer server) {
            server.connection.teleport(player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
        }
    }

    private static void dust(Player player, Roll roll, int count) {
        var block = roll.level.getBlockState(player.getOnPos());
        if (!block.isAir()) {
            roll.level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, block), player.getX(),
                    player.getY() + 0.16, player.getZ(), count, roll.radius * 0.6, 0.075, roll.radius * 0.6, 0.07);
        }
    }

    private static void tone(Player player, Roll roll, Stage stage) {
        var sound = switch (stage) {
            case TUCK -> NarutoSounds.BOULDER_TUCK.get();
            case BUMP -> NarutoSounds.BOULDER_BUMP.get();
            default -> NarutoSounds.BOULDER_ROLL.get();
        };
        roll.level.playSound(null, player.blockPosition(), sound, SoundSource.PLAYERS, 0.92f, roll.radius > 0.7f ? 0.8f : 1);
    }

    private static void publish(Player player, Roll roll, Stage stage) {
        var event = new BoulderRollPacket(roll.level.dimension().location(), player.getId(), stage, player.position(),
                roll.direction, roll.radius, (float) BoulderRoll.rotation(roll.distance, roll.radius));
        PacketHandler.sendNear(event, roll.level, event.feet(), 38);
    }

    private static final class Roll {
        private final ServerLevel level;
        private final long start;
        private final Vec3 direction;
        private final float radius;
        private final float damage;
        private final Set<UUID> hit = new HashSet<>();
        private Vec3 lastSafe;
        private Vec3 lastFeet;
        private boolean restoring;
        private double distance;
        private int index;

        private Roll(ServerLevel level, Player player, Vec3 direction, float rank, boolean large) {
            this.level = level;
            this.start = level.getGameTime();
            this.direction = direction;
            this.radius = BoulderRoll.radius(large);
            this.damage = (large ? 11.5f : 8f) * rank;
            this.lastSafe = player.position();
            this.lastFeet = this.lastSafe;
        }
    }
}
