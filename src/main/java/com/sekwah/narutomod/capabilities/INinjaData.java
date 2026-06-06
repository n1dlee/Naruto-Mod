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
    String getClanId();
    void setClanId(String clanId);
    int getSharinganLevel();
    boolean isSharinganActive();
    int getByakuganRange();
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
}
