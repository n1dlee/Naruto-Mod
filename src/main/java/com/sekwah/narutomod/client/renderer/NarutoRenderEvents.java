package com.sekwah.narutomod.client.renderer;

import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.block.NarutoBlocks;
import com.sekwah.narutomod.client.model.entity.KuramaAvatarModel;
import com.sekwah.narutomod.client.model.entity.KuramaTailModel;
import com.sekwah.narutomod.client.model.entity.SubstitutionLogModel;
import com.sekwah.narutomod.client.model.entity.SusanooModel;
import com.sekwah.narutomod.client.model.entity.WoodGolemModel;
import com.sekwah.narutomod.client.model.item.model.*;
import com.sekwah.narutomod.client.model.jutsu.FireballJutsuModel;
import com.sekwah.narutomod.client.model.jutsu.RasenganJutsuModel;
import com.sekwah.narutomod.client.model.jutsu.WaterBulletModel;
import com.sekwah.narutomod.client.renderer.entity.*;
import com.sekwah.narutomod.client.renderer.entity.jutsuprojectile.FireballJutsuRenderer;
import com.sekwah.narutomod.client.renderer.entity.jutsuprojectile.AmaterasuFlameRenderer;
import com.sekwah.narutomod.client.renderer.entity.jutsuprojectile.GianBeamRenderer;
import com.sekwah.narutomod.client.renderer.entity.jutsuprojectile.IronSandRenderer;
import com.sekwah.narutomod.client.renderer.entity.jutsuprojectile.IceSpearRenderer;
import com.sekwah.narutomod.client.renderer.entity.jutsuprojectile.ClayBombRenderer;
import com.sekwah.narutomod.client.renderer.entity.jutsuprojectile.GustFrontRenderer;
import com.sekwah.narutomod.client.renderer.entity.jutsuprojectile.ShockArcRenderer;
import com.sekwah.narutomod.client.renderer.entity.jutsuprojectile.RasenganRenderer;
import com.sekwah.narutomod.client.renderer.entity.jutsuprojectile.WaterBulletJutsuRenderer;
import com.sekwah.narutomod.client.renderer.entity.jutsuprojectile.WaterShotRenderer;
import com.sekwah.narutomod.entity.NarutoEntities;
import com.sekwah.narutomod.item.NarutoItems;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * Example of new entity render events
 * https://github.com/MinecraftForge/MinecraftForge/blob/1.17.x/src/test/java/net/minecraftforge/debug/client/rendering/EntityRendererEventsTest.java
 */
@Mod.EventBusSubscriber(value=Dist.CLIENT, modid=NarutoMod.MOD_ID, bus= Mod.EventBusSubscriber.Bus.MOD)
public class NarutoRenderEvents {

    public static final BlockEntityWithoutLevelRenderer NARUTO_RENDERER = new NarutoResourceManager();

    @SubscribeEvent
    public static void gunbaiModel(FMLClientSetupEvent event) {
        event.enqueueWork(() -> ItemProperties.register(NarutoItems.GUNBAI.get(),
                new ResourceLocation(NarutoMod.MOD_ID, "gunbai_blocking"),
                (stack, level, actor, seed) -> actor != null && actor.isUsingItem() && actor.getUseItem() == stack ? 1 : 0));
    }

    @SubscribeEvent
    public static void entityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(com.sekwah.narutomod.block.NarutoBlocks.MOKUTON_TIMBER_ENTITY.get(), MokutonTimberRenderer::new);
        event.registerBlockEntityRenderer(NarutoBlocks.ICE_SPIKE_ENTITY.get(), IceSpikeRenderer::new);
        event.registerEntityRenderer(NarutoEntities.KUNAI.get(), KunaiRenderer::new);
        event.registerEntityRenderer(NarutoEntities.EXPLOSIVE_KUNAI.get(), ExplosiveKunaiRenderer::new);
        event.registerEntityRenderer(NarutoEntities.SENBON.get(), SenbonRenderer::new);
        event.registerEntityRenderer(NarutoEntities.HIRAISHIN_KUNAI.get(),
                com.sekwah.narutomod.client.renderer.entity.HiraishinKunaiRenderer::new);
        event.registerEntityRenderer(NarutoEntities.SHURIKEN.get(), ShurikenRenderer::new);
        event.registerEntityRenderer(NarutoEntities.FUMA_SHURIKEN.get(), FumaShurikenRenderer::new);
        event.registerEntityRenderer(NarutoEntities.PAPER_BOMB.get(), PaperBombRenderer::new);

