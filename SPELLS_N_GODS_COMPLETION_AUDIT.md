# Spells 'n Gods — Completion Audit & Remediation

Mod: **Spells 'n Gods** `0.3.0` · Forge **1.20.1** (47.4.10) · Java 17
Audit date: 2026-06-17 · Branch: `claude/audit-remediation-phase1-2`

This document is the release-readiness audit for the mod. It records the findings of a
full-codebase review, the remediation plan, and (at the bottom) the work that was actually
implemented and validated.

Baseline at start of audit: `./gradlew build` **PASSED**, 15/15 unit tests green. The mod
compiles and loads; the problems below are **correctness, completeness and safety** issues, not
build breakage.

---

## 1. Architecture summary

The mod is a data-driven pantheon system. Players bind to one of **9 gods** (bella, bricoleur,
celia, deus, glacia, ingenium, magnus, velox, venatas) by binding a **Rune** to an armor stand,
which converts it into a **Monument** block. Players then:

* **Pray** at their Monument (timed channel) → gain *favor*.
* **Offer** themed items at their Monument → gain *favor* (item-gated by per-god validators).
* Favor crosses **tier thresholds** (Initiate → Devout → Exalted → Ascendant) → unlocks
  **blessings** (attribute modifiers, potion effects, conditional-combat buffs, etc.), all
  authored per-tier in the god JSON.
* Abandoning a god (**apostasy**) inflicts a latent curse and permanent **scars**.
* God **bosses** spawn at generated **temples** and at tag-matched vanilla structures; defeating
  them drops divine weapons.
* **Rivalry** events periodically apply cross-pantheon pressure.

Server-authoritative capability (`PlayerDivinityData`) holds all per-player state; it is synced
to clients via `SyncDivinityPacket` and persists through death/dimension change.

---

## 2. Findings by severity

### 2.1 Build-breaking
None. Project compiles and all unit tests pass.

### 2.2 Runtime-crashing

| ID | Finding | Location |
|----|---------|----------|
| **R1** | **Dedicated-server crash.** `PlayerAnimationPacket.handle` calls the `@OnlyIn(CLIENT)` `PlayerAnimationHandler` directly inside `enqueueWork`, with no `DistExecutor` guard (every sibling packet guards it). On a dedicated server the lambda links client/playerAnimator classes → `NoClassDefFoundError`. This packet is sent to *tracking* players from server prayer/offering flows, so it executes the moment anyone prays near others. | `network/PlayerAnimationPacket.java` |
| **R2** | **Parser crash aborts all blessings for several gods.** `EffectParser.parseConditionalCombat` calls `Condition.valueOf(...)` with no try/catch. God JSON uses conditions absent from the enum (`on_fire_target`, `melee_hit`, `sprinting`, `animal_target`, `undead_target`, `enderman_nearby`, `wither_target`). The thrown `IllegalArgumentException` propagates out of `EffectProfileCache.recompute`, so effect computation dies on login/respawn/tier-up for bella, glacia, velox, venatas, deus, ingenium, magnus. | `effect/EffectParser.java:150` |
| **R3** | `DivineVfxPacket.encode` writes `school` with no null normalization (sibling `BossVisualEffectPacket` defends). Latent NPE on send if a null school is ever passed. | `network/DivineVfxPacket.java` |

### 2.3 Gameplay-breaking (core loop non-functional)

