package com.otectus.spells_n_gods.apostasy;

import com.otectus.spells_n_gods.SpellsNGodsMod;
import com.otectus.spells_n_gods.capability.BlessingState;
import com.otectus.spells_n_gods.capability.DivineTier;
import com.otectus.spells_n_gods.capability.PlayerDivinityCapability;
import com.otectus.spells_n_gods.capability.PlayerDivinityData;
import com.otectus.spells_n_gods.capability.ScarData;
import com.otectus.spells_n_gods.compat.SpellsNGodsEvents;
import com.otectus.spells_n_gods.config.SpellsNGodsConfig;
import com.otectus.spells_n_gods.content.MonumentBlockEntity;
import com.otectus.spells_n_gods.content.RuinedIdolBlockEntity;
import com.otectus.spells_n_gods.data.ApostasyDefinition;
import com.otectus.spells_n_gods.data.GodDefinition;
import com.otectus.spells_n_gods.data.SpellsNGodsDataManager;
import com.otectus.spells_n_gods.effect.EffectProfileCache;
import com.otectus.spells_n_gods.animation.PlayerAnimationType;
import com.otectus.spells_n_gods.network.DivineVfxPacket;
import com.otectus.spells_n_gods.network.ModNetwork;
import com.otectus.spells_n_gods.network.PlayerAnimationPacket;
import com.otectus.spells_n_gods.registry.ModBlocks;
import net.minecraftforge.common.MinecraftForge;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Central handler for the apostasy process.
 * Orchestrates desecration, trials, scar application, and state transitions.
 */
public class ApostasyHandler {

    /**
     * Result of an apostasy operation.
     */
    public record ApostasyResult(boolean success, String messageKey, Object... messageArgs) {
        public static ApostasyResult ok(String messageKey, Object... args) {
            return new ApostasyResult(true, messageKey, args);
        }

        public static ApostasyResult error(String messageKey, Object... args) {
            return new ApostasyResult(false, messageKey, args);
        }
    }

    /**
     * Check if player can initiate apostasy.
     */
    public static ApostasyResult canInitiateApostasy(ServerPlayer player, MonumentBlockEntity monument) {
        // Check if apostasy is enabled
        if (!SpellsNGodsConfig.COMMON.allowApostasy.get()) {
            return ApostasyResult.error("spells_n_gods.apostasy.disabled");
        }

        PlayerDivinityData data = PlayerDivinityCapability.getOrCreate(player);

        // Check if player is bound
        if (!data.isBound()) {
            return ApostasyResult.error("spells_n_gods.apostasy.not_bound");
        }

        // Check if already in trial
        if (data.isInApostasyTrial()) {
            return ApostasyResult.error("spells_n_gods.apostasy.already_in_trial");
        }

        // Check if player owns this monument
        if (monument != null && !monument.getOwner().equals(player.getUUID())) {
            return ApostasyResult.error("spells_n_gods.apostasy.not_your_monument");
        }

        // Check if monument matches player's god
        if (monument != null && data.getChosenGodId() != null) {
            if (!data.getChosenGodId().equals(monument.getGodId())) {
                return ApostasyResult.error("spells_n_gods.apostasy.wrong_monument");
            }
        }

        return ApostasyResult.ok("spells_n_gods.apostasy.can_proceed");
    }

    /**
     * Initiate the apostasy process.
     * If trials are enabled, starts the trial. Otherwise, completes immediately.
     */
    public static ApostasyResult initiateApostasy(ServerPlayer player, MonumentBlockEntity monument) {
        ApostasyResult canProceed = canInitiateApostasy(player, monument);
        if (!canProceed.success()) {
            return canProceed;
        }

        PlayerDivinityData data = PlayerDivinityCapability.getOrCreate(player);
        String godId = data.getChosenGodId();

        // Store the god being abandoned
        data.setLastAbandonedGodId(godId);

        // Store monument position for later transformation
        if (monument != null) {
            BlockPos pos = monument.getBlockPos();
            String dimKey = player.level().dimension().location().toString();
            data.setApostasyMonumentPos(dimKey, pos.getX(), pos.getY(), pos.getZ());
        }

        // Check if trials are enabled
        if (SpellsNGodsConfig.SERVER.enableApostasyTrials.get()) {
            // Start the trial system
            data.setInApostasyTrial(true);
            data.setCurrentTrialPhase(1);
            data.setTrialPhaseStartMs(System.currentTimeMillis());

            player.sendSystemMessage(Component.translatable("spells_n_gods.apostasy.trial_starting"));

            GodDefinition god = SpellsNGodsDataManager.getGods().get(new ResourceLocation(godId));
            if (god != null) {
                ApostasyTrialManager.startTrials(player, god);
            }

            // Send apostasy trial start VFX and struggle animation
            ModNetwork.sendToPlayer(player, new DivineVfxPacket(
                    DivineVfxPacket.VfxType.APOSTASY_TRIAL_START,
                    player.getX(), player.getY(), player.getZ(),
                    "", 0));
            ModNetwork.sendToTrackingAndSelf(player, new PlayerAnimationPacket(
                    player.getUUID(), PlayerAnimationType.APOSTASY_STRUGGLE, PlayerAnimationPacket.Action.PLAY));

            SpellsNGodsMod.LOGGER.info("Player {} started apostasy trials for god {}",
                    player.getName().getString(), godId);

            return ApostasyResult.ok("spells_n_gods.apostasy.trial_started");
        } else {
            // Complete immediately without trials
            return completeApostasy(player);
        }
    }

