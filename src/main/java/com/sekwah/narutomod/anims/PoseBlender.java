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
        GATES,
        /** The one-off roar as a gate is forced open, distinct from the sustained tremor. */
        GATES_OPEN,
        /** Absorbing a drop. Ninja land in a crouch; they do not thump down flat-footed. */
        LANDING
    }

    /**
     * Below this the pose is close enough to absent that applying it is wasted work — and,
     * more usefully, it lets callers skip a dispatch entirely rather than lerping by 0.001.
     */
    public static final float EPSILON = 0.01f;

    /**
     * [0] = current weight, [1] = the ageInTicks at which it was last advanced,
     * [2] = ticks this track has been continuously active, reset the moment it turns off.
     *
     * Slot 2 is what lets a pose move rather than merely appear. Weight alone only says how
     * far a stance has faded in, so a pose driven by weight is one fixed shape that dissolves
     * in and out - which is exactly why every stance in this mod except the run read as a
     * still image. Elapsed time gives a pose somewhere to travel: a wind-up, a strike and a
     * recovery are three points on this clock, not three separate tracks.
     */
    private static final Map<Entity, EnumMap<Track, float[]>> STATE = new WeakHashMap<>();

    private PoseBlender() {
    }

    /**
     * How long this track has been continuously active, in ticks, carrying the partial tick.
     *
     * Returns 0 for a track that is off or has never run. Poses with a known duration should
     * normalise this into a 0..1 phase and feed it to a {@link Curve}; endless stances (a
     * transformation, a channel that runs until released) can use it raw to drive a breath or
     * an idle sway that never repeats exactly on the frame it started.
     */
    public static float elapsed(Entity entity, Track track) {
        EnumMap<Track, float[]> tracks = STATE.get(entity);
        if (tracks == null) {
            return 0f;
        }
        float[] state = tracks.get(track);
        return state == null ? 0f : state[2];
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
            state = new float[]{active ? 1f : 0f, ageInTicks, 0f};
            tracks.put(track, state);
        }

        // Clamped because ageInTicks jumps when a player leaves and re-enters view, and an
        // unclamped delta would teleport the weight and undo the whole point of blending.
        float delta = Mth.clamp(ageInTicks - state[1], 0f, 5f);
        state[1] = ageInTicks;

        // Reset on the falling edge, not while off, so a pose reading elapsed() during its
        // fade-out still sees where it finished rather than snapping back to its first frame.
        state[2] = active ? state[2] + delta : 0f;

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

    /**
     * A keyframed curve for one limb: stops in phase space, each a time plus an x/y/z triple.
     *
     * A jutsu is a movement, not a position. Fireball is hands gathering at the chest, a snap
     * to the mouth, then a drop; a thrown kunai is a cocked arm and a whip forward. Written as
     * a single target rotation - which is all this file could express before - both of those
     * collapse to their middle frame and the throw becomes a salute. A curve lets the pose be
     * authored the way it actually reads.
     *
     * Stops are given flat, four floats at a time, in ascending time order:
     * <pre>
     *   Curve.of(0.00f, -0.4f, -0.2f, 0f,
     *            0.35f, -2.1f, -0.35f, 0f,
     *            1.00f, -0.2f,  0f,    0f)
     * </pre>
     * Time is normally 0..1 across the pose, but nothing here requires that: a looping stance
     * can hand in seconds and let the curve clamp at both ends.
     */
    public static final class Curve {

        private static final int STRIDE = 4;

        private final float[] stops;

        private Curve(float[] stops) {
            this.stops = stops;
        }

        public static Curve of(float... stops) {
            if (stops.length < STRIDE || stops.length % STRIDE != 0) {
                throw new IllegalArgumentException(
                        "A curve is groups of four (time, x, y, z); got " + stops.length + " floats");
            }
            for (int i = STRIDE; i < stops.length; i += STRIDE) {
                if (stops[i] < stops[i - STRIDE]) {
                    throw new IllegalArgumentException("Curve stops must ascend in time");
                }
            }
            return new Curve(stops);
        }

        /** Blends the part's rotation toward this curve's value at the given phase. */
        public void rotate(ModelPart part, float phase, float weight) {
            sample(phase);
            PoseBlender.rotate(part, weight, sx, sy, sz);
        }

        /** Same, for the part's offset from its default position. */
        public void position(ModelPart part, float phase, float weight) {
            sample(phase);
            PoseBlender.position(part, weight, sx, sy, sz);
        }

        /** Adds this curve's value on top of whatever the part already has. */
        public void addRotation(ModelPart part, float phase, float weight) {
            sample(phase);
            PoseBlender.addRotation(part, weight, sx, sy, sz);
        }

        // Single-axis reads, for poses that drive one number - a lean, a sink, a head tilt -
        // and would otherwise need a whole three-axis curve to carry it.

        public float sampleX(float phase) {
            sample(phase);
            return this.sx;
        }

        public float sampleY(float phase) {
            sample(phase);
            return this.sy;
        }

        public float sampleZ(float phase) {
            sample(phase);
            return this.sz;
        }

        // Sampling writes here rather than allocating a vector per limb per frame. Safe
        // because every caller is the client render thread, one limb at a time.
        private float sx;
        private float sy;
        private float sz;

        private void sample(float phase) {
            int last = this.stops.length - STRIDE;
            if (phase <= this.stops[0]) {
                this.sx = this.stops[1];
                this.sy = this.stops[2];
                this.sz = this.stops[3];
                return;
            }
            if (phase >= this.stops[last]) {
                this.sx = this.stops[last + 1];
                this.sy = this.stops[last + 2];
                this.sz = this.stops[last + 3];
                return;
            }
            for (int i = STRIDE; i <= last; i += STRIDE) {
                float end = this.stops[i];
                if (phase > end) {
                    continue;
                }
                float start = this.stops[i - STRIDE];
                float span = end - start;
                // Two stops at the same instant are a deliberate hard cut, not a divide by
                // zero: Java would hand back Infinity here and throw the limb off the model.
                float t = span <= 0f ? 1f : smoothstep((phase - start) / span);
                this.sx = Mth.lerp(t, this.stops[i - 3], this.stops[i + 1]);
                this.sy = Mth.lerp(t, this.stops[i - 2], this.stops[i + 2]);
                this.sz = Mth.lerp(t, this.stops[i - 1], this.stops[i + 3]);
                return;
            }
        }
    }
}
