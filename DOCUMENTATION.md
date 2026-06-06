# Naruto Mod — Complete Developer Documentation

> **Handoff document for Codex / next AI session.**
> This document describes the full project: architecture, what's done, what's next, and how everything works.

---

## 📦 Project Info

| Field | Value |
|-------|-------|
| Mod ID | `narutomod` |
| Package | `com.sekwah.narutomod` |
| Minecraft | 1.20.1 |
| Mod Loader | NeoForge (Forge 47.x) |
| Java | 17 |
| Build | `gradlew.bat build` — outputs to `build/libs/NarutoMod-1.20-1.8.1-SNAPSHOT-universal.jar` |
| Datagen | `gradlew.bat runData` — must run before build when damage types change |
| Dependency | SekCLib (bundled) — provides `@Sync` capability sync annotations |

---

## 🏗 Architecture Overview

### Core Loop

```
Player presses C/V/B combo keys
  → NarutoKeyHandler.java (client)
  → ServerJutsuCastingPacket (plays seal sounds A/B/C)
  → After jutsuCastDelay ticks:
      INSTANT/TOGGLE → ServerAbilityActivatePacket
      CHANNELED → ServerAbilityChannelPacket (START/STOP/MIN_ACTIVATE)
  → Server handler validates, calls ability.handleCost() + ability.performServer()
  → NinjaData capability updated → @Sync sends to client
```

### Key Systems

| System | File | Description |
|--------|------|-------------|
| Player data | `capabilities/NinjaData.java` | All player ninja state (chakra, stamina, XP, rank, clan, etc.) |
| Interface | `capabilities/INinjaData.java` | Contract for NinjaData |
| Tick handler | `capabilities/NinjaCapabilityHandler.java` | Attaches cap, ticks per player, clones on respawn |
| Ability base | `abilities/Ability.java` | Abstract base: INSTANT/TOGGLE/CHANNELED types |
| Ability registry | `abilities/NarutoAbilities.java` | Registers all abilities + combo map |
| Network | `network/PacketHandler.java` | All C2S/S2C packets |
| Config | `config/NarutoConfig.java` | `config/narutomod-server.toml` |
| HUD | `client/gui/ChakraAndStaminaGUI.java` | Chakra/stamina bars + rank/clan HUD |
| Entity registry | `entity/NarutoEntities.java` | All entity types |
| Renderers | `client/renderer/NarutoRenderEvents.java` | Registers all entity renderers |
| Damage types | `damagetypes/NarutoDamageTypes.java` + `src/generated/resources/data/narutomod/damage_type/` | fireball, water_bullet, rasengan |

---

## 🎮 Ability System Deep Dive

### How to create a new Jutsu

1. Create class in `abilities/jutsus/` extending `Ability`
2. Implement required interfaces:
   - `Ability.Cooldown` → add `getCooldown()` returning ticks
   - `Ability.Channeled` → for hold-to-charge abilities
   - `Ability.Toggled` → for toggle abilities
3. Override `activationType()`, `defaultCombo()`, `handleCost()`, `performServer()`
4. Register in `NarutoAbilities.java`
5. If new entity: register in `NarutoEntities.java` + add renderer in `NarutoRenderEvents.java`
6. If new damage type: add to `NarutoDamageTypes.java` + run `gradlew runData`
7. Add lang key to all 8 lang files in `assets/narutomod/lang/`

### Ability.handleCost(player, ninjaData, chargeAmount)
- Called every tick for CHANNELED, once for INSTANT/TOGGLE
- `chargeAmount` = ticks held (CHANNELED) or 0 (INSTANT)
- Return `false` to cancel; show fail message before returning
- Call `ninjaData.useChakra(amount, cooldownTicks)` to spend chakra

### Ability.performServer(player, ninjaData, ticksActive)
- `ticksActive` = ticks held for CHANNELED, 0 for INSTANT
- Use `ninjaData.scheduleDelayedTickEvent(consumer, delayTicks)` for delayed spawning
- Spawn entities via `player.level().addFreshEntity(entity)`

### Ability.Channeled
- `canActivateBelowMinCharge()` → return `false` to require holding
- `handleChannelling(player, ninjaData, ticks)` → called every tick while charging (server-side, spawn particles here)

