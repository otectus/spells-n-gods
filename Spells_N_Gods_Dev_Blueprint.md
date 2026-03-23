# Spells n'' Gods – Development Blueprint (Forge 1.20.1)

This is the **full implementation blueprint** for a Forge 1.20.1 mod called **Spells n'' Gods**.  
The mod is designed as a **divine framework**: servers can **fully replace** the default pantheon using **datapacks** (no code changes required).

Key goals:
- One god per player (binding on first monument creation)
- Monument-driven worship: prayer + offerings
- Favor + Blessing tiers + rival pressure
- Extremely punishing apostasy (optional, configurable)
- **Datapack-first** architecture: gods, rivals, blessings, offerings, events, statues, UI text

---

## 0) Guiding Principles

1) **Everything is data** (except engine glue):
- Gods are JSON.
- Blessings are JSON.
- Offerings are JSON.
- Rivalries are JSON.
- Events are JSON.
- Monument visuals are JSON (with resourcepack hooks).

2) **Safe-by-default**:
- Datapack errors must not crash servers. They should log + fallback.
- Invalid definitions are skipped with warnings.

3) **Anti-cheese**:
- Offerings and “combat trophies” require validation rules.
- Timers use server time (real-world wall clock stored per player).

4) **Compatibility posture**:
- Spells n'' Gods should not hard-depend on other magic mods.
- Integrations live behind optional compat modules and/or datapack tags.

---

## 1) Features Summary

### 1.1 Player Lifecycle
- Player joins world.
- Player is “unbound” until they **bind** to a god.
- Binding occurs by using **Rune of [God]** on an **armor stand**.
- The stand is consumed, replaced by a **Monument** (block + block entity).
- Binding locks the player to that god (unless apostasy enabled).

### 1.2 Worship Loop
- **Prayer**: interaction at monument; slows/stops favor decay for a window.
- **Offerings**: items/actions; maintain blessing active state.
- Missing offering for **72 hours** → Blessing becomes **HALTED**:
  - All positive blessing effects off.
  - Rival curses/events continue.

### 1.3 Favor and Blessings
- Favor is per-player-per-god (but only one chosen god is “primary”).
- Favor gates **blessing tiers** (Initiate/Devout/Exalted/Ascendant).
- Datapack defines tier thresholds or uses global config defaults.

### 1.4 Rival Pressure
- Each god has a primary and optional secondary rival.
- Rival pressure can:
  - Apply debuffs
  - Trigger events
  - Increase offering costs
  - Disrupt prayer/rituals

### 1.5 Apostasy (Optional)
- An explicit, ritualized process to abandon a god.
- Permanent scars + cooldowns + “Unbound” period.
- Previous god may apply a latent curse for days/weeks.

---

## 2) Architecture Overview

### 2.1 Core Modules
1) **Data Layer**
   - Datapack JSON loading
   - Validation + schema versioning
   - Registry objects for gods/effects/offering rules/events/statues

2) **State Layer**
   - Player divinity state (chosen god, favor, timers, halted/active)
   - Monument state (owner, god, chunk anchor, offering history)
   - World flags (optional: divine events, regional influence)

3) **Rules Engine**
   - Favor mutation rules (prayer, decay, offerings, behaviors)
   - Blessing state machine (active/ halted)
   - Rival pressure router
   - Event scheduler

4) **Gameplay Integration**
   - Interactions (right-click monument)
   - Offering UI
   - Prayer channeling
   - Buff application

5) **Client Presentation**
   - Monument rendering (skin-based or statue model)
   - Particles/audio
   - HUD (optional)
   - Simple UI screens for prayer/offering status

### 2.2 Save Data
- Use `SavedData` / `Capability` patterns.
- Player persistent state should survive respawns, dimension changes.
- Use a single **capability** on player:
  - chosenGodId
  - favor (float)
  - lastPrayerTime (epoch ms)
  - lastOfferingTime (epoch ms)
  - blessingState (ACTIVE/HALTED)
  - currentTier
  - apostasyCooldownEnd (epoch ms)
  - latentCurseEnd (epoch ms)
  - offeringHistory (compact ring buffer)

Monument block entity stores:
- ownerUUID
- godId
- createdAt (epoch ms)
- chunkAnchor
- lastPrayerByOwner (epoch ms)
- lastOfferingByOwner (epoch ms)
- offeringHistory (optional)
- integrity / desecrated flag

