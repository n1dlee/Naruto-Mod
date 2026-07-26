package com.sekwah.narutomod.item;

import com.sekwah.narutomod.item.armor.NarutoArmorItem;
import com.sekwah.narutomod.item.armor.NarutoArmorMaterial;
import com.sekwah.narutomod.item.weapons.*;
import com.sekwah.narutomod.sounds.NarutoSounds;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.*;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import static com.sekwah.narutomod.NarutoMod.MOD_ID;

public class NarutoItems {

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MOD_ID);

    public static final RegistryObject<Item> MODEL_TEST = ITEMS.register("model_test", ()
            -> new Item(new Item.Properties()));

    // Weapons
    public static final RegistryObject<Item> KUNAI = ITEMS.register("kunai", ()
            -> new KunaiItem(new Item.Properties()));

    public static final RegistryObject<Item> SENBON = ITEMS.register("senbon", ()
            -> new SenbonItem(new Item.Properties()));

    public static final RegistryObject<Item> EXPLOSIVE_KUNAI = ITEMS.register("explosive_kunai", ()
            -> new ExplosiveKunaiItem(new Item.Properties().stacksTo(16)));

    public static final RegistryObject<Item> SHURIKEN = ITEMS.register("shuriken", ()
            -> new ShurikenItem(new Item.Properties()));


    // Armor

    public static final RegistryObject<Item> RED_ANBU_MASK = ITEMS.register("red_anbu_mask", ()
            -> new NarutoArmorItem(NarutoArmorMaterial.ANBU_MAT, ArmorItem.Type.HELMET, new Item.Properties())
            .setShouldHideNameplate(true));

    public static final RegistryObject<Item> YELLOW_ANBU_MASK = ITEMS.register("yellow_anbu_mask", ()
            -> new NarutoArmorItem(NarutoArmorMaterial.ANBU_MAT, ArmorItem.Type.HELMET, new Item.Properties())
            .setShouldHideNameplate(true));

    public static final RegistryObject<Item> GREEN_ANBU_MASK = ITEMS.register("green_anbu_mask", ()
            -> new NarutoArmorItem(NarutoArmorMaterial.ANBU_MAT, ArmorItem.Type.HELMET, new Item.Properties())
            .setShouldHideNameplate(true));

    public static final RegistryObject<Item> BLUE_ANBU_MASK = ITEMS.register("blue_anbu_mask", ()
            -> new NarutoArmorItem(NarutoArmorMaterial.ANBU_MAT, ArmorItem.Type.HELMET, new Item.Properties())
            .setShouldHideNameplate(true));

    public static final RegistryObject<Item> MIST_ANBU_MASK = ITEMS.register("mist_anbu_mask", ()
            -> new NarutoArmorItem(NarutoArmorMaterial.ANBU_MAT, ArmorItem.Type.HELMET, new Item.Properties())
            .setShouldHideNameplate(true));

    public static final RegistryObject<Item> FLAK_JACKET_NEW = ITEMS.register("flak_jacket_new", ()
            -> new NarutoArmorItem(NarutoArmorMaterial.FLAK_MAT, ArmorItem.Type.CHESTPLATE, new Item.Properties()));

    public static final RegistryObject<Item> FLAK_JACKET = ITEMS.register("flak_jacket", ()
            -> new NarutoArmorItem(NarutoArmorMaterial.FLAK_STRONGER_MAT, ArmorItem.Type.CHESTPLATE, new Item.Properties()));

    public static final RegistryObject<Item> ANBU_ARMOR = ITEMS.register("anbu_armor", ()
            -> new NarutoArmorItem(NarutoArmorMaterial.FLAK_STRONGER_MAT, ArmorItem.Type.CHESTPLATE, new Item.Properties()));

    public static final RegistryObject<Item> AKATSUKI_CLOAK = ITEMS.register("akatsuki_cloak", ()
            -> new NarutoArmorItem(NarutoArmorMaterial.CHARACTER_CLOTHES, ArmorItem.Type.CHESTPLATE, new Item.Properties()));

    public static final RegistryObject<Item> LEAF_CLOAK = ITEMS.register("leaf_cloak", ()
            -> new NarutoArmorItem(NarutoArmorMaterial.CHARACTER_CLOTHES, ArmorItem.Type.CHESTPLATE, new Item.Properties()));

    public static final RegistryObject<Item> SENJU_CLOAK = ITEMS.register("senju_cloak", ()
            -> new NarutoArmorItem(NarutoArmorMaterial.CHARACTER_CLOTHES, ArmorItem.Type.CHESTPLATE, new Item.Properties()));

    public static final RegistryObject<Item> LONELY_MARCH = ITEMS.register("lonely_march", ()
            -> new RecordItem(41, NarutoSounds.LONELY_MARCH, new Item.Properties().stacksTo(1).rarity(Rarity.RARE), 46 * 20));

    public static final RegistryObject<Item> FABRIC = ITEMS.register("fabric", ()
            -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> FABRIC_REINFORCED = ITEMS.register("fabric_reinforced", ()
            -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> FABRIC_REINFORCED_GREEN = ITEMS.register("fabric_reinforced_green", ()
            -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> FABRIC_REINFORCED_BLACK = ITEMS.register("fabric_reinforced_black", ()
            -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> ARMOR_PLATE = ITEMS.register("armor_plate", ()
            -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> ARMOR_PLATE_GREEN = ITEMS.register("armor_plate_green", ()
            -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> KATANA = ITEMS.register("katana", ()
            -> new SwordItem(NinjaTier.KATANA, 3, -1.95F, new Item.Properties()));

    public static final RegistryObject<Item> CHAKRA_BLADE = ITEMS.register("chakra_blade", ()
            -> new ChakraBladeItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> KUSANAGI = ITEMS.register("kusanagi", ()
            -> new KusanagiSwordItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> SMOKE_BOMB = ITEMS.register("smoke_bomb", ()
            -> new SmokeBombItem(new Item.Properties().stacksTo(16)));

    public static final RegistryObject<Item> SOLDIER_PILL = ITEMS.register("soldier_pill", ()
            -> new SoldierPillItem(new Item.Properties().stacksTo(16)));

    public static final RegistryObject<Item> FUMA_SHURIKEN = ITEMS.register("fuma_shuriken", ()
            -> new FumaShurikenItem(new Item.Properties()));

    public static final RegistryObject<Item> KUBIKIRIBOCHO = ITEMS.register("kubikiribocho", ()
            -> new KubikiribochoItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> SAMEHADA = ITEMS.register("samehada", ()
            -> new com.sekwah.narutomod.item.weapons.SamehadaItem(new Item.Properties().stacksTo(1)));

    // --- Phase 17: the rest of the Seven Swordsmen of the Mist, plus Madara's war fan ---
    public static final RegistryObject<Item> NUIBARI = ITEMS.register("nuibari", ()
            -> new NuibariItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> SHIBUKI = ITEMS.register("shibuki", ()
            -> new ShibukiItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> KABUTOWARI = ITEMS.register("kabutowari", ()
            -> new KabutowariItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> HIRAMEKAREI = ITEMS.register("hiramekarei", ()
            -> new HiramekareiItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> KIBA_BLADES = ITEMS.register("kiba_blades", ()
            -> new KibaBladesItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> GUNBAI = ITEMS.register("gunbai", ()
            -> new GunbaiItem(new Item.Properties().stacksTo(1)));

    /** Phase 20: the marked kunai Minato threw ahead of himself. */
    public static final RegistryObject<Item> HIRAISHIN_KUNAI = ITEMS.register("hiraishin_kunai", ()
            -> new HiraishinKunaiItem(new Item.Properties()));

    public static final RegistryObject<Item> BINGO_BOOK = ITEMS.register("bingo_book", ()
            -> new BingoBookItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> CHAKRA_PAPER = ITEMS.register("chakra_paper", ()
            -> new ChakraPaperItem(new Item.Properties().stacksTo(16)));

    /** Phase 16: rare drop from a defeated Mangekyo wielder — awakens the Rinnegan. */
    public static final RegistryObject<Item> RINNEGAN_EYE = ITEMS.register("rinnegan_eye", ()
            -> new RinneganEyeItem(new Item.Properties().stacksTo(1).rarity(net.minecraft.world.item.Rarity.EPIC)));

    // --- Phase 15 C: jutsu technique scrolls ---
    private static RegistryObject<Item> createScroll(String jutsuPath) {
        return ITEMS.register("scroll_" + jutsuPath, ()
                -> new JutsuScrollItem(jutsuPath, new Item.Properties().stacksTo(1)));
    }

    public static final RegistryObject<Item> SCROLL_FIREBALL = createScroll("fireball");
    public static final RegistryObject<Item> SCROLL_WATER_BULLET = createScroll("water_bullet");
    public static final RegistryObject<Item> SCROLL_WATER_DRAGON = createScroll("water_dragon");
    public static final RegistryObject<Item> SCROLL_EARTH_WALL = createScroll("earth_wall");
    public static final RegistryObject<Item> SCROLL_EARTH_SPIKES = createScroll("earth_spikes");
    public static final RegistryObject<Item> SCROLL_GREAT_BREAKTHROUGH = createScroll("great_breakthrough");
    public static final RegistryObject<Item> SCROLL_FALSE_DARKNESS = createScroll("false_darkness");
    public static final RegistryObject<Item> SCROLL_CHIDORI = createScroll("chidori");
    public static final RegistryObject<Item> SCROLL_CHIDORI_DASH = createScroll("chidori_dash");
    public static final RegistryObject<Item> SCROLL_CHIDORI_NAGASHI = createScroll("chidori_nagashi");
    public static final RegistryObject<Item> SCROLL_RASENSHURIKEN = createScroll("rasenshuriken");
    public static final RegistryObject<Item> SCROLL_RASENGAN = createScroll("rasengan");
    public static final RegistryObject<Item> SCROLL_SHADOW_CLONE = createScroll("shadow_clone");
    public static final RegistryObject<Item> SCROLL_MULTIPLE_SHADOW_CLONE = createScroll("multiple_shadow_clone");
    public static final RegistryObject<Item> SCROLL_FLYING_THUNDER_GOD = createScroll("flying_thunder_god");
    public static final RegistryObject<Item> SCROLL_EIGHT_GATES = createScroll("eight_gates");
    public static final RegistryObject<Item> SCROLL_KUCHIYOSE = createScroll("kuchiyose");
    public static final RegistryObject<Item> SCROLL_SAGE_MODE = createScroll("sage_mode");

    // Ninja Masks

    private static RegistryObject<Item> createHeadband(String itemName) {
        return ITEMS.register(itemName, ()
                -> new NarutoArmorItem(NarutoArmorMaterial.HEADBAND, ArmorItem.Type.HELMET, new Item.Properties()));
    }

    //    BLANK(0, "blankBlueHeadBand", "headband_blank);"),
    public static final RegistryObject<Item> HEADBAND_BLUE = createHeadband("headband_blue");
    //    BLANK_BLACK(1, "blankBlackHeadBand", "headband_black);"),
    public static final RegistryObject<Item> HEADBAND_BLACK = createHeadband("headband_black");
    //    BLANK_RED(2, "blankRedHeadBand", "headband_kred);"),
    public static final RegistryObject<Item> HEADBAND_RED = createHeadband("headband_red");
    //    CUSTARD(4, "custardHeadband", "headband_custard);"),
    public static final RegistryObject<Item> HEADBAND_CUSTARD = createHeadband("headband_custard");


    public static final RegistryObject<Item> HEADBAND_LEAF = createHeadband("headband_leaf");
    public static final RegistryObject<Item> HEADBAND_LEAF_SCRATCHED = createHeadband("headband_leaf_scratched");
    public static final RegistryObject<Item> HEADBAND_LEAF_BLACK = createHeadband("headband_leaf_black");
    public static final RegistryObject<Item> HEADBAND_LEAF_BLACK_SCRATCHED = createHeadband("headband_leaf_black_scratched");

    public static final RegistryObject<Item> HEADBAND_ROCK = createHeadband("headband_rock");
    public static final RegistryObject<Item> HEADBAND_ROCK_SCRATCHED = createHeadband("headband_rock_scratched");

    public static final RegistryObject<Item> HEADBAND_SAND = createHeadband("headband_sand");
    public static final RegistryObject<Item> HEADBAND_SAND_SCRATCHED = createHeadband("headband_sand_scratched");

    public static final RegistryObject<Item> HEADBAND_SOUND = createHeadband("headband_sound");
    public static final RegistryObject<Item> HEADBAND_SOUND_SCRATCHED = createHeadband("headband_sound_scratched");

    public static final RegistryObject<Item> HEADBAND_MIST = createHeadband("headband_mist");
    public static final RegistryObject<Item> HEADBAND_MIST_SCRATCHED = createHeadband("headband_mist_scratched");

    public static final RegistryObject<Item> HEADBAND_WATERFALL = createHeadband("headband_waterfall");
    public static final RegistryObject<Item> HEADBAND_WATERFALL_SCRATCHED = createHeadband("headband_waterfall_scratched");

    public static final RegistryObject<Item> HEADBAND_CLOUD = createHeadband("headband_cloud");
    public static final RegistryObject<Item> HEADBAND_CLOUD_SCRATCHED = createHeadband("headband_cloud_scratched");

    public static final RegistryObject<Item> HEADBAND_RAIN = createHeadband("headband_rain");
    public static final RegistryObject<Item> HEADBAND_RAIN_SCRATCHED = createHeadband("headband_rain_scratched");

    public static final RegistryObject<Item> HEADBAND_GRASS = createHeadband("headband_grass");
    public static final RegistryObject<Item> HEADBAND_GRASS_SCRATCHED = createHeadband("headband_grass_scratched");


    public static final RegistryObject<Item> HEADBAND_PRIDE = createHeadband("headband_pride");
    public static final RegistryObject<Item> HEADBAND_YOUTUBE = createHeadband("headband_youtube");
    public static final RegistryObject<Item> HEADBAND_LAVA = createHeadband("headband_lava");


    // Materials

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }

}
