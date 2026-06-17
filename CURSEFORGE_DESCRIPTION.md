# Spells 'n Gods

**A fully modular pantheon, boss, and divine-progression system for Minecraft 1.20.1 (Forge).**

Spells 'n Gods is an add-on for **Iron's Spells 'n Spellbooks** that turns the overworld into a
contested theatre of nine living gods. Find a deity's temple, swear yourself to a patron, earn favor
through prayer and offerings, climb four tiers of blessings — and pay a real price if you ever turn
your back on your god. Every god, blessing, offering, rival event, and temple is **datapack-driven**,
so modpack authors can reshape the entire pantheon without touching code.

---

## ✦ Bind to a God

Bind a **Rune** to an armor stand to raise your patron's **Monument**. From there you grow in favor:

- **Pray** at the Monument (empty hand) to channel devotion over time.
- **Offer** themed items (hold an item and interact) — each god accepts its own kinds of tribute.

Cross the favor thresholds and ascend through **Initiate → Devout → Exalted → Ascendant**, unlocking
that god's blessings the instant you tier up — attribute boosts, potion effects, conditional combat
power that scales with the foe you face, regeneration, luck, and more.

## ✦ Nine Gods, Nine Guardians

Each god is themed to a magic school and guards a hand-built temple with a **GeckoLib-animated boss**
— a towering guardian with a phase system (Idle → Combat → Enraged), arena-leash AI, and a spell pool
powered by Iron's Spells. These guardians **hunt you through the terrain**: they know exactly where you
are within their awareness range and **smash through blocks** like the Wither or Ender Dragon to reach
you, escalating to an area-breaking rampage if you try to wall yourself off — but they can never harm
bedrock or their own protected temple. Defeat one to claim its **divine weapon** and **runic
fragments**. Temples are indestructible (block-break, explosion, and piston protected) and bosses
respawn on configurable timers.

## ✦ Faith Has Consequences

Abandon your god (**apostasy**) and suffer for it: a **latent curse** that amplifies the damage you
take, and **permanent scars** that chip away at your max health, luck, XP gains, and now make your
gear wear out faster. Stay faithful and the right blessings can make your equipment nearly
**unbreakable** instead. Opposing gods exert **rivalry pressure** on each other's followers through
periodic divine events.

## ✦ Built for Modpacks

- **Datapack-everything** — gods, blessings, offerings (item tags + validators), rival events, loot,
  temples, and structure spawning are all JSON.
- **Optional integrations** with Iron's Spells 'n Spellbooks and Simply Swords, with graceful vanilla
  fallback when they're absent.
- **Dedicated-server safe** — strict client/server separation, verified to boot cleanly on servers.
- **Extensive config** for boss stats, structure generation, loot, worship pacing, schematics, and debug.

---

## Requirements

- **Minecraft** 1.20.1
- **Forge** 47.2.0+
- **GeckoLib** 4.x (required)
- **Iron's Spells 'n Spellbooks** (recommended — boss spellcasting)

## Optional

Curios, JEI, Jade, Simply Swords, Apotheosis, Origins, KubeJS, FTB Quests, PlayerAnimator — all
detected at runtime and used if present, never required.

---

*Spells 'n Gods is open to datapack extension — drop your own gods, offerings, and events into a
datapack and they load automatically. See the README for the full schema reference.*