| ID | Finding | Location |
|----|---------|----------|
| **G1** | **Every attribute blessing applies 0.0.** God JSON writes `"amount"` (×39), parser reads `"value"`. All attack-damage/armor/health/speed blessings are silently no-ops. | `effect/EffectParser.java:118` vs `gods/*.json` |
| **G2** | **Every potion blessing is dropped.** God JSON writes `"effect_id"` (×68), parser reads `"effect"` → logs "missing 'effect'" and returns null. No fire-resistance/strength/regen blessings ever apply. | `effect/EffectParser.java:88,93` |
| **G3** | `passive_regen` reads `"base_rate"`, JSON writes `"heal_per_second"` (×2). Regen blessing always defaults. | `effect/EffectParser.java:158` |
| **G4** | **Tier-up never refreshes effects.** Prayer/offering raise the tier numerically but nothing calls `EffectProfileCache.recompute`. `PrayerManager.completePrayer` even bypasses `BlessingStateMachine.onPrayerCompleted` entirely (manual `addFavor`, never sets tier). Net: praying gains favor but **never tiers up**, and offering tiers up but the new blessings only appear after a relog/respawn/dimension-change. The entire blessing reward layer is effectively unreachable in normal play. | `prayer/PrayerManager.java:159`, `offering/OfferingProcessor.java`, `state/BlessingStateMachine.java` |
| **G5** | **Offering system is orphaned.** `OfferingProcessor.processOffering` has zero callers; `MonumentBlock.use` only ever starts a prayer. The whole `offering/` package + 17 validator JSONs do nothing in game. | `content/MonumentBlock.java:99`, `offering/OfferingProcessor.java:24` |
| **G6** | **Offering item-gating is bypassed.** The 9 god-referenced `*_offering` validators gate items via `accept_tags`, but the code only reads `allow_items`. With no allowlist parsed, `checkAllowItems` passes everything. Compounding: the 18 referenced themed item tags have no tag files. | `offering_validators/*.json`, `offering/validators/OfferingRuleChecks.java` |

### 2.4 Incomplete features (built but not wired)

| ID | Finding | Location |
|----|---------|----------|
| **I1** | Latent-curse `damage_multiplier` parsed/saved but never applied (`getDamageMultiplier()` has no consumer). | `apostasy/LatentCurseManager.java:240` |
| **I2** | Scar `durability_penalty` summed but never applied (`getDurabilityPenaltyMultiplier()` unused). | `apostasy/ScarEffectHandler.java:187` |
| **I3** | Scar `death_penalty_increase` written to persistent data on death but never read. | `apostasy/ScarEffectHandler.java:103` |
| **I4** | Blessing `durability_multiplier`, `cooldown_reduction`, `trade_efficiency`, and `transgression` SOUL_LEECH/VOID_TOUCH effects are parsed but have no runtime consumer (no current god JSON authors them in blessings, so latent). | `effect/EffectEventHandler.java:239`, `effect/effects/*` |
| **I5** | 3-phase **apostasy trial** system (`ApostasyTrialManager`) is unreachable — `initiateApostasy` has no caller; the only entry (`apostasy` command) uses `forceApostasy` which skips trials. | `apostasy/ApostasyHandler.java:92` |
| **I6** | Data-driven **weapon-ability** subsystem (`item/ability/**`, all 7 impls + `DivineAbilityRegistry`) is never dispatched: `DivineWeaponItem.use` has a hardcoded `switch(godId)` over all 7 melee gods, so the `default -> tryDataDrivenAbility` branch is dead, and no god JSON defines `weapon_ability`. The hardcoded abilities are the live gameplay; the data path is a parallel unfinished system. | `item/DivineWeaponItem.java:245` |
| **I7** | `rivalry/EventExecutor` `SACRED_SITE_SPAWN` branch is a stub ("Would spawn a structure — for now just notify"); also unreachable because `EventScheduler.chooseEventType` never emits it (nor `DIVINE_BLESSING`/`INSPIRATION`). | `rivalry/EventExecutor.java:147`, `rivalry/EventScheduler.java:179` |
| **I8** | Public `SpellsNGodsEvents` (`TierChangeEvent`, `PrayerCompleteEvent`, `OfferingEvent`, `CurseExpiredEvent`, `BossDefeatedEvent`, `BossSpawnedEvent`) are declared but never posted — a silent trap for KubeJS/FTBQuests integration. | `compat/SpellsNGodsEvents.java` |
| **I9** | `structure protect on/off` command only prints a message; it never sets `SpellsNGodsConfig.COMMON.structureProtection`, so it is a no-op. | `command/SpellsNGodsCommands.java:534` |
| **I10** | Custom schematic loading is unusable: `SchematicLoader` reads `config/runic_gods/schematics`, the mod creates `config/spells_n_gods/schematics`, and `ShrineConfig` docs say a third path. Inert by default (`useCustomSchematics=false`). | `structure/SchematicLoader.java:49` |

