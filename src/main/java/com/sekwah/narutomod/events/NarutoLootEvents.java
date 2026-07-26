package com.sekwah.narutomod.events;

import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.item.NarutoItems;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraftforge.event.LootTableLoadEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Set;

/**
 * Seeds the world's rarest jutsu into vanilla loot.
 *
 * The Flying Thunder God is S-rank, so it deliberately cannot be bought, crafted or earned
 * by rank — the only copy is a scroll sealed away somewhere genuinely dangerous. Ancient
 * Cities and bastions fit: both are late-game, both are guarded, and neither is something
 * a new ninja stumbles into.
 *
 * Injected through LootTableLoadEvent rather than a datapack because the mod ships no loot
 * tables of its own, and this way vanilla's tables stay untouched on disk.
 */
@Mod.EventBusSubscriber(modid = NarutoMod.MOD_ID)
public class NarutoLootEvents {

    private static final ResourceLocation ANCIENT_CITY = new ResourceLocation("chests/ancient_city");
    private static final ResourceLocation ANCIENT_CITY_ICE_BOX = new ResourceLocation("chests/ancient_city_ice_box");
    private static final ResourceLocation BASTION_TREASURE = new ResourceLocation("chests/bastion_treasure");
    private static final ResourceLocation BASTION_OTHER = new ResourceLocation("chests/bastion_other");

    private static final Set<ResourceLocation> HIRAISHIN_CHESTS =
            Set.of(ANCIENT_CITY, ANCIENT_CITY_ICE_BOX, BASTION_TREASURE, BASTION_OTHER);

    /** Roughly one scroll per several chests — it should feel like a find, not a pickup. */
    private static final float SCROLL_CHANCE = 0.18f;

    @SubscribeEvent
    public static void onLootLoad(LootTableLoadEvent event) {
        if (!HIRAISHIN_CHESTS.contains(event.getName())) {
            return;
        }
        event.getTable().addPool(LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1))
                .add(LootItem.lootTableItem(NarutoItems.SCROLL_FLYING_THUNDER_GOD.get())
                        .apply(SetItemCountFunction.setCount(ConstantValue.exactly(1)))
                        .setWeight(1))
                .when(net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition
                        .randomChance(SCROLL_CHANCE))
                .name(NarutoMod.MOD_ID + "_hiraishin_scroll")
                .build());
    }
}
