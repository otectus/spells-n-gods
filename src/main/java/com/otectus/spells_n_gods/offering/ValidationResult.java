package com.otectus.spells_n_gods.offering;

import net.minecraft.network.chat.Component;

public record ValidationResult(boolean valid, Component message) {
    public static ValidationResult ok() {
        return new ValidationResult(true, Component.empty());
    }

    public static ValidationResult ok(String messageKey) {
        return new ValidationResult(true, Component.translatable(messageKey));
    }

    public static ValidationResult fail(String messageKey) {
        return new ValidationResult(false, Component.translatable(messageKey));
    }

    public static ValidationResult fail(String messageKey, Object... args) {
        return new ValidationResult(false, Component.translatable(messageKey, args));
    }
}
