package com.sekwah.narutomod.capabilities;

import com.mojang.logging.LogUtils;
import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.abilities.JutsuScrolls;
import com.sekwah.narutomod.abilities.jutsus.KuramaChakraModeAbility;
import com.sekwah.narutomod.abilities.NarutoAbilities;
import com.sekwah.narutomod.capabilities.toggleabilitydata.ToggleAbilityData;
import com.sekwah.narutomod.config.NarutoConfig;
import com.sekwah.narutomod.gameevents.NarutoGameEvents;
import com.sekwah.narutomod.registries.NarutoRegistries;
import com.sekwah.narutomod.util.NarutoParticles;
import com.sekwah.narutomod.util.SurfaceStride;
import com.sekwah.narutomod.util.SurfaceSupport;
import com.sekwah.narutomod.util.DashStride;
import com.sekwah.narutomod.util.ChargeFeedback;
import com.sekwah.sekclib.capabilitysync.capabilitysync.annotation.Sync;
import net.minecraft.ChatFormatting;
import com.sekwah.narutomod.util.HiraishinFormula;
import com.sekwah.narutomod.events.BodyRiteEffects;
import com.sekwah.narutomod.events.BodyRiteLifecycle;
import com.sekwah.narutomod.events.KuramaCloakEvents;
import com.sekwah.narutomod.events.SusanooCombat;
import com.sekwah.narutomod.util.GiantForm;
import com.sekwah.narutomod.util.SpectralSword;
import com.sekwah.narutomod.util.SusanooForm;
import com.sekwah.narutomod.sounds.NarutoSounds;
import com.sekwah.narutomod.util.KuramaCloakMotion;
import com.sekwah.narutomod.util.EightGateCycle;
import com.sekwah.narutomod.util.SageFocus;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import org.joml.Vector3f;
import org.slf4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.UUID;
import java.util.function.Consumer;

public class NinjaData implements INinjaData, ICapabilityProvider {

    private static final Logger LOGGER = LogUtils.getLogger();

    @Sync(minTicks = 1)
    private float chakra;

    @Sync(minTicks = 1)
    private float stamina;

    @Sync(minTicks = 1)
    private float substitutions;

    /**
     * Kurama's own chakra, lent to an Uzumaki jinchuriki — deliberately separate from the
     * player's own {@link #chakra} pool. Kurama Cloak / Kurama Chakra Mode draw from this
     * instead, since Sasuke pays for Susanoo with his own chakra but Kurama gives freely.
     */
    @Sync(minTicks = 1)
    private float kuramaBond = 0f;

    private static final float MAX_KURAMA_BOND = 1_000_000f;
    private static final float KURAMA_BOND_REGEN = 1000f; // ~50s to refill from empty

    /**
     * 0.0-1.0 meter shared by Susanoo and Kurama Cloak stage/size derivation — set directly
     * by scrolling the mouse wheel while a transformation is active (see
     * ServerScrollAdjustPacket), NOT automatic. Stays exactly where left; resets to 0 only
     * when the transformation ends entirely.
     */
    // syncGlobally: this is the scroll-wheel "push" that decides how large a Susanoo or
    // Kurama cloak is drawn. Without it every other client rendered the transformation at
    // its smallest size no matter how far the owner had pushed it.
    @Sync(minTicks = 1, syncGlobally = true)
    private float transformPower = 0f;

    @Sync
    private float maxChakra;

    /**
     * If the player should have access to all the ninja shit
     */
    @Sync(syncGlobally = true)
    private boolean ninjaModeEnabled;

    @Sync
    private float maxStamina;

    // Unless the player needs to know the max for rendering, no point in rendering.
    private float maxSubstitutions;

    @Sync
    private Vec3 substitutionLocation;

    @Sync
    private ResourceLocation substitutionDimension;

    /**
     * If the player can double jump, will be updated by underlying values server side.
     *
     * Will be true if the player has enough chakra as well as
     */
    @Sync(minTicks = 1)
    private DoubleJumpData doubleJumpData;

    private boolean doubleJumpReady;

    /**
     * The current ability being charged/channeled
     * <p>
     * TODO make this global then expand the channeled logic to be able to handle any visual effects easier.
     * TODO possibly swap the type of this over to an Ability type so that it needs to be looked up less.
     */
    // syncGlobally: read by renderers that run for EVERY player in view, not just the owner.
    // Owner-only sync meant the caster saw their own jutsu and everyone else saw idle hands.
    @Sync(minTicks = 1, syncGlobally = true)
    private ResourceLocation currentlyChanneled;
    private final ChargeFeedback chargeFeedback = new ChargeFeedback();
    @Sync(minTicks = 2, syncGlobally = true)
    private int chargeIntensity;

    @Sync(minTicks = 1)
    private int ticksChanneled;

    // syncGlobally: the set of active toggles is what several renderers read to decide
    // whether to draw anything at all - Akimichi's giant scale in RenderEvents, the Kamui
    // and Byakugan phasing particles, chakra flow. Owner-only sync meant none of it was
    // visible to anyone else, so an Akimichi grew to giant size on their own screen only.
    @Sync(syncGlobally = true)
    private ToggleAbilityData toggleAbilityData;

    @Sync(minTicks = 1, syncGlobally = true)
    private int eyeVisualState;


    /**
     * Depending on what's going might want to swap to a potion effect.
     * This will make the player truly invisible.
     */
    @Sync(minTicks = 1, syncGlobally = true)
    private boolean isInvisible = false;

    private int invisibleTicks = 0;


    private ArrayList<DelayedPlayerTickEvent> delayedTickEvents = new ArrayList<>();
    // CooldownTickEvent to follow the same style you have for DelayedPlayerTickEvent
    private HashMap<String, CooldownTickEvent> cooldownTickEvents = new HashMap<>();

    @Sync
    private float chakraXp = 0;
    @Sync(minTicks = 1)
    private boolean trainingLoadoutEnabled;
    private TrainingLoadout trainingLoadout;

    @Sync
    private int ninjaRank = 0; // 0=Academy, 1=Genin, 2=Chunin, 3=Jonin, 4=Kage

    /**
     * Low/Mid/High within the current base rank, 0-2. Always 0 at Academy.
     * syncGlobally so other players' nameplates and the rank HUD read the right grade.
     */
    @Sync(syncGlobally = true)
    private int rankTier = 0;

    /** The step past Kage. Earned by deeds, not by an XP total - see recordMangekyoBossKill. */
    @Sync(syncGlobally = true)
    private boolean sixPathsUnlocked = false;

    /** Progress toward the Six Paths step. */
    @Sync
    private int mangekyoBossKills = 0;

    @Sync
    private String clanId = ""; // empty = not chosen yet

    /**
     * Chakra nature affinity: "", "fire", "water", "wind", "earth" or "lightning".
     * Jutsu whose element matches deal +25% damage (see PlayerEvents.applyNatureAffinity) —
     * canon: a ninja's affinity nature comes out stronger and cheaper than trained ones.
     */
    @Sync
    private String natureAffinity = "";

    /** csv of unlocked element ids, e.g. "fire,wind". First entry doubles as the affinity. */
    @Sync
    private String unlockedElements = "";
    @Sync
    private float elementXpFire = 0;
    @Sync
    private float elementXpWater = 0;
    @Sync
    private float elementXpEarth = 0;
    @Sync
    private float elementXpWind = 0;
    @Sync
    private float elementXpLightning = 0;

    public static final String[] ALL_ELEMENTS = {"fire", "water", "earth", "wind", "lightning"};

    /**
     * Phase 15 C: csv of jutsu paths learned from scrolls (e.g. "rasengan,chidori").
     * Only jutsu listed in JutsuScrolls.SCROLL_JUTSU need learning — everything else
     * (clan kekkei genkai, utility movement) stays innate.
     */
    @Sync
    private String learnedJutsu = "";

    @Sync
    private int sharinganTomoe = 0; // 0-3

    // syncGlobally: PlayerEyeLayer draws this on every player it renders. Owner-only sync
    // meant everyone else's eyes were drawn from YOUR progression - so a bystander with a
    // Mangekyo showed plain Sharingan, and your own transplant put one eye on all of them.
    @Sync(syncGlobally = true)
    private boolean mangekyoAwakened = false;

    /** Primary MS form chosen at awakening: "", "itachi", "sasuke", "madara", "shisui", "obito". */
    // syncGlobally: picks the Susanoo's canon colour, so without it every other player saw
    // the default purple regardless of whose Mangekyo it was.
    @Sync(syncGlobally = true)
    private String mangekyoForm = "";

    /**
     * Chosen Susanoo tint as a packed 0xRRGGBB, or -1 to keep the wielder's canon colour.
     * syncGlobally because it is read by a renderer that draws for every nearby player.
     */
    @Sync(syncGlobally = true)
    private int susanooColor = -1;

    /** Drives escalating blindness on non-EMS Mangekyo casts. */
    @Sync
    private int msUseCounter = 0;

    /**
     * Movement keys held at the moment of the last cast, -1/0/1 on each axis. Transient and
     * deliberately unsynced: it is a one-shot input snapshot the activation packet fills in
     * just before performServer runs, so directional techniques can read WASD. A ServerPlayer
     * never updates its own xxa/zza, so there is nothing on the server side to read otherwise.
     */
    private float moveStrafe = 0f;
    private float moveForward = 0f;

    @Sync(minTicks = 20)
    private int msBlindnessDecayTicks = 0;

    @Sync
    private boolean eternalMangekyoAwakened = false;

    /** csv of boss form ids defeated, e.g. "itachi,madara" — each grants that form's signature jutsu. */
    @Sync
    private String defeatedMsBosses = "";

    @Sync
    private int byakuganLevel = 0; // 0-4

    @Sync(syncGlobally = true)
    private boolean rinneganAwakened = false;

    /** csv of unlocked Six Paths ids: "deva,preta,animal,naraka". */
    @Sync
    private String rinneganPathsUnlocked = "";

    @Sync
    private boolean rinneSharinganAwakened = false;

    /**
     * World day the once-per-day Phoenix Sage death-save was last used, -1 = never.
     * Deliberately NOT @Sync — the sync layer only handles int/float/boolean/String, and
     * the client never reads this anyway (the charge is spent server-side in
     * PlayerEvents.onPhoenixSage). Persistence is handled manually in serializeNBT.
     */
    private long phoenixSageChargeUsedDay = -1L;

    /**
     * UUID of the creature branded with the Flying Thunder God seal, or empty. Not @Sync —
     * only the server resolves it when the jump happens. Persisted manually like the rest.
     */
    private HiraishinFormula.Book hiraishinFormulas = HiraishinFormula.Book.EMPTY;

    /**
     * Where the wielder was standing when they warped into the Kamui dimension, so the
     * return trip puts them back rather than at world spawn. Server-only (nothing on the
     * client needs it) and persisted by hand, because a player who logs out inside the
     * pocket dimension must still have a way home.
     */
    private String kamuiReturnDimension = "";
    private double kamuiReturnX;
    private double kamuiReturnY;
    private double kamuiReturnZ;

    /**
     * A Sharingan taken from an Uchiha corpse and implanted into someone else (Kakashi's
     * situation). The eye is NOT the wielder's own, so it can never be switched off — it
     * burns chakra every second, forever. That permanent tax is the whole trade-off for a
     * non-Uchiha getting the dojutsu at all.
     */
    @Sync(syncGlobally = true)
    private boolean transplantedSharingan = false;

    /**
     * A single jutsu path the Sharingan has read off an enemy and can throw back once.
     * Last technique learned through the Sharingan, for the HUD.
     */
    @Sync
    private String copiedJutsu = "";

    /** Ticks until the eye can read an attack and sidestep it again. */
    private int sharinganDodgeCooldown = 0;

    /**
     * Set for the instant the Mangekyo inflicts its own eye-strain blindness on the wielder.
     * The genjutsu-resistance handler checks this so the Sharingan cannot shrug off its
     * OWN drawback — without it, an active Sharingan would nullify the entire Mangekyo
     * overuse penalty.
     */
    private transient boolean applyingEyeStrain = false;

    public static final float TRANSPLANT_IDLE_DRAIN = 0.6f;   // per second, always on
    private static final int SHARINGAN_DODGE_COOLDOWN = 30;    // 1.5s between dodges
    private static final float SHARINGAN_DODGE_COST = 6.0f;

    private static final int MS_BLINDNESS_DECAY_TICKS = 1200; // 60s of rest per counter step

    @Sync
    private String bountyTargetId = ""; // entity type id, e.g. "minecraft:zombie"
    @Sync
    private int bountyRemaining = 0;
    private float bountyRewardXp = 0f;

    //
    // The five ranks each split into Low/Mid/High (Academy excepted - a student has no
    // grades), plus a final Six Paths step that XP cannot buy. Everything below is indexed
    // by that 0-13 ladder rather than by the 0-4 base rank.
    //
    // The base rank is deliberately still stored and still 0-4: forty-odd places read it to
    // gate element slots, dojutsu awakening, Susanoo stages and tail count, and those must
    // keep advancing in whole ranks. A tier buys statistics, never permissions.
    //
    // Index: 0 Academy | 1-3 Genin | 4-6 Chunin | 7-9 Jonin | 10-12 Kage | 13 Six Paths.

    /** Ladder steps. Base-rank entries keep their historical values so saves don't shift. */
    private static final float[] RANK_XP_THRESHOLDS = {
            0,
            1000, 2000, 3400,
            5000, 8000, 11000,
            15000, 24000, 35000,
            50000, 80000, 120000,
            Float.MAX_VALUE // Six Paths is an achievement, never an XP total
    };

    /** Absolute chakra pool per ladder step, before the clan multiplier. */
    private static final float[] RANK_CHAKRA_POOL = {
            100,
            500, 750, 1100,
            2000, 3000, 4200,
            6000, 8500, 12000,
            18000, 26000, 36000,
            50000
    };

    /** Absolute stamina pool per ladder step. */
    private static final float[] RANK_STAMINA_POOL = {
            100,
            150, 190, 240,
            300, 380, 480,
            600, 750, 950,
            1200, 1500, 1900,
            2400
    };

    /** Refill time in seconds by rank and tier; the tick rate scales with pool size. */
    private static final float[] RANK_REFILL_SECONDS = {
            100,
            104, 109, 113,
            118, 122, 127,
            131, 136, 140,
            145, 149, 154,
            160
    };

    /** Seconds of Chakra Charge channelling to fill the pool. Higher rank channels faster. */
    private static final float[] RANK_CHARGE_SECONDS = {
            15.0f,
            14.5f, 14.0f, 13.5f,
            13.0f, 12.5f, 12.0f,
            11.5f, 11.0f, 10.5f,
            10.0f, 9.0f, 8.5f,
            8.0f
    };

