package com.sekwah.narutomod.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;

public class SubstitutionLogEntity extends Mob {
    private static final EntityDataAccessor<Integer> FLIP =
            SynchedEntityData.defineId(SubstitutionLogEntity.class, EntityDataSerializers.INT);
    private int aliveTicks = 20 * 3;

    public SubstitutionLogEntity(EntityType<SubstitutionLogEntity> p_21368_, Level p_21369_) {
        super(p_21368_, p_21369_);
        this.setNoAi(true);
        this.setInvulnerable(true);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(FLIP, 0);
    }

    public int flipTicks() {
        return this.entityData.get(FLIP);
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide) {
            if (this.flipTicks() < 10) {
                this.entityData.set(FLIP, this.flipTicks() + 1);
            }
            if (--this.aliveTicks <= 0) {
                this.discard();
            }
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("LogTTL", this.aliveTicks);
        tag.putInt("LogFlip", this.flipTicks());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("LogTTL")) {
            this.aliveTicks = tag.getInt("LogTTL");
            this.entityData.set(FLIP, tag.getInt("LogFlip"));
        }
    }

    public SubstitutionLogEntity(Level worldIn) {
        this(NarutoEntities.SUBSTITUTION_LOG.get(), worldIn);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 8.0D).add(Attributes.MOVEMENT_SPEED, 0.6D).add(Attributes.ATTACK_DAMAGE, 0.0D);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    public void recreateFromPacket(ClientboundAddEntityPacket entityPacket) {
        super.recreateFromPacket(entityPacket);
        this.setYBodyRot(entityPacket.getYRot());
    }
}
