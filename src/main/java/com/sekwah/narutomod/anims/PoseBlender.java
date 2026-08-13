package com.sekwah.narutomod.anims;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

import java.util.EnumMap;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * The blend layer every player pose now goes through.
 *
 * Before this, {@link PlayerAnimHandler} wrote limb rotations straight onto the model with
 * setRotation, which meant a pose was either fully on or fully off on any given frame. Every
 * jutsu therefore popped into its stance and popped back out one frame later — technically a
 * pose, but it read as a glitch rather than a movement, and it was the single biggest reason
 * casting felt unanimated no matter how many stances were added.
 *
 * Here a pose is a weight in 0..1 instead of a boolean. Each {@link Track} keeps its own
 * weight per entity, eases toward 1 while the state is active and back toward 0 when it ends,
 * and the pose is written as a lerp from whatever vanilla's setupAnim already produced toward
 * the target stance. At weight 0 the player animates exactly as vanilla; at 1 the stance is
 * fully applied; in between the two are genuinely mixed, so the arms travel into position.
 *
 * All state here is client-side and purely cosmetic — nothing is synced, and losing it (on a
 * dimension change, say) costs at most one eased-in pose.
 */
public final class PoseBlender {

    /** One independently blended pose slot. A player can be mid-blend on several at once. */
    public enum Track {
        CAST,
        CHANNEL,
        CHIDORI,
        WALL_CLIMB,
        SPRINT,
        SAGE,
        KURAMA,
        SUSANOO,
        GATES
    }

    /**
     * Below this the pose is close enough to absent that applying it is wasted work — and,
     * more usefully, it lets callers skip a dispatch entirely rather than lerping by 0.001.
     */
    public static final float EPSILON = 0.01f;

    /** [0] = current weight, [1] = the ageInTicks at which it was last advanced. */
    private static final Map<Entity, EnumMap<Track, float[]>> STATE = new WeakHashMap<>();

    private PoseBlender() {
    }

    /**
     * Advances and returns this entity's blend weight for one pose.
     *
     * @param active     whether the underlying state is on right now
     * @param rampTicks  how long a full 0 to 1 transition takes
     * @param ageInTicks the model's ageInTicks, which already carries the partial tick — so
     *                   the blend advances smoothly between server ticks instead of stepping
     */
    public static float weight(Entity entity, Track track, boolean active, float rampTicks, float ageInTicks) {
        EnumMap<Track, float[]> tracks = STATE.computeIfAbsent(entity, e -> new EnumMap<>(Track.class));
        float[] state = tracks.get(track);
        if (state == null) {
            // Adopt the current state on first sight rather than easing up from zero. A player
            // who is already mid-jutsu when they come into view should be in the stance, not
            // caught playing the entry as if they had just cast it.
            state = new float[]{active ? 1f : 0f, ageInTicks};
            tracks.put(track, state);
        }

        // Clamped because ageInTicks jumps when a player leaves and re-enters view, and an
        // unclamped delta would teleport the weight and undo the whole point of blending.
        float delta = Mth.clamp(ageInTicks - state[1], 0f, 5f);
        state[1] = ageInTicks;

        float step = rampTicks <= 0f ? 1f : delta / rampTicks;
        state[0] = approach(state[0], active ? 1f : 0f, step);
        return smoothstep(state[0]);
    }

    private static float approach(float current, float target, float step) {
        float difference = target - current;
        if (Math.abs(difference) <= step) {
            return target;
        }
        return current + Math.copySign(step, difference);
    }

    /** Takes the corners off a linear ramp, so the limb accelerates in and settles out. */
    private static float smoothstep(float t) {
        float clamped = Mth.clamp(t, 0f, 1f);
        return clamped * clamped * (3f - 2f * clamped);
    }

    /** Blends the part from its current (vanilla-animated) rotation toward an absolute target. */
    public static void rotate(ModelPart part, float weight, float xRot, float yRot, float zRot) {
        part.xRot = Mth.lerp(weight, part.xRot, xRot);
        part.yRot = Mth.lerp(weight, part.yRot, yRot);
        part.zRot = Mth.lerp(weight, part.zRot, zRot);
    }

    /** Same, for the part's offset from its default position. */
    public static void position(ModelPart part, float weight, float x, float y, float z) {
        part.x = Mth.lerp(weight, part.x, x);
        part.y = Mth.lerp(weight, part.y, y);
        part.z = Mth.lerp(weight, part.z, z);
    }

    /**
     * Adds a rotation offset scaled by the weight, for poses that nudge a part relative to
     * wherever it already is rather than driving it to a fixed angle.
     */
    public static void addRotation(ModelPart part, float weight, float xRot, float yRot, float zRot) {
        part.xRot += xRot * weight;
        part.yRot += yRot * weight;
        part.zRot += zRot * weight;
    }
}
