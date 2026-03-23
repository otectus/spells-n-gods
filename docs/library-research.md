# Forge 1.20.1 Library & API Research for Ote's Gods

Research compiled 2026-03-17. All libraries listed have confirmed Forge 1.20.1 support.

---

## Already In Use / Planned
- **GeckoLib 4.x** - Entity animations (in build.gradle)
- **Curios API** - Accessory slots (commented out, repo issue)
- **JEI** - Recipe viewer (commented out, repo issue)
- **Iron's Spells 'n Spellbooks** - Spell system compat (commented out, repo issue)
- **Jade** - Tooltip overlay (commented out, repo issue)
- **SimplySwords** - Weapon compat (commented out, repo issue)

---

## HIGH PRIORITY - Strongly Recommended

### 1. Patchouli (In-Game Guidebook)
**What it does:** Data-driven in-game documentation books. Supports formatted text, images, crafting recipes, multiblock structure visualization, and advancement-gated category trees.

**Why for this mod:** Perfect for a "Divine Codex" or "Book of the Gods" that unlocks pages as players progress through tiers (INITIATE -> ASCENDANT). Can document god lore, prayer rituals, offering recipes, monument construction (multiblock preview), and scar effects. Pages can unlock via advancements matching your tier system.

**Maven:**
```gradle
// Repository (already in build.gradle)
maven { url = "https://maven.blamejared.com" }

// Dependencies
compileOnly fg.deobf("vazkii.patchouli:Patchouli:1.20.1-84-FORGE:api")
runtimeOnly fg.deobf("vazkii.patchouli:Patchouli:1.20.1-84-FORGE")
```

**Status:** Actively maintained. Latest 1.20.1 build is 84.1-FORGE.

---

### 2. Apothic Attributes (formerly AttributesLib)
**What it does:** Library for custom entity attributes with utilities for registration, debugging, and interaction. Adds several new attribute types and an attribute debugging screen.

**Why for this mod:** Your tier progression (NONE -> ASCENDANT) could grant divine attributes (e.g., "Divine Favor", "Prayer Power", "Holy Resistance"). Provides a clean API for adding, modifying, and displaying custom attributes on entities. Works well for god-granted buffs and boss stat scaling.

**Maven:**
```gradle
maven { url = "https://maven.shadowsoffire.dev/releases" }

implementation fg.deobf("dev.shadowsoffire:Placebo:1.20.1-8.6.0")       // Required dependency
implementation fg.deobf("dev.shadowsoffire:ApothicAttributes:1.20.1-1.3.7")
```

**Status:** Actively maintained by Shadows_of_Fire. Placebo is a required dependency.

---

### 3. playerAnimator (KosmX)
**What it does:** Player animation library for Minecraft 1.16+. Allows custom player animations (prayer poses, ritual gestures, casting stances). Supports GeckoLib animation format.

**Why for this mod:** Animate the player during prayer (kneeling), worship rituals (raised arms), apostasy effects, and divine ability casting. Complements your existing GeckoLib boss animations with player-side animations.

**Maven:**
```gradle
maven { url = "https://maven.kosmx.dev/" }

implementation fg.deobf("dev.kosmx.player-anim:player-animation-lib-forge:1.0.2-rc1+1.20")
```

**Note:** Use JarJar to bundle, do NOT shadow. Cannot be loaded multiple times.

**Status:** Actively maintained. Has a Forge example project at github.com/KosmX/forgePlayerAnimatorExample.

---

## MEDIUM PRIORITY - Valuable Additions

### 4. Bookshelf (Darkhax)
**What it does:** Utility library adding hooks, events, and helpers for common modding tasks: loot modification, villager trades, enchantment helpers, recipe types, entity helpers, and more.

**Why for this mod:** Simplifies many boilerplate tasks. Useful for custom enchantments (divine weapon enchants), loot table modification (god-themed loot), and villager trade helpers (priest/temple NPCs). Reduces code you have to maintain.

**Maven:**
```gradle
// Repository (already in build.gradle)
maven { url = "https://maven.blamejared.com" }

implementation fg.deobf("net.darkhax.bookshelf:bookshelf-forge-1.20.1:20.2.13")
```

**Status:** Actively maintained.

---

### 5. Flywheel (Rendering Engine)
**What it does:** Modern GPU-accelerated rendering engine for Minecraft mods. Provides instanced rendering for large numbers of animated objects, custom render types, and efficient batch rendering.

**Why for this mod:** If you plan to render many divine particle effects, animated monument blocks, or complex boss fight visuals simultaneously, Flywheel can handle the GPU side efficiently. Used by Create mod for its rendering.

