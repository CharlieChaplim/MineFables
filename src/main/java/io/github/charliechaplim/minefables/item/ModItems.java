package io.github.charliechaplim.minefables.item;

import io.github.charliechaplim.minefables.MineFables;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

/** Registro dos itens proprios do MineFables. */
public final class ModItems {
    private static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(MineFables.MODID);

    public static final Supplier<DebuggerAppleItem> DEBUGGER_APPLE =
            ITEMS.registerItem("debugger_apple", DebuggerAppleItem::new,
                    new Item.Properties().stacksTo(1));

    public static final Supplier<WilliamPageItem> WILLIAM_PAGE =
            ITEMS.registerItem("william_page", WilliamPageItem::new,
                    new Item.Properties().stacksTo(16));

    private ModItems() {}

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }

    /** Aparece em Ferramentas e Utilitarios no Criativo. */
    public static void addToCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(DEBUGGER_APPLE.get());
            event.accept(WILLIAM_PAGE.get());
        }
    }
}
