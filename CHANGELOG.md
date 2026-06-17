# Changelog

All notable changes to **Spells 'n Gods** are documented here.
The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and the project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.1] — 2026-06-17

A bugfix release: god bosses can now be spawned by name through the in-game commands.

### Fixed
- **God boss spawn commands now accept namespaced ids.** The `god_id` argument on
  `/spells_n_gods boss spawn`, `boss kill`, `boss locate`, `boss info`, and `setrune` used an
  unquoted string parser that stopped at the `:` character, so the fully-namespaced ids offered by
  tab-completion (e.g. `spells_n_gods:bella`) failed with *"Expected whitespace to end one
  argument, but found trailing data"*. These arguments now read the full id, so both bare (`bella`)
  and namespaced (`spells_n_gods:bella`) forms work, including tab-completed suggestions.

### Changed
- **Command name corrected in docs and messages.** The command root is `/spells_n_gods`
  (with underscores); stale `/spellsngods` references in the README, an in-game hint, and log
  lines were corrected. Clarified in the README that gods are variants of a single `god_boss`
  entity, so `/summon spells_n_gods:bella` does not work — use `/spells_n_gods boss spawn bella`.

## [1.0.0] — 2026-06-17

First stable, release-ready build. This release completes the core worship loop end-to-end,
finishes several systems that were scaffolded but unwired, and hardens the mod for dedicated
servers. See `SPELLS_N_GODS_COMPLETION_AUDIT.md` for the full audit and remediation record.

### Added
- **Themed offerings.** Right-click a Monument while holding an item to offer it; each god accepts
  datapack-defined item tags via per-god validators. Ships 18 themed item tags
  (`fire_themed`, `smelted_goods`, `holy_themed`, `golden_goods`, … `alchemical`), all overridable.
- **Durability modifier system.** Apostasy scars make a player's gear wear faster and Aurex/Permanence
  blessings make it last longer, applied through a minimal, well-contained `ItemStack` Mixin with all
  logic in an internal, unit-tested service (probabilistic rounding so fractional factors matter on a
  1-point hit). Server-side only; respects creative, unbreakable, empty and non-durable items.
- **Latent-curse damage amplification** — cursed apostates take extra damage (from the abandoned
  god's `damage_multiplier`).
- **Scar death penalty** — scars now amplify XP lost on death.
- **`CHANGELOG.md`** and **`CURSEFORGE_DESCRIPTION.md`**.

### Fixed
- **Blessings now actually apply.** Fixed JSON↔parser key mismatches that silently neutralized every
  blessing: attribute magnitude (`amount`), potion id (`effect_id`), and regen rate
  (`heal_per_second`) are now read correctly.
- **Crash on effect computation removed.** `conditional_combat` conditions used by gods
  (`on_fire_target`, `melee_hit`, `sprinting`, `animal_target`, `undead_target`, `enderman_nearby`,
  `wither_target`) are now implemented and parsed safely instead of throwing.
- **Tier-ups apply immediately.** Praying now advances the tier and refreshes blessings on the spot
  (previously prayer never tiered the player up and offerings only applied after a relog).
- **Offering item-gating works** — validators read `accept_tags`, and the previously-missing themed
  tag files exist, so off-theme items are correctly rejected.
- **Dedicated-server crashes fixed:**
  - Player-animation packet guarded with `DistExecutor` (was a hard `NoClassDefFoundError`).
  - Client-only mod-bus listeners (particle providers, client setup) guarded behind `Dist.CLIENT`.
  - Rune scar tooltip moved to a client-only class (was force-loading `LocalPlayer` on servers).
- God boss re-applies enrage buffs when reloaded already enraged (no longer loses them on restart).
- Boss shield no longer accumulates unbounded absorption over a fight.
- Glacia's frost-on-hit now actually places frosted ice / snow (previous block test was contradictory).
- `DivineCrossbow` no longer fires its loaded bolt when its ability is on cooldown.
- `SpellsNGodsAPI.hasTierOrHigher` no longer throws on unknown tier names from scripts.
- `structure protect on/off` command now actually toggles the config value.
- Added missing lang keys (rune scar line, monument-creation error, tier-up message) and sound subtitles.

### Changed
- Version bumped to **1.0.0** to mark the first release-ready, dedicated-server-safe build.
- Build now wires SpongePowered Mixin (mixingradle) with refmap generation for production parity.

## [0.3.0] — Initial development baseline
- Initial datapack-driven pantheon: 9 gods, GeckoLib boss guardians, divine temples, weapons and
  relics, favor/blessing tiers, rivalry, apostasy scaffolding, and the data-driven deity
  structure-spawning system.
