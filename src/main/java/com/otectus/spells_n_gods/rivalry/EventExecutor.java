package com.otectus.spells_n_gods.rivalry;

import com.otectus.spells_n_gods.SpellsNGodsMod;
import com.otectus.spells_n_gods.capability.DivineTier;
import com.otectus.spells_n_gods.capability.PlayerDivinityCapability;
import com.otectus.spells_n_gods.data.EventDefinition;
import com.otectus.spells_n_gods.data.GodDefinition;
import com.otectus.spells_n_gods.data.SpellsNGodsDataManager;
import com.otectus.spells_n_gods.util.SchoolColors;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import org.joml.Vector3f;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Executes divine events and manages their effects on players.
 */
public class EventExecutor {

    // Track active effects per event for cleanup
    private static final Map<UUID, EventEffects> activeEffects = new ConcurrentHashMap<>();

    private record EventEffects(DivineEvent event) {}

    /**
     * Execute a divine event, applying its effects to affected players.
     */
    public static void execute(MinecraftServer server, DivineEvent event) {
        SpellsNGodsMod.LOGGER.info("Executing divine event: {} (type: {}, severity: {})",
                event.eventId(), event.type(), event.severity());

        GodDefinition sourceGod = SpellsNGodsDataManager.getGods().get(event.sourceGodId());
        GodDefinition targetGod = SpellsNGodsDataManager.getGods().get(event.targetGodId());

        String sourceGodName = sourceGod != null ? sourceGod.displayName() : event.sourceGodId().toString();
        String targetGodName = targetGod != null ? targetGod.displayName() : event.targetGodId().toString();

        for (UUID playerId : event.affectedPlayers()) {
            ServerPlayer player = server.getPlayerList().getPlayer(playerId);
            if (player != null) {
                executeForPlayer(player, event, sourceGod, sourceGodName, targetGodName);
            }
        }

        activeEffects.put(event.eventId(), new EventEffects(event));
    }

    private static void executeForPlayer(ServerPlayer player, DivineEvent event,
                                          GodDefinition sourceGod, String sourceGodName, String targetGodName) {
        float intensity = event.severity().getIntensity();
        int durationTicks = (int) ((event.expiresTimeMs() - System.currentTimeMillis()) / 50);

        // Data-driven events (chosen from a god's event_pool) run the JSON-defined presentation + effects.
        EventDefinition def = event.eventDefId() == null ? null
                : SpellsNGodsDataManager.getEvents().get(event.eventDefId());
        if (def != null) {
            executeDataDriven(player, event, def, sourceGod, sourceGodName, intensity, durationTicks);
            return;
        }

        // Legacy enum-driven fallback (no pool entry resolved).
        switch (event.type()) {
            case OMEN -> {
                sendOmenMessage(player, sourceGodName, targetGodName, event.severity());
                playOmenSound(player, event.severity());
                spawnEventParticles(player, sourceGod, ParticleTypes.ENCHANT, 5 + (int)(intensity * 10));
            }

            case FAITH_TEST -> {
                sendFaithTestMessage(player, sourceGodName, event.severity());
                playOmenSound(player, event.severity());
                spawnEventParticles(player, sourceGod, ParticleTypes.ENCHANT, 12);
                // Apply minor debuff
                int amplifier = (int) (intensity * 2);
                player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, durationTicks, amplifier, true, true, true));
            }