---

## ✅ PHASE 1 — COMPLETE: Original mod foundation

Original mod by sekwah41. Already existed when work began.

**Contents:**
- Chakra & Stamina system (100 default, configurable)
- Substitution mechanism (logbook)
- Combo key system (C=1, V=2, B=3)
- Basic weapons: Kunai, Senbon, Shuriken, Explosive Kunai, Katana
- Armor: ANBU masks (5 colors), Flak Jackets, Anbu Armor, Akatsuki Cloak
- Headbands: 21 variants (all ninja villages + specials)
- Blocks: Paper Bomb, Bonsai Tree
- Music Disc: Lonely March
- Sounds: seal sounds, jutsu cast/fail, kunai hit, etc.

---

## ✅ PHASE 2 — COMPLETE: Polish + New Jutsus

**Bug fixes:**
- `FireballJutsuEntity.java`: Fixed `lifeSpan` double-decrement in rain → now decrements correctly (extra -1 in rain is intentional for faster decay)
- `FireballJutsuEntity.java`: Removed unused `explosionPower` field
- `EarthWallEntity.java`: Added missing renderer (`NoopRenderer`) — previously caused crash when casting

**Code cleanup:**
- Removed wrong javadoc ("slight speed boost") from FireballJutsuAbility, WaterBulletJutsuAbility, WaterWalkAbility
- Removed commented-out old damage source in WaterBulletJutsuEntity
- Removed debug `System.out.println` from ChakraDashAbility

**New Jutsus added:**

| Jutsu | Combo | Type | Cost | Cooldown | Notes |
|-------|-------|------|------|----------|-------|
| Chakra Dash (toggle speed) | V (2) | TOGGLE | 0.25/tick moving | — | Speed II, cloud particles |
| Earth Wall | B-C-C (311) | INSTANT | 40 chakra | 5s | 3x3 dirt wall, rises bottom→top, 15s then removed |
| Shadow Clone | C-V-V (122) | INSTANT | 50 chakra | 20s | 3 clones, attack monsters, 30s lifespan |
| Multiple Shadow Clone | C-V-V-V (1222) | INSTANT | 200 chakra | 30s | 20 clones, ±3 block spread |
| Rasengan | V-C-V (212) | CHANNELED | 1.5/tick | 8s | Hold to charge, spiral particles, 7❤ PvP (no kill), knockback 4-6x |

**Rasengan details:**
- Must hold ≥ threshold (15 ticks) to trigger START → charges begin
- `canActivateBelowMinCharge() = false` → quick tap shows "need to channel"
- `chargeAmount` stored on `RasenganEntity` — scales knockback 4.0→6.0 over 20-60 ticks
- Player damage capped: `min(14f, playerHp - 1f)` → cannot kill players

**Datagen fix:**
- Ran `gradlew runData` → generated `src/generated/resources/data/narutomod/damage_type/` for fireball, water_bullet, rasengan
- **IMPORTANT:** Always run `gradlew runData` when adding new damage types, then `gradlew build`

**Shadow Clone renderer:**
- `ShadowCloneRenderer` uses `ModelLayers.ZOMBIE` + zombie texture (Steve was wrong layer type → crash)
- `NoopRenderer` used for `EarthWallEntity` (invisible tracker entity)

---

## ✅ PHASE 3 — COMPLETE: Progression System

### What was added

**1. Chakra XP (`NinjaData.java`):**
- Field: `@Sync private float chakraXp`
- Accumulates automatically in `useChakra()` — every chakra spent = that much XP
- Saved to NBT: `"chakraXp"`

**2. Ninja Ranks (`NinjaData.java`):**
- Field: `@Sync private int ninjaRank` (0=Academy, 1=Genin, 2=Chunin, 3=Jonin, 4=Kage)
- Auto-advances in `addChakraXp()` when threshold reached
- Thresholds: `{0, 1000, 5000, 15000, 50000}`
- Bonuses applied in `getConfigData()`:
  - maxChakra += `{0, 20, 40, 60, 80}[rank]`
  - maxStamina += `{0, 10, 20, 30, 40}[rank]`
