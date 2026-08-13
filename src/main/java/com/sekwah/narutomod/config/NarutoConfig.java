package com.sekwah.narutomod.config;

import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.client.gui.BarDesigns;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

@Mod.EventBusSubscriber(modid = NarutoMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class NarutoConfig {

    public static final String SERVER = "server";
    public static final String CATEGORY_WEAPONS = "weapons";
    public static final String CATEGORY_BOSSES = "bosses";

    // Settings about players
    public static final String CATEGORY_PLAYER = "player";

    public static final String CLIENT = "client";

    public static final String CATEGORY_UI = "ui";
    public static final String ENERGY_BARS = "energy_bars";

    public static final ForgeConfigSpec MOD_CONFIG;

    private static final ForgeConfigSpec.DoubleValue CONFIG_MAX_CHAKRA;
    public static float maxChakra;

    private static final ForgeConfigSpec.DoubleValue CONFIG_MAX_STAMINA;
    public static float maxStamina;

    private static final ForgeConfigSpec.DoubleValue CONFIG_CHAKRA_REGEN;
    public static float chakraRegen;

    private static final ForgeConfigSpec.DoubleValue CONFIG_STAMINA_REGEN;
    public static float staminaRegen;

    private static final ForgeConfigSpec.IntValue CONFIG_MAX_SUBSTITUTIONS;
    public static int maxSubstitutions;

    private static final ForgeConfigSpec.IntValue CONFIG_SUBSTITUTION_REGEN_TIME;
    public static float substitutionRegenRate;

    private static final ForgeConfigSpec.BooleanValue CONFIG_KUNAI_BLOCK_DAMAGE;
    public static boolean kunaiBlockDamage;

    private static final ForgeConfigSpec.DoubleValue CONFIG_KUNAI_EXPLOSION_RADIUS;
    public static float kunaiExplosionRadius;

    private static final ForgeConfigSpec.DoubleValue CONFIG_PAPERBOMB_EXPLOSION_RADIUS;
    public static float paperbombExplosionRadius;

    private static final ForgeConfigSpec.BooleanValue CONFIG_PAPERBOMB_BLOCK_DAMAGE;
    public static boolean paperbombBlockDamage;

    private static final ForgeConfigSpec.IntValue CONFIG_JUTSU_KEYBIND_HOLD_THRESHOLD;
    public static int jutsuKeybindHoldThreshold;

    private static final ForgeConfigSpec.IntValue CONFIG_JUTSU_CAST_DELAY;
    public static int jutsuCastDelay;

    // Phase 16: roaming Mangekyo bosses (spawned by vanilla mob spawning, see the biome modifier)
    private static final ForgeConfigSpec.BooleanValue CONFIG_BOSS_SPAWN_ENABLED;
    public static boolean mangekyoBossSpawnEnabled;

    private static final ForgeConfigSpec.IntValue CONFIG_CHAKRA_BAR_DESIGN;
    public static int chakraBarDesign;

    //private static final ForgeConfigSpec.BooleanValue CONFIG_STARTS_AS_NINJA;
    //public static boolean startsAsNinja;

    static {
        ForgeConfigSpec.Builder configBuilder = new ForgeConfigSpec.Builder();

        // ===========================================================
        // Server Settings
        // ===========================================================
        configBuilder.comment("Server based settings").push(SERVER);

        configBuilder.comment("Player settings").push(CATEGORY_PLAYER);

//        CONFIG_STARTS_AS_NINJA = configBuilder.comment("Does the player start as a ninja by default?")
//                .define("startsAsNinja", true);

        configBuilder.comment("Stuff such as regen rates and maximum (will likely change with updates, e.g. stats system)").push(ENERGY_BARS);

        CONFIG_MAX_CHAKRA = configBuilder.comment("Max Chakra")
                .defineInRange("maxChakra",  100F ,  0d, Double.MAX_VALUE);

        CONFIG_CHAKRA_REGEN = configBuilder.comment("Chakra Regen")
                .defineInRange("chakraRegen",  0.05d ,  0d, Double.MAX_VALUE);

        CONFIG_MAX_STAMINA = configBuilder.comment("Max Stamina")
                .defineInRange("maxStamina",  100F ,  0d, Double.MAX_VALUE);

        CONFIG_STAMINA_REGEN = configBuilder.comment("Stamina Regen")
                .defineInRange("staminaRegen",  0.4d ,  0d, Double.MAX_VALUE);

        CONFIG_MAX_SUBSTITUTIONS = configBuilder.comment("Max Substitutions")
                .defineInRange("maxSubstitutions",  3 ,  0, Integer.MAX_VALUE);

        // Floor of one second, not zero. The rate below is 1/(time*20), and at zero that is a
        // float divide by zero - which in Java is Infinity rather than an exception, so
        // nothing crashed and substitutions simply came back instantly and forever.
        CONFIG_SUBSTITUTION_REGEN_TIME = configBuilder.comment("Substitution Regen Time (Seconds)")
                        .defineInRange("substitutionRegenTime",  60 ,  1, Integer.MAX_VALUE);

        configBuilder.pop();

        configBuilder.pop();

        configBuilder.comment("Variables for weapons").push(CATEGORY_WEAPONS);

        CONFIG_KUNAI_BLOCK_DAMAGE = configBuilder.comment("Explosive Kunai block damage")
                .define("kunaiExplosionBreakBlocks", true);

        CONFIG_KUNAI_EXPLOSION_RADIUS = configBuilder.comment("Explosive Kunai explosion radius")
                .defineInRange("kunaiExplosionRadius",  3d ,  1d, 100d);

        CONFIG_PAPERBOMB_EXPLOSION_RADIUS = configBuilder.comment("Paper Bomb explosion radius")
                .defineInRange("paperBombExplosionRadius",  4.0F ,  1d, 100d);

        CONFIG_PAPERBOMB_BLOCK_DAMAGE = configBuilder.comment("Paper Bomb block damage")
                .define("paperBombExplosionBreakBlocks", true);

        CONFIG_JUTSU_KEYBIND_HOLD_THRESHOLD = configBuilder.comment("Key hold threshold in ticks (20 per second)")
                .defineInRange("jutsuKeyHoldThreshold", 15, 0, Integer.MAX_VALUE);

        CONFIG_JUTSU_CAST_DELAY = configBuilder.comment("Jutsu activation delay (20 per second)")
                .defineInRange("jutsuActivateDelay", 15, 0, Integer.MAX_VALUE);

        configBuilder.pop();

        configBuilder.comment("Roaming Mangekyo wielders (Itachi, Sasuke, Madara, Shisui, Obito). "
                + "Defeating one upgrades a Mangekyo Sharingan to Eternal.").push(CATEGORY_BOSSES);

        // Rate and cap now come from vanilla mob spawning (see the forge biome modifier),
        // so all that is left here is an on/off switch folded into the spawn predicate.
        CONFIG_BOSS_SPAWN_ENABLED = configBuilder.comment("Whether Mangekyo bosses spawn in the world at all")
                .define("mangekyoBossSpawnEnabled", true);

        configBuilder.pop();

        configBuilder.pop();

        // ===========================================================
        // Client Settings
        // ===========================================================
        configBuilder.comment("Client based settings").push(CLIENT);

        configBuilder.comment("Variables for UI").push(CATEGORY_UI);
        CONFIG_CHAKRA_BAR_DESIGN = configBuilder.comment("Design for the chara bar")
                .defineInRange("chakraBarDesign", 0, 0,  BarDesigns.BarInfo.values().length - 1);

        configBuilder.pop();

        configBuilder.pop();

        MOD_CONFIG = configBuilder.build();
    }

    public static void loadVariables() {
        maxChakra = CONFIG_MAX_CHAKRA.get().floatValue();
        maxStamina = CONFIG_MAX_STAMINA.get().floatValue();
        staminaRegen = CONFIG_STAMINA_REGEN.get().floatValue();
        chakraRegen = CONFIG_CHAKRA_REGEN.get().floatValue();
        maxSubstitutions = CONFIG_MAX_SUBSTITUTIONS.get();
        // Clamped here as well as in the range above: an existing config file written before
        // that floor existed still holds a zero, and Forge only corrects values it rejects on
        // load - a belt-and-braces guard is cheaper than an Infinity leaking into the tick.
        substitutionRegenRate = 1f / (Math.max(1, CONFIG_SUBSTITUTION_REGEN_TIME.get()) * 20f);
        kunaiBlockDamage = CONFIG_KUNAI_BLOCK_DAMAGE.get();
        kunaiBlockDamage = CONFIG_KUNAI_BLOCK_DAMAGE.get();
        kunaiExplosionRadius = CONFIG_KUNAI_EXPLOSION_RADIUS.get().floatValue();
        paperbombExplosionRadius = CONFIG_PAPERBOMB_EXPLOSION_RADIUS.get().floatValue();
        paperbombBlockDamage = CONFIG_PAPERBOMB_BLOCK_DAMAGE.get();
        jutsuKeybindHoldThreshold = CONFIG_JUTSU_KEYBIND_HOLD_THRESHOLD.get();
        mangekyoBossSpawnEnabled = CONFIG_BOSS_SPAWN_ENABLED.get();
        chakraBarDesign = CONFIG_CHAKRA_BAR_DESIGN.get();
        jutsuCastDelay = CONFIG_JUTSU_CAST_DELAY
                .get();
        //startsAsNinja = CONFIG_STARTS_AS_NINJA.get();
    }

    @SubscribeEvent
    public static void onLoad(final ModConfigEvent.Loading configEvent) {
        loadVariables();
    }

    @SubscribeEvent
    public static void onReload(final ModConfigEvent.Reloading configEvent) {
        loadVariables();
    }
}
