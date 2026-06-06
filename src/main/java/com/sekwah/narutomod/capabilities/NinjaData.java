package com.sekwah.narutomod.capabilities;

import com.mojang.logging.LogUtils;
import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.toggleabilitydata.ToggleAbilityData;
import com.sekwah.narutomod.config.NarutoConfig;
import com.sekwah.narutomod.gameevents.NarutoGameEvents;
import com.sekwah.narutomod.registries.NarutoRegistries;
import com.sekwah.sekclib.capabilitysync.capabilitysync.annotation.Sync;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import org.joml.Vector3f;
import org.slf4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.UUID;
import java.util.function.Consumer;

public class NinjaData implements INinjaData, ICapabilityProvider {

    private static final Logger LOGGER = LogUtils.getLogger();

    @Sync(minTicks = 1)
    private float chakra;

    @Sync(minTicks = 1)
    private float stamina;

    @Sync(minTicks = 1)
    private float substitutions;

    @Sync
    private float maxChakra;

    /**
     * If the player should have access to all the ninja shit
     */
    @Sync(syncGlobally = true)
    private boolean ninjaModeEnabled;

    @Sync
    private float maxStamina;

    // Unless the player needs to know the max for rendering, no point in rendering.
    private float maxSubstitutions;

    @Sync
    private Vec3 substitutionLocation;

    @Sync
    private ResourceLocation substitutionDimension;

    /**
     * If the player can double jump, will be updated by underlying values server side.
     *
     * Will be true if the player has enough chakra as well as
     */
    @Sync(minTicks = 1)
    private DoubleJumpData doubleJumpData;

    private boolean doubleJumpReady;

    /**
     * The current ability being charged/channeled
     * <p>
     * TODO make this global then expand the channeled logic to be able to handle any visual effects easier.
     * TODO possibly swap the type of this over to an Ability type so that it needs to be looked up less.
     */
    @Sync(minTicks = 1)
    private ResourceLocation currentlyChanneled;

    @Sync(minTicks = 1)
    private int ticksChanneled;

    @Sync
    private ToggleAbilityData toggleAbilityData;


    /**
     * Depending on what's going might want to swap to a potion effect.
     * This will make the player truly invisible.
     */
    @Sync(minTicks = 1, syncGlobally = true)
    private boolean isInvisible = false;

    private int invisibleTicks = 0;


    private ArrayList<DelayedPlayerTickEvent> delayedTickEvents = new ArrayList<>();
    // CooldownTickEvent to follow the same style you have for DelayedPlayerTickEvent
    private HashMap<String, CooldownTickEvent> cooldownTickEvents = new HashMap<>();

    // --- Phase 3: Progression ---
    @Sync
    private float chakraXp = 0;

    @Sync
    private int ninjaRank = 0; // 0=Academy, 1=Genin, 2=Chunin, 3=Jonin, 4=Kage

    @Sync
    private String clanId = ""; // empty = not chosen yet

    // Rank thresholds and bonuses
    private static final float[] RANK_XP_THRESHOLDS = {0, 1000, 5000, 15000, 50000};
    private static final float[] RANK_CHAKRA_BONUS = {0, 400, 2400, 4900, 14900};
    private static final float[] RANK_STAMINA_BONUS = new float[] {0, 50, 200, 500, 900};
    private static final ResourceLocation SHARINGAN_ABILITY = new ResourceLocation("narutomod", "sharingan");
    private static final ResourceLocation BYAKUGAN_ABILITY = new ResourceLocation("narutomod", "byakugan");
    private static final ResourceLocation CHAKRA_DASH_ABILITY = new ResourceLocation("narutomod", "chakra_dash");
    private static final UUID NINJA_SPEED_UUID = UUID.fromString("a3f2c0e1-7b4d-4e8f-9c1a-5d6e7f8a9b0c");
    private static final int[] BYAKUGAN_RANGE = {20, 50, 150, 400, 1000};
    private static final float CHIDORI_TICK_COST = 0.75F;
    private static final DustParticleOptions CHIDORI_PARTICLE = new DustParticleOptions(new Vector3f(0.45F, 0.85F, 1.0F), 1.0F);

    @Sync(minTicks = 1)
    private int chidoriTicks = 0;

    @Sync(minTicks = 1)
    private int wallWalkDirection = -1;

    @Sync(minTicks = 1)
    private int wallWalkTicks = 0;

    @Sync(minTicks = 1)
    private boolean wallWalkAttached = false;

    @Sync(minTicks = 1)
    private int wallWalkDetachTicks = 0;

    public NinjaData(boolean isServer) {
        if (isServer) {
            this.getConfigData();
            this.stamina = this.maxStamina;
            this.chakra = this.maxChakra;
        }
        this.toggleAbilityData = new ToggleAbilityData();
        this.doubleJumpData = new DoubleJumpData(false);
    }

    static class RegenInfo {
        public int cooldown;

        public RegenInfo() {
        }

