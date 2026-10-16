package insane96mcp.nohunger;

import com.mojang.logging.LogUtils;
import insane96mcp.insanelib.setup.ILModConfig;
import insane96mcp.nohunger.network.NetworkHandler;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;

@Mod(NoHunger.MOD_ID)
public class NoHunger {
    public static final String MOD_ID = "nohunger";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static ILModConfig CONFIG;

    public NoHunger(IEventBus eventBus, ModContainer modContainer) {
        CONFIG = new ILModConfig(id("main"), "Single Module", ModConfig.Type.COMMON,
                eventBus, NoHunger.class.getClassLoader());
        modContainer.registerConfig(ModConfig.Type.COMMON, CONFIG.spec);

        eventBus.addListener(NetworkHandler::register);

        // TODO Rune Enchanting has no 26.1.2 version yet, re-enable (with the dependency in build.gradle) when it's ported
        //if (RuneCompat.isLoaded())
        //    new NHRunes(eventBus);

        //NHRegistries.REGISTRIES.forEach(register -> register.register(modEventBus));
    }

    public static Identifier id(final String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    public static String lang(final String path) {
        return MOD_ID + "." + path;
    }
}
