package insane96mcp.nohunger;

import com.mojang.blaze3d.systems.RenderSystem;
import insane96mcp.insanelib.core.feature.Feature;
import insane96mcp.insanelib.mixin.accessor.GuiGraphicsAccessor;
import insane96mcp.insanelib.util.ClientUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
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
    private static final ResourceLocation ARMOR_EMPTY_SPRITE = ResourceLocation.withDefaultNamespace("hud/armor_empty");
    private static final ResourceLocation ARMOR_HALF_SPRITE = ResourceLocation.withDefaultNamespace("hud/armor_half");
    private static final ResourceLocation ARMOR_FULL_SPRITE = ResourceLocation.withDefaultNamespace("hud/armor_full");

    @SubscribeEvent
    public static void registerArmorLayer(RegisterGuiLayersEvent event) {
        event.registerBelow(VanillaGuiLayers.AIR_LEVEL, NoHunger.location("armor"), (guiGraphics, partialTick) -> {
            Minecraft mc = Minecraft.getInstance();
            if (Feature.isEnabled(NoHungerFeature.class) && NoHungerFeature.renderArmorAtHunger && mc.gameMode.canHurtPlayer())
                renderArmor(guiGraphics, mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());
        });
    }

    protected static void renderArmor(GuiGraphics guiGraphics, int width, int height) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null)
            return;
        Gui gui = mc.gui;
        mc.getProfiler().push(NoHunger.MOD_ID + ":armor");

        RenderSystem.enableBlend();
        int left = width / 2 + 82;
        int top = height - gui.rightHeight;

        int level = mc.player.getArmorValue();
        for (int i = 1; level > 0 && i < 20; i += 2)
        {
            if (i < level)
                guiGraphics.blitSprite(ARMOR_FULL_SPRITE, left, top, 9, 9);
            else if (i == level)
                blitSpriteVerticallyMirrored(guiGraphics, ARMOR_HALF_SPRITE, left, top, 9, 9);
            else
                guiGraphics.blitSprite(ARMOR_EMPTY_SPRITE, left, top, 9, 9);
            left -= 8;
        }
        if (level > 0)
            gui.rightHeight += 10;

        RenderSystem.disableBlend();
        mc.getProfiler().pop();
    }

    protected static void blitSpriteVerticallyMirrored(GuiGraphics guiGraphics, ResourceLocation sprite, int x, int y, int width, int height) {
        TextureAtlasSprite textureAtlasSprite = Minecraft.getInstance().getGuiSprites().getSprite(sprite);
        ((GuiGraphicsAccessor) guiGraphics).invokeInnerBlit(
                textureAtlasSprite.atlasLocation(),
                x, x + width, y, y + height, 0,
                textureAtlasSprite.getU1(), textureAtlasSprite.getU0(),
                textureAtlasSprite.getV0(), textureAtlasSprite.getV1());
    }

    protected static final ResourceLocation OT_REGEN_LOCATION = NoHunger.location("textures/gui/ot_regen.png");

    @SubscribeEvent
    public static void registerGui(RegisterGuiLayersEvent event) {
        // Register the "overtime regen" (ot_regen) indicator bar just below the player's health row.
        event.registerBelow(VanillaGuiLayers.PLAYER_HEALTH, NoHunger.location("ot_regen"), (guiGraphics, partialTick) -> {
            Minecraft mc = Minecraft.getInstance();
            // Nothing to show if the feature is disabled or the player can't take damage (creative/spectator).
            if (!Feature.isEnabled(NoHungerFeature.class)
                    || mc.gameMode == null
                    || !mc.gameMode.canHurtPlayer())
                return;

            Player player = mc.player;
            // No player, or no pending regen at all: nothing to draw.
            if (player == null
                    || NoHungerFeature.getFoodRegenLeft(player) <= 0)
                return;

            int screenWidth = mc.getWindow().getGuiScaledWidth();
            int screenHeight = mc.getWindow().getGuiScaledHeight();
            // Left edge of the 90px-wide heart row; the base x position everything else is offset from.
            int right = screenWidth / 2 - 90;
            // aRight is the bar's left edge expressed in HP units (offset from `right`).
            // Default: starts exactly at the player's current health, rounded up.
            float aRight = Mth.ceil(player.getHealth());
            int top = screenHeight - mc.gui.leftHeight - 3 + 10;
            // Use the player's max health so this also renders correctly
            // when max health has been increased or decreased by other mods/effects.
            float maxHealth = player.getMaxHealth();
            // Raw, uncapped amount of health still pending from the "overtime" regen.
            float regenLeft = NoHungerFeature.getFoodRegenLeft(player);
            float regenStrength = NoHungerFeature.getFoodRegenStrength(player) * 20;
            // Add the fractional part of current health (smooths bar growth as health ticks up),
            // then cap at maxHealth + 1. The "+1" (one HP == half a heart) is a deliberate overshoot
            // allowance: it's what makes the bar poke out exactly half a heart past the last heart
            // when regen exceeds the missing health, and it also lets the bar cover the whole row
            // (aRight sliding down to 0) when regenLeft is higher than max health.
            float clampedRegenLeft = Math.round(Math.min(maxHealth + 1, regenLeft + (player.getHealth() - (int) player.getHealth())));
            if (regenStrength == 0f)
                return;
            // Pixel width of the drawn bar: 2 HP == 8px, matching the 8px spacing between hearts.
            int width = (int) (clampedRegenLeft / 2f * 8f);
            float healthMissing = maxHealth - player.getHealth();
            // If the pending regen would push the player past max health, re-anchor the bar so its
            // RIGHT edge always lands exactly at maxHealth + 1 (half a heart past the last heart)
            // instead of letting it keep growing further right as clampedRegenLeft increases.
            // Because aRight + clampedRegenLeft == maxHealth + 1 in this branch, the left edge
            // (aRight) naturally slides left as clampedRegenLeft grows, down to a floor of 0.
            if (healthMissing < clampedRegenLeft || player.getHealth() + clampedRegenLeft >= maxHealth)
                aRight = maxHealth + 1 - clampedRegenLeft;
            // Convert aRight from HP units to pixels and offset the base x position with it.
            right += (int) (aRight / 2f * 8f);
            if (!FMLLoader.isProduction())
                player.displayClientMessage(Component.literal("Health: " + player.getHealth() + " Right: " + right + " Width: " + width + " regenLeft: " + regenLeft + " regenStrength: " + regenStrength), true);
            // Tint the bar based on regen strength.
            ClientUtils.setRenderColor(1.2f - (regenStrength / 0.5f), 0.78f, 0.17f, 1f);
            // Draw the rightmost `width` pixels of the 90px-wide ot_regen texture at (right, top).
            guiGraphics.blit(OT_REGEN_LOCATION, right, top, 90 - width, 0f, width, 3, 90, 3);
            ClientUtils.resetRenderColor();
        });
    }
}