    /** Outgoing jutsu and melee scaling per ladder step. */
    private static final float[] RANK_DAMAGE_MULTIPLIER = {
            0.5f,
            0.8f, 0.87f, 0.94f,
            1.0f, 1.15f, 1.3f,
            1.5f, 1.8f, 2.1f,
            2.5f, 2.9f, 3.4f,
            4.2f
    };

    /** The Academy regen rate the config value is calibrated against. */
    private static final float BASE_REGEN_REFERENCE = 0.05f;

    /** Mangekyo bosses that have to fall before the Six Paths step opens. */
    private static final int SIX_PATHS_BOSS_KILLS = 10;
    private static final ResourceLocation SHARINGAN_ABILITY = new ResourceLocation("narutomod", "sharingan");
    private static final ResourceLocation BYAKUGAN_ABILITY = new ResourceLocation("narutomod", "byakugan");
    private static final ResourceLocation CHAKRA_DASH_ABILITY = new ResourceLocation("narutomod", "chakra_dash");
    private final DashStride dashStride = new DashStride();
    private static final UUID NINJA_SPEED_UUID = UUID.fromString("a3f2c0e1-7b4d-4e8f-9c1a-5d6e7f8a9b0c");
    private static final UUID SHARINGAN_READ_ATTACK = UUID.fromString("04894f30-bb5f-4c6b-83db-ad4a294d816d");
    private static final UUID BYAKUGAN_FOCUS_ATTACK = UUID.fromString("a3b62d81-0d6e-4f0c-b48d-058070a131f0");
    /**
     * Byakugan sight radius by eye level.
     *
     * Capped at 600 on purpose rather than reaching for a bigger number: the radar is drawn
     * client-side from entities the client actually knows about, and the client is only
     * told about entities inside its own loaded chunks. Past roughly 512 blocks (a 32-chunk
     * render distance) there is simply nothing more to report, so a larger figure would be
     * a number in a tooltip rather than extra vision.
     */
    public static final float CHIDORI_TICK_COST = 0.75F;
    private static final DustParticleOptions CHIDORI_PARTICLE = new DustParticleOptions(new Vector3f(0.45F, 0.85F, 1.0F), 1.0F);

    /** syncGlobally: drives the Chidori thrust stance, which onlookers need to see. */
    @Sync(minTicks = 1, syncGlobally = true)
    private int chidoriTicks = 0;

    // Global, not owner-only: the renderer turns a clinging player onto the face they are
    // standing on, and it has to do that for everybody else's ninja too. Owner-only sync left
    // every other player upright and sliding along the wall.
    @Sync(minTicks = 1, syncGlobally = true)
    private int wallWalkDirection = -1;

    @Sync(minTicks = 1)
    private int wallWalkTicks = 0;

    /** syncGlobally: drives the wall-climb stance, which onlookers need to see. */
    @Sync(minTicks = 1, syncGlobally = true)
    private boolean wallWalkAttached = false;

    @Sync(minTicks = 1)
    private int wallWalkDetachTicks = 0;
    private final SurfaceStride surfaceStride = new SurfaceStride();
    private int wallWalkTurnTicks;
    private Direction wallWalkTurnFrom;

    public NinjaData(boolean isServer) {
        if (isServer) {
            this.getConfigData();
            this.stamina = this.maxStamina;
            this.chakra = this.maxChakra;
        }
        this.toggleAbilityData = new ToggleAbilityData();
        this.doubleJumpData = new DoubleJumpData(false);
    }

    static class RegenInfo {
        public int cooldown;

        public RegenInfo() {
        }

        /**
         * Tick down when checked
         *
         * @return if regen should take place
         */
        public boolean canRegen() {
            if (this.cooldown > 0) {
                this.cooldown--;
                return false;
            }
            return true;
        }
    }

    private final RegenInfo chakraRegenInfo = new RegenInfo();
    private final RegenInfo staminaRegenInfo = new RegenInfo();

    private static final String CHAKRA_TAG = "chakra";
    private static final String STAMINA_TAG = "stamina";
    private static final String NINJA_MODE_ENABLED = "ninjaModeEnabled";
    private static final String SAVE_TIME = "save_time";
    private static final String COOLDOWN_TAG = "cooldowns";
    private static final String SUBSTITUTION_TAG = "substitutions";

    private final LazyOptional<INinjaData> holder = LazyOptional.of(() -> this);

    @Override
    public float getChakra() {
        return this.chakra;
    }

    @Override
    public float getMaxChakra() {
        return this.maxChakra;
    }

    @Override
    public float getStamina() {
        return this.stamina;
    }

    @Override
    public float getSubstitutionCount() {
        return this.substitutions;
    }

    @Override
    public float getMaxStamina() {
        return this.maxStamina;
    }

    @Override
    public void setChakra(float chakra) {
        this.chakra = chakra;
    }

    @Override
    public void setStamina(float stamina) {
        this.stamina = stamina;
    }

    @Override
    public void useChakra(float amount, int cooldown) {
        this.chakra -= amount;
        this.chakraRegenInfo.cooldown = Math.max(cooldown, this.chakraRegenInfo.cooldown);
        // Phase 15 C: chakra spend no longer drips rank XP — spamming jutsu into the air
        // was a free ride to Kage. XP now comes from landing hits, kills, bounties and a
        // small per-cast reward (see PlayerEvents + Ability.grantCastXp).
    }

    @Override
    public void useStamina(float amount, int cooldown) {
        this.stamina -= amount;
        this.staminaRegenInfo.cooldown = Math.max(cooldown, this.staminaRegenInfo.cooldown);
    }

    @Override
    public void useSubstitution(float amount) {
        this.substitutions -= amount;
    }

    @Override
    public void addChakra(float amount) {
        this.chakra = Math.min(Math.max(this.chakra + amount, 0), maxChakra);
    }

    @Override
    public void addStamina(float amount) {
        this.stamina = Math.min(Math.max(this.stamina + amount, 0), maxStamina);
    }

    @Override
    public float getKuramaBond() {
        return this.kuramaBond;
    }

    @Override
    public float getMaxKuramaBond() {
        return MAX_KURAMA_BOND;
    }

    @Override
    public void useKuramaBond(float amount) {
        this.kuramaBond = Math.max(this.kuramaBond - amount, 0f);
    }

    @Override
    public void addKuramaBond(float amount) {
        this.kuramaBond = Math.min(Math.max(this.kuramaBond + amount, 0), MAX_KURAMA_BOND);
    }

    @Override
    public float getTransformPower() {
        return this.transformPower;
    }

    @Override
    public void adjustTransformPower(float delta) {
        // Second barrier behind the packet's own validation, because this value feeds the
        // chakra and Kurama-bond drains and then gets saved. Math.min/Math.max PROPAGATE NaN
        // rather than clamping it, so the obvious-looking clamp below is not a guard at all -
        // one non-finite delta poisons the field permanently and every later "can I afford
        // this" comparison against it quietly answers no.
        if (!Float.isFinite(delta)) {
            return;
        }
        float updated = Math.min(Math.max(this.transformPower + delta, 0f), 1f);
        this.transformPower = Float.isFinite(updated) ? updated : 0f;
    }

    private void updateTransformPower() {
        boolean transformed = this.susanooActive || this.kuramaCloakActive;
        if (!transformed) {
            this.transformPower = 0f;
        }
    }

    /**
     * Ticks remaining on the universal "hand seal" cast-flash pose, set whenever an INSTANT
     * jutsu successfully fires (see ServerAbilityActivatePacket). Purely cosmetic — gives
     * every jutsu a brief visible cast animation instead of firing with no tell at all.
     */
    // syncGlobally: a pose is something OTHER people look at. Without it these three reach
    // only the casting player, so in multiplayer everyone else saw jutsu fire out of a
    // completely idle body - the same class of bug that once made Susanoo invisible to
    // everyone but its owner.
    @Sync(minTicks = 1, syncGlobally = true)
    private int castPoseTicks = 0;

    /** True when the cast pose uses the Shadow Clone cross seal. */
    @Sync(minTicks = 1, syncGlobally = true)
    private boolean crossSealPose = false;

    /**
     * Which ability last successfully triggered castPoseTicks — lets PlayerAnimHandler pick a
     * bespoke pose for specific INSTANT jutsu instead of always falling back to the generic
     * hand-seal flash (see ServerAbilityActivatePacket).
     */
    @Sync(minTicks = 1, syncGlobally = true)
    private ResourceLocation lastCastAbilityId;

    @Override
    public int getCastPoseTicks() {
        return this.castPoseTicks;
    }

    @Override
    public void setCastPoseTicks(int ticks) {
        this.castPoseTicks = Math.max(ticks, 0);
    }

    @Override
    public boolean isCrossSealPose() {
        return this.crossSealPose;
    }

    @Override
    public void setCrossSealPose(boolean crossSeal) {
        this.crossSealPose = crossSeal;
    }

    @Override
    public ResourceLocation getLastCastAbilityId() {
        return this.lastCastAbilityId;
    }

    @Override
    public void setLastCastAbilityId(ResourceLocation id) {
        this.lastCastAbilityId = id;
    }

    /**
     * Which summoning contract this ninja has signed, or empty for none yet.
     *
     * A contract is an agreement a ninja makes personally - Jiraiya signed with the toads,
     * Orochimaru with the snakes, Tsunade with the slugs - and it was being decided by which
     * clan you picked at character creation. That put Sakura's slug contract on everyone
     * called Haruno and handed the snakes to every Uchiha, neither of which is how any of it
     * works.
     *
     * Empty falls back to the clan's traditional partner, so nobody who already had a summon
     * loses it; signing overrides that permanently.
     */
    @Sync
    private String summonContract = "";

    @Override
    public String getSummonContract() {
        return this.summonContract;
    }

    @Override
    public void setSummonContract(String contract) {
        this.summonContract = contract != null ? contract : "";
    }

    // syncGlobally: read by renderers that run for EVERY player in view, not just the owner.
    // Owner-only sync meant the caster saw their own jutsu and everyone else saw idle hands.
    @Sync(minTicks = 1, syncGlobally = true)
    private boolean rasenganHeld = false;

    @Sync(minTicks = 1, syncGlobally = true)
    private int rasenganCharge = 20;

    /**
     * Set when the sphere is spent, cleared when the toggle actually ends.
     *
     * Clearing {@link #rasenganHeld} on impact was not enough to put the Rasengan away: it is
     * a TOGGLE, and the toggle loop calls handleCost and then performServer every single tick,
     * so the frame after a hit the ability simply formed a new sphere. The technique was
     * effectively permanent once switched on. This flag is what handleCost reads to refuse the
     * next tick, which is how a toggle is ended from the inside.
     */
    private boolean rasenganConsumed = false;

    @Override
    public boolean isRasenganConsumed() {
        return this.rasenganConsumed;
    }

    @Override
    public void setRasenganConsumed(boolean consumed) {
        this.rasenganConsumed = consumed;
    }

    @Override
    public boolean isRasenganHeld() {
        return this.rasenganHeld;
    }

    @Override
    public void setRasenganHeld(boolean held) {
        this.rasenganHeld = held;
        if (!held) {
            this.rasenganCharge = 20;
        }
    }

    @Override
    public boolean consumeHeldRasengan() {
        if (!this.rasenganHeld) {
            return false;
        }
        this.setRasenganHeld(false);
        this.rasenganConsumed = true;
        return true;
    }

    @Override
    public int getRasenganCharge() {
        return this.rasenganCharge;
    }

    @Override
    public void adjustRasenganCharge(int delta) {
        this.rasenganCharge = Math.min(Math.max(this.rasenganCharge + delta, 20), 60);
    }


    @Override
    public float getChakraXp() {
        return this.trainingLoadoutEnabled
                ? Math.max(this.chakraXp, RANK_XP_THRESHOLDS[TrainingLoadout.HIGH_KAGE]) : this.chakraXp;
    }

    @Override
    public void addChakraXp(float amount) {
        if (this.trainingLoadoutEnabled) {
            return;
        }
        this.chakraXp += amount;
        this.advanceLadder();
        this.checkRankElementPerks();
    }

    @Override
    public boolean hasTrainingLoadout() {
        return this.trainingLoadoutEnabled;
    }

    @Override
    public void enableTrainingLoadout() {
        if (this.trainingLoadout == null) {
            float current = this.chakra;
            this.trainingLoadout = new TrainingLoadout(Float.isFinite(current) ? Math.max(0, current) : 0);
        }
        this.trainingLoadoutEnabled = true;
        this.getConfigData();
        this.chakra = this.maxChakra;
    }

    @Override
    public boolean disableTrainingLoadout() {
        if (this.trainingLoadout == null) {
            return false;
        }
        float previous = this.trainingLoadout.previousChakra();
        this.trainingLoadout = null;
        this.trainingLoadoutEnabled = false;
        this.getConfigData();
        this.chakra = Math.min(previous, this.maxChakra);
        this.stamina = Math.min(this.stamina, this.maxStamina);
        return true;
    }

    /**
     * Walks the 0-13 ladder up to wherever the current XP total reaches, then writes the
     * base rank and tier back out. Never demotes - losing a rank to a stat edit or a
     * threshold retune would be far worse than the alternative.
     */
    private void advanceLadder() {
        for (int index = RANK_XP_THRESHOLDS.length - 1; index >= 0; index--) {
            if (this.chakraXp >= RANK_XP_THRESHOLDS[index]) {
                if (index > this.getPermanentRankIndex()) {
                    this.applyRankIndex(index);
                    this.getConfigData();
                }
                this.checkSixPaths();
                return;
            }
        }
    }

    /** Splits a ladder index back into the base rank and its tier. */
    private void applyRankIndex(int index) {
        int clamped = Math.min(Math.max(index, 0), RANK_XP_THRESHOLDS.length - 1);
        if (clamped >= 13) {
            this.ninjaRank = 4;
            this.rankTier = 2;
            return;
        }
        if (clamped == 0) {
            this.ninjaRank = 0;
            this.rankTier = 0;
            return;
        }
        this.ninjaRank = 1 + (clamped - 1) / 3;
        this.rankTier = (clamped - 1) % 3;
    }

    /**
     * Position on the full ladder: 0 Academy, 1-3 Genin Low/Mid/High, 4-6 Chunin, 7-9 Jonin,
     * 10-12 Kage, 13 Six Paths. Every stat table in this class is indexed by this.
     */
    @Override
    public int getRankIndex() {
        int permanent = this.getPermanentRankIndex();
        return this.trainingLoadoutEnabled ? Math.max(TrainingLoadout.HIGH_KAGE, permanent) : permanent;
    }

