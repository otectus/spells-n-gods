# Spells n' Gods (Forge 1.20.1)

Spells n' Gods is an add-on for Iron's Spells n' Spellbooks which adds a fully modular pantheon and boss system which can be customized entirely via datapack. Players discover divine temples in the overworld, bind themselves to a patron deity, earn favor through prayer and offerings, unlock tiered blessings and face consequences for apostasy. Each god has a unique animated boss guardian, themed loot and rivalry mechanics.

## The Pantheon

| God | Domain | Magic School | Rivals |
|-----|--------|-------------|--------|
| Deus | Holy | Holy | Magnus, Celia |
| Velox | Lightning | Lightning | Glacia, Venatas |
| Celia | Blood | Blood | Deus, Bricoleur |
| Bella | Fire | Fire | Glacia, Ingenium |
| Bricoleur | Evocation | Evocation | Venatas, Celia |
| Ingenium | Ender | Ender | Venatas, Bella |
| Venatas | Nature | Nature | Ingenium, Bricoleur |
| Magnus | Eldritch | Eldritch | Deus, Velox |
| Glacia | Ice | Ice | Bella, Velox |

Gods are fully data-driven via JSON. Add, replace, or modify gods through datapacks.

### Divine Relics

Each god has a unique craftable relic item that reveals the location of their temple when used:

| God | Relic | Crafting Core | Description |
|-----|-------|--------------|-------------|
| Deus | Sunward Relic | Glowstone Dust | A warm golden relic that hums with radiant faith |
| Velox | Stormcall Shard | Copper Ingot | A crackling shard of crystallized lightning |
| Celia | Sanguine Pendulum | Redstone | A pendulum of dark glass pulled by the scent of old blood |
| Bella | Conquest Cinder | Blaze Powder | A smouldering coal that refuses to die |
| Bricoleur | Prophet's Eye | Prismarine Shard | An unblinking eye of pale stone |
| Ingenium | Void Lens | Ender Pearl | A lens ground from the edge of nothing |
| Venatas | Hunter's Fang | Oak Sapling | A yellowed fang that trembles in the hand |
| Magnus | Eldritch Lodestone | Amethyst Shard | A lodestone drawn toward forbidden knowing |
| Glacia | Frozen Tear | Packed Ice | A teardrop of eternal ice that will not melt |

All relics are crafted with 4 Gold Ingots surrounding a god-specific core ingredient.

### Divine Weapons

Each god's boss drops a unique divine weapon with a right-click ability (Shift+Right-click for ranged) and passive effects. Ability cooldowns are tracked per-weapon via NBT and do not interfere with normal weapon use.

| God | Weapon | Ability | Cooldown | Passive |
|-----|--------|---------|----------|---------|
| Deus | Warhammer of Creation | Divine Mandate — AoE shockwave + Glowing | 12s | +3 damage to undead |
| Velox | Bow of Agility | Stormstrike — instant lightning arrow | 12s | No-gravity arrows, Speed I while held |
| Celia | Philosopher's Dagger | Sanguine Pact — sacrifice HP for lifesteal | 10s | Heal 1 heart per kill |
| Bella | Warmonger's Sword | Inferno Charge — dash forward, ignite path | 15s | Set targets on fire |
| Bricoleur | Peacemaking Staff | Prophet's Judgment — summon 3 vex allies | 20s | Nearby hostiles get Glowing |
| Ingenium | Void Orb | Rift Walk — teleport + void pulse | 12s | — |
| Venatas | Crossbow of the Wild | Ensnaring Shot — root + poison bolt | 16s | 25% bonus loot chance on kills |
| Magnus | Book of Magery | Eldritch Barrage — 5 homing magic bolts | 15s | XP on kill |
| Glacia | Glacial Battleaxe | Absolute Zero — frost nova + freeze | 18s | Slowness I on hit |

## Features

