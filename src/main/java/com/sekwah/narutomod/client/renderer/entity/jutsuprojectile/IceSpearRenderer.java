package com.sekwah.narutomod.client.renderer.entity.jutsuprojectile;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.sekwah.narutomod.client.renderer.NarutoRenderTypes;
import com.sekwah.narutomod.entity.jutsuprojectile.IceSpearEntity;
import com.sekwah.narutomod.entity.jutsuprojectile.IceSpearEntity.Phase;
import com.sekwah.narutomod.util.IceSpearVolley;
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

public final class IceSpearRenderer extends EntityRenderer<IceSpearEntity> {
    private static final int[] FACETS = {0x2676AC, 0x73CDE9, 0xC9FBFF, 0x4D9FCC, 0x467BA4, 0x68B9DC};
    private static final int WHITE = 0xE9FDFF;
    private static final int ICE = 0x77C7F9;

    public IceSpearRenderer(EntityRendererProvider.Context context) { super(context); }

    @Override
    public void render(IceSpearEntity spear, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        super.render(spear, yaw, partial, pose, buffers, light);
        boolean low = Minecraft.getInstance().options.graphicsMode().get() == GraphicsStatus.FAST;
        var mesh = new Mesh(pose.last().pose(), buffers.getBuffer(NarutoRenderTypes.WATER_SURFACE));
        var phase = spear.phase();
        double age = spear.age() + partial;
        Vec3 heading = spear.axis();
        var basis = new Basis(heading, IceSpearVolley.side(heading));
        switch (phase) {
            case GATHER -> crystal(mesh, basis, Mth.clamp((age + 0.8) / 5.8, 0.18, 1), low);
            case FLY -> {
                trail(mesh, basis, age, low);
                crystal(mesh, basis, 1, low);
            }
            case CHIP, FADE -> shards(mesh, basis, Vec3.atLowerCornerOf(spear.face().getNormal()), age,
                    phase == Phase.CHIP && !low);
        }
    }

    private static void crystal(Mesh mesh, Basis axis, double growth, boolean low) {
        int sides = low ? 4 : 6;
        double width = IceSpearVolley.RADIUS * growth;
        Vec3 front = axis.forward().scale(width);
        Vec3 collar = axis.forward().scale(-0.19 * growth);
        Vec3 end = axis.forward().scale(-0.69 * growth);
        for (int i = 0; i < sides; i++) {
            Vec3 a = axis.radial(i * Math.PI * 2 / sides).scale(width);
            Vec3 b = axis.radial((i + 1) * Math.PI * 2 / sides).scale(width);
            mesh.quad(front, front, collar.add(a), collar.add(b), FACETS[i], 233);
            mesh.quad(collar.add(a), end.add(a.scale(0.21)), end.add(b.scale(0.21)), collar.add(b), FACETS[i], 192);
        }
        if (!low) {
            Vec3 rim = collar.add(axis.side().scale(width * 0.85));
            mesh.quad(rim, rim.add(axis.side().scale(width * 0.13)), front, end, WHITE, 203);
        }
    }

    private static void trail(Mesh mesh, Basis axis, double age, boolean low) {
        Vec3 end = axis.forward().scale(-Math.min(1.34, 0.49 + age * 0.38));
        Vec3 near = axis.forward().scale(-0.23);
        mesh.ribbon(near, end, axis.side().scale(low ? 0.038 : 0.055), ICE, 166);
        if (!low) {
            mesh.ribbon(near, end.scale(0.71), axis.forward().cross(axis.side()).scale(0.013), WHITE, 187);
        }
    }

    private static void shards(Mesh mesh, Basis axis, Vec3 normal, double age, boolean dense) {
        double time = Mth.clamp(age / IceSpearVolley.BREAK, 0, 1);
        int limit = dense ? 8 : 4;
        for (int i = 0; i < limit; i++) {
            Vec3 stir = axis.radial(i * Math.PI * 2 / limit);
            Vec3 drift = stir.scale(time * (0.31 + 0.16 * (i % 3))).add(normal.scale(time * 0.14));
            if (drift.dot(normal) < 0) {
                drift = drift.subtract(normal.scale(drift.dot(normal)));
            }
            Vec3 point = drift.add(0, -0.20 * time * time, 0);
            Vec3 lead = stir.scale(0.06 + (i % 3) * 0.019);
            Vec3 span = axis.forward().scale(0.016 + time * 0.021);
            mesh.quad(point.add(lead), point.add(span), point.subtract(lead), point.subtract(span),
                    i % 2 == 0 ? WHITE : ICE, (int) (209 * (1 - time)));
        }
    }

    @Override
    public ResourceLocation getTextureLocation(IceSpearEntity entity) { return TextureAtlas.LOCATION_BLOCKS; }

    private record Basis(Vec3 forward, Vec3 side) {
        private Vec3 radial(double angle) {
            return this.side.scale(Math.cos(angle)).add(this.forward.cross(this.side).scale(Math.sin(angle)));
        }
    }

    private record Mesh(Matrix4f matrix, VertexConsumer buffer) {
        private void ribbon(Vec3 a, Vec3 b, Vec3 width, int rgb, int alpha) {
            quad(a.add(width), b.add(width.scale(0.16)), b.subtract(width.scale(0.16)), a.subtract(width), rgb, alpha);
        }

        private void quad(Vec3 a, Vec3 b, Vec3 c, Vec3 d, int rgb, int alpha) {
            vertex(a, rgb, alpha);
            vertex(b, rgb, alpha);
            vertex(c, rgb, alpha);
            vertex(d, rgb, alpha);
        }

        private void vertex(Vec3 point, int rgb, int alpha) {
            this.buffer.vertex(this.matrix, (float) point.x, (float) point.y, (float) point.z)
                    .color((rgb >>> 16) & 255, (rgb >>> 8) & 255, rgb & 255, alpha).endVertex();
        }
    }
}
