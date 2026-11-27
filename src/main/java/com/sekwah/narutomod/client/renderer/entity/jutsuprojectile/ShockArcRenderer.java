package com.sekwah.narutomod.client.renderer.entity.jutsuprojectile;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.sekwah.narutomod.client.renderer.NarutoRenderTypes;
import com.sekwah.narutomod.entity.jutsuprojectile.ShockArcEntity;
import com.sekwah.narutomod.util.ShockArc;
import net.minecraft.client.GraphicsStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public class ShockArcRenderer extends EntityRenderer<ShockArcEntity> {
    private static final Vec3 SKY = new Vec3(0, 1, 0);
    private static final Vec3 SIDE = new Vec3(1, 0, 0);
    private static final Tone HALO = new Tone(61, 123, 233);
    private static final Tone ION = new Tone(191, 237, 255);

    public ShockArcRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(ShockArcEntity shock, float yaw, float partial, PoseStack poses, MultiBufferSource buffers, int light) {
        super.render(shock, yaw, partial, poses, buffers, light);
        var game = Minecraft.getInstance();
        boolean low = game.options.graphicsMode().get() == GraphicsStatus.FAST
                || shock.distanceToSqr(game.getCameraEntity() == null ? shock : game.getCameraEntity()) > 27 * 27;
        var spark = new Mesh(poses.last().pose(), buffers.getBuffer(NarutoRenderTypes.CHAKRA_GLOW));
        float age = shock.age() + partial;
        if (shock.age() < ShockArc.PREPARE) {
            this.charge(spark, shock.getLookAngle(), age, low);
        } else if (shock.arc().lengthSqr() > 0.001) {
            float fade = Mth.clamp(1 - (age - ShockArc.PREPARE) / ShockArc.FADE, 0, 1);
            this.arc(spark, new Trace(shock.arc(), shock.getId(), fade, shock.struck(), low));
        }
    }

    private void charge(Mesh mesh, Vec3 axis, float age, boolean low) {
        Vec3 span = side(axis);
        Vec3 up = span.cross(axis);
        int count = low ? 4 : 7;
        double reach = 0.075 + age * 0.028;
        for (int i = 0; i < count; i++) {
            double theta = i * Math.PI * 2 / count + age * 0.83;
            Vec3 end = span.scale(Math.cos(theta) * reach).add(up.scale(Math.sin(theta) * reach));
            mesh.strip(end, end.scale(0.45).add(axis.scale(0.055)), span.scale(0.013), ION, 160);
        }
    }

    private void arc(Mesh mesh, Trace trace) {
        Vec3 axis = trace.end().normalize();
        Vec3 side = side(axis);
        Vec3 up = side.cross(axis);
        int pieces = Mth.clamp((int) (trace.end().length() * (trace.low() ? 1.65 : 2.6)), 4, 14);
        Vec3 prev = Vec3.ZERO;
        for (int i = 1; i <= pieces; i++) {
            double t = (double) i / pieces;
            double bend = 0.12 * Math.sin(Math.PI * t);
            double noise = trace.seed() * 0.32 + i * 2.6;
            Vec3 next = trace.end().scale(t).add(side.scale(bend * Math.sin(noise))).add(up.scale(bend * Math.cos(noise)));
            int alpha = (int) (230 * trace.fade());
            mesh.strip(prev, next, side.scale(0.041), HALO, (int) (0.63 * alpha));
            mesh.strip(prev, next, up.scale(0.018), ION, i % 4 == 0 ? alpha / 3 : alpha);
            prev = next;
        }
        if (trace.hit()) {
            mesh.strip(trace.end().subtract(side.scale(0.11)), trace.end().add(side.scale(0.11)),
                    up.scale(0.025), ION, (int) (200 * trace.fade()));
        }
    }

    private static Vec3 side(Vec3 axis) {
        return axis.cross(Math.abs(axis.y) > 0.94 ? SIDE : SKY).normalize();
    }

    @Override
    public ResourceLocation getTextureLocation(ShockArcEntity entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }

    private record Trace(Vec3 end, int seed, float fade, boolean hit, boolean low) {
    }

    private record Tone(int r, int g, int b) {
    }

    private record Mesh(Matrix4f matrix, VertexConsumer buffer) {
        private void strip(Vec3 from, Vec3 to, Vec3 width, Tone tone, int alpha) {
            vertex(from.add(width), tone, alpha);
            vertex(from.subtract(width), tone, alpha);
            vertex(to.subtract(width), tone, alpha);
            vertex(to.add(width), tone, alpha);
        }

        private void vertex(Vec3 point, Tone tone, int alpha) {
            buffer.vertex(matrix, (float) point.x, (float) point.y, (float) point.z)
                    .color(tone.r(), tone.g(), tone.b(), alpha).endVertex();
        }
    }
}
