package com.sekwah.narutomod.events;

import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.abilities.NarutoAbilities;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.entity.MangekyoBossEntity;
import com.sekwah.narutomod.entity.jutsuprojectile.YasakaMagatamaEntity;
import com.sekwah.narutomod.network.PacketHandler;
import com.sekwah.narutomod.network.s2c.FinalChakraPacket;
import com.sekwah.narutomod.network.s2c.FinalChakraPacket.Phase;
import com.sekwah.narutomod.network.s2c.FinalChakraPacket.Shape;
import com.sekwah.narutomod.sounds.NarutoSounds;
import com.sekwah.narutomod.util.FinalChakraGeometry;
import com.sekwah.narutomod.util.SusanooFullBody;
import com.sekwah.narutomod.util.SusanooPose;
import com.sekwah.narutomod.util.SusanooForm;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;

@Mod.EventBusSubscriber(modid = NarutoMod.MOD_ID)
public final class YasakaVolleyEvents {
    public static final float COST = 45;
    private static final Map<LivingEntity, Volley> VOLLEYS = new HashMap<>();

    private YasakaVolleyEvents() {
    }

    public static boolean preparing(LivingEntity actor) {
        return VOLLEYS.containsKey(actor);
    }

    static float yawOr(LivingEntity actor, float fallback) {
        var volley = VOLLEYS.get(actor);
        return volley != null ? volley.shot.yaw() : fallback;
    }

    public static void start(ServerPlayer actor, INinjaData data, BiConsumer<ServerPlayer, INinjaData> commit) {
        var caster = new Caster(data::getSusanooStage, () -> data.isNinjaModeEnabled() && data.isSusanooActive(),
                () -> data.getChakra() >= COST, () -> {
            data.useChakra(COST, 20);
            commit.accept(actor, data);
        });
        var shot = new Shot(actor.getLookAngle(), actor.getYRot(), FinalChakraEffects.tint(data),
                (14f + SusanooForm.fromId(data.getSusanooStage()).combatTier() * 7f) * data.getRankDamageMultiplier());
        if (begin(actor, caster, shot)) {
            data.setCastPoseTicks(SusanooPose.THROW_LENGTH);
            data.setLastCastAbilityId(NarutoAbilities.YASAKA_MAGATAMA.getId());
        }
    }

    public static boolean startBoss(MangekyoBossEntity boss, LivingEntity target) {
        Vec3 targetPoint = target.getBoundingBox().getCenter();
        Vec3 aim = targetPoint.subtract(boss.getEyePosition()).normalize();
        Vec3 flat = aim.multiply(1, 0, 1);
        float yaw = flat.lengthSqr() < 1.0e-6 ? boss.getYRot()
                : (float) Math.toDegrees(Math.atan2(-flat.x, flat.z));
        var frame = SusanooPose.frame(boss.position(), yaw);
        int stage = boss.getSusanooStage();
        aim = targetPoint.subtract(SusanooPose.releasePoint(frame, stage, aim, 1)).normalize();
        if (stage == 4) {
            aim = targetPoint.subtract(SusanooPose.releasePoint(frame, stage, aim, 1)).normalize();
        }
        var variant = boss.getVariant();
        var caster = new Caster(boss::getSusanooStage,
                () -> variant.hasSusanoo() && !boss.isChakraExhausted() && target.isAlive()
                        && boss.getTarget() == target && boss.hasLineOfSight(target),
                () -> boss.canUseChakra(COST), () -> boss.useChakra(COST));
        return begin(boss, caster, new Shot(aim, yaw,
                FinalChakraEffects.rgb(variant.susanooRed(), variant.susanooGreen(), variant.susanooBlue()), 14));
    }

    private static boolean begin(LivingEntity actor, Caster caster, Shot shot) {
        if (!(actor.level() instanceof ServerLevel level) || preparing(actor) || SusanooCombat.cutting(actor) || caster.stage().getAsInt() < 2
                || !caster.ready().getAsBoolean() || !caster.canPay().getAsBoolean() || !aliveAndFree(actor)) {
            return false;
        }
        var volley = new Volley(actor, level, caster, shot);
        if (!volley.clear(actor, 0)) {
            return false;
        }
        VOLLEYS.put(actor, volley);
        FinalChakraEffects.broadcast(level, volley.packet(actor, Phase.YASAKA_GATHER));
        var shape = new Shape(volley.hand(actor, 0), shot.aim(), SusanooPose.scale(volley.stage), shot.tint());
        FinalChakraEffects.sound(actor, shape, NarutoSounds.MAGATAMA_GATHER.get(), 0.67f, 1f);
        return true;
    }

