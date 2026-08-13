package com.sekwah.narutomod.client.renderer;

import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.block.NarutoBlocks;
import com.sekwah.narutomod.client.model.entity.KuramaAvatarModel;
import com.sekwah.narutomod.client.model.entity.KuramaTailModel;
import com.sekwah.narutomod.client.model.entity.SubstitutionLogModel;
import com.sekwah.narutomod.client.model.entity.SummonBeastModel;
import com.sekwah.narutomod.client.model.entity.SusanooModel;
import com.sekwah.narutomod.client.model.item.model.*;
import com.sekwah.narutomod.client.model.jutsu.FireballJutsuModel;
import com.sekwah.narutomod.client.model.jutsu.RasenganJutsuModel;
import com.sekwah.narutomod.client.model.jutsu.WaterBulletModel;
import com.sekwah.narutomod.client.renderer.entity.*;
import com.sekwah.narutomod.client.renderer.entity.jutsuprojectile.FireballJutsuRenderer;
import com.sekwah.narutomod.client.renderer.entity.jutsuprojectile.RasenganRenderer;
import com.sekwah.narutomod.client.renderer.entity.jutsuprojectile.WaterBulletJutsuRenderer;
import com.sekwah.narutomod.entity.NarutoEntities;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Example of new entity render events
 * https://github.com/MinecraftForge/MinecraftForge/blob/1.17.x/src/test/java/net/minecraftforge/debug/client/rendering/EntityRendererEventsTest.java
 */
@Mod.EventBusSubscriber(value=Dist.CLIENT, modid=NarutoMod.MOD_ID, bus= Mod.EventBusSubscriber.Bus.MOD)
public class NarutoRenderEvents {

    public static final BlockEntityWithoutLevelRenderer NARUTO_RENDERER = new NarutoResourceManager();

    @SubscribeEvent
    public static void entityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(NarutoEntities.KUNAI.get(), KunaiRenderer::new);
        event.registerEntityRenderer(NarutoEntities.EXPLOSIVE_KUNAI.get(), ExplosiveKunaiRenderer::new);
        event.registerEntityRenderer(NarutoEntities.SENBON.get(), SenbonRenderer::new);
        event.registerEntityRenderer(NarutoEntities.HIRAISHIN_KUNAI.get(),
                com.sekwah.narutomod.client.renderer.entity.HiraishinKunaiRenderer::new);
        event.registerEntityRenderer(NarutoEntities.SHURIKEN.get(), ShurikenRenderer::new);
        event.registerEntityRenderer(NarutoEntities.FUMA_SHURIKEN.get(), FumaShurikenRenderer::new);
        event.registerEntityRenderer(NarutoEntities.PAPER_BOMB.get(), PaperBombRenderer::new);

        event.registerEntityRenderer(NarutoEntities.FIREBALL_JUTSU.get(), FireballJutsuRenderer::new);
        event.registerEntityRenderer(NarutoEntities.WATER_BULLET_JUTSU.get(), WaterBulletJutsuRenderer::new);

        event.registerEntityRenderer(NarutoEntities.SUBSTITUTION_LOG.get(), SubstitutionLogRenderer::new);

