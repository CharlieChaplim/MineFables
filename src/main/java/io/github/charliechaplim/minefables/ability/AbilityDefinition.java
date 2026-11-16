package io.github.charliechaplim.minefables.ability;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.Objects;

/** Acesso ao nivel e a execucao pertencem ao servidor; UI apenas apresenta. */
public record AbilityDefinition(
        ResourceLocation id,
        String nameTranslationKey,
        int minimumLevel,
        AbilityEffect effect
) {
    public AbilityDefinition {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(nameTranslationKey, "nameTranslationKey");
        Objects.requireNonNull(effect, "effect");
        if (minimumLevel < 1) throw new IllegalArgumentException("minimumLevel");
    }

    /** Compativel com personagens sem sistema de paginas (ex.: Teste). */
    public AbilityDefinition(ResourceLocation id, String translationKey, AbilityEffect effect) {
        this(id, translationKey, 1, effect);
    }

    @FunctionalInterface
    public interface AbilityEffect {
        boolean activate(ServerPlayer player);
    }
}
