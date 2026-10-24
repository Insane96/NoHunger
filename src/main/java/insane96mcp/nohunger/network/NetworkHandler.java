package insane96mcp.nohunger.network;

import insane96mcp.nohunger.network.message.FoodRegenSync;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class NetworkHandler {
    @SubscribeEvent
    public static void register(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar("2");
        registrar.playToClient(FoodRegenSync.TYPE, FoodRegenSync.STREAM_CODEC, FoodRegenSync::handle);
    }
}
