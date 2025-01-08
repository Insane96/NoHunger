package insane96mcp.nohunger;

import com.mojang.logging.LogUtils;
import insane96mcp.insanelib.base.Module;
import insane96mcp.nohunger.feature.NoHungerFeature;
import insane96mcp.nohunger.network.NetworkHandler;
import insane96mcp.nohunger.setup.NHCommonConfig;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(NoHunger.MOD_ID)
public class NoHunger {
    public static final String MOD_ID = "nohunger";
    public static final String RESOURCE_PREFIX = MOD_ID + ":";
    private static final Logger LOGGER = LogUtils.getLogger();

    public static Module base;

    public NoHunger() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, NHCommonConfig.CONFIG_SPEC, MOD_ID + ".toml");

        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::commonSetup);
        modEventBus.register(NoHungerFeature.class);

        //NHRegistries.REGISTRIES.forEach(register -> register.register(modEventBus));
    }

    public static void initModule() {
        base = Module.Builder.create(NoHunger.RESOURCE_PREFIX + "base", "base", ModConfig.Type.COMMON, NHCommonConfig.builder).canBeDisabled(false).build();
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        NetworkHandler.init();
    }
}
