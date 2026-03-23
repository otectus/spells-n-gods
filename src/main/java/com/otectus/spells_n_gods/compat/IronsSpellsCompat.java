package com.otectus.spells_n_gods.compat;

import com.otectus.spells_n_gods.SpellsNGodsMod;
import com.otectus.spells_n_gods.boss.GodBossEntity;
import com.otectus.spells_n_gods.data.GodDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Isolated compatibility class for Iron's Spells n' Spellbooks.
 * Uses reflection to access the API at runtime without compile-time dependencies.
 * This class is ONLY referenced when ModCompatHandler.IRONS_SPELLS_LOADED is true.
 *
 * Iron's Spells 3.x API:
 *   SpellRegistry.getSpell(ResourceLocation) → AbstractSpell (returns none() sentinel, NOT null)
 *   SpellRegistry.none() → AbstractSpell sentinel for "no spell"
 *   AbstractSpell.onCast(Level, int, LivingEntity, CastSource, MagicData)
 *   MagicData.getPlayerMagicData(LivingEntity) → works for any entity
 *   CastSource.MOB → enum for mob-origin casting
 */
public class IronsSpellsCompat {

    // Cached reflection objects
    private static boolean reflectionInitialized = false;
    private static boolean reflectionFailed = false;

    private static Method getSpellMethod;       // SpellRegistry.getSpell(ResourceLocation)
    private static Method noneMethod;           // SpellRegistry.none() → sentinel
    private static Object noneSentinel;         // cached none() result for fast == comparison
    private static Method castMethod;           // AbstractSpell.onCast(Level, int, LivingEntity, CastSource, MagicData)
    private static Method getMagicDataMethod;   // MagicData.getPlayerMagicData(LivingEntity)
    private static Object castSourceMob;        // CastSource.MOB enum constant
    private static String castMethodStyle = "";

    private static void initReflection() {
        if (reflectionInitialized) return;
        reflectionInitialized = true;

        try {
            // ── Core API classes ──
            Class<?> spellRegistryClass = Class.forName("io.redspace.ironsspellbooks.api.registry.SpellRegistry");
            Class<?> abstractSpellClass = Class.forName("io.redspace.ironsspellbooks.api.spells.AbstractSpell");
            Class<?> castSourceClass    = Class.forName("io.redspace.ironsspellbooks.api.spells.CastSource");
            Class<?> magicDataClass     = Class.forName("io.redspace.ironsspellbooks.api.magic.MagicData");

            // ── SpellRegistry.getSpell(ResourceLocation) ──
            getSpellMethod = spellRegistryClass.getMethod("getSpell", ResourceLocation.class);

            // ── SpellRegistry.none() — the sentinel returned for unknown spells ──
            noneMethod = spellRegistryClass.getMethod("none");
            noneSentinel = noneMethod.invoke(null);
            SpellsNGodsMod.LOGGER.info("[SpellsNGods] Iron's Spells: SpellRegistry.none() sentinel resolved: {}", noneSentinel);

            // ── CastSource.MOB enum value ──
            @SuppressWarnings("unchecked")
            Object mob = Enum.valueOf((Class<Enum>) castSourceClass, "MOB");
            castSourceMob = mob;

            // ── MagicData.getPlayerMagicData(LivingEntity) ──
            getMagicDataMethod = magicDataClass.getMethod("getPlayerMagicData", LivingEntity.class);

            // ── Resolve onCast method ──
            // Iron's Spells 3.x signature: onCast(Level, int, LivingEntity, CastSource, MagicData)
            // NOTE: First param is Level (not ServerLevel!)
            try {
                castMethod = abstractSpellClass.getMethod("onCast",
                        Level.class, int.class, LivingEntity.class, castSourceClass, magicDataClass);
                castMethodStyle = "onCast";
                SpellsNGodsMod.LOGGER.info("[SpellsNGods] Iron's Spells reflection: resolved onCast(Level, int, LivingEntity, CastSource, MagicData)");
            } catch (NoSuchMethodException e1) {
                SpellsNGodsMod.LOGGER.warn("[SpellsNGods] Iron's Spells: onCast(Level,...) not found, trying ServerLevel variant...", e1);
                // Fallback: some versions might use ServerLevel
                try {
                    castMethod = abstractSpellClass.getMethod("onCast",
                            ServerLevel.class, int.class, LivingEntity.class, castSourceClass, magicDataClass);
                    castMethodStyle = "onCast-ServerLevel";
                    SpellsNGodsMod.LOGGER.info("[SpellsNGods] Iron's Spells reflection: resolved onCast(ServerLevel,...) variant");
                } catch (NoSuchMethodException e2) {
                    SpellsNGodsMod.LOGGER.warn("[SpellsNGods] Iron's Spells: onCast(ServerLevel,...) also not found, trying attemptInitiateCast...", e2);
                    // Last resort: attemptInitiateCast
                    try {
                        castMethod = abstractSpellClass.getMethod("attemptInitiateCast",
                                ItemStack.class, int.class, Level.class,
                                LivingEntity.class, castSourceClass, boolean.class);
                        castMethodStyle = "attemptInitiateCast";
                        SpellsNGodsMod.LOGGER.info("[SpellsNGods] Iron's Spells reflection: resolved attemptInitiateCast");
                    } catch (NoSuchMethodException e3) {
                        // Discovery: scan for any cast-like method with LivingEntity param
                        boolean found = false;
                        for (Method m : abstractSpellClass.getDeclaredMethods()) {
                            String name = m.getName().toLowerCase();
                            if (name.contains("cast") && m.getParameterCount() >= 3) {
                                Class<?>[] params = m.getParameterTypes();
                                for (Class<?> p : params) {
                                    if (LivingEntity.class.isAssignableFrom(p)) {
                                        castMethod = m;
                                        castMethodStyle = "discovered:" + m.getName();
                                        found = true;
                                        SpellsNGodsMod.LOGGER.info("[SpellsNGods] Iron's Spells: discovered cast-like method '{}'", m.getName());
                                        break;
                                    }
                                }
                                if (found) break;
                            }
                        }
                        if (!found) {
                            SpellsNGodsMod.LOGGER.error("[SpellsNGods] Could not find ANY compatible spell cast method in Iron's Spells API. " +
                                    "Vanilla fallbacks will be used. Available methods on AbstractSpell:");
                            for (Method m : abstractSpellClass.getDeclaredMethods()) {
                                SpellsNGodsMod.LOGGER.error("  - {}({})", m.getName(), java.util.Arrays.toString(m.getParameterTypes()));
                            }
                            reflectionFailed = true;
                            return;
                        }
                    }
                }
            }

            SpellsNGodsMod.LOGGER.info("[SpellsNGods] Iron's Spells reflection initialized successfully (method style: {})", castMethodStyle);
        } catch (ClassNotFoundException e) {
            SpellsNGodsMod.LOGGER.error("[SpellsNGods] Iron's Spells API classes not found. Spells will use vanilla fallbacks.", e);
            reflectionFailed = true;
        } catch (Exception e) {
            SpellsNGodsMod.LOGGER.error("[SpellsNGods] Failed to initialize Iron's Spells reflection.", e);
            reflectionFailed = true;
        }
    }