- Saved to NBT: `"ninjaRank"`

**3. Clan System (`NinjaData.java`):**
- Field: `@Sync private String clanId` (empty = not chosen)
- Set via `setClanId(String)` — validated server-side in `ServerSelectClanPacket`
- Cannot be changed once set (packet handler checks `clanId.isEmpty()`)
- Saved to NBT: `"clanId"`

**Clan Bonuses:**

| Clan ID | Passive Effect | Implementation |
|---------|----------------|----------------|
| `uzumaki` | maxChakra ×1.5, chakraRegen ×2 | `getClanChakraMultiplier()`, `getClanChakraRegenMultiplier()` |
| `uchiha` | +30% fire damage (future Sharingan) | Placeholder — ready for Phase 5 |
| `hyuga` | Strength I (melee +30%) | MobEffect every 40 ticks |
| `nara` | Speed I (+20% run speed) | MobEffect every 40 ticks |
| `haruno` | HP regen ~0.5/sec | `player.heal(0.025f)` every tick |

**4. Clan Selection GUI (`client/gui/ClanSelectionScreen.java`):**
- Opens automatically when ninja mode enabled but no clan chosen
- Cannot be closed with ESC (`shouldCloseOnEsc()` = false)
- 5 buttons with description text
- Sends `ServerSelectClanPacket` on click → closes screen

**5. Network Packet (`network/c2s/ServerSelectClanPacket.java`):**
- C2S: sends `clanId` string
- Server validates against Set of allowed clans
- Only applies if `clanId.isEmpty()`
- Registered in `PacketHandler.init()`

**6. HUD Display (`client/gui/ChakraAndStaminaGUI.java`):**
- Above chakra/stamina bars: shows `[RankIcon] RankName [XP] [ClanIcon]`
- Rank icons: 10×10 pixels from `textures/gui/ranks/`
- Clan icons: 12×12 pixels from `textures/gui/clans/`
- Rank colors: grey=Academy, green=Genin, blue=Chunin, gold=Jonin, red=Kage
- `tick()` reads `chakraXp`, `ninjaRank`, `clanId` from capability

**7. New interface methods in `INinjaData`:**
```java
float getChakraXp();
void addChakraXp(float amount);
int getNinjaRank();
String getClanId();
void setClanId(String clanId);
```

**8. Texture assets (already placed by user):**
```
assets/narutomod/textures/gui/clans/uzumaki.png   (32x32)
assets/narutomod/textures/gui/clans/uchiha.png    (32x32)
assets/narutomod/textures/gui/clans/hyuga.png     (32x32)
assets/narutomod/textures/gui/clans/nara.png      (32x32)
assets/narutomod/textures/gui/clans/haruno.png    (32x32)
assets/narutomod/textures/gui/ranks/genin.png     (16x16)
assets/narutomod/textures/gui/ranks/chunin.png    (16x16)
assets/narutomod/textures/gui/ranks/jonin.png     (16x16)
assets/narutomod/textures/gui/ranks/kage.png      (16x16)
```

---

## 📋 COMPLETE COMBO MAP (Current)

| Combo | Keys | Jutsu | Type | Cost | Cooldown | Rank Required |
|-------|------|-------|------|------|----------|---------------|
| 1 | C | Chakra Charge | CHANNELED | Free | — | Any |
| 2 | V | Chakra Dash | TOGGLE | 0.25/tick | — | Any |
| 3 | B | Water Walk | TOGGLE | 0.12/tick | — | Any |
| 12 | C-V | Substitution | CHANNELED | 1 charge | — | Any |
| 121 | C-V-C | Fireball Jutsu | INSTANT | 30 chakra | 3s | Any |
| 122 | C-V-V | Shadow Clone | INSTANT | 50 chakra | 20s | Any |
| 132 | C-B-V | Water Bullet | INSTANT | 30 chakra | 4s | Any |
| 212 | V-C-V | Rasengan | CHANNELED | 1.5/tick | 8s | Any |
| 311 | B-C-C | Earth Wall | INSTANT | 40 chakra | 5s | Any |
| 1222 | C-V-V-V | Multiple Shadow Clone | INSTANT | 200 chakra | 30s | Any |

