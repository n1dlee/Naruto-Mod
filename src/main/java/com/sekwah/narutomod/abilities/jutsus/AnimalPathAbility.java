package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.entity.NarutoEntities;
import com.sekwah.narutomod.entity.SummonBeastEntity;
import com.sekwah.narutomod.entity.SummonBeastVariant;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Animal Path — mass summoning (combo 1321).
 * Where an ordinary summoner is bound to one contract, the Animal Path calls several
 * beasts at once and needs no blood. Reuses the existing kuchiyose beasts rather than
 * inventing new creatures — the Rinnegan's edge here is quantity, not new species.
 */
public class AnimalPathAbility extends Ability implements Ability.Cooldown {

    private static final float CHAKRA_COST = 100f;
    private static final int BEAST_COUNT = 3;

    @Override
    public ActivationType activationType() {
        return ActivationType.INSTANT;
    }

    @Override
    public long defaultCombo() {
        return 1321;
    }

    @Override
    public String requiredEye() {
        return "rinnegan_path:animal";
    }

    @Override
    public int getCooldown() {
        return 120 * 20;
    }

    @Override
    public SoundEvent castingSound() {
        return SoundEvents.RAVAGER_ROAR;
    }

    @Override
    public boolean handleCost(Player player, INinjaData ninjaData, int chargeAmount) {
        if (ninjaData.getChakra() < CHAKRA_COST) {
            player.displayClientMessage(Component.translatable("jutsu.fail.notenoughchakra",
                    Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW)), true);
            return false;
        }
        ninjaData.useChakra(CHAKRA_COST, 60);
        return true;
    }

    @Override
    public void performServer(Player player, INinjaData ninjaData, int ticksActive) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        Vec3 look = player.getLookAngle();
        Vec3 forward = new Vec3(look.x, 0, look.z).normalize();

        for (int i = 0; i < BEAST_COUNT; i++) {
            SummonBeastEntity beast = NarutoEntities.SUMMON_BEAST.get().create(serverLevel);
            if (beast == null) {
                continue;
            }
            // Fan the beasts out in an arc in front of the summoner
            double angle = Math.toRadians(-40 + i * (80.0 / Math.max(1, BEAST_COUNT - 1)));
            Vec3 offset = forward.yRot((float) angle).scale(3.5);
            Vec3 spawnPos = player.position().add(offset);

            // One of each contract, in enum order — the Animal Path's whole point is that it
            // is not bound to a single one.
            SummonBeastVariant variant = SummonBeastVariant.byId(i % SummonBeastVariant.values().length);
            beast.setPos(spawnPos.x, player.getY(), spawnPos.z);
            beast.setOwner(player);
            beast.setVariant(variant);
            beast.setHealth(beast.getMaxHealth());
            beast.setCustomName(Component.literal(variant.getDisplayName()));
            serverLevel.addFreshEntity(beast);

            serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE,
                    spawnPos.x, player.getY() + 1.0, spawnPos.z, 40, 1.2, 1.0, 1.2, 0.07);
        }
        serverLevel.sendParticles(ParticleTypes.POOF,
                player.getX(), player.getY() + 0.5, player.getZ(), 30, 1.5, 0.8, 1.5, 0.05);
    }
}
