package io.github.charliechaplim.minefables.item;

import io.github.charliechaplim.minefables.character.PlayerCharacterService;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Ferramenta de desenvolvimento: nao e comida e nao e consumida. */
public final class DebuggerAppleItem extends Item {

    public DebuggerAppleItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // O cliente exibe a animacao; APENAS o servidor modifica dados.
        if (level.isClientSide()) {
            return InteractionResultHolder.success(stack);
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.pass(stack);
        }

        // Nao permitir que jogadores comuns anulem a escolha definitiva.
        if (!serverPlayer.createCommandSourceStack().hasPermission(2)) {
            serverPlayer.displayClientMessage(
                    Component.translatable("message.minefables.debugger_apple.no_permission"), false);
            return InteractionResultHolder.fail(stack);
        }

        boolean cleared = PlayerCharacterService.resetForDebug(serverPlayer);
        serverPlayer.displayClientMessage(Component.translatable(
                cleared
                        ? "message.minefables.debugger_apple.reset"
                        : "message.minefables.debugger_apple.empty"), false);

        return InteractionResultHolder.consume(stack);
    }
}
