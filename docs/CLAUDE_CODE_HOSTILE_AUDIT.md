# Hostile senior audit — Naruto Mod (Forge 1.20.1)

**Date:** 2026-08-16  
**Rule of this audit:** critique only. No game source was edited, and this document is not a patch.  
**Scope:** Java gameplay/network code, bosses and summons, client animation/rendering/models/VFX/sounds, assets, progression, build and release configuration.

## Executive verdict

The project **compiles**, but that is the least interesting bar it clears. The multiplayer authority model for channelled jutsu is exploitable, several headline boss mechanics either do not work or can damage a server world, and the client presentation is inconsistent between the caster and observers. This is not ready for a public survival server without a hardening pass.

`./gradlew.bat check --stacktrace --warning-mode all` completed successfully on Java 25 using the Java 17 toolchain. However Gradle reports `test NO-SOURCE`: there are **no automated tests** protecting this code. A green `check` currently proves compilation and resource processing, not gameplay correctness.

Severity is based on practical impact:

- **P0 — stop ship:** exploit, data/world loss, or a core mechanic that is fundamentally broken.
- **P1 — high:** major multiplayer, progression, gameplay or release defect.
- **P2 — medium:** clear visual/UX/performance defect or broken edge case.
- **P3 — debt:** a latent defect factory; not necessarily visible on the next play session.

## P0 — stop ship

### P0-01 — Channel STOP grants rank XP without a successful cast