---

## 3) Datapack-First Design

### 3.1 Namespaces
Recommend:
- Mod namespace: `spells_n_gods`
- Default pantheon: `spells_n_gods:gods/*`
- Servers can override by shipping datapack with same paths, or define new gods under their own namespace.

### 3.2 Data Model Files

#### A) Gods
Path: `data/<namespace>/spells_n_gods/gods/<id>.json`

Fields (draft):
```json
{
  "schema": 1,
  "id": "aurex",
  "display_name": "Aurex",
  "gender": "male",
  "domains": ["creation", "form", "crafting"],
  "philosophy": "What is made must endure.",
  "rivals": { "primary": "umbriel", "secondary": "chronyx" },

  "binding": {
    "rune_item": "spells_n_gods:rune_aurex",
    "monument": {
      "block": "spells_n_gods:monument",
      "variant": "aurex",
      "statue_profile": "skin_owner",
      "fallback_profile": "MHF_Steve"
    }
  },

  "favor": {
    "thresholds": { "initiate": 10, "devout": 35, "exalted": 70, "ascendant": 95 },
    "decay_per_day": 2.0
  },

  "worship": {
    "prayer": { "min_seconds": 10, "cooldown_seconds": 300 },
    "offering": { "grace_hours": 72, "diminishing_returns": true, "validator": "crafted_unused_high_quality" }
  },

  "blessings": {
    "initiate": { "effects": [ ... ], "monument_ability": { ... } },
    "devout":   { "effects": [ ... ], "monument_ability": { ... } },
    "exalted":  { "effects": [ ... ], "monument_ability": { ... } },
    "ascendant":{ "effects": [ ... ], "monument_ability": { ... } }
  },

  "rival_pressure": {
    "primary": { "effects": [ ... ], "event_pool": ["..."] },
    "secondary": { "effects": [ ... ], "event_pool": ["..."] }
  },

  "localization": {
    "name_key": "god.spells_n_gods.aurex",
    "desc_key": "god.spells_n_gods.aurex.desc"
  }
}
```

#### B) Effects
Path: `data/<namespace>/spells_n_gods/effects/<id>.json`  
Effects are reusable building blocks used by blessings and rival pressure.

Example:
```json
{
  "schema": 1,
  "id": "durability_multiplier",
  "type": "attribute_modifier",
  "target": "generic.max_health",
  "operation": "multiply_total",
  "value": 0.0
}
```

In practice you’ll likely implement effects as:
- Vanilla potion effects / attributes (easy)
- Custom effect types (durability multipliers, craft rerolls, event chances)
- “Proc” hooks (on hit, on craft, on harvest, on death)

#### C) Offering Validators
Path: `data/<namespace>/spells_n_gods/offering_validators/<id>.json`  
These define rule sets. The *engine* provides validator types; datapack provides parameters.

Example:
```json
{
  "schema": 1,
  "id": "crafted_unused_high_quality",
  "type": "item_rule",
  "rules": {
    "must_be_crafted": true,
    "must_be_unused": true,
    "min_rarity": "rare",
    "deny_tags": ["spells_n_gods:mass_produced"]
  },
  "value_scoring": {
    "base_value": 10,
    "rarity_multiplier": { "common": 1, "uncommon": 1.2, "rare": 1.6, "epic": 2.5 },
    "enchant_weight": 0.3
  }
}
```

#### D) Events
Path: `data/<namespace>/spells_n_gods/events/<id>.json`
Events are god-flavored occurrences (blight wave, arcane audit, lockdown, sabotage).

Example:
```json
{
  "schema": 1,
  "id": "arcane_audit",
  "type": "zone_disruption",
  "duration_seconds": 120,
  "radius": 24,
  "conditions": {
    "requires_rival": "meridian",
    "tier_at_least": "devout"
  },
  "effects": [
    { "type": "increase_cooldown_multiplier", "value": 1.25 },
    { "type": "ritual_failure_chance", "value": 0.15 }
  ],
  "presentation": {
    "title_key": "event.spells_n_gods.arcane_audit",
    "particles": "spells_n_gods:arcane_scrape",
    "sound": "spells_n_gods:divine_gavel"
  }
}
```