        event.registerEntityRenderer(NarutoEntities.FIREBALL_JUTSU.get(), FireballJutsuRenderer::new);
        event.registerEntityRenderer(NarutoEntities.GREAT_FIREBALL.get(),
                com.sekwah.narutomod.client.renderer.entity.jutsuprojectile.GreatFireballRenderer::new);
        event.registerEntityRenderer(NarutoEntities.FIRE_WALL.get(),
                com.sekwah.narutomod.client.renderer.entity.jutsuprojectile.FireWallRenderer::new);
        event.registerEntityRenderer(NarutoEntities.WATER_BULLET_JUTSU.get(), WaterBulletJutsuRenderer::new);
        event.registerEntityRenderer(NarutoEntities.WATER_SHOT.get(), WaterShotRenderer::new);
        event.registerEntityRenderer(NarutoEntities.IRON_SAND.get(), IronSandRenderer::new);
        event.registerEntityRenderer(NarutoEntities.ICE_SPEAR.get(), IceSpearRenderer::new);
        event.registerEntityRenderer(NarutoEntities.CLAY_CHARGE.get(), ClayBombRenderer::new);
        event.registerEntityRenderer(NarutoEntities.GIAN_BEAM.get(), GianBeamRenderer::new);
        event.registerEntityRenderer(NarutoEntities.GUST_FRONT.get(), GustFrontRenderer::new);
        event.registerEntityRenderer(NarutoEntities.SHOCK_ARC.get(), ShockArcRenderer::new);

        event.registerEntityRenderer(NarutoEntities.SUBSTITUTION_LOG.get(), SubstitutionLogRenderer::new);

