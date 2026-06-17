package com.otectus.spells_n_gods.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.otectus.spells_n_gods.SpellsNGodsMod;
import com.otectus.spells_n_gods.apostasy.ApostasyHandler;
import com.otectus.spells_n_gods.apostasy.LatentCurseManager;
import com.otectus.spells_n_gods.apostasy.ScarEffectHandler;
import com.otectus.spells_n_gods.boss.GodBossEntity;
import com.otectus.spells_n_gods.capability.BlessingState;
import com.otectus.spells_n_gods.capability.CapabilityHandler;
import com.otectus.spells_n_gods.config.SpellsNGodsConfig;
import com.otectus.spells_n_gods.capability.DivineTier;
import com.otectus.spells_n_gods.capability.PlayerDivinityCapability;
import com.otectus.spells_n_gods.capability.PlayerDivinityData;
import com.otectus.spells_n_gods.capability.ScarData;
import com.otectus.spells_n_gods.content.RuneItem;
import com.otectus.spells_n_gods.data.GodDefinition;
import com.otectus.spells_n_gods.data.SpellsNGodsDataManager;
import com.otectus.spells_n_gods.registry.ModEntities;
import com.otectus.spells_n_gods.worldstate.GodWorldState;
import com.otectus.spells_n_gods.worldstate.StructureRecord;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.concurrent.TimeUnit;

public final class SpellsNGodsCommands {
    private SpellsNGodsCommands() {
    }

    private static final SuggestionProvider<CommandSourceStack> GOD_SUGGESTIONS = (ctx, builder) -> {
        return SharedSuggestionProvider.suggest(
                SpellsNGodsDataManager.getGods().keySet().stream().map(ResourceLocation::toString),
                builder
        );
    };