### 2.5 Polish / content issues

| ID | Finding | Location |
|----|---------|----------|
| **P1** | Missing lang keys: `item.spells_n_gods.rune.scarred`, `spells_n_gods.error.monument_creation_failed`, `spells_n_gods.tier.advanced` (the tier-up message!). | `lang/en_us.json` |
| **P2** | `sounds.json` declares 9 subtitle keys with no matching lang entries. | `lang/en_us.json` |
| **P3** | Boss reload bug: enrage attribute buffs are not re-applied when a boss is loaded already in the ENRAGED phase (`updatePhase` only buffs on the *transition*). Reloaded enraged bosses permanently lose their speed/damage bonuses. | `boss/GodBossEntity.java` |
| **P4** | `BossShieldGoal.start()` adds +20 absorption each activation, `stop()` never removes it → unbounded absorption accumulation over a long fight. | `boss/ai/BossShieldGoal.java` |
| **P5** | Glacia's "frosted ice on hit" never fires — the block test requires `isSolidRender(...)` **and** `== Blocks.WATER` (contradiction). | `item/DivineWeaponItem.java:~594` |
| **P6** | `SpellsNGodsAPI.hasTierOrHigher` does `DivineTier.valueOf(...)` uncaught — throws on bad script input. | `compat/SpellsNGodsAPI.java:90` |
| **P7** | Orphaned/dead-but-harmless: 9 per-god boss loot tables (drops are code-driven in `ModIntegrationLayer`), 10 command lang keys (commands use `Component.literal`), unused crossbow/bow pull-stage textures, `DomainStructureTags.FORGE`/`forge_structures.json`. | various |
| **P8** | `DivineCrossbowItem` use-on-cooldown falls through to `super.use`, firing the loaded bolt unintentionally. | `item/DivineCrossbowItem.java:71` |

### 2.6 Design notes (intentional or out-of-scope to change)

* **D1** Two parallel god-spawn systems both active (dedicated temple worldgen + dynamic
  tag/tier `StructureSpawnManager`). They use separate SavedData and don't corrupt each other,
  but a god can be encountered via both. Left as-is; documented. A server owner can disable the
  dynamic system via `structure_spawns.toml`.
* **D2** `GodTempleStructure.findGenerationPoint` never rejects a site (no flatness/biome
  filter), and the temple biome tag is `#minecraft:is_overworld`. Temples can generate on poor
  terrain. Mitigated below by narrowing the biome tag; full terrain sampling left as future work.
* **D3** Effects are **code-driven** from god `blessings`; the `EffectDefinition` /
  `effects/*.json` loader path is orphaned (one decorative file). Not harmful.

---

## 3. Remediation plan (ordered)

Foundational correctness first, then wiring, then polish.

1. **Effect parser contract (R2, G1–G3).** Make `EffectParser` tolerant of the data's actual
   keys (`amount`|`value`, `effect_id`|`effect`, `heal_per_second`|`base_rate`) and wrap all enum
   parsing in try/catch. Add the 7 missing `conditional_combat` conditions and make condition
   evaluation target-aware. This is the single highest-impact fix — it makes blessings functional
   and stops the recompute crash.
2. **Reward-loop recompute (G4).** Route `PrayerManager.completePrayer` through
   `BlessingStateMachine.onPrayerCompleted`, and call `EffectEventHandler.onTierChange` after any
   tier increase (prayer + offering). This makes praying actually tier the player up and apply
   blessings immediately.
3. **Offerings (G5, G6).** Wire `OfferingProcessor` into `MonumentBlock.use` (held item → offer),
   recompute on accept; teach the validators to read `accept_tags` as an `allow_items` synonym;
   author the 18 themed item tag files so offerings are real, themed, gated progression.
