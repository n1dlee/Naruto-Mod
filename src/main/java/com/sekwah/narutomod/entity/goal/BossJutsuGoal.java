package com.sekwah.narutomod.entity.goal;

import com.sekwah.narutomod.abilities.NarutoAbilities;
import com.sekwah.narutomod.entity.MangekyoBossEntity;
import com.sekwah.narutomod.entity.jutsuprojectile.AmaterasuFireEntity;
import com.sekwah.narutomod.entity.jutsuprojectile.FireballJutsuEntity;
import com.sekwah.narutomod.entity.jutsuprojectile.WaterBulletJutsuEntity;
import com.sekwah.narutomod.util.NarutoParticles;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.RegistryObject;
import org.joml.Vector3f;

import java.util.EnumSet;

/**
 * Lets a Mangekyo boss actually fight like the wielder it is named after. Nothing else in
 * this mod casts jutsu from the mob side — everything is player-input driven — so this
 * goal is the mob-facing equivalent: on a per-technique cooldown it picks one of that
 * wielder's signature techniques, weighted by how far away the target is, and throws it.
 *
 * Two things used to make bosses feel like ordinary zombies with big health bars:
 *
 *  - a MIN_RANGE of 3 blocks. Every boss also runs MeleeAttackGoal, so it closes to
 *    swinging distance and stays there — which is exactly the range at which this goal
 *    refused to fire. In practice a boss that had reached you never cast anything again.
 *  - one technique per kit, shared between five wielders. Itachi and Sasuke played
 *    identically apart from the particle colour.
 *
 * So range is no longer a floor (only a ceiling), and every wielder now has its own
 * two-to-four technique rotation. Techniques that need to play out over time — Deidara's
 * bombing run, Shisui's flicker chain — run as a {@link Sustained} sequence across ticks
 * instead of being crammed into one frame.
 *
 * Damage is applied the same way the player-side jutsu do it (direct hurt + mob effects,
 * reusing the real projectile entities where one exists) rather than inventing a parallel
 * combat path. Nothing here damages terrain: a boss can spawn next to a base, and losing
 * your house to Deidara is not the fight anyone signed up for.
 */
public class BossJutsuGoal extends Goal {

    /** Only a ceiling now — a boss in melee is still very much allowed to cast. */
    private static final double MAX_RANGE = 32.0;
    /**
     * Enough to cover the most expensive fallback in any rotation. Every branch ends in an
     * unconditional cast, so the floor has to clear that cast's cost or a drained boss would
     * be throwing techniques for free.
     *
     * It did not: the floor was 40 while the costliest unconditional cast is 70, so a boss
     * between those two numbers went right on casting with chakra it did not have. The floor
     * is now the real maximum across every pay() call in this file. Raise it if a more
     * expensive technique is ever added to the bottom of a menu.
     */
    private static final float MIN_CHAKRA = 70f;

    private static final DustParticleOptions CROW_BLACK =
            new DustParticleOptions(new Vector3f(0.08F, 0.08F, 0.12F), 1.4F);
    private static final DustParticleOptions POISON_GREEN =
            new DustParticleOptions(new Vector3f(0.35F, 0.75F, 0.20F), 1.1F);
    private static final DustParticleOptions CURSE_CRIMSON =
            new DustParticleOptions(new Vector3f(0.55F, 0.02F, 0.05F), 1.3F);
    private static final DustParticleOptions IRON_SAND =
            new DustParticleOptions(new Vector3f(0.18F, 0.18F, 0.20F), 1.2F);

    /** Techniques that play out over several ticks rather than resolving in one. */
    private enum Sustained {
        /** Deidara: a walking line of detonations closing on the target. */
        CLAY_BARRAGE,
        /** Shisui: blink around the target, cutting once per appearance. */
        BODY_FLICKER,
        /** Madara: a sweeping wall of fire dragged across the target's position. */
        FIRE_WAVE,
        /** Kisame: three water sharks fired in sequence. */
        SHARK_VOLLEY,
        /** Sasori: a fan of poisoned senbon. */
        SENBON_VOLLEY,
        /** Hinata: the Eight Trigrams palm sequence, doubling as it goes. */
        SIXTY_FOUR_PALMS,
        /** Naruto: a wave of shadow clones piling in one after another. */
        CLONE_FLURRY,
        /** Hashirama: a wooden dragon's head driving forward through the ground. */
        WOOD_DRAGON
    }

    private final MangekyoBossEntity boss;
    private int cooldown;

    private Sustained sustained;
    private int sustainedStep;
    private int sustainedTicks;