    private int getPermanentRankIndex() {
        if (this.sixPathsUnlocked) {
            return 13;
        }
        if (this.ninjaRank <= 0) {
            return 0;
        }
        return 1 + (this.ninjaRank - 1) * 3 + Math.min(Math.max(this.rankTier, 0), 2);
    }

    @Override
    public int getRankTier() {
        return this.trainingLoadoutEnabled ? 2 : this.ninjaRank <= 0 ? 0 : Math.min(Math.max(this.rankTier, 0), 2);
    }

    @Override
    public boolean isSixPaths() {
        return this.sixPathsUnlocked;
    }

    @Override
    public int getMangekyoBossKills() {
        return this.mangekyoBossKills;
    }

    /**
     * Records a felled Mangekyo boss and opens the Six Paths step once a High Kage has put
     * down enough of them. Deliberately not an XP threshold: the last step of the ladder
     * should be something you did, not something you accumulated.
     *
     * @return true if this kill was the one that opened Six Paths
     */
    @Override
    public boolean recordMangekyoBossKill() {
        this.mangekyoBossKills++;
        return this.checkSixPaths();
    }

    /**
     * Opens the Six Paths step once both halves of the requirement are met.
     *
     * Called from the kill counter and from rank advancement, because the two can be
     * satisfied in either order: a player may well have felled ten wielders on the long climb
     * to High Kage, and checking only on kill would leave them waiting for an eleventh.
     */
    private boolean checkSixPaths() {
        if (this.sixPathsUnlocked
                || this.mangekyoBossKills < SIX_PATHS_BOSS_KILLS
                || this.getPermanentRankIndex() < TrainingLoadout.HIGH_KAGE) {
            return false;
        }
        this.sixPathsUnlocked = true;
        this.getConfigData();
        return true;
    }

    @Override
    public int getNinjaRank() {
        return this.trainingLoadoutEnabled ? 4 : this.ninjaRank;
    }

    @Override
    public void setNinjaRank(int rank) {
        this.ninjaRank = Math.min(Math.max(rank, 0), 4);
        this.rankTier = 0;
        float floor = RANK_XP_THRESHOLDS[this.getPermanentRankIndex()];
        if (this.chakraXp < floor) {
            this.chakraXp = floor;
        }
        this.checkRankElementPerks();
        this.getConfigData();
    }

    /** Command-side tier set, for /ninja. Tiers above Academy only. */
    @Override
    public void setRankTier(int tier) {
        this.rankTier = Math.min(Math.max(tier, 0), 2);
        float floor = RANK_XP_THRESHOLDS[this.getPermanentRankIndex()];
        if (this.chakraXp < floor) {
            this.chakraXp = floor;
        }
        this.getConfigData();
    }

    @Override
    public String getClanId() {
        return this.clanId;
    }

    @Override
    public void setClanId(String clanId) {
        this.clanId = clanId != null ? clanId : "";
        this.applyClanDefaultElements();
    }

    @Override
    public String getNatureAffinity() {
        return this.natureAffinity;
    }

    @Override
    public void setNatureAffinity(String nature) {
        this.natureAffinity = nature != null ? nature : "";
    }


    /**
     * Canon-flavored default affinities: picking a clan immediately awakens its
     * signature nature, so every ninja starts with one elemental branch open.
     * Lightning is deliberately nobody's starter — it's the prestige element,
     * earned via chakra paper (or automatically by Uchiha at Jonin, like Sasuke).
     * Senju awaken BOTH Earth and Water at once — the two natures behind Mokuton.
     */
    private void applyClanDefaultElements() {
        if (this.clanId.isEmpty() || !this.unlockedElements.isEmpty()) {
            return;
        }
        switch (this.clanId) {
            case "uchiha", "aburame" -> this.forceUnlockElement("fire");
            case "uzumaki" -> this.forceUnlockElement("wind");
            case "hyuga", "haruno", "yamanaka" -> this.forceUnlockElement("water");
            case "nara", "akimichi", "inuzuka" -> this.forceUnlockElement("earth");
            case "senju" -> {
                this.forceUnlockElement("earth");
                this.forceUnlockElement("water");
            }
            default -> { }
        }
        this.grantNatureStarterJutsu();
    }

    /**
     * Rank-based element perks, checked on every XP gain / rank change:
     * Uchiha awaken Lightning as their second nature at Jonin (canon: Sasuke's
     * Fire + Lightning pairing).
     */
    private void checkRankElementPerks() {
        if (this.ninjaRank >= 3 && "uchiha".equals(this.clanId) && !this.knowsElement("lightning")) {
            this.forceUnlockElement("lightning");
        }
        this.grantNatureStarterJutsu();
        this.checkDojutsuPerks();
    }

    /** The entry technique of each nature, which awakening that nature teaches you outright. */
    private static final java.util.Map<String, String> NATURE_STARTER_JUTSU = java.util.Map.of(
            "fire", "fireball",
            "water", "water_bullet",
            "earth", "earth_wall",
            "wind", "great_breakthrough",
            "lightning", "false_darkness");

    /**
     * Awakening a nature teaches its most basic technique for free - an Uchiha does not need
     * to find a scroll to learn how to breathe fire, and a wind-natured Uzumaki does not need
     * one for Great Breakthrough. Only the entry jutsu of each element is covered; everything
     * deeper still costs a scroll, and the mastery-level gate still applies on top, so a
     * freshly awakened nature cannot skip straight to its advanced techniques.
     *
     * Runs on every XP gain rather than only at clan selection so that saves created before
     * this existed pick their starters up too - learnJutsu already ignores duplicates.
     */
    private void grantNatureStarterJutsu() {
        if (this.unlockedElements.isEmpty()) {
            return;
        }
        for (String element : this.unlockedElements.split(",")) {
            String starter = NATURE_STARTER_JUTSU.get(element);
            if (starter != null) {
                this.learnJutsu(starter);
            }
        }
    }

    /**
     * Rank advancement grows the clan dojutsu. Uchiha gain a tomoe per rank
     * (Genin=1, Chunin=2, Jonin=3) and awaken the Mangekyo at Kage; Hyuga's Byakugan
     * range level tracks rank the same way. Levels only ever grow — never regress.
     */
    private void checkDojutsuPerks() {
        if ("uchiha".equals(this.clanId)) {
            int targetTomoe = Math.min(Math.max(this.ninjaRank, 0), 3);
            if (targetTomoe > this.sharinganTomoe) {
                this.sharinganTomoe = targetTomoe;
            }
            if (this.ninjaRank >= 4 && !this.mangekyoAwakened) {
                this.mangekyoAwakened = true;
            }
        }
        if ("hyuga".equals(this.clanId)) {
            int targetLevel = Math.min(Math.max(this.ninjaRank, 0), 4);
            if (targetLevel > this.byakuganLevel) {
                this.byakuganLevel = targetLevel;
            }
        }
        // Rinne-Sharingan auto-awakens at the summit: Rinnegan + EMS + 3 of 5 bosses defeated
        if (!this.rinneSharinganAwakened && this.rinneganAwakened
                && this.eternalMangekyoAwakened && this.getDefeatedMsBossCount() >= 3) {
            this.rinneSharinganAwakened = true;
        }
    }

    @Override
    public boolean isElementUnlocked(String element) {
        if (element == null || element.isEmpty()) {
            return false;
        }
        return this.trainingLoadoutEnabled && TrainingLoadout.NATURES.contains(element) || this.knowsElement(element);
    }

