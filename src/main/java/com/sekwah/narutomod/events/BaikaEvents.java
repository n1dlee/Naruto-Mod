package com.sekwah.narutomod.events;

import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.abilities.NarutoAbilities;
import com.sekwah.narutomod.capabilities.NinjaCapabilityHandler;
import com.sekwah.narutomod.network.PacketHandler;
import com.sekwah.narutomod.network.s2c.BaikaPacket;
import com.sekwah.narutomod.network.s2c.BaikaPacket.Stage;
import com.sekwah.narutomod.sounds.NarutoSounds;
import com.sekwah.narutomod.util.BaikaScale;
import com.sekwah.narutomod.util.KamuiStep;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

@Mod.EventBusSubscriber(modid = NarutoMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class BaikaEvents {
    private static final Map<Player, Growth> FORMS = new WeakHashMap<>();
    private static final Boost POWER = new Boost(Attributes.ATTACK_DAMAGE, "7a3307ce-2bc3-4f75-9472-62a673c843a8", 3.1,
            AttributeModifier.Operation.ADDITION);
    private static final Boost MASS = new Boost(Attributes.KNOCKBACK_RESISTANCE, "70c19233-1558-4452-96b1-c5c993d83c18", 0.32,
            AttributeModifier.Operation.ADDITION);
    private static final Boost PACE = new Boost(Attributes.MOVEMENT_SPEED, "fcae3953-da72-4c45-bcb5-3351c246802b", -0.29,
            AttributeModifier.Operation.MULTIPLY_TOTAL);

    private BaikaEvents() {
    }

    public static boolean canStart(Player player) {
        return !FORMS.containsKey(player) && !BoulderRollEvents.actionLocked(player) && player.onGround() && player.getPose() == Pose.STANDING
                && suitable(player) && fits(player, BaikaScale.FULL);
    }

    public static void denied(Player player) {
        if (player.level() instanceof ServerLevel level) {
            send(player, level, Stage.DENIED);
            level.playSound(null, player.blockPosition(), NarutoSounds.BAIKA_DENIED.get(), SoundSource.PLAYERS, 0.63f, 1);
        }
    }

    public static boolean maintainable(Player player) {
        Growth state = FORMS.get(player);
        if (state == null) {
            return canStart(player);
        }
        if (state.shrinkAge != 0 || !suitable(player)) {
            return false;
        }
        if (!state.stable && (!player.onGround() || !fits(player, BaikaScale.growth(state.age + 1)))) {
            denied(player);
            return false;
        }
        return true;
    }

    public static boolean full(Player player) {
        Growth state = FORMS.get(player);
        return state != null && state.stable;
    }

    public static void update(Player player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        var state = FORMS.computeIfAbsent(player, owner -> begin(owner, level));
        state.last = level.getGameTime();
        if (!state.stable) {
            grow(player, state);
        } else {
            move(player, state);
        }
    }

    private static Growth begin(Player player, ServerLevel level) {
        level.playSound(null, player.blockPosition(), NarutoSounds.BAIKA_GROW.get(), SoundSource.PLAYERS, 0.91f, 1);
        return new Growth(level, player.position());
    }

    private static void grow(Player player, Growth state) {
        state.age++;
        BaikaScale.apply(player, BaikaScale.growth(state.age));
        if (state.age >= BaikaScale.GROW_TICKS) {
            state.stable = true;
            modifiers(player, true);
            dust(player, state.level, 18);
            send(player, state.level, Stage.STEP);
            state.level.playSound(null, player.blockPosition(), NarutoSounds.BAIKA_STEP.get(), SoundSource.PLAYERS, 1.1f, 0.81f);
        }
        send(player, state.level, state.stable ? Stage.STABLE : Stage.GROW);
    }

    private static void move(Player player, Growth state) {
        long now = state.level.getGameTime();
        Vec3 feet = player.position();
        if (player.onGround()) {
            state.stride += feet.subtract(state.lastFeet).horizontalDistance();
        }
        state.lastFeet = feet;
        if (player.onGround() && state.stride > 1.7 && now - state.lastStep >= 7) {
            state.stride = 0;
            state.lastStep = now;
            dust(player, state.level, 8);
            send(player, state.level, Stage.STEP);
            state.level.playSound(null, player.blockPosition(), NarutoSounds.BAIKA_STEP.get(), SoundSource.PLAYERS, 0.83f, 1);
        } else if (now % 12 == 0) {
            send(player, state.level, Stage.STABLE);
        }
        if (now % 20 == 0) {
            player.causeFoodExhaustion(1.5f);
        }
    }

    public static void end(Player player) {
        Growth state = FORMS.get(player);
        if (state != null && state.shrinkAge == 0) {
            state.stable = false;
            modifiers(player, false);
            state.shrinkStart = BaikaScale.current(player);
            state.shrinkAge = 1;
            send(player, state.level, Stage.SHRINK);
        }
    }

    @SubscribeEvent
    public static void tick(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide) {
            return;
        }
        Growth state = FORMS.get(player);
        if (state == null) {
            return;
        }
        if (!player.isAlive() || player.level() != state.level) {
            clear(player, state);
        } else if (state.shrinkAge > 0) {
            BaikaScale.apply(player, BaikaScale.shrink(state.shrinkStart, state.shrinkAge++));
            send(player, state.level, Stage.SHRINK);
            if (BaikaScale.current(player) == 1) {
                FORMS.remove(player);
            }
        } else if (state.level.getGameTime() - state.last > 2) {
            end(player);
        }
    }

    @SubscribeEvent
    public static void leave(EntityLeaveLevelEvent event) {
        if (!event.getLevel().isClientSide && event.getEntity() instanceof Player player) {
            Growth state = FORMS.get(player);
            if (state != null) {
                clear(player, state);
            }
        }
    }

    private static void clear(Player player, Growth state) {
        modifiers(player, false);
        BaikaScale.apply(player, 1);
        send(player, state.level, Stage.SHRINK);
        player.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(data ->
                data.getToggleAbilityData().removeAbility(NarutoAbilities.BAIKA.getId()));
        FORMS.remove(player);
    }

    private static boolean suitable(Player player) {
        return player.level() instanceof ServerLevel && player.isAlive() && !player.isSpectator() && !player.noPhysics
                && !player.isPassenger() && !player.isFallFlying() && !player.isSwimming() && !player.onClimbable()
                && player.getCapability(NinjaCapabilityHandler.NINJA_DATA).map(data -> !data.isGiantForm()).orElse(false);
    }

    private static boolean fits(Player player, float goal) {
        if (!(player.level() instanceof ServerLevel level)) {
            return false;
        }
        AABB bounds = BaikaScale.bounds(player.getDimensions(Pose.STANDING), BaikaScale.current(player), goal, player.position());
        if (bounds.maxY >= level.getMaxBuildHeight()) {
            return false;
        }
        var border = level.getWorldBorder();
        if (!KamuiStep.insideBorder(bounds, border.getMinX(), border.getMinZ(), border.getMaxX(), border.getMaxZ())) {
            return false;
        }
        for (double x : new double[]{bounds.minX, bounds.maxX}) {
            for (double z : new double[]{bounds.minZ, bounds.maxZ}) {
                if (!level.hasChunkAt(BlockPos.containing(x, bounds.minY, z))) {
                    return false;
                }
            }
        }
        return level.noCollision(player, bounds.deflate(0.002));
    }

    private static void modifiers(Player player, boolean add) {
        POWER.apply(player, add);
        MASS.apply(player, add);
        PACE.apply(player, add);
    }

    private static void dust(Player player, ServerLevel level, int count) {
        BlockPos under = player.getOnPos();
        var block = level.getBlockState(under);
        if (!block.isAir()) {
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, block), player.getX(), player.getY() + 0.09,
                    player.getZ(), count, 0.50, 0.05, 0.5, 0.071);
        }
    }

    private static void send(Player player, ServerLevel level, Stage stage) {
        PacketHandler.sendNear(new BaikaPacket(level.dimension().location(), player.getId(), BaikaScale.current(player), stage, player.position()),
                level, player.position(), 72);
    }

    private static final class Growth {
        private final ServerLevel level;
        private int age;
        private int shrinkAge;
        private float shrinkStart;
        private boolean stable;
        private long last;
        private long lastStep;
        private Vec3 lastFeet;
        private double stride;

        private Growth(ServerLevel level, Vec3 feet) {
            this.level = level;
            this.lastFeet = feet;
        }
    }

    private record Boost(Attribute key, UUID id, double amount, AttributeModifier.Operation operation) {
        private Boost(Attribute key, String id, double amount, AttributeModifier.Operation operation) {
            this(key, UUID.fromString(id), amount, operation);
        }

        private void apply(Player player, boolean add) {
            var instance = player.getAttribute(this.key);
            if (instance != null) {
                instance.removeModifier(this.id);
                if (add) {
                    instance.addTransientModifier(new AttributeModifier(this.id, "Baika", this.amount, this.operation));
                }
            }
        }
    }
}
