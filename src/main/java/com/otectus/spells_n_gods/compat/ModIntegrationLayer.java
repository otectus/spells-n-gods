package com.otectus.spells_n_gods.compat;

import com.otectus.spells_n_gods.SpellsNGodsMod;
import com.otectus.spells_n_gods.boss.GodBossEntity;
import com.otectus.spells_n_gods.config.SpellsNGodsConfig;
import com.otectus.spells_n_gods.data.GodDefinition;
import com.otectus.spells_n_gods.item.RunicFragmentItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.EvokerFangs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;
import org.joml.Vector3f;

import java.util.concurrent.ThreadLocalRandom;

public class ModIntegrationLayer {

    /**
     * Get the weapon ItemStack for a boss, using SimplySwords if available.
     */
    public static ItemStack getWeaponForGod(GodDefinition.BossDefinition bossDef) {
        // Try resolving weapon_id directly (supports spells_n_gods: and simplyswords: namespaces)
        try {
            var item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(bossDef.weaponId()));
            if (item != null && item != Items.AIR) {
                SpellsNGodsMod.LOGGER.debug("Resolved weapon: {}", bossDef.weaponId());
                return new ItemStack(item);
            }
            SpellsNGodsMod.LOGGER.debug("Weapon '{}' not found in registry, trying fallback",
                    bossDef.weaponId());
        } catch (Exception e) {
            SpellsNGodsMod.LOGGER.warn("Failed to resolve weapon '{}': {}",
                    bossDef.weaponId(), e.getMessage());
        }

        // Fallback to configured fallback weapon
        try {
            var fallbackItem = ForgeRegistries.ITEMS.getValue(new ResourceLocation(bossDef.fallbackWeaponId()));
            if (fallbackItem != null && fallbackItem != Items.AIR) {
                SpellsNGodsMod.LOGGER.debug("Using fallback weapon: {}", bossDef.fallbackWeaponId());
                return new ItemStack(fallbackItem);
            }
        } catch (Exception e) {
            SpellsNGodsMod.LOGGER.warn("Failed to resolve fallback weapon '{}': {}",
                    bossDef.fallbackWeaponId(), e.getMessage());
        }

