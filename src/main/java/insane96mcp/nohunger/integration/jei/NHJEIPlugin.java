package insane96mcp.nohunger.integration.jei;

import insane96mcp.nohunger.NoHunger;
import insane96mcp.nohunger.feature.NoHungerFeature;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;

@JeiPlugin
public class NHJEIPlugin implements IModPlugin {
    @Override
    public @NotNull ResourceLocation getPluginUid() {
        return new ResourceLocation(NoHunger.MOD_ID, "jei_plugin");
    }

    @Override
    public void registerRecipes(@NotNull IRecipeRegistration registration) {
        IModPlugin.super.registerRecipes(registration);
        if (NoHungerFeature.buffCakes)
            registration.addIngredientInfo(Items.CAKE, Component.translatable("jei.info.nohunger.cake"));
    }
}