    /**
     * Attempt to cast a spell from Iron's Spellbooks programmatically using reflection.
     *
     * @return true if the spell was successfully cast
     */
    public static boolean castSpell(LivingEntity caster, LivingEntity target,
                                     String spellId, int level) {
        initReflection();

        if (reflectionFailed || castMethod == null) {
            return false;
        }

        try {
            // Resolve the spell from the registry
            ResourceLocation spellRL = new ResourceLocation(spellId);
            Object spell = getSpellMethod.invoke(null, spellRL);

            // Iron's Spells returns SpellRegistry.none() sentinel for unknown spells, NOT null
            if (spell == null || spell == noneSentinel) {
                SpellsNGodsMod.LOGGER.warn("[SpellsNGods] Spell '{}' not found in Iron's Spells registry (got {} sentinel)",
                        spellId, spell == null ? "null" : "none()");
                return false;
            }

            // Aim the caster at the target so directional spells fire correctly
            Vec3 look = target.position().subtract(caster.position()).normalize();
            float yRot = (float) (Math.atan2(-look.x, look.z) * (180.0 / Math.PI));
            float xRot = (float) (Math.atan2(-look.y, look.horizontalDistance()) * (180.0 / Math.PI));
            caster.setYRot(yRot);
            caster.setXRot(xRot);
            caster.yHeadRot = yRot;

            if (caster.level() instanceof ServerLevel serverLevel) {
                switch (castMethodStyle) {
                    case "onCast" -> {
                        // onCast(Level, int, LivingEntity, CastSource, MagicData)
                        // First param is Level — ServerLevel IS-A Level, so we pass it directly
                        Object magicData = getMagicDataMethod.invoke(null, caster);
                        castMethod.invoke(spell, (Level) serverLevel, level, caster, castSourceMob, magicData);
                        SpellsNGodsMod.LOGGER.info("[SpellsNGods] Cast '{}' lv{} via onCast (Level param)", spellId, level);
                        return true;
                    }
                    case "onCast-ServerLevel" -> {
                        // onCast(ServerLevel, int, LivingEntity, CastSource, MagicData)
                        Object magicData = getMagicDataMethod.invoke(null, caster);
                        castMethod.invoke(spell, serverLevel, level, caster, castSourceMob, magicData);
                        SpellsNGodsMod.LOGGER.info("[SpellsNGods] Cast '{}' lv{} via onCast (ServerLevel param)", spellId, level);
                        return true;
                    }
                    case "attemptInitiateCast" -> {
                        castMethod.invoke(spell, ItemStack.EMPTY, level, (Level) serverLevel, caster, castSourceMob, true);
                        SpellsNGodsMod.LOGGER.info("[SpellsNGods] Cast '{}' lv{} via attemptInitiateCast", spellId, level);
                        return true;
                    }
                    default -> {
                        // Discovered method — try invoking with best-guess params
                        SpellsNGodsMod.LOGGER.warn("[SpellsNGods] Discovered method '{}' can't be reliably invoked with unknown signature", castMethodStyle);
                        return false;
                    }
                }
            }

            return false;
        } catch (Exception e) {
            SpellsNGodsMod.LOGGER.error("[SpellsNGods] Failed to cast Iron's Spells spell '{}' lv{}", spellId, level, e);
            return false;
        }
    }