        event.registerEntityRenderer(NarutoEntities.EARTH_WALL.get(), NoopRenderer::new);
        event.registerEntityRenderer(NarutoEntities.SHADOW_CLONE.get(), ShadowCloneRenderer::new);
        event.registerEntityRenderer(NarutoEntities.CLAY_CLONE.get(), ShadowCloneRenderer::new);
        event.registerEntityRenderer(NarutoEntities.ANIMAL_RHINO.get(), AnimalPathRhinoRenderer::new);
        event.registerEntityRenderer(NarutoEntities.NINKEN.get(), net.minecraft.client.renderer.entity.WolfRenderer::new);
        event.registerEntityRenderer(NarutoEntities.RASENGAN.get(), RasenganRenderer::new);
        event.registerEntityRenderer(NarutoEntities.AMATERASU_FIRE.get(), AmaterasuFlameRenderer::new);
        event.registerEntityRenderer(NarutoEntities.RASENSHURIKEN.get(),
                com.sekwah.narutomod.client.renderer.entity.RasenshurikenRenderer::new);
        event.registerEntityRenderer(NarutoEntities.KIRIN_STRIKE.get(),
                com.sekwah.narutomod.client.renderer.entity.KirinStrikeRenderer::new);
        event.registerEntityRenderer(NarutoEntities.SUMMON_BEAST.get(), SummonBeastRenderer::new);
        event.registerEntityRenderer(NarutoEntities.ROGUE_NINJA.get(),
                com.sekwah.narutomod.client.renderer.entity.RogueNinjaRenderer::new);
        event.registerEntityRenderer(NarutoEntities.UCHIHA_ROGUE.get(),
                com.sekwah.narutomod.client.renderer.entity.RogueNinjaRenderer::new);
        event.registerEntityRenderer(NarutoEntities.MANGEKYO_BOSS.get(),
                com.sekwah.narutomod.client.renderer.entity.MangekyoBossRenderer::new);
        event.registerEntityRenderer(NarutoEntities.WOOD_GOLEM.get(),
                com.sekwah.narutomod.client.renderer.entity.WoodGolemRenderer::new);
        event.registerEntityRenderer(NarutoEntities.TAILED_BEAST.get(),
                com.sekwah.narutomod.client.renderer.entity.TailedBeastRenderer::new);
        event.registerEntityRenderer(NarutoEntities.TAILED_BEAST_BOMB.get(),
                com.sekwah.narutomod.client.renderer.entity.TailedBeastBombRenderer::new);
        event.registerEntityRenderer(NarutoEntities.PUPPET.get(),
                com.sekwah.narutomod.client.renderer.entity.PuppetRenderer::new);
        event.registerEntityRenderer(NarutoEntities.CHIBAKU_TENSEI.get(),
                com.sekwah.narutomod.client.renderer.entity.ChibakuTenseiRenderer::new);
        event.registerEntityRenderer(NarutoEntities.ICE_MIRROR.get(),
                com.sekwah.narutomod.client.renderer.entity.IceMirrorRenderer::new);
        event.registerEntityRenderer(NarutoEntities.CHAKRA_DRAGON.get(),
                com.sekwah.narutomod.client.renderer.entity.ChakraDragonRenderer::new);
        event.registerEntityRenderer(NarutoEntities.YASAKA_MAGATAMA.get(),
                com.sekwah.narutomod.client.renderer.entity.YasakaMagatamaRenderer::new);

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
        event.registerLayerDefinition(WoodGolemModel.LAYER, WoodGolemModel::createLayer);
        event.registerLayerDefinition(KuramaTailModel.LAYER_LOCATION, KuramaTailModel::createLayer);
        event.registerLayerDefinition(KuramaAvatarModel.LAYER_LOCATION, KuramaAvatarModel::createLayer);
        event.registerLayerDefinition(com.sekwah.narutomod.client.model.entity.KuramaFoxModel.LAYER_LOCATION,
                com.sekwah.narutomod.client.model.entity.KuramaFoxModel::createBodyLayer);
        event.registerLayerDefinition(SusanooModel.LAYER_LOCATION, SusanooModel::createLayer);
        event.registerLayerDefinition(com.sekwah.narutomod.client.model.entity.DragonHeadModel.LAYER_LOCATION,
                com.sekwah.narutomod.client.model.entity.DragonHeadModel::createBodyLayer);
        event.registerLayerDefinition(com.sekwah.narutomod.client.model.entity.BijuCloakModel.LAYER_LOCATION,
                com.sekwah.narutomod.client.model.entity.BijuCloakModel::createBodyLayer);
        event.registerLayerDefinition(com.sekwah.narutomod.client.model.entity.SusanooSkeletonModel.LAYER_LOCATION,
                com.sekwah.narutomod.client.model.entity.SusanooSkeletonModel::createBodyLayer);
        event.registerLayerDefinition(com.sekwah.narutomod.client.model.entity.SusanooClothedModel.LAYER_LOCATION,
                com.sekwah.narutomod.client.model.entity.SusanooClothedModel::createBodyLayer);
        event.registerLayerDefinition(com.sekwah.narutomod.client.model.entity.SusanooWingedModel.LAYER_LOCATION,
                com.sekwah.narutomod.client.model.entity.SusanooWingedModel::createBodyLayer);
        event.registerLayerDefinition(com.sekwah.narutomod.client.model.entity.GiantToadModel.LAYER_LOCATION,
                com.sekwah.narutomod.client.model.entity.GiantToadModel::createBodyLayer);
        event.registerLayerDefinition(com.sekwah.narutomod.client.model.entity.GiantSnakeModel.LAYER_LOCATION,
                com.sekwah.narutomod.client.model.entity.GiantSnakeModel::createBodyLayer);
        event.registerLayerDefinition(com.sekwah.narutomod.client.model.entity.GiantSlugModel.LAYER_LOCATION,
                com.sekwah.narutomod.client.model.entity.GiantSlugModel::createBodyLayer);
        event.registerLayerDefinition(com.sekwah.narutomod.client.model.entity.EnmaModel.LAYER_LOCATION,
                com.sekwah.narutomod.client.model.entity.EnmaModel::createBodyLayer);

        event.registerLayerDefinition(com.sekwah.narutomod.client.model.entity.OneTailModel.LAYER_LOCATION,
                com.sekwah.narutomod.client.model.entity.OneTailModel::createBodyLayer);
        event.registerLayerDefinition(com.sekwah.narutomod.client.model.entity.TwoTailsModel.LAYER_LOCATION,
                com.sekwah.narutomod.client.model.entity.TwoTailsModel::createBodyLayer);
        event.registerLayerDefinition(com.sekwah.narutomod.client.model.entity.ThreeTailsModel.LAYER_LOCATION,
                com.sekwah.narutomod.client.model.entity.ThreeTailsModel::createBodyLayer);
        event.registerLayerDefinition(com.sekwah.narutomod.client.model.entity.FourTailsModel.LAYER_LOCATION,
                com.sekwah.narutomod.client.model.entity.FourTailsModel::createBodyLayer);
        event.registerLayerDefinition(com.sekwah.narutomod.client.model.entity.FiveTailsModel.LAYER_LOCATION,
                com.sekwah.narutomod.client.model.entity.FiveTailsModel::createBodyLayer);
        event.registerLayerDefinition(com.sekwah.narutomod.client.model.entity.SixTailsModel.LAYER_LOCATION,
                com.sekwah.narutomod.client.model.entity.SixTailsModel::createBodyLayer);
        event.registerLayerDefinition(com.sekwah.narutomod.client.model.entity.SevenTailsModel.LAYER_LOCATION,
                com.sekwah.narutomod.client.model.entity.SevenTailsModel::createBodyLayer);
        event.registerLayerDefinition(com.sekwah.narutomod.client.model.entity.EightTailsModel.LAYER_LOCATION,
                com.sekwah.narutomod.client.model.entity.EightTailsModel::createBodyLayer);
        event.registerLayerDefinition(com.sekwah.narutomod.client.model.entity.BijudamaModel.LAYER_LOCATION,
                com.sekwah.narutomod.client.model.entity.BijudamaModel::createBodyLayer);

