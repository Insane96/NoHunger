package insane96mcp.nohunger;

import com.mojang.logging.LogUtils;
import insane96mcp.insanelib.setup.ILModConfig;
import insane96mcp.nohunger.module.NHRunes;
import insane96mcp.nohunger.module.RuneCompat;
import insane96mcp.nohunger.network.NetworkHandler;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;

@Mod(NoHunger.MOD_ID)
public class NoHunger {
    public static final String MOD_ID = "nohunger";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final String MAIN_MODULE = MOD_ID + ":main";
    public static final String CLIENT_MODULE = MOD_ID + ":client";

    public static ILModConfig CONFIG;
    public static ILModConfig CLIENT_CONFIG;

    public NoHunger(IEventBus eventBus, ModContainer modContainer) {
        CONFIG = new ILModConfig(ResourceLocation.parse(MAIN_MODULE), "Single Module", ModConfig.Type.SERVER,
                eventBus, NoHunger.class.getClassLoader());
        modContainer.registerConfig(ModConfig.Type.SERVER, CONFIG.spec);
        CLIENT_CONFIG = new ILModConfig(ResourceLocation.parse(CLIENT_MODULE), "Client", ModConfig.Type.CLIENT,
                eventBus, NoHunger.class.getClassLoader());
        modContainer.registerConfig(ModConfig.Type.CLIENT, CLIENT_CONFIG.spec);

        eventBus.addListener(NetworkHandler::register);

        if (RuneCompat.isLoaded())
            new NHRunes(eventBus);

        //NHRegistries.REGISTRIES.forEach(register -> register.register(modEventBus));
    }

    public static ResourceLocation id(final String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    public static String lang(final String path) {
        return MOD_ID + "." + path;
    }
}
