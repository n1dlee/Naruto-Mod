package com.sekwah.narutomod.network.s2c;

import com.sekwah.narutomod.client.renderer.HiraishinRenderer;
import com.sekwah.narutomod.util.HiraishinTargeting.Glyph;
import com.sekwah.narutomod.util.HiraishinTargeting.Kind;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record HiraishinPacket(ResourceLocation dimension, int owner, Stage stage, Glyph glyph) {
    public enum Stage { MARK, DEPART, ARRIVE, PREVIEW, CLEAR }

    public static void encode(HiraishinPacket packet, FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(packet.dimension);
        buffer.writeVarInt(packet.owner);
        buffer.writeEnum(packet.stage);
        buffer.writeEnum(packet.glyph.kind());
        buffer.writeDouble(packet.glyph.point().x);
        buffer.writeDouble(packet.glyph.point().y);
        buffer.writeDouble(packet.glyph.point().z);
        buffer.writeEnum(packet.glyph.face());
        buffer.writeVarInt(packet.glyph.entityId());
    }

    public static HiraishinPacket decode(FriendlyByteBuf buffer) {
        var level = buffer.readResourceLocation();
        int owner = buffer.readVarInt();
        var stage = buffer.readEnum(Stage.class);
        var kind = buffer.readEnum(Kind.class);
        Vec3 point = new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
        return new HiraishinPacket(level, owner, stage,
                new Glyph(kind, point, buffer.readEnum(Direction.class), buffer.readVarInt()));
    }

    public static void handle(HiraishinPacket packet, Supplier<NetworkEvent.Context> supplier) {
        var context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> HiraishinRenderer.receive(packet)));
        context.setPacketHandled(true);
    }
}