    public BossJutsuGoal(MangekyoBossEntity boss) {
        this.boss = boss;
        this.setFlags(EnumSet.of(Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (this.cooldown > 0) {
            this.cooldown--;
            return false;
        }
        LivingEntity target = this.boss.getTarget();
        if (target == null || !target.isAlive()) {
            return false;
        }
        double distance = this.boss.distanceTo(target);
        if (distance > MAX_RANGE || this.boss.getChakra() < MIN_CHAKRA) {
            return false;
        }
        // Point blank needs no clean line of sight — they are standing on top of each other.
        return distance < 6.0 || this.boss.getSensing().hasLineOfSight(target);
    }

    @Override
    public boolean canContinueToUse() {
        LivingEntity target = this.boss.getTarget();
        return this.sustained != null && target != null && target.isAlive();
    }

    @Override
    public void start() {
        LivingEntity target = this.boss.getTarget();
        if (target == null) {
            return;
        }
        this.boss.getLookControl().setLookAt(target, 30f, 30f);
        this.sustained = null;
        this.sustainedStep = 0;
        this.sustainedTicks = 0;

        double distance = this.boss.distanceTo(target);
        this.cooldown = switch (this.boss.getVariant()) {
            case ITACHI -> castItachi(target, distance);
            case SASUKE -> castSasuke(target, distance);
            case MADARA -> castMadara(target, distance);
            case SHISUI -> castShisui(target, distance);
            case OBITO -> castObito(target, distance);
            case KISAME -> castKisame(target, distance);
            case ZABUZA -> castZabuza(target, distance);
            case HIDAN -> castHidan(target, distance);
            case DEIDARA -> castDeidara(target, distance);
            case SASORI -> castSasori(target, distance);
            case HASHIRAMA -> castHashirama(target, distance);
            case NAGATO -> castNagato(target, distance);
            case KAKASHI -> castKakashi(target, distance);
            case NARUTO -> castNaruto(target, distance);
            case HINATA -> castHinata(target, distance);
            case SHIKAMARU -> castShikamaru(target, distance);
            case GAARA -> castGaara(target, distance);
            case KANKURO -> castKankuro(target, distance);
            case TEMARI -> castTemari(target, distance);
            case KAKUZU -> castKakuzu(target, distance);
            case HAKU -> castHaku(target, distance);
            case KUROTSUCHI -> castKurotsuchi(target, distance);
            case MIGHT_GUY -> castMightGuy(target, distance);
            case SAKURA -> castSakura(target, distance);
            case TENTEN -> castTenten(target, distance);
            case IRUKA -> castIruka(target, distance);
            case WHITE_ZETSU -> castWhiteZetsu(target, distance);
        };
    }

    @Override
    public void tick() {
        if (this.sustained == null) {
            return;
        }
        LivingEntity target = this.boss.getTarget();
        if (target == null) {
            this.sustained = null;
            return;
        }
        this.boss.getLookControl().setLookAt(target, 30f, 30f);
        this.sustainedTicks++;
        switch (this.sustained) {
            case CLAY_BARRAGE -> stepClayBarrage(target);
            case BODY_FLICKER -> stepBodyFlicker(target);
            case FIRE_WAVE -> stepFireWave(target);
            case SHARK_VOLLEY -> stepSharkVolley(target);
            case SENBON_VOLLEY -> stepSenbonVolley(target);
            case SIXTY_FOUR_PALMS -> stepSixtyFourPalms(target);
            case CLONE_FLURRY -> stepCloneFlurry(target);
            case WOOD_DRAGON -> stepWoodDragon(target);
        }
    }

    @Override
    public void stop() {
        this.sustained = null;
    }

    // ------------------------------------------------------------------ wielders

    /**
     * The range at which a wielder switches from its close menu to its ranged one.
     *
     * Every rotation below is written as two complete menus rather than as one chain of
     * "distance >" guards falling through to a default. The chain version is what made
     * Amaterasu, Kirin, the shark volley and four other techniques unreachable: the bosses
     * chase into melee and stay there, so every guarded branch was skipped and the fight
     * collapsed onto whatever sat at the bottom of the list. Two menus cannot collapse -
     * whichever range the boss is at, it is choosing between real options.
     */
    private static final double CLOSE_RANGE = 7.0;

    /** Itachi: black flame at range, Tsukuyomi up close, crows and fire to fill the gaps. */
    private int castItachi(LivingEntity target, double distance) {
        int roll = this.boss.getRandom().nextInt(100);
        if (distance > CLOSE_RANGE) {
            if (roll < 45 && pay(45f)) {
                castBlackFlame(target);
                offer(NarutoAbilities.AMATERASU);
                return 40;
            }
            if (roll < 80 && pay(26f)) {
                castGreatFireball(target, 24);
                offer(NarutoAbilities.FIREBALL);
                return 26;
            }
            pay(18f);
            castCrowMurder(target);
            offer(NarutoAbilities.CROW_GENJUTSU);
            return 22;
        }
        if (roll < 40 && pay(55f)) {
            castTsukuyomi(target);
            offer(NarutoAbilities.TSUKUYOMI);
            return 90;
        }
        if (roll < 70 && pay(18f)) {
            castCrowMurder(target);
            offer(NarutoAbilities.CROW_GENJUTSU);
            return 22;
        }
        pay(26f);
        castGreatFireball(target, 24);
        offer(NarutoAbilities.FIREBALL);
        return 26;
    }

    /** Sasuke: Kirin from an open sky, otherwise Chidori in one form or another. */
    private int castSasuke(LivingEntity target, double distance) {
        int roll = this.boss.getRandom().nextInt(100);
        if (distance > CLOSE_RANGE) {
            boolean openSky = this.boss.level().canSeeSky(target.blockPosition());
            if (openSky && roll < 45 && pay(70f)) {
                castKirin(target);
                offer(NarutoAbilities.KIRIN);
                return 110;
            }
            if (roll < 80 && pay(26f)) {
                castGreatFireball(target, 20);
                offer(NarutoAbilities.FIREBALL);
                return 24;
            }
            pay(35f);
            castChidoriDash(target);
            offer(NarutoAbilities.CHIDORI_DASH);
            return 34;
        }
        if (roll < 40 && pay(30f)) {
            castNagashi();
            offer(NarutoAbilities.CHIDORI_NAGASHI);
            return 34;
        }
        if (roll < 75 && pay(35f)) {
            castChidoriThrust(target);
            offer(NarutoAbilities.CHIDORI);
            return 30;
        }
        pay(26f);
        castGreatFireball(target, 16);
        offer(NarutoAbilities.FIREBALL);
        return 26;
    }

    /** Madara: the war fan, a wall of fire, and a Susanoo sweep once the shell is up. */
    private int castMadara(LivingEntity target, double distance) {
        int roll = this.boss.getRandom().nextInt(100);
        if (this.boss.getSusanooStage() >= 2 && distance <= 8.0 && roll < 40 && pay(40f)) {
            castSusanooSweep();
            offer(NarutoAbilities.SUSANOO);
            return 45;
        }
        if (roll < 70 && pay(45f)) {
            this.sustained = Sustained.FIRE_WAVE;
            playCastSound(SoundEvents.BLAZE_SHOOT, 0.6f);
            offer(NarutoAbilities.FIREBALL);
            return 60;
        }
        pay(30f);
        castShockwave(target);
        offer(NarutoAbilities.GUNBAI_WIND);
        return 30;
    }

    /** Shisui: takes you out of the fight rather than out of your health bar. */
    private int castShisui(LivingEntity target, double distance) {
        int roll = this.boss.getRandom().nextInt(100);
        if (roll < 30 && pay(60f)) {
            castKotoamatsukami(target);
            offer(NarutoAbilities.KOTOAMATSUKAMI);
            return 140;
        }
        if (roll < 75 && pay(35f)) {
            this.sustained = Sustained.BODY_FLICKER;
            playCastSound(SoundEvents.ENDERMAN_TELEPORT, 1.3f);
            offer(NarutoAbilities.BODY_FLICKER);
            return 48;
        }
        pay(26f);
        castGreatFireball(target, 18);
        offer(NarutoAbilities.FIREBALL);
        return 26;
    }

    /** Obito: never quite where you swung, and pulls you somewhere you did not agree to. */
    private int castObito(LivingEntity target, double distance) {
        int roll = this.boss.getRandom().nextInt(100);
        if (distance > CLOSE_RANGE) {
            if (roll < 55 && pay(35f)) {
                castKamuiPull(target);
                offer(NarutoAbilities.BANSHO_TENIN);
                return 50;
            }
            if (roll < 80 && pay(30f)) {
                castPhaseStrike(target);
                offer(NarutoAbilities.KAMUI);
                return 32;
            }
            pay(26f);
            castGreatFireball(target, 16);
            offer(NarutoAbilities.FIREBALL);
            return 26;
        }
        if (roll < 28 && pay(45f)) {
            castIntangible();
            offer(NarutoAbilities.KAMUI_PHASE);
            return 110;
        }
        if (roll < 78 && pay(30f)) {
            castPhaseStrike(target);
            offer(NarutoAbilities.KAMUI);
            return 32;
        }
        pay(26f);
        castGreatFireball(target, 16);
        offer(NarutoAbilities.FIREBALL);
        return 26;
    }

    /** Kisame: an ocean's worth of chakra and a sword that eats yours. */
    private int castKisame(LivingEntity target, double distance) {
        int roll = this.boss.getRandom().nextInt(100);
        if (distance > CLOSE_RANGE) {
            if (roll < 55 && pay(45f)) {
                this.sustained = Sustained.SHARK_VOLLEY;
                playCastSound(SoundEvents.DOLPHIN_ATTACK, 0.7f);
                offer(NarutoAbilities.WATER_DRAGON);
                return 55;
            }
            pay(35f);
            castWaterPrison(target);
            offer(NarutoAbilities.WATER_BULLET);
            return 60;
        }
        if (roll < 45 && pay(20f)) {
            castSamehadaDrain(target);
            return 26;
        }
        if (roll < 80 && pay(35f)) {
            castWaterPrison(target);
            offer(NarutoAbilities.WATER_BULLET);
            return 60;
        }
        pay(45f);
        this.sustained = Sustained.SHARK_VOLLEY;
        playCastSound(SoundEvents.DOLPHIN_ATTACK, 0.7f);
        offer(NarutoAbilities.WATER_DRAGON);
        return 55;
    }

    /** Zabuza: mist first, then something comes out of it. */
    private int castZabuza(LivingEntity target, double distance) {
        int roll = this.boss.getRandom().nextInt(100);
        if (distance > CLOSE_RANGE) {
            if (roll < 60 && pay(30f)) {
                castWaterDragon(target);
                offer(NarutoAbilities.WATER_DRAGON);
                return 34;
            }
            pay(35f);
            castHiddenMist();
            return 100;
        }
        if (roll < 30 && pay(35f)) {
            castHiddenMist();
            return 100;
        }
        pay(18f);
        castBladeRush(target, 14f);
        return 22;
    }

    /** Hidan: the ritual hurts you for as long as he can stay standing. */
    private int castHidan(LivingEntity target, double distance) {
        int roll = this.boss.getRandom().nextInt(100);
        if (distance > CLOSE_RANGE) {
            if (roll < 60 && pay(22f)) {
                castScytheReach(target);
                return 28;
            }
            pay(40f);
            castCurseRitual(target);
            return 90;
        }
        if (roll < 35 && pay(40f)) {
            castCurseRitual(target);
            return 90;
        }
        pay(18f);
        castBladeRush(target, 13f);
        return 22;
    }

    /** Deidara: art is an explosion, and he was not being modest about the quantity. */
    private int castDeidara(LivingEntity target, double distance) {
        int roll = this.boss.getRandom().nextInt(100);
        if (roll < 30 && pay(55f)) {
            castC2Dragon(target);
            return 70;
        }
        // The barrage is eight detonations for one payment, so it has to cost like eight.
        // At its old 40 it was cheaper than the reserve refilled and he simply never stopped.
        if (pay(55f)) {
            this.sustained = Sustained.CLAY_BARRAGE;
            playCastSound(SoundEvents.CREEPER_PRIMED, 1.2f);
            return 40;
        }
        // Can't afford the run — one bomb, so he is never reduced to punching.
        pay(25f);
        detonate(target.position().add(0, 0.5, 0), 3.0, 10f, 1.0);
        playCastSound(SoundEvents.GENERIC_EXPLODE, 1.3f);
        if (this.boss.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.EXPLOSION,
                    target.getX(), target.getY() + 1.0, target.getZ(), 3, 0.5, 0.5, 0.5, 0.0);
        }
        return 30;
    }

    /** Sasori: poison from a distance, iron sand when you close it. */
    private int castSasori(LivingEntity target, double distance) {
        int roll = this.boss.getRandom().nextInt(100);
        if (distance > CLOSE_RANGE) {
            if (roll < 55 && pay(30f)) {
                this.sustained = Sustained.SENBON_VOLLEY;
                playCastSound(SoundEvents.ARROW_SHOOT, 1.5f);
                return 40;
            }
            pay(35f);
            castFlamethrower(target);
            return 50;
        }
        if (roll < 40 && pay(30f)) {
            castIronSand(target);
            return 45;
        }
        if (roll < 80 && pay(35f)) {
            castFlamethrower(target);
            return 50;
        }
        pay(30f);
        this.sustained = Sustained.SENBON_VOLLEY;
        playCastSound(SoundEvents.ARROW_SHOOT, 1.5f);
        return 40;
    }

    /** Hashirama: the forest answers to him, and he does not stop bleeding out. */
    private int castHashirama(LivingEntity target, double distance) {
        // Senju regeneration deliberately lives on the entity, not here: it fires on health
        // thresholds rather than on a roll the rotation happens to win, which is the whole
        // reason he was unkillable. See MangekyoBossEntity#tickSenjuRegeneration.
        int roll = this.boss.getRandom().nextInt(100);
        if (distance > CLOSE_RANGE) {
            if (roll < 60 && pay(55f)) {
                this.sustained = Sustained.WOOD_DRAGON;
                playCastSound(SoundEvents.WOOD_PLACE, 0.5f);
                offer(NarutoAbilities.WOOD_RELEASE);
                return 65;
            }
            pay(35f);
            castWoodSurge(target);
            offer(NarutoAbilities.WOOD_RELEASE);
            return 40;
        }
        if (roll < 65 && pay(35f)) {
            castWoodGrasp(target);
            offer(NarutoAbilities.WOOD_RELEASE);
            return 45;
        }
        pay(30f);
        castWoodSurge(target);
        offer(NarutoAbilities.WOOD_RELEASE);
        return 40;
    }

    /** Nagato: nothing he does is an attack so much as a change to where things are. */
    private int castNagato(LivingEntity target, double distance) {
        int roll = this.boss.getRandom().nextInt(100);
        if (distance > CLOSE_RANGE) {
            if (roll < 40 && pay(60f)) {
                castChibakuTensei(target);
                offer(NarutoAbilities.BANSHO_TENIN);
                return 90;
            }
            if (roll < 80 && pay(35f)) {
                castKamuiPull(target); // Bansho Ten'in - the same "come here" vector
                offer(NarutoAbilities.BANSHO_TENIN);
                return 45;
            }
            pay(30f);
            castPretaAbsorb();
            offer(NarutoAbilities.PRETA_PATH);
            return 80;
        }
        if (roll < 55 && pay(55f)) {
            castShinraTensei();
            offer(NarutoAbilities.SHINRA_TENSEI);
            return 70;
        }
        if (roll < 80 && pay(30f)) {
            castPretaAbsorb();
            offer(NarutoAbilities.PRETA_PATH);
            return 80;
        }
        pay(35f);
        castKamuiPull(target);
        offer(NarutoAbilities.BANSHO_TENIN);
        return 45;
    }

    /** Kakashi: no single nature, which is exactly the point - he has all of them. */
    private int castKakashi(LivingEntity target, double distance) {
        int roll = this.boss.getRandom().nextInt(100);
        if (distance > CLOSE_RANGE) {
            if (roll < 35 && pay(45f)) {
                castWaterDragon(target);
                offer(NarutoAbilities.WATER_DRAGON);
                return 34;
            }
            if (roll < 70 && pay(26f)) {
                castGreatFireball(target, 20);
                offer(NarutoAbilities.FIREBALL);
                return 26;
            }
            pay(30f);
            castPhaseStrike(target); // Kamui, closing the distance through the gap
            offer(NarutoAbilities.KAMUI);
            return 34;
        }
        if (roll < 50 && pay(40f)) {
            castChidoriThrust(target); // Raikiri
            offer(NarutoAbilities.CHIDORI);
            return 32;
        }
        if (roll < 78 && pay(30f)) {
            castPhaseStrike(target);
            offer(NarutoAbilities.KAMUI);
            return 34;
        }
        pay(30f);
        castNagashi();
        offer(NarutoAbilities.CHIDORI_NAGASHI);
        return 34;
    }

    /** Naruto: clones, spheres, and a fox he can lean on when it stops going his way. */
    private int castNaruto(LivingEntity target, double distance) {
        int roll = this.boss.getRandom().nextInt(100);
        if (this.boss.getHealth() < this.boss.getMaxHealth() * 0.45f && roll < 22 && pay(60f)) {
            castKuramaCloak();
            offer(NarutoAbilities.KURAMA_CLOAK);
            return 130;
        }
        if (distance > CLOSE_RANGE) {
            if (roll < 55 && pay(65f)) {
                castRasenshuriken(target);
                offer(NarutoAbilities.RASENSHURIKEN);
                return 80;
            }
            pay(45f);
            castMultipleShadowClones();
            return 70;
        }
        if (roll < 50 && pay(40f)) {
            castRasenganSlam(target);
            offer(NarutoAbilities.RASENGAN);
            return 40;
        }
        if (roll < 80 && pay(45f)) {
            castMultipleShadowClones();
            return 70;
        }
        pay(45f);
        this.sustained = Sustained.CLONE_FLURRY;
        playCastSound(SoundEvents.PLAYER_ATTACK_STRONG, 1.2f);
        offer(NarutoAbilities.MULTIPLE_SHADOW_CLONE);
        return 55;
    }

    /** Kage Bunshin, as real entities the player has to actually cut down. */
    private void castMultipleShadowClones() {
        this.boss.summonShadowClones(3);
        offer(NarutoAbilities.MULTIPLE_SHADOW_CLONE);
    }

    /** Hinata: the Gentle Fist goes past armour because it was never aimed at armour. */
    private int castHinata(LivingEntity target, double distance) {
        int roll = this.boss.getRandom().nextInt(100);
        if (distance > CLOSE_RANGE) {
            if (roll < 60 && pay(30f)) {
                castVacuumPalm(target);
                offer(NarutoAbilities.AIR_PALM);
                return 34;
            }
            pay(35f);
            castGentleFistDash(target);
            offer(NarutoAbilities.EIGHT_TRIGRAMS_SIXTY_FOUR_PALMS);
            return 40;
        }
        if (roll < 45 && pay(55f)) {
            this.sustained = Sustained.SIXTY_FOUR_PALMS;
            playCastSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.6f);
            offer(NarutoAbilities.EIGHT_TRIGRAMS_SIXTY_FOUR_PALMS);
            return 75;
        }
        if (roll < 80 && pay(35f)) {
            castRotation();
            offer(NarutoAbilities.EIGHT_TRIGRAMS_ROTATION);
            return 50;
        }
        pay(30f);
        castVacuumPalm(target);
        offer(NarutoAbilities.AIR_PALM);
        return 34;
    }

