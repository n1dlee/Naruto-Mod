package com.sekwah.narutomod.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.capabilities.NinjaCapabilityHandler;
import com.sekwah.narutomod.client.renderer.NarutoRenderTypes;
import com.sekwah.narutomod.client.renderer.KotoInfluenceRenderer;
import com.sekwah.narutomod.client.renderer.SharinganGazeRenderer;
import com.sekwah.narutomod.util.ByakuganSight;
import com.sekwah.narutomod.util.DojutsuVisual;
import com.sekwah.narutomod.util.DojutsuVisual.Eye;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.WeakHashMap;

public class PlayerEyeLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private static final Map<Eye, ResourceLocation> TEXTURES = Map.of(
            Eye.TOMOE_ONE, crest(1), Eye.TOMOE_TWO, crest(2), Eye.TOMOE_THREE, crest(3),
            Eye.MANGEKYO, texture("mangekyo"),
            Eye.BYAKUGAN, texture("byakugan"), Eye.BYAKUGAN_FOCUS, texture("byakugan"),
            Eye.RINNEGAN, texture("rinnegan"), Eye.SAGE, texture("sage"));
    private final Map<AbstractClientPlayer, Reveal> reveals = new WeakHashMap<>();

    public PlayerEyeLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player,
                       float swing, float swingAmount, float partial, float age, float headYaw, float headPitch) {
        if (player.isInvisible()) {
            this.reveals.remove(player);
            return;
        }
        int mask = player.getCapability(NinjaCapabilityHandler.NINJA_DATA).map(data -> data.getEyeVisualState()).orElse(0);
        if (mask == 0) {
            this.reveals.remove(player);
            return;
        }
        var reveal = this.reveals.computeIfAbsent(player, ignored -> new Reveal());
        reveal.right = change(reveal.right, DojutsuVisual.eye(mask, true), age);
        reveal.left = change(reveal.left, DojutsuVisual.eye(mask, false), age);
        var glints = new Glints(SharinganGazeRenderer.iris(player, partial), KotoInfluenceRenderer.iris(player, partial));
        pose.pushPose();
        try {
            getParentModel().head.translateAndRotate(pose);
            draw(pose.last(), buffers, reveal.right, true, age, glints);
            draw(pose.last(), buffers, reveal.left, false, age, glints);
        } finally {
            pose.popPose();
        }
    }

    private static Iris change(Iris previous, Eye kind, float age) {
        return previous == null || previous.kind() != kind ? new Iris(kind, age) : previous;
    }

    private static void draw(PoseStack.Pose pose, MultiBufferSource buffers, Iris iris, boolean right, float age, Glints glints) {
        ResourceLocation texture = TEXTURES.get(iris.kind());
        float progress = DojutsuVisual.reveal(age - iris.start());
        if (texture == null || progress <= 0.01f) {
            return;
        }
        VertexConsumer mesh = buffers.getBuffer(RenderType.eyes(texture));
        float u = DojutsuVisual.textureU(right) + 1;
        float v = 13;
        float height = 0.8f * progress + 0.2f;
        boolean crest = iris.kind() == Eye.TOMOE_ONE || iris.kind() == Eye.TOMOE_TWO || iris.kind() == Eye.TOMOE_THREE;
        boolean sharingan = crest || iris.kind() == Eye.MANGEKYO;
        if (sharingan) {
            height *= 1f - glints.gaze() * 0.37f;
        }
        var eye = new Patch(pose, mesh, 0.44f + progress * 0.56f, crest ? u - 1 : -1);
        eye.vertex(u - progress, v - height);
        eye.vertex(u - progress, v + height);
        eye.vertex(u + progress, v + height);
        eye.vertex(u + progress, v - height);
        boolean green = iris.kind() == Eye.MANGEKYO && glints.koto() > 0.01f;
        float strength = green ? glints.koto() : glints.gaze();
        if (sharingan && strength > 0.01f) {
            glint(pose, buffers.getBuffer(NarutoRenderTypes.CHAKRA_GLOW), (u - 12) / 16f, strength, green);
        }
        if (iris.kind() == Eye.BYAKUGAN_FOCUS) {
            float tense = ByakuganSight.reveal(age - iris.start());
            var veins = new Veins(pose, buffers.getBuffer(NarutoRenderTypes.CHAKRA_GLOW), tense);
            veins.draw(right ? -1 : 1);
        }
    }

    private static void glint(PoseStack.Pose pose, VertexConsumer mesh, float x, float gaze, boolean green) {
        float width = 0.045f + 0.026f * gaze;
        glintVertex(pose, mesh, x - width, -0.1875f, 0, green);
        glintVertex(pose, mesh, x, -0.1875f - 0.009f * gaze, gaze * 0.72f, green);
        glintVertex(pose, mesh, x + width, -0.1875f, 0, green);
        glintVertex(pose, mesh, x, -0.1875f + 0.009f * gaze, gaze * 0.72f, green);
    }

    private static void glintVertex(PoseStack.Pose pose, VertexConsumer mesh, float x, float y, float alpha, boolean green) {
        mesh.vertex(pose.pose(), x, y, -0.289f).color(green ? 0.32f : 0.86f, green ? 0.72f : 0.19f,
                green ? 0.50f : 0.26f, alpha).endVertex();
    }

    private static ResourceLocation texture(String name) {
        return new ResourceLocation(NarutoMod.MOD_ID, "textures/entity/eyes/" + name + "_both.png");
    }

    private static ResourceLocation crest(int count) {
        return new ResourceLocation(NarutoMod.MOD_ID, "textures/effects/sharingan/sharingan_" + count + ".png");
    }

    private record Iris(Eye kind, float start) {
    }

    private record Glints(float gaze, float koto) {
    }

    private static final class Reveal {
        private Iris right;
        private Iris left;
    }

    private record Patch(PoseStack.Pose pose, VertexConsumer consumer, float alpha, float crestOrigin) {
        private void vertex(float u, float v) {
            float sampleU = crestOrigin >= 0 ? (u - crestOrigin) * 0.5f : u / 64f;
            float sampleV = crestOrigin >= 0 ? (v - 12) * 0.5f : v / 64f;
            consumer.vertex(pose.pose(), (u - 12) / 16f, (v - 16) / 16f, -0.251f)
                    .color(1f, 1f, 1f, alpha).uv(sampleU, sampleV)
                    .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT)
                    .normal(pose.normal(), 0, 0, -1).endVertex();
        }
    }

    private record Veins(PoseStack.Pose pose, VertexConsumer mesh, float reveal) {
        private void draw(int side) {
            float root = side * 0.17f;
            float fork = root + side * 0.025f * reveal;
            float tip = root + side * 0.067f * reveal;
            this.segment(root, -0.177f, fork, -0.137f, 0.005f, 0.29f);
            this.segment(fork, -0.137f, tip, -0.125f, 0.004f, 0.23f);
            this.segment(fork, -0.137f, root + side * 0.058f * reveal, -0.081f, 0.003f, 0.19f);
            this.segment(root, -0.234f, root + side * 0.052f * reveal, -0.276f, 0.0035f, 0.22f);
        }

        private void segment(float ax, float ay, float bx, float by, float width, float alpha) {
            this.vertex(ax, ay - width, alpha);
            this.vertex(ax, ay + width, alpha);
            this.vertex(bx, by + width * 0.4f, alpha * 0.59f);
            this.vertex(bx, by - width * 0.4f, alpha * 0.59f);
        }

        private void vertex(float x, float y, float alpha) {
            mesh.vertex(pose.pose(), x, y, -0.252f).color(0.70f, 0.76f, 0.89f, alpha * this.reveal).endVertex();
        }
    }
}
