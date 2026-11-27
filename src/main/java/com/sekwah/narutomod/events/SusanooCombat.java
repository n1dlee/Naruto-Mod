package com.sekwah.narutomod.events;

import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.entity.MangekyoBossEntity;
import com.sekwah.narutomod.entity.MangekyoBossVariant;
import com.sekwah.narutomod.network.PacketHandler;
import com.sekwah.narutomod.network.s2c.FinalChakraPacket;
import com.sekwah.narutomod.network.s2c.FinalChakraPacket.Phase;
import com.sekwah.narutomod.network.s2c.FinalChakraPacket.Shape;
import com.sekwah.narutomod.sounds.NarutoSounds;
import com.sekwah.narutomod.util.FinalChakraGeometry;
import com.sekwah.narutomod.util.KamuiStep;
import com.sekwah.narutomod.util.SpectralSword;
import com.sekwah.narutomod.util.SusanooForm;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.SpectralArrow;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.function.BooleanSupplier;

@Mod.EventBusSubscriber(modid = NarutoMod.MOD_ID)
public final class SusanooCombat {
    private static final Map<LivingEntity, Cut> CUTS = new WeakHashMap<>();

    private SusanooCombat() {
    }

    public static boolean cutting(LivingEntity actor) {
        return CUTS.containsKey(actor);
    }

    static float yawOr(LivingEntity actor, float fallback) {
        var cut = CUTS.get(actor);
        return cut != null ? cut.strike().yaw() : fallback;
    }

    public static boolean start(ServerPlayer actor, INinjaData data) {
        int stage = data.getSusanooStage();
        if (stage < 2 || !data.isNinjaModeEnabled() || !data.isSusanooActive() || !canStart(actor)) {
            return false;
        }
        var strike = new Strike(actor.position(), actor.getYRot(), stage, FinalChakraEffects.tint(data),
                (SusanooForm.perfect(stage) ? 38 : 8 + SusanooForm.fromId(stage).combatTier() * 3) * data.getRankDamageMultiplier());
        begin(actor, actor.serverLevel(), strike,
                () -> data.isNinjaModeEnabled() && data.isSusanooActive() && data.getSusanooStage() == stage
                        && Math.abs(Mth.wrapDegrees(actor.getYRot() - strike.yaw())) <= 45);
        return true;
    }

    public static boolean startBoss(MangekyoBossEntity boss, LivingEntity target) {
        int stage = boss.getSusanooStage();
        if (!(boss.level() instanceof ServerLevel world) || boss.getVariant() != MangekyoBossVariant.MADARA
                || SusanooForm.fromId(stage).combatTier() < 2 || boss.isChakraExhausted() || !canStart(boss) || !boss.hasLineOfSight(target)
                || !boss.useChakra(40)) {
            return false;
        }
        Vec3 delta = target.position().subtract(boss.position());
        float yaw = delta.horizontalDistanceSqr() < 1e-6 ? boss.getYRot() : (float) Math.toDegrees(Math.atan2(-delta.x, delta.z));
        var variant = boss.getVariant();
        var strike = new Strike(boss.position(), yaw, stage,
                FinalChakraEffects.rgb(variant.susanooRed(), variant.susanooGreen(), variant.susanooBlue()), stage == 4 ? 24 : 18);
        begin(boss, world, strike, () -> boss.getSusanooStage() == stage && !boss.isChakraExhausted()
                && boss.getTarget() == target && target.isAlive());
        return true;
    }

    private static boolean canStart(LivingEntity actor) {
        return aliveAndFree(actor) && !cutting(actor) && !YasakaVolleyEvents.preparing(actor);
    }

    private static void begin(LivingEntity actor, ServerLevel world, Strike strike, BooleanSupplier ready) {
        var cut = new Cut(world, strike, world.getGameTime(), new HashSet<>(), ready);
        CUTS.put(actor, cut);
        FinalChakraEffects.broadcast(world, cut.packet(actor, Phase.SWORD));
    }

