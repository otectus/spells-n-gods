package com.otectus.spells_n_gods.item;

import com.otectus.spells_n_gods.data.GodDefinition;
import com.otectus.spells_n_gods.data.SpellsNGodsDataManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class RunicFragmentItem extends Item {

    public RunicFragmentItem(Properties properties) {
        super(properties.stacksTo(16).rarity(Rarity.UNCOMMON));
    }

    public static ItemStack createForGod(String godId) {
        ItemStack stack = new ItemStack(com.otectus.spells_n_gods.registry.ModItems.RUNIC_FRAGMENT.get());
        stack.getOrCreateTag().putString("SourceGod", godId);
        return stack;
    }

    @Nullable
    public static String getSourceGod(ItemStack stack) {
        if (stack.hasTag() && stack.getTag().contains("SourceGod")) {
            return stack.getTag().getString("SourceGod");
        }
        return null;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                 List<Component> tooltip, TooltipFlag flag) {
        String godId = getSourceGod(stack);
        if (godId != null) {
            GodDefinition god = SpellsNGodsDataManager.getGods().get(new ResourceLocation(godId));
            if (god != null) {
                tooltip.add(Component.translatable("spells_n_gods.fragment.source", god.displayName())
                        .withStyle(ChatFormatting.GOLD));
                if (!god.magicSchool().isEmpty()) {
                    tooltip.add(Component.translatable("spells_n_gods.fragment.school", god.magicSchool())
                            .withStyle(ChatFormatting.DARK_PURPLE));
                }
            }
        }
        tooltip.add(Component.translatable("spells_n_gods.fragment.tooltip")
                .withStyle(ChatFormatting.GRAY));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
