package com.sekwah.narutomod.events;

import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.capabilities.NinjaCapabilityHandler;
import com.sekwah.narutomod.damagetypes.NarutoDamageTypes;
import com.sekwah.narutomod.entity.MangekyoBossEntity;
import com.sekwah.narutomod.entity.MangekyoBossVariant;
import com.sekwah.narutomod.abilities.jutsus.KamuiPhaseAbility;
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
import net.minecraft.world.item.ItemStack;
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

    /** Extra Rasengan launch per Wind Nature level, and the level past which it stops growing. */
    private static final double RASENGAN_WIND_KNOCKBACK = 0.9;
    private static final int RASENGAN_WIND_LEVEL_CAP = 10;

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
        // Every living thing, not just players: the black flame catches on mobs too, and
        // that is most of what it is for.
        com.sekwah.narutomod.util.AmaterasuFlames.tick(event.getEntity());
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

    /**
     * Single owner for every piece of the player's physics this mod switches off -
     * collision (noPhysics), flight (mayfly), and gravity (noGravity).
     *
     * All three had the same bug in different clothes: the state was set in one place and
     * cleared in several others, each guarded by a condition that could be false at the
     * exact moment cleanup was due. Miss one path and the player is left permanently
     * flying, floating, or walking through walls. Reconciling from the authoritative state
     * once a tick removes the whole class of bug: no exit path has to remember anything,
     * and a state that somehow got stuck repairs itself on the next tick.
     *
     * Two techniques now put the player outside the world's collision: Kamui intangibility
     * and the Byakugan's scouting flight. They share one reconciliation because they share
     * one piece of state - whichever is on, the player phases; when neither is, they must
     * be put back, and doing that twice from two places is how desyncs get written.
     *
     * The toggle framework only fires handleAbilityEnded on the server - the client just
     * watches the synced ability set shrink. Hanging the "stop phasing" cleanup off that
     * callback would therefore leave a client permanently able to walk through walls after
     * switching the jutsu off. Reconciling from the synced set instead is idempotent, works
     * identically on both sides, and repairs itself after a relog, a death or a desync.
     *
     * Spectators are skipped outright: their noPhysics is vanilla's own and not ours to
     * clear.
     */
    private static void reconcileKamuiPhasing(Player player) {
        if (player.isSpectator()) {
            return;
        }
        boolean shouldPhase = player.getCapability(NinjaCapabilityHandler.NINJA_DATA)
                .map(data -> data.isNinjaModeEnabled()
                        && (data.getToggleAbilityData().getAbilitiesHashSet().contains(KAMUI_PHASE_ABILITY)
                                || data.getToggleAbilityData().getAbilitiesHashSet()
                                        .contains(BYAKUGAN_SCOUT_ABILITY)))
                .orElse(false);
        if (shouldPhase) {
            KamuiPhaseAbility.applyPhasing(player);
        } else if (player.noPhysics) {
            // Only ever unwinds a state we put the player into - a non-spectator player has
            // no other reason to have noPhysics set.
            KamuiPhaseAbility.clearPhasing(player);
        }

        // Gravity is the other piece of physics the mod switches off, for wall-walking, and
        // it had the same disease: setNoGravity(false) was scattered across four exit paths
        // (the ability's detach branch, the descend-to-ground branch, the tick decay, and
        // the jump-off packet), each behind its own "am I still attached?" guard. Leave the
        // wall in a way none of them covered - jump off at the same moment the jutsu is
        // toggled off - and gravity was never handed back, so the player simply floated
        // away. Reconciling it here means no exit path has to remember any more.
        // Phasing is no longer in this list: it uses real creative flight now and wants
        // gravity left switched on, because flight is what holds the player up. Only
        // wall-walking still needs the gravity flag itself turned off.
        boolean shouldFloat = player.getCapability(NinjaCapabilityHandler.NINJA_DATA)
                .map(INinjaData::isWallWalkAttached)
                .orElse(false);
        if (!shouldFloat && player.isNoGravity()) {
            player.setNoGravity(false);
        }

        // Last, so it wins over the flight teardown above: inside the pocket dimension
        // everyone flies, exactly as the 1.12.2 EMS helmet did (mayfly = creative || in
        // kamui). Without it the void is a prison - one slip off a slab and you fall
        // forever with nothing to land on.
        //
        // The revoke half matters just as much. Granting flight on arrival but never taking
        // it back meant one trip into Kamui left the player able to fly in the overworld
        // forever, because the phasing teardown above only runs while noPhysics is set and
        // by then it no longer is.
        boolean inKamui = com.sekwah.narutomod.world.KamuiDimension.isKamui(player.level());
        // Complete Body Susanoo flies in canon, and it is most of the reason to grow one:
        // up to stage 3 you have a shell, at stage 4 you have an aircraft carrier. Same
        // reconcile as everything else so it is handed back the moment the stage drops.
        boolean completeBody = player.getCapability(NinjaCapabilityHandler.NINJA_DATA)
                .map(data -> data.isSusanooActive() && data.getSusanooStage() >= 4)
                .orElse(false);
        if ((inKamui || completeBody) && !player.getAbilities().mayfly) {
            player.getAbilities().mayfly = true;
            player.onUpdateAbilities();
        } else if (!inKamui && !completeBody && !shouldPhase && player.getAbilities().mayfly
                && !player.isCreative()) {
            player.getAbilities().mayfly = false;
            player.getAbilities().flying = false;
            player.onUpdateAbilities();
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
                        && data.isSusanooActive() && data.getSusanooStage() >= 4)
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
    private static final float CHAKRA_FLOW_BONUS = 5.0f;     // bonus damage per chakra-flowed hit
    private static final float CHAKRA_FLOW_HIT_COST = 3.0f;

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
     * Chakra Scalpel (Haruno, toggled): while active, every melee strike severs muscle
     * from the inside — bonus rank-scaled cut damage, Weakness on the victim, and a small
     * chakra cost per cut. Toggle state is read straight from the toggle-ability set,
     * same pattern NinjaData.updateNinjaSpeed uses for Chakra Dash.
     */
    private static final net.minecraft.resources.ResourceLocation CHAKRA_SCALPEL_ABILITY =
            new net.minecraft.resources.ResourceLocation(NarutoMod.MOD_ID, "chakra_scalpel");

    private static final net.minecraft.resources.ResourceLocation KAMUI_PHASE_ABILITY =
            new net.minecraft.resources.ResourceLocation(NarutoMod.MOD_ID, "kamui_phase");

    private static final net.minecraft.resources.ResourceLocation BYAKUGAN_SCOUT_ABILITY =
            new net.minecraft.resources.ResourceLocation(NarutoMod.MOD_ID, "byakugan_scout");

    /**
     * Kamui: Intangibility (Obito's Eternal Mangekyo form) — while the toggle is up the
     * player is phased into the Kamui dimension and attacks pass through them entirely.
     * Cancelling the event outright is what "intangible" means; the steep per-tick chakra
     * drain in KamuiPhaseAbility is what keeps it from being permanent.
     */
    private static void applyKamuiIntangibility(LivingHurtEvent event) {
        if (isPhasedIntangible(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    /**
     * The same immunity, one event earlier.
     *
     * LivingHurtEvent only removes the damage number. Everything else a technique does on
     * contact - the hurt flash, the knockback, the shield of red particles, the projectile
     * deciding it has hit something - has already happened by then, so a jutsu thrown at a
     * phased Obito visibly connected and merely did nothing. Cancelling LivingAttackEvent is
     * what actually makes it pass through him.
     */
    @SubscribeEvent
    public static void onLivingAttack(net.minecraftforge.event.entity.living.LivingAttackEvent event) {
        if (isPhasedIntangible(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    private static boolean isPhasedIntangible(net.minecraft.world.entity.LivingEntity entity) {
        if (!(entity instanceof Player player)) {
            return false;
        }
        return player.getCapability(NinjaCapabilityHandler.NINJA_DATA)
                .map(data -> data.isNinjaModeEnabled()
                        && data.getToggleAbilityData().getAbilitiesHashSet().contains(KAMUI_PHASE_ABILITY))
                .orElse(false);
    }

    private static final net.minecraft.resources.ResourceLocation PRETA_PATH_ABILITY =
            new net.minecraft.resources.ResourceLocation(NarutoMod.MOD_ID, "preta_path");

    /**
     * Preta Path — the Rinnegan drinks ninjutsu. Chakra-based damage is mostly absorbed
     * and converted back into the user's own reserve; plain physical hits pass through
     * untouched, which is the canonical hole in the technique.
     */
    private static void applyPretaAbsorption(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        DamageSource source = event.getSource();
        boolean isNinjutsu = source.is(net.minecraft.world.damagesource.DamageTypes.MAGIC)
                || source.is(net.minecraft.world.damagesource.DamageTypes.INDIRECT_MAGIC)
                || source.getMsgId().startsWith("narutomod");
        if (!isNinjutsu) {
            return;
        }
        player.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(ninjaData -> {
            if (!ninjaData.isNinjaModeEnabled()
                    || !ninjaData.getToggleAbilityData().getAbilitiesHashSet().contains(PRETA_PATH_ABILITY)) {
                return;
            }
            float absorbed = event.getAmount() * 0.8f;
            event.setAmount(event.getAmount() - absorbed);
            ninjaData.addChakra(absorbed * 4f);
            if (player.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(new DustParticleOptions(new Vector3f(0.62F, 0.55F, 0.82F), 1.2F),
                        player.getX(), player.getY() + player.getBbHeight() * 0.6, player.getZ(),
                        15, 0.4, 0.5, 0.4, 0.02);
            }
        });
    }

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

    private static final net.minecraft.resources.ResourceLocation CHAKRA_FLOW_ABILITY =
            new net.minecraft.resources.ResourceLocation(NarutoMod.MOD_ID, "chakra_flow");

    /**
     * Chakra Flow: channelling chakra into a held weapon makes it cut far past its
     * material — a plain kunai starts biting like diamond.
     *
     * "Weapon" is decided by what the item actually is, not by whether it can hurt: bows,
     * arrows, potions and building blocks are excluded on purpose, so this sharpens a
     * blade instead of turning a stack of cobblestone into a weapon. Bare fists are
     * excluded too — the technique flows into an object, not into your hand.
     */
    private static boolean isChakraFlowWeapon(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        net.minecraft.world.item.Item item = stack.getItem();
        if (item instanceof net.minecraft.world.item.BlockItem
                || item instanceof net.minecraft.world.item.BowItem
                || item instanceof net.minecraft.world.item.CrossbowItem
                || item instanceof net.minecraft.world.item.ArrowItem
                || item instanceof net.minecraft.world.item.PotionItem
                || item instanceof net.minecraft.world.item.ThrowablePotionItem) {
            return false;
        }
        // Anything that swings: vanilla tools/swords plus every ninja weapon we add.
        return item instanceof net.minecraft.world.item.TieredItem
                || item instanceof net.minecraft.world.item.TridentItem
                || stack.getAttributeModifiers(net.minecraft.world.entity.EquipmentSlot.MAINHAND)
                        .containsKey(Attributes.ATTACK_DAMAGE);
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

    private static void applyChakraFlowHit(LivingHurtEvent event) {
        if (!event.getSource().is(net.minecraft.world.damagesource.DamageTypes.PLAYER_ATTACK)) {
            return;
        }
        if (!(event.getSource().getEntity() instanceof Player attacker)) {
            return;
        }
        ItemStack weapon = attacker.getMainHandItem();
        if (!isChakraFlowWeapon(weapon)) {
            return;
        }
        attacker.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(ninjaData -> {
            if (!ninjaData.isNinjaModeEnabled()
                    || !ninjaData.getToggleAbilityData().getAbilitiesHashSet().contains(CHAKRA_FLOW_ABILITY)) {
                return;
            }
            if (ninjaData.getChakra() < CHAKRA_FLOW_HIT_COST) {
                return;
            }
            ninjaData.useChakra(CHAKRA_FLOW_HIT_COST, 10);
            // Rank scaling capped: at Six Paths the raw multiplier is 4.2, which made a coat
            // of chakra worth more than the blade under it.
            float rankScale = Math.min(ninjaData.getRankDamageMultiplier(), 2.5F);
            event.setAmount(event.getAmount() + CHAKRA_FLOW_BONUS * rankScale);
            if (attacker.level() instanceof ServerLevel serverLevel) {
                Vec3 pos = event.getEntity().position().add(0, event.getEntity().getBbHeight() * 0.6, 0);
                serverLevel.sendParticles(NarutoParticles.CHIDORI_CYAN,
                        pos.x, pos.y, pos.z, 8, 0.25, 0.3, 0.25, 0.02);
            }
        });
    }

    private static void applyChakraScalpelHit(LivingHurtEvent event) {
        if (!event.getSource().is(net.minecraft.world.damagesource.DamageTypes.PLAYER_ATTACK)) {
            return;
        }
        if (!(event.getSource().getEntity() instanceof Player attacker)) {
            return;
        }
        LivingEntity target = event.getEntity();
        attacker.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(ninjaData -> {
            if (!ninjaData.isNinjaModeEnabled()
                    || !ninjaData.getToggleAbilityData().getAbilitiesHashSet().contains(CHAKRA_SCALPEL_ABILITY)) {
                return;
            }
            if (ninjaData.getChakra() < 4f) {
                return;
            }
            ninjaData.useChakra(4f, 10);
            event.setAmount(event.getAmount() + 6.0f * ninjaData.getRankDamageMultiplier());
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 5 * 20, 1, false, true));
            if (attacker.level() instanceof ServerLevel serverLevel) {
                Vec3 pos = target.position().add(0, target.getBbHeight() * 0.6, 0);
                serverLevel.sendParticles(new DustParticleOptions(new Vector3f(0.3F, 0.95F, 0.8F), 1.0F),
                        pos.x, pos.y, pos.z, 10, 0.25, 0.3, 0.25, 0.02);
            }
        });
    }

    /**
     * Chakra nature affinity: jutsu whose element matches the caster's affinity hit +25%
     * harder ("your affinity nature comes out stronger"). Element is derived from the
     * jutsu's damage type, so this covers every fire/water/lightning/wind technique
     * centrally without touching each ability. Earth jutsu reshape terrain rather than
     * use a bespoke damage type, so earth affinity currently has no damage hook.
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
        applyKamuiIntangibility(event);
        if (event.isCanceled()) {
            return;
        }
        applySharinganDodge(event);
        if (event.isCanceled()) {
            return;
        }
        applySharinganStressAwakening(event);
        applyRankMeleeDamage(event);
        applyChakraScalpelHit(event);
        applyChakraFlowHit(event);
        applyNatureAffinity(event);
        applyChidoriMeleeHit(event);
        applyRasenganMeleeHit(event);
        applyTransformationDamageSponge(event);
        applyPretaAbsorption(event);
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

    /**
     * The single place a melee swing gets multiplied.
     *
     * It used to be three: the mode multiplier here, a second Susanoo and Kurama pass on the
     * same event, and Chakra Flow's bonus in between - and all of them
     * multiplied. Sage Mode times Kurama Chakra Mode times the cloak times the shell reached
     * forty-three, which is why a Six Paths player put Madara down in four swings despite his
     * seven hundred effective health.
     *
     * Two rules fix the shape rather than just shaving numbers:
     *
     *  - alternatives take the MAX, not the product. Sage Mode and Kurama Chakra Mode are two
     *    ways of powering the same punch, and so are the shell and the cloak; stacking them
     *    multiplicatively was never a design decision, it was three handlers not knowing
     *    about each other.
     *  - whatever comes out is clamped. A cap means new buffs added later cannot silently
     *    reopen this, which is the part that actually matters.
     */
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

        player.level().playSound(null, player,
                shattering ? net.minecraft.sounds.SoundEvents.GLASS_BREAK
                        : net.minecraft.sounds.SoundEvents.SHIELD_BLOCK,
                SoundSource.PLAYERS, shattering ? 2.2f : 0.8f,
                shattering ? 0.5f : 0.5f + integrity * 0.5f);

        if (player.level() instanceof ServerLevel serverLevel) {
            int count = shattering ? 90 : (int) (6 + (1f - integrity) * 18);
            serverLevel.sendParticles(
                    new DustParticleOptions(new Vector3f(0.55f, 0.25f, 0.85f), shattering ? 2.2f : 1.4f),
                    player.getX(), player.getY() + player.getBbHeight() * 0.5, player.getZ(),
                    count, shattering ? 2.5 : 0.6, shattering ? 2.5 : 0.7, shattering ? 2.5 : 0.6,
                    shattering ? 0.35 : 0.03);
        }
    }

    /** Brings the armour down and starts the lockout that makes breaking it worth doing. */
    private static void shatterSusanoo(Player player, INinjaData ninjaData) {
        ninjaData.setSusanooActive(false);
        ninjaData.setSusanooStage(0);
        ninjaData.setSusanooBrokenTicks(com.sekwah.narutomod.capabilities.NinjaData.SUSANOO_BROKEN_LOCKOUT);
        player.displayClientMessage(net.minecraft.network.chat.Component
                .literal("Your Susanoo shatters.")
                .withStyle(net.minecraft.ChatFormatting.DARK_PURPLE), true);
    }

    private static void applyTransformationDamageSponge(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
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
                shatterSusanoo(player, ninjaData);
                susanooImpact(player, event, ninjaData, true);
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
            // Phase 16 — Sharingan danger sense: a fully matured three-tomoe eye reads an
            // attacker's movement a fraction of a second early, so blows land glancing.
            // Small and additive with the shells above rather than competing with them.
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
     * technique also resolves on its own reach when the user thrusts — and two copies of
     * "what a Chidori does" would drift apart the first time either was tuned.
     */
    private static void applyChidoriMeleeHit(LivingHurtEvent event) {
        if (!(event.getSource().getEntity() instanceof Player attacker)) {
            return;
        }
        LivingEntity target = event.getEntity();
        attacker.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(ninjaData -> {
            if (!ninjaData.isNinjaModeEnabled() || !ninjaData.isChidoriActive()) {
                return;
            }
            com.sekwah.narutomod.util.ChidoriStrike.land(attacker, ninjaData, target);
        });
    }

    /**
     * Ramming a held Rasengan into a target on melee contact — the anime's usual way of
     * landing it, rather than throwing it. Deals the same rank/charge-scaled damage the old
     * thrown projectile did, respects the below-Kage "leave at 1 HP" floor on players, and
     * dismisses the Rasengan afterward (one use per activation, matching Chidori's pattern).
     */
    private static void applyRasenganMeleeHit(LivingHurtEvent event) {
        if (!(event.getSource().getEntity() instanceof Player attacker)) {
            return;
        }
        LivingEntity target = event.getEntity();
        attacker.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(ninjaData -> {
            if (!ninjaData.isNinjaModeEnabled() || !ninjaData.isRasenganHeld()) {
                return;
            }
            DamageSource source = NarutoDamageTypes.getDamageSource(attacker.level(), NarutoDamageTypes.RASENGAN, attacker, attacker);
            int charge = ninjaData.getRasenganCharge();
            float t = Math.max(0, Math.min(charge - 20, 40)) / 40.0f;
            float damageMultiplier = ninjaData.getRankDamageMultiplier();

            // Must clear the held flag BEFORE calling target.hurt() — hurt() fires a new
            // LivingHurtEvent synchronously, which re-enters this method; leaving the flag
            // true here caused unbounded recursion (StackOverflowError / game crash).
            ninjaData.setRasenganHeld(false);
            // ...and the toggle has to be told, or it forms a new sphere on the very next
            // tick and the Rasengan is never actually spent. See RasenganJutsuAbility.
            ninjaData.setRasenganConsumed(true);

            if (target instanceof Player targetPlayer) {
                float damage = 14.0F * damageMultiplier;
                if (ninjaData.getNinjaRank() < 4) {
                    damage = Math.min(damage, targetPlayer.getHealth() - 1.0F);
                }
                if (damage > 0.0F) {
                    target.hurt(source, damage);
                }
            } else {
                target.hurt(source, (15.0F + t * 25.0F) * damageMultiplier);
            }

            Vec3 diff = target.position().subtract(attacker.position());
            double horizLen = diff.horizontalDistance();
            if (horizLen > 0.001) {
                // Wind Nature is what makes a Rasengan throw people; the sphere is a wind
                // technique, so mastery of the element is what the launch scales on.
                int windLevel = Math.max(0, ninjaData.getElementLevel("wind"));
                double windBonus = Math.min(windLevel, RASENGAN_WIND_LEVEL_CAP) * RASENGAN_WIND_KNOCKBACK;
                double kbStrength = 4.0 + t * 6.0 + windBonus;

                target.knockback(kbStrength, -diff.x / horizLen, -diff.z / horizLen);
                Vec3 motion = target.getDeltaMovement();
                target.setDeltaMovement(motion.x, Math.min(motion.y + 0.6 + windBonus * 0.08, 1.6), motion.z);
                // Without this the server never sends the velocity change, so a hit PLAYER
                // simply does not move - which is most of why the Rasengan read as having no
                // knockback at all. Mobs are pushed by the server tick either way.
                target.hurtMarked = true;
            }

            // Not the Water Bullet splash. The Rasengan had been borrowing another
            // technique's sound, which is why slamming one sounded like hitting water.
            attacker.level().playSound(null, attacker,
                    net.minecraft.sounds.SoundEvents.ELDER_GUARDIAN_CURSE, SoundSource.PLAYERS, 1.2F, 1.6F);
            if (attacker.level() instanceof ServerLevel serverLevel) {
                Vec3 pos = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
                serverLevel.sendParticles(ParticleTypes.END_ROD, pos.x, pos.y, pos.z, 20, 0.3D, 0.3D, 0.3D, 0.08D);
            }
        });
    }

    /**
     * Punching a block while holding a Rasengan blasts a small crater — "punching through
     * walls" like the anime, instead of only being usable on entities.
     */
    @SubscribeEvent
    public static void onLeftClickBlock(net.minecraftforge.event.entity.player.PlayerInteractEvent.LeftClickBlock event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        Player player = event.getEntity();
        player.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(ninjaData -> {
            if (!ninjaData.isNinjaModeEnabled() || !ninjaData.isRasenganHeld()) {
                return;
            }
            float cost = 15f;
            if (ninjaData.getChakra() < cost) {
                return;
            }
            ninjaData.useChakra(cost, 10);

            net.minecraft.core.BlockPos center = event.getPos();
            int charge = ninjaData.getRasenganCharge();
            int radius = charge >= 45 ? 2 : 1;

            boolean mobGriefing = net.minecraftforge.event.ForgeEventFactory.getMobGriefingEvent(player.level(), player);
            if (mobGriefing) {
                for (net.minecraft.core.BlockPos pos : net.minecraft.core.BlockPos.betweenClosed(
                        center.offset(-radius, -radius, -radius), center.offset(radius, radius, radius))) {
                    if (!player.level().getBlockState(pos).isAir()) {
                        player.level().destroyBlock(pos, true);
                    }
                }
            }

            if (player.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.EXPLOSION, center.getX() + 0.5, center.getY() + 0.5, center.getZ() + 0.5,
                        6, 0.4, 0.4, 0.4, 0.05);
                serverLevel.sendParticles(ParticleTypes.END_ROD, center.getX() + 0.5, center.getY() + 0.5, center.getZ() + 0.5,
                        16, 0.5, 0.5, 0.5, 0.06);
            }
            player.level().playSound(null, center, net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 1.0F, 1.3F);

            ninjaData.setRasenganHeld(false);
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

    /**
     * Everything a felled wielder leaves behind when it is not a Mangekyo.
     *
     * The roster used to be a binary - Uchiha hand over an eye, everyone else drops a sword -
     * and that has no answer for a Rinnegan, a Byakugan, or a Senju who carries neither. Each
     * legend now leaves the thing that was actually theirs.
     */
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

    /**
     * Phase 16: defeating one of the roaming Mangekyo wielders takes their eyes. An
     * ordinary Mangekyo becomes Eternal — no more escalating blindness — and the killer
     * gains that wielder's signature technique. Beating several stacks their techniques,
     * so hunting all five is a real progression path rather than a one-off swap.
     */
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

        // Anything other than a Mangekyo is settled here and the method returns: the tail of
        // this method is entirely about upgrading the killer's own Mangekyo to Eternal, which
        // is meaningless for a Senju, a Hyuga or a jinchuriki.
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
