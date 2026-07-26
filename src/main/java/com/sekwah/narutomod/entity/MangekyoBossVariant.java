package com.sekwah.narutomod.entity;

import com.sekwah.narutomod.NarutoMod;
import net.minecraft.resources.ResourceLocation;

/**
 * The five Mangekyo wielders that roam the world as bosses. One entity type tells them
 * apart by a variant byte (same trick SummonBeastEntity uses for its three contracts);
 * everything that differs between them — health, reach, Susanoo colour, which signature
 * technique they drop — lives here as data instead of five near-identical classes.
 *
 * Susanoo colours follow canon: Itachi's red-orange, Sasuke's violet, Madara's blue,
 * Shisui's green, Obito's blue-violet.
 */
public enum MangekyoBossVariant {

    // --- Uchiha: defeating one upgrades a Mangekyo to Eternal ---
    ITACHI("itachi", 180f, 11f, 0.30f, 0.85f, 0.35f, 0.05f, BossKit.CROWS_AND_FLAME),
    SASUKE("sasuke", 165f, 12f, 0.33f, 0.55f, 0.30f, 0.85f, BossKit.LIGHTNING),
    MADARA("madara", 230f, 15f, 0.31f, 0.15f, 0.35f, 0.95f, BossKit.GUNBAI),
    SHISUI("shisui", 140f, 10f, 0.38f, 0.25f, 0.80f, 0.35f, BossKit.ILLUSION),
    OBITO("obito", 175f, 12f, 0.32f, 0.45f, 0.20f, 0.75f, BossKit.PHASE),

    // --- Missing-nin: no Sharingan, so no Eternal Mangekyo. They drop their own blade
    // instead, which is what actually made them famous.
    KISAME("kisame", 210f, 14f, 0.31f, 0f, 0f, 0f, BossKit.SWORDSMAN),
    ZABUZA("zabuza", 170f, 13f, 0.32f, 0f, 0f, 0f, BossKit.SWORDSMAN),
    HIDAN("hidan", 190f, 12f, 0.33f, 0f, 0f, 0f, BossKit.SWORDSMAN),
    DEIDARA("deidara", 150f, 10f, 0.34f, 0f, 0f, 0f, BossKit.EXPLOSIVE),
    SASORI("sasori", 165f, 11f, 0.30f, 0f, 0f, 0f, BossKit.EXPLOSIVE);

    /** What flavour of ranged attack this wielder leans on. */
    public enum BossKit {
        /** Itachi: black flame at range, crows that blind up close. */
        CROWS_AND_FLAME,
        /** Sasuke: lightning strikes. */
        LIGHTNING,
        /** Madara: cone shockwaves that fling opponents away. */
        GUNBAI,
        /** Shisui: genjutsu that disables rather than damages. */
        ILLUSION,
        /** Obito: phases out of danger and re-emerges behind you. */
        PHASE,
        /** Swordsmen: close the distance and hit brutally hard with the blade. */
        SWORDSMAN,
        /** Deidara/Sasori: ranged detonations and poisoned volleys. */
        EXPLOSIVE
    }

    private final String formId;
    private final float maxHealth;
    private final float attackDamage;
    private final float movementSpeed;
    private final float susanooRed;
    private final float susanooGreen;
    private final float susanooBlue;
    private final BossKit kit;

    MangekyoBossVariant(String formId, float maxHealth, float attackDamage, float movementSpeed,
                        float susanooRed, float susanooGreen, float susanooBlue, BossKit kit) {
        this.formId = formId;
        this.maxHealth = maxHealth;
        this.attackDamage = attackDamage;
        this.movementSpeed = movementSpeed;
        this.susanooRed = susanooRed;
        this.susanooGreen = susanooGreen;
        this.susanooBlue = susanooBlue;
        this.kit = kit;
    }

    public static MangekyoBossVariant byId(byte id) {
        MangekyoBossVariant[] values = values();
        return values[Math.floorMod(id, values.length)];
    }

    /** Resolves a stored mangekyoForm string back to its wielder, or null if unknown. */
    public static MangekyoBossVariant byFormId(String formId) {
        if (formId == null || formId.isEmpty()) {
            return null;
        }
        for (MangekyoBossVariant variant : values()) {
            if (variant.formId.equals(formId)) {
                return variant;
            }
        }
        return null;
    }

    public String formId() {
        return this.formId;
    }

    /**
     * Only the Uchiha carry a Mangekyo worth taking — the missing-nin are a separate,
     * weapon-dropping boss tier that shares the same entity.
     */
    public boolean isUchiha() {
        return this == ITACHI || this == SASUKE || this == MADARA || this == SHISUI || this == OBITO;
    }

    /** Susanoo only manifests for the Uchiha bosses. */
    public boolean hasSusanoo() {
        return this.isUchiha();
    }

    public float maxHealth() {
        return this.maxHealth;
    }

    public float attackDamage() {
        return this.attackDamage;
    }

    public float movementSpeed() {
        return this.movementSpeed;
    }

    public BossKit kit() {
        return this.kit;
    }

    public float susanooRed() {
        return this.susanooRed;
    }

    public float susanooGreen() {
        return this.susanooGreen;
    }

    public float susanooBlue() {
        return this.susanooBlue;
    }

    public ResourceLocation texture() {
        return new ResourceLocation(NarutoMod.MOD_ID, "textures/entity/boss/" + this.formId + ".png");
    }

    /** Translation key for chat/name display, e.g. "mangekyo.form.itachi". */
    public String translationKey() {
        return "mangekyo.form." + this.formId;
    }
}
