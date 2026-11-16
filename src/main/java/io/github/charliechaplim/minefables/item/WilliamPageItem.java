package io.github.charliechaplim.minefables.item;

import io.github.charliechaplim.minefables.william.WilliamKit;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Uma pagina consumida desbloqueia um nivel: comeca em 1, maximo 5. */
public final class WilliamPageItem extends Item {
    public WilliamPageItem(Properties properties) { super(properties); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) return InteractionResultHolder.success(stack);
        if (!(player instanceof ServerPlayer server) || !WilliamKit.readPage(server))
            return InteractionResultHolder.fail(stack);
        if (!player.getAbilities().instabuild) stack.shrink(1);
        return InteractionResultHolder.consume(stack);
    }
}
