package com.sekwah.narutomod.events;

import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.entity.MangekyoBossVariant;
import com.sekwah.narutomod.network.PacketHandler;
import com.sekwah.narutomod.network.s2c.FinalChakraPacket;
import com.sekwah.narutomod.network.s2c.FinalChakraPacket.Phase;
import com.sekwah.narutomod.network.s2c.FinalChakraPacket.Shape;
import com.sekwah.narutomod.sounds.NarutoSounds;
import com.sekwah.narutomod.util.SusanooForm;
import com.sekwah.narutomod.util.SusanooImpactGeometry;
import com.sekwah.narutomod.util.SusanooPose;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.WeakHashMap;

public final class FinalChakraEffects {
    private static final Map<LivingEntity, ImpactMark> LAST_BLOCK = new WeakHashMap<>();
    private FinalChakraEffects() {
    }

    public static int tint(INinjaData data) {
        if (data.getSusanooColor() >= 0) {
            return data.getSusanooColor();
        }
        var form = MangekyoBossVariant.byFormId(data.getMangekyoForm());
        return form == null ? 0x8C40D9 : rgb(form.susanooRed(), form.susanooGreen(), form.susanooBlue());
    }

    public static int rgb(float red, float green, float blue) {
        return (Math.round(red * 255) << 16) | (Math.round(green * 255) << 8) | Math.round(blue * 255);
    }

    public static void shellImpact(LivingEntity actor, DamageSource source, int stage, int tint, float integrity, boolean shattering) {
        if (!(actor.level() instanceof ServerLevel level)) return;
        var form = SusanooForm.fromId(stage);
        if (form == SusanooForm.NONE) return;
        float yaw = YasakaVolleyEvents.yawOr(actor, SusanooCombat.yawOr(actor, actor.yBodyRot));
        var frame = SusanooPose.frame(actor.position(), yaw);
        var contact = contact(frame, form, source);
        long now = level.getGameTime();
        if (shattering) {
            LAST_BLOCK.remove(actor);
        } else {
            var prior = LAST_BLOCK.get(actor);
            if (prior != null && now - prior.tick() <= 2 && prior.point().distanceToSqr(contact.point()) < .64) return;
            LAST_BLOCK.put(actor, new ImpactMark(now, contact.point()));
        }
        float size = form == SusanooForm.PERFECT ? (shattering ? 3.2f : 1.7f) : (shattering ? 1.9f : .85f);
        var shape = new Shape(contact.point(), contact.normal(), size, tint);
        emit(actor, shattering ? Phase.SHELL_BREAK : Phase.SHELL_CRACK, shape, now, yaw);
        sound(actor, shape, shattering ? NarutoSounds.SUSANOO_BREAK.get() : NarutoSounds.SUSANOO_BLOCK.get(),
                shattering ? 1.5f : .75f, .62f + Mth.clamp(integrity, 0, 1) * .46f);
    }

    private static SusanooImpactGeometry.Contact contact(SusanooPose.Frame frame, SusanooForm form, DamageSource source) {
        var direct = source.getDirectEntity();
        Vec3 from = direct instanceof LivingEntity attacker ? attacker.getEyePosition() : source.getSourcePosition();
        Vec3 motion = direct instanceof Projectile projectile ? projectile.getDeltaMovement() : Vec3.ZERO;
        return SusanooImpactGeometry.sample(frame, form, from, motion);
    }

    public static void emit(Entity subject, Phase phase, Shape shape) {
        emit(subject, phase, shape, -1, 0);
    }

    public static void emit(Entity subject, Phase phase, Shape shape, long started, float yaw) {
        if (subject.level() instanceof ServerLevel level) {
            broadcast(level, new FinalChakraPacket(level.dimension().location(), subject.getId(), phase, shape, started, yaw));
        }
    }

    static void broadcast(ServerLevel level, FinalChakraPacket packet) {
        PacketHandler.sendNear(packet, level, packet.shape().point(), 66 + Math.max(0, packet.shape().size()));
    }

    public static void sound(Entity subject, Shape shape, SoundEvent sound, float volume, float pitch) {
        if (subject.level() instanceof ServerLevel level) {
            var at = shape.point();
            level.playSound(null, at.x, at.y, at.z, sound, SoundSource.PLAYERS, volume, pitch);
        }
    }

    private record ImpactMark(long tick, Vec3 point) {
    }
}