    private boolean knowsElement(String element) {
        for (String unlocked : this.unlockedElements.split(",")) {
            if (unlocked.equals(element)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public java.util.List<String> getUnlockedElements() {
        if (this.trainingLoadoutEnabled) {
            return TrainingLoadout.NATURES;
        }
        java.util.List<String> result = new java.util.ArrayList<>();
        for (String unlocked : this.unlockedElements.split(",")) {
            if (!unlocked.isEmpty()) {
                result.add(unlocked);
            }
        }
        return result;
    }

    /** Ceiling for an ordinary Sharingan bearer. */
    public static final int SHARINGAN_MAX_ELEMENT_SLOTS = 4;
    /** The fifth nature slot requires both Kage rank and an Eternal Mangekyo. */
    public static final int ETERNAL_MAX_ELEMENT_SLOTS = 5;

    /**
     * Total element slots this ninja can have open: 1 base, 2 from Jonin, 3 from Kage.
     * Clan defaults may exceed this (Senju start with two) — the cap only limits
     * chakra-paper unlocks.
     *
     * A Sharingan bearer reads the chakra shape of a technique as it is performed, which
     * is exactly how Kakashi ended up with more natures than anyone trained him in. Two
     * tomoe is where the eye starts resolving nature transformation (+1 slot), three is a
     * full copy wheel (+2), hard-capped at {@link #SHARINGAN_MAX_ELEMENT_SLOTS}.
     */
    @Override
    public int getMaxElementSlots() {
        if (this.trainingLoadoutEnabled) {
            return TrainingLoadout.NATURES.size();
        }
        int slots = this.ninjaRank >= 4 ? 3 : (this.ninjaRank >= 3 ? 2 : 1);
        if (this.hasSharinganEye()) {
            slots += this.getSharinganElementSlotBonus();
            slots = Math.min(slots, this.isEternalMangekyoAwakened()
                    ? ETERNAL_MAX_ELEMENT_SLOTS : SHARINGAN_MAX_ELEMENT_SLOTS);
        }
        return slots;
    }

    /** Extra nature slots the eye itself grants. 0 below two tomoe. */
    @Override
    public int getSharinganElementSlotBonus() {
        if (!this.hasSharinganEye()) {
            return 0;
        }
        if (this.mangekyoAwakened || this.sharinganTomoe >= 3) {
            return 2;
        }
        return this.sharinganTomoe >= 2 ? 1 : 0;
    }

    /**
     * Unlocks respecting the rank slot cap. Returns false when already unlocked
     * or no free slot remains.
     */
    @Override
    public boolean unlockElement(String element) {
        if (this.isElementUnlocked(element)) {
            return false;
        }
        if (this.getUnlockedElements().size() >= this.getMaxElementSlots()) {
            return false;
        }
        this.forceUnlockElement(element);
        return true;
    }

    /** Unlock ignoring the slot cap — clan defaults and rank perks use this. */
    private void forceUnlockElement(String element) {
        if (element == null || element.isEmpty() || this.knowsElement(element)) {
            return;
        }
        this.unlockedElements = this.unlockedElements.isEmpty()
                ? element
                : this.unlockedElements + "," + element;
        // The first awakened nature is the ninja's affinity (+25% matching jutsu damage)
        if (this.natureAffinity.isEmpty()) {
            this.natureAffinity = element;
        }
    }

    @Override
    public float getElementXp(String element) {
        if (element != null && this.trainingLoadoutEnabled && TrainingLoadout.NATURES.contains(element)) {
            return TrainingLoadout.ELEMENT_XP;
        }
        return switch (element == null ? "" : element) {
            case "fire" -> this.elementXpFire;
            case "water" -> this.elementXpWater;
            case "earth" -> this.elementXpEarth;
            case "wind" -> this.elementXpWind;
            case "lightning" -> this.elementXpLightning;
            default -> 0;
        };
    }

    /**
     * Global training-speed multiplier for chakra natures. The mastery curve is quadratic
     * (level = sqrt(xp/25)), so the upper levels were a long grind at the raw award rates.
     */
    private static final float ELEMENT_XP_RATE = 1.45f;

    @Override
    public void addElementXp(String element, float amount) {
        if (this.trainingLoadoutEnabled || !this.isElementUnlocked(element) || amount <= 0) {
            return;
        }
        amount *= ELEMENT_XP_RATE;
        switch (element) {
            case "fire" -> this.elementXpFire += amount;
            case "water" -> this.elementXpWater += amount;
            case "earth" -> this.elementXpEarth += amount;
            case "wind" -> this.elementXpWind += amount;
            case "lightning" -> this.elementXpLightning += amount;
            default -> { }
        }
    }

    /**
     * Sets a nature's mastery outright by writing the XP that level implies, inverting
     * the sqrt curve in getElementLevel. Command-only - normal play earns levels.
     */
    @Override
    public void setElementLevel(String element, int level) {
        if (!this.knowsElement(element)) {
            return;
        }
        float xp = (float) (Math.min(Math.max(level, 1), 20) * (double) level * 25.0);
        switch (element) {
            case "fire" -> this.elementXpFire = xp;
            case "water" -> this.elementXpWater = xp;
            case "earth" -> this.elementXpEarth = xp;
            case "wind" -> this.elementXpWind = xp;
            case "lightning" -> this.elementXpLightning = xp;
            default -> { }
        }
    }

    /**
     * Takes a nature away again, wiping its training with it.
     *
     * Also repairs the affinity if this was it: the affinity is simply the first nature
     * awakened, so removing it has to hand that title to whatever is still left, or the
     * player would keep a damage bonus for an element they no longer have.
     */
    @Override
    public boolean removeElement(String element) {
        if (!this.isElementUnlocked(element)) {
            return false;
        }
        StringBuilder remaining = new StringBuilder();
        for (String unlocked : this.unlockedElements.split(",")) {
            if (!unlocked.isEmpty() && !unlocked.equals(element)) {
                if (remaining.length() > 0) {
                    remaining.append(',');
                }
                remaining.append(unlocked);
            }
        }
        this.unlockedElements = remaining.toString();
        this.setElementXpDirect(element, 0f);
        if (element.equals(this.natureAffinity)) {
            this.natureAffinity = this.unlockedElements.isEmpty()
                    ? ""
                    : this.unlockedElements.split(",")[0];
        }
        return true;
    }

    /**
     * True for the mod's own chakra techniques, which the Susanoo shell must NOT swat away.
     *
     * The shell reverses the velocity of anything incoming, which is right for an arrow and
     * completely wrong for a fireball: a jutsu projectile got bounced back and forth at the
     * edge of the shell every tick, so it never reached anything, never ran its impact code
     * and never despawned. Fire thrown at a Susanoo visibly piled up in mid-air instead of
     * bursting against it.
     *
     * Techniques are allowed through to strike the shell properly. They still barely scratch
     * it - the damage sponge takes up to 90% at Complete Body - but they detonate where they
     * land, which is both what it should look like and what stops them accumulating.
     */
    /** Bypasses the unlocked check in setElementLevel, for wiping a removed nature. */
    private void setElementXpDirect(String element, float xp) {
        switch (element == null ? "" : element) {
            case "fire" -> this.elementXpFire = xp;
            case "water" -> this.elementXpWater = xp;
            case "earth" -> this.elementXpEarth = xp;
            case "wind" -> this.elementXpWind = xp;
            case "lightning" -> this.elementXpLightning = xp;
            default -> { }
        }
    }

    /** Command-side unlock that ignores the rank slot cap, like the clan defaults do. */
    @Override
    public void grantElement(String element) {
        this.forceUnlockElement(element);
    }

    /**
     * Element mastery level from XP: level = floor(sqrt(xp / 25)), capped at 20.
     * Level 1 = 25 XP (a couple of casts), level 10 = 2500, level 20 = 10000.
     * An awakened nature always sits at level 1 minimum — the entry-level jutsu of
     * every element require Lv 1, so a level-0 nature could never earn its own XP.
     */
    @Override
    public int getElementLevel(String element) {
        if (!this.isElementUnlocked(element)) {
            return 0;
        }
        int level = (int) Math.floor(Math.sqrt(this.getElementXp(element) / 25.0));
        return Math.min(Math.max(level, 1), 20);
    }

    @Override
    public boolean isJutsuLearned(String jutsuPath) {
        if (jutsuPath == null || jutsuPath.isEmpty()) {
            return false;
        }
        return this.trainingLoadoutEnabled && JutsuScrolls.requiresScroll(jutsuPath) || this.knowsJutsu(jutsuPath);
    }

    private boolean knowsJutsu(String jutsuPath) {
        for (String learned : this.learnedJutsu.split(",")) {
            if (learned.equals(jutsuPath)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void learnJutsu(String jutsuPath) {
        if (jutsuPath == null || jutsuPath.isEmpty() || this.knowsJutsu(jutsuPath)) {
            return;
        }
        this.learnedJutsu = this.learnedJutsu.isEmpty()
                ? jutsuPath
                : this.learnedJutsu + "," + jutsuPath;
    }

    @Override
    public String getBountyTargetId() {
        return this.bountyTargetId;
    }

    @Override
    public int getBountyRemaining() {
        return this.bountyRemaining;
    }

    @Override
    public float getBountyRewardXp() {
        return this.bountyRewardXp;
    }

    @Override
    public void setBounty(String targetId, int count, float rewardXp) {
        this.bountyTargetId = targetId != null ? targetId : "";
        this.bountyRemaining = Math.max(count, 0);
        this.bountyRewardXp = Math.max(rewardXp, 0f);
    }

    @Override
    public void decrementBounty() {
        if (this.bountyRemaining > 0) {
            this.bountyRemaining--;
        }
    }

    @Override
    public int getSharinganLevel() {
        if (!this.hasSharinganEye()) {
            return 0;
        }
        // Mangekyo reads as "level 4" for legacy callers (overlay texture index, gates)
        if (this.mangekyoAwakened) {
            return 4;
        }
        return Math.min(Math.max(this.sharinganTomoe, 0), 3);
    }

    /** True for a born Uchiha or for anyone carrying a transplanted eye. */
    @Override
    public boolean hasSharinganEye() {
        return "uchiha".equals(this.clanId) || this.transplantedSharingan;
    }

    @Override
    public boolean isTransplantedSharingan() {
        return this.transplantedSharingan;
    }

    /**
     * Implants a mature Sharingan into a non-Uchiha. Comes in at three tomoe because the
     * eye is already fully developed — what the recipient lacks is the Uchiha body to
     * switch it off, which is exactly what the permanent chakra drain represents.
     */
    @Override
    public void setTransplantedSharingan(boolean transplanted) {
        this.transplantedSharingan = transplanted;
        if (transplanted && this.sharinganTomoe < 3) {
            this.sharinganTomoe = 3;
        }
    }

    /**
     * A transplanted eye is always open — it cannot be closed, which is the point. For a
     * born Uchiha the eye is only live while the toggle is on.
     */
    @Override
    public boolean isSharinganActive() {
        return this.transplantedSharingan
                || this.toggleAbilityData.getAbilitiesHashSet().contains(SHARINGAN_ABILITY);
    }

    /** True only when the wielder has deliberately switched the eye into combat mode. */
    @Override
    public boolean isSharinganToggled() {
        return this.toggleAbilityData.getAbilitiesHashSet().contains(SHARINGAN_ABILITY);
    }

    @Override
    public String getCopiedJutsu() {
        return this.copiedJutsu;
    }

    @Override
    public void setCopiedJutsu(String jutsuPath) {
        this.copiedJutsu = jutsuPath != null && JutsuScrolls.requiresScroll(jutsuPath) ? jutsuPath : "";
        this.learnJutsu(this.copiedJutsu);
    }

    @Override
    public boolean isApplyingEyeStrain() {
        return this.applyingEyeStrain;
    }

    /**
     * Reads an attack a fraction of a second before it lands and steps out of its way.
     * Costs chakra and has its own short cooldown, so it thins out incoming damage rather
     * than granting free immunity. Returns true when the attack was actually evaded.
     */
    @Override
    public boolean trySharinganDodge(Player player, float incomingDamage) {
        if (!this.ninjaModeEnabled || !this.isSharinganActive() || this.sharinganDodgeCooldown > 0) {
            return false;
        }
        int tomoe = Math.min(Math.max(this.sharinganTomoe, 0), 3);
        if (this.mangekyoAwakened) {
            tomoe = 3;
        }
        if (tomoe <= 0 || this.chakra < SHARINGAN_DODGE_COST) {
            return false;
        }
        // 20% / 40% / 60% by tomoe - the fully matured eye matches the 1.12.2 mod's 60%.
        float chance = 0.2f * tomoe;
        if (player.getRandom().nextFloat() >= chance) {
            return false;
        }
        this.useChakra(SHARINGAN_DODGE_COST, 10);
        this.sharinganDodgeCooldown = SHARINGAN_DODGE_COOLDOWN;
        return true;
    }

    private void updateSharinganUpkeep(Player player) {
        if (this.sharinganDodgeCooldown > 0) {
            this.sharinganDodgeCooldown--;
        }
        if (!this.transplantedSharingan) {
            return;
        }
        // The implanted eye never closes, so it bills the host every second whether or not
        // they are using it. Toggling it into combat mode costs extra on top (SharinganAbility).
        if (player.tickCount % 20 == 0) {
            if (this.chakra >= TRANSPLANT_IDLE_DRAIN) {
                this.useChakra(TRANSPLANT_IDLE_DRAIN, 0);
            } else {
                // Running on empty with a foreign eye hurts.
                player.hurt(player.damageSources().magic(), 1.0f);
            }
        }
    }

    /**
     * Canon: tomoe open under extreme stress, not on a promotion schedule. Rank still
     * grants them automatically (checkDojutsuPerks), but a near-death moment or a hard-won
     * kill can open the next one EARLY. Returns true when a tomoe actually opened.
     */
    @Override
    public boolean tryAwakenSharinganTomoe(Player player, float triggerChance) {
        if (!this.hasSharinganEye() || this.sharinganTomoe >= 3 || this.mangekyoAwakened) {
            return false;
        }
        if (player.getRandom().nextFloat() >= triggerChance) {
            return false;
        }
        this.sharinganTomoe++;
        player.displayClientMessage(Component.translatable("sharingan.awaken", this.sharinganTomoe)
                .withStyle(ChatFormatting.DARK_RED), false);
        player.level().playSound(null, player.blockPosition(),
                com.sekwah.narutomod.sounds.NarutoSounds.SHARINGAN_ACTIVATE.get(),
                net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 0.8f);
        return true;
    }

    @Override
    public int getByakuganRange() {
        if (!this.ninjaModeEnabled) {
            return 0;
        }
        return com.sekwah.narutomod.util.ByakuganSight.range(this.byakuganLevel, "hyuga".equals(this.clanId),
                this.isByakuganActive());
    }

    /** The Sharingan reads chakra, but only what is nearly in front of it. */
    private static final int SHARINGAN_SIGHT_RANGE = 48;
    /**
     * Senjutsu sees further than any eye does.
     *
     * This is the one sensing feat the story is explicit about: Naruto in Sage Mode picked
     * Nagato out at a distance nobody else in the village could, Byakugan included. So the
     * number has to beat the Byakugan's own maximum rather than sit alongside it.
     */
    private static final int SENJUTSU_SIGHT_RANGE = 900;
    /** Kurama lends his own senses; sharper than an eye, short of full senjutsu. */
    private static final int KURAMA_SIGHT_RANGE = 400;

    /**
     * How far this ninja can feel chakra, whatever is providing it.
     *
     * One number for the whole vision overlay instead of a Byakugan-only figure: the
     * Sharingan, Sage Mode and Kurama Chakra Mode all perceive chakra too, and each used to
     * show the player nothing. Whichever source reaches furthest wins - they do not stack,
     * because two ways of seeing the same chakra is still one sighting.
     */
    @Override
    public int getChakraSightRange() {
        if (!this.ninjaModeEnabled) {
            return 0;
        }
        int range = this.getByakuganRange();
        if (this.isSharinganActive()) {
            range = Math.max(range, SHARINGAN_SIGHT_RANGE);
        }
        if (this.isKcmActive()) {
            range = Math.max(range, KURAMA_SIGHT_RANGE);
        }
        if (this.isSageModeActive()) {
            range = Math.max(range, SENJUTSU_SIGHT_RANGE);
        }
        return range;
    }

    @Override
    public boolean isByakuganActive() {
        return this.toggleAbilityData.getAbilitiesHashSet().contains(BYAKUGAN_ABILITY);
    }

    @Override
    public int getEyeVisualState() {
        return this.eyeVisualState;
    }

    @Override
    public int getChidoriTicks() {
        return this.chidoriTicks;
    }

    @Override
    public void setChidoriTicks(int ticks) {
        this.chidoriTicks = Math.max(ticks, 0);
    }

    @Override
    public boolean isChidoriActive() {
        return this.chidoriTicks > 0;
    }

    @Override
    public Direction getWallWalkDirection() {
        if (this.wallWalkDirection < 0 || this.wallWalkDirection >= Direction.values().length) {
            return null;
        }
        // No horizontal-only filter any more, on either half of this.
        //
        // Both the getter and the setter used to drop anything vertical, left over from when a
        // wall was the only thing you could stand on. That silently defeated ceiling walking
        // twice over: setWallWalkDirection(UP) was a no-op, so the stored face stayed on
        // whatever wall you came off and the next tick pulled you back onto it, and the getter
        // returned null for UP anyway, so the renderer never turned the model over. Walking on
        // a ceiling half worked, flickered, and drew the player the right way up.
        return Direction.values()[this.wallWalkDirection];
    }

    @Override
    public void setWallWalkDirection(Direction direction) {
        if (direction != null) {
            this.wallWalkDirection = direction.ordinal();
            this.wallWalkTicks = Math.min(this.wallWalkTicks + 4, 40);
        } else {
            this.wallWalkDirection = -1;
            this.wallWalkTicks = 0;
        }
    }

    @Override
    public boolean isWallWalkAttached() {
        return this.wallWalkAttached;
    }

    @Override
    public void setWallWalkAttached(boolean attached) {
        if (this.wallWalkAttached && !attached) {
            this.wallWalkTurnTicks = 0;
            this.wallWalkTurnFrom = null;
            this.surfaceStride.advance(Vec3.ZERO, null);
        }
        this.wallWalkAttached = attached;
        if (!attached) {
            this.setWallWalkDirection(null);
            // Note: caller is responsible for player.setNoGravity(false) when detaching
        }
    }

    @Override
    public int getWallWalkTicks() {
        return this.wallWalkTicks;
    }

    @Override
    public SurfaceStride getSurfaceStride() {
        return this.surfaceStride;
    }

    @Override
    public DashStride getDashStride() {
        return this.dashStride;
    }

    @Override
    public int getWallWalkTurnTicks() {
        return this.wallWalkTurnTicks;
    }

    @Override
    public Direction getWallWalkTurnFrom() {
        return this.wallWalkTurnFrom;
    }

    @Override
    public void beginWallWalkTurn(Direction previous) {
        this.wallWalkTurnFrom = previous;
        this.wallWalkTurnTicks = 5;
    }

    @Override
    public int getWallWalkDetachTicks() {
        return this.wallWalkDetachTicks;
    }

    @Override
    public void setWallWalkDetachTicks(int ticks) {
        this.wallWalkDetachTicks = Math.max(ticks, 0);
        if (ticks > 0) {
            this.setWallWalkAttached(false);
        }
    }

    @Override
    public float getRankDamageMultiplier() {
        float mult = RANK_DAMAGE_MULTIPLIER[this.getRankIndex()];
        if (this.sageModeActive) {
            mult *= 1.4f;
        }
        if (this.kuramaCloakActive) {
            mult *= 1.8f;
        }
        return mult;
    }

    /**
     * Backwards-compatible name for older jutsu code.
     */
    public float getJutsuDamageMultiplier() {
        return this.getRankDamageMultiplier();
    }

    @Override
    public void resetProgression() {
        this.disableTrainingLoadout();
        this.chakraXp = 0;
        this.ninjaRank = 0;
        this.rankTier = 0;
        this.sixPathsUnlocked = false;
        this.mangekyoBossKills = 0;
        this.clanId = "";
        this.natureAffinity = "";
        this.unlockedElements = "";
        this.elementXpFire = 0;
        this.elementXpWater = 0;
        this.elementXpEarth = 0;
        this.elementXpWind = 0;
        this.elementXpLightning = 0;
        this.learnedJutsu = "";
        this.sharinganTomoe = 0;
        this.mangekyoAwakened = false;
        this.mangekyoForm = "";
        this.msUseCounter = 0;
        this.msBlindnessDecayTicks = 0;
        this.eternalMangekyoAwakened = false;
        this.defeatedMsBosses = "";
        this.byakuganLevel = 0;
        this.rinneganAwakened = false;
        this.rinneganPathsUnlocked = "";
        this.rinneSharinganAwakened = false;
        this.phoenixSageChargeUsedDay = -1L;
        this.hiraishinFormulas = HiraishinFormula.Book.EMPTY;
        this.transplantedSharingan = false;
        this.copiedJutsu = "";
        this.sharinganDodgeCooldown = 0;
        this.ninjaModeEnabled = false;
        this.toggleAbilityData.getAbilitiesHashSet().clear();
        this.chidoriTicks = 0;
        this.setWallWalkAttached(false);
        this.wallWalkDetachTicks = 0;
        this.getConfigData();
        this.chakra = this.maxChakra;
        this.stamina = this.maxStamina;
        this.substitutions = this.maxSubstitutions;
    }

    /**
     * Get the clan-adjusted max chakra bonus multiplier.
     */
    private float getClanChakraMultiplier() {
        return "uzumaki".equals(this.clanId) ? 1.3f : 1.0f;
    }

    /**
     * Get the clan-adjusted chakra regen multiplier.
     */
    public float getClanChakraRegenMultiplier() {
        return "uzumaki".equals(this.clanId) ? 1.5f : 1.0f;
    }

    @Override
    public float getClanLightningDamageMultiplier() {
        return "uchiha".equals(this.clanId) ? 1.15f : 1.0f;
    }

    @Override
    public float getClanJutsuRangeMultiplier() {
        return "nara".equals(this.clanId) ? 1.10f : 1.0f;
    }

    @Override
    public void setInvisibleTicks(int ticks) {
        this.invisibleTicks = ticks;
    }

    @Override
    public boolean getInvisible() {
        return this.isInvisible;
    }

    @Override
    public Vec3 getSubstitutionLoc() {
        return this.substitutionLocation;
    }

    @Override
    public ResourceLocation getSubstitutionDimension() {
        return this.substitutionDimension;
    }

    @Override
    public void setSubstitutionLoc(Vec3 loc, ResourceLocation dimension) {
        this.substitutionLocation = loc;
        this.substitutionDimension = dimension;
    }

    @Override
    public DoubleJumpData getDoubleJumpData() {
        return this.doubleJumpData;
    }

    @Override
    public ResourceLocation getCurrentlyChanneledAbility() {
        return this.currentlyChanneled;
    }

    @Override
    public int getCurrentlyChanneledTicks() {
        return this.ticksChanneled;
    }

    @Override
    public void setCurrentlyChanneledAbility(Player player, Ability ability) {
        this.chargeFeedback.clear();
        this.chargeIntensity = 0;
        if (ability != null) {
            if (ability.castingSound() != null) {
                player.level().playSound(null, player, ability.castingSound(), SoundSource.PLAYERS, 0.5f, 1.0f);
                player.level().gameEvent(player, NarutoGameEvents.JUTSU_CASTING.get(), player.position().add(0, player.getEyeHeight() * 0.7, 0));
            }

            if(!(ability instanceof Ability.Channeled channeled && channeled.hideChannelMessages())) {
                if (ability instanceof Ability.Channeled channeled && channeled.useChargedMessages()) {
                    player.displayClientMessage(Component.translatable("jutsu.charge.start", Component.translatable(ability.getTranslationKey(this, 1)).withStyle(ChatFormatting.YELLOW)).withStyle(ChatFormatting.GREEN), true);
                } else {
                    player.displayClientMessage(Component.translatable("jutsu.channel.start", Component.translatable(ability.getTranslationKey(this, 1)).withStyle(ChatFormatting.YELLOW)).withStyle(ChatFormatting.GREEN), true);
                }
            }

            NarutoRegistries.ABILITIES.getResourceKey(ability)
                    .ifPresent(abilityResourceKey -> this.currentlyChanneled = abilityResourceKey.location());
        } else {
            if (this.currentlyChanneled != null) {
                Ability currentAbility = NarutoRegistries.ABILITIES.getValue(this.currentlyChanneled);
                if( currentAbility != null) {
                    if(!(currentAbility instanceof Ability.Channeled channeled && channeled.hideChannelMessages())) {
                        if (currentAbility instanceof Ability.Channeled channeled && channeled.useChargedMessages()) {
                            player.displayClientMessage(Component.translatable("jutsu.cast", Component.translatable(currentAbility.getTranslationKey(this, this.ticksChanneled - 1)).withStyle(ChatFormatting.YELLOW)).withStyle(ChatFormatting.GREEN), true);
                        } else {
                            player.displayClientMessage(Component.translatable("jutsu.channel.stop", Component.translatable(currentAbility.getTranslationKey(this)).withStyle(ChatFormatting.YELLOW)).withStyle(ChatFormatting.RED), true);
                        }
                    }
                }
            }
            this.currentlyChanneled = null;
        }
        this.ticksChanneled = 0;
    }

    @Override
    public ToggleAbilityData getToggleAbilityData() {
        return this.toggleAbilityData;
    }

    @Override
    public void updateDataServer(Player player) {
        this.sageStoneTicks = (int) Math.max(0, Math.min(SageFocus.STONE_TICKS,
                this.sageStoneUntil - BodyRiteEffects.clock(player)));
        BodyRiteLifecycle.beforeNinjaMode(player, this);
        if (!player.isAlive()) {
            this.eyeVisualState = 0;
            return;
        }
        this.eyeVisualState = player.isAlive() && !player.isSpectator()
                ? com.sekwah.narutomod.util.DojutsuVisual.resolve(this) : 0;
        // The giant's hitbox is real now, so the server has to notice the form appearing and
        // disappearing too. Left client-only this would keep server-side hit detection on the
        // old player-sized box while the client drew and collided with an eighteen-block one.
        this.refreshGiantFormDimensions(player);

        if(this.invisibleTicks > 0) {
            this.invisibleTicks--;
            this.isInvisible = true;
        } else {
            this.isInvisible = false;
        }

        if (this.castPoseTicks > 0) {
            this.castPoseTicks--;
        }

        if (this.msBlindnessDecayTicks > 0) {
            this.msBlindnessDecayTicks--;
            if (this.msBlindnessDecayTicks == 0 && this.msUseCounter > 0) {
                this.msUseCounter--;
                if (this.msUseCounter > 0) {
                    this.msBlindnessDecayTicks = MS_BLINDNESS_DECAY_TICKS;
                }
            }
        }

        if(!this.isNinjaModeEnabled()) {
            if (this.kuramaCloakActive) {
                this.setKuramaCloakActive(false);
                this.refreshGiantFormDimensions(player);
            }
            this.updateNinjaSpeed(player);
            this.chidoriTicks = 0;
            this.setWallWalkAttached(false);
            this.wallWalkDetachTicks = 0;
            return;
        }

        this.getConfigData();
        Iterator<DelayedPlayerTickEvent> iterator = this.delayedTickEvents.iterator();
        while (iterator.hasNext()) {
            DelayedPlayerTickEvent event = iterator.next();
            event.tick();
            if (event.shouldRun()) {
                event.run(player);
                iterator.remove();
            }
        }

        // Compile list of keys from cooldown map
        ArrayList<String> completeList = new ArrayList<>(cooldownTickEvents.keySet());
        //  loop through to tick and then remove cooldown if complete
        for (String name : completeList) {
            CooldownTickEvent event = cooldownTickEvents.get(name);
            event.tick();
            if (event.isComplete()) {
                cooldownTickEvents.remove(name);
            }
        }

        if (this.staminaRegenInfo.canRegen()) {
            this.stamina += 0.5f + this.getNinjaRank() * 0.3f;
        }
        if (this.chakraRegenInfo.canRegen()) {
            this.chakra += this.getChakraRegenPerTick() * getClanChakraRegenMultiplier();
        }
        this.substitutions += NarutoConfig.substitutionRegenRate;
        this.substitutions = Math.min(Math.max(this.substitutions, 0), this.maxSubstitutions);
        this.stamina = Math.min(Math.max(this.stamina, 0), this.maxStamina);
        this.chakra = Math.min(Math.max(this.chakra, 0), this.maxChakra);
        if ("uzumaki".equals(this.clanId) && !this.kuramaCloakActive && !this.kcmActive) {
            this.kuramaBond = Math.min(this.kuramaBond + KURAMA_BOND_REGEN, MAX_KURAMA_BOND);
        }

        this.updateChidoriState(player);
        this.updateTransformedMelee(player);
        // Last, after every reader above: the edge is "swinging now, was not last tick", so
        // recording this tick's state before they run would make the edge permanently false.
        this.swingingLastTick = player.swinging;
        this.decayWallWalkState(player);
        this.updateSageMode(player);
        this.updateEightGates(player);
        this.updateTransformPower();
        this.updateKuramaCloak(player);
        this.updateKCM(player);
        this.updateSusanoo(player);
        this.updateNinjaSprintStamina(player);
        this.updateNinjaSpeed(player);
        this.updateSharinganUpkeep(player);

        if (this.currentlyChanneled != null) {
            Ability ability = NarutoRegistries.ABILITIES.getValue(this.currentlyChanneled);
            if (ability != null && ability.activationType() == Ability.ActivationType.CHANNELED) {
                if(ability.handleCost(player, this, this.ticksChanneled)) {
                    if (ability instanceof Ability.Channeled channeled) {
                        channeled.handleChannelling(player, this, this.ticksChanneled);
                    }
                } else {
                    // Running out of resources is a release like any other. It used to fire
                    // the technique and register nothing, so letting your chakra run dry was
                    // strictly better than letting go: a fully charged Kirin could strike
                    // with no cooldown at all.
                    if (this.ticksChanneled > 0) {
                        com.sekwah.narutomod.abilities.ChannelCompletion.finish(
                                player, this, ability, this.ticksChanneled - 1,
                                com.sekwah.narutomod.abilities.ChannelCompletion.Reason.EXHAUSTED);
                    }
                }
            } else {
                LOGGER.error("Somehow non channeled ability has been set to ninja data {}", this.currentlyChanneled);
            }
            if (this.currentlyChanneled != null) {
                this.ticksChanneled++;
            }
        }

        if ((player.onGround() && !player.level().noCollision(player, player.getBoundingBox().move(0, -0.14, 0)))
                || player.isInWaterOrBubble() || player.onClimbable()
                || (this.toggleAbilityData.getAbilitiesHashSet().contains(NarutoAbilities.WATER_WALK.getId())
                && SurfaceSupport.waterAtFeet(player, player.position()) != null)) {
            this.doubleJumpData.canDoubleJumpServer = true;
        }

        // Clan passive effects (applied every 40 ticks = 2 sec to avoid overhead)
        if (!this.clanId.isEmpty() && player.tickCount % 40 == 0) {
            switch (this.clanId) {
                case "haruno" -> {} // handled below (every tick)
                case "nara" -> player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED, 60, 0, false, false));
                case "hyuga" -> {
                    if (!this.isByakuganActive()) {
                        player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                                net.minecraft.world.effect.MobEffects.DAMAGE_BOOST, 60, 0, false, false));
                    }
                }
            }
        }
        if ("haruno".equals(this.clanId) && player.getHealth() < player.getMaxHealth()) {
            player.heal(0.03f); // ~0.6 HP per second
        }
    }

    /**
     * Whether the player was already mid-swing last tick.
     *
     * Techniques with their own reach resolve on the swing rather than on a landed vanilla
     * hit, so they need the rising edge: {@code player.swinging} stays true for the whole
     * animation and would otherwise fire them six times per click. One detector shared by all
     * of them, because two would eventually disagree about which tick the swing started on.
     */
    private boolean swingingLastTick = false;

    /** True on the single tick a new swing begins. */
    private boolean swingStarted(Player player) {
        return player.swinging && !this.swingingLastTick;
    }

    private void updateChidoriState(Player player) {
        if (this.chidoriTicks <= 0) {
            return;
        }
        if (this.chakra < CHIDORI_TICK_COST) {
            this.chidoriTicks = 0;
            return;
        }
        this.useChakra(CHIDORI_TICK_COST, 5);
        this.chidoriTicks--;

        // The arm is the weapon while it is lit, so a swing resolves the technique on its own
        // reach and its own line - and a swing that finds nothing spends it. Vanilla melee
        // still lands it too (PlayerEvents), whichever fires first; both clear the flag, so
        // the second one finds nothing to do.
        if (this.swingStarted(player)) {
            com.sekwah.narutomod.util.ChidoriStrike.thrust(player, this);
        }
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 8, 0, false, false));
        // The arcs are drawn client-side from isChidoriActive (see JutsuVfxHandler), which is
        // both denser and free. Two sendParticles a tick per chidori user bought a faint
        // sparkle at real bandwidth cost, and drew it a hand's width from where the arcs are.
    }