**Maven:**
```gradle
maven { url = "https://maven.createmod.net" }

compileOnly fg.deobf("dev.engine-room.flywheel:flywheel-forge-api-1.20.1:1.0.0-beta")
runtimeOnly fg.deobf("dev.engine-room.flywheel:flywheel-forge-1.20.1:1.0.0-beta")
```

**Note:** Intended to be bundled within mods that need it (>= 0.6.9). Heavy dependency -- only worthwhile if you have performance-critical rendering needs.

**Status:** Actively maintained by Engine Room team.

---

### 6. Registrate (Registration Helper)
**What it does:** Fluent API for registering blocks, items, entities, tile entities, etc. with automatic data generation (models, blockstates, loot tables, recipes, lang entries).

**Why for this mod:** If you have many gods, each with unique items (relics, offerings, prayer beads), blocks (monuments, altars), and entities (divine servants, boss variants), Registrate dramatically reduces boilerplate. Auto-generates data files.

**Maven:**
```gradle
maven { url = "https://maven.tterrag.com/" }

implementation fg.deobf("com.tterrag.registrate:Registrate:MC1.20-1.3.11")
jarJar(group: 'com.tterrag.registrate', name: 'Registrate', version: '[MC1.20-1.3.11,MC1.20-1.3.12)')
```

**Note:** Produces a `-all` jar that bundles Registrate. Major refactor if adopted mid-project.

**Status:** Actively maintained.

---

### 7. DragNSounds API (Server-Side Custom Audio)
**What it does:** Upload and play custom sound files from the server without requiring client resource packs. Uses Minecraft's native sound engine.

**Why for this mod:** Play unique divine chants, prayer ambient sounds, boss fight music, and god-specific audio themes without requiring players to install a resource pack. Each god could have a unique musical identity.

**Maven:** Available on Modrinth; use Modrinth Maven:
```gradle
maven { url = "https://api.modrinth.com/maven" }

modImplementation "maven.modrinth:dragnsounds-api:<version>"
```

**Note:** Check Modrinth for exact Forge 1.20.1 version string. Supports Forge, Fabric, NeoForge.

**Status:** Active development.

---

### 8. FTB Quests (Quest System - Integration Target)
**What it does:** Full quest/progression system with custom tasks, rewards, quest trees, and a GUI quest book. Widely used in modpacks.

**Why for this mod:** Rather than building your own quest system, expose hooks that FTB Quests can use. Your god tier progression, prayer milestones, and boss kills can serve as FTB Quest completion triggers. This makes your mod modpack-friendly.

**Maven (Curse Maven):**
```gradle
// Curse Maven (already in build.gradle repos)
maven { url = "https://www.cursemaven.com" }

compileOnly fg.deobf("curse.maven:ftb-quests-forge-289412:4297999")
```

**Note:** Requires FTB Library and FTB Teams as transitive deps. Best used as a soft dependency with `ModList.isLoaded()` guards (same pattern you already use).

**Status:** Actively maintained by FTB team.

---

## LOWER PRIORITY - Situationally Useful

### 9. Architectury API (Cross-Loader Abstraction)
**What it does:** Abstraction layer for writing mods that work on both Forge and Fabric. Provides 90+ event hooks, networking, registry, and platform abstractions.

**Why for this mod:** Only relevant if you ever plan to port to Fabric/NeoForge. Adds complexity if staying Forge-only. Mentioned for completeness.

**Maven (Curse Maven):**
```gradle
compileOnly fg.deobf("curse.maven:architectury-api-419699:5137938")
```

**Status:** Actively maintained.

---

### 10. Caelus API (Elytra Flight Mechanics)
**What it does:** Provides an entity attribute for elytra-like flight, allowing custom items to grant flight.

**Why for this mod:** If ASCENDANT tier or certain gods grant divine flight (angel wings, levitation), Caelus provides the attribute-based flight system. Works with Curios for wing accessories.

**Maven:**
```gradle
// Repository (already in build.gradle)
maven { url = "https://maven.theillusivec4.top/" }

compileOnly fg.deobf("top.theillusivec4.caelus:caelus-forge:3.2.0+1.20.1:api")
runtimeOnly fg.deobf("top.theillusivec4.caelus:caelus-forge:3.2.0+1.20.1")
```

**Status:** Actively maintained (same author as Curios).

---

## RENDERING & VFX - Built-in Forge Capabilities (No Extra Deps)

These don't require external libraries but are worth noting as Forge-native systems you can leverage:

