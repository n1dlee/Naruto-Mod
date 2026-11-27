package com.sekwah.narutomod.client.renderer;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.List;

public final class WaterDragonBodyRenderer {
    private static final int SIDES = 8;
    private static final int SPIRAL_SEGMENTS = 18;
    private static final Vec3 UP = new Vec3(0, 1, 0);
    private static final Vec3 EAST = new Vec3(1, 0, 0);

    private WaterDragonBodyRenderer() {
    }

    public static void draw(Matrix4f matrix, VertexConsumer buffer, List<Vec3> path, Vec3 origin,
                            float scale, float age, boolean breaking) {
        if (path.size() < 2) {
            return;
        }
        Vec3[] previous = new Vec3[SIDES];
        Vec3[] next = new Vec3[SIDES];
        for (int i = 0; i < path.size(); i++) {
            float t = i / (float) (path.size() - 1);
            makeRing(next, path, i, origin, t, scale, age, breaking);
            if (i > 0) {
                stitch(matrix, buffer, previous, next, t, age, breaking);
            }
            Vec3[] swap = previous;
            previous = next;
            next = swap;
        }
    }

    private static void makeRing(Vec3[] ring, List<Vec3> path, int i, Vec3 origin, float t,
                                 float scale, float age, boolean breaking) {
        Vec3 headward = path.get(Math.max(0, i - 1))
                .subtract(path.get(Math.min(path.size() - 1, i + 1))).normalize();
        Vec3 right = headward.cross(Math.abs(headward.y) > 0.94 ? EAST : UP).normalize();
        Vec3 up = right.cross(headward);
        float pulse = 0.9f + 0.1f * Mth.sin(age * 0.82f - t * 17.0f);
        float radius = scale * (0.32f + 0.27f * Mth.sin(Mth.PI * t)) * (1.0f - 0.96f * t) * pulse;
        Vec3 center = path.get(i).subtract(origin)
                .add(right.scale(scale * 0.21 * t * Mth.sin(t * 14.5f - age * 0.49f)))
                .add(up.scale(scale * 0.15 * t * Mth.cos(t * 12.0f - age * 0.42f)));
        for (int j = 0; j < SIDES; j++) {
            float angle = j * Mth.TWO_PI / SIDES;
            float ripple = 1.0f + 0.13f * Mth.sin(angle * 3.0f - t * 12.0f + age * 0.78f);
            ring[j] = center.add(right.scale(radius * Mth.cos(angle) * ripple))
                    .add(up.scale(radius * Mth.sin(angle) * ripple * (breaking ? 0.64 : 1.0)));
        }
    }

    private static void stitch(Matrix4f matrix, VertexConsumer buffer, Vec3[] from, Vec3[] to,
                               float t, float age, boolean breaking) {
        float alpha = (breaking ? 0.34f : 0.72f) * (1.0f - t * 0.85f);
        for (int j = 0; j < SIDES; j++) {
            int k = (j + 1) % SIDES;
            float wave = Mth.sin(age * 0.68f - t * 28.0f + j * Mth.TWO_PI / SIDES);
            float crest = Math.max(0.0f, (wave - 0.48f) * 1.9f);
            quad(matrix, buffer, from[j], from[k], to[k], to[j], crest, alpha);
        }
    }

    public static void drawGather(Matrix4f matrix, VertexConsumer buffer, float progress, float age) {
        float width = 0.03f + 0.115f * progress;
        for (int strand = 0; strand < 3; strand++) {
            Vec3 inner = coil(0, strand, width, progress, age);
            Vec3 outer = coil(0, strand, -width, progress, age);
            for (int i = 1; i <= SPIRAL_SEGMENTS; i++) {
                float t = i / (float) SPIRAL_SEGMENTS;
                Vec3 nextInner = coil(t, strand, width, progress, age);
                Vec3 nextOuter = coil(t, strand, -width, progress, age);
                float fade = Mth.sin(t * Mth.PI) * (0.28f + 0.55f * progress);
                quad(matrix, buffer, inner, outer, nextOuter, nextInner, 0.35f + 0.52f * t, fade);
                inner = nextInner;
                outer = nextOuter;
            }
        }
    }

    private static Vec3 coil(float t, int strand, float width, float progress, float age) {
        float radius = (1.38f - progress * 0.66f) * (1.0f - t * 0.82f) + width;
        float angle = age * 0.9f + strand * Mth.TWO_PI / 3.0f + t * Mth.TWO_PI * 1.2f;
        return new Vec3(Mth.cos(angle) * radius, Mth.sin(angle) * radius * 0.8, -1.55 + t * 1.55);
    }

    private static void quad(Matrix4f matrix, VertexConsumer buffer, Vec3 a, Vec3 b, Vec3 c, Vec3 d,
                             float foam, float alpha) {
        float dark = foam * 0.35f;
        vertex(buffer, matrix, a, dark, alpha);
        vertex(buffer, matrix, b, foam, alpha);
        vertex(buffer, matrix, c, foam, alpha);
        vertex(buffer, matrix, d, dark, alpha);
    }

    private static void vertex(VertexConsumer buffer, Matrix4f matrix, Vec3 point, float foam, float alpha) {
        WaterDragonImpactRenderer.vertex(buffer, matrix, point, 0.045f + 0.86f * foam,
                0.37f + 0.60f * foam, 0.63f + 0.37f * foam, alpha);
    }
}
