package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.config.NarutoConfig;
import com.sekwah.narutomod.entity.NarutoEntities;
import com.sekwah.narutomod.entity.ShadowCloneEntity;
import com.sekwah.narutomod.util.NarutoParticles;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Explosion Release: Suicide Bombing Clone - a double of the caster that walks in and
 * goes off.
 *
 * Reuses the ordinary shadow clone entity rather than introducing a second doppelganger
 * type: to everyone on the receiving end it is a shadow clone right up until it is not,
 * and that is exactly the trick the technique trades on. The fuse is long enough to be
 * read and reacted to.
 */
public class ExplosiveCloneAbility extends Ability implements Ability.Cooldown {

    private static final float CHAKRA_COST = 85f;
    private static final int FUSE_TICKS = 60;
    private static final float BLAST_RADIUS = 4.0f;

    @Override
    public ActivationType activationType() {
        return ActivationType.INSTANT;
    }

    @Override
    public long defaultCombo() {
        return 2322;
    }

    @Override
    public String element() {
        return "earth";
    }

    @Override
    public int elementLevelRequired() {
        return 10;
    }

    @Override
    public String secondaryElement() {
        return "fire";
    }

    @Override
    public int secondaryElementLevelRequired() {
        return 10;
    }

    @Override
    public int getCooldown() {
        return 30 * 20;
    }

    @Override
    public SoundEvent castingSound() {
        return SoundEvents.CREEPER_PRIMED;
    }

    @Override
    public boolean handleCost(Player player, INinjaData ninjaData, int chargeAmount) {
        if (ninjaData.getChakra() < CHAKRA_COST) {
            player.displayClientMessage(Component.translatable("jutsu.fail.notenoughchakra",
                    Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW)), true);
            return false;
        }
        ninjaData.useChakra(CHAKRA_COST, 40);
        return true;
    }

    @Override
    public void performServer(Player player, INinjaData ninjaData, int ticksActive) {
        ShadowCloneEntity clone = new ShadowCloneEntity(NarutoEntities.SHADOW_CLONE.get(), player.level());
        clone.setOwner(player);
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = player.getItemBySlot(slot);
            if (!stack.isEmpty()) {
                clone.setItemSlot(slot, stack.copy());
            }
        }
        Vec3 look = player.getLookAngle();
        Vec3 forward = new Vec3(look.x, 0, look.z).normalize();
        clone.setPos(player.getX() + forward.x * 1.5, player.getY(), player.getZ() + forward.z * 1.5);
        clone.setYRot(player.getYRot());
        clone.setYBodyRot(player.getYRot());
        clone.setYHeadRot(player.getYHeadRot());
        player.level().addFreshEntity(clone);

        if (player.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.POOF,
                    clone.getX(), clone.getY() + clone.getBbHeight() * 0.5, clone.getZ(),
                    20, 0.25, 0.35, 0.25, 0.05);
        }

        ninjaData.scheduleDelayedTickEvent(p -> detonate(p, clone), FUSE_TICKS);
    }

    /**
     * Goes off wherever the clone has walked to by now, not where it was created - the
     * clone is a mob with its own AI and will have moved.
     */
    private void detonate(Player player, ShadowCloneEntity clone) {
        if (!clone.isAlive()) {
            return; // killed before the fuse ran out; the trick failed, and that is fair
        }
        Vec3 at = clone.position();
        Level level = player.level();
        if (level instanceof ServerLevel serverLevel) {
            NarutoParticles.spawnBurst(serverLevel, at.add(0, 1, 0), 30, 0.6, NarutoParticles.CLAY_GREY);
        }
        clone.discard();
        level.explode(player, at.x, at.y + 1.0, at.z, BLAST_RADIUS,
                NarutoConfig.paperbombBlockDamage
                        ? Level.ExplosionInteraction.TNT
                        : Level.ExplosionInteraction.NONE);
    }
}
