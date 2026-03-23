package com.otectus.spells_n_gods.offering;

import com.otectus.spells_n_gods.SpellsNGodsMod;
import com.otectus.spells_n_gods.capability.CapabilityHandler;
import com.otectus.spells_n_gods.capability.DivineTier;
import com.otectus.spells_n_gods.capability.PlayerDivinityCapability;
import com.otectus.spells_n_gods.capability.PlayerDivinityData;
import com.otectus.spells_n_gods.data.GodDefinition;
import com.otectus.spells_n_gods.data.SpellsNGodsDataManager;
import com.otectus.spells_n_gods.animation.PlayerAnimationType;
import com.otectus.spells_n_gods.network.DivineVfxPacket;
import com.otectus.spells_n_gods.network.ModNetwork;
import com.otectus.spells_n_gods.network.PlayerAnimationPacket;
import com.otectus.spells_n_gods.state.BlessingStateMachine;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

public class OfferingProcessor {

    public static OfferingResult processOffering(ServerPlayer player, ItemStack offering) {
        if (offering.isEmpty()) {
            return OfferingResult.fail(Component.translatable("spells_n_gods.offering.empty"));
        }

        Optional<PlayerDivinityData> dataOpt = PlayerDivinityCapability.get(player);
        if (dataOpt.isEmpty()) {
            return OfferingResult.fail(Component.translatable("spells_n_gods.offering.error"));
        }

        PlayerDivinityData data = dataOpt.get();

        if (data.getChosenGodId() == null) {
            return OfferingResult.fail(Component.translatable("spells_n_gods.offering.not_bound"));
        }

        GodDefinition god = SpellsNGodsDataManager.getGods().get(new ResourceLocation(data.getChosenGodId()));
        if (god == null) {
            return OfferingResult.fail(Component.translatable("spells_n_gods.offering.unknown_god"));
        }

        // Get the validator for this god
        String validatorId = god.worship().offering().validator();
        OfferingValidator validator = OfferingValidatorRegistry.getValidator(validatorId);

        // Validate the offering
        ValidationResult validation = validator.validate(offering, player, god);
        if (!validation.valid()) {
            return OfferingResult.fail(validation.message());
        }

        // Compute base value
        float baseValue = validator.computeValue(offering, player, god);

        // Apply diminishing returns if enabled
        float finalValue = baseValue;
        if (god.worship().offering().diminishingReturns()) {
            float multiplier = data.getOfferingHistory().computeDiminishingMultiplier(offering.getItem());
            finalValue = baseValue * multiplier;

            if (multiplier < 1.0f) {
                SpellsNGodsMod.LOGGER.debug("Diminishing returns applied: {} -> {} (multiplier: {})",
                        baseValue, finalValue, multiplier);
            }
        }

        // Record the offering in history
        data.getOfferingHistory().record(offering.getItem());

        // Consume the item
        int consumeCount = offering.getCount();
        offering.shrink(consumeCount);

        // Track tier before offering for tier-up detection
        DivineTier previousTier = data.getCurrentTier();

        // Apply the offering effects via state machine
        BlessingStateMachine.onOfferingAccepted(data, finalValue);

        // Sync to client
        CapabilityHandler.syncToClient(player);

        // Send offering VFX and animation
        String school = god.magicSchool();
        ModNetwork.sendToPlayer(player, new DivineVfxPacket(
                DivineVfxPacket.VfxType.OFFERING_ACCEPTED,
                player.getX(), player.getY(), player.getZ(),
                school, (int) finalValue));
        ModNetwork.sendToTrackingAndSelf(player, new PlayerAnimationPacket(
                player.getUUID(), PlayerAnimationType.OFFERING_PRESENT, PlayerAnimationPacket.Action.PLAY));

        // Check for tier-up and send celebration VFX + animation
        DivineTier newTier = data.getCurrentTier();
        if (newTier.getLevel() > previousTier.getLevel()) {
            ModNetwork.sendToPlayer(player, new DivineVfxPacket(
                    DivineVfxPacket.VfxType.TIER_UP,
                    player.getX(), player.getY(), player.getZ(),
                    school, newTier.getLevel()));
            ModNetwork.sendToTrackingAndSelf(player, new PlayerAnimationPacket(
                    player.getUUID(), PlayerAnimationType.TIER_UP_CELEBRATE, PlayerAnimationPacket.Action.PLAY));
            player.sendSystemMessage(Component.translatable(
                    "spells_n_gods.tier.advanced", newTier.getTierKey())
                    .withStyle(style -> style.withColor(0xFFD700)));
        }

        SpellsNGodsMod.LOGGER.info("Player {} offered {} x{} to {}, gained {} favor",
                player.getName().getString(),
                offering.getItem().getDescriptionId(),
                consumeCount,
                god.displayName(),
                finalValue);

        return OfferingResult.success(
                Component.translatable("spells_n_gods.offering.accepted", god.displayName()),
                finalValue
        );
    }

    public record OfferingResult(boolean successful, Component message, float favorGained) {
        public static OfferingResult success(Component message, float favorGained) {
            return new OfferingResult(true, message, favorGained);
        }

        public static OfferingResult fail(Component message) {
            return new OfferingResult(false, message, 0f);
        }
    }
}
