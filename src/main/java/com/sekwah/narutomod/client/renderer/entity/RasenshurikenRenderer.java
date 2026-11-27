package com.sekwah.narutomod.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.client.renderer.NarutoRenderTypes;
import com.sekwah.narutomod.client.renderer.RasenshurikenMesh;
import com.sekwah.narutomod.entity.jutsuprojectile.RasenshurikenEntity;
import com.sekwah.narutomod.util.WindVortex;
import net.minecraft.client.GraphicsStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class RasenshurikenRenderer extends EntityRenderer<RasenshurikenEntity> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(NarutoMod.MOD_ID, "textures/entity/rasenshuriken.png");

    public RasenshurikenRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(RasenshurikenEntity entity, float yaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffers, int light) {
        var consumer = buffers.getBuffer(NarutoRenderTypes.CHAKRA_GLOW);
        float age = entity.phaseAge() + partialTick;
        var phase = entity.phase();
        poseStack.pushPose();
        if (phase == RasenshurikenEntity.Phase.VORTEX) {
            float radius = (float) WindVortex.radius(age);
            float fade = 1.0f - Mth.clamp((age - WindVortex.EXPANSION_TICKS) / 9.0f, 0, 1);
            boolean low = Minecraft.getInstance().options.graphicsMode().get() == GraphicsStatus.FAST;
            RasenshurikenMesh.vortex(poseStack.last().pose(), consumer, radius, age, fade, low);
        } else {
            float scale = phase == RasenshurikenEntity.Phase.FORM
                    ? Mth.clamp(age / RasenshurikenEntity.FORM_TICKS, 0, 1) : 1.0f;
            poseStack.mulPose(Axis.YP.rotationDegrees(-yaw));
            poseStack.mulPose(Axis.XP.rotationDegrees(Mth.rotLerp(partialTick, entity.xRotO, entity.getXRot())));
            poseStack.mulPose(Axis.ZP.rotationDegrees(16.0f));
            RasenshurikenMesh.disc(poseStack.last().pose(), consumer, entity.tickCount + partialTick, scale);
        }
        poseStack.popPose();
        super.render(entity, yaw, partialTick, poseStack, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(RasenshurikenEntity entity) {
        return TEXTURE;
    }
}
