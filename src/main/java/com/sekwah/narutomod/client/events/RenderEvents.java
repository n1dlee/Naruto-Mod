package com.sekwah.narutomod.client.events;

import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.capabilities.NinjaCapabilityHandler;
import com.sekwah.narutomod.client.renderer.entity.KuramaTailRenderer;
import com.sekwah.narutomod.client.renderer.entity.SusanooRenderer;
import com.sekwah.narutomod.item.interfaces.IShouldHideNameplate;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
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
     * Complete Body Susanoo / Full Kurama Avatar hide the player entirely and render the
     * giant in its place — done directly here in Pre (not delegated to a separate Post
     * hook) because canceling Pre appears to also suppress Post from firing for this
     * entity, which would otherwise leave nothing rendered at all once the player is hidden.
     */
    private static final net.minecraft.resources.ResourceLocation BAIKA_ABILITY =
            new net.minecraft.resources.ResourceLocation(NarutoMod.MOD_ID, "baika");

    @SubscribeEvent
    public static void playerRenderEvent(RenderPlayerEvent.Pre event) {
        event.getEntity().getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(ninjaData -> {
            // Akimichi Multi-Size: enlarge the whole player render. Scaling here (not in a
            // sub-scope) is intentional — the dispatcher push/pops around the full render,
            // so the scale applies to exactly this entity and nothing else.
            if (ninjaData.getToggleAbilityData().getAbilitiesHashSet().contains(BAIKA_ABILITY)) {
                event.getPoseStack().scale(2.2f, 2.2f, 2.2f);
            }
            if (ninjaData.getInvisible()) {
                event.setCanceled(true);
                return;
            }
            if (ninjaData.isSusanooActive() && ninjaData.getSusanooStage() >= 4) {
                event.setCanceled(true);
                SusanooRenderer.renderFullBody(event, ninjaData);
                return;
            }
            if (ninjaData.isKuramaCloakActive() && ninjaData.getKuramaTailCount() >= 9) {
                event.setCanceled(true);
                KuramaTailRenderer.renderFullAvatar(event, ninjaData);
            }
        });
    }

    /**
     * Bumps the LOCAL camera's eye height while a Complete-Body-scale form is active, so the
     * third-person (and first-person) camera actually looks out from up near the giant's head
     * instead of staying pinned at the normal ~1.62-block player eye height while a 40+ block
     * avatar renders around them (which looked like "sinking into the ground" — you were
     * viewing the giant from down near its ankles). Client-side only, deliberately: this only
     * changes where the CAMERA looks from, not the entity's actual hitbox/interaction origin,
     * so mining/melee/item-use reach checks (which run server-side) are unaffected.
     */
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
