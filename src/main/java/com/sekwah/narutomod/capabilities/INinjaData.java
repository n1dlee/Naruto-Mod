package com.sekwah.narutomod.capabilities;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.toggleabilitydata.ToggleAbilityData;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.Tag;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.INBTSerializable;

import java.util.HashMap;
import java.util.UUID;
import java.util.function.Consumer;

public interface INinjaData extends INBTSerializable<Tag> {
    float getChakra();
    float getMaxChakra();
    float getStamina();

    float getSubstitutionCount();

    float getMaxStamina();
    void setChakra(float amount);
    void setStamina(float amount);
    void useChakra(float amount, int cooldown);
    void useStamina(float amount, int cooldown);
    void useSubstitution(float amount);
    void addChakra(float amount);
    void addStamina(float amount);
    void setInvisibleTicks(int ticks);
    boolean getInvisible();

    // --- Kurama's own chakra, separate from the player's chakra pool ---
    float getKuramaBond();
    float getMaxKuramaBond();
    void useKuramaBond(float amount);
    void addKuramaBond(float amount);

    // --- Scroll-wheel size/power control for tiered transformations (Susanoo / Kurama Cloak) ---
    float getTransformPower();
    void adjustTransformPower(float delta);

    // --- Universal hand-seal cast-flash pose (Phase 9 Part B) ---
    int getCastPoseTicks();
    void setCastPoseTicks(int ticks);
    boolean isCrossSealPose();
    void setCrossSealPose(boolean crossSeal);

    // --- Ability-specific cast pose dispatch (Phase 10 Part B) ---
    ResourceLocation getLastCastAbilityId();
    void setLastCastAbilityId(ResourceLocation id);

    // --- Rasengan: held in hand, toggled on/off, resized with the scroll wheel ---
    boolean isRasenganHeld();
    void setRasenganHeld(boolean held);
    int getRasenganCharge();
    void adjustRasenganCharge(int delta);

    Vec3 getSubstitutionLoc();
    ResourceLocation getSubstitutionDimension();
    void setSubstitutionLoc(Vec3 loc, ResourceLocation dimension);

    DoubleJumpData getDoubleJumpData();

    HashMap<String, CooldownTickEvent> getCooldownEvents();

    ResourceLocation getCurrentlyChanneledAbility();
    int getCurrentlyChanneledTicks();
    void setCurrentlyChanneledAbility(Player player, Ability ability);

    ToggleAbilityData getToggleAbilityData();

    void updateDataServer(Player player);

    void scheduleDelayedTickEvent(Consumer<Player> consumer, int tickDelay);

    /**
     * Used to update client tracking information
     * @param player
     */
    void updateDataClient(Player player);

    void setIsNinja(boolean enableNinja);

    boolean isNinjaModeEnabled();

    // --- Phase 3: Progression ---
    float getChakraXp();
    void addChakraXp(float amount);
    int getNinjaRank();
    void setNinjaRank(int rank);

    /**
     * Position on the full rank ladder: 0 Academy, 1-3 Genin Low/Mid/High, 4-6 Chunin,
     * 7-9 Jonin, 10-12 Kage, 13 Six Paths. Stats scale on this; permissions still scale on
     * {@link #getNinjaRank()}, so a tier never unlocks anything a whole rank is meant to.
     */
    int getRankIndex();
    /** Low/Mid/High within the current base rank, 0-2. Always 0 at Academy. */
    int getRankTier();
    void setRankTier(int tier);
    boolean isSixPaths();
    int getMangekyoBossKills();
    /** @return true if this kill was the one that opened the Six Paths step */
    boolean recordMangekyoBossKill();
    /** Chakra restored per tick of Chakra Charge channelling at this rank. */
    float getChakraChargePerTick(boolean moving);
    String getClanId();
    void setClanId(String clanId);
    String getNatureAffinity();
    void setNatureAffinity(String nature);

    // --- Phase 15: Nature Release progression ---
    boolean isElementUnlocked(String element);
    java.util.List<String> getUnlockedElements();
    int getMaxElementSlots();
    int getSharinganElementSlotBonus();
    boolean unlockElement(String element);
    float getElementXp(String element);
    void addElementXp(String element, float amount);
    int getElementLevel(String element);
    // --- Operator overrides (the /ninja command); normal play never calls these ---
    void setElementLevel(String element, int level);
    boolean removeElement(String element);
    void grantElement(String element);
    boolean isJutsuLearned(String jutsuPath);
    void learnJutsu(String jutsuPath);

    // --- Bingo Book bounty ---
    String getBountyTargetId();
    int getBountyRemaining();
    float getBountyRewardXp();
    void setBounty(String targetId, int count, float rewardXp);
    void decrementBounty();
    int getSharinganLevel();
    boolean isSharinganActive();
    int getByakuganRange();
    /**
     * How far this ninja can feel chakra from any source at all - Byakugan, Sharingan,
     * Sage Mode or Kurama Chakra Mode - whichever reaches furthest. Drives the vision overlay.
     */
    int getChakraSightRange();
    boolean isByakuganActive();
    float getRankDamageMultiplier();
    float getClanLightningDamageMultiplier();
    float getClanJutsuRangeMultiplier();
    int getChidoriTicks();
    void setChidoriTicks(int ticks);
    boolean isChidoriActive();
    Direction getWallWalkDirection();
    void setWallWalkDirection(Direction direction);
    boolean isWallWalkAttached();
    void setWallWalkAttached(boolean attached);
    int getWallWalkTicks();
    int getWallWalkDetachTicks();
    void setWallWalkDetachTicks(int ticks);
    void resetProgression();

