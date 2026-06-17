package com.otectus.spells_n_gods.content;

import com.otectus.spells_n_gods.binding.BindingHandler;
import com.otectus.spells_n_gods.data.GodDefinition;
import com.otectus.spells_n_gods.data.SpellsNGodsDataManager;
import com.otectus.spells_n_gods.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public class RuneItem extends Item {
    public static final String TAG_GOD_ID = "GodId";

    public RuneItem(Properties properties) {
        super(properties);
    }

    public static Optional<ResourceLocation> getGodId(ItemStack stack) {
        if (stack.hasTag() && stack.getTag().contains(TAG_GOD_ID)) {
            String id = stack.getTag().getString(TAG_GOD_ID);
            if (!id.isEmpty()) {
                return Optional.of(new ResourceLocation(id));
            }
        }
        return Optional.empty();
    }

    public static void setGodId(ItemStack stack, ResourceLocation godId) {
        stack.getOrCreateTag().putString(TAG_GOD_ID, godId.toString());
    }

    public static ItemStack createForGod(ResourceLocation godId) {
        ItemStack stack = new ItemStack(ModItems.RUNE.get());
        setGodId(stack, godId);
        return stack;
    }

    @Nullable
    public static GodDefinition getGodDefinition(ItemStack stack) {
        return getGodId(stack)
                .map(id -> SpellsNGodsDataManager.getGods().get(id))
                .orElse(null);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);

        GodDefinition god = getGodDefinition(stack);
        if (god != null) {
            tooltip.add(Component.translatable("item.spells_n_gods.rune.bound_to", god.displayName())
                    .withStyle(ChatFormatting.GOLD));

            if (!god.domains().isEmpty()) {
                String domains = String.join(", ", god.domains());
                tooltip.add(Component.translatable("item.spells_n_gods.rune.domains", domains)
                        .withStyle(ChatFormatting.GRAY));
            }

            if (!god.philosophy().isEmpty()) {
                tooltip.add(Component.literal("\"" + god.philosophy() + "\"")
                        .withStyle(ChatFormatting.ITALIC, ChatFormatting.DARK_PURPLE));
            }
        } else {
            Optional<ResourceLocation> godId = getGodId(stack);
            if (godId.isPresent()) {
                tooltip.add(Component.translatable("item.spells_n_gods.rune.unknown_god", godId.get().toString())
                        .withStyle(ChatFormatting.RED));
            } else {
                tooltip.add(Component.translatable("item.spells_n_gods.rune.unbound")
                        .withStyle(ChatFormatting.DARK_GRAY));
            }
        }
        tooltip.add(Component.translatable("item.spells_n_gods.rune.tooltip")
                .withStyle(ChatFormatting.DARK_GRAY));

        // Show scar history for the local player. Delegated to a client-only class via DistExecutor
        // so this common item class never links Minecraft/LocalPlayer (which would crash dedicated
        // servers at class-load time).
        net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,
                () -> () -> com.otectus.spells_n_gods.client.RuneClientTooltip.appendScarInfo(tooltip));
    }

    @Override
    public Component getName(ItemStack stack) {
        GodDefinition god = getGodDefinition(stack);
        if (god != null) {
            return Component.translatable("item.spells_n_gods.rune.named", god.displayName());
        }
        return super.getName(stack);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (target instanceof ArmorStand armorStand) {
            return BindingHandler.tryBind(player, armorStand, stack);
        }
        return InteractionResult.PASS;
    }
}