---

## 🗂 Full File Map

### Abilities
```
abilities/
  Ability.java                          ← Base abstract class
  NarutoAbilities.java                  ← Registry + combo map
  jutsus/
    FireballJutsuAbility.java           ← combo 121, INSTANT, 30 chakra, 3s CD
    WaterBulletJutsuAbility.java        ← combo 132, INSTANT, 30 chakra, 4s CD
    SubstitutionJutsuAbility.java       ← combo 12, CHANNELED, 1 charge
    EarthWallJutsuAbility.java          ← combo 311, INSTANT, 40 chakra, 5s CD
    ShadowCloneJutsuAbility.java        ← combo 122, INSTANT, 50 chakra, 20s CD
    MultipleShadowCloneJutsuAbility.java← combo 1222, INSTANT, 200 chakra, 30s CD
    RasenganJutsuAbility.java           ← combo 212, CHANNELED, 1.5/tick, 8s CD
  utility/
    ChakraChargeAbility.java            ← combo 1, CHANNELED, free
    ChakraDashAbility.java              ← combo 2, TOGGLE, 0.25/tick
    WaterWalkAbility.java               ← combo 3, TOGGLE, 0.12/tick
    DoubleJumpAbility.java              ← no combo, INSTANT
    LeapAbility.java                    ← X key, INSTANT
```

### Entities
```
entity/
  NarutoEntities.java                   ← All EntityType registrations
  ShadowCloneEntity.java                ← PathfinderMob, 600 tick lifespan
  SubstitutionLogEntity.java            ← Mob, 60 tick lifespan, invulnerable
  jutsuprojectile/
    FireballJutsuEntity.java            ← AbstractHurtingProjectile, grows 0.1→1.0
    WaterBulletJutsuEntity.java         ← AbstractNonGlowingHurtingProjectile
    RasenganEntity.java                 ← AbstractNonGlowingHurtingProjectile, chargeAmount field
    EarthWallEntity.java                ← Entity tracker, places/removes dirt blocks
  projectile/
    KunaiEntity.java
    SenbonEntity.java
    ShurikenEntity.java
    ExplosiveKunaiEntity.java
  item/
    PaperBombEntity.java
```

### Capabilities (Player Data)
```
capabilities/
  INinjaData.java                       ← Interface
  NinjaData.java                        ← Implementation (all player state)
  NinjaCapabilityHandler.java           ← Attaches, ticks, clones
  CooldownTickEvent.java                ← Cooldown tracking
  DelayedPlayerTickEvent.java           ← Delayed action scheduling
  DoubleJumpData.java                   ← Double jump state
  toggleabilitydata/
    ToggleAbilityData.java              ← Active toggle abilities set
```

### Networking
```
network/
  PacketHandler.java                    ← Channel registration
  c2s/
    ServerJutsuCastingPacket.java       ← Key press (plays seal sounds)
    ServerAbilityActivatePacket.java    ← INSTANT/TOGGLE ability trigger
    ServerAbilityChannelPacket.java     ← CHANNELED START/STOP/MIN_ACTIVATE
    ServerToggleNinjaPacket.java        ← Enable/disable ninja mode
    ServerSelectClanPacket.java         ← Clan selection (Phase 3)
  s2c/
    ClientTestPacket.java               ← Unused test packet
```

### Client GUI
```
client/gui/
  NarutoInGameGUI.java                  ← Master HUD manager, tick/render events
  ChakraAndStaminaGUI.java              ← Chakra/stamina bars + rank/clan HUD
  ClanSelectionScreen.java              ← Clan picker (shows when ninja, no clan)
  JutsuScreen.java                      ← J key menu (become/stop ninja)
  SubstitutionGUI.java                  ← Substitution charge display
  WorldMarkerGUI.java                   ← World-space marker rendering
  BarDesigns.java                       ← 8 chakra bar visual designs
  PlayerGUI.java                        ← Interface: render() + tick()
```