#### E) Monument Visuals
Path: `data/<namespace>/spells_n_gods/monuments/<variant>.json`
Defines visual/interaction theming.

Example:
```json
{
  "schema": 1,
  "variant": "aurex",
  "material_palette": ["minecraft:deepslate", "minecraft:gold_block"],
  "particle": "spells_n_gods:embers",
  "ambient_sound": "spells_n_gods:anvil_hum",
  "ui_theme": "forge_gold"
}
```

---

## 4) Engine Implementation Details

### 4.1 Registries and Reloading
Use datapack reload events to rebuild runtime registries:
- `AddReloadListenerEvent` (Forge) for JSON resources.
- Parse/validate, then publish an immutable registry snapshot.
- Any active players referencing missing gods:
  - Keep their chosen god id
  - Mark as “MISSING_DEITY”
  - Disable blessings until fixed (log loudly)

### 4.2 Blessing State Machine
States:
- UNBOUND
- ACTIVE
- HALTED
- UNBOUND_COOLDOWN (after apostasy)
- MISSING_DEITY (datapack removed deity)

Transitions:
- UNBOUND → ACTIVE (binding)
- ACTIVE → HALTED (offering timer exceeded)
- HALTED → ACTIVE (valid offering)
- ACTIVE/HALTED → UNBOUND_COOLDOWN (apostasy)
- ANY → MISSING_DEITY (god removed)

### 4.3 Timers (Real-World)
Store epoch milliseconds in player capability:
- lastOfferingEpochMs
- lastPrayerEpochMs

Compute “hours since offering” on login/tick:
- if now - lastOffering > grace → HALTED

### 4.4 Prayer Implementation
- Player interacts with monument → start “channeling” (like eating/using item).
- Server counts seconds; if uninterrupted and meets min, apply:
  - “PrayerWindow” timestamp = now
  - Temporary modifier: favor decay paused for N hours (configurable)
  - Optional: small favor gain

### 4.5 Offering Implementation
- Player opens offering UI at monument:
  - Slot(s) for offering items
  - Optional: “ritual action offering” button (timed challenge, mana burn, etc.)
- On confirm:
  - Validate via god’s `offering.validator`
  - Compute offering value score
  - Update lastOfferingEpochMs
  - Apply blessing active
  - Increase favor proportionally (with diminishing returns)
  - Record offering history

Anti-cheese:
- diminishing returns by repeating same item id
- deny tags list
- require “crafted by player” (NBT or tracked via crafting hook)

### 4.6 Favor Gain and Decay
Favor rules should be data-driven but bounded:
- +X for successful prayer (small)
- +Y for offerings (main)
- -Z per day if not prayed (configurable)
- Rival pressure can apply additional decay

Implement as:
- Daily “tick” computed from epoch delta since lastFavorUpdate.
- Avoid per-tick heavy logic.

---

## 5) Monument Implementation

### 5.1 The Binding Interaction
Listen for:
- Player uses Rune of [God] on ArmorStand entity.

Flow:
1. Verify player is UNBOUND (or apostasy allowed and currently UNBOUND_COOLDOWN expired).
2. Consume rune item (configurable).
3. Remove armor stand.
4. Place monument block at stand position (or nearest valid spot).
5. Create block entity storing ownerUUID + godId + createdAt.
6. Sync to client for rendering.

### 5.2 Monument Block + Block Entity
- Block has a facing, base model.
- Block entity stores owner profile:
  - If `skin_owner`: fetch player GameProfile
  - Cache texture hash
  - Fallback if offline/unknown

### 5.3 Visual Customization
Two approaches:
1) **Resourcepack statue model** (recommended for quality)
   - Use custom block model variant per god
   - Optional: animated texture and particles
2) **Skin statue renderer**
   - Render a simple humanoid statue using player skin
   - God-specific tinting + rune overlay

Both should be selectable in JSON `monument.variant`.

---

## 6) Effects System (Core of Customization)

### 6.1 Effect Types (Engine-Provided)
Implement a robust effect framework with these types:
- `potion_effect` (apply vanilla effect)
- `attribute_modifier` (vanilla attributes)
- `cooldown_multiplier` (custom map keyed by category)
- `mana_cost_multiplier` (optional compat hook)
- `durability_multiplier` (on item damage event)
- `crafting_outcome_roll` (on crafting event)
- `drop_bonus` (on mob death)
- `zone_aura` (chunk/radius effects)
- `event_chance_modifier` (affects event scheduler)
- `restriction_bypass` (rare proc)
- `behavior_rule` (bonus/penalty based on actions)

