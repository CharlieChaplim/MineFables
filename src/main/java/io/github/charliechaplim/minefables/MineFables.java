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
        // Registro de dados persistentes do jogador no barramento de inicializacao.
        ModAttachments.register(modEventBus);

        // Os comandos sao registrados quando um servidor cria sua arvore de comandos.
        NeoForge.EVENT_BUS.addListener(CharacterCommands::register);

        // Elenco do MineFables. O registro de personagens nao seleciona nenhum jogador.
        CharacterBootstrap.registerAll();
        LOGGER.info("MineFables: {} personagens registrados.", CharacterRegistry.getAll().size());
    }
}
