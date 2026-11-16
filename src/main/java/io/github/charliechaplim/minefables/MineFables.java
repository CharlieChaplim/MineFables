package io.github.charliechaplim.minefables;

import com.mojang.logging.LogUtils;
import io.github.charliechaplim.minefables.character.CharacterBootstrap;
import io.github.charliechaplim.minefables.character.CharacterRegistry;
import io.github.charliechaplim.minefables.ability.AbilityBootstrap;
import io.github.charliechaplim.minefables.command.CharacterCommands;
import io.github.charliechaplim.minefables.data.ModAttachments;
import io.github.charliechaplim.minefables.network.ResourceNetworking;
import io.github.charliechaplim.minefables.item.ModItems;
import io.github.charliechaplim.minefables.resource.ResourceEvents;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

@Mod(MineFables.MODID)
public final class MineFables {
    public static final String MODID = "minefables";
    public static final Logger LOGGER = LogUtils.getLogger();

    public MineFables(IEventBus modEventBus) {
        ModAttachments.register(modEventBus);
        ModItems.register(modEventBus);
        modEventBus.addListener(ModItems::addToCreativeTab);
        modEventBus.addListener(ResourceNetworking::register);

        NeoForge.EVENT_BUS.addListener(CharacterCommands::register);
        NeoForge.EVENT_BUS.addListener(ResourceEvents::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(ResourceEvents::onLogin);
        NeoForge.EVENT_BUS.addListener(ResourceEvents::onLogout);
        NeoForge.EVENT_BUS.addListener(ResourceEvents::onRespawn);
        NeoForge.EVENT_BUS.addListener(ResourceEvents::onDimensionChange);
        NeoForge.EVENT_BUS.addListener(ResourceEvents::onDamage);
        NeoForge.EVENT_BUS.addListener(ResourceEvents::onHeal);

        CharacterBootstrap.registerAll();
        AbilityBootstrap.registerAll();
        LOGGER.info("MineFables inicializado com {} personagens.", CharacterRegistry.getAll().size());
    }
}