4. **Server safety (R1, R3).** Guard `PlayerAnimationPacket` with `DistExecutor`; null-normalize
   `DivineVfxPacket`.
5. **Polish (P1–P6, P8).** Missing lang keys + sound subtitles, boss enrage-on-reload, shield
   absorption cleanup, glacia ice bug, API try/catch, crossbow cooldown fall-through.
6. **Wire contained dead effects (I1–I3, I9).** Apply latent-curse damage multiplier
   (`LivingHurtEvent`), scar durability penalty (item-damage), scar death penalty (drops/xp), and
   make the `structure protect` command set its config value.
7. **Validate.** `./gradlew build`, run tests, JSON sanity grep, TODO sweep.

Larger/ambiguous items (I5 apostasy-trial trigger, I6 weapon-ability subsystem, I7 sacred-site
spawn, I8 public events, D1/D2 spawn design) are documented as known/intentional with rationale
rather than rewritten, per the "complete existing systems conservatively, don't invent" directive.

---

## 4. What was implemented

### Core reward loop — now functional end to end
* **`EffectParser`** rewritten to be key-tolerant: reads `amount`|`value` (attribute magnitude),
  `effect_id`|`effect` (potion id), `heal_per_second`|`base_rate` (regen). All enum parsing
  (`conditional_combat`, `transgression`) now goes through a non-throwing `parseEnum` helper that
  logs and skips a bad value instead of aborting the whole recompute. *(Fixes R2, G1, G2, G3.)*
* **`ConditionalCombatEffect`** gained the 7 conditions the data uses
  (`ON_FIRE_TARGET`, `ANIMAL_TARGET`, `UNDEAD_TARGET`, `WITHER_TARGET`, `ENDERMAN_NEARBY`,
  `MELEE_HIT`, `SPRINTING`) plus a target-aware `isConditionMet(player, target)`.
  `EffectEventHandler.onPlayerAttack` now passes the victim so target conditions resolve.
* **`PrayerManager.completePrayer`** now routes through `BlessingStateMachine.onPrayerCompleted`
  (so prayer actually advances the tier), calls `EffectEventHandler.onTierChange` to apply new
  blessings immediately, and fires a tier-up celebration mirroring offerings. *(Fixes G4.)*
* **`OfferingProcessor`** calls `EffectEventHandler.onTierChange` after accepting an offering so a
  tier-up or HALTED→ACTIVE revival applies its effects at once.
* **`MonumentBlock.use`** wires offerings into the world: a held item is routed to
  `OfferingProcessor` (with feedback), an empty hand prays. *(Fixes G5.)*

### Offerings — real, themed, gated
* **`OfferingRuleChecks.parseAllowList`** merges `accept_tags` (item-rule authoring) and
  `allow_items` (action-rule authoring) into one allowlist; both validators use it. *(Fixes G6.)*
* **18 themed item-tag files** authored under `tags/items/` (`fire_themed`, `smelted_goods`,
  `ice_themed`, `frozen_goods`, `holy_themed`, `golden_goods`, `nature_themed`, `harvested_goods`,
  `lightning_themed`, `storm_goods`, `ender_themed`, `void_touched`, `eldritch_themed`,
  `arcane_artifacts`, `evocation_themed`, `mechanisms`, `blood_themed`, `alchemical`), each
  populated with thematically appropriate vanilla items and datapack-overridable.

### Server safety
* **`PlayerAnimationPacket.handle`** now guards the client-only handler with
  `DistExecutor.unsafeRunWhenOn(Dist.CLIENT, …)` — no more dedicated-server `NoClassDefFoundError`.
  *(Fixes R1.)*
* **`DivineVfxPacket`** null-normalizes `school`. *(Fixes R3.)*