        /**
         * Tick down when checked
         *
         * @return if regen should take place
         */
        public boolean canRegen() {
            if (this.cooldown > 0) {
                this.cooldown--;
                return false;
            }
            return true;
        }
    }

    private final RegenInfo chakraRegenInfo = new RegenInfo();
    private final RegenInfo staminaRegenInfo = new RegenInfo();

    private static final String CHAKRA_TAG = "chakra";
    private static final String STAMINA_TAG = "stamina";
    private static final String NINJA_MODE_ENABLED = "ninjaModeEnabled";
    private static final String SAVE_TIME = "save_time";
    private static final String COOLDOWN_TAG = "cooldowns";
    private static final String SUBSTITUTION_TAG = "substitutions";

    private final LazyOptional<INinjaData> holder = LazyOptional.of(() -> this);

    @Override
    public float getChakra() {
        return this.chakra;
    }

    @Override
    public float getMaxChakra() {
        return this.maxChakra;
    }

    @Override
    public float getStamina() {
        return this.stamina;
    }

    @Override
    public float getSubstitutionCount() {
        return this.substitutions;
    }

    @Override
    public float getMaxStamina() {
        return this.maxStamina;
    }

    @Override
    public void setChakra(float chakra) {
        this.chakra = chakra;
    }

    @Override
    public void setStamina(float stamina) {
        this.stamina = stamina;
    }

    @Override
    public void useChakra(float amount, int cooldown) {
        this.chakra -= amount;
        this.chakraRegenInfo.cooldown = Math.max(cooldown, this.chakraRegenInfo.cooldown);
        // Accumulate chakra XP from usage
        if (amount > 0) {
            this.addChakraXp(amount);
        }
    }

    @Override
    public void useStamina(float amount, int cooldown) {
        this.stamina -= amount;
        this.staminaRegenInfo.cooldown = Math.max(cooldown, this.staminaRegenInfo.cooldown);
    }

    @Override
    public void useSubstitution(float amount) {
        this.substitutions -= amount;
    }

    @Override
    public void addChakra(float amount) {
        this.chakra = Math.min(Math.max(this.chakra + amount, 0), maxChakra);
    }

    @Override
    public void addStamina(float amount) {
        this.stamina = Math.min(Math.max(this.stamina + amount, 0), maxStamina);
    }

    // --- Phase 3: Progression methods ---

    @Override
    public float getChakraXp() {
        return this.chakraXp;
    }

    @Override
    public void addChakraXp(float amount) {
        this.chakraXp += amount;
        // Check rank advancement
        for (int i = RANK_XP_THRESHOLDS.length - 1; i >= 0; i--) {
            if (this.chakraXp >= RANK_XP_THRESHOLDS[i]) {
                if (this.ninjaRank < i) {
                    this.ninjaRank = i;
                }
                break;
            }
        }
    }

    @Override
    public int getNinjaRank() {
        return this.ninjaRank;
    }

    @Override
    public void setNinjaRank(int rank) {
        this.ninjaRank = Math.min(Math.max(rank, 0), 4);
        if (this.chakraXp < RANK_XP_THRESHOLDS[this.ninjaRank]) {
            this.chakraXp = RANK_XP_THRESHOLDS[this.ninjaRank];
        }
        this.getConfigData();
    }

    @Override
    public String getClanId() {
        return this.clanId;
    }

    @Override
    public void setClanId(String clanId) {
        this.clanId = clanId != null ? clanId : "";
    }

    @Override
    public int getSharinganLevel() {
        if (!"uchiha".equals(this.clanId)) {
            return 0;
        }
        return Math.min(Math.max(this.ninjaRank, 0), 4);
    }

    @Override
    public boolean isSharinganActive() {
        return this.toggleAbilityData.getAbilitiesHashSet().contains(SHARINGAN_ABILITY);
    }

    @Override
    public int getByakuganRange() {
        if (!"hyuga".equals(this.clanId) || !this.isByakuganActive()) {
            return 0;
        }
        return BYAKUGAN_RANGE[Math.min(Math.max(this.ninjaRank, 0), 4)];
    }

    @Override
    public boolean isByakuganActive() {
        return this.toggleAbilityData.getAbilitiesHashSet().contains(BYAKUGAN_ABILITY);
    }

    @Override
    public int getChidoriTicks() {
        return this.chidoriTicks;
    }

    @Override
    public void setChidoriTicks(int ticks) {
        this.chidoriTicks = Math.max(ticks, 0);
    }

    @Override
    public boolean isChidoriActive() {
        return this.chidoriTicks > 0;
    }

    @Override
    public Direction getWallWalkDirection() {
        if (this.wallWalkDirection < 0 || this.wallWalkDirection >= Direction.values().length) {
            return null;
        }
        Direction direction = Direction.values()[this.wallWalkDirection];
        return direction.getAxis().isHorizontal() ? direction : null;
    }

