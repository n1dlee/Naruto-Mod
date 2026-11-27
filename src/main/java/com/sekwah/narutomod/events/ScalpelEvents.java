package com.sekwah.narutomod.events;

import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.abilities.NarutoAbilities;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.capabilities.NinjaCapabilityHandler;
import com.sekwah.narutomod.network.PacketHandler;
import com.sekwah.narutomod.network.s2c.ScalpelPacket;
import com.sekwah.narutomod.sounds.NarutoSounds;
import com.sekwah.narutomod.util.Faction;
import com.sekwah.narutomod.util.JutsuVfx;
import com.sekwah.narutomod.util.ScalpelEdge;
import com.sekwah.narutomod.util.ScalpelEdge.Phase;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.WeakHashMap;

@Mod.EventBusSubscriber(modid = NarutoMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ScalpelEvents {
    private static final Map<Player, Grip> GRIPS = new WeakHashMap<>();

    private ScalpelEvents() {
    }

    public static boolean freeHand(Player player) {
        return player.getMainHandItem().isEmpty() && !player.isUsingItem() && !player.isSpectator() && player.isAlive()
                && !player.noPhysics && !AdamantineChainEvents.sealed(player);
    }

    public static void maintain(Player player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        long now = level.getGameTime();
        Grip grip = GRIPS.get(player);
        if (grip == null || grip.level != level) {
            grip = new Grip(level, now);
            GRIPS.put(player, grip);
            show(player, grip, Phase.FORM, Vec3.ZERO);
            level.playSound(null, player.blockPosition(), NarutoSounds.SCALPEL_FORM.get(), SoundSource.PLAYERS, 0.70f, 1);
        }
        grip.lastTick = now;
        if (grip.strike != null && grip.strike.tick() != now) {
            grip.strike = null;
        }
        if (now - grip.start >= ScalpelEdge.FORM && now >= grip.recoverUntil && now >= grip.nextUpdate) {
            grip.nextUpdate = now + 8;
            show(player, grip, Phase.READY, Vec3.ZERO);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void armContact(AttackEntityEvent event) {
        Player player = event.getEntity();
        if (!(player.level() instanceof ServerLevel level) || !(event.getTarget() instanceof LivingEntity target)
                || !eligible(player, target) || !freeHand(player) || player.getAttackStrengthScale(0.5f) < 0.65f) {
            return;
        }
        Grip grip = GRIPS.get(player);
        long now = level.getGameTime();
        if (grip == null || grip.level != level || !ScalpelEdge.ready(now - grip.start, now, grip.recoverUntil)) {
            return;
        }
        grip.strike = null;
        Vec3 hand = palm(player);
        Vec3 eye = player.getEyePosition();
        Vec3 aim = player.getLookAngle();
        var aimed = target.getBoundingBox().inflate(0.11).clip(eye, eye.add(aim.scale(3.1)));
        if (aimed.isEmpty()) {
            return;
        }
        Vec3 point = ScalpelEdge.contact(hand, aimed.get().subtract(hand).normalize(), target.getBoundingBox());
        if (point != null && clear(level, player, hand, point) && clear(level, player, player.getEyePosition(), point)) {
            grip.strike = new Strike(target, now, point);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void confirm(LivingDamageEvent event) {
        LivingEntity target = event.getEntity();
        var source = event.getSource();
        if (!(target.level() instanceof ServerLevel level) || !source.is(DamageTypes.PLAYER_ATTACK)
                || !(source.getDirectEntity() instanceof Player player) || source.getEntity() != player
                || event.getAmount() <= 0 || !freeHand(player) || !eligible(player, target)) {
            return;
        }
        Grip grip = GRIPS.get(player);
        Strike strike = grip == null ? null : grip.strike;
        if (strike == null || grip.level != level || !ScalpelEdge.sameStrike(strike.tick(), level.getGameTime(), strike.target() == target)) {
            return;
        }
        grip.strike = null;
        player.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(data -> {
            if (enabled(data) && data.getChakra() >= ScalpelEdge.CUT_COST) {
                finishCut(player, event, grip, strike, data);
            }
        });
    }

    private static void finishCut(Player player, LivingDamageEvent event, Grip grip, Strike strike, INinjaData data) {
        LivingEntity target = event.getEntity();
        data.useChakra(ScalpelEdge.CUT_COST, 10);
        event.setAmount(event.getAmount() + ScalpelEdge.bonus(data.getRankDamageMultiplier(), target instanceof Player));
        grip.recoverUntil = grip.level.getGameTime() + ScalpelEdge.RECOVER;
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, target instanceof Player ? 20 : 32, 0, false, true));
        target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 26, 0, false, true));
        show(player, grip, Phase.CUT, strike.point());
        grip.level.playSound(null, strike.point().x, strike.point().y, strike.point().z, NarutoSounds.SCALPEL_CUT.get(),
                SoundSource.PLAYERS, 0.77f, 1);
    }

    public static void end(Player player) {
        Grip grip = GRIPS.remove(player);
        if (grip != null) {
            show(player, grip, Phase.RELEASE, Vec3.ZERO);
        }
    }

    @SubscribeEvent
    public static void cleanExpired(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide) {
            return;
        }
        Grip grip = GRIPS.get(player);
        if (grip != null && (grip.level != player.level() || !freeHand(player) || grip.level.getGameTime() - grip.lastTick > 2)) {
            end(player);
        }
    }

    @SubscribeEvent
    public static void leave(EntityLeaveLevelEvent event) {
        if (!event.getLevel().isClientSide && event.getEntity() instanceof Player player) {
            end(player);
        }
    }

    private static boolean enabled(INinjaData data) {
        return data.isNinjaModeEnabled() && data.getToggleAbilityData().getAbilitiesHashSet().contains(NarutoAbilities.CHAKRA_SCALPEL.getId());
    }

    private static boolean eligible(Player player, LivingEntity target) {
        return target != player && target.level() == player.level() && target.isAlive() && !target.isSpectator() && !target.noPhysics
                && !player.isAlliedTo(target) && !Faction.sameSide(player, target)
                && !(target instanceof TamableAnimal tame && tame.isOwnedBy(player))
                && (!(target instanceof Player other) || !other.isCreative() && player.canHarmPlayer(other));
    }

    private static boolean clear(ServerLevel level, Player player, Vec3 from, Vec3 to) {
        return level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player))
                .getType() == HitResult.Type.MISS;
    }

    private static Vec3 palm(Player player) {
        return JutsuVfx.handPosition(player, player.getMainArm() == HumanoidArm.RIGHT ? 1 : -1, 1);
    }

    private static void show(Player player, Grip grip, Phase phase, Vec3 contact) {
        Vec3 hand = player.level() == grip.level ? palm(player) : grip.lastPalm;
        grip.lastPalm = hand;
        PacketHandler.sendNear(new ScalpelPacket(grip.level.dimension().location(), player.getId(), phase, hand,
                phase == Phase.CUT ? contact : hand, player.getLookAngle(), player.getMainArm() == HumanoidArm.RIGHT),
                grip.level, hand, 32);
    }

    private record Strike(LivingEntity target, long tick, Vec3 point) {
    }

    private static final class Grip {
        private final ServerLevel level;
        private final long start;
        private long lastTick;
        private long recoverUntil;
        private long nextUpdate;
        private Vec3 lastPalm = Vec3.ZERO;
        private Strike strike;

        private Grip(ServerLevel level, long start) {
            this.level = level;
            this.start = start;
            this.lastTick = start;
        }
    }
}
