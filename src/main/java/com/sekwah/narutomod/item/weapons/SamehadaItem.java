package com.sekwah.narutomod.item.weapons;

import com.sekwah.narutomod.capabilities.NinjaCapabilityHandler;
import com.sekwah.narutomod.item.NinjaTier;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import org.joml.Vector3f;

/**
 * Samehada — Kisame's sentient "shark skin" blade. It doesn't cut, it SHAVES: every hit
 * eats a chunk of the victim's chakra and feeds it to the wielder (chakra if they're a
 * ninja, a sliver of health either way — the blade shares its meal). The strongest
 * chakra-thief in the series, translated directly.
 */
public class SamehadaItem extends SwordItem {

    private static final float CHAKRA_SHAVED = 40f;
    private static final float CHAKRA_FED = 25f;
    private static final float HEAL_ON_FEED = 1.0f;
    private static final DustParticleOptions SHAVE_BLUE =
            new DustParticleOptions(new Vector3f(0.25F, 0.45F, 0.9F), 1.0F);

    public SamehadaItem(Properties properties) {
        super(NinjaTier.KATANA, 6, -2.8f, properties);
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!target.level().isClientSide) {
            // Shave the victim's chakra (if they have any) and feed the wielder
            target.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(targetData -> {
                float shaved = Math.min(CHAKRA_SHAVED, targetData.getChakra());
                if (shaved > 0) {
                    targetData.useChakra(shaved, 20);
                }
            });
            if (attacker instanceof Player player) {
                player.getCapability(NinjaCapabilityHandler.NINJA_DATA)
                        .ifPresent(wielderData -> wielderData.addChakra(CHAKRA_FED));
            }
            attacker.heal(HEAL_ON_FEED);

            if (target.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(SHAVE_BLUE,
                        target.getX(), target.getY() + target.getBbHeight() * 0.6, target.getZ(),
                        12, 0.3, 0.4, 0.3, 0.03);
            }
        }
        return super.hurtEnemy(stack, target, attacker);
    }
}