    @SubscribeEvent
    public static void tick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel world) || CUTS.isEmpty()) {
            return;
        }
        for (LivingEntity actor : List.copyOf(CUTS.keySet())) {
            Cut cut = CUTS.get(actor);
            if (cut != null && cut.world() == world) {
                tickCut(actor, cut);
            }
        }
    }

    @SubscribeEvent
    public static void leave(EntityLeaveLevelEvent event) {
        if (event.getEntity() instanceof LivingEntity actor && !event.getLevel().isClientSide()) {
            cancel(actor);
        }
    }

    @SubscribeEvent
    public static void track(PlayerEvent.StartTracking event) {
        if (event.getEntity() instanceof ServerPlayer viewer && event.getTarget() instanceof LivingEntity actor) {
            Cut cut = CUTS.get(actor);
            if (cut != null) {
                PacketHandler.sendToPlayer(cut.packet(actor, Phase.SWORD), viewer);
            }
        }
    }

    public static void cancel(LivingEntity actor) {
        Cut cut = CUTS.remove(actor);
        if (cut != null) {
            FinalChakraEffects.broadcast(cut.world(), cut.packet(actor, Phase.SWORD_STOP));
        }
    }

    private static void tickCut(LivingEntity actor, Cut cut) {
        Strike strike = cut.strike();
        int tick = (int) (actor.level().getGameTime() - cut.start());
        if (tick >= SpectralSword.length(strike.stage())) {
            CUTS.remove(actor);
        } else if (!aliveAndFree(actor) || cut.world() != actor.level() || !cut.ready().getAsBoolean()
                || actor.position().distanceToSqr(strike.feet()) > (SusanooForm.perfect(strike.stage()) ? 9 : 2.25)) {
            cancel(actor);
        } else if (SpectralSword.active(strike.stage(), tick)) {
            if (tick == SpectralSword.startTick(strike.stage())) {
                FinalChakraEffects.sound(actor, strike.shape(), NarutoSounds.SUSANOO_CUT.get(), 1.05f,
                        SusanooForm.perfect(strike.stage()) ? 0.76f : 1.17f);
            }
            hit(actor, cut, tick);
        }
    }

    private static void hit(LivingEntity actor, Cut cut, int tick) {
        Strike strike = cut.strike();
        boolean giant = SusanooForm.perfect(strike.stage());
        double range = giant ? 23 : 10;
        var volume = new AABB(strike.feet(), strike.feet()).inflate(range, giant ? 16 : 5, range);
        var source = actor instanceof ServerPlayer player ? actor.damageSources().playerAttack(player)
                : actor.damageSources().mobAttack(actor);
        for (LivingEntity victim : cut.world().getEntitiesOfClass(LivingEntity.class, volume,
                target -> !cut.hit().contains(target.getUUID()) && FinalChakraGeometry.enemy(actor, target))) {
            if (clearContact(actor, victim, cut, tick, giant ? 0.95 : 0.27)
                    && cut.hit().add(victim.getUUID()) && victim.hurt(source, strike.damage())) {
                Vec3 away = victim.position().subtract(strike.feet()).multiply(1, 0, 1).normalize();
                victim.knockback(giant ? 2.8 : 1.1, -away.x, -away.z);
                victim.hurtMarked = true;
            }
        }
    }

    private static boolean clearContact(LivingEntity actor, LivingEntity victim, Cut cut, int tick, double width) {
        Strike strike = cut.strike();
        for (double offset : new double[]{-0.5, 0, 0.5}) {
            var blade = SpectralSword.sample(strike.feet(), strike.yaw(), strike.stage(), tick + offset);
            Vec3 contact = blade.contact(victim.getBoundingBox(), width);
            Vec3 chest = strike.feet().add(0, strike.stage() == 4 ? 12.4 : 1.62, 0);
            if (contact != null && FinalChakraGeometry.clear(cut.world(), actor, chest, blade.guard())
                    && FinalChakraGeometry.clear(cut.world(), actor, blade.guard(), contact)) {
                return true;
            }
        }
        return false;
    }

    private static boolean aliveAndFree(LivingEntity actor) {
        return actor.isAlive() && !actor.isSpectator() && !AdamantineChainEvents.sealed(actor);
    }

    public static void deflect(ServerPlayer actor, int stage) {
        var level = actor.serverLevel();
        Vec3 center = actor.position().add(0, actor.getBbHeight() * 0.5, 0);
        for (Projectile arrow : level.getEntitiesOfClass(Projectile.class,
                actor.getBoundingBox().inflate(1.1 + SusanooForm.fromId(stage).combatTier() * 0.35), entity -> entity.getOwner() != actor
                        && (entity.getClass() == Arrow.class || entity.getClass() == SpectralArrow.class))) {
            Vec3 velocity = arrow.getDeltaMovement();
            if (velocity.dot(center.subtract(arrow.position())) > 0.08
                    && FinalChakraGeometry.clear(level, actor, arrow.position(), center)) {
                arrow.setDeltaMovement(velocity.scale(-0.55));
                arrow.hurtMarked = true;
            }
        }
    }

    private record Cut(ServerLevel world, Strike strike, long start, Set<UUID> hit, BooleanSupplier ready) {
        private FinalChakraPacket packet(LivingEntity actor, Phase phase) {
            return new FinalChakraPacket(world.dimension().location(), actor.getId(), phase, strike.shape(), start, strike.yaw());
        }
    }

    private record Strike(Vec3 feet, float yaw, int stage, int tint, float damage) {
        private Shape shape() {
            return new Shape(feet, KamuiStep.facing(yaw), stage, tint);
        }
    }
}