        event.registerLayerDefinition(com.sekwah.narutomod.client.model.entity.HirukoModel.LAYER_LOCATION,
                com.sekwah.narutomod.client.model.entity.HirukoModel::createBodyLayer);
        event.registerLayerDefinition(com.sekwah.narutomod.client.model.entity.KarasuModel.LAYER_LOCATION,
                com.sekwah.narutomod.client.model.entity.KarasuModel::createBodyLayer);
        event.registerLayerDefinition(com.sekwah.narutomod.client.model.entity.SanshouoModel.LAYER_LOCATION,
                com.sekwah.narutomod.client.model.entity.SanshouoModel::createBodyLayer);
        event.registerLayerDefinition(com.sekwah.narutomod.client.model.entity.ThirdKazekageModel.LAYER_LOCATION,
                com.sekwah.narutomod.client.model.entity.ThirdKazekageModel::createBodyLayer);
        event.registerLayerDefinition(com.sekwah.narutomod.client.model.entity.HundredPuppetModel.LAYER_LOCATION,
                com.sekwah.narutomod.client.model.entity.HundredPuppetModel::createBodyLayer);

        event.registerLayerDefinition(com.sekwah.narutomod.client.model.entity.ChibakuCoreModel.LAYER_LOCATION,
                com.sekwah.narutomod.client.model.entity.ChibakuCoreModel::createBodyLayer);
        event.registerLayerDefinition(com.sekwah.narutomod.client.model.entity.IceMirrorModel.LAYER_LOCATION,
                com.sekwah.narutomod.client.model.entity.IceMirrorModel::createBodyLayer);
    }

    @SubscribeEvent
    public static void entityLayers(EntityRenderersEvent.AddLayers event) {
        KuramaTailRenderer.setFoxModel(new com.sekwah.narutomod.client.model.entity.KuramaFoxModel(
                event.getEntityModels().bakeLayer(com.sekwah.narutomod.client.model.entity.KuramaFoxModel.LAYER_LOCATION)));
        SusanooRenderer.setModel(new SusanooModel(event.getEntityModels().bakeLayer(SusanooModel.LAYER_LOCATION)));
        SusanooRenderer.setBossWingedModel(new com.sekwah.narutomod.client.model.entity.SusanooWingedModel(
                event.getEntityModels().bakeLayer(com.sekwah.narutomod.client.model.entity.SusanooWingedModel.LAYER_LOCATION)));
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

        addLayersToPlayers(event);
    }

    /**
     * Hangs the dojutsu eye layer on every player renderer.
     *
     * There is one renderer per skin type ("default" and "slim"), and a player using the other
     * one would simply never show eyes if only the first were patched - which is the usual way
     * this particular registration goes wrong.
     */
    private static void addLayersToPlayers(EntityRenderersEvent.AddLayers event) {
        for (String skin : event.getSkins()) {
            net.minecraft.client.renderer.entity.player.PlayerRenderer renderer = event.getSkin(skin);
            if (renderer != null) {
                renderer.addLayer(new com.sekwah.narutomod.client.renderer.entity.PlayerEyeLayer(renderer));
                renderer.addLayer(new com.sekwah.narutomod.client.renderer.entity.SageStoneLayer(renderer));
                renderer.addLayer(new com.sekwah.narutomod.client.renderer.entity.GoldenChakraLayer(renderer));
                renderer.addLayer(new com.sekwah.narutomod.client.renderer.RasenganHandRenderer(renderer));
                renderer.addLayer(new LightningArmorRenderer(renderer));
            }
        }
    }

}
