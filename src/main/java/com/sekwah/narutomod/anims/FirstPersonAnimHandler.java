package com.sekwah.narutomod.anims;

import com.sekwah.narutomod.abilities.NarutoAbilities;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.capabilities.NinjaCapabilityHandler;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Poses the player's own arm in first person.
 *
 * None of the body stances in {@link PlayerAnimHandler} were ever visible to the person
 * casting. PlayerModel#setupAnim is called for the first-person hand with every argument set
 * to zero, and the mod's model mixin treats that signature as "this is the first-person hand,
 * leave it alone" - so a player in the default camera saw their hands do nothing at all, for
 * every jutsu in the mod. That single early return is most of the reason the mod felt like it
 * had one animation.
 *
 * This runs from a different place: an injection inside PlayerRenderer's hand rendering, after
 * vanilla has zeroed the arm's xRot and immediately before it draws. Whatever is written here
 * is therefore the last word.
 *
 * <h2>Why the poses are scaled down</h2>
 *
 * The first-person arm is drawn through a transform that already has it filling one corner of
 * the screen, pivoting at a shoulder that is somewhere behind the camera. Feeding it the full
 * third-person rotations swings the hand out of frame entirely. Everything here is a fraction
 * of the body pose, tuned so the gesture reads inside the viewport instead of leaving it.
 */
public final class FirstPersonAnimHandler {

    private FirstPersonAnimHandler() {
    }

    /**
     * Which hand PlayerRenderer is drawing right now.
     *
     * renderHand itself cannot tell - the choice is made by its two public callers - so they
     * set this on the way in. A plain static is correct here: this is the render thread, one
     * hand at a time, and the value is consumed within the same call.
     */
    private static boolean renderingRightHand = true;

    public static void setRenderingHand(boolean right) {
        renderingRightHand = right;
    }

    /** How much of the third-person gesture survives into the first-person frame. */
    private static final float SCALE = 0.45f;

    public static void poseHand(AbstractClientPlayer player, ModelPart part) {
        player.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(ninjaData -> {
            if (!ninjaData.isNinjaModeEnabled()) {
                return;
            }
            apply(ninjaData, part, player.tickCount);
        });
    }

    private static void apply(INinjaData ninjaData, ModelPart part, int tickCount) {
        ResourceLocation channelled = ninjaData.getCurrentlyChanneledAbility();

        if (ninjaData.isChidoriActive() && renderingRightHand) {
            // Held out and crackling. The jitter is the same idea as the body pose: a hand
            // holding lightning that sits perfectly still stops looking energised.
            float jitter = Mth.sin(tickCount * 2.7f) * 0.05f + Mth.sin(tickCount * 4.3f) * 0.025f;
            part.xRot += (-0.85f + jitter) * SCALE;
            part.yRot += -0.15f * SCALE;
            return;
        }

        if (ninjaData.isRasenganHeld() && renderingRightHand) {
            // Palm turned up and out, carrying the sphere clear of the body.
            float wobble = Mth.sin(tickCount * 0.55f) * 0.05f;
            part.xRot += (-0.55f + wobble) * SCALE;
            part.yRot += -0.55f * SCALE;
            part.zRot += -0.30f * SCALE;
            return;
        }

        if (channelled != null) {
            boolean fireball = channelled.equals(NarutoAbilities.FIREBALL.getId());
            float breath = Mth.sin(tickCount * (fireball ? 0.3f : 0.18f)) * 0.06f;
            // Fireball brings the hands right up to the mouth, so it leans harder than the
            // generic seal - it is the one channel where the hands should crowd the view.
            float lift = fireball ? -1.5f : -1.05f;
            part.xRot += (lift + breath) * SCALE;
            part.yRot += (renderingRightHand ? -0.40f : 0.40f) * SCALE;
            return;
        }

        if (ninjaData.getCastPoseTicks() > 0) {
            // The seal snap, at the scale the viewport can hold.
            part.xRot += -1.05f * SCALE;
            part.yRot += (renderingRightHand ? -0.30f : 0.30f) * SCALE;
            return;
        }

        int gates = ninjaData.getGatesOpen();
        if (gates >= 5) {
            float shake = Mth.sin(tickCount * 3.0f) * 0.02f * gates;
            part.xRot += shake * SCALE;
            part.zRot += shake * 0.5f * SCALE;
        }
    }
}