    private void updateNinjaSprintStamina(Player player) {
        if (!player.isSprinting()) {
            return;
        }
        // Sprinting is free while the Gates are open, and this is not a courtesy: the gates
        // already bill up to four stamina a tick, so adding the sprint drain on top emptied
        // the bar within a second or two and tripped the exhaustion close above. Holding the
        // sprint key was effectively a cancel button for a technique that should have none.
        if (this.gatesOpen > 0) {
            return;
        }
        if (this.stamina < 0.1F) {
            player.setSprinting(false);
            return;
        }
        this.useStamina(0.1F, 5);
    }

    /**
     * Lets a final form walk over the terrain instead of through it.
     *
     * Something eighteen blocks tall does not stop at a one-block ledge, and it does not
     * clamber. Vanilla gives every entity a 0.6 step, so the giant caught on every fence post
     * and slab it met, which is the opposite of how the fight should read.
     *
     * Four blocks: enough to walk up a cliff face or a small building without noticing, and
     * still short enough that it cannot casually step onto a two-storey roof.
     *
     * Nothing is needed for the "should not fall into small caves" half of this. That comes
     * free from the hitbox being genuinely wide: an entity six blocks across is supported by
     * any ground under any part of that footprint, so a one-block shaft is just a dent in the
     * floor to it. It only worked out that way because the width is real - back when the box
     * was still 0.6 wide, the giant fell down holes a giant should bridge.
     */
    private static final java.util.UUID GIANT_STEP_UUID =
            java.util.UUID.fromString("6e4c1f2a-9d3b-4a7e-8c15-2f0b7d9e4a11");
    private static final double GIANT_STEP_HEIGHT = 4.0;

