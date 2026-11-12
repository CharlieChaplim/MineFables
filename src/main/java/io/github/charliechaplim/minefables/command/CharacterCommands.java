package io.github.charliechaplim.minefables.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.charliechaplim.minefables.MineFables;
import io.github.charliechaplim.minefables.character.CharacterDefinition;
import io.github.charliechaplim.minefables.character.CharacterRegistry;
import io.github.charliechaplim.minefables.character.PlayerCharacterService;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.Optional;
import java.util.stream.Collectors;

public final class CharacterCommands {

    private CharacterCommands() {
    }

    public static void register(RegisterCommandsEvent event) {
        MineFables.LOGGER.info("Registrando comandos de personagens MineFables.");

        event.getDispatcher().register(
                Commands.literal("minefables")
                        .then(Commands.literal("character")
                                .then(Commands.literal("set")
                                        .then(Commands.argument("id", StringArgumentType.word())
                                                .executes(context -> setCharacter(
                                                        context.getSource(),
                                                        StringArgumentType.getString(context, "id")
                                                ))
                                        )
                                )
                                .then(Commands.literal("get")
                                        .executes(context -> showCharacter(context.getSource()))
                                )
                                .then(Commands.literal("list")
                                        .executes(context -> listCharacters(context.getSource()))
                                )
                        )
        );
    }

    private static int setCharacter(CommandSourceStack source, String name)
            throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ResourceLocation id = ResourceLocation.tryParse(MineFables.MODID + ":" + name);
        if (id == null) {
            source.sendFailure(Component.literal("ID invalido: " + name));
            return 0;
        }

        PlayerCharacterService.SelectionResult result = PlayerCharacterService.select(player, id);
        switch (result) {
            case UNKNOWN_CHARACTER -> {
                source.sendFailure(Component.literal("Personagem desconhecido: " + name));
                return 0;
            }
            case ALREADY_USING -> {
                source.sendFailure(Component.literal("Voce ja usa esse personagem."));
                return 0;
            }
            case ALREADY_HAS_CHARACTER -> {
                source.sendFailure(Component.literal(
                        "Voce ja escolheu uma Fabula. Nao pode escolher outro personagem."
                ));
                return 0;
            }
            case SELECTED -> {
                CharacterDefinition selected = PlayerCharacterService.getSelected(player).orElseThrow();
                source.sendSuccess(
                        () -> Component.literal("Personagem selecionado: ")
                                .append(Component.translatable(selected.nameTranslationKey())),
                        false
                );
                return 1;
            }
        }
        return 0;
    }

    private static int showCharacter(CommandSourceStack source)
            throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        Optional<CharacterDefinition> selected = PlayerCharacterService.getSelected(player);
        if (selected.isEmpty()) {
            source.sendFailure(Component.literal("Nenhum personagem selecionado."));
            return 0;
        }

        CharacterDefinition character = selected.get();
        source.sendSuccess(
                () -> Component.literal("Seu personagem: ")
                        .append(Component.translatable(character.nameTranslationKey())),
                false
        );
        return 1;
    }

    private static int listCharacters(CommandSourceStack source) {
        String names = CharacterRegistry.getAll().stream()
                .map(definition -> definition.id().getPath())
                .collect(Collectors.joining(", "));
        int count = CharacterRegistry.getAll().size();
        source.sendSuccess(
                () -> Component.literal("Personagens registrados (" + count + "): " + names),
                false
        );
        return count;
    }
}
