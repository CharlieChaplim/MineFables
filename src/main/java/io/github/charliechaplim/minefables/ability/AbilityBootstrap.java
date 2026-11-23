package io.github.charliechaplim.minefables.ability;

import io.github.charliechaplim.minefables.MineFables;
import io.github.charliechaplim.minefables.resource.ResourceService;
import io.github.charliechaplim.minefables.resource.ResourceType;
import io.github.charliechaplim.minefables.william.WilliamKit;
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
        AbilityRegistry.register(WilliamKit.ID, List.of(
                new AbilityDefinition(id("william_resonance"), "ability.minefables.william_resonance", 1, WilliamKit::resonance),
                new AbilityDefinition(id("william_impetuo"), "ability.minefables.william_impetuo", 2, WilliamKit::impetuar),
                new AbilityDefinition(id("william_walker"), "ability.minefables.william_walker", 3, WilliamKit::walker),
                new AbilityDefinition(id("william_citation"), "ability.minefables.william_citation", 4, WilliamKit::slots),
                new AbilityDefinition(id("william_dive"), "ability.minefables.william_dive", 5, WilliamKit::dive),
                new AbilityDefinition(id("william_dragon"), "ability.minefables.william_dragon", 6, WilliamKit::dragon)
        ));
        AbilityRegistry.registerUltimate(WilliamKit.ID, List.of(
                new AbilityDefinition(id("william_fireball"), "ability.minefables.william_fireball", 6, WilliamKit::fireball),
                new AbilityDefinition(id("william_steam"), "ability.minefables.william_steam", 6, WilliamKit::steam),
                new AbilityDefinition(id("william_overload"), "ability.minefables.william_overload", 6, WilliamKit::overload),
                new AbilityDefinition(id("william_zone"), "ability.minefables.william_zone", 6, WilliamKit::zone)
        ));
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MineFables.MODID, path);
    }
}
