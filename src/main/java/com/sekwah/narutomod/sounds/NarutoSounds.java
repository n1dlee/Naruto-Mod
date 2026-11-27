package com.sekwah.narutomod.sounds;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import static com.sekwah.narutomod.NarutoMod.MOD_ID;

public class NarutoSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, MOD_ID);

    public static final RegistryObject<SoundEvent> KUNAI_THUD = register("kunai_thud");

    public static final RegistryObject<SoundEvent> NEEDLE_HIT = register("needle_hit");

    public static final RegistryObject<SoundEvent> SIZZLE = register("sizzle");

    public static final RegistryObject<SoundEvent> SEAL_A = register("seal_a");
    public static final RegistryObject<SoundEvent> SEAL_B = register("seal_b");
    public static final RegistryObject<SoundEvent> SEAL_C = register("seal_c");

    public static final RegistryObject<SoundEvent> JUTSU_CAST = register("jutsu_cast");
    public static final RegistryObject<SoundEvent> JUTSU_FAIL = register("jutsu_fail");

    public static final RegistryObject<SoundEvent> FIREBALL_EXPLODE = register("fireball.explosion");
    public static final RegistryObject<SoundEvent> FIREBALL_SHOOT = register("fireball.shoot");
    public static final RegistryObject<SoundEvent> FIREBALL_EXHALE = register("great_fireball.exhale");
    public static final RegistryObject<SoundEvent> FIREBALL_STEAM = register("great_fireball.steam");
    public static final RegistryObject<SoundEvent> PHOENIX_VOLLEY = register("phoenix_flower.volley");

    public static final RegistryObject<SoundEvent> WATER_BULLET_SHOOT = register("waterbullet.shoot");
    public static final RegistryObject<SoundEvent> WATER_BULLET_SPLASH = register("waterbullet.splash");
    public static final RegistryObject<SoundEvent> DORYUHEKI_RISE = register("doryuheki.rise");
    public static final RegistryObject<SoundEvent> EARTH_SPIKE_CRACK = register("earth_spikes.crack");
    public static final RegistryObject<SoundEvent> GIAN_CRACK = register("gian.crack");
    public static final RegistryObject<SoundEvent> GUST_EDGE = register("great_breakthrough.gust");
    public static final RegistryObject<SoundEvent> SHOCK_SNAP = register("lightning_shock.snap");
    public static final RegistryObject<SoundEvent> LIGHTNING_COAT_HUM = register("lightning_armor.hum");
    public static final RegistryObject<SoundEvent> AMATERASU_IGNITE = register("amaterasu.ignite");
    public static final RegistryObject<SoundEvent> KAMUI_FOLD = register("kamui.fold");
    public static final RegistryObject<SoundEvent> SHARINGAN_WHISPER = register("sharingan.whisper");
    public static final RegistryObject<SoundEvent> TSUKUYOMI_ECHO = register("tsukuyomi.echo");
    public static final RegistryObject<SoundEvent> CROW_FLOCK_WINGS = register("crow_flock.wings");
    public static final RegistryObject<SoundEvent> GUNBAI_CUT = register("gunbai.wind_cut");
    public static final RegistryObject<SoundEvent> KOTO_WHISPER = register("koto.whisper");
    public static final RegistryObject<SoundEvent> KAMUI_TRANSIT = register("kamui.transit");
    public static final RegistryObject<SoundEvent> SPIRIT_OPEN = register("spirit.open");
    public static final RegistryObject<SoundEvent> SPIRIT_RETURN = register("spirit.return");
    public static final RegistryObject<SoundEvent> SHADOW_WEAVE = register("shadow.weave");
    public static final RegistryObject<SoundEvent> SHADOW_NEEDLE = register("shadow.needle");
    public static final RegistryObject<SoundEvent> SHADOW_GRIP_LIFT = register("shadow.grip_lift");
    public static final RegistryObject<SoundEvent> SHADOW_GRIP_CLENCH = register("shadow.grip_clench");
    public static final RegistryObject<SoundEvent> SHADOW_GRIP_DROP = register("shadow.grip_drop");
    public static final RegistryObject<SoundEvent> AIR_PALM_RELEASE = register("air_palm.release");
    public static final RegistryObject<SoundEvent> AIR_PALM_HIT = register("air_palm.hit");
    public static final RegistryObject<SoundEvent> ADAMANTINE_SPOOL = register("adamantine.spool");
    public static final RegistryObject<SoundEvent> ADAMANTINE_LOCK = register("adamantine.lock");
    public static final RegistryObject<SoundEvent> ADAMANTINE_RELEASE = register("adamantine.release");
    public static final RegistryObject<SoundEvent> KAITEN_SPIN = register("kaiten.spin");
    public static final RegistryObject<SoundEvent> KAITEN_CONTACT = register("kaiten.contact");
    public static final RegistryObject<SoundEvent> TRIGRAM_FIELD = register("trigram.field");
    public static final RegistryObject<SoundEvent> TRIGRAM_SEAL = register("trigram.seal");
    public static final RegistryObject<SoundEvent> MEDICAL_PALM_OPEN = register("medical_palm.open");
    public static final RegistryObject<SoundEvent> MEDICAL_PALM_HEAL = register("medical_palm.heal");
    public static final RegistryObject<SoundEvent> SCALPEL_FORM = register("scalpel.form");
    public static final RegistryObject<SoundEvent> SCALPEL_CUT = register("scalpel.cut");
    public static final RegistryObject<SoundEvent> BAIKA_GROW = register("baika.grow");
    public static final RegistryObject<SoundEvent> BAIKA_STEP = register("baika.step");
    public static final RegistryObject<SoundEvent> BAIKA_DENIED = register("baika.denied");
    public static final RegistryObject<SoundEvent> BOULDER_TUCK = register("boulder.tuck");
    public static final RegistryObject<SoundEvent> BOULDER_ROLL = register("boulder.roll");
    public static final RegistryObject<SoundEvent> BOULDER_BUMP = register("boulder.bump");
    public static final RegistryObject<SoundEvent> SHINRANSHIN_FOCUS = register("shinranshin.focus");
    public static final RegistryObject<SoundEvent> SHINRANSHIN_GRIP = register("shinranshin.grip");
    public static final RegistryObject<SoundEvent> NINKEN_ARRIVE = register("ninken.arrive");
    public static final RegistryObject<SoundEvent> NINKEN_TRACK = register("ninken.track");
    public static final RegistryObject<SoundEvent> NINKEN_DISMISS = register("ninken.dismiss");
    public static final RegistryObject<SoundEvent> GATSUGA_RELEASE = register("gatsuga.release");
    public static final RegistryObject<SoundEvent> GATSUGA_STOP = register("gatsuga.stop");
    public static final RegistryObject<SoundEvent> KIKAICHU_LAUNCH = register("kikaichu.launch");
    public static final RegistryObject<SoundEvent> KIKAICHU_RETURN = register("kikaichu.return");
    public static final RegistryObject<SoundEvent> MOKUTON_WARN = register("mokuton.warn");
    public static final RegistryObject<SoundEvent> MOKUTON_GROW = register("mokuton.grow");
    public static final RegistryObject<SoundEvent> MOKUTON_BURIAL = register("mokuton.burial");
    public static final RegistryObject<SoundEvent> MOKUTON_GRAB = register("mokuton.grab");
    public static final RegistryObject<SoundEvent> MOKUTON_GOLEM = register("mokuton.golem");
    public static final RegistryObject<SoundEvent> IRON_SAND_GATHER = register("iron_sand.gather");
    public static final RegistryObject<SoundEvent> IRON_SAND_BREAK = register("iron_sand.break");
    public static final RegistryObject<SoundEvent> ICE_FIELD_WARN = register("ice_field.warn");
    public static final RegistryObject<SoundEvent> ICE_FIELD_RISE = register("ice_field.rise");
    public static final RegistryObject<SoundEvent> ICE_FIELD_MELT = register("ice_field.melt");
    public static final RegistryObject<SoundEvent> LASER_CIRCUS_FOCUS = register("laser_circus.focus");
    public static final RegistryObject<SoundEvent> LASER_CIRCUS_CAST = register("laser_circus.cast");
    public static final RegistryObject<SoundEvent> CLAY_MOLD = register("clay.mold");
    public static final RegistryObject<SoundEvent> CLAY_ARM = register("clay.arm");
    public static final RegistryObject<SoundEvent> CLAY_BURST = register("clay.burst");
    public static final RegistryObject<SoundEvent> SHINRA_PRESSURE = register("shinra.pressure");
    public static final RegistryObject<SoundEvent> SHINRA_RELEASE = register("shinra.release");
    public static final RegistryObject<SoundEvent> SHINRA_WAKE = register("shinra.wake");
    public static final RegistryObject<SoundEvent> BANSHO_FOCUS = register("bansho.focus");
    public static final RegistryObject<SoundEvent> BANSHO_PULL = register("bansho.pull");
    public static final RegistryObject<SoundEvent> CHIBAKU_SEED = register("chibaku.seed");
    public static final RegistryObject<SoundEvent> CHIBAKU_GATHER = register("chibaku.gather");
    public static final RegistryObject<SoundEvent> CHIBAKU_CRASH = register("chibaku.crash");
    public static final RegistryObject<SoundEvent> PRETA_OPEN = register("preta.open");
    public static final RegistryObject<SoundEvent> PRETA_SIP = register("preta.sip");
    public static final RegistryObject<SoundEvent> NARAKA_RESTORE = register("naraka.restore");
    public static final RegistryObject<SoundEvent> NARAKA_JAW = register("naraka.jaw");
    public static final RegistryObject<SoundEvent> NARAKA_CLOSE = register("naraka.close");
    public static final RegistryObject<SoundEvent> SUMMON_MARK = register("summon.mark");
    public static final RegistryObject<SoundEvent> HIRAISHIN_MARK = register("hiraishin.mark");
    public static final RegistryObject<SoundEvent> HIRAISHIN_DEPART = register("hiraishin.depart");
    public static final RegistryObject<SoundEvent> HIRAISHIN_ARRIVE = register("hiraishin.arrive");
    public static final RegistryObject<SoundEvent> CLONE_SEAL = register("shadow_clone.seal");
    public static final RegistryObject<SoundEvent> CLONE_FORM = register("shadow_clone.form");
    public static final RegistryObject<SoundEvent> CLONE_DISPEL = register("shadow_clone.dispel");
    public static final RegistryObject<SoundEvent> RITE_GATHER = register("body_rite.gather");
    public static final RegistryObject<SoundEvent> RITE_BALANCE = register("body_rite.balance");
    public static final RegistryObject<SoundEvent> RITE_STONE = register("body_rite.stone");
    public static final RegistryObject<SoundEvent> RITE_RELEASE = register("body_rite.release");
    public static final RegistryObject<SoundEvent> RITE_HEART = register("body_rite.heart");
    public static final RegistryObject<SoundEvent> RITE_GATE = register("body_rite.gate");
    public static final RegistryObject<SoundEvent> RITE_END = register("body_rite.end");
    public static final RegistryObject<SoundEvent> KURAMA_RISE = register("kurama_cloak.rise");
    public static final RegistryObject<SoundEvent> KURAMA_PAW = register("kurama_cloak.paw");
    public static final RegistryObject<SoundEvent> KURAMA_SWEEP = register("kurama_cloak.sweep");
    public static final RegistryObject<SoundEvent> SUSANOO_CUT = register("final_chakra.sword");
    public static final RegistryObject<SoundEvent> SUSANOO_BLOCK = register("final_chakra.armor");
    public static final RegistryObject<SoundEvent> SUSANOO_BREAK = register("final_chakra.shatter");
    public static final RegistryObject<SoundEvent> MAGATAMA_GATHER = register("final_chakra.magatama");
    public static final RegistryObject<SoundEvent> MAGATAMA_HIT = register("final_chakra.seal");
    public static final RegistryObject<SoundEvent> KCM_KINDLE = register("final_chakra.kindling");
    public static final RegistryObject<SoundEvent> KCM_DIM = register("final_chakra.dim");
    public static final RegistryObject<SoundEvent> BIJU_BUILD = register("final_chakra.biju_build");
    public static final RegistryObject<SoundEvent> BIJU_LAUNCH = register("final_chakra.biju_launch");
    public static final RegistryObject<SoundEvent> BIJU_ABORT = register("final_chakra.biju_abort");
    public static final RegistryObject<SoundEvent> BIJU_COLLAPSE = register("final_chakra.biju_collapse");
    public static final RegistryObject<SoundEvent> SUMMON_ARRIVE = register("summon.arrive");
    public static final RegistryObject<SoundEvent> ANIMAL_RAM = register("summon.ram");
    public static final RegistryObject<SoundEvent> SUMMON_TOAD = register("summon.toad");
    public static final RegistryObject<SoundEvent> SUMMON_SNAKE = register("summon.snake");
    public static final RegistryObject<SoundEvent> SUMMON_SLUG = register("summon.slug");
    public static final RegistryObject<SoundEvent> SUMMON_MONKEY = register("summon.monkey");
    private static final java.util.List<RegistryObject<SoundEvent>> TRIGRAM_BEATS = java.util.List.of(
            register("trigram.two"), register("trigram.four"), register("trigram.eight"),
            register("trigram.sixteen"), register("trigram.thirty_two"));
    public static final RegistryObject<SoundEvent> WATER_DRAGON_FORM = register("water_dragon.form");
    public static final RegistryObject<SoundEvent> WATER_DRAGON_LAUNCH = register("water_dragon.launch");
    public static final RegistryObject<SoundEvent> WATER_DRAGON_IMPACT = register("water_dragon.impact");
    public static final RegistryObject<SoundEvent> CHIDORI_DASH_LAUNCH = register("chidori_dash.launch");
    public static final RegistryObject<SoundEvent> CHAKRA_DASH = register("chakra_dash.wind");
    public static final RegistryObject<SoundEvent> CHAKRA_CHARGE_BREATH = register("chakra_charge.breath");
    public static final RegistryObject<SoundEvent> CHAKRA_CHARGE_FULL = register("chakra_charge.full");
    public static final RegistryObject<SoundEvent> RASENSHURIKEN_FLIGHT = register("rasenshuriken.flight");
    public static final RegistryObject<SoundEvent> RASENSHURIKEN_BLOOM = register("rasenshuriken.bloom");

    public static final RegistryObject<SoundEvent> LEAP = register("leap");

    public static final RegistryObject<SoundEvent> LONELY_MARCH = register("lonely_march");

    public static final RegistryObject<SoundEvent> DOUBLE_JUMP = register("double_jump");

    public static final RegistryObject<SoundEvent> SHARINGAN_ACTIVATE = register("sharingan_activate");

    public static final RegistryObject<SoundEvent> CHIDORI = register("chidori");
    public static final RegistryObject<SoundEvent> RASENGAN_HIT_ENTITY = register("rasengan.hit_entity");
    public static final RegistryObject<SoundEvent> RASENGAN_HIT_BLOCK = register("rasengan.hit_block");

    // Tailed beasts. Six of the eight had a voice recorded for them in the 1.12.2 mod;
    // Kokuo and Saiken did not, and use the pooled roar rather than someone else's.
    public static final RegistryObject<SoundEvent> BIJU_SHUKAKU = register("biju.shukaku");
    public static final RegistryObject<SoundEvent> BIJU_MATATABI = register("biju.matatabi");
    public static final RegistryObject<SoundEvent> BIJU_ISOBU = register("biju.isobu");
    public static final RegistryObject<SoundEvent> BIJU_SON_GOKU = register("biju.son_goku");
    public static final RegistryObject<SoundEvent> BIJU_CHOMEI = register("biju.chomei");
    public static final RegistryObject<SoundEvent> BIJU_GYUKI = register("biju.gyuki");
    public static final RegistryObject<SoundEvent> BIJU_ROAR = register("biju.roar");
    public static final RegistryObject<SoundEvent> BIJU_HURT = register("biju.hurt");
    public static final RegistryObject<SoundEvent> BIJU_DEATH = register("biju.death");
    public static final RegistryObject<SoundEvent> BIJUDAMA = register("biju.bijudama");

    public static SoundEvent trigramBeat(int phase) {
        return TRIGRAM_BEATS.get(phase).get();
    }

    private static RegistryObject<SoundEvent> register(String key) {
        return SOUNDS.register(key, () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(MOD_ID, key)));
    }

    public static void register(IEventBus eventBus) {
        SOUNDS.register(eventBus);
    }
}