### 6.2 Effect Application Model
- Compute active effects from:
  - blessing tier effects
  - rival pressure effects
  - latent curse effects
- Apply as cached “EffectProfile” per player:
  - recompute when tier changes, blessing toggles, datapack reloads
  - apply lightweight modifiers during events

Performance note:
- Avoid scanning too often.
- Use hooks at meaningful times: on join, on tier change, on offering, on prayer end.

---

## 7) Rival Pressure Engine

### 7.1 Pressure Sources
Pressure increases when:
- Player reaches higher tiers (Exalted/Ascendant)
- Rival’s domain is “fed” by player behavior (optional advanced mode)
- Server events occur

Pressure decreases when:
- Player performs consistent offerings/prayer
- Player performs rival-mitigating actions (optional)

### 7.2 Pressure Outputs
- Passive debuffs (slow decay, random misfires)
- Triggered events from `event_pool`
- Offering cost multipliers
- Monument interference (visual corruption, prayer interruption chance)

All of this is data-driven:
- Each god defines its rival pressure effects and event pools.

---

## 8) Apostasy System

### 8.1 Ritual Item
Apostasy requires an explicit item (datapack item tag):
- `spells_n_gods:apostasy_knife` (example)
- Prevent accidental switching.

### 8.2 Trial Phases
Phase examples (data-driven events):
- Phase 1: “Sever the Oath” (debuff + survive waves)
- Phase 2: “Walk Unbound” (no blessings, heavy rival pressure)
- Phase 3: “Prove Resolve” (timed objective)

### 8.3 Scars and Cooldowns
- Permanent scars: defined in config TOML.
- Cooldown: epoch time stored in player capability.
- Optional: ruined idol remains.

Datapack hooks:
- `apostasy/events/*.json` defines trial encounters
- `apostasy/scars/*.json` defines scar sets (server can swap sets)

---

## 9) Items and Content

### 9.1 Rune Items
- One rune item per god.
- Generated from datapack “god” definition:
  - Either hardcode a generic Rune item and store godId in NBT
  - Or generate separate items per god (more work, more resourcepack overhead)

Recommended:
- **One item**: `spells_n_gods:rune`
- NBT: `{God:"spells_n_gods:aurex"}`
- Tooltip localization via god definition.

### 9.2 Tags for Server Customization
Provide tags:
- `spells_n_gods:offerings/common`
- `spells_n_gods:offerings/rare`
- `spells_n_gods:mass_produced`
- `spells_n_gods:apostasy_items`
- `spells_n_gods:protected_zones` (blocks/areas)
- `spells_n_gods:worthy_mobs` (for Khelr trophy weighting)

Servers can target these in their own datapacks.

---

## 10) UI and Player Feedback

### 10.1 Monument Screen
- Shows:
  - chosen god name + domain
  - favor value + tier
  - blessing state (active/ halted) + remaining time to halt
  - last offering time
  - rival pressure indicator (optional)
- Buttons:
  - Pray
  - Offer
  - View Tenets (datapack text)
  - (Optional) Begin Apostasy

### 10.2 HUD (Optional)
Minimal indicator:
- god icon
- tier
- offering timer (like a candle melting)

Must be togglable server-side.

---

## 11) Custom Pantheon Replacement (Datapack Guarantees)

To ensure servers can replace gods easily, enforce these guarantees:

1) **No hardcoded god list**:
   - Default gods are shipped as datapack JSON inside mod jar.
   - Runtime loads from datapack registry only.

2) **No code assumes Aurex/Nyxara etc**:
   - IDs are strings.
   - Rival ids are references.
   - Effects are typed by generic engine.

3) **Statues are keyed by `variant`**:
   - Any new god supplies its variant name.
   - Resourcepack provides the model/texture if desired.
   - Fallback statue renderer always works.

4) **Offerings use validators**:
   - New gods can point at existing validator types.
   - Or servers can reuse validators with new parameters in JSON.

5) **Events are generic**:
   - Gods reference events by id.
   - Servers can add/replace events freely.