**Evidence:** [`ServerAbilityChannelPacket.java`](../src/main/java/com/sekwah/narutomod/network/c2s/ServerAbilityChannelPacket.java#L109) calls `performServer`, `grantCastXp`, and Sharingan-copy notification on every valid `STOP`; it does not learn whether the jutsu actually succeeded. `ChakraChargeAbility.performServer` is empty, while `MysticalPalm` can also terminate without a hit.

**Reproduce:** a modified client sends `START`, then `STOP` before the next server player tick for Chakra Charge or Mystical Palm. Repeat.

**Impact:** rank XP is generated without chakra, an actual cast, target, or hit. At the 50,000-XP Kage threshold this turns progression into packet spam. An early cancelled Kirin can also be offered to a watching Sharingan as if it had been performed.

**Potential direction:** model a channel completion as `CASTED`, `CANCELLED`, or `FAILED`; grant XP/copy/cooldown/VFX only for `CASTED`. A packet saying STOP is intent, not evidence of a completed jutsu.

### P0-02 — Fireball can be released for zero base chakra

**Evidence:** [`ServerAbilityChannelPacket.java`](../src/main/java/com/sekwah/narutomod/network/c2s/ServerAbilityChannelPacket.java#L109) accepts immediate STOP; [`NinjaData.java`](../src/main/java/com/sekwah/narutomod/capabilities/NinjaData.java#L1749) is where channel tick cost is actually charged; [`FireballJutsuAbility.java`](../src/main/java/com/sekwah/narutomod/abilities/jutsus/FireballJutsuAbility.java#L74) charges the 30-chakra base cost only at tick zero.

**Reproduce:** send `START(fireball)` and `STOP(fireball)` within the same server tick.

**Impact:** `performServer(..., 0)` spawns a normal Fireball without its 30 chakra cost, while still awarding XP. The current two-second cooldown does not make the authority bug acceptable.

**Potential direction:** charge/record a server-side `startCost` when the channel session begins; reject releases before the session has ticked server-side. Split start, sustain, and release costs rather than using one overloaded `handleCost` call.

### P0-03 — Resource exhaustion fires committed channels without their cooldown

**Evidence:** [`NinjaData.java`](../src/main/java/com/sekwah/narutomod/capabilities/NinjaData.java#L1752) calls `performServer` when `handleCost` fails, but does not register a cooldown; cooldown registration exists only in the packet STOP path.

**Reproduce:** begin Fireball or Kirin, spend chakra until the next sustain tick cannot be paid, and do not send STOP.

**Impact:** Fireball is emitted without its two-second cooldown. A Kirin charged for 60 ticks can strike with no 45-second cooldown. This is a direct combat exploit, not an animation mismatch.

**Potential direction:** route explicit release and forced resource termination through one authoritative `finishChannel(reason)` path. If the ability crossed its commit threshold, cooldown must be applied for either exit path.

### P0-04 — C2S `NaN` corrupts transformation economy

**Evidence:** [`ServerScrollAdjustPacket.java`](../src/main/java/com/sekwah/narutomod/network/c2s/ServerScrollAdjustPacket.java#L26) accepts any float and forwards it at line 39. [`NinjaData.adjustTransformPower`](../src/main/java/com/sekwah/narutomod/capabilities/NinjaData.java#L578) uses `Math.min/Math.max`, which propagate `NaN`; the resulting value is used for Kurama bond/chakra drain and Susanoo stages.

**Reproduce:** while Susanoo or Kurama Cloak is active, send `ServerScrollAdjustPacket(Float.NaN)`.

**Impact:** `transformPower`, then chakra/bond arithmetic, can become `NaN`. Comparisons such as `chakra < cost` become false, drains no longer behave correctly, HUD/renderer sync receives invalid data, and the player's saved state may require manual repair.

**Potential direction:** accept only finite values and only the expected discrete input (`-1` / `+1`) at the packet boundary; make capability mutators reject non-finite values as a second barrier.

### P0-05 — Boss Susanoo first appears with zero durability

**Evidence:** [`MangekyoBossEntity.setSusanooStage`](../src/main/java/com/sekwah/narutomod/entity/MangekyoBossEntity.java#L275) writes the new stage at line 285 before calculating `previousMax` at line 288. The initial shell has durability `0`, while [`hurt`](../src/main/java/com/sekwah/narutomod/entity/MangekyoBossEntity.java#L899) absorbs only when durability is positive.

**Reproduce:** lower a transforming Uchiha boss to its first phase threshold.

**Impact:** the visual Susanoo is present, but its first hit goes through to boss HP. The named defensive phase is fake.

**Potential direction:** capture old stage/max before writing new stage; create a fresh shell at full durability for `0 → 1`, and transfer a fraction only between two existing shells.

### P0-06 — Broken boss shell re-enters its phase every server tick

**Evidence:** [`MangekyoBossEntity.tickTransformation`](../src/main/java/com/sekwah/narutomod/entity/MangekyoBossEntity.java#L420) sees the desired phase differs from actual stage, calls `setSusanooStage`, then unconditionally calls `onStageEntered`. During lockout the setter returns at lines 279–280, so actual stage remains zero and this repeats 20 times/second.

**Reproduce:** break a transforming boss's Susanoo while it still wants a nonzero phase.

**Impact:** Naruto/Sasori/Kankuro replenish stage summons repeatedly; Kakuzu can receive 18% max HP per tick; 30 particles per tick are emitted. This can make a fight unwinnable and creates needless server load for the full lockout.

**Potential direction:** make stage transition return whether it actually changed state; run stage-entry effects only on a successful transition.

### P0-07 — Chibaku Tensei permanently deletes world terrain without consent checks

**Evidence:** [`ChibakuTenseiEntity.gatherEarth`](../src/main/java/com/sekwah/narutomod/entity/jutsuprojectile/ChibakuTenseiEntity.java#L167) removes source blocks at line 207 and stores only shell positions. [`releaseEarth`](../src/main/java/com/sekwah/narutomod/entity/jutsuprojectile/ChibakuTenseiEntity.java#L258) deletes that shell; it never restores the source. There is no mob-griefing check, claim/protection hook, or block-break event path.

**Reproduce:** cast it near a build or let Nagato cast it; it can process up to 2,600 blocks per core (`MAX_GATHERED`) and permanently leaves a crater.

**Impact:** silent irreversible base/world damage. The source comments claim terrain survives, but the implementation deletes it.

**Potential direction:** choose an explicit policy: visual/temporary combat mass with no block mutation, or a transaction ledger with exact original states, `mobGriefing`, protection/event integration, a conservative allowlist, and guaranteed rollback.

### P0-08 — Chibaku's temporary terrain ledger is not persisted

**Evidence:** `gathered`, `fallSpeed`, and other in-flight state are runtime fields, while [`addAdditionalSaveData`](../src/main/java/com/sekwah/narutomod/entity/jutsuprojectile/ChibakuTenseiEntity.java#L444) saves only age, size, and owner UUID.

**Reproduce:** unload/reload the chunk or restart while the core is holding its gathered mass.

**Impact:** source blocks were already removed, but the reloaded entity does not know which shell blocks to clean up. The world is left with floating debris and a permanent crater.

**Potential direction:** serialize a bounded, validated rollback ledger and clean it up on all discard paths. A visual entity solution is safer than persistent world mutation.

### P0-09 — Bijudama's tail scaling is an instant-kill exponent

**Evidence:** [`TailedBeastBombEntity`](../src/main/java/com/sekwah/narutomod/entity/jutsuprojectile/TailedBeastBombEntity.java#L169) multiplies damage by `powerMultiplier`; [`TailedBeastVariant.powerMultiplier`](../src/main/java/com/sekwah/narutomod/entity/TailedBeastVariant.java#L147) is `2^(tails-1)`.

**Reproduce:** let Gyūki's bomb connect near the centre. At base power it calculates roughly `26 × 128 × 7.6/5 = 5,058` raw damage, rising to about 6,656 with rage. The projectile cannot be damaged.

**Impact:** this is not a hard but readable ultimate; it is guaranteed death for any normal Minecraft progression level. Canonical power hierarchy was translated directly into an unusable HP exponent.

**Potential direction:** tune against target time-to-kill and endgame armour, cap nonlinear scaling, and retain meaningful dodge/interruption/shield counterplay.

## P1 — high-impact defects

### Channelled jutsu and progression

| Finding | Evidence and consequence | Potential direction |
|---|---|---|
| One-shot Sharingan copies are never spent by channelled casts | [`ServerAbilityChannelPacket.java`](../src/main/java/com/sekwah/narutomod/network/c2s/ServerAbilityChannelPacket.java#L121) reports a cast but never clears `copiedJutsu`; the instant path does. A copied Fireball permanently bypasses its fire-affinity gate. | Centralise all successful-cast bookkeeping, including copy consumption. |
| Sage Mode applies a 60-second cooldown after a failed activation | [`SageModeAbility.performServer`](../src/main/java/com/sekwah/narutomod/abilities/jutsus/SageModeAbility.java#L142) can return for insufficient natural charge or chakra; channel STOP still unconditionally registers cooldown. | Return a cast result or make commitment depend on actual activation. |
| Sage petrification timing is mathematically wrong | [`SageModeAbility.java`](../src/main/java/com/sekwah/narutomod/abilities/jutsus/SageModeAbility.java#L114) computes `overchargeTicks` and never uses it; it tests total channel time, not time at 100 charge. | Store/use a distinct `ticksAtMaxSageCharge`. |
| A `requiresFreeHands=false` START can overwrite another active channel | [`ServerAbilityChannelPacket.java`](../src/main/java/com/sekwah/narutomod/network/c2s/ServerAbilityChannelPacket.java#L93) only gates via free hands; Chakra Charge/Sage/Substitution bypass it and [`NinjaData.setCurrentlyChanneledAbility`](../src/main/java/com/sekwah/narutomod/capabilities/NinjaData.java#L1623) replaces state. | Use an `IDLE → CHARGING → FINISHED` server session with explicit interruption rules. |
| Channel cooldown is invisible in the HUD | Channel code registers cooldown but does not send `ClientCooldownPacket`; instant code does. The client shows ready while server rejects the cast. | One shared cooldown service must update both authority and HUD. |
| Hand-seal packets are unmetered sound/game-event spam | [`ServerJutsuCastingPacket.java`](../src/main/java/com/sekwah/narutomod/network/c2s/ServerJutsuCastingPacket.java#L36) emits a sound and game event for every packet, costing no resource. | Server-side per-player input rate limit and server-owned combo state. |
| Four implemented clans are unavailable in ordinary play | GUI and C2S whitelist only `uzumaki, uchiha, hyuga, nara, haruno, senju` ([`ClanSelectionScreen.java`](../src/main/java/com/sekwah/narutomod/client/gui/ClanSelectionScreen.java#L17), [`ServerSelectClanPacket.java`](../src/main/java/com/sekwah/narutomod/network/c2s/ServerSelectClanPacket.java#L17)); Akimichi/Inuzuka/Yamanaka/Aburame jutsu are admin-only dead content. | One clan registry should drive GUI, validation, defaults, and requirements. |

### Bosses, AI, world integrity and summons

| Finding | Evidence and consequence | Potential direction |
|---|---|---|
| Boss combat state resets on reload | [`MangekyoBossEntity`](../src/main/java/com/sekwah/narutomod/entity/MangekyoBossEntity.java#L1028) serializes variant/stage/phase/chakra but not Susanoo durability/lockout or phase cooldowns. Reloading a chunk/server changes the fight. | Persist all combat-state fields or explicitly normalise them on load. |
| Every “Mangekyo boss”, including non-transforming characters, receives five HP phases | [`MangekyoBossVariant.transforms`](../src/main/java/com/sekwah/narutomod/entity/MangekyoBossVariant.java#L201) returns true for all variants. The nominal HP sequence totals 8.7 base health bars. Iruka/Hidan/Zabuza/Haku receive a Susanoo-style escalation. | Make phase kits data-driven per variant; do not reuse Susanoo as a generic respawn system. |
| Environmental/self damage can start a new boss phase | [`canAdvancePhase`](../src/main/java/com/sekwah/narutomod/entity/MangekyoBossEntity.java#L935) only excludes bypass damage. Lava, falls, and Hidan's self-inflicted ritual damage can replenish a health phase. | Define allowed attackers/sources and exclude self/environmental damage. |
| Nagato can stack active Chibaku cores | [`BossJutsuGoal.java`](../src/main/java/com/sekwah/narutomod/entity/goal/BossJutsuGoal.java#L1742) creates another core before the old one resolves. Each scans a 128-block radius and mutates terrain. | One active core per owner/region, server budgets, and a boss-safe no-grief variant. |
| NPC Amaterasu loses its owner | [`AmaterasuFireEntity.java`](../src/main/java/com/sekwah/narutomod/entity/jutsuprojectile/AmaterasuFireEntity.java#L44) resolves only `getPlayerByUUID`; boss code supplies a boss UUID. The fire can hurt its own boss, clones, and puppets. | Resolve any entity UUID and use one ally/owner predicate. |
| Boss sustain attacks continue through walls | [`BossJutsuGoal.canContinueToUse`](../src/main/java/com/sekwah/narutomod/entity/goal/BossJutsuGoal.java) does not sustain range/LOS; several attacks apply direct AABB damage instead of a projectile/raycast. Hiding after wind-up still gets the player hit. | Revalidate LOS/range per sustain tick or use collision-aware projectiles/hitboxes. |
| Boss AoE uses inconsistent friendly-fire rules | `nearby()` has ownership filtering, but older `victimsNear()` excludes only the boss. Poison/wind/elemental kit attacks can hit their own clones and puppets. | Centralise `isHostileTo(owner, target)` for every damage path. |
| Player shadow clones and wood golems ignore core bosses | [`ShadowCloneEntity.java`](../src/main/java/com/sekwah/narutomod/entity/ShadowCloneEntity.java#L76) and [`WoodGolemEntity.java`](../src/main/java/com/sekwah/narutomod/entity/WoodGolemEntity.java#L84) target `Monster`, while core custom bosses/bijuu are not `Monster`. | Target a common enemy/faction abstraction rather than that concrete vanilla class. |
| Summons, puppets, clones, pets and golems have no shared faction model | Owner-only/same-class filters in [`SummonBeastEntity.java`](../src/main/java/com/sekwah/narutomod/entity/SummonBeastEntity.java#L138) and [`PuppetEntity.java`](../src/main/java/com/sekwah/narutomod/entity/PuppetEntity.java#L133) allow a player's own army to fight itself. | Use owner UUID plus team/faction relationship across all minions. |
| Shadow Clone refunds create chakra | Basic clone costs 20 for 3, each surviving clone refunds 8 after 100 ticks; Multiple clone costs 80 for 20 and can refund 160. | Store paid cost per clone; refund only a bounded fraction for intended causes. |
| Explosive Clone can detonate in the wrong dimension | [`ExplosiveCloneAbility.java`](../src/main/java/com/sekwah/narutomod/abilities/jutsus/ExplosiveCloneAbility.java#L117) retains clone coordinates but uses the current `player.level()` after delayed execution. | Detonate in clone's validated level, or cancel when world/owner no longer matches. |
| Large summons can spawn inside blocks | [`KuchiyoseAbility.java`](../src/main/java/com/sekwah/narutomod/abilities/jutsus/KuchiyoseAbility.java#L84) and [`AnimalPathAbility.java`](../src/main/java/com/sekwah/narutomod/abilities/jutsus/AnimalPathAbility.java#L69) do not validate AABB/headroom/ground. | Search a safe volume based on actual dimensions; fail cleanly if none exists. |
| Katsuyu is described as support but always gets melee/leap AI | [`SummonBeastVariant.java`](../src/main/java/com/sekwah/narutomod/entity/SummonBeastVariant.java#L119) says support has no melee role; [`SummonBeastEntity.java`](../src/main/java/com/sekwah/narutomod/entity/SummonBeastEntity.java#L121) assigns it anyway. | Gate melee goals by role; give support follow/heal/retreat priorities. |
| Cross-dimension follow is only a comment | [`SummonFollowOwnerGoal.java`](../src/main/java/com/sekwah/narutomod/entity/goal/SummonFollowOwnerGoal.java#L27) promises it but searches only the current level and raw-teleports without safe placement. | Explicit cross-dimension resummon/teleport or despawn policy. |
| Temporary NPC Earth Spikes accumulate and can delete player dirt | [`RogueNinjaEntity.java`](../src/main/java/com/sekwah/narutomod/entity/RogueNinjaEntity.java#L163) overlaps volleys and cleanup deletes any dirt at remembered locations. | Use an identifiable temporary state and a persistent ledger; replace the old volley first. |

### Multiplayer rendering, assets and canon presentation

| Finding | Evidence and consequence | Potential direction |
|---|---|---|
| Other clients cannot see channel poses or held Rasengan | `currentlyChanneled`, `rasenganHeld`, and charge are owner-only `@Sync` fields ([`NinjaData.java`](../src/main/java/com/sekwah/narutomod/capabilities/NinjaData.java#L117), [`NinjaData.java`](../src/main/java/com/sekwah/narutomod/capabilities/NinjaData.java#L647)), while renderers iterate remote players. Caster sees a jutsu; observer sees idle hands. | Sync a compact public visual-action state: id, phase, quantized charge. |
| Other clients render wrong eyes | Mangekyo/Rinnegan/transplant fields are owner-only but [`PlayerEyeLayer`](../src/main/java/com/sekwah/narutomod/client/renderer/entity/PlayerEyeLayer.java#L87) uses them for every player. | Replicate a minimal `DojutsuVisualState`, not full progression data. |
| Biju cloak shears/clips; boss shroud leaks stale pose | [`BijuCloakModel.animateFrom`](../src/main/java/com/sekwah/narutomod/client/model/entity/BijuCloakModel.java#L898) copies rotations but not x/y/z positions, despite sprint/jutsu moving limbs. [`BossKuramaLayer`](../src/main/java/com/sekwah/narutomod/client/renderer/entity/BossKuramaLayer.java#L75) uses the model without a full reset/parent pose. | Copy full transforms/reset every render, or use a separate model instance for the boss. |
| Four declared sounds use obsolete paths | [`sounds.json`](../src/main/resources/assets/narutomod/sounds.json#L122) references legacy `mob/`, `random/`, and `liquid/` paths rather than 1.20.1 resource locations/custom OGGs. Fireball/Water Bullet/Rasengan therefore produce missing-sound warnings or silence; Rasengan also invokes Water Bullet splash. | Use valid current vanilla event keys or bundled OGGs; give Rasengan its own sound event. |
| Base Rasengan is incorrectly treated as Wind Release | [`PlayerEvents.java`](../src/main/java/com/sekwah/narutomod/events/PlayerEvents.java#L1053) makes Wind mastery scale Rasengan knockback. Canonically Rasengan is chakra shape transformation; wind belongs to Rasenshuriken/variants. | Keep base Rasengan neutral shape chakra; bind Wind gate/scaling to a distinct advanced version. |

## P2 — visible, UX and performance defects

### Animation and rendering

1. **Wall run still reads as a fast crawl at rest.** [`WaterWalkAbility.java`](../src/main/java/com/sekwah/narutomod/abilities/utility/WaterWalkAbility.java#L243) always adds a 0.035 wall-normal grip velocity. [`PlayerAnimHandler.java`](../src/main/java/com/sekwah/narutomod/anims/PlayerAnimHandler.java#L392) uses total `deltaMovement.length`, therefore it cycles legs while input is released; `cos(age * 1.45)` is about 4.6 cycles/second regardless of distance travelled. The model is no longer rotated 90 degrees, so this is not literal Spider-Man posture; it is a cadence/velocity bug that creates the same impression. **Direction:** subtract normal grip, use tangential travelled distance, dead-zone, and separate idle/climb/run states.

2. **Pose fade-out contradicts its own comment.** [`PoseBlender.java`](../src/main/java/com/sekwah/narutomod/anims/PoseBlender.java#L112) says elapsed time survives fade-out, but line 114 resets it to zero immediately. Releasing Fireball/Chidori jumps to frame one before dissolving. **Direction:** freeze elapsed at falling edge and clear only after blend weight reaches zero.

3. **Pose priority is source-order overwrite, not composition.** `PlayerAnimHandler` applies channel → chidori → cast → wall → Sage → Kurama → Susanoo → landing; [`PoseBlender.rotate`](../src/main/java/com/sekwah/narutomod/anims/PoseBlender.java#L135) lerps toward absolute targets. Kurama/Susanoo can overwrite an active Chidori/channel silhouette. **Direction:** one pose plan per body part, explicit priorities, and only approved additive layers.

4. **Rasengan/Chidori detach from the hand at steep view angles.** [`JutsuVfx.java`](../src/main/java/com/sekwah/narutomod/util/JutsuVfx.java#L223) flattens the look vector's Y component; near vertical view it falls back to world-Z. **Direction:** use a true hand anchor with full orientation for first- and third-person.

5. **Chidori is expensive and visibly steps at 20 Hz.** It spawns roughly 41 particles/tick/player (about 820/sec) with no LOD, and renderer geometry reseeds every game tick. Ten users can request ~8,200 particles/sec and the bolt visibly changes only at 20 Hz. **Direction:** distance/quality LOD, UUID in seed, and interpolated noise/geometry.

6. **Most major creature models slide instead of acting.** Bijuu only lean during Bijudama, many summons are static (snake is the exception), and puppets mostly animate Hiruko's tail. No synced cast state means damage often comes from a still model. **Direction:** minimum idle/move/attack/cast state machines driven by synced action timer.

7. **Bijudama claims a growing sphere at the mouth but renders particles only until projectile spawn.** This weakens the clearest telegraph for the most lethal attack. **Direction:** a synced charge orb anchored at the mouth and scaled by charge progress.

8. **First-person detection is a heuristic, not render context.** [`PlayerModelMixin.java`](../src/main/java/com/sekwah/narutomod/mixin/client/PlayerModelMixin.java#L21) treats zero animation arguments as first-person. A first third-person frame or another renderer can silently skip poses. **Direction:** use the actual hand-render path/context.

9. **Wall walking always plays stone footsteps.** [`WaterWalkAbility.java`](../src/main/java/com/sekwah/narutomod/abilities/utility/WaterWalkAbility.java#L312) uses `STONE_STEP` for glass, leaves, wood, sand, etc. **Direction:** derive sound from contact block or use one intentional chakra-step sound.

10. **Rasengan's old projectile path is dead and visually incompatible if revived.** The registered renderer/model scales to an apparent multi-block sphere while `RasenganEntity` hitbox tops out near two blocks; no normal source currently creates it. **Direction:** remove the obsolete path or use one scale source for renderer, VFX and hitbox.

### AI readability and edge cases

11. **Chōmei keeps flying during a supposedly planted Bijudama charge.** `TailedBeastJutsuGoal` stops navigation, but hover AI continues to write movement because the charge goal does not own MOVE/JUMP. **Direction:** suspend hover and zero horizontal motion during charge.

12. **Boss body-flicker/phase teleports do not validate free volume.** Raw `teleportTo` can place a boss in a wall/low ceiling, producing stuck/suffocation scenarios. **Direction:** safe AABB/headroom search and cancel if absent.

13. **Several rogue “projectile” jutsu are AABB/cone damage through walls.** Great Breakthrough, False Darkness and Earth Spikes select by geometry without raycast. **Direction:** collision-aware projectile/hitbox primitive or per-victim raycast.

14. **Hyuga rogue and Human Boulder apply hit checks before their dash happens.** They display VFX/cooldown but frequently deal no damage at the target, or damage only the start position. **Direction:** multi-tick dash/roll state with collision-time hitbox.

15. **Wood Golem cleanup searches only 96 blocks around owner.** Toggling off after travel/teleport leaves a free orphan. **Direction:** owner-held UUID plus entity self-cleanup across dimensions.

## P3 — release engineering and debt that will generate more defects

1. **Release build logs the Curse API token.** [`build.gradle`](../build.gradle#L314) emits `CURSE_API` directly. Any CI log reader gets publishing credentials. **Direction:** never print secrets; log only whether credentials exist.

2. **Declared compatibility is too broad and internally contradictory.** [`mods.toml`](../src/main/resources/META-INF/mods.toml#L3) says loader `[41,)`, Forge `[46,48)`, Minecraft `[1.20,)`, while compilation is against 1.20.1/Forge 47.1.62. It advertises versions against which this artifact was not built. **Direction:** exact supported Minecraft/Forge interval, with CI matrix tests before widening it.

3. **`mixingradle:0.7+`, librarian `1.+`, and Minotaur `2.+` are floating build dependencies.** The same commit can produce a different build or fail later; Gradle already warns `VersionNumber` is removed in Gradle 9. **Direction:** pin versions and schedule a controlled Gradle 9/Mixin upgrade.

4. **No tests and no client resource smoke test exist.** `check` reports `compileTestJava NO-SOURCE` and `test NO-SOURCE`; four invalid sound paths reached runtime. **Direction:** add unit/regression tests for packet lifecycle and capability invariants, plus a client resource-reload smoke test that fails on missing models/textures/sounds.

5. **Several critical state machines are God objects.** `NinjaData.java` is ~2,950 lines, `BossJutsuGoal.java` ~2,013, `PlayerEvents.java` ~1,372. This is why lifecycle, ownership, persistence and visual state keep diverging. **Direction:** split channel/session, transformation, progression, faction and visual-sync responsibilities into independently testable components.

## Recommended fix order (planning only; nothing was changed)

1. Freeze release: P0-04, P0-01/02/03, P0-05/06, P0-07/08.
2. Build a single authoritative channel-cast lifecycle with a result value; test it against packet spam, early release and resource exhaustion.
3. Make world-mutating jutsu opt-in/protected/rсollback-safe before any public server use.
4. Repair boss phase state/persistence, then cap Bijudama and add common faction/LOS checks.
5. Add compact public visual sync, then resolve wall gait, pose composition, cloak transforms and VFX anchors.
6. Clean release credentials/version ranges and introduce automated server/client smoke tests.

## What was deliberately not claimed

- The build does compile, so no compile error was invented just because parts of the code are ugly.
- The wall-walk model is not currently rotated flat onto the wall; calling it literal Spider-Man would be inaccurate. Its gait is still visibly wrong for the reasons documented above.
- Canon criticism is isolated to mechanics that materially misrepresent a named technique or character (for example Wind Rasengan and universal multi-phase bosses), not subjective preference about every balance number.
