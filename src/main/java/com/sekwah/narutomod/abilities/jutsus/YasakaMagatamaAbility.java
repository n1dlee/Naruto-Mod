package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.entity.jutsuprojectile.YasakaMagatamaEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Susanoo: Yasaka Magatama - three comma-shaped seals thrown at once.
 *
 * The Susanoo had exactly one attack, the sword, so a Complete Body standing across a field
 * from anything had nothing to do but walk. This is what the 1.12.2 Susanoo throws
 * (EntitySusanooClothed$EntityMagatama) and what the shell is known for at range.
 *
 * Three, because that is what the technique is: the name is the three-comma seal, and one
 * would be a different jutsu.
 */
public class YasakaMagatamaAbility extends Ability implements Ability.Cooldown {

    private static final float CHAKRA_COST = 45f;
    private static final int SEAL_COUNT = 3;
    /** Half-angle of the fan, in degrees. Wide enough to cover an approach lane. */
    private static final double SPREAD = 9.0;

    @Override
    public ActivationType activationType() {
        return ActivationType.INSTANT;
    }

    @Override
    public long defaultCombo() {
        return 1331;
    }

    @Override
    public int getCooldown() {
        return 5 * 20;
    }

    /** The throw is a full shoulder movement at Susanoo scale, not a flick. */
    @Override
    public int castPoseTicks() {
        return 14;
    }

    @Override
    public String requiredEye() {
        return "sharingan_ms";
    }

    @Override
    public SoundEvent castingSound() {
        return SoundEvents.AMETHYST_BLOCK_CHIME;
    }

    @Override
    public boolean handleCost(Player player, INinjaData ninjaData, int chargeAmount) {
        // Thrown BY the Susanoo, so there has to be one standing. Without this the technique
        // would be a free ranged attack for any Mangekyo holder, which is not what it is.
        if (!ninjaData.isSusanooActive()) {
            player.displayClientMessage(Component.literal("Susanoo is not manifested.")
                    .withStyle(ChatFormatting.DARK_PURPLE), true);
            return false;
        }
        if (ninjaData.getChakra() < CHAKRA_COST) {
            player.displayClientMessage(Component.translatable("jutsu.fail.notenoughchakra",
                    Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW)), true);
            return false;
        }
        ninjaData.useChakra(CHAKRA_COST, 20);
        return true;
    }

    @Override
    public void performServer(Player player, INinjaData ninjaData, int ticksActive) {
        int stage = Math.max(1, ninjaData.getSusanooStage());
        // Everything about the seal scales with the shell that threw it: a ribcage flicks
        // small ones, a Complete Body throws seals the size of a house.
        float scale = 1.2f + stage * 0.9f;
        float damage = (14f + stage * 7f) * ninjaData.getRankDamageMultiplier();
        double blast = 2.5 + stage * 0.9;

        Vec3 look = player.getLookAngle();
        Vec3 origin = player.getEyePosition().add(look.scale(stage >= 4 ? 8.0 : 1.5));

        for (int i = 0; i < SEAL_COUNT; i++) {
            // Fanned around the aim line rather than scattered, so the spread is a pattern
            // the target can read and try to move out of.
            double angle = Math.toRadians((i - (SEAL_COUNT - 1) / 2.0) * SPREAD);
            double cos = Math.cos(angle);
            double sin = Math.sin(angle);
            Vec3 aim = new Vec3(
                    look.x * cos - look.z * sin,
                    look.y,
                    look.z * cos + look.x * sin).normalize();

            final int index = i;
            // Staggered by a couple of ticks so they leave as a volley, not as one object.
            ninjaData.scheduleDelayedTickEvent(caster -> {
                YasakaMagatamaEntity seal = new YasakaMagatamaEntity(
                        caster, origin, aim, scale, susanooTint(ninjaData))
                        .damage(damage, blast);
                caster.level().addFreshEntity(seal);
                caster.level().playSound(null, caster, SoundEvents.AMETHYST_BLOCK_CHIME,
                        net.minecraft.sounds.SoundSource.PLAYERS, 1.2f, 1.1f + index * 0.08f);
            }, 2 + i * 2);
        }
    }

    /**
     * The shell's own colour.
     *
     * The player's Susanoo has no per-wielder colour the way the bosses do, so this is the
     * mod's standard violet. Kept as a method rather than a constant because that is the
     * thing most likely to become configurable, and the call site should not have to change.
     */
    private static int susanooTint(INinjaData ninjaData) {
        return 0x8C40D9;
    }
}
