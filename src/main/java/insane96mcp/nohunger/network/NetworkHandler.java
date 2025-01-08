package insane96mcp.nohunger.network;

import insane96mcp.nohunger.NoHunger;
import insane96mcp.nohunger.network.message.FoodRegenSync;
import insane96mcp.nohunger.network.message.NoHungerSync;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class NetworkHandler {
	private static final String PROTOCOL_VERSION = Integer.toString(1);
	public static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
			.named(new ResourceLocation(NoHunger.MOD_ID, "network_channel"))
			.clientAcceptedVersions(s -> true)
			.serverAcceptedVersions(s -> true)
			.networkProtocolVersion(() -> PROTOCOL_VERSION)
			.simpleChannel();

	private static int index = 0;

	public static void init() {
		CHANNEL.registerMessage(++index, FoodRegenSync.class, FoodRegenSync::encode, FoodRegenSync::decode, FoodRegenSync::handle);
		CHANNEL.registerMessage(++index, NoHungerSync.class, NoHungerSync::encode, NoHungerSync::decode, NoHungerSync::handle);
	}
}
