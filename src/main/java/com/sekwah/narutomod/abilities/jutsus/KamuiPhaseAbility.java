package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.util.NarutoParticles;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import org.joml.Vector3f;

/**
 * Kamui: Intangibility - Obito's signature Mangekyo state (combo 1212, TOGGLE).
 *
 * The user shifts part of themselves into the Kamui dimension, so nothing in this world
 * can touch them: attacks pass through (applied centrally in PlayerEvents, which reads the
 * toggle straight off the ability set) and so does terrain - they drift through walls and
 * floors exactly as in the anime.
 *
 * Deliberately NOT implemented as spectator mode, which would make the wielder invisible to
 * everyone else. Obito phasing through a wall is something the people fighting him can see
 * and be unsettled by, and that is the whole drama of the technique. So instead the entity
 * keeps its normal rendering and only its collision is switched off: noPhysics for terrain,
 * plus flight so there is a way to move once gravity has nothing to stand on.
 *
 * Bought with a heavy continuous chakra drain.
 */
public class KamuiPhaseAbility extends Ability
        implements Ability.Toggled, Ability.ToggleStartCheck, Ability.HandleEnded {

    /** Exempt from the free-hands gate: this is a body state, not a hand-cast technique. */
    @Override
    public boolean requiresFreeHands() {
        return false;
    }

    /** Per-tick chakra drain — deliberately steep, this is total damage immunity. */
    private static final float CHAKRA_COST = 12f;
    private static final DustParticleOptions KAMUI_VIOLET =
            new DustParticleOptions(new Vector3f(0.45F, 0.15F, 0.65F), 1.2F);

    @Override
    public ActivationType activationType() {
        return ActivationType.TOGGLE;
    }

    @Override
    public long defaultCombo() {
        return 1212;
    }

    @Override
    public String requiredEye() {
        return "sharingan_ms";
    }

    @Override
    public String requiredEyeForm() {
        return "obito";
    }

    @Override
    public SoundEvent castingSound() {
        return SoundEvents.SHULKER_TELEPORT;
    }

    @Override
    public boolean canStartToggle(Player player, INinjaData ninjaData) {
        return validateChakra(player, ninjaData);
    }

    @Override
    public boolean handleCost(Player player, INinjaData ninjaData, int chargeAmount) {
        if (!validateChakra(player, ninjaData)) {
            return false;
        }
        ninjaData.useChakra(CHAKRA_COST, 5);
        return true;
    }

    @Override
    public void performServer(Player player, INinjaData ninjaData, int ticksActive) {
        // Damage immunity is applied in PlayerEvents.livingHurt, and the terrain phasing is
        // driven by PlayerEvents.reconcileKamuiPhasing so that both sides agree.
    }

    @Override
    public void handleAbilityEnded(Player player, INinjaData ninjaData, int ticksActive) {
        // Server-side immediacy; the per-tick reconcile in PlayerEvents is what makes the
        // client agree, and what repairs the state after a relog or a death.
        clearPhasing(player);
    }

    /**
     * Switches off block collision and grants flight.
     *
     * noPhysics is what actually lets the player move through terrain: Entity.move consults
     * it client-side, and ServerGamePacketListenerImpl consults it before rubber-banding a
     * player who appears to be inside a wall - without it the server would drag them back
     * out every tick. It also makes Entity.isInWall report false, so phasing through stone
     * does not suffocate you.
     *
     * Flight comes with it out of necessity: with collision gone, gravity would drop the
     * wielder straight through the floor and into the void with no way back up.
     */
    public static void applyPhasing(Player player) {
        player.noPhysics = true;

        // Creative flight, not hand-rolled anti-gravity.
        //
        // This used to be noGravity(true) plus a one-off flight grant, and it produced both
        // of the bugs players hit. Flight was only granted when mayfly was previously false,
        // so anyone who already had it - notably on the way back out of the Kamui dimension -
        // never got the flying flag set at all. And LocalPlayer.aiStep clears flying the
        // instant it thinks you are standing on something, which happens on the very tick the
        // toggle goes up. Either way you ended up phased, not flying, and with gravity that
        // the mod had switched off in name only: the moment anything cleared noGravity you
        // free-fell through the floor, through bedrock, and out of the world. Slowness made
        // it look instant because it strips the horizontal drift that used to hide it.
        //
        // Vanilla creative flight already means exactly what this technique wants: no falling,
        // space to rise, shift to sink, normal steering. Letting it own the vertical axis and
        // leaving gravity alone removes the whole failure mode.
        var abilities = player.getAbilities();
        boolean changed = false;
        if (!abilities.mayfly) {
            abilities.mayfly = true;
            changed = true;
        }
        if (!abilities.flying) {
            abilities.flying = true;
            changed = true;
        }
        if (changed) {
            player.onUpdateAbilities();
        }
        // Nothing is being stood on while phased, and saying so stops vanilla's
        // landing check from switching the flight back off a tick later.
        player.setOnGround(false);
        if (player.isNoGravity()) {
            player.setNoGravity(false);
        }
        player.fallDistance = 0f;
    }

    public static void clearPhasing(Player player) {
        player.noPhysics = false;
        player.setNoGravity(false);
        player.fallDistance = 0f;
        // Never strip flight from someone who is entitled to it anyway, or a creative-mode
        // player would lose the ability to fly by using a jutsu.
        if (!player.isCreative() && !player.isSpectator()) {
            player.getAbilities().mayfly = false;
            player.getAbilities().flying = false;
            player.onUpdateAbilities();
        }
    }

    @Override
    public void performToggleClient(Player player, INinjaData ninjaData) {
        if (player.tickCount % 3 == 0) {
            player.level().addParticle(KAMUI_VIOLET,
                    player.getX() + (player.getRandom().nextDouble() - 0.5) * 0.7,
                    player.getY() + player.getRandom().nextDouble() * player.getBbHeight(),
                    player.getZ() + (player.getRandom().nextDouble() - 0.5) * 0.7,
                    0.0D, 0.02D, 0.0D);
        }
        if (player.tickCount % 20 == 0) {
            player.level().addParticle(NarutoParticles.SHARINGAN_RED,
                    player.getX(), player.getEyeY() - 0.1D, player.getZ(), 0.0D, 0.0D, 0.0D);
        }
    }

    private boolean validateChakra(Player player, INinjaData ninjaData) {
        if (ninjaData.getChakra() < CHAKRA_COST) {
            player.displayClientMessage(Component.translatable("jutsu.fail.notenoughchakra",
                    Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW)), true);
            return false;
        }
        return true;
    }
}
