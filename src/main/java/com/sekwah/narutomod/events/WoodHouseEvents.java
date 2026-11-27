package com.sekwah.narutomod.events;

import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.network.PacketHandler;
import com.sekwah.narutomod.network.s2c.WoodHousePacket;
import com.sekwah.narutomod.network.s2c.WoodHousePacket.Stage;
import com.sekwah.narutomod.sounds.NarutoSounds;
import com.sekwah.narutomod.util.WoodHouseLayout;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.BiConsumer;

@Mod.EventBusSubscriber(modid = NarutoMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class WoodHouseEvents {
    public static final float COST = 250f;
    private static final Map<Player, Build> ACTIVE = new WeakHashMap<>();

    private WoodHouseEvents() {
    }

    public static void start(ServerPlayer owner, INinjaData data, BiConsumer<ServerPlayer, INinjaData> success) {
        if (!owner.onGround() || owner.isSpectator() || !owner.isAlive() || owner.noPhysics
                || ACTIVE.containsKey(owner) || data.getChakra() < COST) {
            return;
        }
        ServerLevel level = owner.serverLevel();
        var plan = WoodHouseBlueprint.at(level, owner);
        if (plan == null || ACTIVE.values().stream().anyMatch(build -> build.level() == level
                && build.plan().site().box().intersects(plan.site().box())) || !WoodHousePlacement.clear(level, owner, plan)) {
            owner.displayClientMessage(Component.translatable("jutsu.fail.mokuton.house_site"), true);
            return;
        }
        data.useChakra(COST, 60);
        data.setCastPoseTicks(WoodHouseLayout.WINDUP);
        var build = new Build(level, level.getGameTime(), owner.position(), plan, new Receipt(data, success));
        ACTIVE.put(owner, build);
        send(build, Stage.SURVEY);
        level.playSound(null, plan.site().corner(), NarutoSounds.MOKUTON_WARN.get(), SoundSource.BLOCKS, 0.95f, 0.69f);
    }

    @SubscribeEvent
    public static void tick(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer owner)) {
            return;
        }
        Build build = ACTIVE.get(owner);
        if (build == null) {
            return;
        }
        if (!owner.isAlive() || owner.isSpectator() || owner.noPhysics || owner.level() != build.level()
                || owner.position().distanceToSqr(build.origin()) > 4 * 4 || !build.receipt().data().isNinjaModeEnabled()) {
            cancel(owner, build);
            return;
        }
        long elapsed = build.level().getGameTime() - build.start();
        if (elapsed == 10) {
            build.level().playSound(null, build.plan().site().corner(), NarutoSounds.MOKUTON_GROW.get(),
                    SoundSource.BLOCKS, 0.92f, 0.62f);
        } else if (elapsed >= WoodHouseLayout.WINDUP) {
            if (WoodHousePlacement.place(build.level(), owner, build.plan())) {
                ACTIVE.remove(owner);
                complete(build);
                build.receipt().success().accept(owner, build.receipt().data());
            } else {
                cancel(owner, build);
            }
        }
    }

    private static void complete(Build build) {
        send(build, Stage.REVEAL);
        var site = build.plan().site();
        build.level().playSound(null, site.corner().offset(site.size().getX() / 2, 0, site.size().getZ() / 2),
                NarutoSounds.MOKUTON_BURIAL.get(), SoundSource.BLOCKS, 1.32f, 0.61f);
        for (int x : new int[]{0, site.size().getX() - 1}) {
            for (int z : new int[]{0, site.size().getZ() - 1}) {
                Vec3 at = Vec3.atBottomCenterOf(site.corner().offset(x, 0, z));
                build.level().sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SPRUCE_PLANKS.defaultBlockState()),
                        at.x, at.y + 0.2, at.z, 7, 0.42, 0.16, 0.42, 0.04);
            }
        }
    }

    private static void cancel(Player owner, Build build) {
        if (ACTIVE.remove(owner, build)) {
            build.receipt().data().addChakra(COST);
            send(build, Stage.CANCEL);
            if (owner.isAlive() && build.level() == owner.level()) {
                owner.displayClientMessage(Component.translatable("jutsu.fail.mokuton.house_changed"), true);
            }
        }
    }

    private static void send(Build build, Stage stage) {
        var site = build.plan().site();
        PacketHandler.sendNear(new WoodHousePacket(build.level().dimension().location(), site.corner(), site.size(), stage),
                build.level(), site.box().getCenter(), 55);
    }

    @SubscribeEvent
    public static void leave(EntityLeaveLevelEvent event) {
        if (!event.getLevel().isClientSide && event.getEntity() instanceof Player owner) {
            var build = ACTIVE.get(owner);
            if (build != null) {
                cancel(owner, build);
            }
        }
    }

    private record Build(ServerLevel level, long start, Vec3 origin, WoodHouseBlueprint plan, Receipt receipt) {
    }

    private record Receipt(INinjaData data, BiConsumer<ServerPlayer, INinjaData> success) {
    }
}
