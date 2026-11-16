package io.github.charliechaplim.minefables.ability;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.Objects;

/** Uma habilidade ativa de uma Fabula. As regras podem variar por personagem. */
public record AbilityDefinition(
        ResourceLocation id,
        String nameTranslationKey,
        AbilityEffect effect
) {
    public AbilityDefinition {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(nameTranslationKey, "nameTranslationKey");
        Objects.requireNonNull(effect, "effect");
    }

    @FunctionalInterface
    public interface AbilityEffect {
        /** Retorna true quando o uso foi realizado com sucesso. */
        boolean activate(ServerPlayer player);
    }
}
