package com.sekwah.narutomod.item.weapons;

import com.sekwah.narutomod.entity.projectile.HiraishinKunaiEntity;
import com.sekwah.narutomod.util.HiraishinFormula;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

public class HiraishinKunaiItem extends KunaiItem {

    public HiraishinKunaiItem(Properties properties) {
        super(properties);
    }

    @Override
    public HiraishinKunaiEntity createShootingEntity(Level level, Player player) {
        HiraishinKunaiEntity entity = new HiraishinKunaiEntity(level, player);
        entity.pickup = player.getAbilities().instabuild
                ? AbstractArrow.Pickup.CREATIVE_ONLY
                : AbstractArrow.Pickup.ALLOWED;
        return entity;
    }

    @Override
    protected HiraishinKunaiEntity createShootingEntity(Level level, Player player, ItemStack stack) {
        HiraishinKunaiEntity entity = createShootingEntity(level, player);
        var tag = stack.getTag();
        if (tag != null && tag.hasUUID(HiraishinFormula.KUNAI_OWNER)) {
            entity.bindFormula(tag.getUUID(HiraishinFormula.KUNAI_OWNER));
        }
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