    /** Shikamaru: he does not out-hit you, he takes your turn away. */
    private int castShikamaru(LivingEntity target, double distance) {
        int roll = this.boss.getRandom().nextInt(100);
        if (distance > CLOSE_RANGE) {
            if (roll < 60 && pay(45f)) {
                castShadowBind(target);
                offer(NarutoAbilities.SHADOW_POSSESSION);
                return 70;
            }
            pay(35f);
            castShadowSewing(target);
            offer(NarutoAbilities.SHADOW_SEWING);
            return 40;
        }
        if (roll < 40 && pay(50f)) {
            castShadowStrangle(target);
            offer(NarutoAbilities.SHADOW_STRANGLE);
            return 80;
        }
        if (roll < 75 && pay(35f)) {
            castShadowSewing(target);
            offer(NarutoAbilities.SHADOW_SEWING);
            return 40;
        }
        pay(45f);
        castShadowBind(target);
        offer(NarutoAbilities.SHADOW_POSSESSION);
        return 70;
    }

    /**
     * Gaara: the sand does the fighting. Bullets at range, the Binding Coffin when he can
     * reach you, and a wave of it when he cannot be bothered to aim.
     */
    private int castGaara(LivingEntity target, double distance) {
        int roll = this.boss.getRandom().nextInt(100);
        if (distance > CLOSE_RANGE) {
            if (roll < 55 && pay(35f)) {
                castSandBullet(target);
                return 45;
            }
            pay(50f);
            castSandTsunami(target);
            return 70;
        }
        if (roll < 45 && pay(55f)) {
            castSandBindingCoffin(target);
            return 90;
        }
        if (roll < 80 && pay(50f)) {
            castSandTsunami(target);
            return 70;
        }
        pay(35f);
        castSandBullet(target);
        return 45;
    }

    /** Kankuro: he does not fight. Karasu does, and he keeps another one in reserve. */
    private int castKankuro(LivingEntity target, double distance) {
        int roll = this.boss.getRandom().nextInt(100);
        // The puppet is the whole character, so it is checked first at every range - a
        // puppeteer who only deploys from across the field is a puppeteer who never deploys.
        if (roll < 30 && pay(55f)) {
            this.boss.summonPuppet(this.boss.getRandom().nextBoolean()
                    ? com.sekwah.narutomod.entity.PuppetVariant.KARASU
                    : com.sekwah.narutomod.entity.PuppetVariant.HUNDRED);
            return 120;
        }
        if (distance > CLOSE_RANGE) {
            pay(30f);
            castPoisonMist(target);
            return 55;
        }
        pay(35f);
        castHiddenBlades(target);
        return 40;
    }

    /** Temari: wind, and a great deal of it. Nothing she throws is subtle. */
    private int castTemari(LivingEntity target, double distance) {
        int roll = this.boss.getRandom().nextInt(100);
        if (distance > CLOSE_RANGE) {
            if (roll < 55 && pay(40f)) {
                castWindScythe(target);
                return 50;
            }
            pay(55f);
            castCuttingWhirlwind();
            return 75;
        }
        if (roll < 50 && pay(55f)) {
            castCuttingWhirlwind();
            return 75;
        }
        pay(30f);
        castVacuumPalm(target);
        return 35;
    }

    /** Kakuzu: a different mask for every range, and a body that will not stay broken. */
    private int castKakuzu(LivingEntity target, double distance) {
        int roll = this.boss.getRandom().nextInt(100);
        if (distance > CLOSE_RANGE) {
            if (roll < 40 && pay(50f)) {
                castFlamethrower(target);
                return 65;
            }
            if (roll < 75 && pay(45f)) {
                castLightningMask(target);
                return 60;
            }
            pay(40f);
            castWindScythe(target);
            return 50;
        }
        if (roll < 45 && pay(45f)) {
            castEarthSpear();
            return 90;
        }
        pay(40f);
        castThreadStrike(target);
        return 40;
    }

    /** Haku: he is never where you swung, and the needles are already in the air. */
    private int castHaku(LivingEntity target, double distance) {
        int roll = this.boss.getRandom().nextInt(100);
        if (distance > CLOSE_RANGE) {
            if (roll < 55 && pay(50f)) {
                castIceMirrors(target);
                return 70;
            }
            pay(35f);
            castThousandNeedles(target);
            return 45;
        }
        if (roll < 40 && pay(50f)) {
            castIceMirrors(target);
            return 70;
        }
        pay(35f);
        castThousandNeedles(target);
        return 45;
    }

    /** Kurotsuchi: Lava Release. What lands keeps burning, and what does not blinds you. */
    private int castKurotsuchi(LivingEntity target, double distance) {
        int roll = this.boss.getRandom().nextInt(100);
        if (distance > CLOSE_RANGE) {
            if (roll < 55 && pay(45f)) {
                castLavaSpit(target);
                return 60;
            }
            pay(40f);
            castQuicklime(target);
            return 65;
        }
        if (roll < 50 && pay(40f)) {
            castQuicklime(target);
            return 65;
        }
        pay(45f);
        castLavaSpit(target);
        return 60;
    }

    /** Might Guy: no ninjutsu at all, and he does not need any. */
    private int castMightGuy(LivingEntity target, double distance) {
        int roll = this.boss.getRandom().nextInt(100);
        if (distance > CLOSE_RANGE) {
            // Closing the distance IS the technique. He does not stand and trade at range.
            pay(30f);
            castBladeRush(target, this.boss.getVariant().attackDamage() * 1.4f);
            return 35;
        }
        if (roll < 45 && pay(60f)) {
            castEveningElephant(target);
            return 80;
        }
        pay(45f);
        castMorningPeacock(target);
        return 55;
    }

    /** Sakura: one punch, and the ground under it stops existing. */
    private int castSakura(LivingEntity target, double distance) {
        int roll = this.boss.getRandom().nextInt(100);
        if (this.boss.getHealth() < this.boss.getMaxHealth() * 0.35f && pay(70f)) {
            castCreationRebirth();
            return 200;
        }
        if (distance > CLOSE_RANGE) {
            pay(50f);
            castMonstrousStrength();
            return 70;
        }
        if (roll < 55 && pay(45f)) {
            castChakraPunch(target);
            return 45;
        }
        pay(50f);
        castMonstrousStrength();
        return 70;
    }

    /** Tenten: a scroll for every weapon ever made, and she throws all of them. */
    private int castTenten(LivingEntity target, double distance) {
        int roll = this.boss.getRandom().nextInt(100);
        if (distance > CLOSE_RANGE) {
            if (roll < 55 && pay(45f)) {
                castWeaponRain(target);
                return 65;
            }
            pay(30f);
            castShurikenVolley(target);
            return 35;
        }
        if (roll < 40 && pay(45f)) {
            castWeaponRain(target);
            return 65;
        }
        pay(30f);
        castShurikenVolley(target);
        return 35;
    }

    /** Iruka: the basics, done properly. He is the gentlest thing in the Bingo Book. */
    private int castIruka(LivingEntity target, double distance) {
        if (distance > CLOSE_RANGE) {
            pay(25f);
            castShurikenVolley(target);
            return 40;
        }
        pay(25f);
        castBladeRush(target, this.boss.getVariant().attackDamage());
        return 35;
    }

    /** White Zetsu: there is never one of it, and it is never quite where you looked. */
    private int castWhiteZetsu(LivingEntity target, double distance) {
        int roll = this.boss.getRandom().nextInt(100);
        if (roll < 30 && pay(55f)) {
            castMultipleShadowClones();
            return 140;
        }
        if (distance > CLOSE_RANGE) {
            pay(40f);
            castSinkAway();
            return 90;
        }
        pay(30f);
        castHiddenBlades(target);
        return 40;
    }

    // ------------------------------------------------------------------ techniques

    private static final DustParticleOptions SAND_TAN =
            new DustParticleOptions(new Vector3f(0.85F, 0.75F, 0.45F), 1.2F);

