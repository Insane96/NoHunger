package insane96mcp.nohunger.network;

import insane96mcp.nohunger.network.message.FoodRegenSync;
import insane96mcp.nohunger.network.message.NoHungerSync;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class NetworkHandler {
    @SubscribeEvent
    public static void register(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar("1");
        registrar.playToClient(FoodRegenSync.TYPE, FoodRegenSync.STREAM_CODEC, FoodRegenSync::handle);
        registrar.playToClient(NoHungerSync.TYPE, NoHungerSync.STREAM_CODEC, NoHungerSync::handle);
    }
}