### Renderers
```
client/renderer/
  NarutoRenderEvents.java               ← Registers all entity renderers
  entity/
    NoopRenderer.java                   ← Empty renderer (EarthWallEntity)
    ShadowCloneRenderer.java            ← HumanoidMobRenderer with zombie texture
    SubstitutionLogRenderer.java
    KunaiRenderer.java, SenbonRenderer.java, ShurikenRenderer.java
    ExplosiveKunaiRenderer.java, PaperBombRenderer.java
    jutsuprojectile/
      FireballJutsuRenderer.java        ← Sphere, grows, spins, glows
      WaterBulletJutsuRenderer.java     ← Small sphere, follows direction
      RasenganRenderer.java             ← Sphere (reuses FireballJutsuModel), blue tint
```

### Damage Types
```
damagetypes/
  NarutoDamageTypes.java                ← ResourceKey definitions + bootstrap
src/generated/resources/data/narutomod/damage_type/
  fireball.json
  water_bullet.json
  rasengan.json                         ← Added in Phase 2
```

---

## ⚙️ NinjaData Fields Reference

| Field | Type | @Sync | Saved | Description |
|-------|------|-------|-------|-------------|
| `chakra` | float | minTicks=1 | ✅ | Current chakra |
| `stamina` | float | minTicks=1 | ✅ | Current stamina |
| `substitutions` | float | minTicks=1 | ✅ | Substitution charges |
| `maxChakra` | float | ✅ | ❌ | Computed from config+rank+clan |
| `maxStamina` | float | ✅ | ❌ | Computed from config+rank |
| `maxSubstitutions` | float | ❌ | ❌ | From config |
| `ninjaModeEnabled` | boolean | syncGlobally | ✅ | Ninja mode on/off |
| `isInvisible` | boolean | syncGlobally, minTicks=1 | ❌ | Invisibility state |
| `currentlyChanneled` | ResourceLocation | minTicks=1 | ❌ | Active channel ability |
| `ticksChanneled` | int | minTicks=1 | ❌ | Ticks since START |
| `doubleJumpData` | DoubleJumpData | minTicks=1 | ❌ | Double jump ready flag |
| `toggleAbilityData` | ToggleAbilityData | ✅ | ❌ | Active toggles set |
| `substitutionLocation` | Vec3 | ✅ | ❌ | Substitution marker |
| `substitutionDimension` | ResourceLocation | ✅ | ❌ | Marker dimension |
| `chakraXp` | float | ✅ | ✅ | Phase 3: total XP earned |
| `ninjaRank` | int | ✅ | ✅ | Phase 3: 0-4 rank |
| `clanId` | String | ✅ | ✅ | Phase 3: clan name or empty |

---

## 🔧 Config File

Location: `.minecraft/config/narutomod-server.toml`

| Key | Default | Description |
|-----|---------|-------------|
| `maxChakra` | 100 | Base max chakra (rank bonuses added on top) |
| `chakraRegen` | 0.05/tick | Base regen (clan multiplier applied) |
| `maxStamina` | 100 | Base max stamina |
| `staminaRegen` | 0.4/tick | Stamina regen per tick |
| `maxSubstitutions` | 3 | Max substitution charges |
| `substitutionRegenTime` | 60s | Seconds per substitution charge |
| `jutsuKeyHoldThreshold` | 15 | Ticks to hold before CHANNELED START |
| `jutsuActivateDelay` | 15 | Ticks after key release before INSTANT cast |
| `kunaiExplosionRadius` | 3.0 | Explosive kunai blast radius |
| `paperbombExplosionRadius` | 4.0 | Paper bomb blast radius |
| `kunaiExplosionBreakBlocks` | true | Whether kunai breaks blocks |
| `paperBombExplosionBreakBlocks` | true | Whether paper bomb breaks blocks |
| `chakraBarDesign` | 0 | HUD bar design (0-7) |

---

## 🔴 PHASE 4 — NOT STARTED: New Jutsus

All new jutsus planned. No rank-gate code yet. Combos reserved:

