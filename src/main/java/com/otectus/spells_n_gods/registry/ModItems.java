package com.otectus.spells_n_gods.registry;

import com.otectus.spells_n_gods.SpellsNGodsMod;
import com.otectus.spells_n_gods.content.RuneItem;
import com.otectus.spells_n_gods.item.DivineBowItem;
import com.otectus.spells_n_gods.item.DivineCrossbowItem;
import com.otectus.spells_n_gods.item.DivineWeaponItem;
import com.otectus.spells_n_gods.item.GodLocatorItem;
import com.otectus.spells_n_gods.item.RunicFragmentItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.List;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, SpellsNGodsMod.MODID);

    public static final RegistryObject<Item> RUNE = ITEMS.register("rune", () -> new RuneItem(new Item.Properties()));
    public static final RegistryObject<Item> MONUMENT = ITEMS.register("monument", () -> new BlockItem(ModBlocks.MONUMENT.get(), new Item.Properties()));
    public static final RegistryObject<Item> RUINED_IDOL = ITEMS.register("ruined_idol", () -> new BlockItem(ModBlocks.RUINED_IDOL.get(), new Item.Properties()));
    public static final RegistryObject<Item> RUNIC_FRAGMENT = ITEMS.register("runic_fragment", () -> new RunicFragmentItem(new Item.Properties()));

    // God-specific temple relics
    public static final RegistryObject<Item> SUNWARD_RELIC = ITEMS.register("sunward_relic",
            () -> new GodLocatorItem(new Item.Properties(), "spells_n_gods:deus"));
    public static final RegistryObject<Item> STORMCALL_SHARD = ITEMS.register("stormcall_shard",
            () -> new GodLocatorItem(new Item.Properties(), "spells_n_gods:velox"));
    public static final RegistryObject<Item> SANGUINE_PENDULUM = ITEMS.register("sanguine_pendulum",
            () -> new GodLocatorItem(new Item.Properties(), "spells_n_gods:celia"));
    public static final RegistryObject<Item> CONQUEST_CINDER = ITEMS.register("conquest_cinder",
            () -> new GodLocatorItem(new Item.Properties(), "spells_n_gods:bella"));
    public static final RegistryObject<Item> PROPHETS_EYE = ITEMS.register("prophets_eye",
            () -> new GodLocatorItem(new Item.Properties(), "spells_n_gods:bricoleur"));
    public static final RegistryObject<Item> VOID_LENS = ITEMS.register("void_lens",
            () -> new GodLocatorItem(new Item.Properties(), "spells_n_gods:ingenium"));
    public static final RegistryObject<Item> HUNTERS_FANG = ITEMS.register("hunters_fang",
            () -> new GodLocatorItem(new Item.Properties(), "spells_n_gods:venatas"));
    public static final RegistryObject<Item> ELDRITCH_LODESTONE = ITEMS.register("eldritch_lodestone",
            () -> new GodLocatorItem(new Item.Properties(), "spells_n_gods:magnus"));
    public static final RegistryObject<Item> FROZEN_TEAR = ITEMS.register("frozen_tear",
            () -> new GodLocatorItem(new Item.Properties(), "spells_n_gods:glacia"));

    /** All god-specific temple relic items for convenience iteration. */
    public static List<RegistryObject<Item>> getAllLocators() {
        return List.of(SUNWARD_RELIC, STORMCALL_SHARD, SANGUINE_PENDULUM, CONQUEST_CINDER,
                PROPHETS_EYE, VOID_LENS, HUNTERS_FANG,
                ELDRITCH_LODESTONE, FROZEN_TEAR);
    }

    // ── Divine Weapons (dropped by god bosses) ──

    public static final RegistryObject<Item> WARHAMMER_OF_CREATION = ITEMS.register("warhammer_of_creation",
            () -> new DivineWeaponItem("deus", 14, 0.9f, 500,
                    "Divine Mandate",
                    "AoE shockwave: 8 dmg, Glowing, Resistance I",
                    "+3 bonus damage to undead"));

    public static final RegistryObject<Item> BOW_OF_AGILITY = ITEMS.register("bow_of_agility",
            DivineBowItem::new);

    public static final RegistryObject<Item> PHILOSOPHERS_DAGGER = ITEMS.register("philosophers_dagger",
            () -> new DivineWeaponItem("celia", 7, 2.2f, 360,
                    "Sanguine Pact",
                    "Sacrifice 4 hearts: 8s lifesteal + Wither on-hit",
                    "Killing a mob restores 1 heart"));

    public static final RegistryObject<Item> WARMONGERS_SWORD = ITEMS.register("warmongers_sword",
            () -> new DivineWeaponItem("bella", 12, 1.4f, 400,
                    "Inferno Charge",
                    "Dash 6 blocks, 10 fire dmg, ignite path",
                    "Attacks set targets on fire for 3s"));

    public static final RegistryObject<Item> PEACEMAKING_STAFF = ITEMS.register("peacemaking_staff",
            () -> new DivineWeaponItem("bricoleur", 9, 1.2f, 600,
                    "Prophet's Judgment",
                    "Summon 3 spectral Vex allies for 15s",
                    "Nearby hostiles glow while held"));

    public static final RegistryObject<Item> VOID_ORB = ITEMS.register("void_orb",
            () -> new DivineWeaponItem("ingenium", 8, 1.6f, 300,
                    "Rift Walk",
                    "Teleport 16 blocks + void pulse (6 dmg, Blindness)",
                    "Immune to ender pearl damage"));

    public static final RegistryObject<Item> CROSSBOW_OF_THE_WILD = ITEMS.register("crossbow_of_the_wild",
            DivineCrossbowItem::new);

    public static final RegistryObject<Item> BOOK_OF_MAGERY = ITEMS.register("book_of_magery",
            () -> new DivineWeaponItem("magnus", 6, 1.0f, 280,
                    "Eldritch Barrage",
                    "Fire 5 homing projectiles (25 total magic dmg)",
                    "+15% XP, kills restore XP levels"));

    public static final RegistryObject<Item> GLACIAL_BATTLEAXE = ITEMS.register("glacial_battleaxe",
            () -> new DivineWeaponItem("glacia", 15, 0.8f, 440,
                    "Absolute Zero",
                    "6-block frost nova: 8 dmg, freeze 3s, Slowness II 8s",
                    "Attacks apply Slowness I for 2s"));

    /** All divine weapon items for convenience iteration. */
    public static List<RegistryObject<Item>> getAllDivineWeapons() {
        return List.of(WARHAMMER_OF_CREATION, BOW_OF_AGILITY, PHILOSOPHERS_DAGGER,
                WARMONGERS_SWORD, PEACEMAKING_STAFF, VOID_ORB,
                CROSSBOW_OF_THE_WILD, BOOK_OF_MAGERY, GLACIAL_BATTLEAXE);
    }

    private ModItems() {
    }
}
