package com.sekwah.narutomod.events;

import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.capabilities.NinjaCapabilityHandler;
import com.sekwah.narutomod.network.PacketHandler;
import com.sekwah.narutomod.network.s2c.KamuiPhasePacket;
import com.sekwah.narutomod.sounds.NarutoSounds;
import com.sekwah.narutomod.util.KamuiPhaseWindow;
import com.sekwah.narutomod.util.KamuiPhaseWindow.Stage;
import com.sekwah.narutomod.util.KamuiStep;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.EntityTravelToDimensionEvent;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.WeakHashMap;

@Mod.EventBusSubscriber(modid = NarutoMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class KamuiPhaseEvents {
    public static final ResourceLocation ID = new ResourceLocation(NarutoMod.MOD_ID, "kamui_phase");
    public static final ResourceLocation SCOUT = new ResourceLocation(NarutoMod.MOD_ID, "byakugan_scout");
    private static final Map<ServerPlayer, Lease> ACTIVE = new WeakHashMap<>();
    private static final Map<ServerPlayer, Long> LOCKOUT = new WeakHashMap<>();
    private static final Map<Player, Long> CLIENT_COLLISION = new WeakHashMap<>();

    private KamuiPhaseEvents() {
    }

    public static boolean canStart(Player actor) {
        return actor instanceof ServerPlayer player && player.isAlive() && !player.isSpectator()
                && !player.isPassenger() && !player.isVehicle() && !player.isSleeping() && !player.isFallFlying()
                && !player.isUsingItem() && !player.noPhysics && !ACTIVE.containsKey(player)
                && player.serverLevel().getGameTime() >= LOCKOUT.getOrDefault(player, Long.MIN_VALUE)
                && KamuiStep.safe(player.serverLevel(), player, player.position());
    }

    public static boolean reconcile(Player actor, boolean requested) {
        if (!(actor instanceof ServerPlayer player)) {
            return CLIENT_COLLISION.getOrDefault(actor, Long.MIN_VALUE) >= actor.level().getGameTime();
        }
        return reconcileServer(player, requested);
    }

    public static void clientState(Player local, Stage stage) {
        if (stage.collisionOff()) {
            CLIENT_COLLISION.put(local, local.level().getGameTime() + 11);
        } else {
            CLIENT_COLLISION.remove(local);
        }
    }

    public static boolean actionLocked(Player actor) {
        return actor instanceof ServerPlayer player && ACTIVE.containsKey(player);
    }

    public static boolean isPhased(LivingEntity actor) {
        if (actor instanceof ServerPlayer player) {
            Lease lease = ACTIVE.get(player);
            return lease != null && lease.stage.protects();
        }
        return actor instanceof com.sekwah.narutomod.entity.MangekyoBossEntity boss && boss.isKamuiPhased();
    }

    private static boolean reconcileServer(ServerPlayer player, boolean requested) {
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        if (now >= LOCKOUT.getOrDefault(player, Long.MAX_VALUE)) {
            LOCKOUT.remove(player);
        }
        Lease lease = ACTIVE.get(player);
        if (lease == null) {
            if (requested && canStart(player)) {
                lease = new Lease(level, player.position(), now);
                ACTIVE.put(player, lease);
                signal(player, lease, Stage.OPENING, 0);
                sound(player, 0.49f, 1.12f);
            } else if (requested) {
                removeToggle(player);
            }
            return false;
        }
        if (level != lease.level || !player.isAlive() || player.isSpectator()) {
            drop(player);
            return false;
        }
        return advance(player, requested && !player.isPassenger() && !player.isSleeping(), lease, now);
    }

    private static boolean advance(ServerPlayer player, boolean requested, Lease lease, long now) {
        int age = (int) (now - lease.start);
        boolean within = player.position().distanceToSqr(lease.origin) <= KamuiPhaseWindow.TETHER * KamuiPhaseWindow.TETHER
                && withinWorld(player);
        Stage stage = lease.stage == Stage.RESOLVING ? Stage.RESOLVING : KamuiPhaseWindow.advance(age, requested, within);
        if (stage == Stage.RESOLVING) {
            return resolve(player, lease, within);
        }
        if (KamuiStep.safe(lease.level, player, player.position())) {
            lease.lastSafe = player.position();
        }
        if (stage != lease.stage || stage == Stage.PHASED && age % 4 == 0) {
            signal(player, lease, stage, age);
        }
        return stage.collisionOff();
    }

    private static boolean withinWorld(ServerPlayer player) {
        ServerLevel world = player.serverLevel();
        BlockPos feet = player.blockPosition();
        return feet.getY() > world.getMinBuildHeight() && feet.getY() + 2 < world.getMaxBuildHeight()
                && world.hasChunkAt(feet) && world.getWorldBorder().isWithinBounds(feet);
    }

    private static boolean resolve(ServerPlayer player, Lease lease, boolean within) {
        removeToggle(player);
        if (lease.stage == Stage.OPENING) {
            seal(player, lease, player.position(), true);
            return false;
        }
        Vec3 safe = destination(player, lease, within);
        if (safe != null) {
            seal(player, lease, safe, true);
            return false;
        }
        if (lease.stage != Stage.RESOLVING || lease.level.getGameTime() % 4 == 0) {
            signal(player, lease, Stage.RESOLVING, 0);
        }
        if (player.position().distanceToSqr(lease.lastSafe) > 0.12) {
            position(player, lease.lastSafe, true);
        }
        player.setDeltaMovement(Vec3.ZERO);
        return true;
    }

    private static @Nullable Vec3 destination(ServerPlayer player, Lease lease, boolean within) {
        Vec3 origin = lease.origin;
        Vec3 cursor = within ? player.position() : lease.lastSafe;
        Vec3 candidate = KamuiPhaseWindow.settle(cursor, lease.lastSafe, origin, pos -> KamuiStep.safe(lease.level, player, pos));
        if (candidate != null) {
            return candidate;
        }
        BlockPos column = BlockPos.containing(origin);
        if (!lease.level.hasChunkAt(column)) {
            return null;
        }
        int top = lease.level.getHeight(Heightmap.Types.MOTION_BLOCKING, column.getX(), column.getZ());
        Vec3 surface = new Vec3(column.getX() + 0.5, top + 0.08, column.getZ() + 0.5);
        return KamuiStep.safe(lease.level, player, surface) ? surface : null;
    }

    private static void seal(ServerPlayer player, Lease lease, Vec3 safe, boolean network) {
        if (player.position().distanceToSqr(safe) > 0.0001) {
            position(player, safe, network);
        }
        ACTIVE.remove(player);
        LOCKOUT.put(player, lease.level.getGameTime() + KamuiPhaseWindow.RETURN + 8);
        if (network) {
            signal(player, lease, Stage.SEALED, 0);
            sound(player, 0.51f, 0.82f);
        }
    }

    private static void position(ServerPlayer player, Vec3 safe, boolean network) {
        if (network) {
            player.connection.teleport(safe.x, safe.y, safe.z, player.getYRot(), player.getXRot());
        } else {
            player.moveTo(safe.x, safe.y, safe.z, player.getYRot(), player.getXRot());
        }
        player.setDeltaMovement(Vec3.ZERO);
        player.hurtMarked = true;
    }

    private static void signal(ServerPlayer player, Lease lease, Stage stage, int age) {
        lease.stage = stage;
        Vec3 contact = player.position().add(0, player.getBbHeight() * 0.60, 0);
        Vec3 normal = Vec3.ZERO;
        if (stage == Stage.PHASED && lease.lastSafe.distanceToSqr(player.position()) > 0.05) {
            var hit = lease.level.clip(new ClipContext(lease.lastSafe.add(0, player.getBbHeight() * 0.60, 0), contact,
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
            if (hit.getType() == HitResult.Type.BLOCK) {
                contact = hit.getLocation();
                normal = Vec3.atLowerCornerOf(hit.getDirection().getNormal());
            }
        }
        PacketHandler.sendNear(new KamuiPhasePacket(lease.level.dimension().location(), player.getId(), stage, age, contact, normal),
                lease.level, player.position(), 40);
    }

    private static void sound(ServerPlayer player, float volume, float pitch) {
        player.serverLevel().playSound(null, player.getX(), player.getY() + 1, player.getZ(), NarutoSounds.KAMUI_FOLD.get(),
                SoundSource.PLAYERS, volume, pitch);
    }

    @SubscribeEvent
    public static void onAttack(LivingAttackEvent event) {
        var source = event.getSource();
        if (event.getEntity().level().isClientSide || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return;
        }
        if (isPhased(event.getEntity()) && (source.getEntity() != null || source.is(DamageTypeTags.IS_PROJECTILE))
                || source.getDirectEntity() instanceof Player attacker && actionLocked(attacker)
                || source.getDirectEntity() instanceof com.sekwah.narutomod.entity.MangekyoBossEntity boss && boss.isKamuiPhased()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void projectile(ProjectileImpactEvent event) {
        if (!event.getProjectile().level().isClientSide && event.getRayTraceResult() instanceof EntityHitResult hit
                && hit.getEntity() instanceof LivingEntity actor && isPhased(actor)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void melee(AttackEntityEvent event) {
        if (actionLocked(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void interact(PlayerInteractEvent event) {
        if (event.isCancelable() && actionLocked(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void travel(EntityTravelToDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && ACTIVE.get(player) != null) {
            Lease lease = ACTIVE.get(player);
            if (resolve(player, lease, true)) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && ACTIVE.get(player) != null) {
            Lease lease = ACTIVE.get(player);
            Vec3 safe = destination(player, lease, true);
            if (safe != null) {
                seal(player, lease, safe, false);
            }
            drop(player);
        }
        com.sekwah.narutomod.util.PhasePhysics.reconcile(event.getEntity(), false, false);
    }

    @SubscribeEvent
    public static void leave(EntityLeaveLevelEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            drop(player);
        }
    }

    private static void drop(ServerPlayer player) {
        ACTIVE.remove(player);
        LOCKOUT.remove(player);
        removeToggle(player);
    }

    private static void removeToggle(ServerPlayer player) {
        player.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(data -> data.getToggleAbilityData().removeAbility(ID));
    }

    private static final class Lease {
        private final ServerLevel level;
        private final Vec3 origin;
        private final long start;
        private Vec3 lastSafe;
        private Stage stage = Stage.OPENING;

        private Lease(ServerLevel level, Vec3 origin, long start) {
            this.level = level;
            this.origin = origin;
            this.start = start;
            this.lastSafe = origin;
        }
    }
}