        SpellsNGodsMod.LOGGER.warn("All weapon resolution failed, using iron sword as last resort");
        return new ItemStack(Items.IRON_SWORD);
    }

    /**
     * Cast a spell from Iron's Spellbooks, or fall back to vanilla effects.
     * Always falls through to vanilla if Iron's Spells fails, ensuring
     * the boss ALWAYS does something when casting.
     */
    public static boolean castSpell(LivingEntity caster, LivingEntity target,
                                     String spellId, int level, String magicSchool) {
        boolean ironsCast = false;

        if (ModCompatHandler.IRONS_SPELLS_LOADED) {
            try {
                ironsCast = IronsSpellsCompat.castSpell(caster, target, spellId, level);
            } catch (Exception e) {
                SpellsNGodsMod.LOGGER.warn("Iron's Spells cast failed for '{}': {}", spellId, e.getMessage());
            }
        }

        if (!ironsCast) {
            // Always apply vanilla fallback so the boss visibly does something
            return applyVanillaFallback(caster, target, magicSchool, level);
        }
        return true;
    }

    /**
     * Drop compat-mod-specific loot on boss death.
     */
    public static void dropBossLoot(GodBossEntity boss, DamageSource source) {
        GodDefinition god = boss.getGodDefinition();
        if (god == null) return;

        ThreadLocalRandom rng = ThreadLocalRandom.current();

        // Divine weapon drop (supports both spells_n_gods: and simplyswords: weapon IDs)
        double weaponChance = SpellsNGodsConfig.COMMON.dropSimplySwordsChance.get();
        if (rng.nextDouble() < weaponChance) {
            try {
                var item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(god.boss().weaponId()));
                if (item != null && item != Items.AIR) {
                    dropItem(boss, new ItemStack(item));
                }
            } catch (Exception e) {
                SpellsNGodsMod.LOGGER.debug("Could not drop weapon: {}", e.getMessage());
            }
        }

        // Iron's Spells scroll drop
        if (ModCompatHandler.IRONS_SPELLS_LOADED) {
            double scrollChance = SpellsNGodsConfig.COMMON.dropIronsScrollChance.get();
            if (rng.nextDouble() < scrollChance) {
                try {
                    IronsSpellsCompat.dropSchoolScroll(boss, god.boss().spellPool());
                } catch (Exception e) {
                    SpellsNGodsMod.LOGGER.debug("Could not drop Iron's Spells scroll: {}", e.getMessage());
                }
            }
        }

        // Runic Fragment drop (always available, not dependent on compat mods)
        double fragmentChance = SpellsNGodsConfig.COMMON.dropFragmentChance.get();
        if (rng.nextDouble() < fragmentChance) {
            int min = SpellsNGodsConfig.COMMON.dropFragmentMin.get();
            int max = SpellsNGodsConfig.COMMON.dropFragmentMax.get();
            int count = min + (max > min ? rng.nextInt(max - min + 1) : 0);
            if (count > 0) {
                ItemStack fragments = RunicFragmentItem.createForGod(god.id().toString());
                fragments.setCount(count);
                dropItem(boss, fragments);
            }
        }
    }

    private static void dropItem(LivingEntity entity, ItemStack stack) {
        ItemEntity itemEntity = new ItemEntity(
                entity.level(),
                entity.getX(), entity.getY() + 0.5, entity.getZ(),
                stack
        );
        itemEntity.setDefaultPickUpDelay();
        entity.level().addFreshEntity(itemEntity);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Enhanced Vanilla Fallback VFX
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Vanilla-effect fallback when Iron's Spellbooks is not present or fails.
     * Effects themed by magic school with particles and sound for dramatic visual impact.
     */
    private static boolean applyVanillaFallback(LivingEntity caster, LivingEntity target,
                                                  String magicSchool, int level) {
        float damage = 4.0f + (level * 2.0f);
        int effectDuration = 60 + level * 20;
        ServerLevel serverLevel = caster.level() instanceof ServerLevel sl ? sl : null;

        // ── Universal casting windup — spiraling ENCHANT particles around caster ──
        if (serverLevel != null) {
            spawnCastingWindup(serverLevel, caster);
        }

        switch (magicSchool.toLowerCase()) {
            case "holy" -> applyHoly(caster, target, serverLevel, damage, effectDuration, level);
            case "fire" -> applyFire(caster, target, serverLevel, damage, level);
            case "ice" -> applyIce(caster, target, serverLevel, damage, effectDuration, level);
            case "lightning" -> applyLightning(caster, target, serverLevel, damage, level);
            case "blood" -> applyBlood(caster, target, serverLevel, damage, effectDuration, level);
            case "nature" -> applyNature(caster, target, serverLevel, damage, effectDuration, level);
            case "ender" -> applyEnder(caster, target, serverLevel, damage);
            case "eldritch" -> applyEldritch(caster, target, serverLevel, damage, effectDuration, level);
            case "evocation" -> applyEvocation(caster, target, serverLevel, damage, level);
            default -> applyDefault(caster, target, serverLevel, damage);
        }
        return true;
    }

    /**
     * Universal casting windup — spiraling particles around caster's hands
     * before the spell effect fires. Creates a 0.5s visual buildup.
     */
    private static void spawnCastingWindup(ServerLevel level, LivingEntity caster) {
        double cx = caster.getX();
        double cy = caster.getY() + 1.2; // chest height
        double cz = caster.getZ();

        // Spiral ring of ENCHANT particles
        for (int i = 0; i < 24; i++) {
            double angle = (i / 24.0) * Math.PI * 2;
            double radius = 0.6 + (i / 24.0) * 0.4;
            double px = cx + Math.cos(angle) * radius;
            double pz = cz + Math.sin(angle) * radius;
            double py = cy + (i / 24.0) * 0.8;
            level.sendParticles(ParticleTypes.ENCHANT, px, py, pz, 2, 0.05, 0.05, 0.05, 0.02);
        }

        // Inner glow
        level.sendParticles(ParticleTypes.ENCHANTED_HIT, cx, cy, cz, 8, 0.3, 0.3, 0.3, 0.1);

        // Windup sound
        level.playSound(null, caster.blockPosition(),
                SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.HOSTILE, 0.6F, 1.8F);
    }

    // ──────── HOLY ────────
    private static void applyHoly(LivingEntity caster, LivingEntity target,
                                    ServerLevel level, float damage, int effectDuration, int spellLevel) {
        caster.heal(damage * 0.5f);
        target.hurt(caster.damageSources().magic(), damage);

        if (level != null) {
            double tx = target.getX(), ty = target.getY(), tz = target.getZ();
            double cx = caster.getX(), cy = caster.getY(), cz = caster.getZ();

            // Descending pillar of light (END_ROD particles in a column)
            for (int y = 0; y < 8; y++) {
                level.sendParticles(ParticleTypes.END_ROD,
                        tx, ty + y, tz, 6, 0.2, 0.1, 0.2, 0.01);
            }

            // Healing ring around caster (COMPOSTER particles in a circle)
            for (int i = 0; i < 16; i++) {
                double angle = (i / 16.0) * Math.PI * 2;
                level.sendParticles(ParticleTypes.COMPOSTER,
                        cx + Math.cos(angle) * 2.0, cy + 0.5, cz + Math.sin(angle) * 2.0,
                        3, 0.1, 0.3, 0.1, 0.01);
            }

            // Halo above target
            for (int i = 0; i < 12; i++) {
                double angle = (i / 12.0) * Math.PI * 2;
                level.sendParticles(ParticleTypes.END_ROD,
                        tx + Math.cos(angle) * 0.5, ty + target.getBbHeight() + 0.5, tz + Math.sin(angle) * 0.5,
                        1, 0.02, 0.02, 0.02, 0.0);
            }

            // Briefly regenerate caster
            caster.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 40, 0, false, true));

            level.playSound(null, caster.blockPosition(),
                    SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, 1.2F, 1.5F);
            level.playSound(null, target.blockPosition(),
                    SoundEvents.BEACON_DEACTIVATE, SoundSource.HOSTILE, 0.8F, 2.0F);
        }
    }

    // ──────── FIRE ────────
    private static void applyFire(LivingEntity caster, LivingEntity target,
                                    ServerLevel level, float damage, int spellLevel) {
        target.setSecondsOnFire(3 + spellLevel);
        target.hurt(caster.damageSources().magic(), damage);

        if (level != null) {
            double tx = target.getX(), ty = target.getY(), tz = target.getZ();

            // Core flame burst
            level.sendParticles(ParticleTypes.FLAME,
                    tx, ty + 0.5, tz, 40, 0.5, 0.5, 0.5, 0.08);

            // Expanding ring of LAVA particles on ground
            for (int ring = 0; ring < 3; ring++) {
                double radius = 1.0 + ring * 0.8;
                int count = 8 + ring * 4;
                for (int i = 0; i < count; i++) {
                    double angle = (i / (double) count) * Math.PI * 2;
                    level.sendParticles(ParticleTypes.LAVA,
                            tx + Math.cos(angle) * radius, ty + 0.1, tz + Math.sin(angle) * radius,
                            1, 0.1, 0.05, 0.1, 0.0);
                }
            }

            // Rising smoke column
            level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                    tx, ty + 1.0, tz, 12, 0.3, 1.5, 0.3, 0.01);

            // Firecharge launch sound + blaze shoot
            level.playSound(null, caster.blockPosition(),
                    SoundEvents.FIRECHARGE_USE, SoundSource.HOSTILE, 1.2F, 0.8F);
            level.playSound(null, caster.blockPosition(),
                    SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 1.0F, 1.0F);
        }
    }

    // ──────── ICE ────────
    private static void applyIce(LivingEntity caster, LivingEntity target,
                                   ServerLevel level, float damage, int effectDuration, int spellLevel) {
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, effectDuration, Math.min(spellLevel, 3)));
        target.hurt(caster.damageSources().freeze(), damage);
        // Apply freeze ticks for the blue screen overlay
        target.setTicksFrozen(Math.min(target.getTicksFrozen() + 140 + spellLevel * 20, 300));

        if (level != null) {
            double tx = target.getX(), ty = target.getY(), tz = target.getZ();

            // 3 expanding frost burst rings at different heights
            for (int ring = 0; ring < 3; ring++) {
                double radius = 0.8 + ring * 1.0;
                double height = ty + 0.3 + ring * 0.6;
                int count = 12 + ring * 4;
                for (int i = 0; i < count; i++) {
                    double angle = (i / (double) count) * Math.PI * 2;
                    level.sendParticles(ParticleTypes.SNOWFLAKE,
                            tx + Math.cos(angle) * radius, height, tz + Math.sin(angle) * radius,
                            2, 0.1, 0.1, 0.1, 0.02);
                }
            }

            // Snowball impact particles scattered around
            level.sendParticles(ParticleTypes.ITEM_SNOWBALL,
                    tx, ty + 1.0, tz, 20, 0.6, 0.6, 0.6, 0.1);

            // Core frost burst
            level.sendParticles(ParticleTypes.SNOWFLAKE,
                    tx, ty + 0.5, tz, 30, 0.3, 0.8, 0.3, 0.05);

            level.playSound(null, caster.blockPosition(),
                    SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 1.2F, 0.5F);
            level.playSound(null, target.blockPosition(),
                    SoundEvents.POWDER_SNOW_STEP, SoundSource.HOSTILE, 1.0F, 0.7F);
        }
    }

    // ──────── LIGHTNING ────────
    private static void applyLightning(LivingEntity caster, LivingEntity target,
                                         ServerLevel level, float damage, int spellLevel) {
        target.hurt(caster.damageSources().lightningBolt(), damage * 1.5f);

        if (level != null) {
            double tx = target.getX(), ty = target.getY(), tz = target.getZ();

            // Spawn a REAL visual-only lightning bolt on the target
            LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
            if (bolt != null) {
                bolt.setVisualOnly(true);
                bolt.moveTo(tx, ty, tz);
                level.addFreshEntity(bolt);
            }

            // Double particle count — electric sparks everywhere
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    tx, ty + 1.0, tz, 60, 0.5, 0.8, 0.5, 0.3);

            // Flash particles at impact
            level.sendParticles(ParticleTypes.FLASH,
                    tx, ty + 1.0, tz, 2, 0.0, 0.0, 0.0, 0.0);

            // Expanding electric ring
            for (int i = 0; i < 16; i++) {
                double angle = (i / 16.0) * Math.PI * 2;
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                        tx + Math.cos(angle) * 2.0, ty + 0.2, tz + Math.sin(angle) * 2.0,
                        4, 0.1, 0.1, 0.1, 0.15);
            }

            level.playSound(null, target.blockPosition(),
                    SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.HOSTILE, 0.8F, 1.5F);
            level.playSound(null, target.blockPosition(),
                    SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.HOSTILE, 1.0F, 1.0F);
        }
    }

    // ──────── BLOOD ────────
    private static void applyBlood(LivingEntity caster, LivingEntity target,
                                     ServerLevel level, float damage, int effectDuration, int spellLevel) {
        target.hurt(caster.damageSources().magic(), damage);
        caster.heal(damage * 0.4f);
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, effectDuration, 0));

        if (level != null) {
            double tx = target.getX(), ty = target.getY(), tz = target.getZ();
            double cx = caster.getX(), cy = caster.getY(), cz = caster.getZ();

            // Damage indicator burst
            level.sendParticles(ParticleTypes.DAMAGE_INDICATOR,
                    tx, ty + 1.0, tz, 25, 0.5, 0.5, 0.5, 0.02);

            // Dripping obsidian tears around target (looks like dark blood)
            level.sendParticles(ParticleTypes.DRIPPING_OBSIDIAN_TEAR,
                    tx, ty + 2.0, tz, 15, 0.5, 0.5, 0.5, 0.01);

            // Red DUST particles (blood mist)
            DustParticleOptions redDust = new DustParticleOptions(new Vector3f(0.8f, 0.0f, 0.0f), 1.5f);
            level.sendParticles(redDust,
                    tx, ty + 1.0, tz, 20, 0.6, 0.6, 0.6, 0.05);

            // Brief DARKNESS effect for screen tint
            target.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 15, 0, false, false));

            // Life drain trail from target to caster
            Vec3 dir = new Vec3(cx - tx, cy - ty, cz - tz);
            double dist = dir.length();
            dir = dir.normalize();
            for (int i = 0; i < (int) (dist * 3); i++) {
                double t = i / (dist * 3);
                level.sendParticles(ParticleTypes.DAMAGE_INDICATOR,
                        tx + dir.x * t * dist, ty + 1.0 + dir.y * t * dist, tz + dir.z * t * dist,
                        1, 0.05, 0.05, 0.05, 0.0);
            }

            level.playSound(null, caster.blockPosition(),
                    SoundEvents.WITHER_HURT, SoundSource.HOSTILE, 0.9F, 0.7F);
            level.playSound(null, target.blockPosition(),
                    SoundEvents.WITHER_SHOOT, SoundSource.HOSTILE, 0.6F, 0.5F);
        }
    }

    // ──────── NATURE ────────
    private static void applyNature(LivingEntity caster, LivingEntity target,
                                      ServerLevel level, float damage, int effectDuration, int spellLevel) {
        target.addEffect(new MobEffectInstance(MobEffects.POISON, effectDuration, Math.min(spellLevel, 2)));
        target.addEffect(new MobEffectInstance(MobEffects.HUNGER, effectDuration, 1));
        target.hurt(caster.damageSources().magic(), damage * 0.5f);

        if (level != null) {
            double tx = target.getX(), ty = target.getY(), tz = target.getZ();

            // Spore cloud engulfing target
            level.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR,
                    tx, ty + 1.0, tz, 40, 1.2, 1.2, 1.2, 0.05);

            // Falling spore blossoms in a wide area
            level.sendParticles(ParticleTypes.FALLING_SPORE_BLOSSOM,
                    tx, ty + 4.0, tz, 25, 2.0, 1.0, 2.0, 0.01);

            // Ring of HAPPY_VILLAGER (green sparkles) around impact
            for (int i = 0; i < 16; i++) {
                double angle = (i / 16.0) * Math.PI * 2;
                level.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                        tx + Math.cos(angle) * 1.5, ty + 0.3, tz + Math.sin(angle) * 1.5,
                        2, 0.1, 0.1, 0.1, 0.0);
            }

            // Composting particles (growing)
            level.sendParticles(ParticleTypes.COMPOSTER,
                    tx, ty + 0.5, tz, 15, 0.4, 0.3, 0.4, 0.02);

            level.playSound(null, caster.blockPosition(),
                    SoundEvents.BIG_DRIPLEAF_TILT_UP, SoundSource.HOSTILE, 1.0F, 0.6F);
            level.playSound(null, target.blockPosition(),
                    SoundEvents.AZALEA_LEAVES_BREAK, SoundSource.HOSTILE, 1.0F, 0.8F);
        }
    }

    // ──────── ENDER ────────
    private static void applyEnder(LivingEntity caster, LivingEntity target,
                                     ServerLevel level, float damage) {
        // Save pre-teleport position for particle trail
        double preTpX = target.getX(), preTpY = target.getY(), preTpZ = target.getZ();

        // Teleport target randomly within 10 blocks
        double dx = (target.getRandom().nextDouble() - 0.5) * 20;
        double dz = (target.getRandom().nextDouble() - 0.5) * 20;
        double newX = target.getX() + dx;
        double newZ = target.getZ() + dz;
        target.teleportTo(newX, target.getY(), newZ);
        target.hurt(caster.damageSources().magic(), damage * 0.5f);
        target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 80, 0));

        if (level != null) {
            double postTpX = target.getX(), postTpY = target.getY(), postTpZ = target.getZ();

            // REVERSE_PORTAL at origin (where they were)
            level.sendParticles(ParticleTypes.REVERSE_PORTAL,
                    preTpX, preTpY + 1.0, preTpZ, 40, 0.3, 0.8, 0.3, 0.15);

            // PORTAL particles at origin
            level.sendParticles(ParticleTypes.PORTAL,
                    preTpX, preTpY + 1.0, preTpZ, 30, 0.5, 1.0, 0.5, 0.5);

            // REVERSE_PORTAL at destination (where they landed)
            level.sendParticles(ParticleTypes.REVERSE_PORTAL,
                    postTpX, postTpY + 1.0, postTpZ, 40, 0.3, 0.8, 0.3, 0.15);

            // Particle trail from origin to destination
            Vec3 dir = new Vec3(postTpX - preTpX, postTpY - preTpY, postTpZ - preTpZ);
            double dist = dir.length();
            if (dist > 0.5) {
                dir = dir.normalize();
                int steps = (int) Math.min(dist * 2, 30);
                for (int i = 0; i < steps; i++) {
                    double t = i / (double) steps;
                    level.sendParticles(ParticleTypes.PORTAL,
                            preTpX + dir.x * t * dist,
                            preTpY + 1.0 + dir.y * t * dist,
                            preTpZ + dir.z * t * dist,
                            2, 0.1, 0.1, 0.1, 0.1);
                }
            }

            level.playSound(null, BlockPos.containing(preTpX, preTpY, preTpZ),
                    SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.2F, 0.8F);
            level.playSound(null, target.blockPosition(),
                    SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.2F, 1.2F);
        }
    }

    // ──────── ELDRITCH ────────
    private static void applyEldritch(LivingEntity caster, LivingEntity target,
                                        ServerLevel level, float damage, int effectDuration, int spellLevel) {
        target.addEffect(new MobEffectInstance(MobEffects.WITHER, effectDuration, Math.min(spellLevel, 2)));
        target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0));
        target.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 100, 0, false, false));
        target.hurt(caster.damageSources().magic(), damage);

        if (level != null) {
            double tx = target.getX(), ty = target.getY(), tz = target.getZ();

            // SCULK_SOUL core burst
            level.sendParticles(ParticleTypes.SCULK_SOUL,
                    tx, ty + 1.0, tz, 30, 0.5, 0.8, 0.5, 0.05);

            // SCULK_CHARGE_POP expanding ring
            for (int i = 0; i < 20; i++) {
                double angle = (i / 20.0) * Math.PI * 2;
                double radius = 2.0;
                level.sendParticles(ParticleTypes.SCULK_CHARGE_POP,
                        tx + Math.cos(angle) * radius, ty + 0.5, tz + Math.sin(angle) * radius,
                        3, 0.1, 0.1, 0.1, 0.02);
            }

            // SONIC_BOOM particle at the target (the warden's ranged attack visual)
            level.sendParticles(ParticleTypes.SONIC_BOOM,
                    tx, ty + 1.0, tz, 1, 0.0, 0.0, 0.0, 0.0);

            // Knockback pulse for "screen shake" feel
            Vec3 pushDir = target.position().subtract(caster.position()).normalize().scale(0.8);
            target.push(pushDir.x, 0.3, pushDir.z);
            target.hurtMarked = true;

            level.playSound(null, caster.blockPosition(),
                    SoundEvents.WARDEN_ROAR, SoundSource.HOSTILE, 0.7F, 0.4F);
            level.playSound(null, target.blockPosition(),
                    SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 0.5F, 0.6F);
        }
    }

    // ──────── EVOCATION ────────
    private static void applyEvocation(LivingEntity caster, LivingEntity target,
                                         ServerLevel level, float damage, int spellLevel) {
        if (level != null) {
            Vec3 direction = target.position().subtract(caster.position()).normalize();
            boolean enraged = caster instanceof GodBossEntity boss && boss.isEnraged();

            // Evoker fangs — 8 in a line (more when enraged)
            int fangCount = enraged ? 12 : 8;
            for (int i = 1; i <= fangCount; i++) {
                double fx = caster.getX() + direction.x * i * 1.3;
                double fz = caster.getZ() + direction.z * i * 1.3;
                BlockPos fangPos = BlockPos.containing(fx, caster.getY(), fz);
                // Find ground level
                while (level.isEmptyBlock(fangPos) && fangPos.getY() > caster.getY() - 5) {
                    fangPos = fangPos.below();
                }
                EvokerFangs fangs = new EvokerFangs(
                        level, fx, fangPos.getY() + 1, fz,
                        (float) Math.atan2(direction.z, direction.x), i * 2, caster);
                level.addFreshEntity(fangs);
            }

            // Fan pattern when enraged — extra fangs at angles
            if (enraged) {
                for (int side = -1; side <= 1; side += 2) {
                    double spreadAngle = Math.toRadians(25) * side;
                    double spreadX = direction.x * Math.cos(spreadAngle) - direction.z * Math.sin(spreadAngle);
                    double spreadZ = direction.x * Math.sin(spreadAngle) + direction.z * Math.cos(spreadAngle);
                    for (int i = 1; i <= 6; i++) {
                        double fx = caster.getX() + spreadX * i * 1.3;
                        double fz = caster.getZ() + spreadZ * i * 1.3;
                        BlockPos fangPos = BlockPos.containing(fx, caster.getY(), fz);
                        while (level.isEmptyBlock(fangPos) && fangPos.getY() > caster.getY() - 5) {
                            fangPos = fangPos.below();
                        }
                        EvokerFangs fangs = new EvokerFangs(
                                level, fx, fangPos.getY() + 1, fz,
                                (float) Math.atan2(spreadZ, spreadX), i * 2 + 3, caster);
                        level.addFreshEntity(fangs);
                    }
                }
            }

            // WITCH particles swirling around caster
            level.sendParticles(ParticleTypes.WITCH,
                    caster.getX(), caster.getY() + 1.5, caster.getZ(),
                    20, 0.4, 0.4, 0.4, 0.05);

            // Enchanted hit particles at target
            level.sendParticles(ParticleTypes.ENCHANTED_HIT,
                    target.getX(), target.getY() + 1.0, target.getZ(),
                    15, 0.4, 0.4, 0.4, 0.2);

            level.playSound(null, caster.blockPosition(),
                    SoundEvents.EVOKER_CAST_SPELL, SoundSource.HOSTILE, 1.2F, 1.0F);
            level.playSound(null, caster.blockPosition(),
                    SoundEvents.EVOKER_PREPARE_ATTACK, SoundSource.HOSTILE, 1.0F, 0.8F);
        }
        target.hurt(caster.damageSources().magic(), damage * 0.5f);
    }

    // ──────── DEFAULT ────────
    private static void applyDefault(LivingEntity caster, LivingEntity target,
                                       ServerLevel level, float damage) {
        target.hurt(caster.damageSources().magic(), damage);

        if (level != null) {
            double tx = target.getX(), ty = target.getY(), tz = target.getZ();

            // Enchanted hit particles
            level.sendParticles(ParticleTypes.ENCHANTED_HIT,
                    tx, ty + 1.0, tz, 25, 0.5, 0.5, 0.5, 0.3);

            // Enchant particles swirling up
            level.sendParticles(ParticleTypes.ENCHANT,
                    tx, ty + 1.0, tz, 30, 0.5, 0.5, 0.5, 0.5);

            // Brief GLOWING effect on target so they shimmer
            target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 40, 0, false, false));

            level.playSound(null, target.blockPosition(),
                    SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.HOSTILE, 1.0F, 1.2F);
        }
    }
}