            case DIVINE_TRIAL -> {
                sendTrialMessage(player, sourceGodName, event.severity());
                playDramaticSound(player);
                spawnEventParticles(player, sourceGod, ParticleTypes.SOUL_FIRE_FLAME, 20);
                // Apply multiple debuffs based on severity
                int amplifier = (int) (intensity * 3);
                player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, durationTicks, amplifier, true, true, true));
                if (intensity >= 0.3f) {
                    player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, durationTicks, 0, true, true, true));
                }
                if (intensity >= 0.5f) {
                    player.addEffect(new MobEffectInstance(MobEffects.UNLUCK, durationTicks, 0, true, true, true));
                }
            }

            case RIVAL_CURSE -> {
                sendCurseMessage(player, sourceGodName, event.severity());
                playDramaticSound(player);
                spawnEventParticles(player, sourceGod, ParticleTypes.SCULK_SOUL, 15);
                // Apply curse effects
                int amplifier = (int) (intensity * 2);
                player.addEffect(new MobEffectInstance(MobEffects.UNLUCK, durationTicks, amplifier, true, true, true));
                player.addEffect(new MobEffectInstance(MobEffects.BAD_OMEN, durationTicks, 0, true, true, true));
            }

            case DIVINE_BLESSING -> {
                // Positive effect (for source god followers, but we're affecting target here)
                // This shouldn't normally be called for target followers
                sendBlessingMessage(player, targetGodName);
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, durationTicks, 0, true, true, true));
            }

            case INSPIRATION -> {
                // Bonus favor gain (handled elsewhere)
                player.sendSystemMessage(Component.literal("[Divine Inspiration] ")
                        .withStyle(ChatFormatting.GOLD)
                        .append(Component.literal("You feel inspired by " + targetGodName + "'s presence.")
                                .withStyle(ChatFormatting.YELLOW)));
            }

            case DIVINE_MANIFESTATION -> {
                sendManifestationMessage(player, sourceGodName);
                playDramaticSound(player);
                spawnEventParticles(player, sourceGod, ParticleTypes.FLASH, 3);
                spawnEventParticles(player, sourceGod, ParticleTypes.END_ROD, 30);
                player.addEffect(new MobEffectInstance(MobEffects.GLOWING, durationTicks / 2, 0, true, true, true));
            }

            case SACRED_SITE_SPAWN -> {
                // Would spawn a structure - for now just notify
                player.sendSystemMessage(Component.literal("[Divine Event] ")
                        .withStyle(ChatFormatting.LIGHT_PURPLE)
                        .append(Component.literal("A sacred site has manifested nearby...")
                                .withStyle(ChatFormatting.WHITE)));
            }
        }
    }

    // ==================== DATA-DRIVEN EVENTS (god event_pool + EventDefinition) ====================

    private static void executeDataDriven(ServerPlayer player, DivineEvent event, EventDefinition def,
                                          GodDefinition sourceGod, String sourceGodName,
                                          float intensity, int durationTicks) {
        // requires_rival should equal the target god by construction; log a mismatch but never drop.
        String requiresRival = def.requiresRival();
        if (!requiresRival.isBlank() && !requiresRival.equals(event.targetGodId().getPath())) {
            SpellsNGodsMod.LOGGER.debug("Event {} requires_rival '{}' != target '{}'",
                    def.id(), requiresRival, event.targetGodId().getPath());
        }

        // Presentation always shows — even sub-tier followers feel the omen.
        sendDataDrivenTitle(player, def, sourceGodName, event.severity());
        playPresentationSound(player, def, event.severity());
        spawnPresentationParticles(player, def, sourceGod, intensity);

        // Tier gate: only sufficiently devoted followers feel the mechanical effects.
        DivineTier required = parseTier(def.tierAtLeast());
        if (required != DivineTier.NONE
                && !PlayerDivinityCapability.getOrCreate(player).getCurrentTier().isAtLeast(required)) {
            return;
        }

        int effectTicks = def.durationSeconds() > 0 ? def.durationSeconds() * 20 : durationTicks;
        for (EventDefinition.EffectEntry fx : def.effects()) {
            applyEffect(player, fx, intensity, effectTicks, def);
        }
    }

    /**
     * Apply one data-driven effect entry. Many effects map cleanly onto vanilla mechanics; a handful are
     * documented approximations (no clean hook exists yet), and zone-scoped effects are logged for a
     * future area-effect system rather than applied per player.
     */
    private static void applyEffect(ServerPlayer player, EventDefinition.EffectEntry fx,
                                    float intensity, int durationTicks, EventDefinition def) {
        switch (fx.type()) {
            // --- Direct vanilla mob effects (clean) ---
            case "blindness" -> addEffect(player, MobEffects.BLINDNESS, fx.durationSeconds(4) * 20, 0);
            case "weakness" -> addEffect(player, MobEffects.WEAKNESS, durationTicks, fx.amplifier());
            case "slowness", "reduced_movement", "movement_slow" ->
                    addEffect(player, MobEffects.MOVEMENT_SLOWDOWN, durationTicks, fx.amplifier());
            case "hunger_drain" -> addEffect(player, MobEffects.HUNGER, durationTicks, fx.amplifier());
            case "mining_fatigue" -> addEffect(player, MobEffects.DIG_SLOWDOWN, durationTicks, fx.amplifier());
            case "nausea" -> addEffect(player, MobEffects.CONFUSION,
                    fx.durationSeconds(def.durationSeconds()) * 20, fx.amplifier());
            case "darkness", "reduced_visibility" ->
                    addEffect(player, MobEffects.DARKNESS, durationTicks, fx.amplifier());

            // --- Direct actions (clean) ---
            case "ignite", "fire_tick" -> player.setSecondsOnFire(Math.max(1, fx.durationSeconds(3)));
            case "teleport_random", "unstable_teleport_chance" -> teleportRandom(player, def.radius());
            case "knockback_burst" -> knockback(player, (float) fx.number("strength", 1.0));
            case "thorns_damage" ->
                    player.hurt(player.damageSources().magic(), (float) fx.number("amount", 0.5));
            case "poison_chance" -> {
                if (player.getRandom().nextDouble() < fx.number("chance", 0.1)) {
                    addEffect(player, MobEffects.POISON, durationTicks, 0);
                }
            }

            // --- Clean mod hook: lengthen prayer cooldowns for the event's duration ---
            case "increase_cooldown_multiplier" -> PlayerDivinityCapability.getOrCreate(player)
                    .setEventCooldownModifier((float) fx.number("value", 1.25),
                            System.currentTimeMillis() + durationTicks * 50L);

            // --- Approximations (no clean hook; closest vanilla analogue) ---
            case "ritual_failure_chance" -> addEffect(player, MobEffects.UNLUCK, durationTicks, 0);
            case "fire_damage_vulnerability" -> addEffect(player, MobEffects.GLOWING, durationTicks, 0);
            case "reduced_lifesteal", "reduced_healing" ->
                    addEffect(player, MobEffects.WEAKNESS, durationTicks, 0);
            case "reduced_attack_speed" -> addEffect(player, MobEffects.DIG_SLOWDOWN, durationTicks, 0);

            // --- Zone-scoped: not per-player; defer to a future area-effect system ---
            case "crop_wither", "structure_instability", "trade_disable" ->
                    SpellsNGodsMod.LOGGER.debug("Zone-scoped event effect '{}' not yet applied per player",
                            fx.type());

            default -> SpellsNGodsMod.LOGGER.debug("Unhandled divine-event effect '{}'", fx.type());
        }
    }

    private static void addEffect(ServerPlayer player, MobEffect effect, int durationTicks, int amplifier) {
        int ticks = durationTicks <= 0 ? 100 : durationTicks;
        player.addEffect(new MobEffectInstance(effect, ticks, Math.max(0, amplifier), true, true, true));
    }

    private static void teleportRandom(ServerPlayer player, int radius) {
        double r = Math.max(4.0, radius);
        double x = player.getX() + (player.getRandom().nextDouble() - 0.5) * 2.0 * r;
        double z = player.getZ() + (player.getRandom().nextDouble() - 0.5) * 2.0 * r;
        player.randomTeleport(x, player.getY(), z, true);
    }

    private static void knockback(ServerPlayer player, float strength) {
        float yaw = player.getYRot() * ((float) Math.PI / 180f);
        player.knockback(strength * 0.5, Math.sin(yaw), -Math.cos(yaw));
        player.hurtMarked = true;
    }

    private static DivineTier parseTier(String raw) {
        if (raw == null || raw.isBlank()) {
            return DivineTier.NONE;
        }
        try {
            return DivineTier.valueOf(raw.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return DivineTier.NONE;
        }
    }

    private static void sendDataDrivenTitle(ServerPlayer player, EventDefinition def, String sourceGodName,
                                            DivineEvent.EventSeverity severity) {
        ChatFormatting color = switch (severity) {
            case MINOR -> ChatFormatting.GRAY;
            case MODERATE -> ChatFormatting.YELLOW;
            case MAJOR -> ChatFormatting.GOLD;
            case LEGENDARY -> ChatFormatting.RED;
        };
        MutableComponent title = def.titleKey().isBlank()
                ? Component.literal(humanize(def.id().getPath()))
                : Component.translatable(def.titleKey());
        player.sendSystemMessage(Component.literal("[" + sourceGodName + "] ")
                .withStyle(ChatFormatting.DARK_PURPLE)
                .append(title.withStyle(color)));
    }

    private static void playPresentationSound(ServerPlayer player, EventDefinition def,
                                              DivineEvent.EventSeverity severity) {
        SoundEvent sound = def.sound().isBlank() ? null
                : BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.tryParse(def.sound()));
        if (sound != null) {
            float volume = 0.6f + severity.getIntensity() * 0.4f;
            player.level().playSound(null, player.blockPosition(), sound, SoundSource.AMBIENT, volume, 1.0f);
        } else {
            playOmenSound(player, severity);
        }
    }

    private static void spawnPresentationParticles(ServerPlayer player, EventDefinition def,
                                                   GodDefinition sourceGod, float intensity) {
        ParticleOptions particle = ParticleTypes.ENCHANT;
        if (!def.particles().isBlank()) {
            ResourceLocation pid = ResourceLocation.tryParse(def.particles());
            if (pid != null && BuiltInRegistries.PARTICLE_TYPE.get(pid) instanceof SimpleParticleType simple) {
                particle = simple;
            }
        }
        spawnEventParticles(player, sourceGod, particle, 8 + (int) (intensity * 12));
    }

    private static String humanize(String path) {
        String s = path.replace('_', ' ');
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private static void sendOmenMessage(ServerPlayer player, String sourceGod, String targetGod,
                                         DivineEvent.EventSeverity severity) {
        String message = switch (severity) {
            case MINOR -> "You sense " + sourceGod + "'s presence looming over " + targetGod + "'s domain...";
            case MODERATE -> "The power of " + sourceGod + " grows. " + targetGod + "'s influence wanes...";
            case MAJOR -> sourceGod + "'s dominion expands! " + targetGod + "'s faithful feel the pressure.";
            case LEGENDARY -> "The heavens tremble! " + sourceGod + " asserts divine supremacy over " + targetGod + "!";
        };

        ChatFormatting color = switch (severity) {
            case MINOR -> ChatFormatting.GRAY;
            case MODERATE -> ChatFormatting.YELLOW;
            case MAJOR -> ChatFormatting.GOLD;
            case LEGENDARY -> ChatFormatting.RED;
        };

        player.sendSystemMessage(Component.literal("[Omen] ").withStyle(ChatFormatting.DARK_PURPLE)
                .append(Component.literal(message).withStyle(color)));
    }

    private static void sendFaithTestMessage(ServerPlayer player, String sourceGod,
                                              DivineEvent.EventSeverity severity) {
        player.sendSystemMessage(Component.literal("[Faith Test] ").withStyle(ChatFormatting.GOLD)
                .append(Component.literal(sourceGod + " challenges your devotion. Stand firm!")
                        .withStyle(ChatFormatting.YELLOW)));
    }

    private static void sendTrialMessage(ServerPlayer player, String sourceGod,
                                          DivineEvent.EventSeverity severity) {
        player.sendSystemMessage(Component.literal("[Divine Trial] ").withStyle(ChatFormatting.RED)
                .append(Component.literal(sourceGod + " has sent a trial upon you. Prove your worth!")
                        .withStyle(ChatFormatting.DARK_RED)));
    }

    private static void sendCurseMessage(ServerPlayer player, String sourceGod,
                                          DivineEvent.EventSeverity severity) {
        player.sendSystemMessage(Component.literal("[Rival's Curse] ").withStyle(ChatFormatting.DARK_PURPLE)
                .append(Component.literal(sourceGod + "'s shadow falls upon you...")
                        .withStyle(ChatFormatting.LIGHT_PURPLE)));
    }

    private static void sendBlessingMessage(ServerPlayer player, String godName) {
        player.sendSystemMessage(Component.literal("[Divine Blessing] ").withStyle(ChatFormatting.AQUA)
                .append(Component.literal(godName + "'s favor shines upon you!")
                        .withStyle(ChatFormatting.WHITE)));
    }

    private static void sendManifestationMessage(ServerPlayer player, String godName) {
        player.sendSystemMessage(Component.literal("[Divine Manifestation] ").withStyle(ChatFormatting.LIGHT_PURPLE)
                .append(Component.literal("The presence of " + godName + " manifests in the mortal realm!")
                        .withStyle(ChatFormatting.WHITE)));
    }

    private static void spawnEventParticles(ServerPlayer player, GodDefinition sourceGod,
                                               net.minecraft.core.particles.ParticleOptions particle, int count) {
        if (!(player.level() instanceof ServerLevel serverLevel)) return;

        // Spawn the themed particle
        serverLevel.sendParticles(particle,
                player.getX(), player.getY() + 1.0, player.getZ(),
                count, 0.5, 0.5, 0.5, 0.05);

        // Also spawn school-colored dust for the source god
        if (sourceGod != null) {
            Vector3f color = SchoolColors.getSchoolColor(sourceGod.magicSchool());
            DustParticleOptions dust = new DustParticleOptions(color, 1.2f);
            serverLevel.sendParticles(dust,
                    player.getX(), player.getY() + 1.0, player.getZ(),
                    count / 2 + 1, 0.4, 0.4, 0.4, 0.02);
        }
    }

    private static void playOmenSound(ServerPlayer player, DivineEvent.EventSeverity severity) {
        float volume = 0.5f + (severity.getIntensity() * 0.5f);
        float pitch = 1.0f - (severity.getIntensity() * 0.3f);
        player.level().playSound(null, player.blockPosition(),
                SoundEvents.AMBIENT_CAVE.value(), SoundSource.AMBIENT, volume, pitch);
    }

    private static void playDramaticSound(ServerPlayer player) {
        player.level().playSound(null, player.blockPosition(),
                SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 0.5f, 1.5f);
    }

    /**
     * Clean up effects when an event expires.
     */
    public static void cleanup(DivineEvent event) {
        activeEffects.remove(event.eventId());
        SpellsNGodsMod.LOGGER.debug("Cleaned up divine event: {}", event.eventId());
        // Effects will naturally expire based on duration
        // If we need immediate removal, we'd track and remove specific effects here
    }

    /**
     * Force cleanup all active event effects for a player.
     */
    public static void cleanupForPlayer(ServerPlayer player) {
        // Remove any divine event debuffs
        // The effects will expire naturally, but this can be used for logout/disconnect
    }

    public static int getActiveEffectsCount() {
        return activeEffects.size();
    }
}
