package com.sekwah.narutomod.entity;

import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.sounds.NarutoSounds;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import org.joml.Vector3f;

/**
 * The eight tailed beasts, One through Eight. Kurama is deliberately absent: the Nine Tails
 * already exists in this mod twice over, as the player's own cloak and avatar and as the
 * Naruto boss's escalation, and a third Kurama walking around would undercut both.
 *
 * Sizes come from the imported geometry rather than from taste: every model was measured
 * (tools/model_extents.py) and the scale here is the multiplier that puts it at the target
 * height, with the hitbox matched to what that actually draws. They grow with tail count,
 * from Shukaku at six and a half blocks to Gyuki at nearly eleven.
 *
 * A word on what that means to play against: nothing in vanilla pathfinding copes with a
 * six-block-wide mob. These are siege fights - the beast holds ground, throws techniques and
 * swats what comes close - not chases. That is deliberate, and it is also simply what the
 * hitbox forces.
 *
 * WARNING: the ordinal is written to NBT. Append, never insert.
 */
public enum TailedBeastVariant {

    /** Shukaku, the One Tail. Wind and sand; the tanuki that never sleeps. */
    SHUKAKU("shukaku", "Shukaku", "onetail", 1,
            435f, 20f, 0.28f, 2.4f, 6.6f, 2.20f, 1.5300f,
            new Vector3f(0.85f, 0.75f, 0.45f)),

    /** Matatabi, the Two Tails. The blue-flame cat. */
    MATATABI("matatabi", "Matatabi", "twotails", 2,
            490f, 21f, 0.34f, 3.6f, 7.1f, 1.35f, 2.4613f,
            new Vector3f(0.25f, 0.55f, 1.0f)),

    /** Isobu, the Three Tails. The armoured turtle; slow, and very hard to move. */
    ISOBU("isobu", "Isobu", "threetails", 3,
            560f, 22f, 0.24f, 6.2f, 7.8f, 4.00f, 1.5000f,
            new Vector3f(0.35f, 0.7f, 0.85f)),

    /** Son Goku, the Four Tails. Lava Release: what he hits stays burning. */
    SON_GOKU("son_goku", "Son Goku", "fourtails", 4,
            600f, 26f, 0.30f, 6.0f, 8.4f, 5.20f, 1.5513f,
            new Vector3f(1.0f, 0.35f, 0.1f)),

    /** Kokuo, the Five Tails. Boil Release, and a charge that goes through people. */
    KOKUO("kokuo", "Kokuo", "fivetails", 5,
            640f, 25f, 0.36f, 2.5f, 9.0f, 4.80f, 1.7369f,
            new Vector3f(0.9f, 0.9f, 0.95f)),

    /** Saiken, the Six Tails. Corrosive slime; everything it touches rots. */
    SAIKEN("saiken", "Saiken", "sixtails", 6,
            690f, 24f, 0.26f, 6.0f, 9.6f, 7.80f, 1.5000f,
            new Vector3f(0.75f, 0.95f, 0.55f)),

    /** Chomei, the Seven Tails. The only one that flies, and it never stops moving. */
    CHOMEI("chomei", "Chomei", "seventails", 7,
            660f, 24f, 0.40f, 2.5f, 10.1f, 2.60f, 3.3656f,
            new Vector3f(0.55f, 0.9f, 0.35f)),

    /** Gyuki, the Eight Tails. The ox-octopus, and the largest thing here. */
    GYUKI("gyuki", "Gyuki", "eighttails", 8,
            790f, 30f, 0.29f, 5.0f, 10.7f, 2.80f, 1.7500f,
            new Vector3f(0.35f, 0.35f, 0.55f));

    private final String name;
    private final String displayName;
    private final String textureName;
    private final int tails;
    private final float health;
    private final float damage;
    private final float speed;
    private final float width;
    private final float height;
    private final float renderScale;
    /** Blocks below the model's origin its feet sit, at scale 1. */
    private final float feetOffset;
    /** Chakra colour, used for the Tailed Beast Bomb and this beast's aura particles. */
    private final Vector3f chakraColour;

    TailedBeastVariant(String name, String displayName, String textureName, int tails,
                       float health, float damage, float speed, float width, float height,
                       float renderScale, float feetOffset, Vector3f chakraColour) {
        this.name = name;
        this.displayName = displayName;
        this.textureName = textureName;
        this.tails = tails;
        this.health = health;
        this.damage = damage;
        this.speed = speed;
        this.width = width;
        this.height = height;
        this.renderScale = renderScale;
        this.feetOffset = feetOffset;
        this.chakraColour = chakraColour;
    }

    public String getName() {
        return this.name;
    }

    /** ASCII only - the shader font pages cannot draw macrons. */
    public String getDisplayName() {
        return this.displayName;
    }

    public int getTails() {
        return this.tails;
    }

    public float getHealth() {
        return this.health;
    }

    public float getDamage() {
        return this.damage;
    }

    public float getSpeed() {
        return this.speed;
    }

    public float getWidth() {
        return this.width;
    }

    public float getHeight() {
        return this.height;
    }

    public float getRenderScale() {
        return this.renderScale;
    }

    public float getFeetOffset() {
        return this.feetOffset;
    }

    public Vector3f getChakraColour() {
        return this.chakraColour;
    }

    public ResourceLocation getTexture() {
        return new ResourceLocation(NarutoMod.MOD_ID, "textures/entity/biju/" + this.textureName + ".png");
    }

    /** Chomei is the one that actually flies rather than only jumping well. */
    public boolean isFlyer() {
        return this == CHOMEI;
    }

    /** Isobu and Saiken are at home in water and should not be held back by it. */
    public boolean isAquatic() {
        return this == ISOBU || this == SAIKEN;
    }

    /**
     * The beast's own voice where the legacy mod had one recorded for it. Kokuo and Saiken
     * never got their own, so they draw from the shared roar pool rather than borrowing a
     * cat's or an ox's and sounding wrong.
     */
    public SoundEvent getRoar() {
        return switch (this) {
            case SHUKAKU -> NarutoSounds.BIJU_SHUKAKU.get();
            case MATATABI -> NarutoSounds.BIJU_MATATABI.get();
            case ISOBU -> NarutoSounds.BIJU_ISOBU.get();
            case SON_GOKU -> NarutoSounds.BIJU_SON_GOKU.get();
            case CHOMEI -> NarutoSounds.BIJU_CHOMEI.get();
            case GYUKI -> NarutoSounds.BIJU_GYUKI.get();
            default -> NarutoSounds.BIJU_ROAR.get();
        };
    }

    public static TailedBeastVariant byId(int ordinal) {
        TailedBeastVariant[] values = values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : SHUKAKU;
    }

    /**
     * Which beast the world offers next. Flat, not weighted: there are only eight, each is a
     * set-piece, and making the low-tail ones common would mean grinding Shukaku for hours
     * before ever seeing Gyuki.
     */
    public static TailedBeastVariant random(RandomSource random) {
        return values()[random.nextInt(values().length)];
    }
}