        event.registerEntityRenderer(NarutoEntities.EARTH_WALL.get(), NoopRenderer::new);
        event.registerEntityRenderer(NarutoEntities.SHADOW_CLONE.get(), ShadowCloneRenderer::new);
        event.registerEntityRenderer(NarutoEntities.RASENGAN.get(), RasenganRenderer::new);
        event.registerEntityRenderer(NarutoEntities.AMATERASU_FIRE.get(), NoopRenderer::new);
        event.registerEntityRenderer(NarutoEntities.RASENSHURIKEN.get(),
                com.sekwah.narutomod.client.renderer.entity.RasenshurikenRenderer::new);
        event.registerEntityRenderer(NarutoEntities.SUMMON_BEAST.get(), SummonBeastRenderer::new);
        event.registerEntityRenderer(NarutoEntities.ROGUE_NINJA.get(),
                com.sekwah.narutomod.client.renderer.entity.RogueNinjaRenderer::new);
        event.registerEntityRenderer(NarutoEntities.UCHIHA_ROGUE.get(),
                com.sekwah.narutomod.client.renderer.entity.RogueNinjaRenderer::new);
        event.registerEntityRenderer(NarutoEntities.MANGEKYO_BOSS.get(),
                com.sekwah.narutomod.client.renderer.entity.MangekyoBossRenderer::new);
        event.registerEntityRenderer(NarutoEntities.WOOD_GOLEM.get(),
                com.sekwah.narutomod.client.renderer.entity.WoodGolemRenderer::new);

    }

    @SubscribeEvent
    public static void reloadListener(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(new NarutoResourceManager());
    }

    @SubscribeEvent
    public static void layerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event)
    {
        // Items
        event.registerLayerDefinition(AnbuMaskModel.LAYER_LOCATION, () -> AnbuMaskModel.createLayer(true));
        event.registerLayerDefinition(AnbuMaskModel.LAYER_LOCATION_WITHOUT_EARS, () -> AnbuMaskModel.createLayer(false));
        event.registerLayerDefinition(HeadbandModel.LAYER_LOCATION, HeadbandModel::createLayer);

        event.registerLayerDefinition(FlakJacketNewModel.LAYER_LOCATION, FlakJacketNewModel::createLayer);
        event.registerLayerDefinition(FlakJacketModel.LAYER_LOCATION, FlakJacketModel::createLayer);
        event.registerLayerDefinition(AnbuArmorModel.LAYER_LOCATION, AnbuArmorModel::createLayer);
        event.registerLayerDefinition(AkatsukiCloakModel.LAYER_LOCATION, AkatsukiCloakModel::createLayer);

        // Jutsu
        event.registerLayerDefinition(FireballJutsuModel.LAYER_LOCATION, FireballJutsuModel::createLayer);
        event.registerLayerDefinition(WaterBulletModel.LAYER_LOCATION, WaterBulletModel::createLayer);
        event.registerLayerDefinition(RasenganJutsuModel.LAYER_LOCATION, RasenganJutsuModel::createLayer);

        // Entity
        event.registerLayerDefinition(SubstitutionLogModel.LAYER_LOCATION, SubstitutionLogModel::createBodyLayer);
        event.registerLayerDefinition(KuramaTailModel.LAYER_LOCATION, KuramaTailModel::createLayer);
        event.registerLayerDefinition(KuramaAvatarModel.LAYER_LOCATION, KuramaAvatarModel::createLayer);
        event.registerLayerDefinition(SusanooModel.LAYER_LOCATION, SusanooModel::createLayer);
        event.registerLayerDefinition(com.sekwah.narutomod.client.model.entity.BijuCloakModel.LAYER_LOCATION,
                com.sekwah.narutomod.client.model.entity.BijuCloakModel::createBodyLayer);
        // Phase 18: the three detailed Susanoo bodies (skeleton / clothed / winged)
        event.registerLayerDefinition(com.sekwah.narutomod.client.model.entity.SusanooSkeletonModel.LAYER_LOCATION,
                com.sekwah.narutomod.client.model.entity.SusanooSkeletonModel::createBodyLayer);
        event.registerLayerDefinition(com.sekwah.narutomod.client.model.entity.SusanooClothedModel.LAYER_LOCATION,
                com.sekwah.narutomod.client.model.entity.SusanooClothedModel::createBodyLayer);
        event.registerLayerDefinition(com.sekwah.narutomod.client.model.entity.SusanooWingedModel.LAYER_LOCATION,
                com.sekwah.narutomod.client.model.entity.SusanooWingedModel::createBodyLayer);
        event.registerLayerDefinition(SummonBeastModel.LAYER_LOCATION, SummonBeastModel::createLayer);
    }

    @SubscribeEvent
    public static void entityLayers(EntityRenderersEvent.AddLayers event) {
        KuramaTailRenderer.setModel(new KuramaTailModel(event.getEntityModels().bakeLayer(KuramaTailModel.LAYER_LOCATION)));
        KuramaTailRenderer.setAvatarModel(new KuramaAvatarModel(event.getEntityModels().bakeLayer(KuramaAvatarModel.LAYER_LOCATION)));
        SusanooRenderer.setModel(new SusanooModel(event.getEntityModels().bakeLayer(SusanooModel.LAYER_LOCATION)));
        com.sekwah.narutomod.client.renderer.entity.BijuCloakRenderer.setModel(
                new com.sekwah.narutomod.client.model.entity.BijuCloakModel(event.getEntityModels()
                        .bakeLayer(com.sekwah.narutomod.client.model.entity.BijuCloakModel.LAYER_LOCATION)));
        SusanooRenderer.setDetailedModels(
                new com.sekwah.narutomod.client.model.entity.SusanooSkeletonModel(event.getEntityModels()
                        .bakeLayer(com.sekwah.narutomod.client.model.entity.SusanooSkeletonModel.LAYER_LOCATION)),
                new com.sekwah.narutomod.client.model.entity.SusanooClothedModel(event.getEntityModels()
                        .bakeLayer(com.sekwah.narutomod.client.model.entity.SusanooClothedModel.LAYER_LOCATION)),
                new com.sekwah.narutomod.client.model.entity.SusanooWingedModel(event.getEntityModels()
                        .bakeLayer(com.sekwah.narutomod.client.model.entity.SusanooWingedModel.LAYER_LOCATION)));
    }

}
