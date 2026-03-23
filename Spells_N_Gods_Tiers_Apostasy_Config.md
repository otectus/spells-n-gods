# Spells n'' Gods – Blessings, Apostasy, and Config Schema Draft

This document extends the **Spells n'' Gods** design bible with:
- **Blessing tiers per god** (what you gain at each tier)
- **Apostasy consequences** (switching gods is possible, but miserable)
- A **config schema draft** (server-first, Forge 1.20.1 friendly)

---

## 1) Global Definitions

### 1.1 Favor vs Blessing State
- **Favor**: long-term relationship score with your chosen deity.
- **Blessing**: active status effects/permissions granted by your deity.

Blessings require upkeep:
- Prayer prevents Favor decay.
- Offerings maintain Blessings.
- **No offering for 72 hours** → Blessing enters **HALTED** state:
  - All positive blessing effects stop.
  - Rival god effects/curses **continue**.
  - Favor does not reset; you can recover by making a valid offering.

### 1.2 Monument as the Single Interface
- Binding creates a **Monument** for your chosen god.
- All prayer/offerings occur at the monument.
- Monument is chunk-bound, protected by configurable rules, and acts as the “divine anchor.”

---

## 2) Blessing Tiers (Global Structure)

These tiers apply to all gods. Each god defines its own effects per tier.

### Blessing Tier Names
- **Tier 0: Haltered** (Blessing HALTED due to missed offering; Favor unchanged)
- **Tier 1: Initiate** (baseline)
- **Tier 2: Devout** (strong)
- **Tier 3: Exalted** (high-impact)
- **Tier 4: Ascendant** (server-defining; rare; intended for late game)

### How Tiers Are Reached
- Tier is determined by **Favor thresholds** AND **Blessing active state**.
- If Blessing is HALTED, the player is effectively “Tier 0” for benefits even if Favor is high.

Suggested thresholds (configurable):
- Initiate: Favor >= 10
- Devout: Favor >= 35
- Exalted: Favor >= 70
- Ascendant: Favor >= 95

---

## 3) Per-God Blessing Tiers

Below, each god lists:
- **Passive**: always-on benefits while Blessing is active
- **Monument Ability**: a bound interaction at the monument (ritual, boon, conversion, etc.)
- **Rival Pressure**: how primary/secondary rivals push back at higher tiers

### 3.1 AUREX (God of Creation & Form)
Theme: permanence, mastery, craftsmanship

**Initiate**
- Passive: +minor durability bonus; reduced crafting waste
- Monument: “Temper” one crafted item per day (minor quality roll)
- Rival Pressure: Umbriel increases minor defect chance during storms

**Devout**
- Passive: chance for “Flawless” crafts; repair costs reduced
- Monument: “Seal” an item (slower degradation for X time)
- Rival Pressure: Chronyx adds periodic micro-decay pulses to sealed items

**Exalted**
- Passive: crafted gear gains 1 bonus modifier slot (configurable)
- Monument: “Forge Oath” (bind a tool to you; improved performance, cannot be traded)
- Rival Pressure: Umbriel can trigger catastrophic flaws if you offer mass-produced junk

**Ascendant**
- Passive: structures you build gain reinforced integrity (reduced explosion/block damage)
- Monument: “Anoint a Workshop” (a chunk-based aura improves all crafting inside)
- Rival Pressure: Chronyx accelerates entropy events in that chunk if you stagnate

---

### 3.2 NYXARA (Goddess of Magic & Arcana)
Theme: research, mutation, dangerous knowledge

**Initiate**
- Passive: small mana efficiency; minor cast-speed bonus
- Monument: “Arcane Study” (convert XP into Favor or temporary mana pool)
- Rival Pressure: Meridian increases cooldowns near towns/colonies

**Devout**
- Passive: reduced mana cost for diverse spell usage; minor spell mutation chance (beneficial)
- Monument: “Glyph Whisper” (unlock a randomized minor glyph augmentation weekly)
- Rival Pressure: Mortyss adds backlash chance if you spam one spell repeatedly

**Exalted**
- Passive: access to forbidden glyph variants (server-approved list)
- Monument: “Ritual Rewrite” (reroll one spell modifier at a cost)
- Rival Pressure: Meridian causes “Arcane Audit” events that disrupt rituals in orderly zones

