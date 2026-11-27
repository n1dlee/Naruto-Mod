package com.sekwah.narutomod.util;

import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.events.HiraishinEffects;
import com.sekwah.narutomod.item.NarutoItems;
import com.sekwah.narutomod.util.HiraishinTargeting.Glyph;
import com.sekwah.narutomod.util.HiraishinTargeting.Kind;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public final class HiraishinSealing {
    private HiraishinSealing() {
    }

    public static boolean clear(ServerPlayer actor, INinjaData data) {
        if (data.getHiraishinFormulas().equals(HiraishinFormula.Book.EMPTY)) {
            HiraishinEffects.message(actor, "hiraishin.clear.none");
        } else {
            data.setHiraishinFormulas(HiraishinFormula.Book.EMPTY);
            HiraishinEffects.clear(actor);
            HiraishinEffects.message(actor, "hiraishin.clear.done");
        }
        return true;
    }

    public static boolean mark(ServerPlayer actor, INinjaData data) {
        if (!actor.isAlive() || actor.isSpectator()) {
            return false;
        }
        for (InteractionHand hand : InteractionHand.values()) {
            if (actor.getItemInHand(hand).is(NarutoItems.KUNAI.get())) {
                return markKunai(actor, hand);
            }
        }
        var anchor = new HiraishinFormula.Anchor(actor.getUUID(), actor.level().dimension().location());
        var target = HiraishinTargeting.brandTarget(actor);
        if (target != null) {
            data.setHiraishinFormulas(data.getHiraishinFormulas().withBrand(new HiraishinFormula.Brand(anchor, target.getUUID())));
            var glyph = new Glyph(Kind.CREATURE,
                    target.position().add(0, target.getBbHeight() * 0.72, 0), target.getDirection(), target.getId());
            HiraishinEffects.mark(actor, glyph);
            HiraishinEffects.message(actor, "hiraishin.seal.entity", target.getDisplayName());
            return true;
        }
        return markGround(actor, data, anchor);
    }

    private static boolean markGround(ServerPlayer actor, INinjaData data, HiraishinFormula.Anchor anchor) {
        var ground = HiraishinTargeting.markGround(actor, anchor);
        if (ground == null) {
            HiraishinEffects.message(actor, "hiraishin.fail.support");
            return false;
        }
        data.setHiraishinFormulas(data.getHiraishinFormulas().withGround(ground));
        var pos = ground.support();
        Vec3 at = new Vec3(pos.getX() + 0.5, actor.getY(), pos.getZ() + 0.5);
        HiraishinEffects.mark(actor, new Glyph(Kind.GROUND, at, Direction.UP, -1));
        HiraishinEffects.message(actor, "hiraishin.seal.position", pos.getX(), pos.getY(), pos.getZ());
        return true;
    }

    private static boolean markKunai(ServerPlayer actor, InteractionHand hand) {
        ItemStack held = actor.getItemInHand(hand);
        ItemStack marked = new ItemStack(NarutoItems.HIRAISHIN_KUNAI.get());
        marked.getOrCreateTag().putUUID(HiraishinFormula.KUNAI_OWNER, actor.getUUID());
        if (held.getCount() == 1) {
            actor.setItemInHand(hand, marked);
        } else {
            if (!actor.getInventory().add(marked) && actor.drop(marked, false) == null) {
                HiraishinEffects.message(actor, "hiraishin.fail.item");
                return false;
            }
            held.shrink(1);
        }
        Vec3 at = actor.getEyePosition().add(actor.getLookAngle().scale(0.55)).add(0, -0.36, 0);
        HiraishinEffects.mark(actor, new Glyph(Kind.KUNAI, at, actor.getDirection().getOpposite(), -1));
        HiraishinEffects.message(actor, "hiraishin.seal.kunai");
        return true;
    }
}
