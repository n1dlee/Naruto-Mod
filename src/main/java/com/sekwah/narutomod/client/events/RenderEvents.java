package com.sekwah.narutomod.client.events;

import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.capabilities.NinjaCapabilityHandler;
import com.sekwah.narutomod.client.renderer.SurfaceWalkRenderer;
import com.sekwah.narutomod.client.renderer.BoulderRollRenderer;
import com.sekwah.narutomod.client.renderer.entity.KuramaTailRenderer;
import com.sekwah.narutomod.item.interfaces.IShouldHideNameplate;
import com.sekwah.narutomod.util.BaikaScale;
import com.sekwah.narutomod.util.SusanooFullBody;
import com.sekwah.narutomod.util.SusanooPose;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.RenderNameTagEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = NarutoMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class RenderEvents {

    /**
     * {@link net.minecraftforge.client.event.RenderPlayerEvent}
     *
     * Both giant forms hide the player model. Susanoo renders in the world pass; Kurama
     * still draws here because cancelling Pre also suppresses the Post event.
     */
    @SubscribeEvent
    public static void playerRenderEvent(RenderPlayerEvent.Pre event) {
        event.getEntity().getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(ninjaData -> {
            SurfaceWalkRenderer.orient(event.getEntity(), event.getPoseStack(), event.getPartialTick());
            if (ninjaData.getInvisible()) {
                event.setCanceled(true);
                return;
            }
            if (BoulderRollRenderer.hidesPlayer(event.getEntity())) {
                event.setCanceled(true);
                return;
            }
            if (ninjaData.isSusanooActive() && ninjaData.getSusanooStage() == 4) {
                event.setCanceled(true);
                return;
            }
            if (ninjaData.isSusanooActive() && SusanooFullBody.standing(ninjaData.getSusanooStage())) {
                double settle = SusanooPose.smooth((ninjaData.getSusanooStageAge() + event.getPartialTick()) / 9);
                event.getPoseStack().translate(0, SusanooFullBody.seatShift(event.getEntity().getEyeHeight()) * settle, 0);
                return;
            }
            if (ninjaData.isKuramaCloakActive() && ninjaData.getKuramaTailCount() >= 9) {
                event.setCanceled(true);
                KuramaTailRenderer.renderFullAvatar(event, ninjaData);
                return;
            }
            float scale = BaikaScale.current(event.getEntity());
            if (scale > 1) {
                event.getPoseStack().scale(scale, scale, scale);
            }
        });
    }

    /** Keeps the client eye height aligned with the giant hitbox set by the common size handler. */
    @SubscribeEvent
    public static void onEntitySize(EntityEvent.Size event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        player.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(ninjaData -> {
            float giantEyeHeight = ninjaData.getGiantEyeHeight();
            if (giantEyeHeight > 0) {
                event.setNewEyeHeight(giantEyeHeight);
            }
        });
    }

    @SubscribeEvent
    public static void renderHand(RenderHandEvent event) {
        var client = Minecraft.getInstance();
        var player = client.player;
        if (player != null && client.getCameraEntity() == player && player.getCapability(NinjaCapabilityHandler.NINJA_DATA)
                .map(data -> data.isSusanooActive() && SusanooFullBody.hasLegs(data.getSusanooStage())).orElse(false)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void renderNameplateEvent(RenderNameTagEvent event) {
        if (event.getResult() != Event.Result.DENY) {
            Entity entity = event.getEntity();
            if (entity instanceof Player player) {
                ItemStack itemStack = player.getItemBySlot(EquipmentSlot.HEAD);
                Item item = itemStack.getItem();
                if (item instanceof IShouldHideNameplate hideNameplate && (hideNameplate.shouldHideNameplate(entity))) {
                    event.setResult(Event.Result.DENY);
                }
            }
        }
    }

}