### Polish & contained completions
* **Lang:** added `item.spells_n_gods.rune.scarred`, `spells_n_gods.error.monument_creation_failed`,
  `spells_n_gods.tier.advanced` (the tier-up message), and the 9 `subtitles.*` sound subtitles.
  *(Fixes P1, P2.)*
* **`GodBossEntity.readAdditionalSaveData`** re-applies enrage buffs when a boss loads already in
  the ENRAGED phase (idempotent). *(Fixes P3.)*
* **`BossShieldGoal`** removes the absorption it granted in `stop()` so it can't accumulate. *(P4.)*
* **`DivineWeaponItem`** Glacia frost: water sources → frosted ice, solid ground → survivable snow
  layer (the old `solidRender && == WATER` test was a contradiction). *(P5.)*
* **`SpellsNGodsAPI.hasTierOrHigher`** guards `DivineTier.valueOf` against bad script input. *(P6.)*
* **`SpellsNGodsCommands`** `structure protect on/off` now actually sets the config value. *(I9.)*
* **`DivineCrossbowItem`** ability-on-cooldown swallows the input instead of firing the loaded
  bolt. *(P8.)*
* **Apostasy:** latent-curse `damage_multiplier` applied via a `LivingHurtEvent` handler in
  `LatentCurseTickHandler` *(I1)*; scar `death_penalty_increase` consumed as amplified XP loss in
  `ScarEffectHandler.onXpChange` *(I3)*.

### Durability scaling — implemented via Mixin *(resolves I2 + the blessing `durability_multiplier`)*

Forge 1.20.1 has no per-point item-durability event, so a minimal, well-contained **Mixin** now
applies the scar penalty (items wear faster) and Aurex/Permanence blessing (items wear slower):

* **`mixin/ItemStackMixin`** — a single `@ModifyVariable(method="hurt", at=HEAD, argsOnly, ordinal=0)`
  that scales `ItemStack.hurt`'s damage `amount`. `hurt` is the one chokepoint all tool/armor/elytra
  wear routes through; its `@Nullable ServerPlayer` arg makes the hook **server-and-player only**.
  The mixin contains **no gameplay logic** — it delegates to the service.
* **`durability/DurabilityDamageHandler`** (MC-facing service) — guards (null player, creative
  `instabuild`, empty/unbreakable/non-durable stack, non-positive amount → vanilla unchanged),
  resolves the two existing hook points (`ScarEffectHandler.getDurabilityPenaltyMultiplier` ≥ 1.0,
  and `EffectProfileCache.get(player).getMultiplier(DURABILITY_MULTIPLIER)` ≤ 1.0), and delegates the
  math. Returns the input untouched whenever the combined multiplier is exactly 1.0.
* **`durability/logic/DurabilityModifierService`** (pure, no Minecraft imports) — multiplicative
  `combineMultipliers` + **probabilistic rounding** `applyMultiplier(base, mult, rng)`, so a
  fractional factor is meaningful on the usual 1-point hit (×1.5 → 50% chance of 2; ×0.5 → 50% chance
  of 0), using the `RandomSource` `hurt` already holds. Deterministic given the RNG draw.
* **Build wiring:** `org.spongepowered.mixin` (mixingradle 0.7.38) plugin + `annotationProcessor
  'org.spongepowered:mixin:0.8.5:processor'` + a `mixin { add …; config … }` block; `mixins.json`
  in resources; `[[mixins]]` declared in `mods.toml`. The AP generates `spells_n_gods.refmap.json`
  (verified: `hurt` → SRG `m_220157_`), embedded in the jar so the mixin applies in production too.
* **Tests:** 11 new JUnit cases in `DurabilityModifierServiceTest` cover identity, probabilistic
  bounds, scar/blessing/combined factors, clamping, and determinism.

Safety properties: server-side only (ServerPlayer-gated), single application (one chokepoint),
respects creative/unbreakable/empty/non-durable items, and preserves vanilla + Unbreaking behavior
unless a modifier explicitly applies.

### Bonus: two pre-existing dedicated-server crashes fixed (found via the mixin runtime smoke test)

