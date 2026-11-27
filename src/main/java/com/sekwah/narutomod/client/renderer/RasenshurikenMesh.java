package com.sekwah.narutomod.client.renderer;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

public final class RasenshurikenMesh {
    private static final float[][] BLADE = {
            {-0.12f, 0.15f}, {-0.29f, 0.42f}, {-0.30f, 0.78f}, {-0.05f, 1.13f}, {0.28f, 1.44f},
            {0.18f, 1.03f}, {0.40f, 0.73f}, {0.43f, 0.46f}, {0.21f, 0.18f},
    };
    private static final int SECTORS = 24;

    private RasenshurikenMesh() {
    }

    public static void disc(Matrix4f matrix, VertexConsumer buffer, float age, float shaping) {
        float angle = age * 0.71f;
        bands(matrix, buffer, 0.34f, age * 0.44f, 0.91f, 3);
        for (int blade = 0; blade < 4; blade++) {
            float theta = angle + blade * Mth.HALF_PI;
            drawBladeLayer(matrix, buffer, shaping, theta, 0.045f, 0.78f);
            drawBladeLayer(matrix, buffer, shaping, theta, -0.045f, 0.42f);
        }
        flightWake(matrix, buffer, shaping, age);
    }

    private static void drawBladeLayer(Matrix4f matrix, VertexConsumer buffer, float size, float theta,
                                       float height, float alpha) {
        float cos = Mth.cos(theta);
        float sin = Mth.sin(theta);
        for (int i = 0; i < BLADE.length; i++) {
            float[] a = BLADE[i];
            float[] b = BLADE[(i + 1) % BLADE.length];
            v(matrix, buffer, sin * 0.35f * size, height, cos * 0.35f * size, 0.4f, 0.78f, alpha * 0.75f);
            v(matrix, buffer, sin * 0.35f * size, height, cos * 0.35f * size, 0.4f, 0.78f, alpha * 0.75f);
            tip(matrix, buffer, a, size, cos, sin, height, alpha);
            tip(matrix, buffer, b, size, cos, sin, height, alpha);
        }
    }

    private static void tip(Matrix4f matrix, VertexConsumer buffer, float[] point, float scale,
                            float cos, float sin, float y, float alpha) {
        float x = (point[0] * cos + point[1] * sin) * scale;
        float z = (-point[0] * sin + point[1] * cos) * scale;
        v(matrix, buffer, x, y, z, 0.79f, 0.96f, alpha * (point[1] > 1.0f ? 0.94f : 0.62f));
    }

    private static void flightWake(Matrix4f matrix, VertexConsumer buffer, float shaping, float age) {
        float phase = Mth.sin(age * 0.67f);
        for (int i = -1; i <= 1; i++) {
            float x = i * 0.18f;
            float end = -shaping * (i == 0 ? 1.55f : 0.98f);
            v(matrix, buffer, x - 0.052f, 0.06f, 0.10f, 0.40f, 0.85f, 0.54f * shaping);
            v(matrix, buffer, x + 0.052f, 0.06f, 0.10f, 0.64f, 0.94f, 0.47f * shaping);
            v(matrix, buffer, x + phase * 0.13f, 0.10f, end, 0.18f, 0.54f, 0);
            v(matrix, buffer, x + phase * 0.13f, 0.10f, end, 0.18f, 0.54f, 0);
        }
    }

    public static void vortex(Matrix4f matrix, VertexConsumer buffer, float radius, float age, float fade, boolean low) {
        bands(matrix, buffer, radius, age * 1.03f, fade, low ? 5 : 8);
        bands(matrix, buffer, radius * 0.82f, -age * 0.88f, fade * 0.58f, low ? 3 : 5);
    }

    private static void bands(Matrix4f matrix, VertexConsumer buffer, float radius, float twist, float alpha, int count) {
        for (int band = 0; band < count; band++) {
            float lat = Mth.lerp((band + 0.5f) / count, -1.27f, 1.27f);
            float offset = twist * (band % 2 == 0 ? 1 : -1) + band * 1.41f;
            for (int i = 0; i < SECTORS; i++) {
                float start = offset + (i / (float) SECTORS) * Mth.TWO_PI;
                float end = start + Mth.TWO_PI / SECTORS;
                float streak = Math.max(0, Mth.sin(i * 0.63f + band * 0.77f + twist * 0.35f));
                point(matrix, buffer, radius, lat - 0.04f, start, 0.19f, 0.62f, alpha * 0.08f * streak);
                point(matrix, buffer, radius, lat + 0.06f, start, 0.63f, 0.91f, alpha * 0.48f * streak);
                point(matrix, buffer, radius, lat + 0.06f, end, 0.66f, 1.0f, alpha * 0.65f * streak);
                point(matrix, buffer, radius, lat - 0.04f, end, 0.24f, 0.66f, alpha * 0.12f * streak);
            }
        }
    }

    private static void point(Matrix4f matrix, VertexConsumer buffer, float radius, float lat, float turn,
                              float red, float green, float alpha) {
        float y = Mth.sin(lat + 0.11f * Mth.sin(turn * 2.0f)) * radius;
        float ring = Mth.sqrt(Math.max(0, radius * radius - y * y));
        v(matrix, buffer, Mth.cos(turn) * ring, y, Mth.sin(turn) * ring, red, green, alpha);
    }

    private static void v(Matrix4f matrix, VertexConsumer buffer, float x, float y, float z,
                          float red, float green, float alpha) {
        buffer.vertex(matrix, x, y, z).color(red, green, 1.0f, alpha).endVertex();
    }
}