    private void updateGiantStepHeight(Player player) {
        AttributeInstance stepAttr = player.getAttribute(
                net.minecraftforge.common.ForgeMod.STEP_HEIGHT_ADDITION.get());
        if (stepAttr == null) {
            return;
        }
        stepAttr.removeModifier(GIANT_STEP_UUID);
        if (this.isGiantForm()) {
            stepAttr.addTransientModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                    GIANT_STEP_UUID, "narutomod_giant_step", GIANT_STEP_HEIGHT,
                    net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADDITION));
        }
    }

    private void updateNinjaSpeed(Player player) {
        this.updateGiantStepHeight(player);
        int eye = this.ninjaModeEnabled && this.isSharinganToggled() && this.hasSharinganEye()
                ? this.getSharinganLevel() : 0;
        double focus = this.isByakuganActive() && this.getByakuganRange() > 0
                ? com.sekwah.narutomod.util.ByakuganSight.strength(this.byakuganLevel) : 0;
        var damage = player.getAttribute(Attributes.ATTACK_DAMAGE);
        syncEyeAttack(damage, SHARINGAN_READ_ATTACK, "Sharingan timing", eye >= 3 ? 3 : 0);
        syncEyeAttack(damage, BYAKUGAN_FOCUS_ATTACK, "Byakugan focus", focus);
        boolean dash = this.ninjaModeEnabled && !this.wallWalkAttached && !player.isPassenger()
                && !player.isFallFlying() && !player.isSwimming()
                && this.toggleAbilityData.getAbilitiesHashSet().contains(CHAKRA_DASH_ABILITY);
        this.dashStride.update(player.position(), dash);

        AttributeInstance speedAttr = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speedAttr == null) {
            return;
        }

        speedAttr.removeModifier(NINJA_SPEED_UUID);
        if (!this.ninjaModeEnabled) {
            return;
        }

        double speedBonus = eye >= 2 ? 0.2D : 0.0D;
        if (dash) {
            double[] rankSpeeds = new double[] {0.3D, 0.5D, 0.8D, 1.2D, 2.0D};
            speedBonus += rankSpeeds[Math.min(Math.max(this.getNinjaRank(), 0), 4)];
        }
        if (this.gatesOpen > 0) {
            speedBonus += this.gatesOpen * 0.4D;
        }
        if (this.sageModeActive) {
            speedBonus += 0.3D;
        }
        // Grows with the tails rather than being a flat bonus for wearing the cloak at all.
        speedBonus += this.getKuramaSpeedBonus();
        if (this.kcmActive) {
            // The yellow cloak with no beast around it is nothing BUT speed - it is the form
            // that dodged the Fourth Raikage, who then said out loud that Naruto was faster.
            // At 2.5 it sat below the Raikage's own armour and below eight tails, which made
            // the one thing KCM is for the one thing it was not best at.
            speedBonus += 4.0D;
        }
        // Shisui no Shunshin - "Shisui of the Body Flicker". The boss moves fast enough that
        // you can barely track him, but inheriting his eyes handed over Kotoamatsukami and
        // nothing else, so the one thing he was actually famous for did not carry across.
        // It is a passive of the form now, active whenever his Mangekyo is the one you wear.
        if (this.mangekyoAwakened && "shisui".equals(this.mangekyoForm)) {
            speedBonus += 0.85D;
        }
        if (!this.wallWalkAttached && com.sekwah.narutomod.util.LightningCoat.active(player, this)) {
            speedBonus += com.sekwah.narutomod.util.LightningCoat.SPEED_BONUS;
        }

        if (speedBonus > 0.0D) {
            speedAttr.addTransientModifier(new AttributeModifier(
                    NINJA_SPEED_UUID,
                    "Naruto ninja speed",
                    speedBonus,
                    AttributeModifier.Operation.MULTIPLY_BASE));
        }
    }

    private static void syncEyeAttack(AttributeInstance damage, UUID key, String name, double bonus) {
        if (damage == null) {
            return;
        }
        var previous = damage.getModifier(key);
        if (previous != null && Double.compare(previous.getAmount(), bonus) == 0) {
            return;
        }
        damage.removeModifier(key);
        if (bonus > 0) {
            damage.addTransientModifier(new AttributeModifier(key, name, bonus, AttributeModifier.Operation.ADDITION));
        }
    }

    private void decayWallWalkState(Player player) {
        if (this.wallWalkTurnTicks > 0) {
            this.wallWalkTurnTicks--;
        }
        if (this.wallWalkDetachTicks > 0) {
            this.wallWalkDetachTicks--;
        }
        if (this.wallWalkTicks > 0) {
            this.wallWalkTicks--;
        } else {
            if (this.wallWalkAttached) {
                // Ticks ran out while attached — restore gravity
                player.setNoGravity(false);
            }
            this.setWallWalkAttached(false);
        }
    }

    private UUID shadowPossessedTargetUUID = null;
    private int shadowPossessionTicks = 0;

    @Override
    public void setShadowPossessedTarget(UUID targetUUID, int ticks) {
        this.shadowPossessedTargetUUID = targetUUID;
        this.shadowPossessionTicks = ticks;
    }

    @Override
    public UUID getShadowPossessedTargetUUID() {
        return this.shadowPossessedTargetUUID;
    }

    @Override
    public int getShadowPossessionTicks() {
        return this.shadowPossessionTicks;
    }

    @Override
    public boolean hasShadowTarget() {
        return this.shadowPossessedTargetUUID != null && this.shadowPossessionTicks > 0;
    }


    @Sync(minTicks = 1, syncGlobally = true)
    private boolean sageModeActive = false;

    @Sync(minTicks = 1)
    private int sageModeTicks = 0;

    @Sync(minTicks = 1)
    private int sageCharge = 0; // 0-100, accumulated while standing still

    public static final float SAGE_CHAKRA_PER_TICK = 2.0f;
    private final SageFocus sageFocus = new SageFocus();
    private long sageStoneUntil;
    @Sync(minTicks = 5, syncGlobally = true)
    private int sageStoneTicks;

    @Override
    public boolean isSageModeActive() {
        return this.sageModeActive;
    }

    @Override
    public void setSageModeActive(boolean active) {
        this.sageModeActive = active;
        if (!active) {
            this.sageModeTicks = 0;
        }
    }

    @Override
    public int getSageModeTicks() {
        return this.sageModeTicks;
    }

    @Override
    public void setSageModeTicks(int ticks) {
        this.sageModeTicks = ticks;
    }

    @Override
    public int getSageCharge() {
        return this.sageCharge;
    }

    @Override
    public void setSageCharge(int charge) {
        this.sageCharge = Math.max(0, Math.min(charge, SageFocus.MAXIMUM));
    }

    @Override
    public SageFocus getSageFocus() {
        return this.sageFocus;
    }

    @Override
    public long getSageStoneUntil() {
        return this.sageStoneUntil;
    }

    @Override
    public void setSageStoneUntil(long until) {
        this.sageStoneUntil = until;
    }

    @Override
    public int getSageStoneTicks() {
        return this.sageStoneTicks;
    }

    private void updateSageMode(Player player) {
        BodyRiteLifecycle.tickSage(player, this, SAGE_CHAKRA_PER_TICK);
    }

    @Override
    public HiraishinFormula.Book getHiraishinFormulas() {
        return this.hiraishinFormulas;
    }

    @Override
    public void setHiraishinFormulas(HiraishinFormula.Book book) {
        this.hiraishinFormulas = book;
    }

    public static final int MAX_GATES = EightGateCycle.LAST;
    /** Stamina per tick, per open gate. Read by the active-jutsu HUD as well. */
    public static final float GATE_STAMINA_PER_TICK_PER_GATE = 0.5F;

    /** syncGlobally: drives the Eight Gates tremor, which onlookers need to see. */
    @Sync(minTicks = 1, syncGlobally = true)
    private int gatesOpen = 0;
    /** Synced so the HUD can count the gates down; the client cannot derive this. */
    @Sync(minTicks = 1)
    private int gatesTicks = 0; // ticks remaining before gates auto-close
    private final EightGateCycle gateCycle = new EightGateCycle();

    @Override
    public EightGateCycle getGateCycle() {
        return this.gateCycle;
    }

    @Override
    public int getGatesOpen() {
        return this.gatesOpen;
    }

    @Override
    public void setGatesOpen(int gates) {
        this.gatesOpen = Math.max(0, Math.min(gates, MAX_GATES));
    }

    @Override
    public int getGatesTicks() {
        return this.gatesTicks;
    }

    @Override
    public void setGatesTicks(int ticks) {
        this.gatesTicks = ticks;
    }

    private void updateEightGates(Player player) {
        BodyRiteLifecycle.tickGates(player, this, GATE_STAMINA_PER_TICK_PER_GATE);
    }

    @Sync(minTicks = 1, syncGlobally = true)
    private boolean kuramaCloakActive = false;
    @Sync(minTicks = 1, syncGlobally = true)
    private int kuramaTailAge;

    @Sync(minTicks = 1)
    private int kuramaCloakTicks = 0;

    @Sync(minTicks = 20, syncGlobally = true)
    private int kuramaTailCount = 0; // 0=none, 1=Jonin, 4=Kage-partial, 9=Kage-max

    // Tuned so the 1,000,000-point bond pool drains in ~5 minutes (6000 ticks) at base tier:
    // 100/tick base drain + 67/tick donated into the player's own chakra = 167/tick,
    // 1_000_000 / 167 ≈ 5988 ticks ≈ 299s ≈ 5 minutes. Higher tail tiers (transformPower)
    // add extra drain on top, so pushing to Full Avatar burns through it faster than 5 min.
    public static final float KURAMA_CHAKRA_PER_TICK = 100.0f;
    public static final float KURAMA_CHAKRA_DONATION_PER_TICK = 67.0f;
    public static final float KURAMA_SURGE_DRAIN = 100.0f;

    @Override
    public boolean isKuramaCloakActive() {
        return this.kuramaCloakActive;
    }

    @Override
    public void setKuramaCloakActive(boolean active) {
        if (active && !this.kuramaCloakActive) {
            this.setKuramaTailCount(1);
        }
        this.kuramaCloakActive = active;
        if (!active) {
            this.kuramaCloakTicks = 0;
            this.setKuramaTailCount(0);
        } else {
            // Mutually exclusive with KCM — the tailed cloak and "just a glow" mode don't stack
            this.setKcmActive(false);
            this.toggleAbilityData.removeAbility(KuramaChakraModeAbility.ID);
        }
    }

    @Override
    public int getKuramaCloakTicks() {
        return this.kuramaCloakTicks;
    }

    @Override
    public void setKuramaCloakTicks(int ticks) {
        this.kuramaCloakTicks = ticks;
    }

    @Override
    public int getKuramaTailCount() {
        return this.kuramaTailCount;
    }

    @Override
    public void setKuramaTailCount(int count) {
        if (this.kuramaTailCount != count) {
            this.kuramaTailCount = count;
            this.kuramaTailAge = 0;
        }
    }

    @Override
    public int getKuramaTailAge() {
        return this.kuramaTailAge;
    }

    private void updateKuramaCloak(Player player) {
        if (!this.kuramaCloakActive) {
            return;
        }
        if (this.getNinjaRank() < 3 || !"uzumaki".equals(this.clanId) || !player.isAlive()) {
            this.setKuramaCloakActive(false);
            return;
        }
        advanceKuramaStage(player);
        float base = KURAMA_CHAKRA_PER_TICK + this.transformPower * KURAMA_SURGE_DRAIN;
        if (this.kuramaBond < base) {
            this.setKuramaCloakActive(false);
            player.displayClientMessage(Component.translatable("kurama.cloak.spent").withStyle(ChatFormatting.RED), true);
            return;
        }
        float donation = Math.min(Math.max(0, this.maxChakra - this.chakra),
                Math.min(KURAMA_CHAKRA_DONATION_PER_TICK, this.kuramaBond - base));
        this.useKuramaBond(base + donation);
        this.addChakra(donation);
        if (player.tickCount % 20 == 0) {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 30, 2, false, false));
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 30, 0, false, false));
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 30, 0, false, false));
        }
    }

    private void advanceKuramaStage(Player player) {
        this.kuramaTailAge = Math.min(this.kuramaTailAge + 1, 24);
        int wanted = KuramaCloakMotion.desiredTails(this.transformPower, this.getNinjaRank());
        if (wanted == this.kuramaTailCount || this.kuramaTailAge < KuramaCloakMotion.stageTicks(this.kuramaTailCount)) {
            return;
        }
        int next = this.kuramaTailCount + Integer.signum(wanted - this.kuramaTailCount);
        if (next == 9 && !KuramaCloakEvents.canGrow(player)) {
            if (player.tickCount % 40 == 0) {
                player.displayClientMessage(Component.translatable("kurama.cloak.clearance"), true);
            }
            return;
        }
        this.setKuramaTailCount(next);
        this.refreshGiantFormDimensions(player);
        if (next == 9 || next == 4) {
            player.level().playSound(null, player.blockPosition(), NarutoSounds.KURAMA_RISE.get(), SoundSource.PLAYERS, 0.85f,
                    next == 9 ? 0.77f : 1.14f);
        }
    }

    /**
     * Returns extra damage multiplier for melee hits while Kurama Cloak is active.
     * Called externally for AoE explosion on punch.
     */
    /**
     * How much a tail is worth, on the same ladder the beasts themselves use.
     *
     * The beasts double with every tail — Kurama is 256 times a Shukaku — and the cloak was
     * ignoring that completely. One tail and eight tails moved at exactly the same speed and
     * hit for exactly the same 2.5x, so growing tails changed the silhouette and nothing else.
     *
     * The full ladder cannot be handed to a player: 256 times the movement speed is not a
     * transformation, it is a teleport. So the same base is taken to a fractional power, which
     * keeps the shape that matters — the step from seven tails to eight is far bigger than the
     * step from one to two — while landing somewhere a person can still control. The root is
     * per-axis because speed and damage want different curves, and passing it in is what stops
     * the two quietly drifting apart later.
     */
    private double tailLadder(double root) {
        int tails = Math.min(Math.max(this.kuramaTailCount, 1), 9);
        return Math.pow(com.sekwah.narutomod.entity.TailedBeastVariant.POWER_PER_TAIL,
                (tails - 1) / root);
    }

    /** One tail hits for about 1.5x; nine reaches the melee cap on its own. */
    public float getKuramaMeleeDamageMultiplier() {
        return this.kuramaCloakActive ? (float) (1.5D * tailLadder(6.5D)) : 1.0f;
    }

    /** One tail is a jog; eight is faster than an opened Eighth Gate. */
    public double getKuramaSpeedBonus() {
        return this.kuramaCloakActive ? 0.45D * tailLadder(2.6D) : 0.0D;
    }

    @Sync(minTicks = 1, syncGlobally = true)
    private boolean kcmActive = false;
    @Sync(minTicks = 1, syncGlobally = true)
    private int kcmFormAge;
    // "Just a glow" is cheaper than the full tailed cloak — 30/tick base + 20/tick donated
    // to the player's own chakra = 50/tick, 1_000_000 / 50 = 20,000 ticks ≈ 16.7 minutes.
    private static final float KCM_BOND_PER_TICK = 30f;
    private static final float KCM_CHAKRA_DONATION_PER_TICK = 20f;

    @Override
    public boolean isKcmActive() {
        return this.kcmActive;
    }

    @Override
    public void setKcmActive(boolean active) {
        if (active != this.kcmActive) {
            this.kcmFormAge = 0;
        }
        this.kcmActive = active;
        if (active) {
            // Mutually exclusive with the tailed cloak — KCM is the "just a glow" alternative
            this.setKuramaCloakActive(false);
        }
    }

    @Override
    public int getKcmFormAge() {
        return this.kcmFormAge;
    }

    private void updateKCM(Player player) {
        if (!this.kcmActive) {
            return;
        }
        if (this.kuramaBond < KCM_BOND_PER_TICK) {
            KuramaChakraModeAbility.standDown(player, this);
            return;
        }
        this.kcmFormAge = Math.min(24, this.kcmFormAge + 1);
        float missing = Math.max(0, this.maxChakra - this.chakra);
        float available = this.kuramaBond - KCM_BOND_PER_TICK;
        float donation = Math.min(KCM_CHAKRA_DONATION_PER_TICK, Math.min(available, missing));
        this.useKuramaBond(KCM_BOND_PER_TICK + donation);
        this.addChakra(donation);
        if (player.tickCount % 20 == 0) {
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 24, 2, false, false));
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 24, 1, false, false));
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 24, 3, false, false));
        }
    }

    // syncGlobally because these drive what OTHER players see. Without it the capability
    // only ever reaches its owner, so a transformation was visible to the person using it
    // and to nobody else in the world - the one situation where a giant spectral avatar is
    // pointless.
    @Sync(minTicks = 1, syncGlobally = true)
    private boolean susanooActive = false;

    @Sync(minTicks = 1, syncGlobally = true)
    private int susanooStage = 0; // 0=inactive, 1=ribcage (Jonin), 2=full Susanoo (Kage)
    @Sync(minTicks = 1, syncGlobally = true)
    private int susanooStageAge;

    /**
     * The armour's remaining integrity. Every point of damage aimed at the wearer is spent
     * here instead of on their health, which is what makes Susanoo a wall rather than a
     * percentage discount - the fight becomes "break the shell, then reach the person".
     *
     * syncGlobally: an attacker has to be able to see the shell cracking, or there is no way
     * to tell a fight you are winning from one you are not.
     */
    @Sync(minTicks = 1, syncGlobally = true)
    private float susanooDurability = 0f;
    private float storedSusanooIntegrity = 1f;

    /**
     * Ticks left before a shattered Susanoo can be raised again.
     *
     * The whole point of breaking it is the window afterwards. Without a lockout the wearer
     * simply raises a fresh shell the same second and nothing was accomplished.
     */
    @Sync(minTicks = 20, syncGlobally = true)
    private int susanooBrokenTicks = 0;

    /**
     * Integrity by stage. Deliberately steep: at a Six Paths player's roughly sixty per swing,
     * a Complete Body takes about thirty clean hits to open, and that is before its own arms
     * are swatting the attacker back out of reach.
     */

    /** Three minutes, as specified: long enough that losing the shell decides the fight. */
    public static final int SUSANOO_BROKEN_LOCKOUT = 3 * 60 * 20;

    @Override
    public float getSusanooDurability() {
        return this.susanooDurability;
    }

    @Override
    public void setSusanooDurability(float durability) {
        this.susanooDurability = Math.max(0f, durability);
    }

    @Override
    public float getSusanooMaxDurability() {
        return SusanooForm.fromId(this.susanooStage).integrity();
    }

    @Override
    public int getSusanooBrokenTicks() {
        return this.susanooBrokenTicks;
    }

    @Override
    public void setSusanooBrokenTicks(int ticks) {
        this.susanooBrokenTicks = Math.max(0, ticks);
    }

    /**
     * Gated on owning a Sharingan rather than on being born Uchiha: Kakashi drove a
     * Mangekyo out of a transplanted eye, and there is no reason the mod should be
     * stricter than the source. What still separates the two is HOW it opens - a born
     * Uchiha grows into it with rank (see checkDojutsuPerks), while a transplant holder
     * has only the bond-break path (see PlayerEvents.onBondBreakAwakening), exactly as
     * in canon.
     */
    @Override
    public boolean isMangekyoAwakened() {
        return this.hasSharinganEye() && this.mangekyoAwakened;
    }

    @Override
    public void setMangekyoAwakened(boolean awakened) {
        this.mangekyoAwakened = awakened;
    }

    @Override
    public int getSharinganTomoe() {
        return this.sharinganTomoe;
    }

    @Override
    public void setSharinganTomoe(int tomoe) {
        this.sharinganTomoe = Math.min(Math.max(tomoe, 0), 3);
    }

    @Override
    public String getMangekyoForm() {
        return this.mangekyoForm;
    }

    @Override
    public int getSusanooColor() {
        return this.susanooColor;
    }

    @Override
    public void setSusanooColor(int packedRgb) {
        this.susanooColor = packedRgb;
    }

    @Override
    public void setMoveInput(float strafe, float forward) {
        this.moveStrafe = strafe;
        this.moveForward = forward;
    }

    @Override
    public float getMoveStrafe() {
        return this.moveStrafe;
    }

    @Override
    public float getMoveForward() {
        return this.moveForward;
    }

    @Override
    public void setMangekyoForm(String form) {
        this.mangekyoForm = form != null ? form : "";
    }

    /**
     * Every non-Eternal Mangekyo cast strains the eyes — escalating blindness
     * (2s, 4s, 8s... capped at 64s). EMS eyes never strain: that is the whole point of
     * hunting the Mangekyo bosses. Counter relaxes one step per 60s of rest (see
     * updateDataServer) and clears fully on sleep (PlayerEvents.onWakeUp).
     */
    @Override
    public void registerMangekyoUse(Player player) {
        if (this.eternalMangekyoAwakened) {
            return;
        }
        int durationTicks = Math.min(40 * (1 << Math.min(this.msUseCounter, 5)), 1280);
        // Flagged so the Sharingan's genjutsu resistance does not cancel the eye's OWN
        // punishment — otherwise having the eye open would erase the Mangekyo drawback.
        this.applyingEyeStrain = true;
        player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, durationTicks, 0, false, true));
        this.applyingEyeStrain = false;
        this.msUseCounter++;
        this.msBlindnessDecayTicks = MS_BLINDNESS_DECAY_TICKS;
        if (this.msUseCounter >= 3) {
            player.displayClientMessage(
                    Component.translatable("jutsu.mangekyo.strain").withStyle(ChatFormatting.DARK_RED), true);
        }
    }

    @Override
    public void setKamuiReturnPoint(String dimensionId, double x, double y, double z) {
        this.kamuiReturnDimension = dimensionId == null ? "" : dimensionId;
        this.kamuiReturnX = x;
        this.kamuiReturnY = y;
        this.kamuiReturnZ = z;
    }

    @Override
    public void clearKamuiReturnPoint() {
        this.kamuiReturnDimension = "";
        this.kamuiReturnX = 0;
        this.kamuiReturnY = 0;
        this.kamuiReturnZ = 0;
    }

    @Override
    public String getKamuiReturnDimension() {
        return this.kamuiReturnDimension;
    }

    @Override
    public double getKamuiReturnX() {
        return this.kamuiReturnX;
    }

    @Override
    public double getKamuiReturnY() {
        return this.kamuiReturnY;
    }

    @Override
    public double getKamuiReturnZ() {
        return this.kamuiReturnZ;
    }

    @Override
    public int getMsUseCounter() {
        return this.msUseCounter;
    }

    @Override
    public void clearMangekyoStrain() {
        this.msUseCounter = 0;
        this.msBlindnessDecayTicks = 0;
    }

    @Override
    public boolean isEternalMangekyoAwakened() {
        return this.eternalMangekyoAwakened;
    }

    @Override
    public void setEternalMangekyoAwakened(boolean awakened) {
        this.eternalMangekyoAwakened = awakened;
        this.checkRankElementPerks();
    }

    @Override
    public String getDefeatedMsBosses() {
        return this.defeatedMsBosses;
    }

    @Override
    public void addDefeatedMsBoss(String formId) {
        if (formId == null || formId.isEmpty()) {
            return;
        }
        for (String defeated : this.defeatedMsBosses.split(",")) {
            if (defeated.equals(formId)) {
                return;
            }
        }
        this.defeatedMsBosses = this.defeatedMsBosses.isEmpty()
                ? formId
                : this.defeatedMsBosses + "," + formId;
        this.checkRankElementPerks();
    }

    @Override
    public int getDefeatedMsBossCount() {
        if (this.defeatedMsBosses.isEmpty()) {
            return 0;
        }
        return this.defeatedMsBosses.split(",").length;
    }

    /**
     * True when the player commands this Mangekyo form's signature technique — either it
     * is their chosen primary form, or they took it from its owner by defeating that boss.
     */
    @Override
    public boolean hasSignatureForm(String formId) {
        if (formId == null || formId.isEmpty()) {
            return false;
        }
        if (formId.equals(this.mangekyoForm)) {
            return true;
        }
        for (String defeated : this.defeatedMsBosses.split(",")) {
            if (defeated.equals(formId)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int getByakuganLevel() {
        return this.byakuganLevel;
    }

    @Override
    public void setByakuganLevel(int level) {
        this.byakuganLevel = Math.min(Math.max(level, 0), 4);
    }

    @Override
    public boolean isRinneganAwakened() {
        return this.rinneganAwakened;
    }

    @Override
    public void setRinneganAwakened(boolean awakened) {
        this.rinneganAwakened = awakened;
        this.checkRankElementPerks();
    }

    @Override
    public boolean isRinneganPathUnlocked(String pathId) {
        if (this.rinneSharinganAwakened) {
            return true; // the Rinne-Sharingan commands all Six Paths at once
        }
        if (pathId == null || pathId.isEmpty()) {
            return false;
        }
        for (String path : this.rinneganPathsUnlocked.split(",")) {
            if (path.equals(pathId)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void unlockRinneganPath(String pathId) {
        if (pathId == null || pathId.isEmpty()) {
            return;
        }
        for (String path : this.rinneganPathsUnlocked.split(",")) {
            if (path.equals(pathId)) {
                return;
            }
        }
        this.rinneganPathsUnlocked = this.rinneganPathsUnlocked.isEmpty()
                ? pathId
                : this.rinneganPathsUnlocked + "," + pathId;
    }

    @Override
    public boolean isRinneSharinganAwakened() {
        return this.rinneSharinganAwakened;
    }

    /**
     * Phoenix Sage death-save (Rinne-Sharingan): usable once per Minecraft day.
     * Returns true if the charge was available and is now consumed.
     */
    @Override
    public boolean tryConsumePhoenixSageCharge(long worldDay) {
        if (!this.rinneSharinganAwakened || this.phoenixSageChargeUsedDay == worldDay) {
            return false;
        }
        this.phoenixSageChargeUsedDay = worldDay;
        return true;
    }

    @Override
    public boolean isSusanooActive() {
        return this.susanooActive;
    }

    @Override
    public void setSusanooActive(boolean active) {
        if (active && !this.susanooActive) {
            this.setSusanooStage(1);
            this.susanooDurability = this.getSusanooMaxDurability() * this.storedSusanooIntegrity;
        } else if (!active && this.susanooActive) {
            this.storedSusanooIntegrity = this.susanooIntegrity();
        }
        this.susanooActive = active;
        if (!active) {
            this.setSusanooStage(0);
            this.susanooSwingTicks = 0;
        }
    }

    private float susanooIntegrity() {
        return Math.max(0, Math.min(1, this.susanooDurability / Math.max(1, this.getSusanooMaxDurability())));
    }

    @Override
    public int getSusanooStage() {
        return this.susanooStage;
    }

    @Override
    public void setSusanooStage(int stage) {
        this.susanooStage = SusanooForm.fromId(stage).id();
        this.susanooStageAge = 0;
    }

    @Override
    public int getSusanooStageAge() {
        return this.susanooStageAge;
    }

    @Override
    public float getSusanooMeleeDamageMultiplier() {
        if (!this.susanooActive) return 1.0f;
        return this.susanooStage >= 2 ? 2.5f : 1.8f;
    }

    /**
     * Eye-height override applied client-side only (see NinjaCapabilityHandler's
     * EntityEvent.Size handler) while a Complete-Body-scale form is active. Without this,
     * the giant model renders around the player but the camera stays pinned at the normal
     * ~1.62-block eye height, making it look like the "giant" is sinking into the ground —
     * you're viewing it from down near its ankles instead of from up near its head.
     * Server-side dimensions are deliberately left untouched so reach/interaction raycasts
     * (mining, melee, item use) keep originating from the player's real position.
     * @return the eye height to use, or -1 if no giant form is active.
     */
    @Override
    public float getGiantEyeHeight() {
        /*
         * Both giant forms stand at the one shared height now, so the camera is derived from
         * that height instead of being written down twice.
         *
         * The Kurama figure was 42, tuned back when the fox was drawn at sixty-three blocks.
         * Unifying the final forms brought it down to eighteen and left the camera where it
         * was - so a player in the nine-tail avatar was parked two dozen blocks above their
         * own head, looking at empty sky with the procedural tails streaming past. Exactly
         * the failure the Susanoo note above describes, repeated because the number lived in
         * two places instead of one.
         */
        // ~0.85 of the form's height puts the camera near its head, the ratio the original
        // Susanoo value used.
        return this.isGiantForm() ? com.sekwah.narutomod.util.GiantForm.HEIGHT_BLOCKS * 0.85f : -1f;
    }

    /**
     * Whether a final form is standing right now.
     *
     * Split out from {@link #getGiantEyeHeight()} because the size handler needs the same
     * question answered on the SERVER, where an eye height means nothing. Before this the
     * giant was purely cosmetic: an eighteen-block avatar rendered around a hitbox that was
     * still 0.6 by 1.8, so the wielder could be punched in the ankles by anything that walked
     * up, could not block a doorway, and read as standing on the ground inside their own
     * form - the complaint that a Susanoo user "фактически стоит на земле".
     */
    @Override
    public boolean isGiantForm() {
        return (this.susanooActive && SusanooForm.perfect(this.susanooStage))
                || (this.kuramaCloakActive && this.kuramaTailCount >= 9);
    }

    private boolean wasGiantFormActive = false;

    /** Refreshes the hitbox and eye height on both sides because forms do not change the vanilla pose. */
    private void refreshGiantFormDimensions(Player player) {
        boolean isGiantNow = this.isGiantForm();
        if (isGiantNow != this.wasGiantFormActive) {
            this.wasGiantFormActive = isGiantNow;
            player.refreshDimensions();
        }
    }

    public static final float SUSANOO_SURGE_DRAIN = 4.0f;

    private void updateSusanoo(Player player) {
        if (this.susanooBrokenTicks > 0) {
            this.susanooBrokenTicks--;
            if (this.susanooBrokenTicks == 0) {
                this.storedSusanooIntegrity = 1f;
            }
        }
        if (this.susanooSwingTicks > 0) {
            this.susanooSwingTicks--;
        }
        if (!this.susanooActive) {
            if (this.susanooBrokenTicks == 0) {
                this.storedSusanooIntegrity = Math.min(1, this.storedSusanooIntegrity + 1f / 300);
            }
            return;
        }
        this.susanooStageAge = Math.min(20, this.susanooStageAge + 1);
        var current = SusanooForm.fromId(this.susanooStage);
        var target = SusanooForm.forPower(this.transformPower, this.getNinjaRank() >= 4);
        if (current != target && this.susanooStageAge >= 12) {
            var next = current.stepToward(target);
            if (next != SusanooForm.PERFECT || GiantForm.canRaiseSusanoo(player)) {
                float max = this.getSusanooMaxDurability();
                float integrity = max > 0 ? Math.min(1, this.susanooDurability / max) : 1;
                this.setSusanooStage(next.id());
                this.susanooDurability = this.getSusanooMaxDurability() * integrity;
                this.refreshGiantFormDimensions(player);
            } else if (player.tickCount % 40 == 0) {
                player.displayClientMessage(Component.translatable("susanoo.clearance"), true);
            }
        }
        this.useChakra(Math.min(this.chakra, this.transformPower * SUSANOO_SURGE_DRAIN), 5);
        if (player.tickCount % 20 == 0) {
            int tier = SusanooForm.fromId(this.susanooStage).combatTier();
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 24, Math.max(0, tier - 1), false, false));
        }
        if (player instanceof ServerPlayer actor) {
            SusanooCombat.deflect(actor, this.susanooStage);
        }
    }

    private void updateTransformedMelee(Player player) {
        if (this.swingStarted(player) && player instanceof ServerPlayer actor) {
            if (SusanooCombat.start(actor, this)) {
                this.susanooSwingTicks = SpectralSword.length(this.susanooStage);
            }
            KuramaCloakEvents.start(actor, this);
        }
    }

    @Sync(minTicks = 1, syncGlobally = true)
    private int susanooSwingTicks;

    public static final int SUSANOO_SWING_TICKS = SpectralSword.LENGTH;

    @Override
    public int getSusanooSwingTicks() {
        return this.susanooSwingTicks;
    }


    private void getConfigData() {
        int index = this.getRankIndex();
        // The config's maxChakra is still honoured, but as a scale on the Academy step rather
        // than as a flat base every rank is added to - otherwise the ladder's own curve would
        // be flattened by whatever the config happened to say.
        float configScale = NarutoConfig.maxChakra / 100f;
        this.maxChakra = RANK_CHAKRA_POOL[index] * configScale * getClanChakraMultiplier();
        this.maxStamina = RANK_STAMINA_POOL[index] * (NarutoConfig.maxStamina / 100f);
        this.maxSubstitutions = NarutoConfig.maxSubstitutions;
    }

    /** Per-tick regeneration keeps refill time and config scaling consistent across chakra pools. */
    private float getChakraRegenPerTick() {
        int index = this.getRankIndex();
        float configScale = NarutoConfig.chakraRegen / BASE_REGEN_REFERENCE;
        return this.maxChakra / (20f * RANK_REFILL_SECONDS[index]) * configScale;
    }

    /**
     * How much a tick of Chakra Charge channelling restores. Scales with the pool for the
     * same reason regeneration does, and the fill time additionally shortens with rank, so
     * a Kage genuinely channels faster and not merely bigger.
     */
    @Override
    public float getChakraChargePerTick(boolean moving) {
        int index = this.getRankIndex();
        float perTick = this.maxChakra / (20f * RANK_CHARGE_SECONDS[index]);
        return moving ? perTick * 0.2f : perTick;
    }

    @Override
    public ChargeFeedback chakraChargeFeedback() {
        return this.chargeFeedback;
    }

    @Override
    public int getChargeIntensity() {
        return this.chargeIntensity;
    }

    @Override
    public void setChargeIntensity(int intensity) {
        this.chargeIntensity = Math.max(0, Math.min(intensity, 4));
    }

    @Override
    public void scheduleDelayedTickEvent(Consumer<Player> consumer, int tickDelay) {
        this.delayedTickEvents.add(new DelayedPlayerTickEvent(consumer, tickDelay));
    }

    @Override
    public void updateDataClient(Player player) {
        this.doubleJumpData.stuckCheck();
        this.refreshGiantFormDimensions(player);
        if (player.isLocalPlayer()) {
            if (this.wallWalkTurnTicks > 0) {
                this.wallWalkTurnTicks--;
            }
            if (this.wallWalkAttached && !this.toggleAbilityData.getAbilitiesHashSet().contains(NarutoAbilities.WATER_WALK.getId())) {
                this.setWallWalkAttached(false);
                player.setNoGravity(false);
            }
        }
    }

    @Override
    public void setIsNinja(boolean enableNinja) {
        this.ninjaModeEnabled = enableNinja;
    }

    @Override
    public boolean isNinjaModeEnabled() {
        return this.ninjaModeEnabled;
    }

    @Override
    public Tag serializeNBT() {
        final CompoundTag nbt = new CompoundTag();
        nbt.putFloat(CHAKRA_TAG, this.chakra);
        nbt.putFloat(STAMINA_TAG, this.stamina);
        nbt.putBoolean(NINJA_MODE_ENABLED, this.ninjaModeEnabled);
        long currentTime = System.currentTimeMillis();
        nbt.putLong(SAVE_TIME, currentTime);
        final CompoundTag cooldownData = new CompoundTag();
        for (String key : this.cooldownTickEvents.keySet()) {
            CooldownTickEvent event = this.cooldownTickEvents.get(key);
            cooldownData.putInt(key, event.ticks);
        }
        nbt.put(COOLDOWN_TAG, cooldownData);
        nbt.putFloat(SUBSTITUTION_TAG, this.substitutions);
        nbt.putFloat("chakraXp", this.chakraXp);
        if (this.trainingLoadout != null) {
            this.trainingLoadout.save(nbt);
        }
        nbt.putInt("ninjaRank", this.ninjaRank);
        nbt.putInt("rankTier", this.rankTier);
        nbt.putInt("susanooColor", this.susanooColor);
        nbt.putInt("susanooBrokenTicks", this.susanooBrokenTicks);
        nbt.putFloat("susanooIntegrity", this.susanooActive ? this.susanooIntegrity() : this.storedSusanooIntegrity);
        nbt.putBoolean("sixPathsUnlocked", this.sixPathsUnlocked);
        nbt.putInt("mangekyoBossKills", this.mangekyoBossKills);
        nbt.putString("clanId", this.clanId);
        nbt.putString("natureAffinity", this.natureAffinity);
        nbt.putString("unlockedElements", this.unlockedElements);
        nbt.putFloat("elementXpFire", this.elementXpFire);
        nbt.putFloat("elementXpWater", this.elementXpWater);
        nbt.putFloat("elementXpEarth", this.elementXpEarth);
        nbt.putFloat("elementXpWind", this.elementXpWind);
        nbt.putFloat("elementXpLightning", this.elementXpLightning);
        nbt.putString("learnedJutsu", this.learnedJutsu);
        // Dojutsu progression is persisted separately from capability sync.
        nbt.putInt("sharinganTomoe", this.sharinganTomoe);
        nbt.putBoolean("mangekyoAwakened", this.mangekyoAwakened);
        nbt.putString("mangekyoForm", this.mangekyoForm);
        nbt.putString("summonContract", this.summonContract);
        nbt.putInt("msUseCounter", this.msUseCounter);
        nbt.putBoolean("eternalMangekyoAwakened", this.eternalMangekyoAwakened);
        nbt.putString("defeatedMsBosses", this.defeatedMsBosses);
        nbt.putInt("byakuganLevel", this.byakuganLevel);
        nbt.putBoolean("rinneganAwakened", this.rinneganAwakened);
        nbt.putString("rinneganPathsUnlocked", this.rinneganPathsUnlocked);
        nbt.putBoolean("rinneSharinganAwakened", this.rinneSharinganAwakened);
        nbt.putLong("phoenixSageChargeUsedDay", this.phoenixSageChargeUsedDay);
        this.hiraishinFormulas.save(nbt);
        nbt.putString("kamuiReturnDimension", this.kamuiReturnDimension);
        nbt.putDouble("kamuiReturnX", this.kamuiReturnX);
        nbt.putDouble("kamuiReturnY", this.kamuiReturnY);
        nbt.putDouble("kamuiReturnZ", this.kamuiReturnZ);
        nbt.putBoolean("transplantedSharingan", this.transplantedSharingan);
        nbt.putString("copiedJutsu", this.copiedJutsu);
        nbt.putString("bountyTargetId", this.bountyTargetId);
        nbt.putInt("bountyRemaining", this.bountyRemaining);
        nbt.putFloat("bountyRewardXp", this.bountyRewardXp);
        nbt.putInt("sageCharge", this.sageCharge);
        nbt.putLong("sageStoneUntil", this.sageStoneUntil);
        this.gateCycle.save(nbt);
        return nbt;
    }

    @Override
    public void deserializeNBT(Tag tag) {
        if (tag instanceof CompoundTag compoundTag) {
            long currentTime = System.currentTimeMillis();
            long saveTime = compoundTag.getLong(SAVE_TIME);
            int ticksPassed = Math.max((int) ((currentTime - saveTime) / 1000 * 20), 0);
            this.chakra = compoundTag.getFloat(CHAKRA_TAG);
            this.stamina = compoundTag.getFloat(STAMINA_TAG);
            this.ninjaModeEnabled = compoundTag.getBoolean(NINJA_MODE_ENABLED);
            CompoundTag cooldownData = compoundTag.getCompound(COOLDOWN_TAG);
            for (String key : cooldownData.getAllKeys()) {
                this.cooldownTickEvents.put(key, new CooldownTickEvent(cooldownData.getInt(key) - ticksPassed));
            }
            this.substitutions = compoundTag.getFloat(SUBSTITUTION_TAG);
            this.chakraXp = compoundTag.getFloat("chakraXp");
            this.trainingLoadout = TrainingLoadout.read(compoundTag);
            this.trainingLoadoutEnabled = this.trainingLoadout != null;
            this.ninjaRank = compoundTag.getInt("ninjaRank");
            // Absent on saves written before the ladder existed; those players simply start
            // their current rank at Low, which is exactly where the old thresholds put them.
            this.rankTier = compoundTag.getInt("rankTier");
            // Absent means "never picked one", which is the canon colour, not black.
            this.susanooColor = compoundTag.contains("susanooColor")
                    ? compoundTag.getInt("susanooColor") : -1;
            this.setSusanooBrokenTicks(compoundTag.getInt("susanooBrokenTicks"));
            float stored = compoundTag.contains("susanooIntegrity", Tag.TAG_FLOAT) ? compoundTag.getFloat("susanooIntegrity") : 1f;
            this.storedSusanooIntegrity = Float.isFinite(stored) ? Math.max(0, Math.min(1, stored)) : 1f;
            this.sixPathsUnlocked = compoundTag.getBoolean("sixPathsUnlocked");
            this.mangekyoBossKills = compoundTag.getInt("mangekyoBossKills");
            this.clanId = compoundTag.getString("clanId");
            this.natureAffinity = compoundTag.getString("natureAffinity");
            this.unlockedElements = compoundTag.getString("unlockedElements");
            this.elementXpFire = compoundTag.getFloat("elementXpFire");
            this.elementXpWater = compoundTag.getFloat("elementXpWater");
            this.elementXpEarth = compoundTag.getFloat("elementXpEarth");
            this.elementXpWind = compoundTag.getFloat("elementXpWind");
            this.elementXpLightning = compoundTag.getFloat("elementXpLightning");
            this.learnedJutsu = compoundTag.getString("learnedJutsu");
            // Migration for worlds saved before Phase 15: grant the clan default element
            if (this.unlockedElements.isEmpty() && !this.clanId.isEmpty()) {
                this.applyClanDefaultElements();
            }
            this.sharinganTomoe = compoundTag.getInt("sharinganTomoe");
            this.mangekyoAwakened = compoundTag.getBoolean("mangekyoAwakened");
            this.mangekyoForm = compoundTag.getString("mangekyoForm");
            this.summonContract = compoundTag.getString("summonContract");
            this.msUseCounter = compoundTag.getInt("msUseCounter");
            this.eternalMangekyoAwakened = compoundTag.getBoolean("eternalMangekyoAwakened");
            this.defeatedMsBosses = compoundTag.getString("defeatedMsBosses");
            this.byakuganLevel = compoundTag.getInt("byakuganLevel");
            this.rinneganAwakened = compoundTag.getBoolean("rinneganAwakened");
            this.rinneganPathsUnlocked = compoundTag.getString("rinneganPathsUnlocked");
            this.rinneSharinganAwakened = compoundTag.getBoolean("rinneSharinganAwakened");
            this.phoenixSageChargeUsedDay = compoundTag.contains("phoenixSageChargeUsedDay")
                    ? compoundTag.getLong("phoenixSageChargeUsedDay")
                    : -1L;
            this.hiraishinFormulas = HiraishinFormula.Book.read(compoundTag);
            this.kamuiReturnDimension = compoundTag.getString("kamuiReturnDimension");
            this.kamuiReturnX = compoundTag.getDouble("kamuiReturnX");
            this.kamuiReturnY = compoundTag.getDouble("kamuiReturnY");
            this.kamuiReturnZ = compoundTag.getDouble("kamuiReturnZ");
            this.transplantedSharingan = compoundTag.getBoolean("transplantedSharingan");
            this.setCopiedJutsu(compoundTag.getString("copiedJutsu"));
            // Restore rank-earned dojutsu when loading saves without explicit eye tiers.
            this.checkDojutsuPerks();
            this.bountyTargetId = compoundTag.getString("bountyTargetId");
            this.bountyRemaining = compoundTag.getInt("bountyRemaining");
            this.bountyRewardXp = compoundTag.getFloat("bountyRewardXp");
            this.setSageCharge(compoundTag.getInt("sageCharge"));
            this.sageStoneUntil = Math.max(0, compoundTag.getLong("sageStoneUntil"));
            this.gateCycle.load(compoundTag);
            this.setGatesOpen(this.gateCycle.hasDeathDeadline() ? MAX_GATES : 0);
            this.getConfigData();
        }
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        return NinjaCapabilityHandler.NINJA_DATA.orEmpty(cap, holder);
    }

    @Override
    public HashMap<String, CooldownTickEvent> getCooldownEvents() {
        return cooldownTickEvents;
    }
}