Running `./gradlew runServer` to verify the mixin surfaced two latent client/server leaks that
crashed dedicated servers **before any of this work** (the original mod was only ever launched via
`runClient`). Both are now fixed:

* **`SpellsNGodsMod`** registered `onRegisterParticleProviders` (and `onClientSetup`) on the mod bus
  unconditionally; on a dedicated server FML force-loaded the client-only
  `RegisterParticleProvidersEvent` → `ParticleEngine` and crashed during `CONSTRUCT`. Now those
  client-only mod-bus listeners are guarded behind `FMLEnvironment.dist == Dist.CLIENT`.
* **`RuneItem.appendHoverText`** referenced `Minecraft.getInstance().player` (client-only
  `LocalPlayer`) directly, so the item class failed to load on a dedicated server (RuntimeDistCleaner
  "invalid dist") during item registration. The client tooltip logic moved to a new client-only
  `client/RuneClientTooltip`, invoked via `DistExecutor.unsafeRunWhenOn(Dist.CLIENT, …)`.

With these, the dedicated server now boots to `Done` with the mixin applied.

## 5. Final status

### Validation run
| Command | Result |
|---------|--------|
| `./gradlew clean build --offline` | **BUILD SUCCESSFUL** |
| Unit tests (`:test`) | **26/26 PASS** (SpawnLogic 13, EventPool 2, **DurabilityModifierService 11**) |
| `./gradlew runServer --offline` | **Boots to `Done (17.9s)`**; `ItemStackMixin` applies cleanly; no mixin/dist errors |
| Refmap generated + embedded | `spells_n_gods.refmap.json` in jar: `hurt` → SRG `m_220157_` |
| JSON validity sweep (all `data/` + `assets/` JSON) | **0 invalid** |

Datagen (`runData`) is **not applicable** — the mod registers no `GatherDataEvent` providers; all
resources are hand-authored.

### Files changed
21 files modified, 19 added. Java: effect parser/conditions/handler, prayer manager, offering
processor + validators, monument block, boss entity + shield goal, two network packets, apostasy
curse + scar handlers, divine weapon/crossbow, API, commands. Data: 18 new item tags, lang
additions. Docs: README worship section, this audit.

### Resolved
All build-breaking (none), runtime-crash (R1–R3), and gameplay-breaking (G1–G6) findings are fixed.
The blessing reward layer, prayer/offering progression, and themed offering gating now work
end-to-end and apply effects immediately on tier-up. Contained incomplete features I1, I3, I9 and
all targeted polish (P1–P6, P8) are done.

### Remaining risks / recommended future work (intentionally deferred)
* **I4** — `cooldown_reduction` / `trade_efficiency` / `transgression` SOUL_LEECH/VOID_TOUCH are
  parsed but unused; no current god authors them in blessings, so they are latent, not broken.
* **I5 — apostasy trials** (`ApostasyTrialManager`) remain reachable only via admin
  `forceApostasy`; a player-facing trigger for `initiateApostasy` is a design decision left to the
  author.
* **I6 — data-driven weapon-ability subsystem** (`item/ability/**`) is superseded by the hardcoded
  per-god abilities in `DivineWeaponItem` and stays as an unused-but-registered fallback path.
* **I7 — `SACRED_SITE_SPAWN`** rival event is an unreachable stub (`EventScheduler` never emits it).
* **I8 — public `SpellsNGodsEvents`** are mostly unposted; integrators relying on
  `TierChangeEvent`/`BossDefeatedEvent` etc. will not receive them yet.
* **D1/D2 — spawn design:** the dedicated-temple and dynamic tag/tier spawn systems both run; a god
  can be met via either. Server owners can disable the dynamic system in `structure_spawns.toml`.
  Temple terrain-fit filtering (flatness/biome) remains future work.
* **Offering consumption:** offering a stack consumes the whole stack (existing `OfferingProcessor`
  design, with a stack-size value bonus). Players should offer deliberate amounts.
