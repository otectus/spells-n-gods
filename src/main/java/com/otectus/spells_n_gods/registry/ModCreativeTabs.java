package com.otectus.spells_n_gods.registry;

import com.otectus.spells_n_gods.SpellsNGodsMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModCreativeTabs {

    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, SpellsNGodsMod.MODID);

    public static final RegistryObject<CreativeModeTab> RUNIC_GODS_TAB = CREATIVE_TABS.register("runic_gods_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.spells_n_gods"))
                    .icon(() -> new ItemStack(ModItems.RUNE.get()))
                    .displayItems((params, output) -> {
                        SpellsNGodsMod.LOGGER.debug("[SpellsNGods] Creative tab displayItems called");
                        output.accept(ModItems.RUNE.get());
                        output.accept(ModItems.RUNIC_FRAGMENT.get());
                        // God-specific temple locators
                        for (RegistryObject<Item> locator : ModItems.getAllLocators()) {
                            output.accept(locator.get());
                        }
                        output.accept(ModItems.MONUMENT.get());
                        output.accept(ModItems.RUINED_IDOL.get());
                        // Divine weapons
                        for (RegistryObject<Item> weapon : ModItems.getAllDivineWeapons()) {
                            output.accept(weapon.get());
                        }
                    })
                    .build());

    /**
     * Called from the mod bus to add items via the Forge event as a secondary mechanism.
     */
    public static void onBuildContents(BuildCreativeModeTabContentsEvent event) {
        if (event.getTab() == RUNIC_GODS_TAB.get()) {
            SpellsNGodsMod.LOGGER.info("[SpellsNGods] BuildCreativeModeTabContentsEvent fired for Spells n' Gods tab");
        }
    }

    private ModCreativeTabs() {
    }
}