**Ascendant**
- Passive: “Spell Signature” effect (spells gain a unique flourish and stronger scaling)
- Monument: “Open the Veil” (short-lived zone where magic is amplified, but unstable)
- Rival Pressure: Mortyss spawns transient wraiths attracted to amplified casting

---

### 3.3 KHELR (God of War & Strife)
Theme: honorable risk, dominance, pressure

**Initiate**
- Passive: minor damage bonus while outnumbered; slight knockback resistance
- Monument: “War Vow” (select a target type; bonus drops for that target for 24h)
- Rival Pressure: Viren increases food exhaustion after prolonged fights

**Devout**
- Passive: rally aura for allies during combat; crit chance rises with danger
- Monument: “Blood Oath” (temporary combat boon with a cost if you retreat)
- Rival Pressure: Meridian penalizes you for attacking “protected” NPC zones

**Exalted**
- Passive: executes on low-health enemies (PvE configurable); reduced fear effects
- Monument: “Banner of Strife” (place a banner that buffs allies but draws raids)
- Rival Pressure: Viren triggers wildlife hostility in contested regions

**Ascendant**
- Passive: “Champion” state (combat-only) with dramatic scaling but high upkeep
- Monument: “Call the Trial” (summon a worthy challenge; winning grants major Favor)
- Rival Pressure: Meridian may impose “Sanctions” that disable trading while championed

---

### 3.4 VIREN (Goddess of Life & Nature)
Theme: renewal, balance, sustainable growth

**Initiate**
- Passive: minor regeneration; crop yield bonus (sustainable conditions only)
- Monument: “Green Oath” (blessing tied to replanting/animal welfare metrics)
- Rival Pressure: Khelr increases hostile mob spawns if you avoid conflict entirely

**Devout**
- Passive: disease/poison resistance; animals breed more reliably
- Monument: “Seasonal Rite” (choose Spring/Summer/Autumn/Winter buff cycle)
- Rival Pressure: Aurex reduces benefits if you industrialize terrain too aggressively

**Exalted**
- Passive: biome attunement (movement/regen benefits in natural biomes)
- Monument: “Warden’s Claim” (chunk aura protects flora, but demands stewardship)
- Rival Pressure: Mortyss causes blight waves if you hoard life without letting it end

**Ascendant**
- Passive: “Verdant Dominion” (large-scale growth aura; expensive upkeep)
- Monument: “Bloom Event” (world event improving ecosystems, but draws predators)
- Rival Pressure: Mortyss increases soul-loss penalties during bloom if neglected

---

### 3.5 MORTYSS (Goddess of Death & Transition)
Theme: endings, souls, balance, inevitability

**Initiate**
- Passive: reduced death penalty; minor grave recovery bonus
- Monument: “Ledger Mark” (record one death; later convert to soul resource)
- Rival Pressure: Viren causes healing reduction if you farm deaths

**Devout**
- Passive: corpse/grave tracking; partial item retention chances (configurable)
- Monument: “Rite of Passing” (stabilize a death zone to prevent hauntings)
- Rival Pressure: Nyxara increases backlash if you attempt immortality tricks

**Exalted**
- Passive: controlled undead utility (non-griefing limits); stronger soul recovery
- Monument: “Soul Audit” (reroll death penalties once per week at a cost)
- Rival Pressure: Viren causes plants to wither near your monument if you over-necromance

**Ascendant**
- Passive: “Finality” aura (reduces enemy healing/resurrection within a radius)
- Monument: “Open the Gate” (temporary access to afterlife mechanics or boss trials)
- Rival Pressure: Nyxara spawns arcane anomalies around the gate

---

### 3.6 CHRONYX (God of Time & Change)
Theme: momentum, pacing, causality

**Initiate**
- Passive: minor cooldown reduction; small XP gain window bonus
- Monument: “Time Contract” (take a timed challenge for Favor)
- Rival Pressure: Aurex increases item wear when you repeat identical routines

**Devout**
- Passive: periodic “rush” buff (short burst of speed/haste); reduced delay effects
- Monument: “Timeline Swap” (trade one benefit for another for 24h)
- Rival Pressure: Meridian increases debuff duration if you ignore civic systems