    /**
     * Complete the apostasy process.
     * Applies scars, curses, and transitions to UNBOUND_COOLDOWN state.
     */
    public static ApostasyResult completeApostasy(ServerPlayer player) {
        PlayerDivinityData data = PlayerDivinityCapability.getOrCreate(player);
        String godId = data.getLastAbandonedGodId();
        if (godId == null) {
            godId = data.getChosenGodId();
        }

        if (godId == null) {
            return ApostasyResult.error("spells_n_gods.apostasy.no_god");
        }

        GodDefinition god = SpellsNGodsDataManager.getGods().get(new ResourceLocation(godId));
        ApostasyDefinition apostasyDef = god != null ? god.apostasy() : ApostasyDefinition.defaultDefinition();

        // Remove active effects
        EffectProfileCache.invalidate(player);

        // Apply scar
        applyScar(player, godId, apostasyDef);

        // Apply latent curse
        applyCurse(player, god);

        // Set cooldown
        long cooldownMs = SpellsNGodsConfig.COMMON.apostasyCooldownDays.get() * 24L * 3600_000L;
        long cooldownEnd = System.currentTimeMillis() + cooldownMs;
        data.setApostasyCooldownEndMs(cooldownEnd);

        // Transition to UNBOUND_COOLDOWN state
        data.setBlessingState(BlessingState.UNBOUND_COOLDOWN);
        data.setChosenGodId(null);
        data.setFavor(0.0f);
        data.setCurrentTier(DivineTier.NONE);

        // Clear trial state
        data.setInApostasyTrial(false);
        data.setCurrentTrialPhase(0);
        data.setTrialPhaseStartMs(0L);

        // Transform monument to ruined idol
        transformMonumentToRuinedIdol(player, data);

        // Send message
        String godName = god != null ? god.displayName() : godId;
        player.sendSystemMessage(Component.translatable("spells_n_gods.apostasy.completed", godName));

        // Send apostasy completion VFX
        ModNetwork.sendToPlayer(player, new DivineVfxPacket(
                DivineVfxPacket.VfxType.APOSTASY_COMPLETE,
                player.getX(), player.getY(), player.getZ(),
                "", 0));

        SpellsNGodsMod.LOGGER.info("Player {} completed apostasy from god {}",
                player.getName().getString(), godId);

        // Recompute scar effects
        ScarEffectHandler.recomputeScarEffects(player);

        // Fire event for mod compatibility (KubeJS, FTB Quests, etc.)
        MinecraftForge.EVENT_BUS.post(new SpellsNGodsEvents.ApostasyCompleteEvent(
                player, godId, data.getScarData().getScarCount()));

        return ApostasyResult.ok("spells_n_gods.apostasy.completed", godName);
    }

    /**
     * Handle apostasy trial failure.
     * Applies harsher penalties.
     */
    public static ApostasyResult failApostasy(ServerPlayer player) {
        PlayerDivinityData data = PlayerDivinityCapability.getOrCreate(player);
        String godId = data.getLastAbandonedGodId();
        if (godId == null) {
            godId = data.getChosenGodId();
        }

        GodDefinition god = godId != null ?
                SpellsNGodsDataManager.getGods().get(new ResourceLocation(godId)) : null;
        ApostasyDefinition apostasyDef = god != null ? god.apostasy() : ApostasyDefinition.defaultDefinition();

        // Apply harsher scar (1.5x penalties)
        ApostasyDefinition.ScarModifiers scars = apostasyDef.scarModifiers();
        ScarData.ScarRecord scar = new ScarData.ScarRecord(
                godId != null ? godId : "unknown",
                System.currentTimeMillis(),
                scars.healthReduction() * 1.5f,
                scars.xpPenaltyBase() * 1.5f,
                scars.deathPenaltyIncrease() * 1.5f,
                scars.luckReduction() * 1.5f,
                scars.durabilityPenalty() * 1.5f
        );
        data.getScarData().addScar(scar);

        // Apply longer curse
        ApostasyDefinition.CurseDefinition curse = apostasyDef.latentCurse();
        long curseEnd = System.currentTimeMillis() + (long)(curse.getDurationMs() * 1.5);
        data.setLatentCurseEndMs(curseEnd);

        // Capture trial phase before clearing
        int failedPhase = data.getCurrentTrialPhase();

        // Clear trial state but keep bound (trial failed, you're still bound)
        data.setInApostasyTrial(false);
        data.setCurrentTrialPhase(0);
        data.setTrialPhaseStartMs(0L);
        data.setLastAbandonedGodId(null);
        data.clearApostasyMonumentPos();

        String godName = god != null ? god.displayName() : (godId != null ? godId : "Unknown");
        player.sendSystemMessage(Component.translatable("spells_n_gods.apostasy.trial_failed", godName));

        SpellsNGodsMod.LOGGER.info("Player {} failed apostasy trials, remains bound to {}",
                player.getName().getString(), godId);

        ScarEffectHandler.recomputeScarEffects(player);

        // Fire event for mod compatibility (KubeJS, FTB Quests, etc.)
        String finalGodId = godId;
        MinecraftForge.EVENT_BUS.post(new SpellsNGodsEvents.ApostasyFailedEvent(
                player, finalGodId != null ? finalGodId : "unknown", failedPhase));

        return ApostasyResult.ok("spells_n_gods.apostasy.trial_failed", godName);
    }

