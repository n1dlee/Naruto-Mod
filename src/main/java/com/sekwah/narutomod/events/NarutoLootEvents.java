package com.sekwah.narutomod.events;

import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.item.NarutoItems;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraftforge.event.LootTableLoadEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.RegistryObject;

import java.util.List;
import java.util.Set;

/**
 * Scatters jutsu scrolls through the world's chests.
 *
 * Every scroll is still craftable — this is a parallel path, not a replacement. The point
 * is that a ninja should be able to *find* techniques out in the world instead of only
 * ever buying them from a crafting grid, and that where you find one should say something
 * about how dangerous it is.
 *
 * Four tiers, each pointed at structures that match its difficulty:
 *   basic     - academy fundamentals, turn up wherever ordinary people live or travel
 *   advanced  - real combat ninjutsu, guarded ruins and strongholds
 *   elite     - techniques a village would not hand out, end-game structures
 *   forbidden - the Flying Thunder God, sealed in the two nastiest places in the game
 *
 * Injected via LootTableLoadEvent rather than a datapack because the mod ships no loot
 * tables of its own, and this leaves vanilla's tables untouched on disk.
 */
@Mod.EventBusSubscriber(modid = NarutoMod.MOD_ID)
public class NarutoLootEvents {

    // --- Where ordinary people keep things: villages, travellers, shipwrecks ---
    private static final Set<ResourceLocation> BASIC_CHESTS = Set.of(
            new ResourceLocation("chests/village/village_armorer"),
            new ResourceLocation("chests/village/village_weaponsmith"),
            new ResourceLocation("chests/village/village_toolsmith"),
            new ResourceLocation("chests/village/village_cartographer"),
            new ResourceLocation("chests/village/village_temple"),
            new ResourceLocation("chests/ruined_portal"),
            new ResourceLocation("chests/shipwreck_supply"),
            new ResourceLocation("chests/igloo_chest"),
            new ResourceLocation("chests/spawn_bonus_chest"));

    // --- Places somebody deliberately hid something and left guards ---
    private static final Set<ResourceLocation> ADVANCED_CHESTS = Set.of(
            new ResourceLocation("chests/simple_dungeon"),
            new ResourceLocation("chests/abandoned_mineshaft"),
            new ResourceLocation("chests/desert_pyramid"),
            new ResourceLocation("chests/jungle_temple"),
            new ResourceLocation("chests/stronghold_corridor"),
            new ResourceLocation("chests/stronghold_crossing"),
            new ResourceLocation("chests/underwater_ruin_big"),
            new ResourceLocation("chests/pillager_outpost"),
            new ResourceLocation("chests/shipwreck_treasure"));

    // --- End-game: you had to fight your way in ---
    private static final Set<ResourceLocation> ELITE_CHESTS = Set.of(
            new ResourceLocation("chests/stronghold_library"),
            new ResourceLocation("chests/woodland_mansion"),
            new ResourceLocation("chests/nether_bridge"),
            new ResourceLocation("chests/end_city_treasure"),
            new ResourceLocation("chests/buried_treasure"),
            new ResourceLocation("chests/bastion_bridge"),
            new ResourceLocation("chests/bastion_hoglin_stable"));

    // --- The two nastiest places in the game, for the one forbidden technique ---
    private static final Set<ResourceLocation> FORBIDDEN_CHESTS = Set.of(
            new ResourceLocation("chests/ancient_city"),
            new ResourceLocation("chests/ancient_city_ice_box"),
            new ResourceLocation("chests/bastion_treasure"),
            new ResourceLocation("chests/bastion_other"));

    /** Academy fundamentals — cheap to craft too, so these are the common finds. */
    private static final List<RegistryObject<Item>> BASIC_SCROLLS = List.of(
            NarutoItems.SCROLL_SHADOW_CLONE, NarutoItems.SCROLL_EARTH_WALL,
            NarutoItems.SCROLL_FIREBALL, NarutoItems.SCROLL_WATER_BULLET,
            NarutoItems.SCROLL_GREAT_BREAKTHROUGH, NarutoItems.SCROLL_FALSE_DARKNESS);

    private static final List<RegistryObject<Item>> ADVANCED_SCROLLS = List.of(
            NarutoItems.SCROLL_EARTH_SPIKES, NarutoItems.SCROLL_WATER_DRAGON,
            NarutoItems.SCROLL_CHIDORI, NarutoItems.SCROLL_KUCHIYOSE,
            NarutoItems.SCROLL_MULTIPLE_SHADOW_CLONE, NarutoItems.SCROLL_RASENGAN);

    private static final List<RegistryObject<Item>> ELITE_SCROLLS = List.of(
            NarutoItems.SCROLL_CHIDORI_DASH, NarutoItems.SCROLL_CHIDORI_NAGASHI,
            NarutoItems.SCROLL_RASENSHURIKEN, NarutoItems.SCROLL_EIGHT_GATES,
            NarutoItems.SCROLL_SAGE_MODE);

    private static final List<RegistryObject<Item>> FORBIDDEN_SCROLLS = List.of(
            NarutoItems.SCROLL_FLYING_THUNDER_GOD);

    /**
     * Per-chest odds, deliberately low enough that a scroll is a find rather than clutter,
     * and lower the stronger the tier — an Eight Gates scroll should be a story, not a
     * routine pickup.
     */
    private static final float CHANCE_BASIC = 0.22f;
    private static final float CHANCE_ADVANCED = 0.16f;
    private static final float CHANCE_ELITE = 0.10f;
    private static final float CHANCE_FORBIDDEN = 0.18f;

    @SubscribeEvent
    public static void onLootLoad(LootTableLoadEvent event) {
        ResourceLocation table = event.getName();
        if (BASIC_CHESTS.contains(table)) {
            addScrollPool(event, BASIC_SCROLLS, CHANCE_BASIC, "basic");
        } else if (ADVANCED_CHESTS.contains(table)) {
            addScrollPool(event, ADVANCED_SCROLLS, CHANCE_ADVANCED, "advanced");
        } else if (ELITE_CHESTS.contains(table)) {
            addScrollPool(event, ELITE_SCROLLS, CHANCE_ELITE, "elite");
        } else if (FORBIDDEN_CHESTS.contains(table)) {
            addScrollPool(event, FORBIDDEN_SCROLLS, CHANCE_FORBIDDEN, "forbidden");
        }
    }

    /**
     * One pool per tier holding every scroll of that tier at equal weight, gated behind a
     * single chance roll. That way a chest yields at most one scroll and which one it is
     * stays a surprise, instead of each scroll rolling independently and occasionally
     * dumping the whole tier into one chest.
     */
    private static void addScrollPool(LootTableLoadEvent event, List<RegistryObject<Item>> scrolls,
                                      float chance, String tierName) {
        LootPool.Builder pool = LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1))
                .when(LootItemRandomChanceCondition.randomChance(chance))
                .name(NarutoMod.MOD_ID + "_" + tierName + "_scroll");
        for (RegistryObject<Item> scroll : scrolls) {
            pool.add(LootItem.lootTableItem(scroll.get())
                    .apply(SetItemCountFunction.setCount(ConstantValue.exactly(1)))
                    .setWeight(1));
        }
        event.getTable().addPool(pool.build());
    }
}
