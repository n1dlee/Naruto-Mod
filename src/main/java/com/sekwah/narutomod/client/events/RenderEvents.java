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

    /**
     * Stands a clinging ninja on the surface they are actually clinging to.
     *
     * Wall walking rendered the player bolt upright with their feet in the air, sliding up the
     * bricks like a lift, because nothing ever turned the model - the pose handler bent the
     * limbs into a climb and that was the whole of it. Standing on a ceiling was worse, since
     * there was no way to draw it at all.
     *
     * The rotation goes on before the renderer's own transforms, so the body yaw the renderer
     * applies afterwards turns the player about their new up axis rather than about the
     * world's. Turning on a wall then works exactly like turning on the ground.
     *
     * Pivoted at the feet, and the feet are first moved onto the surface itself.
     *
     * Turning about the middle of the body looked like the obvious choice and buried half the
     * model in the wall: with the body horizontal, the feet swing a full half-height PAST the
     * pivot, and a hitbox is only three tenths of a block wide. The player ended up embedded
     * in the bricks with their legs somewhere inside the next chunk.
     *
     * So the pivot moves to where the feet are supposed to be - against the face - and the
     * body then extends outward from it. That is one number for a wall, where the surface is
     * half a hitbox-width to the side, and a different one for a ceiling, where it is a whole
     * body-height above the entity's own position.
     */
    private static void orientToSurface(RenderPlayerEvent.Pre event,
                                        com.sekwah.narutomod.capabilities.INinjaData ninjaData) {
        if (!ninjaData.isWallWalkAttached()) {
            return;
        }
        net.minecraft.core.Direction surface = ninjaData.getWallWalkDirection();
        if (surface == null) {
            return;
        }
        // Feet are against the surface, so the body's up points back out of it.
        net.minecraft.world.phys.Vec3 up = net.minecraft.world.phys.Vec3
                .atLowerCornerOf(surface.getOpposite().getNormal());
        double dot = up.y;
        if (dot > 0.999D) {
            return; // standing on a floor; nothing to turn
        }
        // Upside down is the degenerate case: world up and body up are antiparallel, so their
        // cross product is zero and normalising it would hand back NaN. Any horizontal axis
        // gives the same half turn, so pick one.
        net.minecraft.world.phys.Vec3 axis = dot < -0.999D
                ? new net.minecraft.world.phys.Vec3(1.0D, 0.0D, 0.0D)
                : new net.minecraft.world.phys.Vec3(0.0D, 1.0D, 0.0D).cross(up).normalize();
        float angle = (float) Math.acos(net.minecraft.util.Mth.clamp(dot, -1.0D, 1.0D));

        // How far the surface is from the entity's own position, along the way into it.
        // Sideways that is half the hitbox width; overhead it is the whole standing height,
        // because the entity's position sits at its feet and the ceiling is what its head is
        // pressed against.
        Player entity = event.getEntity();
        double reach = surface.getAxis().isVertical()
                ? entity.getBbHeight()
                : entity.getBbWidth() * 0.5D;
        net.minecraft.world.phys.Vec3 into = net.minecraft.world.phys.Vec3
                .atLowerCornerOf(surface.getNormal()).scale(reach);

        var poseStack = event.getPoseStack();
        poseStack.translate(into.x, into.y, into.z);
        poseStack.mulPose(new org.joml.Quaternionf().rotateAxis(
                angle, (float) axis.x, (float) axis.y, (float) axis.z));
    }

    @SubscribeEvent
    public static void playerRenderEvent(RenderPlayerEvent.Pre event) {
        event.getEntity().getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(ninjaData -> {
            orientToSurface(event, ninjaData);
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

    /**
     * Banks the camera over so the surface underfoot reads as the floor.
     *
     * The body was already being turned onto the wall, and leaving the view bolt upright while
     * it happened is what made wall walking feel wrong: everything on screen said you were
     * running up a vertical face and the horizon said you were standing in a field. On a
     * ceiling it was worse - upside down with the sky still above you.
     *
     * Roll is the only axis Minecraft's camera actually offers, and it is also the right one:
     * banking is what a person walking round a surface experiences. Facing straight into the
     * wall is genuinely ambiguous - no amount of roll puts a surface you are nose-first
     * against underneath you - and the formula returns zero there, which is the honest answer.
     */
    @SubscribeEvent
    public static void bankCameraOnSurface(net.minecraftforge.client.event.ViewportEvent.ComputeCameraAngles event) {
        if (!(event.getCamera().getEntity() instanceof Player player)) {
            return;
        }
        player.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(ninjaData -> {
            if (!ninjaData.isWallWalkAttached()) {
                return;
            }
            net.minecraft.core.Direction surface = ninjaData.getWallWalkDirection();
            if (surface == null || surface == net.minecraft.core.Direction.DOWN) {
                return;
            }
            net.minecraft.world.phys.Vec3 into = net.minecraft.world.phys.Vec3
                    .atLowerCornerOf(surface.getNormal());

            // Where the surface sits relative to the screen: measured against the camera's own
            // right and the world's down, which together span what "roll" can express.
            float yawRadians = (float) Math.toRadians(event.getYaw());
            net.minecraft.world.phys.Vec3 right = new net.minecraft.world.phys.Vec3(
                    -net.minecraft.util.Mth.cos(yawRadians), 0.0D, -net.minecraft.util.Mth.sin(yawRadians));
            double alongRight = into.dot(right);
            double alongDown = -into.y;

            float roll = (float) Math.toDegrees(Math.atan2(alongRight, alongDown));
            // Eased in over the first few ticks of clinging so attaching does not snap the
            // horizon through ninety degrees in one frame.
            float settle = Math.min(1.0f, ninjaData.getWallWalkTicks() / 12.0f);
            event.setRoll(roll * settle);
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
