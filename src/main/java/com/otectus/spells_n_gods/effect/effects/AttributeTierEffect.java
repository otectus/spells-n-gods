package com.otectus.spells_n_gods.effect.effects;

import com.otectus.spells_n_gods.effect.EffectType;
import com.otectus.spells_n_gods.effect.TierEffect;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import java.util.UUID;

public class AttributeTierEffect implements TierEffect {
    private final Attribute attribute;
    private final UUID modifierId;
    private final String modifierName;
    private final double value;
    private final AttributeModifier.Operation operation;

    public AttributeTierEffect(Attribute attribute, UUID modifierId, String modifierName,
                                double value, AttributeModifier.Operation operation) {
        this.attribute = attribute;
        this.modifierId = modifierId;
        this.modifierName = modifierName;
        this.value = value;
        this.operation = operation;
    }

    public AttributeTierEffect(Attribute attribute, String modifierName, double value, AttributeModifier.Operation operation) {
        this(attribute, UUID.nameUUIDFromBytes(modifierName.getBytes()), modifierName, value, operation);
    }

    @Override
    public EffectType getType() {
        return EffectType.ATTRIBUTE_MODIFIER;
    }

    @Override
    public void apply(ServerPlayer player) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance != null) {
            // Remove existing modifier with same ID first
            instance.removeModifier(modifierId);
            // Add new modifier
            instance.addTransientModifier(new AttributeModifier(
                    modifierId,
                    modifierName,
                    value,
                    operation
            ));
        }
    }

    @Override
    public void remove(ServerPlayer player) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance != null) {
            instance.removeModifier(modifierId);
        }
    }

    @Override
    public String getEffectId() {
        return "attribute_" + modifierName;
    }

    public Attribute getAttribute() {
        return attribute;
    }

    public double getValue() {
        return value;
    }

    public AttributeModifier.Operation getOperation() {
        return operation;
    }
}
