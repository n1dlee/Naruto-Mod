package com.sekwah.narutomod.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.client.renderer.FinalChakraRenderer;
import com.sekwah.narutomod.events.FinalChakraEffects;
import com.sekwah.narutomod.entity.jutsuprojectile.TailedBeastBombEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

public final class TailedBeastBombRenderer extends EntityRenderer<TailedBeastBombEntity> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(NarutoMod.MOD_ID, "textures/entity/biju/bijudama.png");

    public TailedBeastBombRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(TailedBeastBombEntity entity, float yaw, float partial,
                       PoseStack poses, MultiBufferSource buffers, int light) {
        var color = entity.getVariant().getChakraColour();
        var frame = new FinalChakraRenderer.Core(1, entity.getPower(),
                FinalChakraEffects.rgb(color.x(), color.y(), color.z()), entity.tickCount + partial, false);
        FinalChakraRenderer.bijuCore(poses.last().pose(), buffers, Vec3.ZERO, entity.getDeltaMovement(), frame);
        super.render(entity, yaw, partial, poses, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(TailedBeastBombEntity entity) {
        return TEXTURE;
    }
}
