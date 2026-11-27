package com.sekwah.narutomod.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.sekwah.narutomod.capabilities.NinjaCapabilityHandler;
import com.sekwah.narutomod.util.JutsuVfx;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.WeakHashMap;

public final class RasenganHandRenderer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private static final float FORM_TICKS = 8f;
    private static final float RELEASE_TICKS = 4f;
    private static final Map<AbstractClientPlayer, Charge> CHARGES = new WeakHashMap<>();

    public RasenganHandRenderer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffer, int light, AbstractClientPlayer player,
                       float swing, float swingAmount, float partialTick, float age, float yaw, float pitch) {
        var viewer = Minecraft.getInstance().player;
        if (viewer != null && !player.isInvisibleTo(viewer)) {
            var model = getParentModel();
            ModelPart arm = player.getMainArm() == HumanoidArm.RIGHT ? model.rightArm : model.leftArm;
            drawAtArm(player, arm, pose, buffer, partialTick);
        }
    }

    public static void drawAtArm(AbstractClientPlayer player, ModelPart arm, PoseStack pose,
                                 MultiBufferSource buffer, float partialTick) {
        float age = player.tickCount + partialTick;
        float radius = visualRadius(player, age);
        if (radius <= 0f || !arm.visible) {
            return;
        }
        pose.pushPose();
        arm.translateAndRotate(pose);
        double side = player.getMainArm() == HumanoidArm.RIGHT ? -1.0 : 1.0;
        Vec3 palm = new Vec3(side / 16.0, 0.66 + radius * 0.36, -0.105);
        JutsuWorldRenderer.drawRasengan(pose, buffer.getBuffer(NarutoRenderTypes.CHAKRA_GLOW), palm, radius, age);
        pose.popPose();
    }

    static boolean drawWithHeldItem(AbstractClientPlayer player, PoseStack pose, VertexConsumer consumer, float partialTick) {
        float age = player.tickCount + partialTick;
        float radius = visualRadius(player, age);
        if (radius <= 0f) {
            return false;
        }
        double side = player.getMainArm() == HumanoidArm.RIGHT ? -1.0 : 1.0;
        Vec3 palm = JutsuVfx.handPosition(player, side, partialTick);
        JutsuWorldRenderer.drawRasengan(pose, consumer, palm, radius, age);
        return true;
    }

    private static float visualRadius(AbstractClientPlayer player, float age) {
        var data = player.getCapability(NinjaCapabilityHandler.NINJA_DATA).resolve();
        if (data.isPresent() && data.get().isNinjaModeEnabled() && data.get().isRasenganHeld()) {
            Charge previous = CHARGES.get(player);
            float started = previous == null || !previous.held ? age : previous.changedAt;
            float strength = Mth.clamp((data.get().getRasenganCharge() - 20) / 40f, 0f, 1f);
            float progress = Mth.clamp((age - started) / FORM_TICKS, 0f, 1f);
            float radius = (0.24f + strength * 0.26f) * (0.14f + 0.86f * progress * (2f - progress));
            CHARGES.put(player, new Charge(true, started, radius));
            return radius;
        }
        Charge previous = CHARGES.get(player);
        if (previous == null) {
            return 0f;
        }
        float releasedAt = previous.held ? age : previous.changedAt;
        float remaining = Mth.clamp(1f - (age - releasedAt) / RELEASE_TICKS, 0f, 1f);
        if (remaining <= 0f) {
            CHARGES.remove(player);
            return 0f;
        }
        if (previous.held) {
            CHARGES.put(player, new Charge(false, releasedAt, previous.radius));
        }
        return previous.radius * remaining * remaining;
    }

    private record Charge(boolean held, float changedAt, float radius) {
    }
}