### Custom RenderTypes & Shaders
Forge 1.20.1 supports custom `RenderType` registration via `RegisterNamedRenderTypesEvent`. You can write custom GLSL vertex/fragment shaders and bind them through `ShaderStateShard`. This is how you'd implement:
- God-specific glow effects on monuments
- Custom translucency for divine entities
- Screen-space effects during boss fights (vignette, chromatic aberration)

Reference: [Forge RenderType docs](https://docs.minecraftforge.net/en/latest/rendering/modelextensions/rendertypes/) and [gigaherz's shader howto gist](https://gist.github.com/gigaherz/b8756ff463541f07a644ef8f14cb10f5).

### Forge Biome Modifiers (Custom World Gen)
Forge 1.20.1 has built-in JSON-driven biome modifiers for adding features, structures, and spawns to existing biomes. No library needed -- use datapack JSON. Relevant for:
- Divine realm dimensions
- God-themed biome features (holy groves, cursed wastelands)
- Structure generation (temples, shrines)

Reference: [Forge docs on biome modifiers](https://docs.minecraftforge.net/en/1.20.1/concepts/registries/)

---

## Summary: Recommended build.gradle Additions

If you were to add the highest-value libraries, here's what the repositories and dependencies sections would look like:

```gradle
repositories {
    mavenCentral()
    maven { url = "https://maven.blamejared.com" }           // JEI, Patchouli, Bookshelf
    maven { url = "https://www.cursemaven.com" }              // CurseForge mods
    maven { url = "https://maven.theillusivec4.top/" }        // Curios, Caelus
    maven { url = "https://modmaven.dev" }                    // Various mods
    maven { url = "https://dl.cloudsmith.io/public/geckolib3/geckolib/maven/" }  // GeckoLib
    maven { url = "https://maven.kosmx.dev/" }                // playerAnimator
    maven { url = "https://maven.shadowsoffire.dev/releases" } // Apothic Attributes, Placebo
    maven {
        name = "Iron's Spells Maven"
        url = "https://code.redspace.io/releases"
    }
}

dependencies {
    minecraft "net.minecraftforge:forge:${minecraft_version}-${forge_version}"

    // === REQUIRED ===
    implementation fg.deobf("software.bernie.geckolib:geckolib-forge-1.20.1:4.8.3")

    // === RECOMMENDED NEW ADDITIONS ===
    // Patchouli - In-game guidebook for god lore, rituals, monument building
    compileOnly fg.deobf("vazkii.patchouli:Patchouli:1.20.1-84-FORGE:api")
    runtimeOnly fg.deobf("vazkii.patchouli:Patchouli:1.20.1-84-FORGE")

    // Apothic Attributes - Custom divine attributes for tier progression
    // compileOnly fg.deobf("dev.shadowsoffire:Placebo:1.20.1-8.6.0")
    // compileOnly fg.deobf("dev.shadowsoffire:ApothicAttributes:1.20.1-1.3.7")

    // playerAnimator - Prayer/worship/casting player animations
    // compileOnly fg.deobf("dev.kosmx.player-anim:player-animation-lib-forge:1.0.2-rc1+1.20")

    // === OPTIONAL INTEGRATIONS (soft deps with ModList.isLoaded guards) ===
    // compileOnly fg.deobf("top.theillusivec4.curios:curios-forge:5.4.7+1.20.1")
    // compileOnly fg.deobf("mezz.jei:jei-1.20.1-forge:15.2.0.27")
    // compileOnly fg.deobf("io.redspace.ironsspellbooks:irons_spellbooks-forge-1.20.1:3.1.5")
}
```

---

## Decision Matrix

| Library | Value for Gods Mod | Integration Effort | Required Dep? | Recommendation |
|---------|-------------------|-------------------|---------------|----------------|
| Patchouli | Very High (lore, guidebook) | Low (data-driven JSON) | Optional | Add now |
| Apothic Attributes | High (divine stats) | Medium | Optional | Add when implementing stat system |
| playerAnimator | High (prayer animations) | Medium | Optional | Add when implementing rituals |
| Bookshelf | Medium (utilities) | Low | Required | Add if you need its helpers |
| Flywheel | Medium (VFX perf) | High | Required | Add only if VFX perf is a problem |
| DragNSounds | Medium (custom audio) | Low | Required | Add if custom music is planned |
| FTB Quests | Medium (modpack compat) | Low (soft dep) | Optional | Add for modpack friendliness |
| Registrate | Medium (boilerplate) | Very High (refactor) | Bundled | Skip -- too disruptive mid-project |
| Caelus | Low-Medium (flight) | Low | Optional | Add if divine flight is a feature |
| Architectury | Low (cross-loader) | Very High | Required | Skip unless porting to Fabric |