    @Override
    public void setWallWalkDirection(Direction direction) {
        if (direction != null && direction.getAxis().isHorizontal()) {
            this.wallWalkDirection = direction.ordinal();
            this.wallWalkTicks = Math.min(this.wallWalkTicks + 4, 40);
        } else {
            this.wallWalkDirection = -1;
            this.wallWalkTicks = 0;
        }
    }

    @Override
    public boolean isWallWalkAttached() {
        return this.wallWalkAttached;
    }

    @Override
    public void setWallWalkAttached(boolean attached) {
        this.wallWalkAttached = attached;
        if (!attached) {
            this.setWallWalkDirection(null);
            // Note: caller is responsible for player.setNoGravity(false) when detaching
        }
    }

    @Override
    public int getWallWalkTicks() {
        return this.wallWalkTicks;
    }

    @Override
    public int getWallWalkDetachTicks() {
        return this.wallWalkDetachTicks;
    }

    @Override
    public void setWallWalkDetachTicks(int ticks) {
        this.wallWalkDetachTicks = Math.max(ticks, 0);
        if (ticks > 0) {
            this.setWallWalkAttached(false);
        }
    }

    @Override
    public float getRankDamageMultiplier() {
        float mult = switch (this.ninjaRank) {
            case 0 -> 0.5f;
            case 1 -> 0.8f;
            case 2 -> 1.0f;
            case 3 -> 1.5f;
            case 4 -> 2.5f;
            default -> 1.0f;
        };
        if (this.sageModeActive) {
            mult *= 1.4f;
        }
        if (this.kuramaCloakActive) {
            mult *= 1.8f;
        }
        return mult;
    }

    /**
     * Backwards-compatible name for older jutsu code.
     */
    public float getJutsuDamageMultiplier() {
        return this.getRankDamageMultiplier();
    }

    @Override
    public void resetProgression() {
        this.chakraXp = 0;
        this.ninjaRank = 0;
        this.clanId = "";
        this.ninjaModeEnabled = false;
        this.toggleAbilityData.getAbilitiesHashSet().clear();
        this.chidoriTicks = 0;
        this.setWallWalkAttached(false);
        this.wallWalkDetachTicks = 0;
        this.getConfigData();
        this.chakra = this.maxChakra;
        this.stamina = this.maxStamina;
        this.substitutions = this.maxSubstitutions;
    }

    /**
     * Get the clan-adjusted max chakra bonus multiplier.
     */
    private float getClanChakraMultiplier() {
        return "uzumaki".equals(this.clanId) ? 1.3f : 1.0f;
    }

    /**
     * Get the clan-adjusted chakra regen multiplier.
     */
    public float getClanChakraRegenMultiplier() {
        return "uzumaki".equals(this.clanId) ? 1.5f : 1.0f;
    }

    @Override
    public float getClanLightningDamageMultiplier() {
        return "uchiha".equals(this.clanId) ? 1.15f : 1.0f;
    }

    @Override
    public float getClanJutsuRangeMultiplier() {
        return "nara".equals(this.clanId) ? 1.10f : 1.0f;
    }

    @Override
    public void setInvisibleTicks(int ticks) {
        this.invisibleTicks = ticks;
    }

    @Override
    public boolean getInvisible() {
        return this.isInvisible;
    }

    @Override
    public Vec3 getSubstitutionLoc() {
        return this.substitutionLocation;
    }

    @Override
    public ResourceLocation getSubstitutionDimension() {
        return this.substitutionDimension;
    }

    @Override
    public void setSubstitutionLoc(Vec3 loc, ResourceLocation dimension) {
        this.substitutionLocation = loc;
        this.substitutionDimension = dimension;
    }

    @Override
    public DoubleJumpData getDoubleJumpData() {
        return this.doubleJumpData;
    }

    @Override
    public ResourceLocation getCurrentlyChanneledAbility() {
        return this.currentlyChanneled;
    }

    @Override
    public int getCurrentlyChanneledTicks() {
        return this.ticksChanneled;
    }

