package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.entity.NarutoEntities;
import com.sekwah.narutomod.entity.WoodGolemEntity;
import com.sekwah.narutomod.util.NarutoParticles;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Wood Release: Wood Golem - toggled, because a golem is something you keep standing, not
 * something you fire.
 *
 * Holding it costs chakra every tick, and the moment the reserve runs dry the technique
 * fails its own cost check and the toggle framework tears it down for us - which is why
 * the upkeep here is the whole balance lever. Toggling off, or dying, dissolves it too.
 */
public class WoodGolemAbility extends Ability implements Ability.Toggled, Ability.ToggleStartCheck,
        Ability.HandleEnded {

    /** Summoning is the expensive part; keeping it up is a steady bleed. */
    private static final float SUMMON_COST = 200f;
    private static final float UPKEEP_PER_TICK = 2.5f;

    @Override
    public ActivationType activationType() {
        return ActivationType.TOGGLE;
    }

    @Override
    public long defaultCombo() {
        return 3222;
    }

    @Override
    public String requiredClan() {
        return "senju";
    }

    @Override
    public String element() {
        return "earth";
    }

    @Override
    public int elementLevelRequired() {
        return 12;
    }

    @Override
    public String secondaryElement() {
        return "water";
    }

    @Override
    public int secondaryElementLevelRequired() {
        return 12;
    }

    @Override
    public SoundEvent castingSound() {
        return SoundEvents.WOOD_PLACE;
    }

    @Override
    public boolean canStartToggle(Player player, INinjaData ninjaData) {
        if (ninjaData.getChakra() < SUMMON_COST) {
            player.displayClientMessage(Component.translatable("jutsu.fail.notenoughchakra",
                    Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW)), true);
            return false;
        }
        ninjaData.useChakra(SUMMON_COST, 60);
        summon(player);
        return true;
    }

    @Override
    public boolean handleCost(Player player, INinjaData ninjaData, int chargeAmount) {
        if (ninjaData.getChakra() < UPKEEP_PER_TICK) {
            return false; // toggle framework calls handleAbilityEnded for us
        }
        ninjaData.useChakra(UPKEEP_PER_TICK, 10);
        return true;
    }

    @Override
    public void performServer(Player player, INinjaData ninjaData, int ticksActive) {
        // Nothing to do: the golem is an autonomous entity once raised.
    }

    @Override
    public void performToggleClient(Player player, INinjaData ninjaData) {
        // The golem does its own rendering; the caster needs no extra effect.
    }

    @Override
    public void handleAbilityEnded(Player player, INinjaData ninjaData, int ticksActive) {
        dissolveOwned(player);
    }

    private void summon(Player player) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        // Raised just in front of the summoner, facing the same way they are.
        Vec3 look = player.getLookAngle();
        Vec3 forward = new Vec3(look.x, 0, look.z).normalize();
        Vec3 spawn = player.position().add(forward.scale(3.0));

        WoodGolemEntity golem = new WoodGolemEntity(NarutoEntities.WOOD_GOLEM.get(), serverLevel);
        golem.setPos(spawn.x, player.getY(), spawn.z);
        golem.setYRot(player.getYRot());
        golem.setOwner(player);
        serverLevel.addFreshEntity(golem);

        NarutoParticles.spawnRing(serverLevel, spawn, 2.5, 40, NarutoParticles.LOG_BROWN);
        serverLevel.playSound(null, golem.blockPosition(), SoundEvents.AZALEA_PLACE,
                net.minecraft.sounds.SoundSource.PLAYERS, 2.0f, 0.5f);
    }

    /**
     * Takes down this player's golem. Searched by owner rather than tracked by id so a
     * golem can never be orphaned by a relog or a chunk unload mid-summon.
     */
    private void dissolveOwned(Player player) {
        if (player.level().isClientSide) {
            return;
        }
        for (WoodGolemEntity golem : player.level().getEntitiesOfClass(WoodGolemEntity.class,
                new AABB(player.blockPosition()).inflate(96.0), g -> g.isOwnedBy(player))) {
            golem.dissolve();
        }
    }
}
