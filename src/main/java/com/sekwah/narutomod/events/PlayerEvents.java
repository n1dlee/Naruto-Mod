package com.sekwah.narutomod.events;

import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.capabilities.NinjaCapabilityHandler;
import com.sekwah.narutomod.damagetypes.NarutoDamageTypes;
import com.sekwah.narutomod.entity.MangekyoBossEntity;
import com.sekwah.narutomod.entity.MangekyoBossVariant;
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
    private static final double[] HEALTH_BONUS_VALUES = new double[] {0.0D, 8.0D, 16.0D, 28.0D, 48.0D};
    private static final float[] MOB_DAMAGE_MULTIPLIERS = new float[] {1.0F, 0.9F, 0.8F, 0.65F, 0.5F};
    private static final DustParticleOptions CHIDORI_PARTICLE = new DustParticleOptions(new Vector3f(0.45F, 0.85F, 1.0F), 1.0F);

    @SubscribeEvent
    public static void onEntityUpdate(LivingEvent.LivingTickEvent event) {
        if (event.getEntity() instanceof Player player) {
            player.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(ninjaData -> {
                if (!ninjaData.isNinjaModeEnabled()) {
                    syncNinjaHealth(player, 0);
                    return;
                }
                applyRankSurvivability(player, ninjaData);
            });
        }
    }

    private static void applyRankSurvivability(Player player, INinjaData ninjaData) {
        int rank = Math.min(Math.max(ninjaData.getNinjaRank(), 0), 4);
        if (player.tickCount % 40 != 0) {
            return;
        }

        syncNinjaHealth(player, rank);
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

    private static void syncNinjaHealth(Player player, int rank) {
        AttributeInstance maxHealth = player.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth == null) {
            return;
        }

        double targetBonus = HEALTH_BONUS_VALUES[Math.min(Math.max(rank, 0), 4)];
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

    private static final float[] SUSANOO_DAMAGE_REDUCTION = {0f, 0.30f, 0.50f, 0.70f, 0.90f}; // by stage 0-4
    private static final float[] KURAMA_DAMAGE_REDUCTION = {0f, 0.10f, 0.10f, 0.10f, 0.30f, 0.30f, 0.30f, 0.30f, 0.50f, 0.80f}; // by tail count 0-9
    private static final float SHARINGAN_DANGER_SENSE_REDUCTION = 0.15f; // 3-tomoe Sharingan, see applyTransformationDamageSponge
    private static final float RINNEGAN_DROP_CHANCE = 0.15f; // per Mangekyo boss kill
    private static final float CHAKRA_FLOW_BONUS = 5.0f;     // bonus damage per chakra-flowed hit
    private static final float CHAKRA_FLOW_HIT_COST = 3.0f;

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

    /**
     * Kamui: Intangibility (Obito's Eternal Mangekyo form) — while the toggle is up the
     * player is phased into the Kamui dimension and attacks pass through them entirely.
     * Cancelling the event outright is what "intangible" means; the steep per-tick chakra
     * drain in KamuiPhaseAbility is what keeps it from being permanent.
     */
    private static void applyKamuiIntangibility(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        player.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(ninjaData -> {
            if (ninjaData.isNinjaModeEnabled()
                    && ninjaData.getToggleAbilityData().getAbilitiesHashSet().contains(KAMUI_PHASE_ABILITY)) {
                event.setCanceled(true);
            }
        });
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
            event.setAmount(event.getAmount() + CHAKRA_FLOW_BONUS * ninjaData.getRankDamageMultiplier());
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
     * Phase 16: a full night's rest clears accumulated Mangekyo eye strain, so the
     * escalating blindness can't stack forever across a long session.
     */
    @SubscribeEvent
    public static void onWakeUp(PlayerWakeUpEvent event) {
        Player player = event.getEntity();
        player.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(ninjaData -> {
            if (ninjaData.getMsUseCounter() <= 0) {
                return;
            }
            ninjaData.clearMangekyoStrain();
            player.displayClientMessage(
                    Component.translatable("jutsu.mangekyo.strain.rested").withStyle(ChatFormatting.GREEN), true);
        });
    }

    @SubscribeEvent
    public static void livingHurt(LivingHurtEvent event) {
        applyKamuiIntangibility(event);
        if (event.isCanceled()) {
            return;
        }
        applyRankMeleeDamage(event);
        applyChakraScalpelHit(event);
        applyChakraFlowHit(event);
        applyNatureAffinity(event);
        applyChidoriMeleeHit(event);
        applyRasenganMeleeHit(event);
        applyTransformationMeleeHit(event);
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
            int rank = Math.min(Math.max(ninjaData.getNinjaRank(), 0), 4);
            event.setAmount(event.getAmount() * MOB_DAMAGE_MULTIPLIERS[rank]);
        });
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
        if (!(event.getSource().getEntity() instanceof Player attacker) || event.getEntity() instanceof Player) {
            return;
        }
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
     * would double-dip. (Kurama Cloak / Susanoo melee multipliers stack separately in
     * applyTransformationMeleeHit.)
     */
    private static final float[] RANK_MELEE_FLAT_BONUS = {0F, 1F, 2F, 4F, 6F};

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

            float modeMultiplier = 1.0F;
            if (ninjaData.isSageModeActive()) {
                modeMultiplier *= 2.0F;
            }
            if (ninjaData.isKcmActive()) {
                modeMultiplier *= 3.5F;
            }
            if (ninjaData.getGatesOpen() > 0) {
                modeMultiplier *= 1.0F + ninjaData.getGatesOpen() * 0.35F;
            }

            // PvP stays a fight, not a one-punch delete: half the flat bonus, modes capped
            // at 2x. Mobs get the full anime treatment.
            if (versusPlayer) {
                flat *= 0.5F;
                modeMultiplier = Math.min(modeMultiplier, 2.0F);
            }
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
    private static void applyTransformationDamageSponge(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        player.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(ninjaData -> {
            float reduction = 0f;
            if (ninjaData.isSusanooActive()) {
                int stage = Math.min(Math.max(ninjaData.getSusanooStage(), 0), 4);

                boolean physicalHit = event.getSource().getDirectEntity() != null;
                float blockCost = 18f - stage * 2.5f;
                if (physicalHit && ninjaData.getChakra() >= blockCost) {
                    ninjaData.useChakra(blockCost, 10);
                    event.setCanceled(true);

                    // The Susanoo's arm swats melee attackers back out of reach
                    if (event.getSource().getDirectEntity() instanceof LivingEntity attacker
                            && attacker == event.getSource().getEntity()
                            && attacker.distanceTo(player) < 4.0) {
                        Vec3 away = attacker.position().subtract(player.position()).normalize();
                        attacker.knockback(1.6, -away.x, -away.z);
                    }

                    player.level().playSound(null, player,
                            net.minecraft.sounds.SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 0.8f, 0.6f);
                    if (player.level() instanceof ServerLevel serverLevel) {
                        serverLevel.sendParticles(
                                new DustParticleOptions(new Vector3f(0.55f, 0.25f, 0.85f), 1.4f),
                                player.getX(), player.getY() + player.getBbHeight() * 0.5, player.getZ(),
                                12, 0.6, 0.7, 0.6, 0.03);
                    }
                    return;
                }
                reduction = Math.max(reduction, SUSANOO_DAMAGE_REDUCTION[stage]);
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
            if (reduction > 0f) {
                event.setAmount(event.getAmount() * (1f - reduction));
            }
        });
    }

    private static void applyChidoriMeleeHit(LivingHurtEvent event) {
        if (!(event.getSource().getEntity() instanceof Player attacker)) {
            return;
        }
        LivingEntity target = event.getEntity();
        attacker.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(ninjaData -> {
            if (!ninjaData.isNinjaModeEnabled() || !ninjaData.isChidoriActive()) {
                return;
            }
            DamageSource source = NarutoDamageTypes.getDamageSource(attacker.level(), NarutoDamageTypes.CHIDORI, attacker, attacker);
            ninjaData.setChidoriTicks(0);
            float damageMultiplier = ninjaData.getRankDamageMultiplier() * ninjaData.getClanLightningDamageMultiplier();
            if (target instanceof Player targetPlayer) {
                float damage = 16.0F * damageMultiplier;
                if (ninjaData.getNinjaRank() < 4) {
                    damage = Math.min(damage, targetPlayer.getHealth() - 1.0F);
                }
                if (damage > 0.0F) {
                    target.hurt(source, damage);
                }
            } else {
                target.hurt(source, 20.0F * damageMultiplier);
            }
            attacker.level().playSound(null, attacker, NarutoSounds.CHIDORI.get(), SoundSource.PLAYERS, 1.0F, 1.15F);
            if (attacker.level() instanceof ServerLevel serverLevel) {
                Vec3 pos = target.position().add(0.0D, target.getBbHeight() * 0.55D, 0.0D);
                serverLevel.sendParticles(CHIDORI_PARTICLE, pos.x, pos.y, pos.z, 14, 0.3D, 0.35D, 0.3D, 0.04D);
                serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, pos.x, pos.y, pos.z, 18, 0.35D, 0.4D, 0.35D, 0.08D);
            }
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
                double kbStrength = 4.0 + t * 6.0;
                target.knockback(kbStrength, -diff.x / horizLen, -diff.z / horizLen);
                Vec3 motion = target.getDeltaMovement();
                target.setDeltaMovement(motion.x, Math.min(motion.y + 0.6, 1.2), motion.z);
            }

            attacker.level().playSound(null, attacker, NarutoSounds.WATER_BULLET_SPLASH.get(), SoundSource.PLAYERS, 1.5F, 1.4F);
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
    private static void applyTransformationMeleeHit(LivingHurtEvent event) {
        if (!(event.getSource().getEntity() instanceof Player attacker)) {
            return;
        }
        LivingEntity target = event.getEntity();
        attacker.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(ninjaData -> {
            if (!ninjaData.isNinjaModeEnabled()) {
                return;
            }
            float multiplier = ninjaData.getKuramaMeleeDamageMultiplier() * ninjaData.getSusanooMeleeDamageMultiplier();
            if (multiplier != 1.0F) {
                event.setAmount(event.getAmount() * multiplier);
            }
            ninjaData.triggerSusanooArmSwipe(attacker, target);
            ninjaData.triggerKuramaTailLash(attacker, target);
        });
    }

    /**
     * Phase 16: defeating one of the roaming Mangekyo wielders takes their eyes. An
     * ordinary Mangekyo becomes Eternal — no more escalating blindness — and the killer
     * gains that wielder's signature technique. Beating several stacks their techniques,
     * so hunting all five is a real progression path rather than a one-off swap.
     */
    @SubscribeEvent
    public static void onMangekyoBossKill(net.minecraftforge.event.entity.living.LivingDeathEvent event) {
        if (!(event.getEntity() instanceof MangekyoBossEntity boss) || boss.level().isClientSide) {
            return;
        }
        if (!(event.getSource().getEntity() instanceof Player killer)) {
            return;
        }
        MangekyoBossVariant variant = boss.getVariant();
        // A few of these wielders had transplanted a Rinnegan — rarely, it survives them.
        if (boss.level().random.nextFloat() < RINNEGAN_DROP_CHANCE) {
            boss.spawnAtLocation(new net.minecraft.world.item.ItemStack(
                    com.sekwah.narutomod.item.NarutoItems.RINNEGAN_EYE.get()));
        }

        // The missing-nin have no Mangekyo to hand over — they drop the blade that made
        // their name instead, which is the whole reason to hunt them.
        if (!variant.isUchiha()) {
            net.minecraft.world.item.Item trophy = switch (variant) {
                case KISAME -> com.sekwah.narutomod.item.NarutoItems.SAMEHADA.get();
                case ZABUZA -> com.sekwah.narutomod.item.NarutoItems.KUBIKIRIBOCHO.get();
                case HIDAN -> com.sekwah.narutomod.item.NarutoItems.KABUTOWARI.get();
                case DEIDARA -> com.sekwah.narutomod.item.NarutoItems.SHIBUKI.get();
                default -> com.sekwah.narutomod.item.NarutoItems.NUIBARI.get();
            };
            boss.spawnAtLocation(new net.minecraft.world.item.ItemStack(trophy));
            killer.getCapability(NinjaCapabilityHandler.NINJA_DATA)
                    .ifPresent(data -> data.addChakraXp(350f + boss.getMaxHealth()));
            killer.displayClientMessage(Component.translatable("mangekyo.boss.trophy",
                    Component.translatable(variant.translationKey()).withStyle(ChatFormatting.RED))
                    .withStyle(ChatFormatting.GOLD), false);
            return;
        }

        killer.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(ninjaData -> {
            ninjaData.addChakraXp(500f + boss.getMaxHealth());
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
     * Bingo Book bounty tracking: every kill by a player is checked against their active
     * bounty; completing it pays out chakra XP (see BingoBookItem for issuing bounties).
     */
    @SubscribeEvent
    public static void onBountyKill(net.minecraftforge.event.entity.living.LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof Player killer) || killer.level().isClientSide) {
            return;
        }
        String killedId = net.minecraft.world.entity.EntityType.getKey(event.getEntity().getType()).toString();
        killer.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(ninjaData -> {
            // Phase 15 C: every hostile kill trains the ninja (tougher mob = more XP);
            // bounty completion below still pays its big lump on top of this.
            if (ninjaData.isNinjaModeEnabled() && event.getEntity() instanceof net.minecraft.world.entity.monster.Monster) {
                ninjaData.addChakraXp(10f + event.getEntity().getMaxHealth() * 0.5f);
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
