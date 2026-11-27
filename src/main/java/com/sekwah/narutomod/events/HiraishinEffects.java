package com.sekwah.narutomod.events;

import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.abilities.jutsus.HiraishinTeleportAbility;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.capabilities.NinjaCapabilityHandler;
import com.sekwah.narutomod.network.PacketHandler;
import com.sekwah.narutomod.network.s2c.HiraishinPacket;
import com.sekwah.narutomod.network.s2c.HiraishinPacket.Stage;
import com.sekwah.narutomod.sounds.NarutoSounds;
import com.sekwah.narutomod.util.HiraishinTargeting;
import com.sekwah.narutomod.util.HiraishinTargeting.Glyph;
import com.sekwah.narutomod.util.HiraishinTargeting.Kind;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = NarutoMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class HiraishinEffects {
    private HiraishinEffects() {
    }

    @SubscribeEvent
    public static void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer actor)
                || !actor.isAlive() || actor.isSpectator() || (actor.tickCount + actor.getId()) % 10 != 0) {
            return;
        }
        actor.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(data -> preview(actor, data));
    }

    private static void preview(ServerPlayer actor, INinjaData data) {
        if (data.isNinjaModeEnabled() && data.isJutsuLearned(HiraishinTeleportAbility.SCROLL)) {
            var landing = HiraishinTargeting.choose(actor, data.getHiraishinFormulas());
            if (landing != null) {
                PacketHandler.sendToPlayer(packet(actor, Stage.PREVIEW, landing.glyph()), actor);
            }
        }
    }

    public static boolean canAfford(Player actor, INinjaData data, float cost, String key) {
        if (data.getChakra() >= cost) {
            return true;
        }
        message(actor, "jutsu.fail.notenoughchakra", Component.translatable(key));
        return false;
    }

    public static void message(Player actor, String key, Object... args) {
        actor.displayClientMessage(Component.translatable(key, args).withStyle(ChatFormatting.GOLD), true);
    }

    public static void mark(ServerPlayer actor, Glyph glyph) {
        emit(actor, Stage.MARK, glyph);
        play(actor, glyph.point(), NarutoSounds.HIRAISHIN_MARK.get(), 0.72f);
    }

    public static void clear(ServerPlayer actor) {
        PacketHandler.sendToPlayer(packet(actor, Stage.CLEAR, vertical(actor, Kind.GROUND, actor.position())), actor);
    }

    public static void jump(ServerPlayer actor, Vec3 from, HiraishinTargeting.Landing landing) {
        Kind kind = landing.glyph().kind();
        Vec3 feet = landing.feet();
        emit(actor, Stage.DEPART, vertical(actor, kind, from));
        emit(actor, Stage.ARRIVE, vertical(actor, kind, feet));
        play(actor, from, NarutoSounds.HIRAISHIN_DEPART.get(), 0.66f);
        play(actor, feet, NarutoSounds.HIRAISHIN_ARRIVE.get(), 0.72f);
    }

    private static Glyph vertical(ServerPlayer actor, Kind kind, Vec3 point) {
        Direction face = actor.getDirection().getOpposite();
        return new Glyph(kind, point, face, -1);
    }

    private static void emit(ServerPlayer actor, Stage stage, Glyph glyph) {
        PacketHandler.sendNear(packet(actor, stage, glyph), actor.serverLevel(), glyph.point(), 48);
    }

    private static HiraishinPacket packet(ServerPlayer actor, Stage stage, Glyph glyph) {
        return new HiraishinPacket(actor.level().dimension().location(), actor.getId(), stage, glyph);
    }

    private static void play(ServerPlayer actor, Vec3 at, SoundEvent event, float volume) {
        actor.serverLevel().playSound(null, at.x, at.y, at.z, event, SoundSource.PLAYERS, volume, 1.0f);
    }
}
