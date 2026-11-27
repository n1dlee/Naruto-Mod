package com.sekwah.narutomod.client.renderer.entity;

import com.mojang.math.Axis;
import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.client.model.entity.KuramaFoxModel;
import com.sekwah.narutomod.client.renderer.KuramaClawRenderer;
import com.sekwah.narutomod.util.GiantForm;
import com.sekwah.narutomod.util.KuramaCloakMotion;
import com.sekwah.narutomod.util.KuramaCloakMotion.Strike;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.client.event.RenderPlayerEvent;

public final class KuramaTailRenderer {
    private static final ResourceLocation FOX_TEXTURE =
            new ResourceLocation(NarutoMod.MOD_ID, "textures/entity/biju/ninetails.png");
    private static final RenderType FOX_RENDER_TYPE = RenderType.entityTranslucent(FOX_TEXTURE);
    private static final float SCALE = GiantForm.HEIGHT_BLOCKS / KuramaFoxModel.BODY_HEIGHT_BLOCKS;
    private static KuramaFoxModel foxModel;

    private KuramaTailRenderer() {
    }

    public static void setFoxModel(KuramaFoxModel bakedModel) {
        foxModel = bakedModel;
    }

    static KuramaFoxModel foxModel() {
        return foxModel;
    }

    static ResourceLocation foxTexture() {
        return FOX_TEXTURE;
    }

    public static void renderFullAvatar(RenderPlayerEvent.Pre event, INinjaData data) {
        var model = foxModel;
        var player = event.getEntity();
        if (model == null || player.isInvisible()) {
            return;
        }
        var poses = event.getPoseStack();
        float partial = event.getPartialTick();
        float yaw = Mth.rotLerp(partial, player.yBodyRotO, player.yBodyRot);
        float reveal = KuramaCloakMotion.unfold(data.getKuramaTailAge() + partial, 9);
        poses.pushPose();
        try {
            poses.mulPose(Axis.YP.rotationDegrees(180 - yaw));
            poses.translate(0, 0, 0.75);
            poses.scale(-SCALE, -SCALE, SCALE);
            poses.translate(0, -KuramaFoxModel.FEET_OFFSET, 0);
            model.waveTails(player.tickCount + partial + player.getId() * 0.37f);
            var attack = KuramaClawRenderer.pose(player, partial);
            if (attack != null && attack.kind() == Strike.PAW) {
                model.strikePaw(attack.progress());
            } else if (attack != null) {
                model.strikeTails(attack.progress());
            }
            model.renderToBuffer(poses, event.getMultiBufferSource().getBuffer(FOX_RENDER_TYPE),
                    event.getPackedLight(), OverlayTexture.NO_OVERLAY, 1, 0.49f, 0.42f, 0.46f + reveal * 0.45f);
        } finally {
            poses.popPose();
        }
    }
}
