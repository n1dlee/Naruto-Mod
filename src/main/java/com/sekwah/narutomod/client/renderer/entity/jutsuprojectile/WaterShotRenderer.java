package com.sekwah.narutomod.client.renderer.entity.jutsuprojectile;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.sekwah.narutomod.client.renderer.NarutoRenderTypes;
import com.sekwah.narutomod.entity.jutsuprojectile.WaterShotEntity;
import com.sekwah.narutomod.util.WaterShotShape;
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

public class WaterShotRenderer extends EntityRenderer<WaterShotEntity> {
    private static final Vec3 UP = new Vec3(0, 1, 0);
    private static final Vec3 X = new Vec3(1, 0, 0);
    private static final Tone BODY = new Tone(28, 114, 163);
    private static final Tone EDGE = new Tone(125, 223, 245);
    private static final Tone FOAM = new Tone(213, 252, 255);

    public WaterShotRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(WaterShotEntity entity, float yaw, float partial, PoseStack poses, MultiBufferSource buffers, int light) {
        super.render(entity, yaw, partial, poses, buffers, light);
        Minecraft game = Minecraft.getInstance();
        boolean low = game.options.graphicsMode().get() == GraphicsStatus.FAST
                || entity.distanceToSqr(game.getCameraEntity() == null ? entity : game.getCameraEntity()) > 32 * 32;
        var matrix = poses.last().pose();
        var water = new Surface(matrix, buffers.getBuffer(NarutoRenderTypes.WATER_SURFACE));
        float age = entity.age() + partial;
        Vec3 heading = entity.heading();
        switch (entity.phase()) {
            case GATHER -> body(water, heading, WaterShotShape.radius(entity.heavy()),
                    Mth.clamp(age / WaterShotShape.PREPARE, 0.15f, 1), low);
            case FLIGHT -> body(water, heading, WaterShotShape.radius(entity.heavy()), 1, low);
            case BLOCK, ENTITY, WATER -> splash(water, entity, heading, age, low);
            case FADE -> body(water, heading, WaterShotShape.radius(entity.heavy()),
                    Mth.clamp(1 - age / 4, 0, 1), low);
        }
    }

    private static void body(Surface surface, Vec3 heading, double width, float amount, boolean low) {
        if (amount <= 0) {
            return;
        }
        Frame frame = Frame.around(heading);
        double radius = width * amount;
        Vec3 front = heading.scale(radius);
        Vec3 neck = heading.scale(-radius * 0.35);
        Vec3 tail = heading.scale(-radius * 4.7 * amount);
        int facets = low ? 6 : 10;
        for (int i = 0; i < facets; i++) {
            Vec3 a = neck.add(frame.radial(i, facets).scale(radius));
            Vec3 b = neck.add(frame.radial(i + 1, facets).scale(radius));
            surface.quad(front, front, a, b, i % 3 == 0 ? EDGE : BODY, 205);
            surface.quad(a, b, tail, tail, i % 3 == 0 ? EDGE : BODY, 146);
        }
        if (!low && amount > 0.6f) {
            Vec3 gleam = frame.radial(1, facets).scale(radius * 0.96).add(neck);
            Vec3 thin = frame.across().scale(radius * 0.18);
            surface.quad(gleam.add(thin), gleam.subtract(thin), tail.scale(0.65), front.scale(0.82), FOAM, 169);
        }
    }

    private static void splash(Surface surface, WaterShotEntity shot, Vec3 heading, float age, boolean low) {
        float t = Mth.clamp(age / WaterShotShape.SPLASH_TICKS, 0, 1);
        Vec3 normal = Vec3.atLowerCornerOf(shot.face().getNormal());
        if (shot.phase() == WaterShotEntity.Phase.WATER) {
            ripple(surface, Frame.around(normal), normal, (0.12 + t) * (shot.heavy() ? 1.35 : 0.92), t, low);
            return;
        }
        Vec3 flow = WaterShotShape.reflect(heading, normal).scale(0.6).add(normal).normalize();
        Frame frame = Frame.around(flow);
        int droplets = low ? 6 : 12;
        double distance = (shot.heavy() ? 2.15 : 1.45) * (0.16 + t * 0.84);
        for (int i = 0; i < droplets; i++) {
            Vec3 spread = frame.radial(i, droplets);
            Vec3 axis = flow.add(spread.scale(0.25 + i % 4 * 0.2)).normalize();
            if (axis.dot(normal) <= 0) {
                axis = axis.add(normal.scale(0.3 - axis.dot(normal))).normalize();
            }
            Vec3 point = axis.scale(distance * (0.72 + (i % 3) * 0.13));
            Vec3 span = spread.scale(0.027 * (1 + t));
            Vec3 foot = point.scale(0.64);
            surface.quad(foot, point.subtract(span), point.add(axis.scale(0.18)), point.add(span),
                    i % 3 == 0 ? FOAM : EDGE, (int) (207 * (1 - t)));
        }
    }

    private static void ripple(Surface surface, Frame frame, Vec3 normal, double radius, float t, boolean low) {
        Vec3 origin = normal.scale(0.028);
        int sides = low ? 12 : 20;
        for (int i = 0; i < sides; i++) {
            Vec3 a = frame.radial(i, sides);
            Vec3 b = frame.radial(i + 1, sides);
            surface.quad(origin.add(a.scale(radius)), origin.add(b.scale(radius)),
                    origin.add(b.scale(radius * 0.85)), origin.add(a.scale(radius * 0.85)), FOAM, (int) (181 * (1 - t)));
        }
    }

    @Override
    public ResourceLocation getTextureLocation(WaterShotEntity entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }

    private record Frame(Vec3 across, Vec3 up) {
        private static Frame around(Vec3 direction) {
            Vec3 across = direction.cross(Math.abs(direction.y) < 0.95 ? UP : X).normalize();
            return new Frame(across, direction.cross(across).normalize());
        }

        private Vec3 radial(int index, int count) {
            double angle = index * 2 * Math.PI / count;
            return across.scale(Math.cos(angle)).add(up.scale(Math.sin(angle)));
        }
    }

    private record Tone(int r, int g, int b) {
    }

    private record Surface(Matrix4f matrix, VertexConsumer buffer) {
        private void quad(Vec3 a, Vec3 b, Vec3 c, Vec3 d, Tone color, int alpha) {
            vertex(a, color, alpha);
            vertex(b, color, alpha);
            vertex(c, color, alpha);
            vertex(d, color, alpha);
        }

        private void vertex(Vec3 point, Tone color, int alpha) {
            buffer.vertex(matrix, (float) point.x, (float) point.y, (float) point.z)
                    .color(color.r(), color.g(), color.b(), alpha).endVertex();
        }
    }
}
