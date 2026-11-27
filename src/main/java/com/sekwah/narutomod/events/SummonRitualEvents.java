package com.sekwah.narutomod.events;

import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.entity.SummonBeastEntity;
import com.sekwah.narutomod.network.PacketHandler;
import com.sekwah.narutomod.network.s2c.SummonSealPacket;
import com.sekwah.narutomod.sounds.NarutoSounds;
import com.sekwah.narutomod.util.SummoningCircle;
import com.sekwah.narutomod.util.SummoningCircle.Stage;
import com.sekwah.narutomod.util.SummoningCircle.Style;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

@Mod.EventBusSubscriber(modid = NarutoMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SummonRitualEvents {
    private static final Map<ServerPlayer, Ritual> ACTIVE = new WeakHashMap<>();

    private SummonRitualEvents() {
    }

    public static boolean start(ServerPlayer owner, INinjaData data, BiConsumer<ServerPlayer, INinjaData> success, Breed breed) {
        if (!available(owner, data) || ACTIVE.containsKey(owner) || data.getChakra() < breed.cost()) {
            return false;
        }
        ServerLevel level = owner.serverLevel();
        Vec3 site = SummoningCircle.find(level, owner, breed.width(), breed.height());
        if (site == null) {
            return false;
        }
        data.useChakra(breed.cost(), 60);
        data.setCastPoseTicks(breed.windup());
        var ritual = new Ritual(level, level.getGameTime(), site, new Stamp(owner.getYRot(), owner.position()),
                breed, new Payment(data, success));
        ACTIVE.put(owner, ritual);
        publish(owner, level, site, breed.style(), Stage.PREPARE, breed.width());
        level.playSound(null, site.x, site.y, site.z, NarutoSounds.SUMMON_MARK.get(), SoundSource.PLAYERS, 0.75f, 1);
        return true;
    }

    @SubscribeEvent
    public static void tick(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer owner)) {
            return;
        }
        var ritual = ACTIVE.get(owner);
        if (ritual == null) {
            return;
        }
        if (!available(owner, ritual.pay().data()) || owner.level() != ritual.level()
                || owner.position().distanceToSqr(ritual.stamp().origin()) > 3.3 * 3.3) {
            cancel(owner, ritual);
        } else if (ritual.level().getGameTime() - ritual.start() >= ritual.breed().windup()) {
            if (complete(owner, ritual)) {
                ACTIVE.remove(owner);
                ritual.pay().success().accept(owner, ritual.pay().data());
            } else {
                cancel(owner, ritual);
            }
        }
    }

    private static boolean complete(ServerPlayer owner, Ritual ritual) {
        var breed = ritual.breed();
        var level = ritual.level();
        if (!SummoningCircle.legal(level, owner, ritual.site(), breed.width(), breed.height())) {
            return false;
        }
        PathfinderMob mob = breed.factory().apply(owner, level);
        if (mob == null) {
            return false;
        }
        Vec3 site = ritual.site();
        mob.setPos(site);
        mob.setYRot(ritual.stamp().yaw());
        mob.setYBodyRot(ritual.stamp().yaw());
        mob.setYHeadRot(ritual.stamp().yaw());
        if (!level.addFreshEntity(mob)) {
            return false;
        }
        if (mob instanceof SummonBeastEntity contract && !ContractSummonEvents.admit(owner, contract, level)) {
            mob.discard();
            return false;
        }
        publish(owner, level, site, breed.style(), Stage.EMERGE, breed.width());
        level.sendParticles(ParticleTypes.POOF, site.x, site.y + 0.20, site.z, 27, breed.width() * 0.25, 0.20,
                breed.width() * 0.25, 0.06);
        level.playSound(null, site.x, site.y, site.z, arrival(breed.style()), SoundSource.PLAYERS, 1, 0.98f);
        return true;
    }

    private static SoundEvent arrival(Style style) {
        return switch (style) {
            case RHINO -> NarutoSounds.SUMMON_ARRIVE.get();
            case TOAD -> NarutoSounds.SUMMON_TOAD.get();
            case SNAKE -> NarutoSounds.SUMMON_SNAKE.get();
            case SLUG -> NarutoSounds.SUMMON_SLUG.get();
            case MONKEY -> NarutoSounds.SUMMON_MONKEY.get();
        };
    }

    private static boolean available(ServerPlayer owner, INinjaData data) {
        return owner.isAlive() && !owner.isSpectator() && !owner.noPhysics && data.isNinjaModeEnabled();
    }

    public static void publish(ServerPlayer owner, ServerLevel level, Vec3 center, Style style, Stage stage, float span) {
        PacketHandler.sendNear(new SummonSealPacket(level.dimension().location(), owner.getId(), style, stage, center, span),
                level, center, 44);
    }

    private static void cancel(ServerPlayer owner, Ritual ritual) {
        if (ACTIVE.remove(owner, ritual)) {
            ritual.pay().data().addChakra(ritual.breed().cost());
            publish(owner, ritual.level(), ritual.site(), ritual.breed().style(), Stage.CANCEL, ritual.breed().width());
        }
    }

    @SubscribeEvent
    public static void leave(EntityLeaveLevelEvent event) {
        if (event.getEntity() instanceof ServerPlayer owner) {
            Ritual ritual = ACTIVE.get(owner);
            if (ritual != null) {
                cancel(owner, ritual);
            }
        }
    }

    public record Breed(float cost, float width, float height, Style style, int windup,
                        BiFunction<ServerPlayer, ServerLevel, PathfinderMob> factory) {
    }

    private record Ritual(ServerLevel level, long start, Vec3 site, Stamp stamp, Breed breed, Payment pay) {
    }

    private record Stamp(float yaw, Vec3 origin) {
    }

    private record Payment(INinjaData data, BiConsumer<ServerPlayer, INinjaData> success) {
    }
}
