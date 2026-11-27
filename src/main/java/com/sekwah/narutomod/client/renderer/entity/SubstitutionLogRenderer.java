package com.sekwah.narutomod.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.sekwah.narutomod.client.model.entity.SubstitutionLogModel;
import com.sekwah.narutomod.entity.SubstitutionLogEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;


public class SubstitutionLogRenderer extends MobRenderer<SubstitutionLogEntity, SubstitutionLogModel<SubstitutionLogEntity>> {
   public static final ResourceLocation SUBSTITUTION_LOG = new ResourceLocation("narutomod", "textures/entity/jutsu/substitution_log.png");

   public SubstitutionLogRenderer(EntityRendererProvider.Context manager) {
      super(manager, new SubstitutionLogModel(manager.bakeLayer(SubstitutionLogModel.LAYER_LOCATION)), 0.5F);
      this.shadowRadius = 0.5F;
   }

   @Override
   public void render(SubstitutionLogEntity log, float yaw, float partialTick, PoseStack stack, MultiBufferSource buffers, int packedLight) {
      this.shadowRadius = 0.3F;
      float t = Mth.clamp((log.flipTicks() + partialTick) / 10.0f, 0f, 1f);
      stack.pushPose();
      stack.translate(0, 0.34, 0);
      stack.mulPose(Axis.ZP.rotationDegrees(360f * (1f - (1f - t) * (1f - t))));
      stack.translate(0, -0.34, 0);
      super.render(log, yaw, partialTick, stack, buffers, packedLight);
      stack.popPose();
   }

   @Override
   public ResourceLocation getTextureLocation(SubstitutionLogEntity entity) {
      return SUBSTITUTION_LOG;
   }

}
