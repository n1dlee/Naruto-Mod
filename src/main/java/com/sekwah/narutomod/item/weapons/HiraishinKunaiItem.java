package com.sekwah.narutomod.item.weapons;

import com.sekwah.narutomod.entity.projectile.HiraishinKunaiEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * A kunai carrying the Flying Thunder God seal — the marked blade Minato threw ahead of
 * himself. Identical to a normal kunai in the hand, but where it lands becomes a place he
 * can be, instantly.
 *
 * Made by casting the Flying Thunder God seal while holding a plain kunai; used by
 * throwing it and then triggering HiraishinTeleportAbility.
 */
public class HiraishinKunaiItem extends KunaiItem {

    public HiraishinKunaiItem(Properties properties) {
        super(properties);
    }

    @Override
    public AbstractArrow createShootingEntity(Level level, Player player) {
        HiraishinKunaiEntity entity = new HiraishinKunaiEntity(level, player);
        entity.pickup = player.getAbilities().instabuild
                ? AbstractArrow.Pickup.CREATIVE_ONLY
                : AbstractArrow.Pickup.ALLOWED;
        return entity;
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("weapon.hiraishin_kunai.tooltip").withStyle(ChatFormatting.GOLD));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