**Exalted**
- Passive: “Causality Edge” (buff triggers after taking damage, rewards reactive play)
- Monument: “Rewind Tax” (save a small fraction of lost XP/items on death, costly)
- Rival Pressure: Aurex weakens rewinds on crafted relics (anti-permanence)

**Ascendant**
- Passive: time-bending aura (strong but limited; heavy upkeep)
- Monument: “Temporal Storm” (world event that accelerates growth/decay/evolution)
- Rival Pressure: Meridian triggers civil disruption during storms

---

### 3.7 MERIDIAN (God of Order & Society)
Theme: civilization, law, infrastructure

**Initiate**
- Passive: better villager/colony reputation; minor trade discount
- Monument: “Civic Tithe” (convert resources into stability bonuses)
- Rival Pressure: Umbriel increases random events in your claimed chunks

**Devout**
- Passive: colony efficiency; raid frequency reduced
- Monument: “Edict” (choose one law-like buff for your zone, daily/weekly)
- Rival Pressure: Nyxara increases magic instability in orderly zones if you suppress it

**Exalted**
- Passive: large trade benefits; NPC loyalty/stability bonuses
- Monument: “Census” (see unrest/crime metrics; pay to correct them)
- Rival Pressure: Umbriel can trigger “Sabotage” events unless defenses are maintained

**Ascendant**
- Passive: “Imperial” aura (infrastructure buff, huge upkeep)
- Monument: “Decree of Silence” (temporarily suppress chaos events in a radius)
- Rival Pressure: Umbriel backlash after decree ends is brutal (configurable severity)

---

### 3.8 UMBRIEL (Goddess of Chaos & Freedom)
Theme: rebellion, chance, possibility

**Initiate**
- Passive: slight luck boost; occasional “break the rules” proc
- Monument: “The Gamble” (sacrifice valuables for a random boon)
- Rival Pressure: Meridian increases penalties if you disrupt order zones

**Devout**
- Passive: higher proc chance; chaos boons with small collateral
- Monument: “Wild Pact” (choose risk tier; higher tiers can backfire dramatically)
- Rival Pressure: Aurex injects defects into gear if you gamble with mass crafting

**Exalted**
- Passive: “Unshackled” state (rare proc that temporarily bypasses restrictions)
- Monument: “Ruin or Glory” (high-stakes ritual; can grant exalted-level boons instantly)
- Rival Pressure: Meridian triggers “Lockdown” events limiting movement/trade

**Ascendant**
- Passive: chaotic aura shifts local reality (powerful, unpredictable)
- Monument: “Break the Chain” (server event: frees one constraint, breaks another)
- Rival Pressure: Meridian reacts with “Purge” mechanics unless disabled by config

---

## 4) Apostasy: Switching Gods

Apostasy should exist, but feel like tearing your soul out with pliers.

### 4.1 The Apostasy Sequence (Suggested)
1. **Desecrate your monument** (special ritual item; cannot be accidental)
2. Survive **Apostasy Trials** (3 phases; timed; escalating penalties)
3. Become **Unbound** for a long duration (days/weeks)
4. Bind to a new god by creating a new monument (rune + armor stand)

### 4.2 Consequences (Recommended Default Set)

**Permanent Scars (choose one or more)**
- Max health reduction
- Mana pool reduction / increased mana costs
- Durability loss multiplier
- Reduced XP gain
- Increased death penalty severity
- Reduced luck baseline

**Immediate Penalties**
- Favor set to a deep negative value with the old god
- “Marked” status: rival gods more aggressive for a time
- Temporary inability to place monuments in claimed chunks

**Cooldowns**
- Apostasy lockout in **real-world days** (recommended: 14–30 days)
- Only one apostasy attempt per cooldown window

**World/Server-Visible Consequences**
- Monument becomes a **Ruined Idol** (warning marker)
- Apostate triggers periodic hostile events near their location
- Optional: server broadcast / lore log entry

### 4.3 Old God Never Forgives
Even after switching:
- The previous god may continue to apply a **latent curse** (configurable duration)
- Primary rival gods are “eager” to exploit your weakness

