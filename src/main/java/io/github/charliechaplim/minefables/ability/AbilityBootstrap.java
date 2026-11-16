package io.github.charliechaplim.minefables.ability;

import io.github.charliechaplim.minefables.MineFables;
import io.github.charliechaplim.minefables.resource.ResourceService;
import io.github.charliechaplim.minefables.resource.ResourceType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/** Duas habilidades demonstrativas, sem conceder poderes a outros personagens. */
public final class AbilityBootstrap {
    public static final ResourceLocation TEST_CHARACTER = id("teste");
    private static final long TEST_MANA_AMOUNT = 40;

    private AbilityBootstrap() {}

    public static void registerAll() {
        AbilityRegistry.register(TEST_CHARACTER, List.of(
                new AbilityDefinition(
                        id("teste_1"), "ability.minefables.teste_1",
                        player -> {
                            if (!ResourceService.trySpend(player, ResourceType.MANA, TEST_MANA_AMOUNT)) {
                                player.displayClientMessage(Component.literal("Mana insuficiente!"), true);
                                return false;
                            }
                            return true;
                        }
                ),
                new AbilityDefinition(
                        id("teste_2"), "ability.minefables.teste_2",
                        player -> {
                            long current = ResourceService.get(player, ResourceType.MANA);
                            long maximum = ResourceService.capacity(player, ResourceType.MANA);
                            if (current >= maximum) {
                                player.displayClientMessage(Component.literal("Mana ja esta cheia!"), true);
                                return false;
                            }
                            ResourceService.add(player, ResourceType.MANA, TEST_MANA_AMOUNT);
                            return true;
                        }
                )
        ));
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MineFables.MODID, path);
    }
}
