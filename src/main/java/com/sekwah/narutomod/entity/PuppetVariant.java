package com.sekwah.narutomod.entity;

import com.sekwah.narutomod.NarutoMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;

/**
 * Sasori's puppet corps.
 *
 * A puppet master fights through his collection, not with his hands, and until now Sasori
 * fought with senbon and nothing else - which is the one thing the character is not. These
 * are the five the 1.12.2 mod had geometry for, each with the role it has in the story:
 * Hiruko is the shell he hides inside, Karasu the ranged one, Sanshouo the shield, the Third
 * Kazekage his prize, and the Hundred Puppets the army he opens the last act with.
 *
 * WARNING: the ordinal is written to NBT. Append, never insert.
 */
public enum PuppetVariant {

    /** The armoured shell. Slow, heavily plated, and everything it touches is poisoned. */
    HIRUKO("hiruko", "Hiruko", "hiruko", 120f, 13f, 0.26f, 0.85f,
            1.5f, 2.9f, 1.30f, 1.6875f),

    /** Karasu. Comes apart to fire, so it fights at range and dies quickly up close. */
    KARASU("karasu", "Karasu", "karasu", 70f, 9f, 0.34f, 0.25f,
            0.9f, 2.0f, 1.00f, 1.5000f),

    /** Sanshouo, the salamander. A wall: it soaks hits meant for whatever stands behind it. */
    SANSHOUO("sanshouo", "Sanshouo", "sanshouo", 200f, 8f, 0.20f, 1.00f,
            1.7f, 4.6f, 1.00f, 4.1250f),

    /** The Third Kazekage. Iron Sand - the strongest thing in the collection, and he knows it. */
    THIRD_KAZEKAGE("third_kazekage", "Third Kazekage", "puppet_3rdkazekage", 150f, 16f, 0.30f, 0.55f,
            1.0f, 2.4f, 1.00f, 1.5000f),

    /** One of the Hundred. Individually nothing; the point is that there are never one. */
    HUNDRED("hundred", "Puppet", "puppet_hundred1", 45f, 7f, 0.33f, 0.20f,
            1.0f, 2.5f, 1.00f, 1.5000f);

    private final String name;
    private final String displayName;
    private final String textureName;
    private final float health;
    private final float damage;
    private final float speed;
    private final float knockbackResistance;
    private final float width;
    private final float height;
    private final float renderScale;
    /** Blocks below the model's origin its feet sit, at scale 1. */
    private final float feetOffset;

    PuppetVariant(String name, String displayName, String textureName, float health, float damage,
                  float speed, float knockbackResistance, float width, float height,
                  float renderScale, float feetOffset) {
        this.name = name;
        this.displayName = displayName;
        this.textureName = textureName;
        this.health = health;
        this.damage = damage;
        this.speed = speed;
        this.knockbackResistance = knockbackResistance;
        this.width = width;
        this.height = height;
        this.renderScale = renderScale;
        this.feetOffset = feetOffset;
    }

    public String getName() {
        return this.name;
    }

    /** ASCII only - the shader font pages cannot draw the rest. */
    public String getDisplayName() {
        return this.displayName;
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

    public float getKnockbackResistance() {
        return this.knockbackResistance;
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

    public ResourceLocation getTexture() {
        return new ResourceLocation(NarutoMod.MOD_ID, "textures/entity/puppet/" + this.textureName + ".png");
    }

    /** Everything in Sasori's collection is coated. Sanshouo is armour, not a weapon. */
    public boolean isPoisoned() {
        return this != SANSHOUO;
    }

    /** Karasu and the Third Kazekage fight at range; the rest close. */
    public boolean isRanged() {
        return this == KARASU || this == THIRD_KAZEKAGE;
    }

    /** Sanshouo puts itself between an attacker and whatever it is guarding. */
    public boolean isGuardian() {
        return this == SANSHOUO;
    }

    /**
     * How many of this puppet a summon produces. The Hundred are the only ones that arrive
     * in numbers - that is the entire idea behind them.
     */
    public int summonCount() {
        return this == HUNDRED ? 4 : 1;
    }

    public static PuppetVariant byId(int ordinal) {
        PuppetVariant[] values = values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : HIRUKO;
    }

    /**
     * The order Sasori reaches for them as a fight turns, indexed by his escalation stage.
     * Hiruko first because it is what he is already wearing, the Hundred last because that is
     * the point he stops fighting as a person at all.
     */
    public static PuppetVariant forSasoriStage(int stage) {
        return switch (stage) {
            case 1 -> KARASU;
            case 2 -> SANSHOUO;
            case 3 -> THIRD_KAZEKAGE;
            default -> HUNDRED;
        };
    }

    public static PuppetVariant random(RandomSource random) {
        return values()[random.nextInt(values().length)];
    }
}