- **9 gods** with unique worship mechanics, blessings, rivalries, and apostasy consequences
- **GeckoLib-animated boss entities** (3 blocks tall) that guard one-per-world temples
- **Boss phase system** (Idle / Combat / Enraged) with configurable health thresholds
- **Arena leash AI** that keeps bosses tethered to their temple
- **Siege AI** — bosses track the player through walls within their awareness range and break through blocks (Wither/Ender-Dragon style) to reach them, escalating to an area smash if walled out; bedrock, block entities, temples, and the `#spells_n_gods:boss_unbreakable` tag are always spared (`[boss_siege]` config)
- **Indestructible temple structures** with block-break, explosion, and piston protection
- **Custom schematic support** for modpack creators to override temple generation with `.nbt` files
- **Boss respawn system** with configurable timers and chunk-aware deferred spawning
- **9 divine weapons** (6 melee + 2 ranged + 1 caster) with unique right-click abilities and passive on-hit effects
- **9 god-specific divine relics** (craftable), each themed to its deity, that reveal the location of that god's temple
- **Runic Fragment drops** with per-god NBT sourcing and enchant glint
- **4-tier blessing system** (Initiate / Devout / Exalted / Ascendant) with unique effects per god, applied the instant you tier up
- **Themed offerings** — each god accepts datapack-defined item tags, gated by per-god validators
- **Favor decay and rivalry pressure** between opposing gods
- **Apostasy system** with latent curses (amplified damage), cumulative scars (reduced health/luck/XP, faster gear wear), and multi-phase trials
- **Durability modifiers** — apostasy scars wear gear faster, Aurex/Permanence blessings make it last longer (server-side, deterministic, applied via a contained Mixin)
- **Per-god loot tables** with themed vanilla drops
- **Optional mod integration** with Iron's Spells n' Spellbooks and SimplySwords (graceful fallback to vanilla when absent)
- **Integrator events** (Forge event bus) for KubeJS/FTB Quests: player bound, tier change, prayer complete, offering (cancelable), boss spawned/defeated, apostasy, and curse expired
- **Dedicated-server safe** — strict client/server separation, no client classes loaded on servers
- **Extensive config** (common + server + shrine) covering boss stats, structure generation, loot, locator, worship, schematics, and debug options

## Worship & Progression

Bind a **Rune** to an armor stand to raise your god's **Monument**, then earn *favor* at it:

- **Pray** — right-click the Monument **with an empty hand** to channel a timed prayer (stand
  still and stay close). Each completed prayer grants favor.
- **Offer** — right-click the Monument **while holding an item**. If the item matches your god's
  themed offering (see below), it is consumed and grants favor scaled by rarity, enchantments and
  stack size; otherwise you are told why it was rejected and nothing is consumed.

Favor crosses four **tier thresholds** (Initiate → Devout → Exalted → Ascendant), each unlocking
the blessings authored in that god's datapack file. Blessings apply **immediately** on tier-up.
Neglecting a god past its grace window *halts* blessings until you next make an offering; abandoning
a god (**apostasy**) inflicts a latent curse and permanent scars.

### Offering themes

Each god accepts items via item tags under `data/spells_n_gods/tags/items/`. The themed tags ship
with sensible vanilla defaults and are fully datapack-overridable:

| God theme | Accepts (tags) |
|-----------|----------------|
| Fire | `fire_themed`, `smelted_goods` |
| Ice | `ice_themed`, `frozen_goods` |
| Holy | `holy_themed`, `golden_goods` |
| Nature | `nature_themed`, `harvested_goods` |
| Lightning | `lightning_themed`, `storm_goods` |
| Ender | `ender_themed`, `void_touched` |
| Eldritch | `eldritch_themed`, `arcane_artifacts` |
| Evocation | `evocation_themed`, `mechanisms` |
| Blood | `blood_themed`, `alchemical` (also requires a recent kill) |

## Requirements

- Minecraft 1.20.1
- Forge 47.4.10+ (47.x)
- GeckoLib 4.7.x (required)
- Java 17

### Optional Integrations

The mod detects and integrates with the following if present:
- Iron's Spells n' Spellbooks (boss spellcasting)
- SimplySwords (boss weapons)
- Jade/WAILA (monument tooltips)
- Curios (trinket slots)
- JEI (recipe viewing)
- KubeJS (scripting bindings)
- FTB Quests (quest hooks)

## Build

```
./gradlew build
```

Output JAR: `build/libs/spells_n_gods-<version>.jar`

> **Note:** If `JAVA_HOME` points to a non-JDK-17 installation, the build uses `org.gradle.java.home` from `gradle.properties` to target JDK 17.

## Configuration

All config files live under `config/spells_n_gods/`:

```
config/spells_n_gods/
  spells_n_gods-common.toml   - Main mod config (divinity, worship, boss, structure, loot, etc.)
  spells_n_gods-server.toml   - Server-side config (UI, apostasy trials)
  shrines.toml                - Shrine/schematic config (custom schematics toggle, fallback behavior)
  schematics/                 - Drop custom .nbt structure files here (see below)
```

### Custom Schematics

Modpack creators can replace the procedural temple generation with custom `.nbt` structure files:

1. Set `useCustomSchematics = true` in `config/spells_n_gods/shrines.toml`
2. Place `.nbt` files in `config/spells_n_gods/schematics/` using the naming convention `<god_id>.nbt` (e.g., `deus.nbt`, `velox.nbt`)
3. For datapack-added gods with a custom namespace, use `<namespace>_<id>.nbt` (e.g., `mygods_zeus.nbt` for `mygods:zeus`)
4. If `fallbackToProcedural = true` (default), gods without a matching schematic will still generate procedurally

Schematics are loaded on server start and cached in memory. The structure is centered on the chosen world position at floor level.