    /**
     * Drop a spell scroll for a random spell from the boss's spell pool.
     * Uses reflection to create a real Iron's Spells scroll item via
     * ISpellContainer.createScrollContainer(AbstractSpell, int, ItemStack).
     * Silently drops nothing if reflection fails or the spell pool is empty.
     */
    public static void dropSchoolScroll(GodBossEntity boss, List<GodDefinition.SpellEntry> spellPool) {
        if (spellPool == null || spellPool.isEmpty()) return;

        initReflection();
        if (reflectionFailed || getSpellMethod == null) return;

        try {
            // Pick a weighted-random spell from the pool
            int totalWeight = spellPool.stream().mapToInt(GodDefinition.SpellEntry::weight).sum();
            if (totalWeight <= 0) return;

            ThreadLocalRandom rng = ThreadLocalRandom.current();
            int roll = rng.nextInt(totalWeight);
            GodDefinition.SpellEntry chosen = spellPool.get(spellPool.size() - 1);
            int cumulative = 0;
            for (GodDefinition.SpellEntry entry : spellPool) {
                cumulative += entry.weight();
                if (roll < cumulative) {
                    chosen = entry;
                    break;
                }
            }

            // Resolve the scroll item from Iron's Spells registry
            var scrollItem = ForgeRegistries.ITEMS.getValue(new ResourceLocation("irons_spellbooks", "scroll"));
            if (scrollItem == null || scrollItem == Items.AIR) {
                SpellsNGodsMod.LOGGER.debug("[SpellsNGods] irons_spellbooks:scroll item not found in registry, skipping scroll drop");
                return;
            }

            // Resolve the AbstractSpell object from Iron's Spells SpellRegistry
            ResourceLocation spellRL = new ResourceLocation(chosen.spellId());
            Object spell = getSpellMethod.invoke(null, spellRL);
            if (spell == null || spell == noneSentinel) {
                SpellsNGodsMod.LOGGER.debug("[SpellsNGods] Spell '{}' not found in Iron's Spells registry, skipping scroll drop", chosen.spellId());
                return;
            }

            // Pick a random level within the entry's range
            int spellLevel = chosen.minLevel() + (chosen.maxLevel() > chosen.minLevel()
                    ? rng.nextInt(chosen.maxLevel() - chosen.minLevel() + 1) : 0);

            // Create scroll ItemStack and apply spell data via ISpellContainer.createScrollContainer
            ItemStack scrollStack = new ItemStack(scrollItem);
            Class<?> spellContainerInterface = Class.forName("io.redspace.ironsspellbooks.api.spells.ISpellContainer");
            Class<?> abstractSpellClass = Class.forName("io.redspace.ironsspellbooks.api.spells.AbstractSpell");
            Method createScrollContainer = spellContainerInterface.getMethod(
                    "createScrollContainer", abstractSpellClass, int.class, ItemStack.class);
            createScrollContainer.invoke(null, spell, spellLevel, scrollStack);

            // Drop the scroll
            ItemEntity itemEntity = new ItemEntity(
                    boss.level(),
                    boss.getX(), boss.getY() + 0.5, boss.getZ(),
                    scrollStack
            );
            itemEntity.setDefaultPickUpDelay();
            boss.level().addFreshEntity(itemEntity);

            SpellsNGodsMod.LOGGER.debug("[SpellsNGods] Dropped Iron's Spells scroll: {} lv{}", chosen.spellId(), spellLevel);
        } catch (Exception e) {
            SpellsNGodsMod.LOGGER.warn("[SpellsNGods] Failed to drop Iron's Spells scroll, skipping: {}", e.getMessage());
        }
    }
}
