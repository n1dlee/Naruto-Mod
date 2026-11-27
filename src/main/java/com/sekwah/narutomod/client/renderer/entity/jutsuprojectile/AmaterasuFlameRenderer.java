package com.sekwah.narutomod.client.renderer.entity.jutsuprojectile;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.sekwah.narutomod.client.renderer.NarutoRenderTypes;
import com.sekwah.narutomod.entity.jutsuprojectile.AmaterasuFireEntity;
import com.sekwah.narutomod.util.AmaterasuFlames;
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

public class AmaterasuFlameRenderer extends EntityRenderer<AmaterasuFireEntity> {
    private static final Vec3 UP = new Vec3(0, 1, 0);
    private static final Vec3 RIGHT = new Vec3(1, 0, 0);
    private static final Tone CORE = new Tone(12, 7, 15);
    private static final Tone FOLD = new Tone(32, 13, 28);
    private static final Tone GARNET = new Tone(177, 33, 63);
    private static final Tone EMBER = new Tone(123, 39, 72);

    public AmaterasuFlameRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(AmaterasuFireEntity flame, float yaw, float partial, PoseStack poses, MultiBufferSource buffers, int light) {
        super.render(flame, yaw, partial, poses, buffers, light);
        var game = Minecraft.getInstance();
        boolean low = game.options.graphicsMode().get() == GraphicsStatus.FAST
                || flame.distanceToSqr(game.getCameraEntity() == null ? flame : game.getCameraEntity()) > 36 * 36;
        var matrix = poses.last().pose();
        float age = flame.age() + partial;
        if (flame.phase() == AmaterasuFireEntity.Phase.FOCUS) {
            focus(new Mesh(matrix, buffers.getBuffer(NarutoRenderTypes.CHAKRA_GLOW)), flame.getLookAngle(), age);
            return;
        }
        Vec3 normal = flame.body() ? UP : Vec3.atLowerCornerOf(flame.face().getNormal());
        Vec3 plume = normal.scale(0.84).add(UP.scale(0.61)).normalize();
        var frame = Frame.around(plume);
        float intensity = flame.phase() == AmaterasuFireEntity.Phase.FADE
                ? 1 - Mth.clamp(age / AmaterasuFlames.FADE, 0, 1)
                : 0.35f + 0.65f * Mth.clamp(age / 7, 0, 1);
        var look = new FlameLook(flame.getId(), age, intensity, flame.body(), low);
        var core = new Mesh(matrix, buffers.getBuffer(NarutoRenderTypes.WATER_SURFACE));
        drawDark(core, frame, look);
        var rim = new Mesh(matrix, buffers.getBuffer(NarutoRenderTypes.CHAKRA_GLOW));
        drawEmbers(rim, frame, look);
    }

    private static void focus(Mesh mesh, Vec3 heading, float age) {
        Frame frame = Frame.around(heading);
        float t = Mth.clamp(age / AmaterasuFlames.FOCUS, 0, 1);
        Vec3 forward = heading.scale(0.22);
        for (int i = 0; i < 3; i++) {
            Vec3 ray = frame.radial(i * 2.1 + age * 0.67);
            Vec3 tail = forward.add(ray.scale(0.12 * (1 - t)));
            Vec3 inner = forward.add(ray.scale(0.03 + t * 0.018));
            mesh.ribbon(tail, inner, frame.side().scale(0.009), GARNET, (int) (76 + 95 * t));
        }
    }

    private static void drawDark(Mesh mesh, Frame frame, FlameLook look) {
        for (int i = 0; i < look.blades(); i++) {
            var blade = look.blade(frame, i);
            int alpha = (int) (235 * look.intensity());
            mesh.quad(blade.foot().add(blade.span()), blade.foot().subtract(blade.span()),
                    blade.crest().subtract(blade.span().scale(0.61)), blade.crest().add(blade.span().scale(0.61)), CORE, alpha);
            mesh.quad(blade.crest().add(blade.span().scale(0.61)), blade.crest().subtract(blade.span().scale(0.61)),
                    blade.tip(), blade.tip(), i % 2 == 0 ? CORE : FOLD, (int) (alpha * 0.91));
        }
    }

    private static void drawEmbers(Mesh mesh, Frame frame, FlameLook look) {
        for (int i = 0; i < look.blades(); i++) {
            var blade = look.blade(frame, i);
            Vec3 a = blade.foot().add(blade.span());
            Vec3 b = blade.crest().add(blade.span().scale(0.61));
            Vec3 width = blade.span().normalize().scale(0.012);
            int alpha = (int) ((94 + i % 3 * 13) * look.intensity());
            mesh.ribbon(a, b, width, EMBER, alpha / 2);
            mesh.ribbon(b, blade.tip(), width.scale(0.76), GARNET, alpha);
        }
    }

    @Override
    public ResourceLocation getTextureLocation(AmaterasuFireEntity entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }

    private record FlameLook(int seed, float age, float intensity, boolean body, boolean low) {
        private int blades() {
            return low ? 5 : 9;
        }

        private Blade blade(Frame frame, int i) {
            double q = i * (Math.PI * 2 / this.blades()) + seed * 0.24;
            Vec3 radial = frame.radial(q);
            Vec3 span = frame.axis().cross(radial).scale((body ? 0.105 : 0.145) * (0.48 + intensity * 0.52));
            double lick = (0.66 + 0.25 * Math.sin(age * 0.42 + q * 2)) * (body ? 1.18 : 1.31);
            Vec3 foot = radial.scale(body ? 0.18 : 0.46).subtract(frame.axis().scale(body ? 0.18 : 0));
            Vec3 crest = foot.add(frame.axis().scale(lick * 0.45 * intensity)).add(radial.scale(0.068));
            Vec3 tip = foot.add(frame.axis().scale(lick * intensity)).add(frame.radial(q + 0.67).scale(0.21));
            return new Blade(foot, crest, tip, span);
        }
    }

    private record Blade(Vec3 foot, Vec3 crest, Vec3 tip, Vec3 span) {
    }

    private record Frame(Vec3 axis, Vec3 side, Vec3 up) {
        private static Frame around(Vec3 axis) {
            Vec3 side = axis.cross(Math.abs(axis.y) > 0.91 ? RIGHT : UP).normalize();
            return new Frame(axis, side, axis.cross(side).normalize());
        }

        private Vec3 radial(double angle) {
            return side.scale(Math.cos(angle)).add(up.scale(Math.sin(angle)));
        }
    }

    private record Tone(int red, int green, int blue) {
    }

    private record Mesh(Matrix4f matrix, VertexConsumer buffer) {
        private void ribbon(Vec3 from, Vec3 to, Vec3 width, Tone tint, int alpha) {
            this.quad(from.add(width), from.subtract(width), to.subtract(width), to.add(width), tint, alpha);
        }

        private void quad(Vec3 a, Vec3 b, Vec3 c, Vec3 d, Tone tint, int alpha) {
            vertex(a, tint, alpha);
            vertex(b, tint, alpha);
            vertex(c, tint, alpha);
            vertex(d, tint, alpha);
        }

        private void vertex(Vec3 point, Tone tint, int alpha) {
            buffer.vertex(matrix, (float) point.x, (float) point.y, (float) point.z)
                    .color(tint.red(), tint.green(), tint.blue(), alpha).endVertex();
        }
    }
}
