package com.sekwah.narutomod.item.weapons;

import com.sekwah.narutomod.entity.projectile.FumaShurikenEntity;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Fuma Shuriken — a heavier windmill shuriken that returns to the thrower after
 * reaching max range instead of embedding in the ground.
 */
public class FumaShurikenItem extends Item {

    public FumaShurikenItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level worldIn, Player playerIn, InteractionHand handIn) {
        ItemStack usedItem = playerIn.getItemInHand(handIn);
        playerIn.getCooldowns().addCooldown(this, 20);

        worldIn.playSound(null, playerIn.getX(), playerIn.getY(), playerIn.getZ(), SoundEvents.ARROW_SHOOT,
                SoundSource.PLAYERS, 1.0F, 1.0F / (worldIn.getRandom().nextFloat() * 0.4F + 1.2F) + 0.5F);

        if (!worldIn.isClientSide) {
            FumaShurikenEntity entity = new FumaShurikenEntity(worldIn, playerIn);
            entity.pickup = playerIn.getAbilities().instabuild ? AbstractArrow.Pickup.CREATIVE_ONLY : AbstractArrow.Pickup.ALLOWED;
            entity.shootFromRotation(playerIn, playerIn.getXRot(), playerIn.getYRot(), 0.0F, 3.0F, 1.0F);
            entity.setBaseDamage(5.0);
            worldIn.addFreshEntity(entity);
        }

        if (!playerIn.getAbilities().instabuild) {
            usedItem.shrink(1);
        }

        return InteractionResultHolder.sidedSuccess(usedItem, worldIn.isClientSide);
    }
}
