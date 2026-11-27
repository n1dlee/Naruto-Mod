package com.sekwah.narutomod.events;

import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.capabilities.NinjaCapabilityHandler;
import com.sekwah.narutomod.network.PacketHandler;
import com.sekwah.narutomod.network.s2c.PretaGuardPacket;
import com.sekwah.narutomod.sounds.NarutoSounds;
import com.sekwah.narutomod.util.JutsuAim;
import com.sekwah.narutomod.util.PretaReservoir;
import com.sekwah.narutomod.util.PretaReservoir.Stage;
import com.sekwah.narutomod.util.ProjectileAxis;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.WeakHashMap;

@Mod.EventBusSubscriber(modid = NarutoMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class PretaGuardEvents {
    private static final ResourceLocation ID = new ResourceLocation(NarutoMod.MOD_ID, "preta_path");
    private static final TagKey<DamageType> DAMAGE = TagKey.create(Registries.DAMAGE_TYPE, key());
    private static final TagKey<EntityType<?>> PROJECTILES = TagKey.create(Registries.ENTITY_TYPE, key());
    private static final String NATURE = "NarutoPretaNature";
    private static final int DEFAULT_TINT = 0xAE92EB;
    private static final Map<Player, Guard> GUARDS = new WeakHashMap<>();
    private static final Map<Player, Integer> RETRY = new WeakHashMap<>();

    private PretaGuardEvents() {
    }

    public static boolean start(Player actor, INinjaData data) {
        if (!(actor.level() instanceof ServerLevel level) || !available(actor, data) || GUARDS.containsKey(actor)
                || actor.tickCount < RETRY.getOrDefault(actor, 0)) {
            return false;
        }
        var guard = new Guard(level, level.getGameTime(), data);
        GUARDS.put(actor, guard);
        data.setCastPoseTicks(PretaReservoir.WINDUP);
        Vec3 center = JutsuAim.center(actor);
        send(actor, guard, Stage.OPEN, center, DEFAULT_TINT);
        tone(guard, center, NarutoSounds.PRETA_OPEN.get(), 0.68f);
        return true;
    }

    public static boolean maintain(Player actor) {
        Guard guard = GUARDS.get(actor);
        if (guard == null || actor.level() != guard.level || guard.pool.exhausted() || !available(actor, guard.data)) {
            return false;
        }
        long age = guard.level.getGameTime() - guard.start;
        if (age >= PretaReservoir.WINDUP && (!guard.ready || (age - PretaReservoir.WINDUP) % 12 == 0)) {
            guard.ready = true;
            send(actor, guard, Stage.READY, JutsuAim.center(actor), DEFAULT_TINT);
        }
        return true;
    }

    public static void end(Player player) {
        var guard = GUARDS.remove(player);
        if (guard != null) {
            RETRY.put(player, player.tickCount + PretaReservoir.REST);
            send(player, guard, Stage.FADE, JutsuAim.center(player), DEFAULT_TINT);
        }
    }

    @SubscribeEvent
    public static void spawn(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide && event.getEntity() instanceof Projectile shot && shot.getType().is(PROJECTILES)) {
            if (shot.getOwner() != null && !shot.getPersistentData().contains(NATURE)) {
                shot.getPersistentData().putBoolean(NATURE, sage(shot.getOwner()));
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void projectile(ProjectileImpactEvent event) {
        Projectile shot = event.getProjectile();
        if (!shot.getType().is(PROJECTILES) || shot.isRemoved()
                || !(event.getRayTraceResult() instanceof EntityHitResult hit) || !(hit.getEntity() instanceof Player actor)) {
            return;
        }
        Guard guard = accepting(actor, shot.getOwner(), shot);
        if (guard == null) {
            return;
        }
        String type = BuiltInRegistries.ENTITY_TYPE.getKey(shot.getType()).getPath();
        float load = switch (type) {
            case "great_fireball", "rasenshuriken" -> 10;
            case "ice_spear" -> 3;
            default -> 6;
        };
        var receipt = guard.pool.absorb(load, true);
        if (receipt.absorbed() > 0) {
            event.setCanceled(true);
            shot.discard();
            credit(actor, guard, receipt, hit.getLocation(), tint(type));
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void damage(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof Player actor) || !event.getSource().is(DAMAGE)) {
            return;
        }
        var source = event.getSource();
        Guard guard = accepting(actor, source.getEntity(), source.getDirectEntity());
        if (guard == null) {
            return;
        }
        var receipt = guard.pool.absorb(event.getAmount(), false);
        if (receipt.absorbed() > 0) {
            event.setAmount(event.getAmount() - receipt.absorbed());
            Vec3 position = source.getSourcePosition();
            if (position == null && source.getEntity() != null) {
                position = source.getEntity().position();
            }
            credit(actor, guard, receipt, position == null ? JutsuAim.center(actor) : position, tint(source.getMsgId()));
        }
    }

    private static @Nullable Guard accepting(Player actor, @Nullable Entity source, @Nullable Entity direct) {
        Guard guard = GUARDS.get(actor);
        if (guard == null || actor.level() != guard.level || !guard.ready || guard.pool.exhausted()
                || !available(actor, guard.data) || !guard.data.getToggleAbilityData().getAbilitiesHashSet().contains(ID)
                || !(source instanceof LivingEntity attacker) || !JutsuAim.attackable(actor, attacker)
                || attacker instanceof Player && !guard.level.getServer().isPvpAllowed()) {
            return null;
        }
        boolean natural = direct instanceof Projectile shot && shot.getPersistentData().contains(NATURE)
                ? shot.getPersistentData().getBoolean(NATURE) : sage(source);
        return natural ? null : guard;
    }

    private static boolean sage(@Nullable Entity owner) {
        return owner instanceof Player player
                && player.getCapability(NinjaCapabilityHandler.NINJA_DATA).map(INinjaData::isSageModeActive).orElse(false);
    }

    private static boolean available(Player actor, INinjaData data) {
        return actor.isAlive() && !actor.isSpectator() && !actor.noPhysics && data.isNinjaModeEnabled();
    }

    private static void credit(Player actor, Guard guard, PretaReservoir.Receipt receipt, Vec3 impact, int tint) {
        if (receipt.chakra() > 0) {
            guard.data.addChakra(receipt.chakra());
        }
        Vec3 center = JutsuAim.center(actor);
        Vec3 outward = ProjectileAxis.direction(impact.subtract(center), actor.getLookAngle());
        Vec3 contact = center.add(outward.scale(Math.max(0.52, actor.getBbWidth() * 0.95)));
        send(actor, guard, Stage.SIP, contact, tint);
        if (guard.level.getGameTime() - guard.lastSip >= 4) {
            guard.lastSip = guard.level.getGameTime();
            tone(guard, contact, NarutoSounds.PRETA_SIP.get(), 0.78f);
        }
    }

    private static int tint(String type) {
        if (type.contains("water") || type.contains("ice") || type.contains("shuriken")) {
            return 0x83D5FE;
        }
        if (type.contains("fire")) {
            return 0xFFC07F;
        }
        return type.contains("chidori") || type.contains("shock") ? 0xBCFAFF : DEFAULT_TINT;
    }

    private static ResourceLocation key() { return new ResourceLocation(NarutoMod.MOD_ID, "preta_absorbable"); }

    private static void send(Player player, Guard guard, Stage stage, Vec3 contact, int tint) {
        Vec3 at = JutsuAim.center(player);
        PacketHandler.sendNear(new PretaGuardPacket(guard.level.dimension().location(), player.getId(), stage, at, contact, tint),
                guard.level, at, 35);
    }

    private static void tone(Guard guard, Vec3 at, SoundEvent tone, float volume) {
        guard.level.playSound(null, at.x, at.y, at.z, tone, SoundSource.PLAYERS, volume, 1f);
    }

    @SubscribeEvent
    public static void leave(EntityLeaveLevelEvent event) {
        if (!event.getLevel().isClientSide && event.getEntity() instanceof Player player) {
            end(player);
        }
    }

    private static final class Guard {
        private final ServerLevel level;
        private final long start;
        private final INinjaData data;
        private final PretaReservoir pool = new PretaReservoir();
        private boolean ready;
        private long lastSip = -100;

        private Guard(ServerLevel level, long start, INinjaData data) {
            this.level = level;
            this.start = start;
            this.data = data;
        }
    }
}