| Combo | Jutsu | Style | Effect |
|-------|-------|-------|--------|
| 321 | Phoenix Flower | Fire | 5 small fireballs in fan spread |
| 3121 | Fire Dragon | Fire | 7 fireballs in line |
| 1321 | Water Prison | Water | Slowness IV on targets in radius |
| 1312 | Water Dragon | Water | Large water projectile + big knockback |
| 313 | Earth Spikes | Earth | 5 dirt pillars in line, knockback up |
| 3311 | Earth Dome | Earth | Dome of dirt around player |
| 232 | Lightning Bolt | Lightning | Summons lightning at target point |
| 2121 | Chidori | Lightning | Dash 3 blocks, 20 dmg to first hit |
| 231 | Vacuum Blade | Wind | Ignores 50% armor |
| 2312 | Wind Scythe | Wind | AoE wave, knockback all |

**Rasengan revision in this phase:**
- Already done! (CHANNELED, 7❤ PvP no-kill, knockback scales 4→6)

**Texture needed before Chidori:**
- `assets/narutomod/textures/particles/chidori_spark.png` (16x16, blue lightning)

---

## 🔴 PHASE 5 — NOT STARTED: Sharingan (Uchiha Clan)

**Requires textures first:**
- `textures/effects/sharingan/sharingan_1.png` (64x64) — 1 tomoe
- `textures/effects/sharingan/sharingan_2.png` (64x64) — 2 tomoe
- `textures/effects/sharingan/sharingan_3.png` (64x64) — 3 tomoe
- `textures/effects/sharingan/mangekyou.png` (64x64) — Mangekyou
- `textures/effects/sharingan/amaterasu.png` (16x16) — black flame particle

**Planned mechanics:**
- Uchiha-only (check `clanId.equals("uchiha")` in handleCost)
- Levels unlock with rank: Lv1=Genin, Lv2=Chunin, Lv3=Jonin, Mangekyou=Kage
- Active while toggle on: 1 chakra/tick drain
- Lv1: Night Vision effect
- Lv2: Speed/attack boost
- Lv3: Copy last jutsu used by nearby enemy
- Mangekyou: Amaterasu (black fire entity, burns forever, not extinguished by water)
- Screen vignette overlay while active
- `AmaterasuFireEntity` needed: custom fire entity

---

## 🔴 PHASE 6 — NOT STARTED: Sage Mode

**Requires textures:**
- `textures/effects/sage_eyes.png` (256x64) — toad eye overlay
- `textures/particles/sage_aura.png` (16x16) — orange nature energy

**Planned mechanics:**
- Jonin rank required
- Stand still 5 seconds to accumulate "nature energy"
- When full: Sage Mode activates for 60 seconds
- Effects: maxChakra ×1.5, all jutsu +50% damage, 60s timer
- Special: Sage Art: Massive Rasengan (giant radius 3 Rasengan)
- New fields needed in NinjaData: `sageEnergy`, `sageModeActive`

---

## 🔴 PHASE 7 — NOT STARTED: Eight Gates + Bijuu Mode

**Requires textures:**
- `particles/gate_steam.png` (16x16) — green steam
- `effects/bijuu_cloak.png` (16x16) — orange chakra cloak particle
- `effects/bijuu_eyes.png` (128x32) — eye overlay for Bijuu mode

**Eight Gates:** Jonin/Kage only. 8 levels of power boost. Gate 8 = near-death.

**Bijuu Mode:** Uzumaki only. Triggers at HP < 20%. Orange chakra cloak. +100% stats.

---

## 🔴 PHASE 8 — NOT STARTED: World Content

- Ninja village structures (Leaf, Sand, Mist)
- Jutsu scroll items that teach abilities
- Boss mobs: Orochimaru, Kakuzu, Pain
- Requires many textures + structure NBT files

---

## 🔴 PHASE 9 — NOT STARTED: Multiplayer & Polish

- Full jutsu menu (show cooldowns, learned jutsus, unlock requirements)
- Ninja profile screen (clan, rank, stats)
- Team system (3-man squads)
- Clan wars

---

## 🎨 Textures Already Generated

