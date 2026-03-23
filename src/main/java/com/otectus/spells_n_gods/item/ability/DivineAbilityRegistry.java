package com.otectus.spells_n_gods.item.ability;

import com.otectus.spells_n_gods.SpellsNGodsMod;
import com.otectus.spells_n_gods.item.ability.impl.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Registry for data-driven divine weapon ability types.
 * Each type maps to a {@link DivineAbility} implementation that handles
 * the actual execution with JSON-defined parameters.
 */
public class DivineAbilityRegistry {

    private static final Map<String, DivineAbility> ABILITIES = new HashMap<>();

    public static void init() {
        register(new AoeBurstAbility());
        register(new DashAbility());
        register(new TeleportAbility());
        register(new SummonAbility());
        register(new ProjectileBarrageAbility());
        register(new SelfBuffAbility());
        register(new TargetedDebuffAbility());

        SpellsNGodsMod.LOGGER.info("Registered {} divine ability types", ABILITIES.size());
    }

    public static void register(DivineAbility ability) {
        ABILITIES.put(ability.getType(), ability);
    }

    public static DivineAbility get(String type) {
        return ABILITIES.get(type);
    }

    public static boolean has(String type) {
        return ABILITIES.containsKey(type);
    }
}
