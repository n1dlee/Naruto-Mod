package com.sekwah.narutomod.network.s2c;

import com.sekwah.narutomod.client.renderer.ShadowTetherRenderer;
import com.sekwah.narutomod.util.ShadowSurface;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.ArrayList;
import java.util.function.Supplier;

public record ShadowTetherPacket(ResourceLocation dimension, int actor, int id, Cue cue, List<Vec3> path) {
    public enum Cue { CAST, BOUND, SNAP, SEW_TRACE, SEW_HIT, SEW_MISS }

    public ShadowTetherPacket {
        path = List.copyOf(path);
        if (path.size() > ShadowSurface.MAX_POINTS) {
            throw new IllegalArgumentException("Shadow route exceeds point budget");
        }
    }

    public static void encode(ShadowTetherPacket packet, FriendlyByteBuf buf) {
        buf.writeResourceLocation(packet.dimension);
        buf.writeVarInt(packet.actor);
        buf.writeVarInt(packet.id);
        buf.writeEnum(packet.cue);
        buf.writeByte(packet.path.size());
        if (!packet.path.isEmpty()) {
            Vec3 base = packet.path.get(0);
            buf.writeDouble(base.x);
            buf.writeDouble(base.y);
            buf.writeDouble(base.z);
            for (int i = 1; i < packet.path.size(); i++) {
                Vec3 delta = packet.path.get(i).subtract(base);
                buf.writeShort((int) Math.round(delta.x * 64));
                buf.writeShort((int) Math.round(delta.y * 64));
                buf.writeShort((int) Math.round(delta.z * 64));
            }
        }
    }

    public static ShadowTetherPacket decode(FriendlyByteBuf buf) {
        ResourceLocation level = buf.readResourceLocation();
        int actor = buf.readVarInt();
        int id = buf.readVarInt();
        Cue cue = buf.readEnum(Cue.class);
        int count = buf.readUnsignedByte();
        if (count > ShadowSurface.MAX_POINTS) {
            throw new IllegalArgumentException("Shadow route exceeds point budget");
        }
        List<Vec3> points = new ArrayList<>(count);
        if (count > 0) {
            Vec3 base = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
            points.add(base);
            for (int i = 1; i < count; i++) {
                points.add(base.add(buf.readShort() / 64.0, buf.readShort() / 64.0, buf.readShort() / 64.0));
            }
        }
        return new ShadowTetherPacket(level, actor, id, cue, points);
    }

    public static void handle(ShadowTetherPacket packet, Supplier<NetworkEvent.Context> supplier) {
        var context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ShadowTetherRenderer.receive(packet)));
        context.setPacketHandled(true);
    }
}
