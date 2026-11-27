package com.sekwah.narutomod.network.s2c;

import com.sekwah.narutomod.client.renderer.AdamantineChainRenderer;
import com.sekwah.narutomod.util.AdamantineChain.Phase;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record AdamantineChainPacket(ResourceLocation dimension, int id, Phase phase, int age, Vec3 root, Vec3 tip,
                                    float girth, float fraction) {
    public static void encode(AdamantineChainPacket chain, FriendlyByteBuf buf) {
        buf.writeResourceLocation(chain.dimension);
        buf.writeVarInt(chain.id);
        buf.writeEnum(chain.phase);
        buf.writeByte(chain.age);
        writePoint(buf, chain.root);
        writePoint(buf, chain.tip);
        buf.writeFloat(chain.girth);
        buf.writeFloat(chain.fraction);
    }

    public static AdamantineChainPacket decode(FriendlyByteBuf buf) {
        return new AdamantineChainPacket(buf.readResourceLocation(), buf.readVarInt(), buf.readEnum(Phase.class),
                buf.readUnsignedByte(), readPoint(buf), readPoint(buf), buf.readFloat(), buf.readFloat());
    }

    public static void handle(AdamantineChainPacket chain, Supplier<NetworkEvent.Context> supplier) {
        var context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> AdamantineChainRenderer.receive(chain)));
        context.setPacketHandled(true);
    }

    private static void writePoint(FriendlyByteBuf buf, Vec3 p) {
        buf.writeDouble(p.x);
        buf.writeDouble(p.y);
        buf.writeDouble(p.z);
    }

    private static Vec3 readPoint(FriendlyByteBuf buf) {
        return new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
    }
}