---

## 5) Config Schema Draft

This is a **draft** intended to map cleanly into Forge config (TOML) + data-driven JSON.

### 5.1 High-Level Layout
- `common.toml`: core rules (timers, lockouts, thresholds)
- `server.toml`: server policy (severity, pvp rules, protected zones)
- `data/spells_n_gods/gods/*.json`: per-god definitions (effects, offerings, rivals)
- `data/spells_n_gods/offering_rules/*.json`: offering validators and weights

---

### 5.2 Common Config (TOML Draft)
```toml
[divinity]
allow_apostasy = true
apostasy_cooldown_days = 21
unbound_duration_hours = 72
binding_requires_monument = true
one_god_per_player = true

[worship]
offering_grace_hours = 72
prayer_min_seconds = 10
prayer_cooldown_seconds = 300
favor_decay_per_day = 2.0
favor_decay_paused_while_praying = true

[favor_thresholds]
initiate = 10
devout = 35
exalted = 70
ascendant = 95

[rivalry]
primary_rival_pressure_multiplier = 1.0
secondary_rival_pressure_multiplier = 0.5
rival_pressure_enabled = true

[monument]
use_player_skin = true
offline_skin_fallback = "MHF_Steve"
chunk_anchor_radius = 1
allow_monument_breaking = false
allow_monument_moving = false
```
---

### 5.3 Per-God JSON Draft (Data Pack)
File: `data/spells_n_gods/gods/aurex.json`
```json
{
  "id": "aurex",
  "display_name": "Aurex",
  "gender": "male",
  "domain": ["creation", "form", "crafting"],
  "philosophy": "What is made must endure.",
  "rivals": {
    "primary": "umbriel",
    "secondary": "chronyx"
  },
  "blessings": {
    "initiate": {
      "effects": [
        {"type": "durability_multiplier", "value": 0.95},
        {"type": "crafting_waste_reduction", "value": 0.10}
      ],
      "monument_ability": {"type": "temper_item", "cooldown_hours": 24}
    },
    "devout": {
      "effects": [
        {"type": "flawless_craft_chance", "value": 0.05},
        {"type": "repair_cost_multiplier", "value": 0.85}
      ],
      "monument_ability": {"type": "seal_item", "cooldown_hours": 24}
    },
    "exalted": {
      "effects": [
        {"type": "bonus_modifier_slot", "value": 1}
      ],
      "monument_ability": {"type": "forge_oath", "cooldown_hours": 168}
    },
    "ascendant": {
      "effects": [
        {"type": "structure_resilience", "value": 0.25}
      ],
      "monument_ability": {"type": "anoint_workshop", "cooldown_hours": 168}
    }
  },
  "offering_rules": {
    "validator": "crafted_unused_high_quality",
    "diminishing_returns": true
  }
}
```
---

### 5.4 Offering Validator Registry (Draft)
Offerings should be validated by pluggable validators:
- `crafted_unused_high_quality`
- `mana_expenditure_or_scrolls`
- `combat_trophies_no_cheese`
- `sustainable_harvest_and_replant`
- `souls_graves_deathbound`
- `timed_objectives_only`
- `systemic_trade_and_tithe`
- `voluntary_risk_gambles`

Each validator can expose config:
- allowed items/tags
- required NBT conditions
- minimum rarity/value score
- anti-cheese rules

---

### 5.5 Apostasy Policy (TOML Draft)
```toml
[apostasy]
enabled = true
cooldown_days = 21
trial_phases = 3
unbound_duration_hours = 72

[apostasy.permanent_scars]
max_health_reduction = 2
mana_cost_multiplier = 1.10
durability_loss_multiplier = 1.15
xp_gain_multiplier = 0.90
luck_baseline_delta = -1

[apostasy.latent_curse]
enabled = true
duration_days = 14
severity = "medium"  # low | medium | high
```
---

## 6) Next Implementation Targets (Practical)
- Implement Favor + Blessing state machine (including HALTED).
- Monument block entity with skin rendering (or configurable statue model).
- Data-driven god registry + JSON parsing.
- Offering validators with anti-cheese hooks.
- Rival pressure event router (effects + world events).

---