    // --- Shadow Possession (Nara clan) ---
    void setShadowPossessedTarget(UUID targetUUID, int ticks);
    UUID getShadowPossessedTargetUUID();
    int getShadowPossessionTicks();
    boolean hasShadowTarget();

    // --- Sage Mode ---
    boolean isSageModeActive();
    void setSageModeActive(boolean active);
    int getSageModeTicks();
    void setSageModeTicks(int ticks);
    int getSageCharge();
    void setSageCharge(int charge);

    // --- Flying Thunder God ---
    BlockPos getThunderGodMark();
    void setThunderGodMark(BlockPos pos);

    // --- Eight Gates ---
    int getGatesOpen();
    void setGatesOpen(int gates);
    int getGatesTicks();
    void setGatesTicks(int ticks);

    // --- Kurama Cloak (Jinchuriki) ---
    boolean isKuramaCloakActive();
    void setKuramaCloakActive(boolean active);
    int getKuramaCloakTicks();
    void setKuramaCloakTicks(int ticks);
    int getKuramaTailCount();
    void setKuramaTailCount(int count);
    float getKuramaMeleeDamageMultiplier();
    void triggerKuramaTailLash(Player player, net.minecraft.world.entity.LivingEntity primaryTarget);

    // --- Kurama Chakra Mode (KCM) ---
    boolean isKcmActive();
    void setKcmActive(boolean active);

    // --- Phase 16: Dojutsu progression (stored, not rank-derived) ---
    int getSharinganTomoe();
    void setSharinganTomoe(int tomoe);
    /** Born Uchiha, or anyone carrying a transplanted eye. */
    boolean hasSharinganEye();
    boolean isTransplantedSharingan();
    void setTransplantedSharingan(boolean transplanted);
    /** True only while the wielder has deliberately switched the eye into combat mode. */
    boolean isSharinganToggled();
    /** Single jutsu path read off an enemy and castable once, or empty. */
    String getCopiedJutsu();
    void setCopiedJutsu(String jutsuPath);
    /** True for the instant the Mangekyo is inflicting its own eye strain on the wielder. */
    boolean isApplyingEyeStrain();
    /** Rolls the Sharingan's evasion; true when the incoming attack was dodged. */
    boolean trySharinganDodge(Player player, float incomingDamage);
    /** Rolls an early tomoe awakening from a stress trigger; true when one opened. */
    boolean tryAwakenSharinganTomoe(Player player, float triggerChance);
    void setMangekyoAwakened(boolean awakened);
    String getMangekyoForm();
    void setMangekyoForm(String form);
    /** Chosen Susanoo tint as packed 0xRRGGBB, or -1 for the wielder's canon colour. */
    int getSusanooColor();
    void setSusanooColor(int packedRgb);
    /** Movement keys held at the last cast, -1/0/1 per axis; see NinjaData for why. */
    void setMoveInput(float strafe, float forward);
    float getMoveStrafe();
    float getMoveForward();
    void registerMangekyoUse(Player player);
    /** UUID string of the creature branded with the Flying Thunder God seal, or empty. */
    String getHiraishinEntityMark();
    void setHiraishinEntityMark(String entityUuid);

    // --- Kamui pocket dimension: where to put the wielder back when they leave ---
    void setKamuiReturnPoint(String dimensionId, double x, double y, double z);
    void clearKamuiReturnPoint();
    String getKamuiReturnDimension();
    double getKamuiReturnX();
    double getKamuiReturnY();
    double getKamuiReturnZ();
    /** How many un-rested Mangekyo casts have stacked up — drives the blindness duration. */
    int getMsUseCounter();
    /** Wipes accumulated Mangekyo eye strain — a full night's rest clears it. */
    void clearMangekyoStrain();
    boolean isEternalMangekyoAwakened();
    void setEternalMangekyoAwakened(boolean awakened);
    String getDefeatedMsBosses();
    void addDefeatedMsBoss(String formId);
    int getDefeatedMsBossCount();
    boolean hasSignatureForm(String formId);
    int getByakuganLevel();
    void setByakuganLevel(int level);
    boolean isRinneganAwakened();
    void setRinneganAwakened(boolean awakened);
    boolean isRinneganPathUnlocked(String pathId);
    void unlockRinneganPath(String pathId);
    boolean isRinneSharinganAwakened();
    boolean tryConsumePhoenixSageCharge(long worldDay);

    // --- Mangekyo Sharingan / Susanoo (Uchiha) ---
    boolean isMangekyoAwakened();
    boolean isSusanooActive();
    void setSusanooActive(boolean active);
    int getSusanooStage();
    void setSusanooStage(int stage);
    float getSusanooMeleeDamageMultiplier();
    void triggerSusanooArmSwipe(Player player, net.minecraft.world.entity.LivingEntity primaryTarget);

    // --- Giant-form camera eye height (Susanoo Complete Body / Kurama Full Avatar) ---
    float getGiantEyeHeight();
}
