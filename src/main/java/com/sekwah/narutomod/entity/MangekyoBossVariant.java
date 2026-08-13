package com.sekwah.narutomod.entity;

import com.sekwah.narutomod.NarutoMod;
import net.minecraft.resources.ResourceLocation;

/**
 * Note on attackDamage: this is the wielder's BARE-HANDED figure. Whatever {@link #weapon()}
 * hands them adds its own attack-damage modifier on top, so the number a player actually
 * feels is base + weapon. The four heaviest weapons (gunbai, Kubikiribocho, Samehada,
 * Kabutowari) are worth 7.5-8.5 on their own, which is why their carriers read low here —
 * left at their pre-weapon values they landed 21-22 per swing against a rank-0 player with
 * 20 health, i.e. a guaranteed one-shot with no window to react to.
 *
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
    ITACHI("itachi", 260f, 11f, 0.30f, 0.85f, 0.35f, 0.05f, 10),
    SASUKE("sasuke", 240f, 12f, 0.33f, 0.55f, 0.30f, 0.85f, 10),
    MADARA("madara", 320f, 11f, 0.31f, 0.15f, 0.35f, 0.95f, 10),
    SHISUI("shisui", 210f, 10f, 0.38f, 0.25f, 0.80f, 0.35f, 10),
    OBITO("obito", 250f, 12f, 0.32f, 0.45f, 0.20f, 0.75f, 10),

    // --- Missing-nin: no Sharingan, so no Eternal Mangekyo. They drop their own blade
    // instead, which is what actually made them famous.
    KISAME("kisame", 290f, 10f, 0.31f, 0f, 0f, 0f, 6),
    ZABUZA("zabuza", 240f, 9f, 0.32f, 0f, 0f, 0f, 6),
    HIDAN("hidan", 270f, 8f, 0.33f, 0f, 0f, 0f, 6),
    DEIDARA("deidara", 220f, 10f, 0.34f, 0f, 0f, 0f, 6),
    SASORI("sasori", 235f, 11f, 0.30f, 0f, 0f, 0f, 6),

    // --- Legends of the other great bloodlines. Appended, never inserted: the variant is
    // stored in NBT as an ordinal, so reordering this enum would turn every saved boss in
    // every existing world into somebody else.
    /** First Hokage. The strongest shinobi who ever lived, and the wood proves it. */
    HASHIRAMA("hashirama", 420f, 13f, 0.29f, 0.25f, 0.75f, 0.35f, 2),
    /** Pain, in Yahiko's body. Rinnegan, and gravity does what he says. */
    NAGATO("nagato", 340f, 12f, 0.28f, 0.55f, 0.40f, 0.75f, 3),
    /** Copy Ninja. A transplanted Sharingan, a Kamui, and a thousand stolen techniques. */
    KAKASHI("kakashi", 280f, 10f, 0.34f, 0.50f, 0.55f, 0.85f, 4),
    /** Kurama's jinchuriki. Clones, spiralling spheres, and a fox behind all of it. */
    NARUTO("naruto", 360f, 12f, 0.35f, 0.95f, 0.55f, 0.10f, 3),
    /** Byakugan and the Gentle Fist. She does not break your armour, she goes through it. */
    HINATA("hinata", 240f, 9f, 0.33f, 0.80f, 0.85f, 0.95f, 4),
    /** Nara tactician. He will not out-hit you; he will make sure you cannot move. */
    SHIKAMARU("shikamaru", 230f, 9f, 0.32f, 0.10f, 0.05f, 0.20f, 4),
    /**
     * Shukaku's jinchuriki. The sand answers before he does - it blocks what he never saw
     * coming - and when the fight turns, the One Tail comes out through it.
     */
    GAARA("gaara", 300f, 10f, 0.29f, 0.85f, 0.72f, 0.38f, 4);

    /** What killing this wielder leaves behind. */
    public enum BossDrop {
        /** Uchiha: their Mangekyo, upgrading the killer's to Eternal. */
        MANGEKYO,
        /** Missing-nin: the blade that made their name. */
        BLADE,
        /** Nagato: the Rinnegan, the rarest prize in the mod. */
        RINNEGAN,
        /** Hinata: a Byakugan, the only route to one for a non-Hyuga. */
        BYAKUGAN,
        /** Kakashi: his transplanted Sharingan - exactly how he got it himself. */
        SHARINGAN,
        /** Hashirama, Naruto, Shikamaru: no eye to take, so a scroll of their art. */
        SCROLL
    }

    /**
     * Kept separate from {@link #isUchiha()} because the roster outgrew that question. The
     * original split was binary - Uchiha hand over a Mangekyo, everyone else drops a sword -
     * and it has no answer at all for a Rinnegan, a Byakugan, or a Senju with neither.
     */
    public BossDrop dropKind() {
        return switch (this) {
            case ITACHI, SASUKE, MADARA, SHISUI, OBITO -> BossDrop.MANGEKYO;
            case KISAME, ZABUZA, HIDAN, DEIDARA, SASORI -> BossDrop.BLADE;
            case NAGATO -> BossDrop.RINNEGAN;
            case HINATA -> BossDrop.BYAKUGAN;
            case KAKASHI -> BossDrop.SHARINGAN;
            case HASHIRAMA, NARUTO, SHIKAMARU, GAARA -> BossDrop.SCROLL;
        };
    }

    private final String formId;
    private final float maxHealth;
    private final float attackDamage;
    private final float movementSpeed;
    private final float susanooRed;
    private final float susanooGreen;
    private final float susanooBlue;
    private final int spawnWeight;

    MangekyoBossVariant(String formId, float maxHealth, float attackDamage, float movementSpeed,
                        float susanooRed, float susanooGreen, float susanooBlue, int spawnWeight) {
        this.formId = formId;
        this.maxHealth = maxHealth;
        this.attackDamage = attackDamage;
        this.movementSpeed = movementSpeed;
        this.susanooRed = susanooRed;
        this.susanooGreen = susanooGreen;
        this.susanooBlue = susanooBlue;
        this.spawnWeight = spawnWeight;
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

    /** Naruto's escalation is the fox, not a shell, but it rides the same stage ladder. */
    public boolean hasKuramaCloak() {
        return this == NARUTO;
    }

    /**
     * Gaara's escalation is Shukaku coming out through the sand - the same ladder again, with
     * the One Tail's own cloak instead of the fox's, and the One Tail itself at the top.
     */
    public boolean hasSandCloak() {
        return this == GAARA;
    }

    /**
     * The sand moves on its own. This is the trait Gaara is defined by, so it is a mechanic
     * rather than a buff: a share of everything aimed at him is stopped before it lands, and
     * the share grows as the fight forces more sand out of the gourd.
     */
    public boolean hasAutomaticDefence() {
        return this == GAARA;
    }

    /**
     * Every wielder escalates now. A boss that fights identically at full health and at five
     * percent has no second act, and the whole appeal of these fights is watching someone
     * reach for the next thing when the first one stops working.
     *
     * What escalating MEANS is per character - see MangekyoBossEntity#onStageEntered.
     */
    public boolean transforms() {
        return true;
    }

    /**
     * Whether stage 4 turns this wielder into an actual giant with a thirteen-block hitbox.
     *
     * Only the two who canonically have one. It has to stay separate from {@link #transforms()}
     * because the hitbox growth, the headroom requirement and the crush aura all hang off it -
     * letting Shikamaru reach stage 4 should sharpen his shadows, not inflate him into a
     * building.
     */
    public boolean hasGiantForm() {
        return this.hasSusanoo() || this.hasKuramaCloak() || this.hasSandCloak();
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

    /**
     * Relative likelihood of this wielder being the one that spawns.
     *
     * Picking uniformly stopped working when the roster grew from ten to sixteen: the five
     * Uchiha went from half of all boss spawns to under a third, which quietly stretched the
     * road to an Eternal Mangekyo by about sixty percent without anyone touching that system.
     * Weighting holds the Uchiha share at fifty percent and lets the legends stay rare -
     * Hashirama rarest of all, which is the point of him.
     */
    public int spawnWeight() {
        return this.spawnWeight;
    }

    /** Total of every weight, for the weighted roll in MangekyoBossEntity#finalizeSpawn. */
    public static int totalSpawnWeight() {
        int total = 0;
        for (MangekyoBossVariant variant : values()) {
            total += variant.spawnWeight;
        }
        return total;
    }

    /** Picks a wielder in proportion to {@link #spawnWeight()}. */
    public static MangekyoBossVariant weightedRandom(net.minecraft.util.RandomSource random) {
        int roll = random.nextInt(totalSpawnWeight());
        for (MangekyoBossVariant variant : values()) {
            roll -= variant.spawnWeight;
            if (roll < 0) {
                return variant;
            }
        }
        return ITACHI;
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

    /**
     * What this wielder walks around holding. Resolved lazily through a switch rather than
     * stored on the enum constant: the item registry is populated long after this enum is
     * class-loaded, so a RegistryObject captured in the constructor would be a null hazard.
     *
     * The held weapon is not cosmetic — a mob's melee damage picks up the item's attribute
     * modifiers, so Zabuza swinging Kubikiribocho genuinely hits harder than Deidara does.
     */
    public net.minecraftforge.registries.RegistryObject<net.minecraft.world.item.Item> weapon() {
        return switch (this) {
            // Itachi, Shisui and Obito all fought with a plain kunai far more often than
            // with anything exotic.
            case ITACHI, SHISUI, OBITO -> com.sekwah.narutomod.item.NarutoItems.KUNAI;
            case SASUKE -> com.sekwah.narutomod.item.NarutoItems.KUSANAGI;
            case MADARA -> com.sekwah.narutomod.item.NarutoItems.GUNBAI;
            case KISAME -> com.sekwah.narutomod.item.NarutoItems.SAMEHADA;
            case ZABUZA -> com.sekwah.narutomod.item.NarutoItems.KUBIKIRIBOCHO;
            // No scythe exists in this mod; Kabutowari is the only two-handed executioner's
            // weapon on the roster and reads correctly at a distance.
            case HIDAN -> com.sekwah.narutomod.item.NarutoItems.KABUTOWARI;
            case DEIDARA -> com.sekwah.narutomod.item.NarutoItems.EXPLOSIVE_KUNAI;
            case SASORI -> com.sekwah.narutomod.item.NarutoItems.SENBON;
            // Kakashi's White Light Chakra Sabre; the chakra blade is its closest match here.
            case KAKASHI -> com.sekwah.narutomod.item.NarutoItems.CHAKRA_BLADE;
            case NARUTO, SHIKAMARU -> com.sekwah.narutomod.item.NarutoItems.KUNAI;
            // Hashirama, Nagato and Hinata all fought empty-handed - the wood, the gravity
            // and the Gentle Fist are the weapon. Giving them a knife would be wrong twice
            // over: wrong for the character, and it would quietly add its damage on top.
            // The gourd is the weapon, and it is never in his hands.
            case HASHIRAMA, NAGATO, HINATA, GAARA -> null;
        };
    }

    public ResourceLocation texture() {
        return new ResourceLocation(NarutoMod.MOD_ID, "textures/entity/boss/" + this.formId + ".png");
    }

    /** Translation key for chat/name display, e.g. "mangekyo.form.itachi". */
    public String translationKey() {
        return "mangekyo.form." + this.formId;
    }
}
