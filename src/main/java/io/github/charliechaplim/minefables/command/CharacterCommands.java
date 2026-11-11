package io.github.charliechaplim.minefables.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import io.github.charliechaplim.minefables.MineFables;
import io.github.charliechaplim.minefables.character.CharacterDefinition;
import io.github.charliechaplim.minefables.character.PlayerCharacterService;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.Optional;

public final class CharacterCommands {

    private CharacterCommands() {
    }

    public static void register(RegisterCommandsEvent event) {
        MineFables.LOGGER.info(
        "[MineFables] Registrando comandos de personagens!"
    );
        event.getDispatcher().register(
                Commands.literal("minefables")
                        .then(Commands.literal("character")
                                .then(Commands.literal("set")
                                        .requires(source -> source.hasPermission(2))
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
                        )
        );
    }

    private static int setCharacter(CommandSourceStack source, String name)
            throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ResourceLocation id = ResourceLocation.tryParse(MineFables.MODID + ":" + name);

        if (id == null || !PlayerCharacterService.select(player, id)) {
            source.sendFailure(Component.literal("Personagem desconhecido: " + name));
            return 0;
        }

        CharacterDefinition selected = PlayerCharacterService.getSelected(player).orElseThrow();
        source.sendSuccess(
                () -> Component.literal("Personagem selecionado: ")
                        .append(Component.translatable(selected.nameTranslationKey())),
                false
        );
        return 1;
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
}
