package insane96mcp.nohunger;

import com.mojang.blaze3d.systems.RenderSystem;
import insane96mcp.insanelib.core.feature.Feature;
import insane96mcp.insanelib.util.ClientUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

@EventBusSubscriber(modid = NoHunger.MOD_ID, value = Dist.CLIENT)
public class NoHungerFeatureClient {

    @SubscribeEvent
    public static void registerArmorLayer(RegisterGuiLayersEvent event) {
        event.registerBelow(VanillaGuiLayers.AIR_LEVEL, NoHunger.location("armor"), (guiGraphics, partialTick) -> {
            Minecraft mc = Minecraft.getInstance();
            if (Feature.isEnabled(NoHungerFeature.class) && NoHungerFeature.renderArmorAtHunger && mc.gameMode.canHurtPlayer())
                renderArmor(guiGraphics, mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());
        });
    }

    protected static final ResourceLocation GUI_ICONS_LOCATION = ResourceLocation.parse("textures/gui/icons.png");

    protected static void renderArmor(GuiGraphics guiGraphics, int width, int height) {
        Minecraft mc = Minecraft.getInstance();
        Gui gui = mc.gui;
        mc.getProfiler().push(NoHunger.MOD_ID + ":armor");

        RenderSystem.enableBlend();
        int left = width / 2 + 82;
        int top = height - gui.rightHeight;

        int level = mc.player.getArmorValue();
        for (int i = 1; level > 0 && i < 20; i += 2)
        {
            if (i < level)
                guiGraphics.blit(GUI_ICONS_LOCATION, left, top, 34, 9, 9, 9, 256, 256);
            else if (i == level)
                ClientUtils.blitVerticallyMirrored(GUI_ICONS_LOCATION, guiGraphics, left, top, 25, 9, 9, 9, 256, 256);
            else
                guiGraphics.blit(GUI_ICONS_LOCATION, left, top, 16, 9, 9, 9, 256, 256);
            left -= 8;
        }
        if (level > 0)
            gui.rightHeight += 10;

        RenderSystem.disableBlend();
        mc.getProfiler().pop();
    }

    protected static final ResourceLocation OT_REGEN_LOCATION = NoHunger.location("textures/gui/ot_regen.png");

    @SubscribeEvent
    public static void registerGui(RegisterGuiLayersEvent event) {
        event.registerBelow(VanillaGuiLayers.PLAYER_HEALTH, NoHunger.location("ot_regen"), (guiGraphics, partialTick) -> {
            Minecraft mc = Minecraft.getInstance();
            if (!Feature.isEnabled(NoHungerFeature.class)
                    || !mc.gameMode.canHurtPlayer())
                return;

            Player player = mc.player;
            if (player == null
                    || NoHungerFeature.getFoodRegenLeft(player) <= 0)
                return;

            int screenWidth = mc.getWindow().getGuiScaledWidth();
            int screenHeight = mc.getWindow().getGuiScaledHeight();
            int right = screenWidth / 2 - 90;
            float aRight = Mth.ceil(player.getHealth());
            int top = screenHeight - mc.gui.leftHeight - 3 + 10;
            float regenLeft = Math.round(Math.min(20, NoHungerFeature.getFoodRegenLeft(player)) + (player.getHealth() - (int) player.getHealth()));
            float regenStrength = NoHungerFeature.getFoodRegenStrength(player) * 20;
            if (regenStrength == 0f)
                return;
            int width = (int) (regenLeft / 2f * 8f);
            float healthMissing = player.getMaxHealth() - player.getHealth();
            if (healthMissing < regenLeft || player.getHealth() + regenLeft >= 20)
                aRight = 21 - regenLeft;
            right += (int) (aRight / 2f * 8f);
            if (!FMLLoader.isProduction())
                player.displayClientMessage(Component.literal("Health: " + player.getHealth() + " Right: " + right + " Width: " + width + " regenLeft: " + regenLeft + " regenStrength: " + regenStrength), true);
            ClientUtils.setRenderColor(1.2f - (regenStrength / 0.5f), 0.78f, 0.17f, 1f);
            guiGraphics.blit(OT_REGEN_LOCATION, right, top, 90 - width, 0f, width, 3, 90, 3);
            ClientUtils.resetRenderColor();
        });
    }
}
