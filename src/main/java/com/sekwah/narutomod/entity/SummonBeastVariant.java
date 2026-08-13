package com.sekwah.narutomod.entity;

import com.sekwah.narutomod.NarutoMod;
import net.minecraft.resources.ResourceLocation;

/**
 * The four Kuchiyose contracts, each a real named boss summon rather than a tinted copy of
 * one silhouette. Which one answers is decided by the summoner's clan, the way the contracts
 * run in the story: Myoboku's toads to the Uzumaki, Ryuchi Cave's serpents to the Uchiha,
 * Shikkotsu Forest's slugs to the Senju and Haruno, and the Monkey King - the Leaf's own
 * contract, held by the Third - to everyone else.
 *
 * WARNING: the ordinal is what goes into NBT, so new contracts are appended, never inserted.
 * The first three deliberately keep the order the old tinted placeholder used, so summons
 * already standing in a saved world come back as the right species.
 */
public enum SummonBeastVariant {

    /** Chief Toad of Mount Myoboku. The bruiser: huge, tanky, spits water bullets. */
    GAMABUNTA("gamabunta", "Gamabunta", 220f, 22f, 0.30f, 0.85f,
            4.4f, 5.0f, 4.0f, 1.5913f),

    /** The serpent of Ryuchi Cave. Fast, fragile for its size, and venomous. */
    MANDA("manda", "Manda", 170f, 26f, 0.42f, 0.35f,
            2.2f, 1.7f, 4.0f, 1.5250f),

    /** Katsuyu of Shikkotsu Forest. A support summon: she heals, she does not brawl. */
    KATSUYU("katsuyu", "Katsuyu", 260f, 6f, 0.24f, 0.90f,
            1.6f, 4.1f, 1.4f, 2.8906f),

    /** Enma, the Monkey King. Fights with the Adamantine Staff, so he outreaches everything. */
    ENMA("enma", "Enma", 190f, 18f, 0.36f, 0.55f,
            1.6f, 3.0f, 1.2f, 1.8125f);

    private final String name;
    private final String displayName;
    private final float health;
    private final float damage;
    private final float speed;
    private final float knockbackResistance;
    private final float width;
    private final float height;
    /** Multiplier from the imported model's own units to the size drawn in the world. */
    private final float renderScale;
    /**
     * How far below the model's origin its feet sit, in blocks at scale 1. The imported models
     * are authored with +Y downward and none of them put the origin on the ground, so the
     * renderer has to lift by exactly this much or the summon stands buried or floating.
     */
    private final float feetOffset;

    SummonBeastVariant(String name, String displayName, float health, float damage, float speed,
                       float knockbackResistance, float width, float height,
                       float renderScale, float feetOffset) {
        this.name = name;
        this.displayName = displayName;
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

    /** Shown over the summon's head. ASCII only - the shader font pages cannot draw the rest. */
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
        // Manda reuses the legacy purple serpent skin; the rest have their own.
        String file = this == MANDA ? "snake_purple" : this.name;
        return new ResourceLocation(NarutoMod.MOD_ID, "textures/entity/summon/" + file + ".png");
    }

    /** Katsuyu mends rather than fights, so the melee goals stand down for her. */
    public boolean isSupport() {
        return this == KATSUYU;
    }

    /** The contract a clan holds. Everyone without one of their own calls the Monkey King. */
    public static SummonBeastVariant forClan(String clanId) {
        if (clanId == null) {
            return ENMA;
        }
        return switch (clanId) {
            case "uzumaki" -> GAMABUNTA;
            case "uchiha" -> MANDA;
            case "senju", "haruno" -> KATSUYU;
            default -> ENMA;
        };
    }

    public static SummonBeastVariant byId(int ordinal) {
        SummonBeastVariant[] values = values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : GAMABUNTA;
    }
}
