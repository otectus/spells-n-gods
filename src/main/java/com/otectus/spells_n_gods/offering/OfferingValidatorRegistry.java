package com.otectus.spells_n_gods.offering;

import com.google.gson.JsonObject;
import com.otectus.spells_n_gods.SpellsNGodsMod;
import com.otectus.spells_n_gods.data.OfferingValidatorDefinition;
import com.otectus.spells_n_gods.data.SpellsNGodsDataManager;
import com.otectus.spells_n_gods.offering.validators.ActionRuleValidator;
import com.otectus.spells_n_gods.offering.validators.AlwaysValidValidator;
import com.otectus.spells_n_gods.offering.validators.ItemRuleValidator;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class OfferingValidatorRegistry {
    private static final Map<String, OfferingValidator> validatorCache = new ConcurrentHashMap<>();
    private static final OfferingValidator FALLBACK = new AlwaysValidValidator();

    public static OfferingValidator getValidator(String validatorId) {
        if (validatorId == null || validatorId.isEmpty()) {
            return FALLBACK;
        }

        return validatorCache.computeIfAbsent(validatorId, id -> {
            ResourceLocation resourceId = ResourceLocation.tryParse(id);
            if (resourceId == null) {
                resourceId = new ResourceLocation(SpellsNGodsMod.MODID, id);
            }

            OfferingValidatorDefinition def = SpellsNGodsDataManager.getOfferingValidators().get(resourceId);
            if (def == null) {
                SpellsNGodsMod.LOGGER.warn("Unknown offering validator: {}, using fallback", id);
                return FALLBACK;
            }

            return createValidator(id, def);
        });
    }

    private static OfferingValidator createValidator(String id, OfferingValidatorDefinition def) {
        JsonObject json = def.raw();
        String type = json.has("type") ? json.get("type").getAsString() : "item_rule";

        return switch (type) {
            case "item_rule" -> new ItemRuleValidator(id, json);
            case "action_rule" -> new ActionRuleValidator(id, json);
            case "always_valid" -> new AlwaysValidValidator();
            default -> {
                SpellsNGodsMod.LOGGER.warn("Unknown validator type: {}, using fallback", type);
                yield FALLBACK;
            }
        };
    }

    public static void clearCache() {
        validatorCache.clear();
    }
}