    /** Sand Shuriken, in effect: compressed sand fired hard enough to cut. */
    private void castSandBullet(LivingEntity target) {
        Vec3 origin = this.boss.position().add(0, this.boss.getBbHeight() * 0.8, 0);
        Vec3 impact = target.position().add(0, target.getBbHeight() * 0.5, 0);
        for (LivingEntity victim : this.boss.level().getEntitiesOfClass(LivingEntity.class,
                new net.minecraft.world.phys.AABB(impact, impact).inflate(2.2),
                candidate -> candidate != this.boss && candidate.isAlive())) {
            victim.hurt(this.boss.damageSources().mobAttack(this.boss), 9f);
            victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 0));
        }
        if (this.boss.level() instanceof ServerLevel serverLevel) {
            NarutoParticles.spawnBolt(serverLevel, origin, impact, 2, 0.3, SAND_TAN);
            NarutoParticles.spawnBurst(serverLevel, impact, 30, 1.4, SAND_TAN);
        }
        this.boss.level().playSound(null, this.boss.blockPosition(),
                SoundEvents.SAND_BREAK, SoundSource.HOSTILE, 1.3f, 0.6f);
    }

    /**
     * Sabaku Kyu into Sabaku Sōsō - the sand closes on one target and crushes.
     *
     * Single-target and slow to come round again, because it is the technique that ends
     * fights: pinned in place, unable to move, taking damage the whole time.
     */
    private void castSandBindingCoffin(LivingEntity target) {
        target.hurt(this.boss.damageSources().mobAttack(this.boss), 16f);
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 4));
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 1));
        target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 100, 2));
        // Held where it stands: the coffin is about not being able to move at all.
        target.setDeltaMovement(0, target.getDeltaMovement().y * 0.1, 0);
        target.hurtMarked = true;
        if (this.boss.level() instanceof ServerLevel serverLevel) {
            Vec3 centre = target.position();
            for (double r = 0.6; r <= 1.8; r += 0.6) {
                NarutoParticles.spawnRing(serverLevel, centre.add(0, r, 0), 1.6 - r * 0.4, 18, SAND_TAN);
            }
            NarutoParticles.spawnBurst(serverLevel, centre.add(0, 1.0, 0), 60, 1.6, SAND_TAN);
        }
        this.boss.level().playSound(null, this.boss.blockPosition(),
                SoundEvents.SAND_PLACE, SoundSource.HOSTILE, 1.6f, 0.5f);
    }

    /** Sand Tsunami: a wave rolled out from the gourd, wide and unaimed. */
    private void castSandTsunami(LivingEntity target) {
        Vec3 origin = this.boss.position();
        Vec3 direction = target.position().subtract(origin).normalize();
        for (double step = 2.0; step <= 16.0; step += 2.0) {
            Vec3 point = origin.add(direction.scale(step));
            for (LivingEntity victim : this.boss.level().getEntitiesOfClass(LivingEntity.class,
                    new net.minecraft.world.phys.AABB(point, point).inflate(3.0),
                    candidate -> candidate != this.boss && candidate.isAlive())) {
                if (victim.hurt(this.boss.damageSources().mobAttack(this.boss), 8f)) {
                    victim.setDeltaMovement(direction.x * 1.1, 0.45, direction.z * 1.1);
                    victim.hurtMarked = true;
                }
            }
            if (this.boss.level() instanceof ServerLevel serverLevel) {
                NarutoParticles.spawnRing(serverLevel, point, 2.8, 20, SAND_TAN);
            }
        }
        this.boss.level().playSound(null, this.boss.blockPosition(),
                SoundEvents.SAND_BREAK, SoundSource.HOSTILE, 2.0f, 0.4f);
    }

    // --- Phase 20C: the Sand siblings, the second Akatsuki tier, and the Leaf's own -------

    private static final DustParticleOptions WIND_PALE =
            new DustParticleOptions(new Vector3f(0.82F, 0.92F, 0.88F), 1.1F);
    private static final DustParticleOptions LAVA_RED =
            new DustParticleOptions(new Vector3f(1.0F, 0.30F, 0.05F), 1.4F);
    private static final DustParticleOptions LIME_WHITE =
            new DustParticleOptions(new Vector3f(0.95F, 0.95F, 0.88F), 1.3F);
    private static final DustParticleOptions THREAD_BLACK =
            new DustParticleOptions(new Vector3f(0.12F, 0.12F, 0.14F), 1.0F);

    /** Everything worth hitting near a point. The boss never catches itself. */
    private java.util.List<LivingEntity> victimsNear(Vec3 centre, double radius) {
        return this.boss.level().getEntitiesOfClass(LivingEntity.class,
                new net.minecraft.world.phys.AABB(centre, centre).inflate(radius),
                candidate -> candidate != this.boss && candidate.isAlive());
    }

    /** Kankuro's puppets breathe poison as much as they stab with it. */
    private void castPoisonMist(LivingEntity target) {
        Vec3 centre = target.position();
        for (LivingEntity victim : victimsNear(centre, 4.5)) {
            victim.hurt(this.boss.damageSources().mobAttack(this.boss), 6f);
            victim.addEffect(new MobEffectInstance(MobEffects.POISON, 180, 1));
            victim.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 120, 0));
        }
        if (this.boss.level() instanceof ServerLevel serverLevel) {
            NarutoParticles.spawnBurst(serverLevel, centre.add(0, 1.2, 0), 70, 3.0, POISON_GREEN);
        }
        this.boss.level().playSound(null, this.boss.blockPosition(),
                SoundEvents.BREWING_STAND_BREW, SoundSource.HOSTILE, 1.4f, 0.6f);
    }

    /** Blades out of a sleeve - a puppet's, or a Zetsu's own arm. */
    private void castHiddenBlades(LivingEntity target) {
        if (target.hurt(this.boss.damageSources().mobAttack(this.boss),
                this.boss.getVariant().attackDamage() * 1.5f)) {
            target.addEffect(new MobEffectInstance(MobEffects.POISON, 120, 0));
        }
        if (this.boss.level() instanceof ServerLevel serverLevel) {
            NarutoParticles.spawnBurst(serverLevel,
                    target.position().add(0, target.getBbHeight() * 0.6, 0), 20, 0.9,
                    NarutoParticles.METAL_GRAY);
        }
        this.boss.level().playSound(null, this.boss.blockPosition(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 1.0f, 1.4f);
    }

    /** Kamaitachi: a blade of wind down a line, and it cuts through armour. */
    private void castWindScythe(LivingEntity target) {
        Vec3 origin = this.boss.position().add(0, this.boss.getBbHeight() * 0.7, 0);
        Vec3 direction = target.position().add(0, target.getBbHeight() * 0.5, 0)
                .subtract(origin).normalize();
        for (double step = 2.0; step <= 20.0; step += 2.0) {
            Vec3 point = origin.add(direction.scale(step));
            for (LivingEntity victim : victimsNear(point, 1.8)) {
                if (victim.hurt(this.boss.damageSources().mobAttack(this.boss), 11f)) {
                    victim.setDeltaMovement(direction.x * 0.9, 0.3, direction.z * 0.9);
                    victim.hurtMarked = true;
                }
            }
        }
        if (this.boss.level() instanceof ServerLevel serverLevel) {
            NarutoParticles.spawnBolt(serverLevel, origin, origin.add(direction.scale(20.0)),
                    2, 0.25, WIND_PALE);
        }
        this.boss.level().playSound(null, this.boss.blockPosition(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 1.8f, 0.9f);
    }

    /** The fan opened all three moons: a whirlwind that lifts everything around her. */
    private void castCuttingWhirlwind() {
        Vec3 centre = this.boss.position();
        for (LivingEntity victim : victimsNear(centre, 8.0)) {
            if (victim.hurt(this.boss.damageSources().mobAttack(this.boss), 9f)) {
                Vec3 push = victim.position().subtract(centre).normalize().scale(1.2);
                victim.setDeltaMovement(push.x, 0.75, push.z);
                victim.hurtMarked = true;
            }
        }
        if (this.boss.level() instanceof ServerLevel serverLevel) {
            for (double r = 2.0; r <= 8.0; r += 2.0) {
                NarutoParticles.spawnRing(serverLevel, centre.add(0, r * 0.2, 0), r, (int) (r * 6), WIND_PALE);
            }
        }
        this.boss.level().playSound(null, this.boss.blockPosition(),
                SoundEvents.ENDER_DRAGON_FLAP, SoundSource.HOSTILE, 2.2f, 0.8f);
    }

    /** Kakuzu's lightning mask. One bolt, and it does not care what you are wearing. */
    private void castLightningMask(LivingEntity target) {
        Vec3 origin = this.boss.position().add(0, this.boss.getBbHeight() * 0.8, 0);
        Vec3 impact = target.position().add(0, target.getBbHeight() * 0.5, 0);
        for (LivingEntity victim : victimsNear(impact, 2.5)) {
            victim.hurt(this.boss.damageSources().mobAttack(this.boss), 14f);
            victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
        }
        if (this.boss.level() instanceof ServerLevel serverLevel) {
            NarutoParticles.spawnBolt(serverLevel, origin, impact, 4, 0.7,
                    NarutoParticles.LIGHTNING_GOLD);
        }
        this.boss.level().playSound(null, this.boss.blockPosition(),
                SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.HOSTILE, 1.2f, 1.4f);
    }

    /** Earth Spear: he hardens, and for a while nothing gets through. */
    private void castEarthSpear() {
        this.boss.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 160, 2, false, false));
        this.boss.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 160, 0, false, false));
        if (this.boss.level() instanceof ServerLevel serverLevel) {
            NarutoParticles.spawnRing(serverLevel, this.boss.position().add(0, 1.0, 0), 1.5, 22, IRON_SAND);
        }
        this.boss.level().playSound(null, this.boss.blockPosition(),
                SoundEvents.STONE_PLACE, SoundSource.HOSTILE, 1.4f, 0.5f);
    }

    /** The threads. His arm comes off and reaches across the room. */
    private void castThreadStrike(LivingEntity target) {
        Vec3 origin = this.boss.position().add(0, this.boss.getBbHeight() * 0.6, 0);
        Vec3 impact = target.position().add(0, target.getBbHeight() * 0.5, 0);
        if (target.hurt(this.boss.damageSources().mobAttack(this.boss),
                this.boss.getVariant().attackDamage() * 1.6f)) {
            // Dragged in: the threads pull you back to him.
            Vec3 pull = origin.subtract(impact).normalize().scale(0.9);
            target.setDeltaMovement(pull.x, 0.25, pull.z);
            target.hurtMarked = true;
        }
        if (this.boss.level() instanceof ServerLevel serverLevel) {
            NarutoParticles.spawnBolt(serverLevel, origin, impact, 3, 0.3, THREAD_BLACK);
        }
        this.boss.level().playSound(null, this.boss.blockPosition(),
                SoundEvents.LEASH_KNOT_PLACE, SoundSource.HOSTILE, 1.4f, 0.6f);
    }

    /**
     * Demonic Mirroring Ice Crystals: he steps out of one mirror and into another, and the
     * needles arrive from wherever he is not.
     */
    private void castIceMirrors(LivingEntity target) {
        if (!(this.boss.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        Vec3 centre = target.position();
        java.util.List<com.sekwah.narutomod.entity.jutsuprojectile.IceMirrorEntity> standing =
                this.standingMirrors(centre);

        // The ring goes up once and then stands until it is broken. Re-raising it every cast
        // would make the mirrors decoration again; leaving them up is what turns the technique
        // into a structure the player has to dismantle.
        if (standing.isEmpty()) {
            standing = com.sekwah.narutomod.entity.jutsuprojectile.IceMirrorEntity
                    .raiseRing(this.boss, centre);
        }

        // Step into one of the surviving panes, and cut once from it on the way through.
        com.sekwah.narutomod.entity.jutsuprojectile.IceMirrorEntity into =
                standing.get(this.boss.getRandom().nextInt(standing.size()));
        this.boss.randomTeleport(into.getX(), into.getY(), into.getZ(), false);
        this.boss.getLookControl().setLookAt(target, 30f, 30f);

        target.invulnerableTime = 0;
        target.hurt(this.boss.damageSources().mobAttack(this.boss), 7f);
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 1));
        NarutoParticles.spawnBolt(serverLevel, into.position().add(0, 1.2, 0),
                target.position().add(0, target.getBbHeight() * 0.5, 0), 2, 0.2,
                NarutoParticles.ICE_PALE);
        this.boss.level().playSound(null, this.boss.blockPosition(),
                SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 1.6f, 1.2f);
    }

    /** This wielder's own mirrors that are still standing near a point. */
    private java.util.List<com.sekwah.narutomod.entity.jutsuprojectile.IceMirrorEntity> standingMirrors(Vec3 centre) {
        return this.boss.level().getEntitiesOfClass(
                com.sekwah.narutomod.entity.jutsuprojectile.IceMirrorEntity.class,
                new net.minecraft.world.phys.AABB(centre, centre).inflate(
                        com.sekwah.narutomod.entity.jutsuprojectile.IceMirrorEntity.RING_RADIUS + 4.0),
                mirror -> mirror.isAlive()
                        && mirror.getOwnerUUID().map(this.boss.getUUID()::equals).orElse(false));
    }

    /** A Thousand Flying Water Needles, frozen. A cone of them, and they all land. */
    private void castThousandNeedles(LivingEntity target) {
        Vec3 origin = this.boss.position().add(0, this.boss.getBbHeight() * 0.7, 0);
        Vec3 direction = target.position().add(0, target.getBbHeight() * 0.5, 0)
                .subtract(origin).normalize();
        for (double step = 1.5; step <= 14.0; step += 1.5) {
            Vec3 point = origin.add(direction.scale(step));
            for (LivingEntity victim : victimsNear(point, 2.2)) {
                victim.hurt(this.boss.damageSources().mobAttack(this.boss), 6f);
                victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 1));
            }
            if (this.boss.level() instanceof ServerLevel serverLevel) {
                NarutoParticles.spawnBurst(serverLevel, point, 12, 1.0, NarutoParticles.ICE_PALE);
            }
        }
        this.boss.level().playSound(null, this.boss.blockPosition(),
                SoundEvents.ARROW_SHOOT, SoundSource.HOSTILE, 1.4f, 1.6f);
    }

    /** Lava Release: it lands, and then it keeps burning. */
    private void castLavaSpit(LivingEntity target) {
        Vec3 impact = target.position();
        for (LivingEntity victim : victimsNear(impact, 3.5)) {
            victim.hurt(this.boss.damageSources().mobAttack(this.boss), 10f);
            victim.setSecondsOnFire(7);
        }
        if (this.boss.level() instanceof ServerLevel serverLevel) {
            NarutoParticles.spawnBolt(serverLevel,
                    this.boss.position().add(0, this.boss.getBbHeight() * 0.8, 0),
                    impact.add(0, 1.0, 0), 3, 0.5, LAVA_RED);
            NarutoParticles.spawnRing(serverLevel, impact.add(0, 0.2, 0), 3.0, 26, LAVA_RED);
        }
        this.boss.level().playSound(null, this.boss.blockPosition(),
                SoundEvents.LAVA_POP, SoundSource.HOSTILE, 2.0f, 0.6f);
    }

    /** Quicklime Congealing: it goes in your eyes first and sets afterwards. */
    private void castQuicklime(LivingEntity target) {
        Vec3 impact = target.position();
        for (LivingEntity victim : victimsNear(impact, 4.0)) {
            victim.hurt(this.boss.damageSources().mobAttack(this.boss), 5f);
            victim.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 120, 0));
            victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 140, 2));
        }
        if (this.boss.level() instanceof ServerLevel serverLevel) {
            NarutoParticles.spawnBurst(serverLevel, impact.add(0, 1.2, 0), 70, 3.2, LIME_WHITE);
        }
        this.boss.level().playSound(null, this.boss.blockPosition(),
                SoundEvents.LAVA_EXTINGUISH, SoundSource.HOSTILE, 1.6f, 0.9f);
    }

    /** Morning Peacock: so many punches the air catches fire. */
    private void castMorningPeacock(LivingEntity target) {
        for (int i = 0; i < 6; i++) {
            target.invulnerableTime = 0;
            target.hurt(this.boss.damageSources().mobAttack(this.boss),
                    this.boss.getVariant().attackDamage() * 0.55f);
        }
        target.setSecondsOnFire(4);
        if (this.boss.level() instanceof ServerLevel serverLevel) {
            NarutoParticles.spawnBurst(serverLevel,
                    target.position().add(0, target.getBbHeight() * 0.6, 0), 60, 1.6, LAVA_RED);
        }
        this.boss.level().playSound(null, this.boss.blockPosition(),
                SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.HOSTILE, 2.0f, 1.3f);
    }

    /** Evening Elephant: one hit, and everything behind it goes too. */
    private void castEveningElephant(LivingEntity target) {
        Vec3 direction = target.position().subtract(this.boss.position()).normalize();
        Vec3 centre = this.boss.position().add(direction.scale(3.0));
        for (LivingEntity victim : victimsNear(centre, 5.0)) {
            if (victim.hurt(this.boss.damageSources().mobAttack(this.boss),
                    this.boss.getVariant().attackDamage() * 2.2f)) {
                victim.setDeltaMovement(direction.x * 1.8, 0.6, direction.z * 1.8);
                victim.hurtMarked = true;
            }
        }
        if (this.boss.level() instanceof ServerLevel serverLevel) {
            NarutoParticles.spawnRing(serverLevel, centre, 4.5, 30, WIND_PALE);
            serverLevel.sendParticles(ParticleTypes.EXPLOSION, centre.x, centre.y + 1.0, centre.z,
                    4, 1.2, 0.6, 1.2, 0.0);
        }
        this.boss.level().playSound(null, this.boss.blockPosition(),
                SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 2.0f, 0.7f);
    }

    /** Creation Rebirth: the stored chakra goes back in, and the wounds close. */
    private void castCreationRebirth() {
        this.boss.heal(this.boss.getMaxHealth() * 0.30f);
        this.boss.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 1, false, false));
        if (this.boss.level() instanceof ServerLevel serverLevel) {
            NarutoParticles.spawnRing(serverLevel, this.boss.position().add(0, 1.0, 0), 1.8, 26,
                    new DustParticleOptions(new Vector3f(0.4F, 1.0F, 0.6F), 1.2F));
            serverLevel.sendParticles(ParticleTypes.HEART,
                    this.boss.getX(), this.boss.getY() + 2.0, this.boss.getZ(), 12, 0.6, 0.5, 0.6, 0.0);
        }
        this.boss.level().playSound(null, this.boss.blockPosition(),
                SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, 1.4f, 1.2f);
    }

    /** Shannaro: the fist goes into the ground and the ground stops being flat. */
    private void castMonstrousStrength() {
        Vec3 centre = this.boss.position();
        for (LivingEntity victim : victimsNear(centre, 7.0)) {
            if (victim.hurt(this.boss.damageSources().mobAttack(this.boss),
                    this.boss.getVariant().attackDamage() * 1.3f)) {
                Vec3 push = victim.position().subtract(centre).normalize().scale(0.9);
                victim.setDeltaMovement(push.x, 0.8, push.z);
                victim.hurtMarked = true;
            }
        }
        if (this.boss.level() instanceof ServerLevel serverLevel) {
            NarutoParticles.spawnRing(serverLevel, centre, 6.5, 40, NarutoParticles.LOG_BROWN);
            serverLevel.sendParticles(ParticleTypes.EXPLOSION, centre.x, centre.y, centre.z,
                    5, 2.0, 0.2, 2.0, 0.0);
        }
        this.boss.level().playSound(null, this.boss.blockPosition(),
                SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 2.2f, 0.5f);
    }

    /** Chakra Enhanced Strength, on one target. It is not subtle and it does not need to be. */
    private void castChakraPunch(LivingEntity target) {
        if (target.hurt(this.boss.damageSources().mobAttack(this.boss),
                this.boss.getVariant().attackDamage() * 2.4f)) {
            Vec3 direction = target.position().subtract(this.boss.position()).normalize();
            target.setDeltaMovement(direction.x * 1.6, 0.7, direction.z * 1.6);
            target.hurtMarked = true;
        }
        if (this.boss.level() instanceof ServerLevel serverLevel) {
            NarutoParticles.spawnBurst(serverLevel,
                    target.position().add(0, target.getBbHeight() * 0.5, 0), 40, 1.2,
                    NarutoParticles.ROTATION_WHITE);
        }
        this.boss.level().playSound(null, this.boss.blockPosition(),
                SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.HOSTILE, 2.0f, 0.8f);
    }

    /** Rising Twin Dragons: the scrolls open and everything in them comes down at once. */
    private void castWeaponRain(LivingEntity target) {
        Vec3 centre = target.position();
        for (LivingEntity victim : victimsNear(centre, 5.0)) {
            for (int i = 0; i < 3; i++) {
                victim.invulnerableTime = 0;
                victim.hurt(this.boss.damageSources().mobAttack(this.boss), 5f);
            }
        }
        if (this.boss.level() instanceof ServerLevel serverLevel) {
            for (int i = 0; i < 18; i++) {
                double angle = this.boss.getRandom().nextDouble() * Math.PI * 2;
                double radius = this.boss.getRandom().nextDouble() * 4.5;
                Vec3 from = centre.add(Math.cos(angle) * radius, 8.0, Math.sin(angle) * radius);
                NarutoParticles.spawnBolt(serverLevel, from,
                        from.subtract(0, 8.0, 0), 1, 0.1, NarutoParticles.METAL_GRAY);
            }
        }
        this.boss.level().playSound(null, this.boss.blockPosition(),
                SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 1.6f, 1.3f);
    }

    /** A plain volley of shuriken. Every ninja in the world can do this one. */
    private void castShurikenVolley(LivingEntity target) {
        Vec3 origin = this.boss.position().add(0, this.boss.getBbHeight() * 0.7, 0);
        Vec3 impact = target.position().add(0, target.getBbHeight() * 0.5, 0);
        for (LivingEntity victim : victimsNear(impact, 2.0)) {
            for (int i = 0; i < 2; i++) {
                victim.invulnerableTime = 0;
                victim.hurt(this.boss.damageSources().mobAttack(this.boss), 4.5f);
            }
        }
        if (this.boss.level() instanceof ServerLevel serverLevel) {
            NarutoParticles.spawnBolt(serverLevel, origin, impact, 1, 0.2, NarutoParticles.METAL_GRAY);
        }
        this.boss.level().playSound(null, this.boss.blockPosition(),
                SoundEvents.ARROW_SHOOT, SoundSource.HOSTILE, 1.0f, 1.2f);
    }

    /** Zetsu goes into the ground and comes up somewhere you were not watching. */
    private void castSinkAway() {
        this.boss.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 120, 0, false, false));
        this.boss.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 120, 1, false, false));
        if (this.boss.level() instanceof ServerLevel serverLevel) {
            NarutoParticles.spawnRing(serverLevel, this.boss.position(), 1.2, 18,
                    new DustParticleOptions(new Vector3f(0.85F, 0.90F, 0.85F), 1.2F));
            serverLevel.sendParticles(ParticleTypes.CLOUD,
                    this.boss.getX(), this.boss.getY() + 0.2, this.boss.getZ(), 25, 0.5, 0.2, 0.5, 0.01);
        }
        this.boss.level().playSound(null, this.boss.blockPosition(),
                SoundEvents.SLIME_SQUISH, SoundSource.HOSTILE, 1.2f, 0.7f);
    }

    /** Itachi: Amaterasu — the flame entity spreads on its own from where it lands. */
    private void castBlackFlame(LivingEntity target) {
        AmaterasuFireEntity fire = new AmaterasuFireEntity(this.boss.level(), this.boss,
                target.getX(), target.getY(), target.getZ());
        fire.setDamageMultiplier(1.2f);
        this.boss.level().addFreshEntity(fire);
        playCastSound(SoundEvents.WARDEN_SONIC_BOOM, 1.0f);
    }

    /** Itachi: Tsukuyomi — no damage worth speaking of, but you stop being a participant. */
    private void castTsukuyomi(LivingEntity target) {
        target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 8 * 20, 0, false, false));
        target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 12 * 20, 0, false, false));
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 12 * 20, 2, false, true));
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 6 * 20, 2, false, true));
        target.hurt(this.boss.damageSources().indirectMagic(this.boss, this.boss), 6f);
        playCastSound(SoundEvents.ELDER_GUARDIAN_CURSE, 0.7f);

        if (this.boss.level() instanceof ServerLevel serverLevel) {
            NarutoParticles.spawnRing(serverLevel, target.getEyePosition(), 1.2, 32,
                    NarutoParticles.GENJUTSU_RED);
            NarutoParticles.spawnBurst(serverLevel, target.position().add(0, 1.2, 0), 30, 1.0, CROW_BLACK);
        }
    }

    /** Itachi: a murder of crows that blinds and cuts on the way past. */
    private void castCrowMurder(LivingEntity target) {
        target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 5 * 20, 0, false, true));
        target.hurt(this.boss.damageSources().mobAttack(this.boss), 5f);
        playCastSound(SoundEvents.PARROT_IMITATE_WITHER_SKELETON, 0.8f);

        if (this.boss.level() instanceof ServerLevel serverLevel) {
            NarutoParticles.spawnBurst(serverLevel, target.position().add(0, 1.2, 0), 30, 1.1, CROW_BLACK);
        }
    }

    /** Shared Uchiha staple — a real charged fireball, aimed the way a player would aim it. */
    private void castGreatFireball(LivingEntity target, int charge) {
        Vec3 aim = aimVector(target);
        FireballJutsuEntity fireball = new FireballJutsuEntity(this.boss, aim.x, aim.y, aim.z);
        fireball.setChargeAmount(charge, true, 1.0f);
        this.boss.level().addFreshEntity(fireball);
        playCastSound(SoundEvents.BLAZE_SHOOT, 0.8f);
    }

    /** Sasuke: Kirin — a branching bolt out of the sky, using the shared fractal helper. */
    private void castKirin(LivingEntity target) {
        Vec3 impact = target.position();
        if (this.boss.level() instanceof ServerLevel serverLevel) {
            Vec3 sky = impact.add(0, 40, 0);
            NarutoParticles.spawnBolt(serverLevel, sky, impact.add(0, 1.0, 0), 5, 2.5,
                    NarutoParticles.CHIDORI_CYAN);
            for (int branch = 0; branch < 3; branch++) {
                Vec3 offset = impact.add(
                        (this.boss.getRandom().nextDouble() - 0.5) * 8,
                        12 + this.boss.getRandom().nextDouble() * 12,
                        (this.boss.getRandom().nextDouble() - 0.5) * 8);
                NarutoParticles.spawnBolt(serverLevel, offset, impact.add(0, 1.0, 0), 4, 1.8,
                        NarutoParticles.LIGHTNING_GOLD);
            }
            serverLevel.sendParticles(ParticleTypes.EXPLOSION, impact.x, impact.y + 0.5, impact.z,
                    4, 1.0, 0.5, 1.0, 0.0);
        }
        for (LivingEntity caught : nearby(impact, 3.5)) {
            caught.hurt(this.boss.damageSources().mobAttack(this.boss), caught == target ? 22f : 12f);
            caught.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 4 * 20, 1, false, true));
        }
        playCastSound(SoundEvents.LIGHTNING_BOLT_THUNDER, 0.8f);
    }

    /** Sasuke: closes the gap in a lightning-wrapped charge. */
    private void castChidoriDash(LivingEntity target) {
        Vec3 toTarget = target.position().subtract(this.boss.position()).normalize();
        this.boss.setDeltaMovement(toTarget.scale(1.8).add(0, 0.3, 0));
        this.boss.hurtMarked = true;
        target.hurt(this.boss.damageSources().mobAttack(this.boss), 15f);
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 3 * 20, 1, false, true));
        playCastSound(SoundEvents.LIGHTNING_BOLT_IMPACT, 1.4f);

        if (this.boss.level() instanceof ServerLevel serverLevel) {
            NarutoParticles.spawnBolt(serverLevel, this.boss.getEyePosition(),
                    target.position().add(0, 1.0, 0), 4, 0.8, NarutoParticles.CHIDORI_CYAN);
        }
    }

    /**
     * Sasuke: a standing Chidori through the chest. The dash reads as movement; this is the
     * technique itself, and it ignores armour the way a fistful of lightning should.
     */
    private void castChidoriThrust(LivingEntity target) {
        target.hurt(this.boss.damageSources().indirectMagic(this.boss, this.boss), 17f);
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 4 * 20, 2, false, true));
        playCastSound(SoundEvents.LIGHTNING_BOLT_IMPACT, 1.5f);

        if (this.boss.level() instanceof ServerLevel serverLevel) {
            NarutoParticles.spawnBolt(serverLevel, this.boss.getEyePosition(),
                    target.position().add(0, target.getBbHeight() * 0.6, 0), 3, 0.35,
                    NarutoParticles.CHIDORI_CYAN);
            NarutoParticles.spawnBurst(serverLevel, target.position().add(0, 1.0, 0), 24, 0.7,
                    NarutoParticles.CHIDORI_CYAN);
        }
    }

    /** Sasuke: Chidori Nagashi — current through everything touching him. */
    private void castNagashi() {
        for (LivingEntity caught : nearby(this.boss.position(), 5.0)) {
            caught.hurt(this.boss.damageSources().mobAttack(this.boss), 11f);
            caught.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 4 * 20, 1, false, true));
        }
        playCastSound(SoundEvents.LIGHTNING_BOLT_IMPACT, 1.6f);

        if (this.boss.level() instanceof ServerLevel serverLevel) {
            NarutoParticles.spawnRing(serverLevel, this.boss.position().add(0, 1.0, 0), 4.5, 40,
                    NarutoParticles.CHIDORI_CYAN);
        }
    }

    /** Madara: the fan sweep that hurls you back across the clearing. */
    private void castShockwave(LivingEntity target) {
        target.hurt(this.boss.damageSources().mobAttack(this.boss), 14f);
        Vec3 push = target.position().subtract(this.boss.position()).normalize().scale(2.4).add(0, 0.6, 0);
        target.setDeltaMovement(target.getDeltaMovement().add(push));
        target.hurtMarked = true;
        playCastSound(SoundEvents.PHANTOM_SWOOP, 0.9f);

        if (this.boss.level() instanceof ServerLevel serverLevel) {
            NarutoParticles.spawnRing(serverLevel, this.boss.position().add(0, 1.0, 0), 3.0, 30,
                    ParticleTypes.CLOUD);
        }
    }

    /** Madara: once the shell is up, the shell is the weapon. */
    private void castSusanooSweep() {
        for (LivingEntity caught : nearby(this.boss.position(), 7.0)) {
            caught.hurt(this.boss.damageSources().mobAttack(this.boss), 18f);
            Vec3 push = caught.position().subtract(this.boss.position()).normalize().scale(1.4).add(0, 0.5, 0);
            caught.setDeltaMovement(caught.getDeltaMovement().add(push));
            caught.hurtMarked = true;
        }
        playCastSound(SoundEvents.WITHER_BREAK_BLOCK, 0.6f);

        if (this.boss.level() instanceof ServerLevel serverLevel) {
            NarutoParticles.spawnRing(serverLevel, this.boss.position().add(0, 1.5, 0), 6.5, 48,
                    NarutoParticles.SHARINGAN_RED);
        }
    }

    /** Shisui: Kotoamatsukami — everything that makes you dangerous, switched off. */
    private void castKotoamatsukami(LivingEntity target) {
        target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 10 * 20, 0, false, false));
        target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 15 * 20, 0, false, false));
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 15 * 20, 2, false, true));
        target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 15 * 20, 2, false, true));
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 8 * 20, 2, false, true));
        playCastSound(SoundEvents.EVOKER_CAST_SPELL, 0.6f);

        if (this.boss.level() instanceof ServerLevel serverLevel) {
            NarutoParticles.spawnRing(serverLevel, target.getEyePosition(), 1.0, 28,
                    NarutoParticles.GENJUTSU_RED);
            NarutoParticles.spawnSpiral(serverLevel, target.position(), 1.4, 0.12, 24,
                    NarutoParticles.SHARINGAN_RED);
        }
    }

    /** Obito: goes intangible for a moment and comes back healed. */
    private void castIntangible() {
        this.boss.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 4 * 20, 3, false, true));
        this.boss.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 4 * 20, 1, false, true));
        this.boss.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 6 * 20, 1, false, true));
        playCastSound(SoundEvents.SHULKER_TELEPORT, 0.6f);

        if (this.boss.level() instanceof ServerLevel serverLevel) {
            NarutoParticles.spawnSpiral(serverLevel, this.boss.position(), 1.0, 0.15, 20,
                    NarutoParticles.SHARINGAN_RED);
        }
    }

    /** Obito: the other end of Kamui — you come to him. */
    private void castKamuiPull(LivingEntity target) {
        Vec3 pull = this.boss.position().subtract(target.position()).normalize().scale(1.8).add(0, 0.4, 0);
        target.setDeltaMovement(pull);
        target.hurtMarked = true;
        target.hurt(this.boss.damageSources().mobAttack(this.boss), 8f);
        target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 5 * 20, 0, false, false));
        playCastSound(SoundEvents.PORTAL_TRIGGER, 1.4f);

        if (this.boss.level() instanceof ServerLevel serverLevel) {
            NarutoParticles.spawnSpiral(serverLevel, target.position(), 1.6, 0.14, 22,
                    NarutoParticles.SHARINGAN_RED);
        }
    }

    /** Obito: phases out and re-emerges behind you, striking as he lands. */
    private void castPhaseStrike(LivingEntity target) {
        if (this.boss.level() instanceof ServerLevel serverLevel) {
            NarutoParticles.spawnBurst(serverLevel, this.boss.position().add(0, 1.0, 0), 20, 0.6,
                    NarutoParticles.SHARINGAN_RED);
        }
        Vec3 behind = target.position().subtract(target.getLookAngle().scale(1.5));
        this.boss.teleportTo(behind.x, target.getY(), behind.z);
        target.hurt(this.boss.damageSources().mobAttack(this.boss), 11f);
        playCastSound(SoundEvents.SHULKER_TELEPORT, 1.0f);
    }

    /** Kisame: a bubble of water that holds you still and drowns you in it. */
    private void castWaterPrison(LivingEntity target) {
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 6 * 20, 4, false, true));
        target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 6 * 20, 2, false, true));
        target.hurt(this.boss.damageSources().drown(), 9f);
        target.setAirSupply(Math.max(0, target.getAirSupply() - 120));
        playCastSound(SoundEvents.PLAYER_SPLASH_HIGH_SPEED, 0.7f);

        if (this.boss.level() instanceof ServerLevel serverLevel) {
            NarutoParticles.spawnRing(serverLevel, target.position().add(0, 1.0, 0), 1.2, 30,
                    NarutoParticles.WATER_BLUE);
        }
    }

    /** Kisame: Samehada takes a bite out of you and gives it to him. */
    private void castSamehadaDrain(LivingEntity target) {
        target.hurt(this.boss.damageSources().mobAttack(this.boss), 12f);
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 8 * 20, 1, false, true));
        this.boss.heal(10f);
        playCastSound(SoundEvents.GUARDIAN_ATTACK, 0.8f);

        if (this.boss.level() instanceof ServerLevel serverLevel) {
            NarutoParticles.spawnBolt(serverLevel, target.getEyePosition(),
                    this.boss.getEyePosition(), 3, 0.4, NarutoParticles.WATER_BLUE);
        }
    }

    /** Zabuza: the mist he does his actual work inside. */
    private void castHiddenMist() {
        for (LivingEntity caught : nearby(this.boss.position(), 14.0)) {
            caught.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 10 * 20, 0, false, false));
            caught.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 10 * 20, 0, false, true));
        }
        this.boss.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 12 * 20, 1, false, true));
        this.boss.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 12 * 20, 0, false, true));
        playCastSound(SoundEvents.FIRE_EXTINGUISH, 0.5f);

        if (this.boss.level() instanceof ServerLevel serverLevel) {
            for (int ring = 1; ring <= 4; ring++) {
                NarutoParticles.spawnRing(serverLevel, this.boss.position().add(0, 0.6, 0),
                        ring * 3.0, 30, ParticleTypes.CLOUD);
            }
        }
    }

    /** Zabuza: a single heavy water round rather than a volley. */
    private void castWaterDragon(LivingEntity target) {
        Vec3 aim = aimVector(target);
        WaterBulletJutsuEntity bullet = new WaterBulletJutsuEntity(this.boss, aim.x, aim.y, aim.z);
        bullet.setDamageMultiplier(1.6f);
        bullet.setYRot(this.boss.getYRot() - 180);
        this.boss.level().addFreshEntity(bullet);
        playCastSound(SoundEvents.DOLPHIN_ATTACK, 0.6f);
    }

    /** Hidan: his own blood pays for it, so it hurts you regardless of your armour. */
    private void castCurseRitual(LivingEntity target) {
        target.addEffect(new MobEffectInstance(MobEffects.WITHER, 10 * 20, 1, false, true));
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 10 * 20, 1, false, true));
        target.hurt(this.boss.damageSources().magic(), 8f);
        // He carves the circle out of himself; the trade is real damage for real protection.
        this.boss.hurt(this.boss.damageSources().magic(), 6f);
        this.boss.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 10 * 20, 1, false, true));
        playCastSound(SoundEvents.WITHER_SPAWN, 1.4f);

        if (this.boss.level() instanceof ServerLevel serverLevel) {
            NarutoParticles.spawnRing(serverLevel, this.boss.position().add(0, 0.1, 0), 2.0, 36,
                    CURSE_CRIMSON);
            NarutoParticles.spawnBolt(serverLevel, this.boss.getEyePosition(),
                    target.getEyePosition(), 4, 0.6, CURSE_CRIMSON);
        }
    }

    /** Hidan: the scythe is on a cable — distance is not the escape you thought it was. */
    private void castScytheReach(LivingEntity target) {
        target.hurt(this.boss.damageSources().mobAttack(this.boss), 12f);
        Vec3 pull = this.boss.position().subtract(target.position()).normalize().scale(1.2).add(0, 0.35, 0);
        target.setDeltaMovement(target.getDeltaMovement().add(pull));
        target.hurtMarked = true;
        playCastSound(SoundEvents.PLAYER_ATTACK_SWEEP, 0.6f);

        if (this.boss.level() instanceof ServerLevel serverLevel) {
            NarutoParticles.spawnBolt(serverLevel, this.boss.getEyePosition(),
                    target.position().add(0, 1.0, 0), 2, 0.25, NarutoParticles.METAL_GRAY);
        }
    }

    /** Deidara: C2, one large sculpture rather than a handful of small ones. */
    private void castC2Dragon(LivingEntity target) {
        detonate(target.position().add(0, 0.5, 0), 5.0, 20f, 2.0);
        playCastSound(SoundEvents.GENERIC_EXPLODE, 0.7f);

        if (this.boss.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                    target.getX(), target.getY() + 1.0, target.getZ(), 3, 1.2, 0.8, 1.2, 0.0);
            NarutoParticles.spawnBurst(serverLevel, target.position().add(0, 1.0, 0), 40, 2.0,
                    NarutoParticles.CLAY_GREY);
        }
    }

    /** Sasori: a cone of fire out of a puppet's mouth. */
    private void castFlamethrower(LivingEntity target) {
        Vec3 direction = target.position().subtract(this.boss.position()).normalize();
        for (LivingEntity caught : nearby(this.boss.position(), 9.0)) {
            Vec3 toward = caught.position().subtract(this.boss.position()).normalize();
            if (toward.dot(direction) < 0.6) {
                continue; // outside the cone
            }
            caught.hurt(this.boss.damageSources().mobAttack(this.boss), 10f);
            caught.setSecondsOnFire(6);
        }
        playCastSound(SoundEvents.FIRECHARGE_USE, 0.7f);

        if (this.boss.level() instanceof ServerLevel serverLevel) {
            for (int step = 1; step <= 9; step++) {
                Vec3 point = this.boss.getEyePosition().add(direction.scale(step));
                serverLevel.sendParticles(ParticleTypes.FLAME, point.x, point.y, point.z,
                        6, step * 0.06, step * 0.06, step * 0.06, 0.02);
            }
        }
    }

    /** Sasori: iron sand collapses inward on whatever is standing in it. */
    private void castIronSand(LivingEntity target) {
        target.hurt(this.boss.damageSources().mobAttack(this.boss), 13f);
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 6 * 20, 3, false, true));
        target.addEffect(new MobEffectInstance(MobEffects.JUMP, 6 * 20, 128, false, false));
        playCastSound(SoundEvents.SAND_BREAK, 0.5f);

        if (this.boss.level() instanceof ServerLevel serverLevel) {
            NarutoParticles.spawnSpiral(serverLevel, target.position(), 2.0, -0.08, 26, IRON_SAND);
        }
    }

    // --- Hashirama ---------------------------------------------------------------

    /** Roots tear up under the target, hold them, and keep grinding. */
    private void castWoodSurge(LivingEntity target) {
        for (LivingEntity caught : nearby(target.position(), 3.5)) {
            caught.hurt(this.boss.damageSources().mobAttack(this.boss), 12f);
            caught.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 5 * 20, 2, false, true));
        }
        playCastSound(SoundEvents.WOOD_BREAK, 0.6f);
        if (this.boss.level() instanceof ServerLevel serverLevel) {
            NarutoParticles.spawnRing(serverLevel, target.position(), 3.0, 30, NarutoParticles.LOG_BROWN);
            NarutoParticles.spawnSpiral(serverLevel, target.position(), 1.6, 0.18, 20, NarutoParticles.LOG_BROWN);
        }
    }

    /** Wood closes around them: pinned in place, and it does not let go quickly. */
    private void castWoodGrasp(LivingEntity target) {
        target.hurt(this.boss.damageSources().mobAttack(this.boss), 10f);
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 8 * 20, 5, false, true));
        target.addEffect(new MobEffectInstance(MobEffects.JUMP, 8 * 20, 128, false, false));
        target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 8 * 20, 2, false, true));
        target.setDeltaMovement(0, 0, 0);
        target.hurtMarked = true;
        playCastSound(SoundEvents.WOOD_PLACE, 0.5f);
        if (this.boss.level() instanceof ServerLevel serverLevel) {
            NarutoParticles.spawnSpiral(serverLevel, target.position(), 1.2, 0.22, 26, NarutoParticles.LOG_BROWN);
        }
    }

    // --- Nagato ------------------------------------------------------------------

    /** Shinra Tensei: everything near him stops being near him. */
    private void castShinraTensei() {
        for (LivingEntity caught : nearby(this.boss.position(), 9.0)) {
            caught.hurt(this.boss.damageSources().mobAttack(this.boss), 16f);
            Vec3 push = caught.position().subtract(this.boss.position()).normalize().scale(3.0).add(0, 0.8, 0);
            caught.setDeltaMovement(push);
            caught.hurtMarked = true;
        }
        playCastSound(SoundEvents.GENERIC_EXPLODE, 0.5f);
        if (this.boss.level() instanceof ServerLevel serverLevel) {
            for (int ring = 1; ring <= 4; ring++) {
                NarutoParticles.spawnRing(serverLevel, this.boss.position().add(0, 1.0, 0),
                        ring * 2.2, 34, ParticleTypes.CLOUD);
            }
        }
    }

    /** Chibaku Tensei: a core drops and everything is dragged into it. */
    private void castChibakuTensei(LivingEntity target) {
        // A real core now, rather than one frame of pull and a puff of particles: it rises,
        // hangs for eight seconds dragging everything toward it, and then comes down. The
        // entity owns all of that, so the player's version and Nagato's are the same thing.
        Vec3 origin = target.position().add(0, 3.0, 0);
        this.boss.level().addFreshEntity(
                new com.sekwah.narutomod.entity.jutsuprojectile.ChibakuTenseiEntity(this.boss, origin));
        playCastSound(SoundEvents.END_PORTAL_SPAWN, 1.3f);
        if (this.boss.level() instanceof ServerLevel serverLevel) {
            NarutoParticles.spawnSpiral(serverLevel, origin, 4.0, -0.25, 40, NarutoParticles.SHADOW_PURPLE);
        }
    }

    /** Preta Path: for a few seconds, hitting him feeds him. */
    private void castPretaAbsorb() {
        this.boss.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 6 * 20, 3, false, true));
        this.boss.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 6 * 20, 2, false, true));
        playCastSound(SoundEvents.ENCHANTMENT_TABLE_USE, 0.6f);
        if (this.boss.level() instanceof ServerLevel serverLevel) {
            NarutoParticles.spawnRing(serverLevel, this.boss.position().add(0, 1.0, 0), 1.8, 28,
                    NarutoParticles.SHADOW_PURPLE);
        }
    }

    // --- Naruto ------------------------------------------------------------------

    /** A thrown Rasenshuriken: wide, shredding, and it does not care about armour. */
    private void castRasenshuriken(LivingEntity target) {
        Vec3 impact = target.position().add(0, 1.0, 0);
        for (LivingEntity caught : nearby(impact, 5.0)) {
            caught.hurt(this.boss.damageSources().indirectMagic(this.boss, this.boss), 20f);
            caught.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 10 * 20, 2, false, true));
            caught.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 6 * 20, 1, false, true));
        }
        playCastSound(SoundEvents.PHANTOM_SWOOP, 0.7f);
        if (this.boss.level() instanceof ServerLevel serverLevel) {
            NarutoParticles.spawnBolt(serverLevel, this.boss.getEyePosition(), impact, 3, 0.4,
                    NarutoParticles.RASENGAN_BLUE);
            for (int ring = 1; ring <= 3; ring++) {
                NarutoParticles.spawnRing(serverLevel, impact, ring * 1.7, 32, NarutoParticles.ROTATION_WHITE);
            }
        }
    }

    /** A Rasengan driven into the chest, with the grinding knockback that goes with it. */
    private void castRasenganSlam(LivingEntity target) {
        target.hurt(this.boss.damageSources().mobAttack(this.boss), 18f);
        Vec3 push = target.position().subtract(this.boss.position()).normalize().scale(2.4).add(0, 0.4, 0);
        target.setDeltaMovement(push);
        target.hurtMarked = true;
        playCastSound(SoundEvents.PLAYER_ATTACK_CRIT, 0.8f);
        if (this.boss.level() instanceof ServerLevel serverLevel) {
            NarutoParticles.spawnSpiral(serverLevel, target.position().add(0, 1.0, 0), 1.1, 0.1, 24,
                    NarutoParticles.RASENGAN_BLUE);
        }
    }

    /** The fox's chakra: faster, stronger, and it stops the fight going the wrong way. */
    private void castKuramaCloak() {
        this.boss.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 20 * 20, 2, false, true));
        this.boss.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 20 * 20, 1, false, true));
        this.boss.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 20 * 20, 1, false, true));
        this.boss.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 20 * 20, 1, false, true));
        playCastSound(SoundEvents.ENDER_DRAGON_GROWL, 1.4f);
        if (this.boss.level() instanceof ServerLevel serverLevel) {
            NarutoParticles.spawnSpiral(serverLevel, this.boss.position(), 1.6, 0.2, 34,
                    NarutoParticles.KURAMA_ORANGE);
        }
    }

    // --- Hinata ------------------------------------------------------------------

    /** Hakke Kusho: a palm thrust that arrives as compressed air a dozen blocks away. */
    private void castVacuumPalm(LivingEntity target) {
        target.hurt(this.boss.damageSources().indirectMagic(this.boss, this.boss), 11f);
        Vec3 push = target.position().subtract(this.boss.position()).normalize().scale(1.8).add(0, 0.35, 0);
        target.setDeltaMovement(target.getDeltaMovement().add(push));
        target.hurtMarked = true;
        playCastSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.3f);
        if (this.boss.level() instanceof ServerLevel serverLevel) {
            NarutoParticles.spawnBolt(serverLevel, this.boss.getEyePosition(),
                    target.position().add(0, 1.0, 0), 2, 0.3, NarutoParticles.ROTATION_WHITE);
        }
    }

    /** Closes the gap the way a Hyuga does: instantly, and already inside your guard. */
    private void castGentleFistDash(LivingEntity target) {
        Vec3 toTarget = target.position().subtract(this.boss.position()).normalize();
        this.boss.setDeltaMovement(toTarget.scale(1.7).add(0, 0.3, 0));
        this.boss.hurtMarked = true;
        gentleFistHit(target, 8f);
    }

    /** Kaiten: he spins, and everything touching him leaves. */
    private void castRotation() {
        for (LivingEntity caught : nearby(this.boss.position(), 5.0)) {
            caught.hurt(this.boss.damageSources().mobAttack(this.boss), 10f);
            Vec3 push = caught.position().subtract(this.boss.position()).normalize().scale(2.2).add(0, 0.5, 0);
            caught.setDeltaMovement(push);
            caught.hurtMarked = true;
        }
        playCastSound(SoundEvents.ELYTRA_FLYING, 1.6f);
        if (this.boss.level() instanceof ServerLevel serverLevel) {
            for (int ring = 1; ring <= 3; ring++) {
                NarutoParticles.spawnRing(serverLevel, this.boss.position().add(0, ring * 0.6, 0),
                        4.0, 36, NarutoParticles.ROTATION_WHITE);
            }
        }
    }

    /**
     * One Gentle Fist strike. Magic damage on purpose: the whole point of the style is that
     * it goes through armour to the chakra network, so routing it through the normal
     * armour-reduced melee path would be exactly backwards.
     */
    private void gentleFistHit(LivingEntity target, float damage) {
        target.hurt(this.boss.damageSources().indirectMagic(this.boss, this.boss), damage);
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 6 * 20, 1, false, true));
        target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 6 * 20, 1, false, true));
        playCastSound(SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1.5f);
        if (this.boss.level() instanceof ServerLevel serverLevel) {
            NarutoParticles.spawnBurst(serverLevel, target.position().add(0, 1.1, 0), 12, 0.5,
                    NarutoParticles.ROTATION_WHITE);
        }
    }

    // --- Shikamaru ---------------------------------------------------------------

    /** Shadow Possession: you are still alive, you simply do not get to move. */
    private void castShadowBind(LivingEntity target) {
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 6 * 20, 6, false, true));
        target.addEffect(new MobEffectInstance(MobEffects.JUMP, 6 * 20, 128, false, false));
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 6 * 20, 2, false, true));
        target.setDeltaMovement(0, target.getDeltaMovement().y, 0);
        target.hurtMarked = true;
        playCastSound(SoundEvents.SCULK_BLOCK_CHARGE, 0.8f);
        if (this.boss.level() instanceof ServerLevel serverLevel) {
            NarutoParticles.spawnBolt(serverLevel, this.boss.position(), target.position(), 2, 0.15,
                    NarutoParticles.SHADOW_PURPLE);
            NarutoParticles.spawnRing(serverLevel, target.position(), 1.0, 20, NarutoParticles.SHADOW_PURPLE);
        }
    }

    /** Shadow Sewing: the shadow sharpens and comes up through the floor. */
    private void castShadowSewing(LivingEntity target) {
        target.hurt(this.boss.damageSources().indirectMagic(this.boss, this.boss), 13f);
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 4 * 20, 2, false, true));
        playCastSound(SoundEvents.SCULK_SHRIEKER_SHRIEK, 1.4f);
        if (this.boss.level() instanceof ServerLevel serverLevel) {
            NarutoParticles.spawnBurst(serverLevel, target.position(), 22, 0.9, NarutoParticles.SHADOW_PURPLE);
        }
    }

    /** Shadow Strangle: it reaches the throat and stays there. */
    private void castShadowStrangle(LivingEntity target) {
        target.addEffect(new MobEffectInstance(MobEffects.WITHER, 8 * 20, 2, false, true));
        target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 5 * 20, 0, false, false));
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 8 * 20, 3, false, true));
        target.hurt(this.boss.damageSources().indirectMagic(this.boss, this.boss), 8f);
        playCastSound(SoundEvents.WARDEN_HEARTBEAT, 0.7f);
        if (this.boss.level() instanceof ServerLevel serverLevel) {
            NarutoParticles.spawnSpiral(serverLevel, target.position(), 0.9, 0.16, 22,
                    NarutoParticles.SHADOW_PURPLE);
        }
    }

    // ------------------------------------------------------------------ sustained steps

    /** Deidara: a detonation every few ticks, walking in from short of the target. */
    private void stepClayBarrage(LivingEntity target) {
        if (this.sustainedTicks % 4 != 0) {
            return;
        }
        // Starts short and marches onto the target, so standing still is punished hardest.
        double progress = 0.55 + 0.45 * (this.sustainedStep / 7.0);
        Vec3 point = this.boss.position().add(
                target.position().subtract(this.boss.position()).scale(progress)).add(0, 0.5, 0);
        detonate(point, 3.0, 9f, 0.9);

        if (this.boss.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.EXPLOSION, point.x, point.y, point.z,
                    3, 0.4, 0.4, 0.4, 0.0);
            NarutoParticles.spawnBurst(serverLevel, point, 14, 0.9, NarutoParticles.CLAY_GREY);
        }
        playCastSound(SoundEvents.GENERIC_EXPLODE, 1.4f);

        if (++this.sustainedStep >= 8) {
            this.sustained = null;
        }
    }

    /** Shisui: five appearances, one cut each, no travel time in between. */
    private void stepBodyFlicker(LivingEntity target) {
        if (this.sustainedTicks % 5 != 0) {
            return;
        }
        double angle = this.boss.getRandom().nextDouble() * Math.PI * 2;
        Vec3 spot = target.position().add(Math.cos(angle) * 1.8, 0, Math.sin(angle) * 1.8);
        if (this.boss.level() instanceof ServerLevel serverLevel) {
            NarutoParticles.spawnBurst(serverLevel, this.boss.position().add(0, 1.0, 0), 12, 0.5,
                    NarutoParticles.ROTATION_WHITE);
        }
        this.boss.teleportTo(spot.x, target.getY(), spot.z);
        target.hurt(this.boss.damageSources().mobAttack(this.boss), 7f);
        playCastSound(SoundEvents.ENDERMAN_TELEPORT, 1.5f);

        if (++this.sustainedStep >= 5) {
            this.sustained = null;
        }
    }

    /** Madara: a line of fire dragged sideways across the target's ground. */
    private void stepFireWave(LivingEntity target) {
        if (this.sustainedTicks % 3 != 0) {
            return;
        }
        Vec3 direction = target.position().subtract(this.boss.position()).normalize();
        Vec3 side = new Vec3(-direction.z, 0, direction.x);
        double sweep = (this.sustainedStep - 3) * 1.6;
        for (int step = 2; step <= 10; step++) {
            Vec3 point = this.boss.position().add(direction.scale(step)).add(side.scale(sweep)).add(0, 0.4, 0);
            if (this.boss.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.FLAME, point.x, point.y, point.z,
                        5, 0.35, 0.35, 0.35, 0.02);
            }
            for (LivingEntity caught : nearby(point, 1.6)) {
                caught.hurt(this.boss.damageSources().mobAttack(this.boss), 5f);
                caught.setSecondsOnFire(5);
            }
        }
        playCastSound(SoundEvents.BLAZE_SHOOT, 0.5f);

        if (++this.sustainedStep >= 7) {
            this.sustained = null;
        }
    }

    /** Kisame: three sharks, one after another, so dodging the first is not enough. */
    private void stepSharkVolley(LivingEntity target) {
        if (this.sustainedTicks % 8 != 0) {
            return;
        }
        castWaterDragon(target);
        if (++this.sustainedStep >= 3) {
            this.sustained = null;
        }
    }

    /** Sasori: a spread of poisoned needles, tracked as instant hits along the line. */
    private void stepSenbonVolley(LivingEntity target) {
        if (this.sustainedTicks % 3 != 0) {
            return;
        }
        target.hurt(this.boss.damageSources().mobAttack(this.boss), 4f);
        target.addEffect(new MobEffectInstance(MobEffects.POISON, 6 * 20, 1, false, true));
        playCastSound(SoundEvents.ARROW_HIT, 1.7f);

        if (this.boss.level() instanceof ServerLevel serverLevel) {
            NarutoParticles.spawnBolt(serverLevel, this.boss.getEyePosition(),
                    target.position().add(0, 1.0, 0), 1, 0.15, POISON_GREEN);
        }
        if (++this.sustainedStep >= 6) {
            this.sustained = null;
        }
    }

    /**
     * Hinata: the Eight Trigrams sequence, and it doubles the way the count does - two
     * palms, then four, then eight. Each strike bypasses armour and shaves the target's
     * ability to fight back rather than simply their health.
     */
    private void stepSixtyFourPalms(LivingEntity target) {
        if (this.sustainedTicks % 3 != 0) {
            return;
        }
        if (this.boss.distanceTo(target) > 4.5) {
            this.sustained = null; // they broke away; the sequence cannot follow
            return;
        }
        // Later palms in the sequence land harder, matching the two-four-eight escalation.
        float damage = 4f + this.sustainedStep * 0.7f;
        gentleFistHit(target, damage);
        if (++this.sustainedStep >= 12) {
            this.sustained = null;
        }
    }

    /** Naruto: clones pile in one after another from wherever they happen to be. */
    private void stepCloneFlurry(LivingEntity target) {
        if (this.sustainedTicks % 4 != 0) {
            return;
        }
        double angle = this.boss.getRandom().nextDouble() * Math.PI * 2;
        Vec3 from = target.position().add(Math.cos(angle) * 2.2, 0, Math.sin(angle) * 2.2);
        target.hurt(this.boss.damageSources().mobAttack(this.boss), 6f);
        Vec3 push = target.position().subtract(from).normalize().scale(0.5).add(0, 0.25, 0);
        target.setDeltaMovement(target.getDeltaMovement().add(push));
        target.hurtMarked = true;
        playCastSound(SoundEvents.PLAYER_ATTACK_STRONG, 1.3f);

        if (this.boss.level() instanceof ServerLevel serverLevel) {
            NarutoParticles.spawnBurst(serverLevel, from.add(0, 1.0, 0), 14, 0.5,
                    NarutoParticles.ROTATION_WHITE);
        }
        if (++this.sustainedStep >= 8) {
            this.sustained = null;
        }
    }

    /** Hashirama: a wooden head drives forward through the ground toward the target. */
    private void stepWoodDragon(LivingEntity target) {
        if (this.sustainedTicks % 3 != 0) {
            return;
        }
        Vec3 direction = target.position().subtract(this.boss.position()).normalize();
        double reach = 2.0 + this.sustainedStep * 1.6;
        Vec3 head = this.boss.position().add(direction.scale(reach)).add(0, 0.5, 0);

        for (LivingEntity caught : nearby(head, 2.2)) {
            caught.hurt(this.boss.damageSources().mobAttack(this.boss), 9f);
            caught.setDeltaMovement(caught.getDeltaMovement().add(direction.scale(0.7)).add(0, 0.35, 0));
            caught.hurtMarked = true;
        }
        playCastSound(SoundEvents.WOOD_BREAK, 0.5f);

        if (this.boss.level() instanceof ServerLevel serverLevel) {
            NarutoParticles.spawnRing(serverLevel, head, 1.8, 24, NarutoParticles.LOG_BROWN);
            serverLevel.sendParticles(ParticleTypes.COMPOSTER, head.x, head.y, head.z, 14, 0.7, 0.5, 0.7, 0.03);
        }
        if (++this.sustainedStep >= 8) {
            this.sustained = null;
        }
    }

    // ------------------------------------------------------------------ helpers

    /** Spends chakra if the reserve covers it. */
    /**
     * Spends chakra, and says whether it could.
     *
     * Callers deliberately ignore the result: every rotation ends in an unconditional cast,
     * because a menu whose last option can fail is a menu that sometimes does nothing at all.
     * What makes that safe is {@link #MIN_CHAKRA} - the goal refuses to start unless the boss
     * can afford the most expensive unconditional fallback in any rotation, so by the time a
     * technique is chosen the payment cannot fail. If a cheaper floor is ever wanted, the
     * fallbacks have to start checking the result first.
     */
    private boolean pay(float cost) {
        return this.boss.useChakra(cost);
    }

    /**
     * Everything hittable in a radius: not the boss, and not anything the boss put there.
     *
     * Excluding only the boss itself meant every area technique swept up its own side -
     * Naruto trampling the clones he had just summoned, Sasori catching his own collection in
     * a poison cloud. A boss that has to fight around its own summons is not harder, it is
     * broken.
     */
    private java.util.List<LivingEntity> nearby(Vec3 center, double radius) {
        return this.boss.level().getEntitiesOfClass(LivingEntity.class,
                new net.minecraft.world.phys.AABB(
                        center.x - radius, center.y - radius, center.z - radius,
                        center.x + radius, center.y + radius, center.z + radius),
                entity -> entity != this.boss && entity.isAlive() && !isOwnSide(entity)
                        && entity.distanceToSqr(center) <= radius * radius);
    }

    /** A summon this boss called out: its shadow clones and its puppets. */
    private boolean isOwnSide(LivingEntity candidate) {
        java.util.UUID self = this.boss.getUUID();
        if (candidate instanceof com.sekwah.narutomod.entity.ShadowCloneEntity clone) {
            return clone.getOwnerUUID().map(self::equals).orElse(false);
        }
        if (candidate instanceof com.sekwah.narutomod.entity.PuppetEntity puppet) {
            return puppet.getOwnerUUID().map(self::equals).orElse(false);
        }
        return false;
    }

    /**
     * An explosion in effect but not in mechanics: damage and knockback, no terrain damage
     * and no self-damage. Bosses spawn wherever the world puts them, including next to a
     * base, so Deidara is not permitted to rearrange it.
     */
    private void detonate(Vec3 center, double radius, float damage, double knockback) {
        for (LivingEntity caught : nearby(center, radius)) {
            double falloff = 1.0 - Math.min(1.0, Math.sqrt(caught.distanceToSqr(center)) / radius);
            caught.hurt(this.boss.damageSources().explosion(this.boss, this.boss),
                    (float) (damage * (0.4 + 0.6 * falloff)));
            Vec3 push = caught.position().subtract(center).normalize().scale(knockback * falloff).add(0, 0.3, 0);
            caught.setDeltaMovement(caught.getDeltaMovement().add(push));
            caught.hurtMarked = true;
        }
    }

    /** Aim vector from the boss's eyes to the target's chest, normalised. */
    private Vec3 aimVector(LivingEntity target) {
        return target.position().add(0, target.getBbHeight() * 0.6, 0)
                .subtract(this.boss.getEyePosition()).normalize();
    }

    /**
     * Exposes the technique the boss just used to any Sharingan watching. This is what
     * makes the copy-wheel meaningful in single player: standing in front of Itachi with
     * the eye open is how you take Amaterasu off him, exactly as the eye is supposed to
     * work. The boss AI doesn't run through the Ability system, so each cast site names
     * the equivalent registered jutsu itself.
     */
    private void offer(RegistryObject<? extends com.sekwah.narutomod.abilities.Ability> ability) {
        if (ability == null) {
            return;
        }
        com.sekwah.narutomod.util.SharinganCopy.onJutsuPerformed(
                this.boss, ability.get(), ability.getId().getPath());
    }

    /** Kisame/Zabuza/Hidan: close in and land a brutal blade hit. */
    private void castBladeRush(LivingEntity target, float damage) {
        Vec3 toTarget = target.position().subtract(this.boss.position()).normalize();
        this.boss.setDeltaMovement(toTarget.scale(1.6).add(0, 0.35, 0));
        this.boss.hurtMarked = true;
        target.hurt(this.boss.damageSources().mobAttack(this.boss), damage);
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 3 * 20, 0, false, true));
        playCastSound(SoundEvents.PLAYER_ATTACK_SWEEP, 0.7f);

        if (this.boss.level() instanceof ServerLevel serverLevel) {
            NarutoParticles.spawnBurst(serverLevel, target.position().add(0, 1.0, 0), 20, 0.8,
                    NarutoParticles.METAL_GRAY);
        }
    }

    private void playCastSound(SoundEvent sound, float pitch) {
        this.boss.level().playSound(null, this.boss.blockPosition(), sound, SoundSource.HOSTILE, 1.4f, pitch);
    }
}