    /**
     * Force apostasy without trials (for debug/admin).
     */
    public static ApostasyResult forceApostasy(ServerPlayer player) {
        PlayerDivinityData data = PlayerDivinityCapability.getOrCreate(player);

        if (!data.isBound()) {
            return ApostasyResult.error("spells_n_gods.apostasy.not_bound");
        }

        data.setLastAbandonedGodId(data.getChosenGodId());
        return completeApostasy(player);
    }

    private static void applyScar(ServerPlayer player, String godId, ApostasyDefinition apostasyDef) {
        PlayerDivinityData data = PlayerDivinityCapability.getOrCreate(player);
        ApostasyDefinition.ScarModifiers scars = apostasyDef.scarModifiers();

        ScarData.ScarRecord scar = new ScarData.ScarRecord(
                godId,
                System.currentTimeMillis(),
                scars.healthReduction(),
                scars.xpPenaltyBase(),
                scars.deathPenaltyIncrease(),
                scars.luckReduction(),
                scars.durabilityPenalty()
        );

        data.getScarData().addScar(scar);

        player.sendSystemMessage(Component.translatable("spells_n_gods.scar.notification",
                data.getScarData().getScarCount()));
    }

    private static void applyCurse(ServerPlayer player, GodDefinition god) {
        // Use LatentCurseManager to apply the curse effects
        LatentCurseManager.applyCurse(player, god);
    }

    /**
     * Transform the monument at the stored position into a ruined idol.
     */
    private static void transformMonumentToRuinedIdol(ServerPlayer player, PlayerDivinityData data) {
        if (!data.hasApostasyMonumentPos()) {
            return;
        }

        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }

        String dimKey = data.getApostasyMonumentDimension();
        ResourceKey<net.minecraft.world.level.Level> dimension = ResourceKey.create(
                Registries.DIMENSION,
                new ResourceLocation(dimKey)
        );

        ServerLevel level = server.getLevel(dimension);
        if (level == null) {
            SpellsNGodsMod.LOGGER.warn("Could not find dimension {} for monument transformation", dimKey);
            data.clearApostasyMonumentPos();
            return;
        }

        BlockPos pos = new BlockPos(
                data.getApostasyMonumentX(),
                data.getApostasyMonumentY(),
                data.getApostasyMonumentZ()
        );

        BlockEntity existingBe = level.getBlockEntity(pos);
        if (!(existingBe instanceof MonumentBlockEntity monument)) {
            // Monument was destroyed or replaced
            SpellsNGodsMod.LOGGER.debug("Monument at {} no longer exists, skipping transformation", pos);
            data.clearApostasyMonumentPos();
            return;
        }

        // Store monument data before replacing
        String originalGodId = monument.getGodId();
        java.util.UUID originalOwner = monument.getOwner();

        // Replace with ruined idol
        level.setBlock(pos, ModBlocks.RUINED_IDOL.get().defaultBlockState(), 3);

        // Initialize the ruined idol block entity
        BlockEntity newBe = level.getBlockEntity(pos);
        if (newBe instanceof RuinedIdolBlockEntity ruinedIdol) {
            ruinedIdol.setOriginalGodId(originalGodId);
            ruinedIdol.setOriginalOwner(originalOwner);
            ruinedIdol.setDesecrationTimeMs(System.currentTimeMillis());
        }

        SpellsNGodsMod.LOGGER.info("Transformed monument at {} into ruined idol", pos);

        // Clear the stored position
        data.clearApostasyMonumentPos();
    }
}
