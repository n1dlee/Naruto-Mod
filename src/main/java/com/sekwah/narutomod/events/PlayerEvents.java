package com.sekwah.narutomod.events;

import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.capabilities.NinjaCapabilityHandler;
import com.sekwah.narutomod.damagetypes.NarutoDamageTypes;
import com.sekwah.narutomod.entity.MangekyoBossEntity;
import com.sekwah.narutomod.entity.MangekyoBossVariant;
import com.sekwah.narutomod.util.PhasePhysics;
import com.sekwah.narutomod.util.SusanooForm;
import com.sekwah.narutomod.entity.RogueNinjaEntity;
import com.sekwah.narutomod.sounds.NarutoSounds;
import com.sekwah.narutomod.util.NarutoParticles;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerWakeUpEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector3f;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = NarutoMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class PlayerEvents {

    private static final UUID NINJA_HEALTH_MODIFIER_ID = UUID.fromString("d26b89a1-8dc2-4d13-a68e-fb10c2a5e95e");
    // Indexed by the 0-13 rank ladder (see INinjaData#getRankIndex), not the 0-4 base rank.
    // The old five-entry tables meant Jonin to Kage handed over twenty hearts and a third of
    // all incoming damage in one step; spread over three tiers it arrives in readable pieces.
    private static final double[] HEALTH_BONUS_VALUES = new double[] {
            0.0D,
            8.0D, 10.0D, 13.0D,
            16.0D, 20.0D, 24.0D,
            28.0D, 34.0D, 41.0D,
            48.0D, 58.0D, 70.0D,
            90.0D
    };
    /**
     * Incoming damage multiplier while running a Chidori with no Sharingan open.
     *
     * Canon reason: the speed is the danger to the user, and the eye is what lets them react
     * at it. Whether Chidori is castable without a Sharingan is a design choice; being free
     * to do so is not.
     */
    private static final float CHIDORI_TUNNEL_VISION = 1.35f;

    private static final float[] MOB_DAMAGE_MULTIPLIERS = new float[] {
            1.0F,
            0.90F, 0.88F, 0.85F,
            0.80F, 0.75F, 0.70F,
            0.65F, 0.61F, 0.56F,
            0.50F, 0.45F, 0.40F,
            0.32F
    };

    @SubscribeEvent
    public static void onEntityUpdate(LivingEvent.LivingTickEvent event) {
        if (event.getEntity() instanceof Player player) {
            // Runs before the ninja-mode gate on purpose: if the mode is switched off while
            // phased, the wielder still has to be put back into a solid body.
            reconcileKamuiPhasing(player);
            player.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(ninjaData -> {
                if (!ninjaData.isNinjaModeEnabled()) {
                    syncNinjaHealth(player, 0);
                    return;
                }
                applyRankSurvivability(player, ninjaData);
            });
        }
    }

    private static void reconcileKamuiPhasing(Player player) {
        var state = player.getCapability(NinjaCapabilityHandler.NINJA_DATA).resolve();
        boolean phaseRequested = state.filter(INinjaData::isNinjaModeEnabled)
                .map(data -> data.getToggleAbilityData().getAbilitiesHashSet().contains(KamuiPhaseEvents.ID)).orElse(false);
        boolean kamui = KamuiPhaseEvents.reconcile(player, phaseRequested);
        boolean completeBody = state.map(data -> data.isSusanooActive() && SusanooForm.perfect(data.getSusanooStage())).orElse(false);
        boolean pocket = com.sekwah.narutomod.world.KamuiDimension.isKamui(player.level());
        PhasePhysics.reconcile(player, kamui, pocket || completeBody);
        if (!player.isSpectator() && player.isNoGravity() && !state.map(INinjaData::isWallWalkAttached).orElse(false)) {
            player.setNoGravity(false);
        }
    }

    /**
     * Complete Body Susanoo cannot be staggered. A thirteen-block avatar being knocked back
     * by an arrow was the clearest sign that stage 4 was cosmetic - the whole point of the
     * final form is that nothing moves it.
     */
    @SubscribeEvent
    public static void onKnockback(net.minecraftforge.event.entity.living.LivingKnockBackEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        boolean immovable = player.getCapability(NinjaCapabilityHandler.NINJA_DATA)
                .map(data -> data.isNinjaModeEnabled()
                        && data.isSusanooActive() && SusanooForm.perfect(data.getSusanooStage()))
                .orElse(false);
        if (immovable) {
            event.setCanceled(true);
        }
    }

    private static void applyRankSurvivability(Player player, INinjaData ninjaData) {
        int rank = Math.min(Math.max(ninjaData.getNinjaRank(), 0), 4);
        int index = Math.min(Math.max(ninjaData.getRankIndex(), 0), HEALTH_BONUS_VALUES.length - 1);
        if (player.tickCount % 40 != 0) {
            return;
        }

        syncNinjaHealth(player, index);
        player.removeEffect(MobEffects.HEALTH_BOOST);

        if (rank >= 3) {
            int resistanceAmplifier = rank >= 4 ? 1 : 0;
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 80, resistanceAmplifier, false, false));
        }

        float healthRatio = player.getHealth() / player.getMaxHealth();
        if (rank >= 4 && healthRatio < 0.8F) {
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 80, 1, false, false));
        } else if (rank >= 3 && healthRatio < 0.7F) {
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 80, 0, false, false));
        } else if (rank >= 2 && healthRatio < 0.5F) {
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 80, 0, false, false));
        }
    }

    private static void syncNinjaHealth(Player player, int rankIndex) {
        AttributeInstance maxHealth = player.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth == null) {
            return;
        }

        double targetBonus = HEALTH_BONUS_VALUES[
                Math.min(Math.max(rankIndex, 0), HEALTH_BONUS_VALUES.length - 1)];
        AttributeModifier currentModifier = maxHealth.getModifier(NINJA_HEALTH_MODIFIER_ID);
        if (targetBonus <= 0.0D) {
            if (currentModifier != null) {
                maxHealth.removeModifier(NINJA_HEALTH_MODIFIER_ID);
                player.setHealth(Math.min(player.getHealth(), player.getMaxHealth()));
            }
            return;
        }

        if (currentModifier != null && Math.abs(currentModifier.getAmount() - targetBonus) < 0.01D) {
            return;
        }

        double oldMaxHealth = player.getMaxHealth();
        if (currentModifier != null) {
            maxHealth.removeModifier(NINJA_HEALTH_MODIFIER_ID);
        }
        maxHealth.addTransientModifier(new AttributeModifier(
                NINJA_HEALTH_MODIFIER_ID,
                "Naruto ninja rank health",
                targetBonus,
                AttributeModifier.Operation.ADDITION));

        if (player.getHealth() >= oldMaxHealth - 0.01F) {
            player.setHealth(player.getMaxHealth());
        }
    }

    // The old percentage-reduction table lived here. It is gone with the mechanic: the shell
    // has its own integrity now (NinjaData.getSusanooDurability) and absorbs damage outright
    // rather than discounting it.
    private static final float[] KURAMA_DAMAGE_REDUCTION = {0f, 0.10f, 0.10f, 0.10f, 0.30f, 0.30f, 0.30f, 0.30f, 0.50f, 0.80f}; // by tail count 0-9
    private static final float SHARINGAN_DANGER_SENSE_REDUCTION = 0.15f; // 3-tomoe Sharingan, see applyTransformationDamageSponge
    private static final float RINNEGAN_DROP_CHANCE = 0.15f; // per Mangekyo boss kill
    /**
     * Per Uchiha boss kill. Raised alongside the boss spawn weight coming down to 4: with
     * bosses this rare, a one-in-three roll meant most players would never see an eye from
     * the source that is supposed to be the canonical one.
     */
    private static final float SHARINGAN_EYE_DROP_CHANCE = 0.60f;

    /**
     * Kill rewards for the mod's own mobs. Flat values rather than health-derived ones so
     * the payout stays predictable when a variant's health is retuned: an S-rank is worth
     * an S-rank's XP regardless of which wielder it happened to be.
     *
     * Vanilla monsters deliberately keep the generic 10 + maxHealth/2 formula - grinding
     * zombies should stay far slower than hunting ninja.
     *
     * The boss payout is sized against the rank ladder rather than against a rogue: Jonin to
     * Kage is 35000, so 4000 puts a Kage roughly nine bosses away. At the old 1500 it was
     * twenty-three, which made the rarest fight in the mod worse value than three rogues.
     */
    private static final float BOSS_KILL_XP = 4000f;
    /** Chunin rogue. A Jonin is worth the multiplier below, and a clan rogue is a Jonin. */
    private static final float ROGUE_NINJA_KILL_XP = 200f;
    private static final float ROGUE_JONIN_XP_MULTIPLIER = 2.5f;



    /**
     * Rinne Sharingan — once a day, the wielder simply refuses to die: the killing blow
     * is cancelled and they are left standing at a sliver of health with a moment of
     * invulnerability to escape. Restoration, not resurrection.
     */
    @SubscribeEvent
    public static void onPhoenixSage(net.minecraftforge.event.entity.living.LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide) {
            return;
        }
        long worldDay = player.level().getDayTime() / 24000L;
        player.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(ninjaData -> {
            if (!ninjaData.isRinneSharinganAwakened() || !ninjaData.tryConsumePhoenixSageCharge(worldDay)) {
                return;
            }
            event.setCanceled(true);
            player.setHealth(1.0f);
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 5 * 20, 4, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 10 * 20, 1, false, true));
            player.displayClientMessage(
                    Component.translatable("rinne_sharingan.phoenix").withStyle(ChatFormatting.LIGHT_PURPLE), false);
            if (player.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.END_ROD,
                        player.getX(), player.getY() + 1.0, player.getZ(), 60, 0.6, 1.0, 0.6, 0.05);
            }
        });
    }

    /**
     * The Sharingan reads an attack a heartbeat before it arrives and the body steps out of
     * the way. Cancels the hit outright and throws the player clear — sideways off a
     * projectile or melee swing, straight up out of a blast. Rolls and costs are handled in
     * NinjaData.trySharinganDodge (chance scales 20/40/60% with tomoe, matching the 1.12.2
     * mod's 60% at full maturity, plus a chakra cost and a short cooldown so it thins
     * damage rather than granting immunity).
     *
     * Deliberately skips damage you cannot dodge by moving: falling, drowning, starving,
     * poison and the like — sidestepping your own suffocation would read as a bug.
     */
    private static void applySharinganDodge(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        DamageSource source = event.getSource();
        if (source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)
                || source.is(net.minecraft.tags.DamageTypeTags.IS_FALL)
                || source.is(net.minecraft.tags.DamageTypeTags.IS_DROWNING)
                || source.is(net.minecraft.tags.DamageTypeTags.IS_FIRE)
                || source.is(net.minecraft.world.damagesource.DamageTypes.STARVE)
                || source.is(net.minecraft.world.damagesource.DamageTypes.MAGIC)) {
            return;
        }
        player.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(ninjaData -> {
            if (!ninjaData.trySharinganDodge(player, event.getAmount())) {
                return;
            }
            event.setCanceled(true);

            // An explosion has no single direction to sidestep, so leap out of it instead.
            boolean explosion = source.is(net.minecraft.tags.DamageTypeTags.IS_EXPLOSION);
            Vec3 escape;
            if (explosion || source.getEntity() == null) {
                escape = new Vec3(0, 0.85, 0);
            } else {
                // Sidestep perpendicular to the incoming line, with a small hop.
                Vec3 fromAttacker = player.position().subtract(source.getEntity().position()).normalize();
                Vec3 sideways = new Vec3(-fromAttacker.z, 0, fromAttacker.x)
                        .scale(player.getRandom().nextBoolean() ? 0.9 : -0.9);
                escape = sideways.add(0, 0.35, 0);
            }
            player.setDeltaMovement(player.getDeltaMovement().add(escape));
            player.hurtMarked = true;
            player.resetFallDistance();

            player.displayClientMessage(
                    Component.translatable("sharingan.dodge").withStyle(ChatFormatting.AQUA), true);
            player.level().playSound(null, player.blockPosition(), NarutoSounds.JUTSU_CAST.get(),
                    SoundSource.PLAYERS, 0.4f, 1.8f);
            if (player.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(NarutoParticles.SHARINGAN_RED,
                        player.getX(), player.getY() + 1.0, player.getZ(), 12, 0.3, 0.4, 0.3, 0.02);
            }
        });
    }

    /**
     * Canon: tomoe open under extreme stress, not on a promotion schedule. Rank still hands
     * them out automatically, but nearly dying can open the next one early — which is how
     * every Uchiha in the story actually awakened theirs.
     */
    private static void applySharinganStressAwakening(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        player.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(ninjaData -> {
            if (!ninjaData.isNinjaModeEnabled() || !ninjaData.hasSharinganEye()) {
                return;
            }
            // Only a genuinely lethal-feeling moment counts: the hit has to leave them
            // under a fifth of their health.
            float remaining = player.getHealth() - event.getAmount();
            if (remaining > 0 && remaining <= player.getMaxHealth() * 0.2f) {
                ninjaData.tryAwakenSharinganTomoe(player, 0.35f);
            }
        });
    }


    /**
     * Chakra nature affinity: jutsu whose element matches the caster's affinity hit +25%
     * harder ("your affinity nature comes out stronger"). Element is derived from the
     * jutsu's damage type, so this covers every fire/water/lightning/wind technique
     * centrally without touching each ability.
     */
    private static void applyNatureAffinity(LivingHurtEvent event) {
        if (!(event.getSource().getEntity() instanceof Player attacker)) {
            return;
        }
        String element;
        if (event.getSource().is(NarutoDamageTypes.FIREBALL) || event.getSource().is(NarutoDamageTypes.AMATERASU)) {
            element = "fire";
        } else if (event.getSource().is(NarutoDamageTypes.WATER_BULLET)) {
            element = "water";
        } else if (event.getSource().is(NarutoDamageTypes.EARTH_SPIKE)) {
            element = "earth";
        } else if (event.getSource().is(NarutoDamageTypes.CHIDORI)) {
            element = "lightning";
        } else if (event.getSource().is(NarutoDamageTypes.RASENGAN)) {
            element = "wind";
        } else {
            return;
        }
        attacker.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(ninjaData -> {
            if (ninjaData.isNinjaModeEnabled() && element.equals(ninjaData.getNatureAffinity())) {
                event.setAmount(event.getAmount() * 1.25f);
            }
        });
    }

    /**
     * A full night's rest restores the ninja completely: chakra and stamina back to full,
     * and accumulated Mangekyo eye strain cleared so the escalating blindness can't stack
     * forever across a long session.
     *
     * Sleeping through the night is already a real cost in survival - you give up the night
     * and you have to be somewhere safe - so it is a fair full refill rather than a partial
     * one, and it gives the bed a purpose beyond skipping mobs.
     */
    @SubscribeEvent
    public static void onWakeUp(PlayerWakeUpEvent event) {
        Player player = event.getEntity();
        player.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(ninjaData -> {
            if (!ninjaData.isNinjaModeEnabled()) {
                return;
            }
            boolean strained = ninjaData.getMsUseCounter() > 0;
            if (strained) {
                ninjaData.clearMangekyoStrain();
            }
            ninjaData.setChakra(ninjaData.getMaxChakra());
            ninjaData.setStamina(ninjaData.getMaxStamina());
            player.displayClientMessage(Component.translatable(strained
                            ? "jutsu.mangekyo.strain.rested"
                            : "naruto.rested").withStyle(ChatFormatting.GREEN), true);
        });
    }

    /**
     * The Sharingan sees through illusions. Blindness and disorientation are the hallmarks
     * of genjutsu in this mod (every genjutsu technique here applies one or both), so an
     * open eye shortens them and a fully matured one refuses them outright.
     *
     * Two things it deliberately does NOT block:
     *  - the Mangekyo's own eye strain, flagged via isApplyingEyeStrain, or having the eye
     *    open would erase the entire overuse drawback
     *  - anything the player drank themselves; only hostile applications are illusions
     */
    /**
     * Guards the re-apply below: adding the shortened effect fires this same event again,
     * which would otherwise shorten it recursively down to a single tick.
     */
    private static boolean reapplyingShortenedGenjutsu = false;

    @SubscribeEvent
    public static void onGenjutsuEffect(net.minecraftforge.event.entity.living.MobEffectEvent.Applicable event) {
        if (reapplyingShortenedGenjutsu || !(event.getEntity() instanceof Player player)) {
            return;
        }
        MobEffectInstance instance = event.getEffectInstance();
        if (instance.getEffect() != MobEffects.BLINDNESS && instance.getEffect() != MobEffects.CONFUSION) {
            return;
        }
        player.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(ninjaData -> {
            if (!ninjaData.isNinjaModeEnabled() || !ninjaData.isSharinganActive()
                    || ninjaData.isApplyingEyeStrain()) {
                return;
            }
            int tomoe = ninjaData.isMangekyoAwakened() ? 3 : ninjaData.getSharinganTomoe();
            if (tomoe <= 0) {
                return;
            }
            if (tomoe >= 3) {
                event.setResult(net.minecraftforge.eventbus.api.Event.Result.DENY);
                player.displayClientMessage(
                        Component.translatable("sharingan.genjutsu.broken").withStyle(ChatFormatting.AQUA), true);
                return;
            }
            // One or two tomoe blunt the illusion rather than dispelling it. The instance's
            // duration is not writable, so deny the original and re-apply a shortened copy.
            float keep = tomoe == 1 ? 0.6f : 0.35f;
            int shortened = Math.max(1, (int) (instance.getDuration() * keep));
            event.setResult(net.minecraftforge.eventbus.api.Event.Result.DENY);
            reapplyingShortenedGenjutsu = true;
            try {
                player.addEffect(new MobEffectInstance(instance.getEffect(), shortened,
                        instance.getAmplifier(), instance.isAmbient(), instance.isVisible()));
            } finally {
                reapplyingShortenedGenjutsu = false;
            }
        });
    }

    @SubscribeEvent
    public static void livingHurt(LivingHurtEvent event) {
        applySharinganDodge(event);
        if (event.isCanceled()) {
            return;
        }
        applySharinganStressAwakening(event);
        applyRankMeleeDamage(event);
        applyNatureAffinity(event);
        applyChidoriMeleeHit(event);
        RasenganImpactEvents.onMeleeHit(event);
        applyTransformationDamageSponge(event);
        applyCombatXp(event);
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (!(event.getSource().getEntity() instanceof Mob)) {
            return;
        }
        player.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(ninjaData -> {
            if (!ninjaData.isNinjaModeEnabled()) {
                return;
            }
            int index = Math.min(Math.max(ninjaData.getRankIndex(), 0), MOB_DAMAGE_MULTIPLIERS.length - 1);
            event.setAmount(event.getAmount() * MOB_DAMAGE_MULTIPLIERS[index]);
        });
    }

    /**
     * The player who should be credited for damage from this source, seeing through a
     * shadow clone to the ninja who made it.
     *
     * Everything a clone learns flows back to the original the moment it disperses - that
     * is the entire reason Kage Bunshin is a training technique and not just extra bodies.
     * Mechanically the clone is its own entity, so every "instanceof Player" check silently
     * dropped its kills on the floor: no chakra XP, no bounty credit, and (via
     * lastHurtByPlayer) not even vanilla experience orbs.
     *
     * @return the player to credit, or null when nothing player-owned dealt the damage.
     */
    @javax.annotation.Nullable
    private static Player creditedPlayer(net.minecraft.world.damagesource.DamageSource source,
                                         net.minecraft.world.level.Level level) {
        net.minecraft.world.entity.Entity attacker = source.getEntity();
        if (attacker instanceof Player player) {
            return player;
        }
        if (attacker instanceof com.sekwah.narutomod.entity.ShadowCloneEntity clone) {
            return clone.getOwnerUUID().map(level::getPlayerByUUID).orElse(null);
        }
        return null;
    }

    /**
     * Phase 15 C: rank XP is earned in combat now, not by burning chakra into the air.
     * Landed jutsu hits (own damage types) pay double, plain melee pays face value.
     * No XP for hitting other players — PvP shouldn't be a training dummy exploit.
     */
    private static void applyCombatXp(LivingHurtEvent event) {
        if (event.isCanceled() || event.getAmount() <= 0) {
            return;
        }
        Player attacker = creditedPlayer(event.getSource(), event.getEntity().level());
        if (attacker == null || event.getEntity() instanceof Player) {
            return;
        }
        // Tell vanilla a player was responsible. Without this a mob a clone killed drops no
        // experience orbs and no looting-enchanted loot at all, because both are gated on
        // lastHurtByPlayer being set.
        event.getEntity().setLastHurtByPlayer(attacker);

        boolean jutsuHit = event.getSource().is(NarutoDamageTypes.FIREBALL)
                || event.getSource().is(NarutoDamageTypes.WATER_BULLET)
                || event.getSource().is(NarutoDamageTypes.EARTH_SPIKE)
                || event.getSource().is(NarutoDamageTypes.RASENGAN)
                || event.getSource().is(NarutoDamageTypes.AMATERASU)
                || event.getSource().is(NarutoDamageTypes.CHIDORI);
        boolean meleeHit = event.getSource().is(net.minecraft.world.damagesource.DamageTypes.PLAYER_ATTACK);
        if (!jutsuHit && !meleeHit) {
            return;
        }
        float xp = Math.min(event.getAmount(), 40f) * (jutsuHit ? 2f : 1f);
        attacker.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(ninjaData -> {
            if (ninjaData.isNinjaModeEnabled()) {
                ninjaData.addChakraXp(xp);
            }
        });
    }

    /**
     * A ninja's taijutsu grows with rank: bare-hand/weapon melee gains a flat rank bonus
     * (Academy student punches like a civilian, a Kage caves in walls), then active modes
     * multiply the result — Sage Mode doubles it, Kurama Chakra Mode is "Flash-level"
     * (x3.5), and each open Inner Gate stacks +35%. At high tiers this one-shots common
     * mobs, matching how casually top-rank ninja dispatch fodder in the anime.
     *
     * Deliberately restricted to the VANILLA player-attack damage type: jutsu use their own
     * damage types and already scale via getRankDamageMultiplier(), so boosting them here
     * would double-dip. (Kurama Cloak and Susanoo fold into meleeStateMultiplier below.)
     */
    private static final float[] RANK_MELEE_FLAT_BONUS = {0F, 1F, 2F, 4F, 6F};

    /** Hard ceiling on how much every buff together may multiply one melee swing. */
    private static final float MELEE_MULTIPLIER_CAP = 3.5F;
    private static final float MELEE_MULTIPLIER_CAP_PVP = 2.0F;

    private static float meleeStateMultiplier(INinjaData ninjaData) {
        // Chakra states: the strongest one you are in, not all of them at once.
        float state = 1.0F;
        if (ninjaData.isSageModeActive()) {
            state = Math.max(state, 2.0F);
        }
        if (ninjaData.isKcmActive()) {
            state = Math.max(state, 3.0F);
        }
        // Manifested forms: likewise the strongest, not the product.
        float form = 1.0F;
        if (ninjaData.isKuramaCloakActive()) {
            form = Math.max(form, ninjaData.getKuramaMeleeDamageMultiplier());
        }
        if (ninjaData.isSusanooActive()) {
            form = Math.max(form, ninjaData.getSusanooMeleeDamageMultiplier());
        }
        // The Gates are pure taijutsu, so they genuinely do compound - but additively, and
        // the cap below still contains them.
        float gates = 1.0F + ninjaData.getGatesOpen() * 0.20F;

        return Math.max(state, form) * gates;
    }

    private static void applyRankMeleeDamage(LivingHurtEvent event) {
        if (!event.getSource().is(net.minecraft.world.damagesource.DamageTypes.PLAYER_ATTACK)) {
            return;
        }
        if (!(event.getSource().getEntity() instanceof Player attacker)) {
            return;
        }
        boolean versusPlayer = event.getEntity() instanceof Player;
        attacker.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(ninjaData -> {
            if (!ninjaData.isNinjaModeEnabled()) {
                return;
            }
            int rank = Math.min(Math.max(ninjaData.getNinjaRank(), 0), 4);
            float flat = RANK_MELEE_FLAT_BONUS[rank];

            float modeMultiplier = meleeStateMultiplier(ninjaData);

            // PvP stays a fight, not a one-punch delete: half the flat bonus and a tighter cap.
            if (versusPlayer) {
                flat *= 0.5F;
            }
            modeMultiplier = Math.min(modeMultiplier,
                    versusPlayer ? MELEE_MULTIPLIER_CAP_PVP : MELEE_MULTIPLIER_CAP);
            event.setAmount((event.getAmount() + flat) * modeMultiplier);
        });
    }

    /**
     * Susanoo / Kurama Cloak defense.
     *
     * Susanoo is canon's "absolute defense": ANY physical attack (melee swing or projectile)
     * that reaches the user is taken by the shell instead — fully blocked, with the wielder
     * paying chakra for every absorbed hit (cheaper at higher stages, the shell is thicker).
     * Melee attackers are swatted away by the Susanoo's arm. If the wielder can't afford the
     * block cost, the shell flickers and the hit falls through to the old percentage sponge.
     * Non-physical damage (fire ticks, magic, fall, explosions with no direct entity) still
     * uses the stage-scaled percentage reduction — the shell is armor, not a status cure.
     *
     * Kurama Cloak keeps the tail-scaled percentage sponge.
     */
    /**
     * The shell taking a hit: sound, sparks, and an arm swatting a melee attacker away.
     *
     * The pitch and the particle count both track how close to breaking it is, so the state
     * of the armour is readable from outside without a health bar - which is how Tsunade
     * cracking Madara's reads on screen, and the only feedback an attacker gets on whether
     * they are making progress.
     */
    private static void susanooImpact(Player player, LivingHurtEvent event,
                                      INinjaData ninjaData, boolean shattering) {
        float max = Math.max(1f, ninjaData.getSusanooMaxDurability());
        float integrity = ninjaData.getSusanooDurability() / max;

        if (!shattering && event.getSource().getDirectEntity() instanceof LivingEntity attacker
                && attacker == event.getSource().getEntity()
                && attacker.distanceTo(player) < 4.0) {
            Vec3 away = attacker.position().subtract(player.position()).normalize();
            attacker.knockback(1.6, -away.x, -away.z);
            attacker.hurtMarked = true;
        }

        FinalChakraEffects.shellImpact(player, event.getSource(), ninjaData.getSusanooStage(),
                FinalChakraEffects.tint(ninjaData), integrity, shattering);
    }

    /** Brings the armour down and starts the lockout that makes breaking it worth doing. */
    private static void shatterSusanoo(Player player, INinjaData ninjaData) {
        ninjaData.setSusanooActive(false);
        ninjaData.setSusanooStage(0);
        ninjaData.getToggleAbilityData().removeAbility(com.sekwah.narutomod.abilities.NarutoAbilities.SUSANOO.getId());
        ninjaData.setSusanooBrokenTicks(com.sekwah.narutomod.capabilities.NinjaData.SUSANOO_BROKEN_LOCKOUT);
        player.displayClientMessage(net.minecraft.network.chat.Component
                .literal("Your Susanoo shatters.")
                .withStyle(net.minecraft.ChatFormatting.DARK_PURPLE), true);
    }

    private static void applyTransformationDamageSponge(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof Player player)
                || event.getSource().is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return;
        }
        player.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(ninjaData -> {
            float reduction = 0f;
            if (ninjaData.isSusanooActive() && ninjaData.getSusanooDurability() > 0f) {
                // The shell eats the blow outright. It used to pay chakra per hit and then
                // fall back to a percentage reduction, which meant Susanoo was a discount on
                // your own health bar - you still died, just slower, and there was nothing to
                // break. Now the armour has its own integrity and the fight is about opening
                // it: get through the shell, then reach the person inside.
                float durability = ninjaData.getSusanooDurability();
                float incoming = event.getAmount();

                if (incoming < durability) {
                    ninjaData.setSusanooDurability(durability - incoming);
                    event.setCanceled(true);
                    susanooImpact(player, event, ninjaData, false);
                    return;
                }

                // Overflow passes through. Absorbing a two-thousand-point hit on the last
                // point of integrity would make the final blow the safest one to take.
                ninjaData.setSusanooDurability(0f);
                susanooImpact(player, event, ninjaData, true);
                shatterSusanoo(player, ninjaData);
                event.setAmount(incoming - durability);
                if (event.getAmount() <= 0f) {
                    event.setCanceled(true);
                    return;
                }
            }
            if (ninjaData.isKuramaCloakActive()) {
                int tails = Math.min(Math.max(ninjaData.getKuramaTailCount(), 0), 9);
                reduction = Math.max(reduction, KURAMA_DAMAGE_REDUCTION[tails]);
            }
            if (ninjaData.isSharinganActive() && ninjaData.getSharinganTomoe() >= 3) {
                reduction = Math.max(reduction, SHARINGAN_DANGER_SENSE_REDUCTION);
            }
            // Chidori's tunnel vision.
            //
            // The reason the technique needs a Sharingan is not the lightning - it is that at
            // that speed the user cannot see a counterattack coming. Without the eye there was
            // no cost at all for using it, which made the Sharingan requirement pure flavour.
            // Holding one now means anything that does connect hits harder.
            if (ninjaData.isChidoriActive() && !ninjaData.isSharinganActive()) {
                event.setAmount(event.getAmount() * CHIDORI_TUNNEL_VISION);
            }
            if (reduction > 0f) {
                event.setAmount(event.getAmount() * (1f - reduction));
            }
        });
    }

    /**
     * Landing a Chidori the ordinary way, by hitting somebody while it is lit.
     *
     * The hit itself lives in {@link com.sekwah.narutomod.util.ChidoriStrike} now, because the
     * technique also resolves on its own reach when the user swings into air.
     */
    private static void applyChidoriMeleeHit(LivingHurtEvent event) {
        if (!(event.getSource().getEntity() instanceof Player attacker)
                || event.getSource().getDirectEntity() != attacker
                || !event.getSource().is(net.minecraft.world.damagesource.DamageTypes.PLAYER_ATTACK)) {
            return;
        }
        attacker.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(ninjaData -> {
            if (!ninjaData.isNinjaModeEnabled() || !ninjaData.isChidoriActive()) {
                return;
            }
            com.sekwah.narutomod.util.ChidoriStrike.thrust(attacker, ninjaData);
        });
    }

    /**
     * Melee damage boost + AoE cleave while Kurama Cloak or full Susanoo is active.
     * Both multipliers default to 1.0 when their respective transformation is inactive,
     * so this is safe to apply unconditionally on every player melee hit.
     */

    /**
     * The canon path to a Mangekyo: it opens through the trauma of killing someone you
     * love, not through training. A wolf you tamed and raised yourself is the closest
     * thing Minecraft has to that bond, so putting it down is what triggers the awakening.
     *
     * It has to be YOUR wolf, killed by YOUR hand - a stray wolf or someone else's pet
     * costs you nothing, and the whole point is that it costs something.
     *
     * This runs alongside the rank path (Kage still awakens it) rather than replacing it,
     * so it works as a shortcut: a Jonin with three tomoe can open the Mangekyo early if
     * they are willing to pay for it.
     */
    @SubscribeEvent
    public static void onBondBreakAwakening(net.minecraftforge.event.entity.living.LivingDeathEvent event) {
        if (!(event.getEntity() instanceof net.minecraft.world.entity.animal.Wolf wolf)
                || wolf.level().isClientSide) {
            return;
        }
        // Deliberately NOT routed through creditedPlayer: the Mangekyo opens through doing
        // the thing yourself. Ordering a clone to put the dog down is exactly the dodge the
        // technique is supposed to refuse.
        if (!(event.getSource().getEntity() instanceof Player killer)) {
            return;
        }
        if (!wolf.isTame() || !killer.getUUID().equals(wolf.getOwnerUUID())) {
            return;
        }
        killer.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(ninjaData -> {
            // Gated on the same check isMangekyoAwakened() uses, so this can never silently
            // set a flag that the getter then refuses to report. A transplanted eye counts:
            // this is the only route to a Mangekyo for a non-Uchiha, since the rank path in
            // checkDojutsuPerks stays blood-only.
            if (!ninjaData.isNinjaModeEnabled() || !ninjaData.hasSharinganEye()) {
                return;
            }
            if (ninjaData.isMangekyoAwakened() || ninjaData.getSharinganTomoe() < 3) {
                return;
            }
            ninjaData.setMangekyoAwakened(true);
            killer.displayClientMessage(Component.translatable("mangekyo.awaken.bond")
                    .withStyle(ChatFormatting.DARK_RED), false);
            killer.displayClientMessage(Component.translatable("mangekyo.awaken.bond.cost")
                    .withStyle(ChatFormatting.GRAY), false);
            if (killer.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                        killer.getX(), killer.getEyeY(), killer.getZ(), 40, 0.4, 0.4, 0.4, 0.05);
            }
        });
    }

    private static void awardNonMangekyoSpoils(Player killer, MangekyoBossEntity boss,
                                               MangekyoBossVariant variant) {
        killer.getCapability(NinjaCapabilityHandler.NINJA_DATA)
                .ifPresent(data -> data.addChakraXp(BOSS_KILL_XP));

        switch (variant.dropKind()) {
            case BLADE -> {
                net.minecraft.world.item.Item trophy = switch (variant) {
                    case KISAME -> com.sekwah.narutomod.item.NarutoItems.SAMEHADA.get();
                    case ZABUZA -> com.sekwah.narutomod.item.NarutoItems.KUBIKIRIBOCHO.get();
                    case HIDAN -> com.sekwah.narutomod.item.NarutoItems.KABUTOWARI.get();
                    case DEIDARA -> com.sekwah.narutomod.item.NarutoItems.SHIBUKI.get();
                    case TEMARI -> com.sekwah.narutomod.item.NarutoItems.FOLDING_FAN.get();
                    case TENTEN -> com.sekwah.narutomod.item.NarutoItems.FUMA_SHURIKEN.get();
                    default -> com.sekwah.narutomod.item.NarutoItems.NUIBARI.get();
                };
                boss.spawnAtLocation(new net.minecraft.world.item.ItemStack(trophy));
            }
            case RINNEGAN ->
                // Nagato IS the Rinnegan. A 15% incidental roll would be absurd here, so his
                // is guaranteed - and it is the only reliable source of one in the game.
                    boss.spawnAtLocation(new net.minecraft.world.item.ItemStack(
                            com.sekwah.narutomod.item.NarutoItems.RINNEGAN_EYE.get()));
            case BYAKUGAN -> boss.spawnAtLocation(new net.minecraft.world.item.ItemStack(
                    com.sekwah.narutomod.item.NarutoItems.BYAKUGAN_EYE.get()));
            case SHARINGAN ->
                // Kakashi's was transplanted in the first place; taking it is how the eye
                // has always changed hands.
                    boss.spawnAtLocation(new net.minecraft.world.item.ItemStack(
                            com.sekwah.narutomod.item.NarutoItems.SHARINGAN_EYE.get()));
            case SCROLL -> {
                net.minecraft.world.item.Item scroll = switch (variant) {
                    case HASHIRAMA -> com.sekwah.narutomod.item.NarutoItems.SCROLL_KUCHIYOSE.get();
                    case NARUTO -> com.sekwah.narutomod.item.NarutoItems.SCROLL_RASENSHURIKEN.get();
                    // The sand technique closest to what this mod already has him doing.
                    case GAARA -> com.sekwah.narutomod.item.NarutoItems.SCROLL_EARTH_SPIKES.get();
                    // Each of these is the technique that wielder is actually known for,
                    // mapped onto the closest thing this mod already teaches.
                    case KANKURO -> com.sekwah.narutomod.item.NarutoItems.SCROLL_KUCHIYOSE.get();
                    case KAKUZU -> com.sekwah.narutomod.item.NarutoItems.SCROLL_FALSE_DARKNESS.get();
                    case HAKU -> com.sekwah.narutomod.item.NarutoItems.SCROLL_WATER_DRAGON.get();
                    case KUROTSUCHI -> com.sekwah.narutomod.item.NarutoItems.SCROLL_FIREBALL.get();
                    case MIGHT_GUY -> com.sekwah.narutomod.item.NarutoItems.SCROLL_EIGHT_GATES.get();
                    case SAKURA -> com.sekwah.narutomod.item.NarutoItems.SCROLL_EARTH_WALL.get();
                    case IRUKA -> com.sekwah.narutomod.item.NarutoItems.SCROLL_SHADOW_CLONE.get();
                    case WHITE_ZETSU -> com.sekwah.narutomod.item.NarutoItems.SCROLL_MULTIPLE_SHADOW_CLONE.get();
                    default -> com.sekwah.narutomod.item.NarutoItems.SCROLL_SHADOW_CLONE.get();
                };
                boss.spawnAtLocation(new net.minecraft.world.item.ItemStack(scroll));
            }
            default -> { }
        }

        killer.displayClientMessage(Component.translatable("mangekyo.boss.trophy",
                        Component.translatable(variant.translationKey()).withStyle(ChatFormatting.RED))
                .withStyle(ChatFormatting.GOLD), false);
    }

    /** Felling a tailed beast is the largest single thing a ninja can do in this world. */
    private static final float TAILED_BEAST_KILL_XP = 14000f;

    /**
     * Bringing down a tailed beast.
     *
     * The reward is deliberately not an eye or a bloodline: nothing about killing Gyuki makes
     * a ninja an Uchiha. It is chakra - a great deal of it, scaled by how many tails the thing
     * had - plus its technique scroll, which is the part worth carrying home.
     */
    @SubscribeEvent
    public static void onTailedBeastKill(net.minecraftforge.event.entity.living.LivingDeathEvent event) {
        if (!(event.getEntity() instanceof com.sekwah.narutomod.entity.TailedBeastEntity beast)
                || beast.level().isClientSide) {
            return;
        }
        Player killer = creditedPlayer(event.getSource(), beast.level());
        if (killer == null) {
            return;
        }
        com.sekwah.narutomod.entity.TailedBeastVariant variant = beast.getVariant();
        killer.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(data ->
                data.addChakraXp(TAILED_BEAST_KILL_XP * (0.6f + 0.05f * variant.getTails())));

        killer.displayClientMessage(Component.literal(variant.getDisplayName() + " has been felled")
                .withStyle(ChatFormatting.GOLD), false);
        killer.level().playSound(null, killer.blockPosition(),
                net.minecraft.sounds.SoundEvents.UI_TOAST_CHALLENGE_COMPLETE,
                net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 0.9f);

        // Chakra torn loose from a beast this size is worth something to a medic or a sealer.
        beast.spawnAtLocation(new net.minecraft.world.item.ItemStack(
                com.sekwah.narutomod.item.NarutoItems.SOLDIER_PILL.get(), 4 + variant.getTails()));
        beast.spawnAtLocation(new net.minecraft.world.item.ItemStack(tailedBeastScroll(variant)));
    }

    /** One scroll per beast, picked to fit what that beast actually does. */
    private static net.minecraft.world.item.Item tailedBeastScroll(
            com.sekwah.narutomod.entity.TailedBeastVariant variant) {
        return switch (variant) {
            case SHUKAKU -> com.sekwah.narutomod.item.NarutoItems.SCROLL_GREAT_BREAKTHROUGH.get();
            case MATATABI -> com.sekwah.narutomod.item.NarutoItems.SCROLL_FIREBALL.get();
            case ISOBU -> com.sekwah.narutomod.item.NarutoItems.SCROLL_WATER_DRAGON.get();
            case SON_GOKU -> com.sekwah.narutomod.item.NarutoItems.SCROLL_EARTH_SPIKES.get();
            case KOKUO -> com.sekwah.narutomod.item.NarutoItems.SCROLL_WATER_BULLET.get();
            case SAIKEN -> com.sekwah.narutomod.item.NarutoItems.SCROLL_EARTH_WALL.get();
            case CHOMEI -> com.sekwah.narutomod.item.NarutoItems.SCROLL_RASENSHURIKEN.get();
            case GYUKI -> com.sekwah.narutomod.item.NarutoItems.SCROLL_RASENGAN.get();
        };
    }

    @SubscribeEvent
    public static void onMangekyoBossKill(net.minecraftforge.event.entity.living.LivingDeathEvent event) {
        if (!(event.getEntity() instanceof MangekyoBossEntity boss) || boss.level().isClientSide) {
            return;
        }
        Player killer = creditedPlayer(event.getSource(), boss.level());
        if (killer == null) {
            return;
        }
        MangekyoBossVariant variant = boss.getVariant();
        // Every felled wielder counts toward the Six Paths step, Uchiha or missing-nin alike.
        killer.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(data -> {
            if (data.recordMangekyoBossKill()) {
                killer.displayClientMessage(Component.translatable("rank.six_paths.unlocked")
                        .withStyle(ChatFormatting.GOLD), false);
                killer.level().playSound(null, killer.blockPosition(),
                        net.minecraft.sounds.SoundEvents.UI_TOAST_CHALLENGE_COMPLETE,
                        net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 1.0f);
            }
        });

        // A few of these wielders had transplanted a Rinnegan - rarely, it survives them.
        //
        // Restricted to the Mangekyo tier. This roll used to run for every boss in the game,
        // which was defensible when the roster was five Uchiha and five Akatsuki, and stopped
        // being defensible the moment Hinata and Shikamaru joined it: the rarest item in the
        // mod would have been farmable off the two weakest new bosses, neither of whom has
        // any business carrying one. Nagato is excluded for the opposite reason - his is
        // handed over outright below, and rolling here as well would sometimes drop two.
        if (variant.dropKind() == MangekyoBossVariant.BossDrop.MANGEKYO
                && boss.level().random.nextFloat() < RINNEGAN_DROP_CHANCE) {
            boss.spawnAtLocation(new net.minecraft.world.item.ItemStack(
                    com.sekwah.narutomod.item.NarutoItems.RINNEGAN_EYE.get()));
        }

        if (variant.dropKind() != MangekyoBossVariant.BossDrop.MANGEKYO) {
            awardNonMangekyoSpoils(killer, boss, variant);
            return;
        }

        // An Uchiha corpse still has its eyes. This is how a non-Uchiha ever gets one.
        if (boss.level().random.nextFloat() < SHARINGAN_EYE_DROP_CHANCE) {
            boss.spawnAtLocation(new net.minecraft.world.item.ItemStack(
                    com.sekwah.narutomod.item.NarutoItems.SHARINGAN_EYE.get()));
        }

        killer.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(ninjaData -> {
            ninjaData.addChakraXp(BOSS_KILL_XP);

            if (!ninjaData.isMangekyoAwakened()) {
                // No Mangekyo to upgrade yet — the kill still counts for the XP above.
                killer.displayClientMessage(Component.translatable("mangekyo.ems.nomangekyo")
                        .withStyle(ChatFormatting.GRAY), false);
                return;
            }
            boolean firstTime = !ninjaData.isEternalMangekyoAwakened();
            ninjaData.setEternalMangekyoAwakened(true);
            ninjaData.addDefeatedMsBoss(variant.formId());
            if (ninjaData.getMangekyoForm().isEmpty()) {
                ninjaData.setMangekyoForm(variant.formId());
            }
            ninjaData.clearMangekyoStrain();

            Component formName = Component.translatable(variant.translationKey()).withStyle(ChatFormatting.RED);
            if (firstTime) {
                killer.displayClientMessage(Component.translatable("mangekyo.ems.awakened", formName)
                        .withStyle(ChatFormatting.LIGHT_PURPLE), false);
            }
            killer.displayClientMessage(Component.translatable("mangekyo.form.taken", formName)
                    .withStyle(ChatFormatting.LIGHT_PURPLE), false);
        });
    }

    /**
     * What a dead missing-nin was worth. Scales with their rank for the same reason the
     * player's own rank matters: a Jonin took longer to become dangerous, so killing one
     * teaches you more.
     */
    private static float rogueKillXp(RogueNinjaEntity rogue) {
        return rogue.getNinjaRank() >= RogueNinjaEntity.RANK_JONIN
                ? ROGUE_NINJA_KILL_XP * ROGUE_JONIN_XP_MULTIPLIER
                : ROGUE_NINJA_KILL_XP;
    }

    /**
     * Bingo Book bounty tracking: every kill by a player is checked against their active
     * bounty; completing it pays out chakra XP (see BingoBookItem for issuing bounties).
     */
    @SubscribeEvent
    public static void onBountyKill(net.minecraftforge.event.entity.living.LivingDeathEvent event) {
        Player killer = creditedPlayer(event.getSource(), event.getEntity().level());
        if (killer == null || killer.level().isClientSide) {
            return;
        }
        String killedId = net.minecraft.world.entity.EntityType.getKey(event.getEntity().getType()).toString();
        killer.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(ninjaData -> {
            // Phase 15 C: every hostile kill trains the ninja (tougher mob = more XP);
            // bounty completion below still pays its big lump on top of this.
            // A rogue ninja is worth far more than its 30 health would suggest: you learn
            // from fighting someone who fights back, not from swatting a zombie.
            if (ninjaData.isNinjaModeEnabled() && event.getEntity() instanceof net.minecraft.world.entity.monster.Monster) {
                ninjaData.addChakraXp(event.getEntity() instanceof RogueNinjaEntity rogue
                        ? rogueKillXp(rogue)
                        : 10f + event.getEntity().getMaxHealth() * 0.5f);
            }
            if (ninjaData.getBountyRemaining() <= 0 || !killedId.equals(ninjaData.getBountyTargetId())) {
                return;
            }
            ninjaData.decrementBounty();
            int remaining = ninjaData.getBountyRemaining();
            if (remaining > 0) {
                killer.displayClientMessage(Component.literal("Bounty: " + remaining + "x "
                                + com.sekwah.narutomod.item.BingoBookItem.prettyName(killedId) + " remaining")
                        .withStyle(ChatFormatting.GOLD), true);
            } else {
                float reward = ninjaData.getBountyRewardXp();
                ninjaData.addChakraXp(reward);
                ninjaData.setBounty("", 0, 0f);
                killer.displayClientMessage(Component.literal("Bounty complete! +" + (int) reward + " chakra XP")
                        .withStyle(ChatFormatting.GREEN), false);
            }
        });
    }

    // Handle if they have some agility perk or leaps.
    //
    @SubscribeEvent
    public static void livingFall(LivingFallEvent event) {
        if (event.getEntity() instanceof Player player){
            player.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(ninjaData -> {
                if (!ninjaData.isNinjaModeEnabled()) {
                    return;
                }
                float distance = event.getDistance();
                if(distance < 9){
                    distance *= 0.3f;
                }
                if(distance > 3) {
                    distance -= 5f;
                    distance *= 0.6f;
                }
                event.setDistance(distance);
            });
        }
    }

}
