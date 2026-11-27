package com.sekwah.narutomod.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.client.model.entity.ChibakuCoreModel;
import com.sekwah.narutomod.client.renderer.NarutoRenderTypes;
import com.sekwah.narutomod.entity.jutsuprojectile.ChibakuTenseiEntity;
import com.sekwah.narutomod.util.ChibakuFormation;
import com.sekwah.narutomod.util.ChibakuFormation.Phase;
import net.minecraft.client.GraphicsStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public class ChibakuTenseiRenderer extends EntityRenderer<ChibakuTenseiEntity> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(NarutoMod.MOD_ID, "textures/entity/jutsu/chibaku_core.png");
    private static final float MODEL_DIAMETER = 5f / 16f;
    private static final int MAUVE = 0xAA83DB;
    private static final int SILVER = 0xE3CFFA;
    private static final int STONE = 0x78658D;
    private final ChibakuCoreModel model;

    public ChibakuTenseiRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new ChibakuCoreModel(context.bakeLayer(ChibakuCoreModel.LAYER_LOCATION));
    }

    @Override
    public void render(ChibakuTenseiEntity entity, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        Phase phase = entity.phase();
        if (phase == Phase.LEGACY) {
            return;
        }
        var game = Minecraft.getInstance();
        boolean low = game.options.graphicsMode().get() == GraphicsStatus.FAST
                || game.getCameraEntity() != null && game.getCameraEntity().distanceToSqr(entity) > 32 * 32;
        double age = entity.age() + partial;
        double radius = entity.size(partial);
        if (phase == Phase.RISE || phase == Phase.GATHER || phase == Phase.FALL) {
            core(pose, buffers, (float) (radius * 0.62), (entity.tickCount + partial) * 1.62f);
        }
        var solid = new Mesh(pose.last().pose(), buffers.getBuffer(NarutoRenderTypes.BOULDER_BODY));
        debris(solid, new View(entity, partial, radius, age, low));
        var haze = new Mesh(pose.last().pose(), buffers.getBuffer(NarutoRenderTypes.WATER_SURFACE));
        atmosphere(haze, new View(entity, partial, radius, age, low));
        super.render(entity, yaw, partial, pose, buffers, light);
    }

    private void core(PoseStack pose, MultiBufferSource buffers, float size, float angle) {
        float scale = size * 2 / MODEL_DIAMETER;
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(angle));
        pose.mulPose(Axis.XP.rotationDegrees(angle * 0.44f));
        pose.scale(-scale, -scale, scale);
        var output = buffers.getBuffer(RenderType.entityTranslucent(TEXTURE));
        this.model.renderToBuffer(pose, output, 15728880, OverlayTexture.NO_OVERLAY, 0.29f, 0.17f, 0.44f, 1);
        pose.popPose();
    }

    private static void debris(Mesh mesh, View view) {
        var core = view.entity();
        if (core.phase() == Phase.RISE) {
            return;
        }
        var sources = core.sources();
        if (sources.isEmpty()) {
            return;
        }
        int count = view.low() ? Math.min(11, sources.size()) : sources.size();
        double spin = (core.tickCount + view.partial()) * 0.023;
        Vec3 world = core.getPosition(view.partial());
        for (int i = 0; i < count; i++) {
            var source = sources.get(i);
            Vec3 bearing = ChibakuFormation.radial(i, count, spin);
            Vec3 orbit = bearing.scale(view.radius() * (0.83 + 0.10 * (i % 3)));
            double size = (0.24 + (i % 4) * 0.079) * (view.low() ? 1.15 : 1);
            Vec3 at = fragment(view, source, orbit, world, i);
            double fade = ending(core.phase()) ? 1 - view.age() / ChibakuFormation.SETTLE : 1;
            mesh.stone(at, size * Mth.clamp(fade, 0.02, 1), iron(source.tint(), i));
        }
    }

    private static Vec3 fragment(View view, ChibakuFormation.Source source, Vec3 orbit, Vec3 world, int i) {
        Phase phase = view.entity().phase();
        if (phase == Phase.GATHER) {
            Vec3 base = Vec3.atCenterOf(source.origin()).add(0, 0.53, 0).subtract(world);
            return ChibakuFormation.drift(base, orbit, ChibakuFormation.flight(view.age(), i), i);
        }
        if (ending(phase)) {
            double f = view.age() / ChibakuFormation.SETTLE;
            return orbit.scale(1 + (phase == Phase.IMPACT ? 1.8 : 0.61) * f).add(0, -f * f * 3.2, 0);
        }
        return orbit;
    }

    private static boolean ending(Phase phase) { return phase == Phase.IMPACT || phase == Phase.DISSOLVE; }

    private static int iron(int tint, int index) {
        if (tint == 0) {
            tint = 0x756C7E;
        }
        double tone = 0.68 + index % 5 * 0.075;
        int r = Math.min(255, (int) (((tint >>> 16) & 255) * tone + 30));
        int g = Math.min(255, (int) (((tint >>> 8) & 255) * tone + 21));
        int b = Math.min(255, (int) ((tint & 255) * tone + 51));
        return r << 16 | g << 8 | b;
    }

    private static void atmosphere(Mesh mesh, View view) {
        Phase phase = view.entity().phase();
        if (phase == Phase.IMPACT) {
            double t = Mth.clamp(view.age() / ChibakuFormation.SETTLE, 0, 1);
            double out = view.entity().profile().shockRange() * (0.09 + 0.91 * t);
            double height = -view.radius() + 0.13;
            mesh.ring(out, height, 0.29 + t * 0.32, view.low() ? 26 : 56, SILVER, (int) (169 * (1 - t)));
            if (!view.low()) {
                mesh.ring(out * 0.84, height + 0.13, 0.21, 40, STONE, (int) (91 * (1 - t)));
            }
        } else if (phase != Phase.DISSOLVE) {
            double amount = phase == Phase.RISE ? 0.32 + Math.min(0.64, view.age() * 0.034) : 1;
            int count = view.low() ? 18 : 38;
            double radius = 0.28 + view.radius() * 1.06;
            mesh.ring(radius, 0.02, 0.055 + view.radius() * 0.041, count, MAUVE, (int) (168 * amount));
            if (!view.low()) {
                mesh.ring(radius * 1.19, view.radius() * 0.35, 0.046, count, SILVER, (int) (91 * amount));
                mesh.ring(radius * 0.88, -view.radius() * 0.51, 0.057, count, STONE, (int) (154 * amount));
            }
            if (phase == Phase.GATHER && view.entity().profile() == ChibakuFormation.PLAYER) {
                double reach = view.entity().profile().pullRange();
                mesh.ring(reach, 0.0, 0.19, count, MAUVE, 46);
            }
        }
    }

    @Override
    public ResourceLocation getTextureLocation(ChibakuTenseiEntity entity) { return TEXTURE; }

    private record View(ChibakuTenseiEntity entity, float partial, double radius, double age, boolean low) {
    }

    private record Mesh(Matrix4f matrix, VertexConsumer buffer) {
        private void stone(Vec3 center, double half, int tint) {
            Vec3 top = center.add(half * 0.19, half, -half * 0.22);
            Vec3 foot = center.subtract(0, half * 0.75, 0);
            Vec3[] belt = {center.add(half, 0.13 * half, 0), center.add(0, -0.13 * half, half),
                    center.add(-half, 0, 0), center.add(0, 0.12 * half, -half)};
            for (int i = 0; i < belt.length; i++) {
                Vec3 a = belt[i];
                Vec3 b = belt[(i + 1) % belt.length];
                quad(top, a, b, top, i % 2 == 0 ? tint : shade(tint, 0.72), 255);
                quad(foot, b, a, foot, shade(tint, 0.62 + 0.06 * i), 255);
            }
        }

        private void ring(double radius, double height, double width, int cuts, int tint, int opacity) {
            double step = Math.PI * 2 / cuts;
            for (int i = 0; i < cuts; i++) {
                Vec3 a = new Vec3(Math.cos(i * step), 0, Math.sin(i * step));
                Vec3 b = new Vec3(Math.cos((i + 1) * step), 0, Math.sin((i + 1) * step));
                quad(a.scale(radius).add(0, height, 0), b.scale(radius).add(0, height, 0),
                        b.scale(Math.max(0, radius - width)).add(0, height + 0.058, 0),
                        a.scale(Math.max(0, radius - width)).add(0, height + 0.058, 0), tint, opacity);
            }
        }

        private static int shade(int tint, double scale) {
            return (int) ((tint >>> 16 & 255) * scale) << 16 | (int) ((tint >>> 8 & 255) * scale) << 8 | (int) ((tint & 255) * scale);
        }

        private void quad(Vec3 a, Vec3 b, Vec3 c, Vec3 d, int tint, int alpha) {
            vertex(a, tint, alpha);
            vertex(b, tint, alpha);
            vertex(c, tint, alpha);
            vertex(d, tint, alpha);
        }

        private void vertex(Vec3 at, int tint, int alpha) {
            this.buffer.vertex(this.matrix, (float) at.x, (float) at.y, (float) at.z)
                    .color((tint >>> 16) & 255, (tint >>> 8) & 255, tint & 255, alpha).endVertex();
        }
    }
}