    /**
     * Resolve a god ID string to a GodDefinition, trying multiple namespace strategies:
     * 1. Exact match as-is (e.g. "spells_n_gods:bella")
     * 2. With "spells_n_gods:" prefix if no namespace given (e.g. "bella" → "spells_n_gods:bella")
     */
    private static Map.Entry<ResourceLocation, GodDefinition> resolveGod(String godIdStr) {
        Map<ResourceLocation, GodDefinition> gods = SpellsNGodsDataManager.getGods();

        // Try exact match first
        ResourceLocation exactId = ResourceLocation.tryParse(godIdStr);
        if (exactId != null && gods.containsKey(exactId)) {
            return Map.entry(exactId, gods.get(exactId));
        }

        // Try with spells_n_gods: prefix if no namespace was provided
        if (!godIdStr.contains(":")) {
            ResourceLocation namespacedId = new ResourceLocation(SpellsNGodsMod.MODID, godIdStr);
            if (gods.containsKey(namespacedId)) {
                return Map.entry(namespacedId, gods.get(namespacedId));
            }
        }

        return null;
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("spells_n_gods")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("listgods")
                        .executes(ctx -> {
                            var gods = SpellsNGodsDataManager.getGods();
                            if (gods.isEmpty()) {
                                ctx.getSource().sendSuccess(() -> Component.literal("No gods loaded! The data pack may not have been applied.").withStyle(ChatFormatting.RED), false);
                                ctx.getSource().sendSuccess(() -> Component.literal("Try /reload to refresh data packs, then /spellsngods listgods again.").withStyle(ChatFormatting.YELLOW), false);
                                return 0;
                            }
                            int count = 0;
                            for (var entry : gods.entrySet()) {
                                GodDefinition def = entry.getValue();
                                ctx.getSource().sendSuccess(() -> Component.literal(entry.getKey() + " - " + def.displayName())
                                        .withStyle(ChatFormatting.GREEN), false);
                                count++;
                            }
                            final int finalCount = count;
                            ctx.getSource().sendSuccess(() -> Component.literal("Loaded gods: " + finalCount).withStyle(ChatFormatting.GOLD), false);
                            return count;
                        }))
                .then(Commands.literal("setrune")
                        .then(Commands.argument("god_id", StringArgumentType.string())
                                .suggests(GOD_SUGGESTIONS)
                                .executes(ctx -> {
                                    String godIdStr = StringArgumentType.getString(ctx, "god_id");
                                    try {
                                        var resolved = resolveGod(godIdStr);
                                        if (resolved == null) {
                                            ctx.getSource().sendFailure(Component.literal("Unknown god: " + godIdStr));
                                            return 0;
                                        }
                                        ResourceLocation godId = resolved.getKey();
                                        GodDefinition god = resolved.getValue();

                                        ServerPlayer player = ctx.getSource().getPlayerOrException();
                                        ItemStack rune = RuneItem.createForGod(godId);
                                        player.getInventory().add(rune);

                                        ctx.getSource().sendSuccess(() -> Component.literal("Gave rune of " + god.displayName()), true);
                                        return 1;
                                    } catch (Exception e) {
                                        SpellsNGodsMod.LOGGER.error("[SpellsNGods] Error in setrune command", e);
                                        ctx.getSource().sendFailure(Component.literal("Error: " + e.getMessage()));
                                        return 0;
                                    }
                                })))
                .then(Commands.literal("getstatus")
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            PlayerDivinityData data = PlayerDivinityCapability.getOrCreate(player);

                            ctx.getSource().sendSuccess(() -> Component.literal("=== Divine Status ===").withStyle(ChatFormatting.GOLD), false);

                            if (data.getChosenGodId() == null) {
                                ctx.getSource().sendSuccess(() -> Component.literal("God: Unbound").withStyle(ChatFormatting.GRAY), false);
                            } else {
                                GodDefinition god = SpellsNGodsDataManager.getGods().get(new ResourceLocation(data.getChosenGodId()));
                                String godName = god != null ? god.displayName() : data.getChosenGodId();
                                ctx.getSource().sendSuccess(() -> Component.literal("God: " + godName).withStyle(ChatFormatting.AQUA), false);
                            }

                            ctx.getSource().sendSuccess(() -> Component.literal("Favor: " + String.format("%.2f", data.getFavor())), false);
                            ctx.getSource().sendSuccess(() -> Component.literal("Tier: " + data.getCurrentTier().name()), false);
                            ctx.getSource().sendSuccess(() -> Component.literal("State: " + data.getBlessingState().name()), false);

                            if (data.getLastPrayerEpochMs() > 0) {
                                long hoursSincePrayer = TimeUnit.MILLISECONDS.toHours(System.currentTimeMillis() - data.getLastPrayerEpochMs());
                                ctx.getSource().sendSuccess(() -> Component.literal("Last Prayer: " + hoursSincePrayer + " hours ago"), false);
                            }

                            if (data.getLastOfferingEpochMs() > 0) {
                                long hoursSinceOffering = TimeUnit.MILLISECONDS.toHours(System.currentTimeMillis() - data.getLastOfferingEpochMs());
                                ctx.getSource().sendSuccess(() -> Component.literal("Last Offering: " + hoursSinceOffering + " hours ago"), false);
                            }

                            return 1;
                        }))
                .then(Commands.literal("setfavor")
                        .then(Commands.argument("amount", FloatArgumentType.floatArg(0))
                                .executes(ctx -> {
                                    float amount = FloatArgumentType.getFloat(ctx, "amount");
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    PlayerDivinityData data = PlayerDivinityCapability.getOrCreate(player);

                                    data.setFavor(amount);

                                    if (data.getChosenGodId() != null) {
                                        GodDefinition god = SpellsNGodsDataManager.getGods().get(new ResourceLocation(data.getChosenGodId()));
                                        if (god != null) {
                                            data.setCurrentTier(DivineTier.computeFromFavor(amount, god));
                                        }
                                    }

                                    CapabilityHandler.syncToClient(player);

                                    ctx.getSource().sendSuccess(() -> Component.literal("Set favor to " + amount + " (Tier: " + data.getCurrentTier().name() + ")"), true);
                                    return 1;
                                })))
                .then(Commands.literal("unbind")
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            PlayerDivinityData data = PlayerDivinityCapability.getOrCreate(player);

                            String oldGod = data.getChosenGodId();
                            data.reset();

                            CapabilityHandler.syncToClient(player);

                            if (oldGod != null) {
                                ctx.getSource().sendSuccess(() -> Component.literal("Unbound from " + oldGod).withStyle(ChatFormatting.YELLOW), true);
                            } else {
                                ctx.getSource().sendSuccess(() -> Component.literal("Player was already unbound").withStyle(ChatFormatting.GRAY), false);
                            }
                            return 1;
                        }))
                .then(Commands.literal("setstate")
                        .then(Commands.argument("state", StringArgumentType.word())
                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                                        java.util.Arrays.stream(BlessingState.values()).map(Enum::name),
                                        builder
                                ))
                                .executes(ctx -> {
                                    String stateStr = StringArgumentType.getString(ctx, "state");
                                    BlessingState state;
                                    try {
                                        state = BlessingState.valueOf(stateStr.toUpperCase());
                                    } catch (IllegalArgumentException e) {
                                        ctx.getSource().sendFailure(Component.literal("Invalid state: " + stateStr));
                                        return 0;
                                    }

                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    PlayerDivinityData data = PlayerDivinityCapability.getOrCreate(player);
                                    data.setBlessingState(state);

                                    CapabilityHandler.syncToClient(player);

                                    ctx.getSource().sendSuccess(() -> Component.literal("Set blessing state to " + state.name()), true);
                                    return 1;
                                })))
                .then(Commands.literal("refreshoffering")
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            PlayerDivinityData data = PlayerDivinityCapability.getOrCreate(player);

                            data.setLastOfferingEpochMs(System.currentTimeMillis());

                            if (data.getBlessingState() == BlessingState.HALTED) {
                                data.setBlessingState(BlessingState.ACTIVE);
                            }

                            CapabilityHandler.syncToClient(player);

                            ctx.getSource().sendSuccess(() -> Component.literal("Refreshed offering timer").withStyle(ChatFormatting.GREEN), true);
                            return 1;
                        }))
                // Apostasy commands
                .then(Commands.literal("apostasy")
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            ApostasyHandler.ApostasyResult result = ApostasyHandler.forceApostasy(player);

                            if (result.success()) {
                                CapabilityHandler.syncToClient(player);
                                ctx.getSource().sendSuccess(() -> Component.literal("Apostasy completed").withStyle(ChatFormatting.RED), true);
                            } else {
                                ctx.getSource().sendFailure(Component.translatable(result.messageKey(), result.messageArgs()));
                            }
                            return result.success() ? 1 : 0;
                        }))
                .then(Commands.literal("clearcurse")
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            PlayerDivinityData data = PlayerDivinityCapability.getOrCreate(player);

                            if (!data.hasLatentCurse()) {
                                ctx.getSource().sendSuccess(() -> Component.literal("No active curse to clear").withStyle(ChatFormatting.GRAY), false);
                                return 0;
                            }

                            LatentCurseManager.clearCurse(player);
                            CapabilityHandler.syncToClient(player);

                            ctx.getSource().sendSuccess(() -> Component.literal("Cleared latent curse").withStyle(ChatFormatting.GREEN), true);
                            return 1;
                        }))
                .then(Commands.literal("clearscars")
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            PlayerDivinityData data = PlayerDivinityCapability.getOrCreate(player);

                            int scarCount = data.getScarData().getScarCount();
                            if (scarCount == 0) {
                                ctx.getSource().sendSuccess(() -> Component.literal("No scars to clear").withStyle(ChatFormatting.GRAY), false);
                                return 0;
                            }

                            data.getScarData().clear();
                            ScarEffectHandler.recomputeScarEffects(player);
                            CapabilityHandler.syncToClient(player);

                            final int count = scarCount;
                            ctx.getSource().sendSuccess(() -> Component.literal("Cleared " + count + " scars").withStyle(ChatFormatting.GREEN), true);
                            return 1;
                        }))
                .then(Commands.literal("scarstatus")
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            PlayerDivinityData data = PlayerDivinityCapability.getOrCreate(player);
                            ScarData scars = data.getScarData();

                            ctx.getSource().sendSuccess(() -> Component.literal("=== Scar Status ===").withStyle(ChatFormatting.DARK_RED), false);
                            ctx.getSource().sendSuccess(() -> Component.literal("Apostasy Count: " + scars.getScarCount()), false);

                            if (scars.getScarCount() > 0) {
                                ctx.getSource().sendSuccess(() -> Component.literal("Cumulative Penalties:").withStyle(ChatFormatting.YELLOW), false);
                                ctx.getSource().sendSuccess(() -> Component.literal("  Health: -" + String.format("%.1f%%", scars.getTotalHealthReduction() * 100)), false);
                                ctx.getSource().sendSuccess(() -> Component.literal("  XP Gain: -" + String.format("%.1f%%", scars.getTotalXpPenalty() * 100)), false);
                                ctx.getSource().sendSuccess(() -> Component.literal("  Death Penalty: +" + String.format("%.1f%%", scars.getTotalDeathPenalty() * 100)), false);
                                ctx.getSource().sendSuccess(() -> Component.literal("  Luck: -" + String.format("%.1f%%", scars.getTotalLuckReduction() * 100)), false);
                                ctx.getSource().sendSuccess(() -> Component.literal("  Durability Loss: +" + String.format("%.1f%%", scars.getTotalDurabilityPenalty() * 100)), false);
                            }

                            if (data.hasLatentCurse()) {
                                long remainingHours = LatentCurseManager.getRemainingCurseHours(player);
                                ctx.getSource().sendSuccess(() -> Component.literal("Curse remaining: " + remainingHours + " hours").withStyle(ChatFormatting.DARK_PURPLE), false);
                            }

                            return 1;
                        }))
                .then(Commands.literal("fullreset")
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            PlayerDivinityData data = PlayerDivinityCapability.getOrCreate(player);

                            data.fullReset();
                            ScarEffectHandler.recomputeScarEffects(player);
                            CapabilityHandler.syncToClient(player);

                            ctx.getSource().sendSuccess(() -> Component.literal("Full reset complete (including scars)").withStyle(ChatFormatting.GOLD), true);
                            return 1;
                        }))
                // Boss commands
                .then(Commands.literal("boss")
                        .then(Commands.literal("spawn")
                                .then(Commands.argument("god_id", StringArgumentType.string())
                                        .suggests(GOD_SUGGESTIONS)
                                        .executes(ctx -> {
                                            String godIdStr = StringArgumentType.getString(ctx, "god_id");
                                            try {
                                                SpellsNGodsMod.LOGGER.info("[SpellsNGods] /spellsngods boss spawn '{}' — attempting...", godIdStr);

                                                var resolved = resolveGod(godIdStr);
                                                if (resolved == null) {
                                                    // List available gods for debugging
                                                    var keys = SpellsNGodsDataManager.getGods().keySet();
                                                    SpellsNGodsMod.LOGGER.warn("[SpellsNGods] God '{}' not found. Available: {}", godIdStr, keys);
                                                    ctx.getSource().sendFailure(Component.literal("Unknown god: " + godIdStr + ". Available: " + keys));
                                                    return 0;
                                                }

                                                ResourceLocation godId = resolved.getKey();
                                                GodDefinition god = resolved.getValue();
                                                SpellsNGodsMod.LOGGER.info("[SpellsNGods] Resolved god: {} ('{}')", godId, god.displayName());

                                                ServerPlayer player = ctx.getSource().getPlayerOrException();
                                                ServerLevel level = player.serverLevel();

                                                SpellsNGodsMod.LOGGER.info("[SpellsNGods] Creating GodBossEntity...");
                                                GodBossEntity boss = ModEntities.GOD_BOSS.get().create(level);
                                                if (boss == null) {
                                                    SpellsNGodsMod.LOGGER.error("[SpellsNGods] ModEntities.GOD_BOSS.get().create(level) returned null!");
                                                    ctx.getSource().sendFailure(Component.literal("Failed to create boss entity — EntityType.create returned null."));
                                                    return 0;
                                                }
                                                SpellsNGodsMod.LOGGER.info("[SpellsNGods] Entity created successfully: {}", boss);

                                                boss.moveTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), 0);
                                                boss.setGodId(godId.toString());
                                                SpellsNGodsMod.LOGGER.info("[SpellsNGods] Set god ID to '{}', applying stats...", godId);
                                                boss.applyGodStats();
                                                SpellsNGodsMod.LOGGER.info("[SpellsNGods] Stats applied. Health={}/{}, adding to world...",
                                                        boss.getHealth(), boss.getMaxHealth());

                                                boolean added = level.addFreshEntity(boss);
                                                SpellsNGodsMod.LOGGER.info("[SpellsNGods] addFreshEntity returned: {}. Entity ID: {}, UUID: {}",
                                                        added, boss.getId(), boss.getUUID());

                                                if (added) {
                                                    ctx.getSource().sendSuccess(() -> Component.literal("Spawned " + god.displayName() + " boss at your location.")
                                                            .withStyle(ChatFormatting.GREEN), true);
                                                    return 1;
                                                } else {
                                                    ctx.getSource().sendFailure(Component.literal("addFreshEntity returned false — boss may have been rejected by the world."));
                                                    return 0;
                                                }
                                            } catch (Exception e) {
                                                SpellsNGodsMod.LOGGER.error("[SpellsNGods] Exception in boss spawn command for '{}'", godIdStr, e);
                                                ctx.getSource().sendFailure(Component.literal("Error spawning boss: " + e.getMessage()));
                                                return 0;
                                            }
                                        })))
                        .then(Commands.literal("kill")
                                .then(Commands.argument("god_id", StringArgumentType.string())
                                        .suggests(GOD_SUGGESTIONS)
                                        .executes(ctx -> {
                                            String godIdStr = StringArgumentType.getString(ctx, "god_id");
                                            try {
                                                var resolved = resolveGod(godIdStr);
                                                String resolvedId = resolved != null ? resolved.getKey().toString() : godIdStr;
                                                String displayName = resolved != null ? resolved.getValue().displayName() : godIdStr;

                                                ServerLevel level = ctx.getSource().getLevel();
                                                GodWorldState state = GodWorldState.get(level.getServer());
                                                var optRecord = state.getStructure(resolvedId);

                                                if (optRecord.isEmpty() || !optRecord.get().bossAlive()) {
                                                    ctx.getSource().sendFailure(Component.literal(displayName + " boss is not currently alive (or has no temple)."));
                                                    return 0;
                                                }

                                                StructureRecord record = optRecord.get();
                                                if (record.bossEntityUUID() != null) {
                                                    var entity = level.getEntity(record.bossEntityUUID());
                                                    if (entity instanceof GodBossEntity bossEntity) {
                                                        bossEntity.kill();
                                                    }
                                                }
                                                state.markBossAlive(resolvedId, false);

                                                ctx.getSource().sendSuccess(() -> Component.literal("Killed " + displayName + " boss.").withStyle(ChatFormatting.YELLOW), true);
                                                return 1;
                                            } catch (Exception e) {
                                                SpellsNGodsMod.LOGGER.error("[SpellsNGods] Error in boss kill command", e);
                                                ctx.getSource().sendFailure(Component.literal("Error: " + e.getMessage()));
                                                return 0;
                                            }
                                        })))
                        .then(Commands.literal("locate")
                                .then(Commands.argument("god_id", StringArgumentType.string())
                                        .suggests(GOD_SUGGESTIONS)
                                        .executes(ctx -> {
                                            String godIdStr = StringArgumentType.getString(ctx, "god_id");
                                            try {
                                                var resolved = resolveGod(godIdStr);
                                                String resolvedId = resolved != null ? resolved.getKey().toString() : godIdStr;

                                                GodWorldState state = GodWorldState.get(ctx.getSource().getServer());
                                                var optRecord = state.getStructure(resolvedId);

                                                if (optRecord.isEmpty()) {
                                                    ctx.getSource().sendFailure(Component.literal("No temple found for " + godIdStr +
                                                            ". Temples generate naturally in the world — you may need to explore further."));
                                                    return 0;
                                                }

                                                StructureRecord record = optRecord.get();
                                                BlockPos pos = record.center();
                                                ctx.getSource().sendSuccess(() -> Component.literal(
                                                        godIdStr + " temple at [" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + "] in " + record.dimension()
                                                ).withStyle(ChatFormatting.AQUA), false);
                                                return 1;
                                            } catch (Exception e) {
                                                SpellsNGodsMod.LOGGER.error("[SpellsNGods] Error in boss locate command", e);
                                                ctx.getSource().sendFailure(Component.literal("Error: " + e.getMessage()));
                                                return 0;
                                            }
                                        })))
                        .then(Commands.literal("info")
                                .then(Commands.argument("god_id", StringArgumentType.string())
                                        .suggests(GOD_SUGGESTIONS)
                                        .executes(ctx -> {
                                            String godIdStr = StringArgumentType.getString(ctx, "god_id");
                                            try {
                                                var resolved = resolveGod(godIdStr);
                                                String resolvedId = resolved != null ? resolved.getKey().toString() : godIdStr;

                                                GodWorldState state = GodWorldState.get(ctx.getSource().getServer());
                                                var optRecord = state.getStructure(resolvedId);

                                                if (optRecord.isEmpty()) {
                                                    ctx.getSource().sendFailure(Component.literal("No temple found for " + godIdStr + "."));
                                                    return 0;
                                                }

                                                StructureRecord record = optRecord.get();
                                                long gameTime = ctx.getSource().getLevel().getGameTime();
                                                long respawnIn = record.bossAlive() ? 0 : Math.max(0, record.respawnDueTime() - gameTime);

                                                ctx.getSource().sendSuccess(() -> Component.literal(
                                                        godIdStr + " - Alive: " + (record.bossAlive() ? "Yes" : "No") +
                                                                " | Respawn in: " + respawnIn + " ticks"
                                                ).withStyle(ChatFormatting.GOLD), false);
                                                return 1;
                                            } catch (Exception e) {
                                                SpellsNGodsMod.LOGGER.error("[SpellsNGods] Error in boss info command", e);
                                                ctx.getSource().sendFailure(Component.literal("Error: " + e.getMessage()));
                                                return 0;
                                            }
                                        })))
                        .then(Commands.literal("respawnall")
                                .executes(ctx -> {
                                    try {
                                        ServerLevel level = ctx.getSource().getLevel();
                                        GodWorldState state = GodWorldState.get(level.getServer());

                                        int count = 0;
                                        for (StructureRecord record : state.getAllStructures()) {
                                            if (!record.bossAlive()) {
                                                state.markBossSpawned(record.godId(), null);
                                                count++;
                                            }
                                        }

                                        final int finalCount = count;
                                        if (finalCount == 0) {
                                            ctx.getSource().sendSuccess(() -> Component.literal("No dead bosses to respawn (structures found: " +
                                                    state.getAllStructures().size() + ").").withStyle(ChatFormatting.GRAY), false);
                                        } else {
                                            ctx.getSource().sendSuccess(() -> Component.literal("Forced respawn of " + finalCount +
                                                    " bosses. They will appear on the next tick cycle.").withStyle(ChatFormatting.GREEN), true);
                                        }
                                        return count;
                                    } catch (Exception e) {
                                        SpellsNGodsMod.LOGGER.error("[SpellsNGods] Error in respawnall command", e);
                                        ctx.getSource().sendFailure(Component.literal("Error: " + e.getMessage()));
                                        return 0;
                                    }
                                })))
                // Structure commands
                .then(Commands.literal("structure")
                        .then(Commands.literal("list")
                                .executes(ctx -> {
                                    try {
                                        GodWorldState state = GodWorldState.get(ctx.getSource().getServer());
                                        var structures = state.getAllStructures();

                                        if (structures.isEmpty()) {
                                            ctx.getSource().sendSuccess(() -> Component.literal("No structures recorded. God temples generate naturally in the world — " +
                                                    "you need to explore to discover them, or use /locate structure spells_n_gods:god_temple_bella.").withStyle(ChatFormatting.GRAY), false);
                                            return 0;
                                        }

                                        ctx.getSource().sendSuccess(() -> Component.literal("=== Recorded Structures ===").withStyle(ChatFormatting.GOLD), false);
                                        for (StructureRecord r : structures) {
                                            BlockPos p = r.center();
                                            ctx.getSource().sendSuccess(() -> Component.literal(
                                                    "  " + r.godId() + ": [" + p.getX() + ", " + p.getY() + ", " + p.getZ() + "] in " + r.dimension() +
                                                            " (alive=" + r.bossAlive() + ")"
                                            ), false);
                                        }
                                        return structures.size();
                                    } catch (Exception e) {
                                        SpellsNGodsMod.LOGGER.error("[SpellsNGods] Error in structure list command", e);
                                        ctx.getSource().sendFailure(Component.literal("Error: " + e.getMessage()));
                                        return 0;
                                    }
                                }))
                        .then(Commands.literal("protect")
                                .then(Commands.literal("on")
                                        .executes(ctx -> {
                                            SpellsNGodsConfig.COMMON.structureProtection.set(true);
                                            ctx.getSource().sendSuccess(() -> Component.literal("Structure protection enabled").withStyle(ChatFormatting.GREEN), true);
                                            return 1;
                                        }))
                                .then(Commands.literal("off")
                                        .executes(ctx -> {
                                            SpellsNGodsConfig.COMMON.structureProtection.set(false);
                                            ctx.getSource().sendSuccess(() -> Component.literal("Structure protection disabled").withStyle(ChatFormatting.YELLOW), true);
                                            return 1;
                                        }))))
                // Data-driven tag/tier spawning diagnostics
                .then(Commands.literal("spawning")
                        .then(Commands.literal("here")
                                .executes(ctx -> {
                                    try {
                                        ServerPlayer player = ctx.getSource().getPlayerOrException();
                                        ServerLevel level = player.serverLevel();
                                        BlockPos pos = player.blockPosition();
                                        var registry = level.registryAccess().registryOrThrow(
                                                net.minecraft.core.registries.Registries.STRUCTURE);
                                        var here = level.structureManager().getAllStructuresAt(pos);

                                        ctx.getSource().sendSuccess(() -> Component.literal(
                                                "=== Structures at your position ===").withStyle(ChatFormatting.GOLD), false);
                                        if (here.isEmpty()) {
                                            ctx.getSource().sendSuccess(() -> Component.literal(
                                                    "  (none — stand inside a structure)").withStyle(ChatFormatting.GRAY), false);
                                            return 0;
                                        }
                                        int shown = 0;
                                        for (var structure : here.keySet()) {
                                            var start = level.structureManager().getStructureAt(pos, structure);
                                            if (start == null || !start.isValid()) continue;
                                            var id = registry.getKey(structure);
                                            if (id == null) continue;
                                            var holder = registry.wrapAsHolder(structure);
                                            StringBuilder tags = new StringBuilder();
                                            for (var tag : com.otectus.spells_n_gods.spawning.DomainStructureTags.DOMAIN_TAGS) {
                                                if (holder.is(tag)) {
                                                    tags.append(" #").append(tag.location());
                                                }
                                            }
                                            String line = "  " + id + (tags.length() == 0 ? " (no domain tags)" : " ->" + tags);
                                            ctx.getSource().sendSuccess(() -> Component.literal(line)
                                                    .withStyle(ChatFormatting.AQUA), false);
                                            shown++;
                                        }
                                        return shown;
                                    } catch (Exception e) {
                                        ctx.getSource().sendFailure(Component.literal("Error: " + e.getMessage()));
                                        return 0;
                                    }
                                }))
                        .then(Commands.literal("encounters")
                                .executes(ctx -> {
                                    var server = ctx.getSource().getServer();
                                    var state = com.otectus.spells_n_gods.worldstate.StructureEncounterState.get(server);
                                    long now = server.overworld().getGameTime();
                                    ctx.getSource().sendSuccess(() -> Component.literal(
                                            "=== Encounter records: " + state.size() + " ===").withStyle(ChatFormatting.GOLD), false);
                                    for (var r : state.all()) {
                                        String line = "  " + r.structureId() + " active=" + r.activeDeityCount()
                                                + " spawned=" + r.spawnedCount()
                                                + (r.isPermanentlyExhausted() ? " [EXHAUSTED]" : "")
                                                + " key=" + r.key();
                                        ctx.getSource().sendSuccess(() -> Component.literal(line), false);
                                    }
                                    return state.size();
                                }))
                        .then(Commands.literal("prune")
                                .executes(ctx -> {
                                    var server = ctx.getSource().getServer();
                                    var state = com.otectus.spells_n_gods.worldstate.StructureEncounterState.get(server);
                                    int removed = state.prune(server.overworld().getGameTime());
                                    ctx.getSource().sendSuccess(() -> Component.literal(
                                            "Pruned " + removed + " expired encounter record(s).")
                                            .withStyle(ChatFormatting.GREEN), true);
                                    return removed;
                                })))
                // Force-spawn: bare entity test (no god lookup, just entity creation)
                .then(Commands.literal("testspawn")
                        .executes(ctx -> {
                            try {
                                ServerPlayer player = ctx.getSource().getPlayerOrException();
                                ServerLevel level = player.serverLevel();

                                SpellsNGodsMod.LOGGER.info("[SpellsNGods] /spellsngods testspawn — creating bare GodBossEntity...");
                                GodBossEntity boss = ModEntities.GOD_BOSS.get().create(level);
                                if (boss == null) {
                                    SpellsNGodsMod.LOGGER.error("[SpellsNGods] testspawn: create() returned null");
                                    ctx.getSource().sendFailure(Component.literal("Entity creation returned null."));
                                    return 0;
                                }

                                boss.moveTo(player.getX() + 2, player.getY(), player.getZ(), 0, 0);
                                // Don't set god ID — just spawn with default attributes
                                boolean added = level.addFreshEntity(boss);
                                SpellsNGodsMod.LOGGER.info("[SpellsNGods] testspawn: addFreshEntity={}, id={}, pos={},{},{}",
                                        added, boss.getId(), boss.getX(), boss.getY(), boss.getZ());

                                ctx.getSource().sendSuccess(() -> Component.literal("Spawned bare GodBossEntity (no god). Added=" + added +
                                        " HP=" + boss.getHealth() + "/" + boss.getMaxHealth()).withStyle(ChatFormatting.YELLOW), false);
                                return 1;
                            } catch (Exception e) {
                                SpellsNGodsMod.LOGGER.error("[SpellsNGods] testspawn exception", e);
                                ctx.getSource().sendFailure(Component.literal("Exception: " + e.getClass().getSimpleName() + ": " + e.getMessage()));
                                return 0;
                            }
                        }))
                // Debug/diagnostics command
                .then(Commands.literal("debug")
                        .executes(ctx -> {
                            ctx.getSource().sendSuccess(() -> Component.literal("=== Spells n' Gods Debug ===").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);

                            // Gods data
                            var gods = SpellsNGodsDataManager.getGods();
                            ctx.getSource().sendSuccess(() -> Component.literal("Gods loaded: " + gods.size()).withStyle(
                                    gods.isEmpty() ? ChatFormatting.RED : ChatFormatting.GREEN), false);
                            for (var key : gods.keySet()) {
                                GodDefinition god = gods.get(key);
                                ctx.getSource().sendSuccess(() -> Component.literal("  " + key + " (" + god.displayName() + ") - HP:" +
                                        god.boss().maxHealth() + " ATK:" + god.boss().attackDamage() +
                                        " Weapon:" + god.boss().weaponId()), false);
                            }

                            // Structures
                            GodWorldState state = GodWorldState.get(ctx.getSource().getServer());
                            var structures = state.getAllStructures();
                            ctx.getSource().sendSuccess(() -> Component.literal("Structures registered: " + structures.size()).withStyle(
                                    structures.isEmpty() ? ChatFormatting.RED : ChatFormatting.GREEN), false);
                            for (StructureRecord r : structures) {
                                BlockPos p = r.center();
                                ctx.getSource().sendSuccess(() -> Component.literal("  " + r.godId() + " at " +
                                        p.getX() + "," + p.getY() + "," + p.getZ() +
                                        " dim=" + r.dimension() +
                                        " alive=" + r.bossAlive() +
                                        " respawnDue=" + r.respawnDueTime()), false);
                            }

                            // Compat mods
                            ctx.getSource().sendSuccess(() -> Component.literal("--- Mod Compat ---").withStyle(ChatFormatting.AQUA), false);
                            ctx.getSource().sendSuccess(() -> Component.literal("  SimplySwords: " +
                                    com.otectus.spells_n_gods.compat.ModCompatHandler.SIMPLY_SWORDS_LOADED), false);
                            ctx.getSource().sendSuccess(() -> Component.literal("  Iron's Spells: " +
                                    com.otectus.spells_n_gods.compat.ModCompatHandler.IRONS_SPELLS_LOADED), false);
                            ctx.getSource().sendSuccess(() -> Component.literal("  GeckoLib: " +
                                    com.otectus.spells_n_gods.compat.ModCompatHandler.JADE_LOADED + " (Jade)"), false);

                            return 1;
                        }))
        );
    }
}