    @Override
    public void setCurrentlyChanneledAbility(Player player, Ability ability) {
        if (ability != null) {
            if (ability.castingSound() != null) {
                player.level().playSound(null, player, ability.castingSound(), SoundSource.PLAYERS, 0.5f, 1.0f);
                player.level().gameEvent(player, NarutoGameEvents.JUTSU_CASTING.get(), player.position().add(0, player.getEyeHeight() * 0.7, 0));
            }

            if(!(ability instanceof Ability.Channeled channeled && channeled.hideChannelMessages())) {
                if (ability instanceof Ability.Channeled channeled && channeled.useChargedMessages()) {
                    player.displayClientMessage(Component.translatable("jutsu.charge.start", Component.translatable(ability.getTranslationKey(this, 1)).withStyle(ChatFormatting.YELLOW)).withStyle(ChatFormatting.GREEN), true);
                } else {
                    player.displayClientMessage(Component.translatable("jutsu.channel.start", Component.translatable(ability.getTranslationKey(this, 1)).withStyle(ChatFormatting.YELLOW)).withStyle(ChatFormatting.GREEN), true);
                }
            }

            NarutoRegistries.ABILITIES.getResourceKey(ability)
                    .ifPresent(abilityResourceKey -> this.currentlyChanneled = abilityResourceKey.location());
        } else {
            if (this.currentlyChanneled != null) {
                Ability currentAbility = NarutoRegistries.ABILITIES.getValue(this.currentlyChanneled);
                if( currentAbility != null) {
                    if(!(currentAbility instanceof Ability.Channeled channeled && channeled.hideChannelMessages())) {
                        if (currentAbility instanceof Ability.Channeled channeled && channeled.useChargedMessages()) {
                            player.displayClientMessage(Component.translatable("jutsu.cast", Component.translatable(currentAbility.getTranslationKey(this, this.ticksChanneled - 1)).withStyle(ChatFormatting.YELLOW)).withStyle(ChatFormatting.GREEN), true);
                        } else {
                            player.displayClientMessage(Component.translatable("jutsu.channel.stop", Component.translatable(currentAbility.getTranslationKey(this)).withStyle(ChatFormatting.YELLOW)).withStyle(ChatFormatting.RED), true);
                        }
                    }
                }
            }
            this.currentlyChanneled = null;
        }
        this.ticksChanneled = 0;
    }

    @Override
    public ToggleAbilityData getToggleAbilityData() {
        return this.toggleAbilityData;
    }

