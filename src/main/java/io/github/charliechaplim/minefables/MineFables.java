
package io.github.charliechaplim.minefables;

import com.mojang.logging.LogUtils;
import io.github.charliechaplim.minefables.character.CharacterBootstrap;
import io.github.charliechaplim.minefables.character.CharacterRegistry;
import io.github.charliechaplim.minefables.command.CharacterCommands;
import io.github.charliechaplim.minefables.data.ModAttachments;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

@Mod(MineFables.MODID)
public final class MineFables {

    public static final String MODID = "minefables";
    public static final Logger LOGGER = LogUtils.getLogger();

    public MineFables(IEventBus modEventBus) {

        // Registra os dados persistentes dos jogadores.
        ModAttachments.register(modEventBus);

        // Registra os comandos dos personagens.
        NeoForge.EVENT_BUS.addListener(CharacterCommands::register);

        // Registra os personagens disponíveis no mod.
        CharacterBootstrap.registerAll();

        LOGGER.info(
                "MineFables inicializado com {} personagem(ns) registrado(s).",
                CharacterRegistry.getAll().size()
        );
    }
}
