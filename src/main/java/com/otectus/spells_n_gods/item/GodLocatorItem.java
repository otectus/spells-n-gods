package com.otectus.spells_n_gods.item;

import com.otectus.spells_n_gods.config.SpellsNGodsConfig;
import com.otectus.spells_n_gods.data.GodDefinition;
import com.otectus.spells_n_gods.data.SpellsNGodsDataManager;
import com.otectus.spells_n_gods.worldstate.GodWorldState;
import com.otectus.spells_n_gods.worldstate.StructureRecord;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public class GodLocatorItem extends Item {

    private final String godId;

    public GodLocatorItem(Properties properties, String godId) {
        super(properties.stacksTo(1).rarity(Rarity.RARE));
        this.godId = godId;
    }

    public String getGodId() {
        return godId;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide()) {
            return InteractionResultHolder.success(stack);
        }

        ServerPlayer serverPlayer = (ServerPlayer) player;
        GodWorldState state = GodWorldState.get(serverPlayer.getServer());
        String dimKey = level.dimension().location().toString();
        BlockPos playerPos = serverPlayer.blockPosition();

        // Resolve god display name
        String godName = resolveGodName();

        Optional<StructureRecord> recordOpt = state.findStructureForGod(this.godId, dimKey);

        if (recordOpt.isPresent()) {
            StructureRecord record = recordOpt.get();
            double distance = Math.sqrt(record.center().distSqr(playerPos));
            String direction = getCardinalDirection(
                    record.center().getX() - playerPos.getX(),
                    record.center().getZ() - playerPos.getZ()
            );

            serverPlayer.sendSystemMessage(
                    Component.translatable("spells_n_gods.locator.found", godName, (int) distance, direction)
                            .withStyle(ChatFormatting.GOLD));
        } else {
            serverPlayer.sendSystemMessage(
                    Component.translatable("spells_n_gods.locator.none_found_god", godName)
                            .withStyle(ChatFormatting.GRAY));
        }

        player.getCooldowns().addCooldown(this, SpellsNGodsConfig.COMMON.locatorCooldownTicks.get());
        return InteractionResultHolder.consume(stack);
    }

    /**
     * Resolves the god's display name from the data manager,
     * falling back to the raw god ID if the definition isn't available.
     */
    private String resolveGodName() {
        GodDefinition god = SpellsNGodsDataManager.getGods().get(new ResourceLocation(this.godId));
        return god != null ? god.displayName() : this.godId;
    }

    private static String getCardinalDirection(int dx, int dz) {
        double angle = Math.toDegrees(Math.atan2(-dx, dz));
        if (angle < 0) angle += 360;
        if (angle >= 337.5 || angle < 22.5) return "South";
        if (angle < 67.5) return "Southwest";
        if (angle < 112.5) return "West";
        if (angle < 157.5) return "Northwest";
        if (angle < 202.5) return "North";
        if (angle < 247.5) return "Northeast";
        if (angle < 292.5) return "East";
        return "Southeast";
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                 List<Component> tooltip, TooltipFlag flag) {
        // Per-god thematic description (e.g. "spells_n_gods.locator.desc.deus")
        ResourceLocation godLoc = new ResourceLocation(this.godId);
        tooltip.add(Component.translatable("spells_n_gods.locator.desc." + godLoc.getPath())
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        // Functional hint
        String godName = resolveGodName();
        tooltip.add(Component.translatable("spells_n_gods.locator.tooltip.god", godName)
                .withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
