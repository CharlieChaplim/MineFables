package io.github.charliechaplim.minefables.command;

import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.charliechaplim.minefables.resource.ResourceService;
import io.github.charliechaplim.minefables.resource.ResourceType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.stream.Collectors;

/** Subarvore de debug: /minefables resources [set <tipo> <valor>] */
public final class ResourceCommands {
    private ResourceCommands() {}

    public static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("resources")
                .executes(ctx -> show(ctx.getSource()))
                .then(Commands.literal("set")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.argument("type", StringArgumentType.word())
                                .then(Commands.argument("amount", LongArgumentType.longArg(0))
                                        .executes(ctx -> set(ctx.getSource(),
                                                StringArgumentType.getString(ctx, "type"),
                                                LongArgumentType.getLong(ctx, "amount"))))));
    }

    private static int show(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        if (ResourceService.types(player).isEmpty()) {
            source.sendSuccess(() -> Component.literal("Este personagem nao possui barras de energia."), false);
            return 1;
        }
        String values = ResourceService.types(player).stream()
                .map(type -> type.id() + " " + ResourceService.get(player, type) + "/" + ResourceService.capacity(player, type))
                .collect(Collectors.joining(" | "));
        source.sendSuccess(() -> Component.literal("Recursos: " + values), false);
        return 1;
    }

    private static int set(CommandSourceStack source, String id, long amount) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ResourceType type = ResourceType.fromId(id).orElse(null);
        if (type == null || !ResourceService.set(player, type, amount)) {
            source.sendFailure(Component.literal("Recurso invalido ou nao permitido para sua Fabula: " + id));
            return 0;
        }
        source.sendSuccess(() -> Component.literal(
                type.displayName() + " alterado para " + ResourceService.get(player, type)), false);
        return 1;
    }
}