In short: servers can delete the default pantheon files, add their own, and the mod still functions.

---

## 12) Implementation Plan (Milestones)

### Milestone 1: Skeleton + Data Loading
- Project setup Forge 1.20.1
- JSON reload listeners
- Registry objects: GodDef, EventDef, ValidatorDef, MonumentDef
- Validation + logging

Deliverable: `/reload` picks up gods; debug command lists loaded gods.

### Milestone 2: Player State + Binding
- Player capability and persistence
- Rune item with NBT god id
- Armor stand interaction → place monument
- Bind player to god

Deliverable: players can bind and see god assigned.

### Milestone 3: Monument UI + Prayer
- Monument screen
- Prayer channeling + cooldown
- Favor decay model

Deliverable: prayer works and reduces decay.

### Milestone 4: Offerings + Blessing State
- Offering UI
- Validator framework (start with 2–3 core validators)
- Blessing active vs halted behavior
- Tier computation

Deliverable: offerings maintain blessings; 72h lapse halts.

### Milestone 5: Effects Engine (Tier Effects)
- Implement effect types (potions/attributes first)
- Apply tier effects on player tick (cached)
- Visual feedback

Deliverable: each tier meaningfully changes gameplay.

### Milestone 6: Rival Pressure + Events
- Pressure routing
- Event scheduler + execution
- Start with 4–6 generic event types

Deliverable: rival gods “push back” when you get strong.

### Milestone 7: Apostasy
- Ritual item + UI
- Trial events
- Scars + cooldowns + unbound period
- Ruined idol marker

Deliverable: switching gods works and is punitive.

### Milestone 8: Compat Add-Ons (Optional)
- Hooks for mana mods (Ars/Iron’s/Botania/Blood Magic) via optional integration modules
- Expose compat toggles in server config

Deliverable: if mods present, blessings can affect their resources.

### Milestone 9: Polish + Docs
- Full datapack authoring guide
- Example custom pantheon datapack
- Resourcepack templates for statues/icons
- Performance profiling

Deliverable: server owners can author gods without code.

---

## 13) Datapack Authoring Guide (Short, Practical)

### Replacing the Default Pantheon
1. Make a datapack.
2. Add `data/spells_n_gods/spells_n_gods/gods/` files with your new gods.
3. Optionally delete/override the defaults by providing same ids.
4. Provide monument variants and localization keys.
5. `/reload`.

### Adding a New God
- Create a new `gods/<id>.json` with:
  - domains
  - rivals
  - thresholds
  - validator
  - blessings per tier
- Add `monuments/<variant>.json` (optional)
- Add localization entries in your resourcepack.

---

## 14) Performance Notes
- Do not run heavy scans per tick.
- Cache player effect profiles; recompute only on state changes.
- Event scheduler should be low-frequency (e.g., check every 5–20 seconds).
- Favor decay computed based on epoch delta, not per-tick subtraction.

---

## 15) Testing Checklist
- Datapack reload with malformed god file: logs, skips safely.
- Player binds, logs out, logs in: state persists.
- Offering timer continues across restarts (epoch-based).
- Blessing halts after grace; resumes after offering.
- Rival pressure triggers events at high tiers.
- Apostasy cannot be accidental; scars apply; cooldown enforced.
- Removing a god definition puts bound players into MISSING_DEITY safely.

---

## 16) Deliverables to Ship
- Default datapack pantheon (8 gods)
- Default validators + events
- Base rune item + UI
- Monument block/entity + rendering
- Server config TOML
- Datapack authoring docs + sample custom pantheon datapack

---

## Appendix A: Effect Type Registry (Suggested IDs)
- `potion_effect`
- `attribute_modifier`
- `durability_multiplier`
- `craft_quality_roll`
- `drop_bonus`
- `cooldown_multiplier`
- `mana_cost_multiplier` (compat)
- `zone_aura`
- `event_trigger`
- `proc_rule`

---

## Appendix B: Minimal JSON Schema Versioning
Every JSON includes:
- `"schema": 1`
Future updates bump schema and migrate or warn.

---

If you want, I can also generate:
- A **sample custom pantheon datapack** folder layout (ready to zip)
- A **validator rules cookbook** (anti-cheese patterns)
- An **event catalog** with 30+ events across the domains
