package io.github.charliechaplim.minefables;

import io.github.charliechaplim.minefables.client.ResourceHud;
import io.github.charliechaplim.minefables.client.ClientDragonInstallState;
import io.github.charliechaplim.minefables.client.ClientImpetuoState;
import io.github.charliechaplim.minefables.client.render.ImpetuoRingLayer;
import io.github.charliechaplim.minefables.client.render.DragonInstallLayer;
import io.github.charliechaplim.minefables.client.ClientResourceState;
import io.github.charliechaplim.minefables.client.ClientAbilitySelection;
import io.github.charliechaplim.minefables.client.AbilityKeys;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

/** Ponto de entrada exclusivo do cliente: nao carrega no servidor dedicado. */
@Mod(value = MineFables.MODID, dist = Dist.CLIENT)
public final class MineFablesClient {
    public MineFablesClient(IEventBus modEventBus) {
        modEventBus.addListener(ResourceHud::register);
        modEventBus.addListener(AbilityKeys::register);
        modEventBus.addListener(DragonInstallLayer::register);
        modEventBus.addListener(ImpetuoRingLayer::register);
        NeoForge.EVENT_BUS.addListener(AbilityKeys::onClientTick);
        NeoForge.EVENT_BUS.addListener(this::onLogout);
    }
    private void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientResourceState.clear();
        ClientAbilitySelection.clear();
        ClientDragonInstallState.clear();
        ClientImpetuoState.clear();
        io.github.charliechaplim.minefables.client.ClientWilliamState.clear();
    }
}