    @Override
    public void updateDataServer(Player player) {

        if(this.invisibleTicks > 0) {
            this.invisibleTicks--;
            this.isInvisible = true;
        } else {
            this.isInvisible = false;
        }

        if(!this.isNinjaModeEnabled()) {
            this.updateNinjaSpeed(player);
            this.chidoriTicks = 0;
            this.setWallWalkAttached(false);
            this.wallWalkDetachTicks = 0;
            return;
        }

        this.getConfigData();
        Iterator<DelayedPlayerTickEvent> iterator = this.delayedTickEvents.iterator();
        while (iterator.hasNext()) {
            DelayedPlayerTickEvent event = iterator.next();
            event.tick();
            if (event.shouldRun()) {
                event.run(player);
                iterator.remove();
            }
        }

        // Compile list of keys from cooldown map
        ArrayList<String> completeList = new ArrayList<>(cooldownTickEvents.keySet());
        //  loop through to tick and then remove cooldown if complete
        for (String name : completeList) {
            CooldownTickEvent event = cooldownTickEvents.get(name);
            event.tick();
            if (event.isComplete()) {
                cooldownTickEvents.remove(name);
            }
        }

        if (this.staminaRegenInfo.canRegen()) {
            this.stamina += 0.5f + this.ninjaRank * 0.3f;
        }
        if (this.chakraRegenInfo.canRegen()) {
            this.chakra += NarutoConfig.chakraRegen * getClanChakraRegenMultiplier();
        }
        this.substitutions += NarutoConfig.substitutionRegenRate;
        this.substitutions = Math.min(Math.max(this.substitutions, 0), this.maxSubstitutions);
        this.stamina = Math.min(Math.max(this.stamina, 0), this.maxStamina);
        this.chakra = Math.min(Math.max(this.chakra, 0), this.maxChakra);

        this.updateChidoriState(player);
        this.decayWallWalkState(player);
        this.updateShadowPossession(player);
        this.updateSageMode(player);
        this.updateEightGates(player);
        this.updateKuramaCloak(player);
        this.updateNinjaSprintStamina(player);
        this.updateNinjaSpeed(player);

        if (this.currentlyChanneled != null) {
            Ability ability = NarutoRegistries.ABILITIES.getValue(this.currentlyChanneled);
            if (ability != null && ability.activationType() == Ability.ActivationType.CHANNELED) {
                if(ability.handleCost(player, this, this.ticksChanneled)) {
                    if (ability instanceof Ability.Channeled channeled) {
                        channeled.handleChannelling(player, this, this.ticksChanneled);
                    }
                } else {
                    if (this.ticksChanneled > 0) {
                        int finalTicksChanneled = this.ticksChanneled - 1;
                        ability.performServer(player, this, finalTicksChanneled);
                        this.setCurrentlyChanneledAbility(player, null);
                    }
                }
            } else {
                LOGGER.error("Somehow non channeled ability has been set to ninja data {}", this.currentlyChanneled);
            }
            this.ticksChanneled++;
        }

        if(player.onGround()) {
            this.doubleJumpData.canDoubleJumpServer = true;
        }

        // Clan passive effects (applied every 40 ticks = 2 sec to avoid overhead)
        if (!this.clanId.isEmpty() && player.tickCount % 40 == 0) {
            switch (this.clanId) {
                case "haruno" -> {} // handled below (every tick)
                case "nara" -> player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED, 60, 0, false, false));
                case "hyuga" -> {
                    if (!this.isByakuganActive()) {
                        player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                                net.minecraft.world.effect.MobEffects.DAMAGE_BOOST, 60, 0, false, false));
                    }
                }
            }
        }
        if ("haruno".equals(this.clanId) && player.getHealth() < player.getMaxHealth()) {
            player.heal(0.03f); // ~0.6 HP per second
        }
    }

    private void updateChidoriState(Player player) {
        if (this.chidoriTicks <= 0) {
            return;
        }
        if (this.chakra < CHIDORI_TICK_COST) {
            this.chidoriTicks = 0;
            return;
        }
        this.useChakra(CHIDORI_TICK_COST, 5);
        this.chidoriTicks--;
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 8, 0, false, false));
        if (player.level() instanceof ServerLevel serverLevel) {
            Vec3 look = player.getLookAngle();
            Vec3 right = look.cross(new Vec3(0.0D, 1.0D, 0.0D)).normalize().scale(0.35D);
            Vec3 hand = player.position().add(0.0D, player.getBbHeight() * 0.65D, 0.0D).add(right);
            serverLevel.sendParticles(CHIDORI_PARTICLE, hand.x, hand.y, hand.z, 2, 0.08D, 0.08D, 0.08D, 0.02D);
            serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, hand.x, hand.y, hand.z, 2, 0.12D, 0.12D, 0.12D, 0.04D);
        }
    }

    private void updateNinjaSprintStamina(Player player) {
        if (!player.isSprinting()) {
            return;
        }
        if (this.stamina < 0.1F) {
            player.setSprinting(false);
            return;
        }
        this.useStamina(0.1F, 5);
    }

    private void updateNinjaSpeed(Player player) {
        AttributeInstance speedAttr = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speedAttr == null) {
            return;
        }

        speedAttr.removeModifier(NINJA_SPEED_UUID);
        if (!this.ninjaModeEnabled) {
            return;
        }

        double speedBonus = 0.0D;
        if (this.toggleAbilityData.getAbilitiesHashSet().contains(CHAKRA_DASH_ABILITY)) {
            double[] rankSpeeds = new double[] {0.3D, 0.5D, 0.8D, 1.2D, 2.0D};
            speedBonus += rankSpeeds[Math.min(Math.max(this.ninjaRank, 0), 4)];
        }
        if (this.gatesOpen > 0) {
            speedBonus += this.gatesOpen * 0.4D;
        }
        if (this.sageModeActive) {
            speedBonus += 0.3D;
        }
        if (this.kuramaCloakActive) {
            speedBonus += 0.8D;
        }

        if (speedBonus > 0.0D) {
            speedAttr.addTransientModifier(new AttributeModifier(
                    NINJA_SPEED_UUID,
                    "Naruto ninja speed",
                    speedBonus,
                    AttributeModifier.Operation.MULTIPLY_BASE));
        }
    }

    private void decayWallWalkState(Player player) {
        if (this.wallWalkDetachTicks > 0) {
            this.wallWalkDetachTicks--;
        }
        if (this.wallWalkTicks > 0) {
            this.wallWalkTicks--;
        } else {
            if (this.wallWalkAttached) {
                // Ticks ran out while attached — restore gravity
                player.setNoGravity(false);
            }
            this.wallWalkDirection = -1;
            this.wallWalkAttached = false;
        }
    }

    // --- Shadow Possession (Nara clan) ---
    private UUID shadowPossessedTargetUUID = null;
    private int shadowPossessionTicks = 0;

    @Override
    public void setShadowPossessedTarget(UUID targetUUID, int ticks) {
        this.shadowPossessedTargetUUID = targetUUID;
        this.shadowPossessionTicks = ticks;
    }

    @Override
    public UUID getShadowPossessedTargetUUID() {
        return this.shadowPossessedTargetUUID;
    }

    @Override
    public int getShadowPossessionTicks() {
        return this.shadowPossessionTicks;
    }

    @Override
    public boolean hasShadowTarget() {
        return this.shadowPossessedTargetUUID != null && this.shadowPossessionTicks > 0;
    }

    private void updateShadowPossession(Player player) {
        if (!hasShadowTarget()) {
            this.shadowPossessedTargetUUID = null;
            this.shadowPossessionTicks = 0;
            return;
        }
        this.shadowPossessionTicks--;
        if (this.shadowPossessionTicks <= 0) {
            this.shadowPossessedTargetUUID = null;
            return;
        }
        if (!(player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel)) return;
        net.minecraft.world.entity.Entity targetEntity = null;
        for (net.minecraft.world.entity.Entity e : serverLevel.getEntities().getAll()) {
            if (e.getUUID().equals(this.shadowPossessedTargetUUID)) {
                targetEntity = e;
                break;
            }
        }
        if (!(targetEntity instanceof net.minecraft.world.entity.Mob mob)) {
            this.shadowPossessedTargetUUID = null;
            this.shadowPossessionTicks = 0;
            return;
        }
        // Mirror player's movement: apply player's delta movement to mob
        Vec3 playerVel = player.getDeltaMovement();
        Vec3 mobVel = mob.getDeltaMovement();
        mob.setDeltaMovement(playerVel.x, mobVel.y, playerVel.z);
        // Face same direction as player
        mob.setYRot(player.getYRot());
        mob.yHeadRot = player.getYHeadRot();
        // Dark particles along shadow path (every 5 ticks)
        if (this.shadowPossessionTicks % 5 == 0) {
            Vec3 playerPos = player.position();
            Vec3 mobPos = mob.position();
            int steps = (int) playerPos.distanceTo(mobPos) * 3;
            for (int i = 0; i <= steps; i++) {
                double t = steps == 0 ? 0 : i / (double) steps;
                Vec3 pos = playerPos.lerp(mobPos, t);
                serverLevel.sendParticles(
                        new net.minecraft.core.particles.DustParticleOptions(
                                new org.joml.Vector3f(0.05f, 0.0f, 0.15f), 1.0f),
                        pos.x, pos.y, pos.z, 1, 0.1, 0.0, 0.1, 0.0);
            }
        }
    }

    // --- Sage Mode ---
    @Sync(minTicks = 1)
    private boolean sageModeActive = false;

    @Sync(minTicks = 1)
    private int sageModeTicks = 0;

    @Sync(minTicks = 1)
    private int sageCharge = 0; // 0-100, accumulated while standing still

    private static final float SAGE_CHAKRA_PER_TICK = 2.0f;
    private static final int SAGE_MAX_DURATION = 30 * 20; // 30 seconds
    private static final int SAGE_MAX_CHARGE = 100;
    // Overcharge/petrification now handled in SageModeAbility.handleChannelling()

    @Override
    public boolean isSageModeActive() {
        return this.sageModeActive;
    }

    @Override
    public void setSageModeActive(boolean active) {
        this.sageModeActive = active;
        if (!active) {
            this.sageModeTicks = 0;
        }
    }

    @Override
    public int getSageModeTicks() {
        return this.sageModeTicks;
    }

    @Override
    public void setSageModeTicks(int ticks) {
        this.sageModeTicks = ticks;
    }

    @Override
    public int getSageCharge() {
        return this.sageCharge;
    }

    @Override
    public void setSageCharge(int charge) {
        this.sageCharge = Math.max(0, Math.min(charge, SAGE_MAX_CHARGE));
    }

    private void updateSageMode(Player player) {
        // Natural energy gathering is now handled by SageModeAbility (CHANNELED — hold combo to gather)

        // --- Active Sage Mode tick ---
        if (this.sageModeActive) {
            if (this.chakra < SAGE_CHAKRA_PER_TICK || this.sageModeTicks <= 0) {
                // Run out of chakra or time — deactivate
                this.sageModeActive = false;
                this.sageModeTicks = 0;
                player.displayClientMessage(Component.translatable("sage.deactivate").withStyle(ChatFormatting.RED), true);
                return;
            }

            this.useChakra(SAGE_CHAKRA_PER_TICK, 5);
            this.sageModeTicks--;

            // Apply Sage Mode buffs every 20 ticks (1 second)
            if (this.sageModeTicks % 20 == 0) {
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 30, 1, false, false));     // Strength II
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 30, 0, false, false));     // Regen I
            }

            // Orange aura particles (every 3 ticks)
            if (this.sageModeTicks % 3 == 0 && player.level() instanceof ServerLevel serverLevel) {
                double angle = Math.toRadians((player.tickCount * 15) % 360);
                double px = player.getX() + 0.6 * Math.cos(angle);
                double pz = player.getZ() + 0.6 * Math.sin(angle);
                serverLevel.sendParticles(
                        new DustParticleOptions(new org.joml.Vector3f(1.0f, 0.65f, 0.05f), 1.0f),
                        px, player.getY() + 0.2 + Math.random() * 1.5, pz,
                        1, 0.05, 0.1, 0.05, 0.01);
                serverLevel.sendParticles(ParticleTypes.FLAME,
                        player.getX() + (Math.random() - 0.5) * 0.5,
                        player.getY() + Math.random() * 1.8,
                        player.getZ() + (Math.random() - 0.5) * 0.5,
                        1, 0.0, 0.02, 0.0, 0.005);
            }
        }
    }

    // --- Flying Thunder God ---
    private BlockPos thunderGodMark = null;

    @Override
    public BlockPos getThunderGodMark() {
        return this.thunderGodMark;
    }

    @Override
    public void setThunderGodMark(BlockPos pos) {
        this.thunderGodMark = pos;
    }

    // --- Eight Gates ---
    @Sync(minTicks = 1)
    private int gatesOpen = 0;
    private int gatesTicks = 0; // ticks remaining before gates auto-close

    @Override
    public int getGatesOpen() {
        return this.gatesOpen;
    }

    @Override
    public void setGatesOpen(int gates) {
        this.gatesOpen = Math.max(0, Math.min(gates, 8));
    }

    @Override
    public int getGatesTicks() {
        return this.gatesTicks;
    }

    @Override
    public void setGatesTicks(int ticks) {
        this.gatesTicks = ticks;
    }

    private void updateEightGates(Player player) {
        if (this.gatesOpen <= 0) return;

        this.gatesTicks--;
        float staminaCost = this.gatesOpen * 0.5F;
        if (this.stamina < staminaCost) {
            this.gatesOpen = 0;
            this.gatesTicks = 0;
            player.displayClientMessage(Component.literal("Eight Gates closed: stamina exhausted.")
                    .withStyle(ChatFormatting.RED), true);
            return;
        }
        this.useStamina(this.gatesOpen * 0.5F, 5);

        // Apply effects based on gates open
        if (this.gatesTicks % 20 == 0) {
            int strengthLevel = Math.min(this.gatesOpen - 1, 3); // Strength 0-3
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 30, strengthLevel, false, false));

            if (this.gatesOpen >= 3) {
                player.addEffect(new MobEffectInstance(MobEffects.JUMP, 30, 1, false, false));
            }
            if (this.gatesOpen == 8) {
                // Gate of Death: God Mode
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 30, 4, false, false));
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 30, 9, false, false));
            }
        }

        // Gates 5-7: lose 2 HP/sec
        if (this.gatesOpen >= 5 && this.gatesOpen <= 7 && this.gatesTicks % 20 == 0) {
            player.hurt(player.damageSources().magic(), 2.0f);
        }

        // Green aura particles
        if (this.gatesTicks % 4 == 0 && player.level() instanceof ServerLevel serverLevel) {
            double angle = Math.toRadians((player.tickCount * 20) % 360);
            serverLevel.sendParticles(
                    new DustParticleOptions(new Vector3f(0.1f, 0.9f, 0.2f), 1.2f),
                    player.getX() + 0.5 * Math.cos(angle),
                    player.getY() + 0.3 + Math.random() * 1.5,
                    player.getZ() + 0.5 * Math.sin(angle),
                    1, 0.05, 0.1, 0.05, 0.01);
        }

        // Auto-close
        if (this.gatesTicks <= 0) {
            if (this.gatesOpen == 8) {
                // Gate of Death: instant death
                player.hurt(player.damageSources().magic(), 999.0f);
                player.displayClientMessage(Component.literal("The Gate of Death closes...")
                        .withStyle(ChatFormatting.DARK_RED), true);
            } else {
                player.displayClientMessage(Component.literal("Eight Gates closed.")
                        .withStyle(ChatFormatting.GREEN), true);
            }
            this.gatesOpen = 0;
            this.gatesTicks = 0;
        }
    }

    // --- Kurama Cloak (Jinchuriki) ---
    @Sync(minTicks = 1)
    private boolean kuramaCloakActive = false;

    @Sync(minTicks = 1)
    private int kuramaCloakTicks = 0;

    private static final float KURAMA_CHAKRA_PER_TICK = 5.0f;

    @Override
    public boolean isKuramaCloakActive() {
        return this.kuramaCloakActive;
    }

    @Override
    public void setKuramaCloakActive(boolean active) {
        this.kuramaCloakActive = active;
        if (!active) {
            this.kuramaCloakTicks = 0;
        }
    }

    @Override
    public int getKuramaCloakTicks() {
        return this.kuramaCloakTicks;
    }

    @Override
    public void setKuramaCloakTicks(int ticks) {
        this.kuramaCloakTicks = ticks;
    }

    private void updateKuramaCloak(Player player) {
        if (!this.kuramaCloakActive) return;

        if (this.chakra < KURAMA_CHAKRA_PER_TICK || this.kuramaCloakTicks <= 0) {
            this.kuramaCloakActive = false;
            this.kuramaCloakTicks = 0;
            player.displayClientMessage(Component.literal("Kurama Cloak faded!")
                    .withStyle(ChatFormatting.RED), true);
            return;
        }

        this.useChakra(KURAMA_CHAKRA_PER_TICK, 5);
        this.kuramaCloakTicks--;

        // Buffs every second
        if (this.kuramaCloakTicks % 20 == 0) {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 30, 2, false, false));     // Strength III
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 30, 0, false, false)); // Resistance I
        }

        // Orange-red aura particles
        if (this.kuramaCloakTicks % 2 == 0 && player.level() instanceof ServerLevel serverLevel) {
            double angle = Math.toRadians((player.tickCount * 25) % 360);
            double px = player.getX() + 0.7 * Math.cos(angle);
            double pz = player.getZ() + 0.7 * Math.sin(angle);

            // Orange dust
            serverLevel.sendParticles(
                    new DustParticleOptions(new Vector3f(1.0f, 0.4f, 0.05f), 1.3f),
                    px, player.getY() + 0.2 + Math.random() * 1.8, pz,
                    1, 0.05, 0.1, 0.05, 0.01);

            // Flame particles
            serverLevel.sendParticles(ParticleTypes.FLAME,
                    player.getX() + (Math.random() - 0.5) * 0.6,
                    player.getY() + Math.random() * 2.0,
                    player.getZ() + (Math.random() - 0.5) * 0.6,
                    1, 0.0, 0.03, 0.0, 0.008);

            // Red core particles every 4 ticks
            if (this.kuramaCloakTicks % 4 == 0) {
                serverLevel.sendParticles(
                        new DustParticleOptions(new Vector3f(0.9f, 0.15f, 0.05f), 1.0f),
                        player.getX(), player.getY() + 1.0, player.getZ(),
                        2, 0.3, 0.6, 0.3, 0.02);
            }
        }
    }

    /**
     * Returns extra damage multiplier for melee hits while Kurama Cloak is active.
     * Called externally for AoE explosion on punch.
     */
    public float getKuramaMeleeDamageMultiplier() {
        return this.kuramaCloakActive ? 1.5f : 1.0f;
    }

    private void getConfigData() {
        float baseChakra = NarutoConfig.maxChakra + RANK_CHAKRA_BONUS[Math.min(this.ninjaRank, 4)];
        this.maxChakra = baseChakra * getClanChakraMultiplier();
        this.maxStamina = NarutoConfig.maxStamina + RANK_STAMINA_BONUS[Math.min(this.ninjaRank, 4)];
        this.maxSubstitutions = NarutoConfig.maxSubstitutions;
    }

    @Override
    public void scheduleDelayedTickEvent(Consumer<Player> consumer, int tickDelay) {
        this.delayedTickEvents.add(new DelayedPlayerTickEvent(consumer, tickDelay));
    }

    @Override
    public void updateDataClient(Player player) {
        this.doubleJumpData.stuckCheck();
    }

    @Override
    public void setIsNinja(boolean enableNinja) {
        this.ninjaModeEnabled = enableNinja;
    }

    @Override
    public boolean isNinjaModeEnabled() {
        return this.ninjaModeEnabled;
    }

    @Override
    public Tag serializeNBT() {
        final CompoundTag nbt = new CompoundTag();
        nbt.putFloat(CHAKRA_TAG, this.chakra);
        nbt.putFloat(STAMINA_TAG, this.stamina);
        nbt.putBoolean(NINJA_MODE_ENABLED, this.ninjaModeEnabled);
        long currentTime = System.currentTimeMillis();
        nbt.putLong(SAVE_TIME, currentTime);
        final CompoundTag cooldownData = new CompoundTag();
        for (String key : this.cooldownTickEvents.keySet()) {
            CooldownTickEvent event = this.cooldownTickEvents.get(key);
            cooldownData.putInt(key, event.ticks);
        }
        nbt.put(COOLDOWN_TAG, cooldownData);
        nbt.putFloat(SUBSTITUTION_TAG, this.substitutions);
        nbt.putFloat("chakraXp", this.chakraXp);
        nbt.putInt("ninjaRank", this.ninjaRank);
        nbt.putString("clanId", this.clanId);
        nbt.putInt("sageCharge", this.sageCharge);
        if (this.thunderGodMark != null) {
            nbt.putInt("thunderGodX", this.thunderGodMark.getX());
            nbt.putInt("thunderGodY", this.thunderGodMark.getY());
            nbt.putInt("thunderGodZ", this.thunderGodMark.getZ());
            nbt.putBoolean("hasThunderGodMark", true);
        }
        return nbt;
    }

    @Override
    public void deserializeNBT(Tag tag) {
        if (tag instanceof CompoundTag compoundTag) {
            long currentTime = System.currentTimeMillis();
            long saveTime = compoundTag.getLong(SAVE_TIME);
            int ticksPassed = Math.max((int) ((currentTime - saveTime) / 1000 * 20), 0);
            this.chakra = compoundTag.getFloat(CHAKRA_TAG);
            this.stamina = compoundTag.getFloat(STAMINA_TAG);
            this.ninjaModeEnabled = compoundTag.getBoolean(NINJA_MODE_ENABLED);
            CompoundTag cooldownData = compoundTag.getCompound(COOLDOWN_TAG);
            for (String key : cooldownData.getAllKeys()) {
                this.cooldownTickEvents.put(key, new CooldownTickEvent(cooldownData.getInt(key) - ticksPassed));
            }
            this.substitutions = compoundTag.getFloat(SUBSTITUTION_TAG);
            this.chakraXp = compoundTag.getFloat("chakraXp");
            this.ninjaRank = compoundTag.getInt("ninjaRank");
            this.clanId = compoundTag.getString("clanId");
            this.sageCharge = compoundTag.getInt("sageCharge");
            if (compoundTag.getBoolean("hasThunderGodMark")) {
                this.thunderGodMark = new BlockPos(
                        compoundTag.getInt("thunderGodX"),
                        compoundTag.getInt("thunderGodY"),
                        compoundTag.getInt("thunderGodZ"));
            }
        }
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        return NinjaCapabilityHandler.NINJA_DATA.orEmpty(cap, holder);
    }

    @Override
    public HashMap<String, CooldownTickEvent> getCooldownEvents() {
        return cooldownTickEvents;
    }
}