| File | Location | Used For |
|------|----------|---------|
| `clans/uzumaki.png` | `textures/gui/clans/` | HUD clan icon |
| `clans/uchiha.png` | `textures/gui/clans/` | HUD clan icon |
| `clans/hyuga.png` | `textures/gui/clans/` | HUD clan icon |
| `clans/nara.png` | `textures/gui/clans/` | HUD clan icon |
| `clans/haruno.png` | `textures/gui/clans/` | HUD clan icon |
| `ranks/genin.png` | `textures/gui/ranks/` | HUD rank icon |
| `ranks/chunin.png` | `textures/gui/ranks/` | HUD rank icon |
| `ranks/jonin.png` | `textures/gui/ranks/` | HUD rank icon |
| `ranks/kage.png` | `textures/gui/ranks/` | HUD rank icon |

---

## 🎨 Textures Still Needed (by Phase)

| Phase | File | Size | Description |
|-------|------|------|-------------|
| 4 | `particles/chidori_spark.png` | 16x16 | Blue lightning spark particle |
| 5 | `effects/sharingan_1.png` | 64x64 | Red eye, 1 tomoe |
| 5 | `effects/sharingan_2.png` | 64x64 | Red eye, 2 tomoe |
| 5 | `effects/sharingan_3.png` | 64x64 | Red eye, 3 tomoe |
| 5 | `effects/mangekyou.png` | 64x64 | Mangekyou Sharingan pattern |
| 5 | `effects/amaterasu.png` | 16x16 | Black flame particle |
| 6 | `effects/sage_eyes.png` | 256x64 | Screen overlay: toad eyes (orange, horizontal pupil) |
| 6 | `particles/sage_aura.png` | 16x16 | Orange nature energy particle |
| 7 | `particles/gate_steam.png` | 16x16 | Green steam particle |
| 7 | `effects/bijuu_cloak.png` | 16x16 | Orange chakra cloak particle |
| 7 | `effects/bijuu_eyes.png` | 128x32 | Screen overlay: Bijuu red eyes |
| 8 | `entity/boss/orochimaru.png` | 64x64 | Orochimaru mob texture |
| 8 | `entity/boss/kakuzu.png` | 64x64 | Kakuzu mob texture |

---

## 🔊 Sounds Needed (by Phase)

| Phase | Sound | Description |
|-------|-------|-------------|
| 5 | `sounds/sharingan_activate.ogg` | Whoosh sound |
| 4 | `sounds/chidori.ogg` | Bird chirping + electricity crackle |
| 6 | `sounds/sage_mode.ogg` | Nature ambience whoosh |

> All sounds go in `assets/narutomod/sounds/` and must be registered in `sounds.json` and `NarutoSounds.java`

---

## ⚠️ Known Issues / TODOs in Code

| File | Issue |
|------|-------|
| `WaterWalkAbility.java` | Has TODO: rewrite old water detection logic, handle waterlogged blocks |
| `NinjaData.java` | TODO: make currentlyChanneled global for visual effects |
| `NarutoKeyHandler.java` | TODO: configure held key threshold, fix key held states |
| `NarutoInGameGUI.java` | TODO: switch to new RegisterGuiOverlaysEvent (currently uses deprecated event) |
| `ShadowCloneEntity.java` | Uses zombie texture — ideally should use owner's player skin |
| `ClanSelectionScreen.java` | Icons defined but not yet rendered (buttons are text-only). To add icons: render clan PNG next to each button in the `render()` method |

---

## 🛠 How to Build

```bash
# Normal build
./gradlew build

# When adding new damage types (run first, then build)
./gradlew runData
./gradlew build

# Use JDK 17 (not default JDK 25)
$env:JAVA_HOME = "C:\Program Files\Java\jdk-17"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
```

Output: `build/libs/NarutoMod-1.20-1.8.1-SNAPSHOT-universal.jar`
Place in: `.minecraft/mods/`
Also requires: `SekCLib-1.20-1.0.2-universal.jar` in mods folder

---

## 📝 Lang Keys Pattern

All abilities follow: `narutomod:<ability_id>` → display name
All messages follow: `jutsu.fail.*`, `jutsu.cast`, `jutsu.toggle.*` etc.

Supported languages: `en_us`, `de_de`, `fr_fr`, `ru_ru`, `es_es`, `esan`, `tr_tr`, `hi_in`

When adding a new ability, add to ALL 8 lang files.