## Project Layout

```
src/main/java/com/otectus/spells_n_gods/
  apostasy/       - Apostasy handler, latent curses, scar effects, trials
  binding/        - God binding logic
  boss/           - GodBossEntity, GeckoLib model/renderer, AI goals, phase system
  capability/     - PlayerDivinityCapability, blessing state, scar data
  client/         - Client-side rendering and UI
  command/        - /spells_n_gods command tree
  compat/         - Optional mod integration (Iron's Spells, SimplySwords, Jade)
  config/         - ForgeConfigSpec (common + server + shrine)
  content/        - Blocks (MonumentBlock, RuinedIdolBlock, RuneItem)
  data/           - GodDefinition records, SpellsNGodsDataManager (JSON reload)
  effect/         - Custom mob effects
  favor/          - Favor calculation and tier progression
  item/           - Divine weapons, GodLocatorItem (9 relics), RunicFragmentItem
  network/        - SimpleChannel packets
  offering/       - Offering processing
  prayer/         - Prayer mechanics
  registry/       - DeferredRegister for entities, items, blocks, structures
  rivalry/        - God rivalry pressure system
  structure/      - GodTempleStructure, GodTempleStructurePiece, SchematicLoader
  worldstate/     - GodWorldState (SavedData), StructureRecord, respawn tick handler

src/main/resources/
  data/spells_n_gods/spells_n_gods/gods/        - 9 god definition JSONs (schema 2)
  data/spells_n_gods/spells_n_gods/monuments/   - 9 monument palette JSONs
  data/spells_n_gods/worldgen/structure/        - 9 structure definitions
  data/spells_n_gods/worldgen/structure_set/    - Structure placement config
  data/spells_n_gods/loot_tables/entities/      - 9 boss loot tables
  data/spells_n_gods/recipes/                   - 9 divine relic crafting recipes
  data/spells_n_gods/tags/                      - Biome tags for structure generation
  assets/spells_n_gods/geo/                     - GeckoLib boss geometry
  assets/spells_n_gods/animations/              - GeckoLib boss animations (idle, walk, attack, cast, death)
  assets/spells_n_gods/models/item/             - Item models (9 relics, rune, runic_fragment, monument, ruined_idol)
  assets/spells_n_gods/textures/entity/         - Boss skin textures (boss_<god>.png, 64x64)
```

## Commands

All commands are under the `/spells_n_gods` root and require permission level 2.
A god id may be given bare (`bella`) or fully namespaced (`spells_n_gods:bella`) —
for example, `/spells_n_gods boss spawn bella`. Note that gods are variants of a
single `god_boss` entity, so `/summon spells_n_gods:bella` does **not** work; use the
`boss spawn` command below.

| Command | Description |
|---------|-------------|
| `listgods` | List all loaded gods |
| `setrune <god_id>` | Give yourself a rune for a god |
| `getstatus` | Show your divine status, favor, and tier |
| `setfavor <amount>` | Set your favor value |
| `unbind` | Remove your god binding |
| `setstate <state>` | Set blessing state (ACTIVE, HALTED, etc.) |
| `refreshoffering` | Reset offering cooldown timer |
| `apostasy` | Force apostasy from your current god |
| `clearcurse` | Remove active latent curse |
| `clearscars` | Remove all apostasy scars |
| `scarstatus` | Show scar count and cumulative penalties |
| `fullreset` | Reset all divine data including scars |
| `boss spawn <god_id>` | Spawn a god boss at your location |
| `boss kill <god_id>` | Kill a god's boss |
| `boss locate <god_id>` | Show a god's temple coordinates |
| `boss info <god_id>` | Show boss alive status and respawn timer |
| `boss respawnall` | Force respawn all dead bosses |
| `structure list` | List all placed temple locations |
| `structure protect on/off` | Toggle structure protection at runtime |

## Datapack Extensibility

Gods are loaded from `data/<namespace>/spells_n_gods/gods/<god_id>.json` (schema version 2). Each god JSON defines:

- Display info (name, title, gender, domains, magic school)
- Binding config (rune materials, monument materials)
- Favor thresholds and decay rates
- Worship mechanics (prayer cooldowns, offering items/values)
- 4 blessing tiers with effects and monument abilities
- Rival pressure (primary/secondary rivals with effect penalties)
- Apostasy consequences (latent curse, scar modifiers, 3-phase trials)
- Boss config (HP, armor, damage, speed, weapon, spell pool, phases, respawn delay)
- Structure config (template ID, biome tag, size)

To add a custom god, create a datapack with your JSON at the path above. The mod will merge it with the default pantheon on reload.

Custom gods added via datapack can also have custom temple schematics by placing a `.nbt` file in `config/spells_n_gods/schematics/` with the appropriate naming convention (see [Custom Schematics](#custom-schematics) above).
