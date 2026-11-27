package com.sekwah.narutomod.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.sekwah.narutomod.client.model.entity.OneTailModel;
import com.sekwah.narutomod.client.model.entity.TwoTailsModel;
import com.sekwah.narutomod.client.model.entity.ThreeTailsModel;
import com.sekwah.narutomod.client.model.entity.FourTailsModel;
import com.sekwah.narutomod.client.model.entity.FiveTailsModel;
import com.sekwah.narutomod.client.model.entity.SixTailsModel;
import com.sekwah.narutomod.client.model.entity.SevenTailsModel;
import com.sekwah.narutomod.client.model.entity.EightTailsModel;
import com.sekwah.narutomod.client.renderer.FinalChakraRenderer;
import com.sekwah.narutomod.entity.TailedBeastEntity;
import com.sekwah.narutomod.entity.TailedBeastVariant;
import com.sekwah.narutomod.events.FinalChakraEffects;
import com.sekwah.narutomod.util.BijudamaProfile;
import com.sekwah.narutomod.util.KamuiStep;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Function;

public final class TailedBeastRenderer extends EntityRenderer<TailedBeastEntity> {
    private final Map<TailedBeastVariant, Rig> models = new EnumMap<>(TailedBeastVariant.class);

    public TailedBeastRenderer(EntityRendererProvider.Context context) {
        super(context);
        register(TailedBeastVariant.SHUKAKU, context.bakeLayer(OneTailModel.LAYER_LOCATION), OneTailModel::new);
        register(TailedBeastVariant.MATATABI, context.bakeLayer(TwoTailsModel.LAYER_LOCATION), TwoTailsModel::new);
        register(TailedBeastVariant.ISOBU, context.bakeLayer(ThreeTailsModel.LAYER_LOCATION), ThreeTailsModel::new);
        register(TailedBeastVariant.SON_GOKU, context.bakeLayer(FourTailsModel.LAYER_LOCATION), FourTailsModel::new);
        register(TailedBeastVariant.KOKUO, context.bakeLayer(FiveTailsModel.LAYER_LOCATION), FiveTailsModel::new);
        register(TailedBeastVariant.SAIKEN, context.bakeLayer(SixTailsModel.LAYER_LOCATION), SixTailsModel::new);
        register(TailedBeastVariant.CHOMEI, context.bakeLayer(SevenTailsModel.LAYER_LOCATION), SevenTailsModel::new);
        register(TailedBeastVariant.GYUKI, context.bakeLayer(EightTailsModel.LAYER_LOCATION), EightTailsModel::new);
    }

    private void register(TailedBeastVariant variant, ModelPart root, Function<ModelPart, Model> factory) {
        boolean alternate = variant == TailedBeastVariant.MATATABI || variant == TailedBeastVariant.KOKUO;
        ModelPart head = root.getChild(alternate ? "field_78148_b" : "field_78115_e")
                .getChild(alternate ? "field_78150_a" : "field_78116_c");
        this.models.put(variant, new Rig(factory.apply(root), head));
    }

    @Override
    public void render(TailedBeastEntity entity, float yaw, float partial,
                       PoseStack poses, MultiBufferSource buffers, int light) {
        var variant = entity.getVariant();
        Rig rig = this.models.get(variant);
        if (rig == null) {
            return;
        }
        float scale = variant.getRenderScale();
        float body = Mth.rotLerp(partial, entity.yBodyRotO, entity.yBodyRot);
        float head = Mth.rotLerp(partial, entity.yHeadRotO, entity.yHeadRot);
        poses.pushPose();
        try {
            poses.translate(0, variant.getFeetOffset() * scale, 0);
            poses.mulPose(Axis.YP.rotationDegrees(180 - body));
            poses.scale(-scale, -scale, scale);
            rig.pose(entity, partial, head - body);
            rig.model().renderToBuffer(poses, buffers.getBuffer(RenderType.entityCutoutNoCull(variant.getTexture())),
                    light, OverlayTexture.NO_OVERLAY, 1, 1, 1, 1);
        } finally {
            rig.head().resetPose();
            poses.popPose();
        }
        if (entity.getBijudamaCharge() > 0) {
            Vec3 mouth = BijudamaProfile.mouth(variant, Vec3.ZERO, entity.getBbWidth(), entity.getBbHeight(), head);
            var tint = variant.getChakraColour();
            var core = new FinalChakraRenderer.Core(entity.getBijudamaCharge(), BijudamaProfile.radius(variant, entity.getRage()),
                    FinalChakraEffects.rgb(tint.x(), tint.y(), tint.z()), entity.tickCount + partial, true);
            FinalChakraRenderer.bijuCore(poses.last().pose(), buffers, mouth, KamuiStep.facing(head), core);
        }
        super.render(entity, yaw, partial, poses, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(TailedBeastEntity entity) {
        return entity.getVariant().getTexture();
    }

    private record Rig(Model model, ModelPart head) {
        private void pose(TailedBeastEntity entity, float partial, float yaw) {
            head.resetPose();
            head.yRot += Mth.clamp(Mth.wrapDegrees(yaw), -50, 50) * Mth.DEG_TO_RAD;
            float recoil = Mth.clamp((entity.getBijudamaRecoil() - partial) / 8, 0, 1);
            float charge = entity.getBijudamaCharge();
            head.xRot += recoil * recoil * 0.35f - charge * charge * 0.16f;
        }
    }
}