    @SubscribeEvent
    public static void tick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel world) || VOLLEYS.isEmpty()) {
            return;
        }
        for (var actor : List.copyOf(VOLLEYS.keySet())) {
            Volley volley = VOLLEYS.get(actor);
            if (volley == null || volley.level != world) {
                continue;
            }
            if (!volley.valid(actor)) {
                cancel(actor);
            } else if (world.getGameTime() >= volley.started + SusanooPose.releaseTick(volley.index)) {
                release(actor, volley);
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
        var volley = VOLLEYS.get(event.getTarget());
        if (volley != null && event.getEntity() instanceof ServerPlayer viewer
                && event.getTarget() instanceof LivingEntity actor) {
            PacketHandler.sendToPlayer(volley.packet(actor, Phase.YASAKA_GATHER), viewer);
        }
    }

    public static void cancel(LivingEntity actor) {
        Volley volley = VOLLEYS.remove(actor);
        if (volley != null) {
            FinalChakraEffects.broadcast(volley.level, volley.packet(actor, Phase.YASAKA_STOP));
        }
    }

    private static void release(LivingEntity actor, Volley volley) {
        if (!volley.clear(actor, volley.index) || volley.index == 0 && !volley.caster.canPay().getAsBoolean()) {
            cancel(actor);
            return;
        }
        Vec3 from = volley.hand(actor, volley.index);
        Vec3 aim = volley.direction(volley.index);
        float scale = SusanooPose.scale(volley.stage);
        var seal = new YasakaMagatamaEntity(actor, from, aim, scale, volley.shot.tint())
                .profile(volley.id, volley.shot.damage(), 2.5 + SusanooForm.fromId(volley.stage).combatTier() * .9);
        if (!volley.level.addFreshEntity(seal)) {
            cancel(actor);
            return;
        }
        if (volley.index == 0) {
            volley.caster.pay().run();
        }
        var shape = new Shape(from, aim, scale, volley.shot.tint());
        FinalChakraEffects.emit(actor, Phase.YASAKA_RELEASE, shape);
        if (++volley.index == SusanooPose.SHOTS) {
            VOLLEYS.remove(actor);
        }
    }

    public static Vec3 direction(Vec3 aim, Vec3 right, int index) {
        return aim.normalize().add(right.normalize().scale((index - 1) * .16)).normalize();
    }

    private static boolean aliveAndFree(LivingEntity actor) {
        return actor.isAlive() && !actor.isSpectator() && !AdamantineChainEvents.sealed(actor);
    }

    private record Caster(IntSupplier stage, BooleanSupplier ready, BooleanSupplier canPay, Runnable pay) {
    }

    private record Shot(Vec3 aim, float yaw, int tint, float damage) {
    }

    private static final class Volley {
        private final UUID id = UUID.randomUUID();
        private final ServerLevel level;
        private final int stage;
        private final Vec3 from;
        private final long started;
        private final Caster caster;
        private final Shot shot;
        private int index;

        private Volley(LivingEntity actor, ServerLevel level, Caster caster, Shot shot) {
            this.level = level;
            this.caster = caster;
            this.shot = shot;
            this.stage = caster.stage().getAsInt();
            this.from = actor.position();
            this.started = level.getGameTime();
        }

        private boolean valid(LivingEntity actor) {
            return actor.level() == level && actor.position().distanceToSqr(from) <= 4 && aliveAndFree(actor)
                    && caster.stage().getAsInt() == stage && caster.ready().getAsBoolean();
        }

        private Vec3 direction(int shotIndex) {
            return YasakaVolleyEvents.direction(shot.aim(), SusanooPose.frame(from, shot.yaw()).right(), shotIndex);
        }

        private Vec3 hand(LivingEntity actor, int shotIndex) {
            return SusanooPose.releasePoint(SusanooPose.frame(actor.position(), shot.yaw()), stage, shot.aim(), shotIndex);
        }

        private boolean clear(LivingEntity actor, int shotIndex) {
            var frame = SusanooPose.frame(actor.position(), shot.yaw());
            Vec3 muzzle = hand(actor, shotIndex);
            double age = SusanooPose.releaseTick(shotIndex);
            var arm = SusanooPose.throwArm(frame, stage, shot.aim(), age);
            if (!FinalChakraGeometry.clear(level, actor, arm.shoulder(), arm.elbow())
                    || !FinalChakraGeometry.clear(level, actor, arm.elbow(), muzzle)) {
                return false;
            }
            Vec3 groundSource = actor.position().add(0, 1.9, 0);
            Vec3 source = SusanooFullBody.hasLegs(stage) ? actor.position().add(0, 12.6 * SusanooFullBody.scale(stage), 0) : groundSource;
            if (SusanooFullBody.standing(stage) && !FinalChakraGeometry.clear(level, actor, groundSource, source)) return false;
            return FinalChakraGeometry.clear(level, actor, source, muzzle)
                    && FinalChakraGeometry.clear(level, actor, muzzle, muzzle.add(direction(shotIndex).scale(.6)));
        }

        private FinalChakraPacket packet(LivingEntity actor, Phase phase) {
            var shape = new Shape(actor.position(), shot.aim(), stage, shot.tint());
            return new FinalChakraPacket(level.dimension().location(), actor.getId(), phase, shape, started, shot.yaw());
        }
    }
}
