package insane96mcp.nohunger;

import com.mojang.logging.LogUtils;
import insane96mcp.insanelib.base.Module;
import insane96mcp.nohunger.network.NetworkHandler;
import insane96mcp.nohunger.setup.NHCommonConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(NoHunger.MOD_ID)
public class NoHunger {
    public static final String MOD_ID = "nohunger";
    private static final Logger LOGGER = LogUtils.getLogger();

    public static Module base;

    public NoHunger(FMLJavaModLoadingContext context) {
        context.registerConfig(ModConfig.Type.COMMON, NHCommonConfig.CONFIG_SPEC, MOD_ID + ".toml");

        IEventBus modEventBus = context.getModEventBus();
        modEventBus.addListener(this::commonSetup);
        modEventBus.register(NoHungerFeature.class);

        //NHRegistries.REGISTRIES.forEach(register -> register.register(modEventBus));
    }

    public static void initModule() {
        base = Module.Builder.create(MOD_ID + ":base", "base", ModConfig.Type.COMMON, NHCommonConfig.builder).canBeDisabled(false).build();
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        NetworkHandler.init();
    }

    public static ResourceLocation location(final String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    public static String lang(final String path) {
        return MOD_ID + "." + path;
    }
}
