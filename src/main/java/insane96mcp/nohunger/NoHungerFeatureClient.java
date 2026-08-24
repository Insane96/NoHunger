package insane96mcp.nohunger;

import com.mojang.blaze3d.systems.RenderSystem;
import insane96mcp.insanelib.InsaneLib;
import insane96mcp.insanelib.core.feature.Feature;
import insane96mcp.insanelib.mixin.accessor.GuiGraphicsAccessor;
import insane96mcp.insanelib.util.ClientUtils;
import insane96mcp.insanelib.util.MCUtils;
import insane96mcp.nohunger.mixin.client.GuiAccessor;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

@EventBusSubscriber(modid = NoHunger.MOD_ID, value = Dist.CLIENT)
public class NoHungerFeatureClient {
    private static final ResourceLocation ARMOR_EMPTY_SPRITE = ResourceLocation.withDefaultNamespace("hud/armor_empty");
    private static final ResourceLocation ARMOR_HALF_SPRITE = ResourceLocation.withDefaultNamespace("hud/armor_half");
    private static final ResourceLocation ARMOR_FULL_SPRITE = ResourceLocation.withDefaultNamespace("hud/armor_full");
    private static final String HEALTH_LANG = NoHunger.lang("tooltip.health");
    private static final String MISSING_HEALTH_LANG = NoHunger.lang("tooltip.missing_health");
    private static final String SEC_LANG = NoHunger.lang("tooltip.sec");

    @SubscribeEvent
    public static void registerArmorLayer(RegisterGuiLayersEvent event) {
        event.registerBelow(VanillaGuiLayers.AIR_LEVEL, NoHunger.id("armor"), (guiGraphics, partialTick) -> {
            Minecraft mc = Minecraft.getInstance();
            if (Feature.isEnabled(NoHungerFeature.class) && NoHungerFeature.renderArmorAtHunger && mc.gameMode.canHurtPlayer() && !mc.options.hideGui)
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

    protected static final ResourceLocation OT_REGEN_LOCATION = NoHunger.id("textures/gui/ot_regen.png");

    // LOWEST: registerAbove(PLAYER_HEALTH, ...) always inserts immediately after PLAYER_HEALTH, so whichever mod
    // calls it last claims that adjacent slot and pushes earlier registrants (e.g. Insane Survival Overhaul's
    // regenerating absorption bar, which also anchors to PLAYER_HEALTH) further out. Registering last here means
    // this layer renders right after PLAYER_HEALTH, before any other mod's leftHeight-inflating layer can run.
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void registerGui(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.PLAYER_HEALTH, NoHunger.id("ot_regen"), (guiGraphics, partialTick) -> {
            Minecraft mc = Minecraft.getInstance();
            if (!Feature.isEnabled(NoHungerFeature.class)
                    || mc.gameMode == null
                    || !mc.gameMode.canHurtPlayer())
                return;

            Player player = mc.player;
            if (player == null)
                return;

            float regenLeft = NoHungerFeature.getFoodRegenLeft(player);
            if (regenLeft <= 0)
                return;

            float regenStrength = NoHungerFeature.getFoodRegenStrength(player) * 20;
            if (regenStrength == 0f)
                return;

            Gui gui = mc.gui;
            GuiAccessor accessor = (GuiAccessor) gui;
            RandomSource random = accessor.getRandom();

            float currentHealth = player.getHealth();
            int ceilCurrentHealth = Mth.ceil(currentHealth);
            int displayHealth = accessor.getDisplayHealth();
            float maxHealth = player.getMaxHealth();
            float f = Math.max(maxHealth, Math.max(displayHealth, ceilCurrentHealth));
            int absorptionAmount = Mth.ceil(player.getAbsorptionAmount());
            int rows = Mth.ceil((f + absorptionAmount) / 2f / 10f);
            int rowHeight = Math.max(10 - (rows - 2), 3);

            int guiWidth = mc.getWindow().getGuiScaledWidth();
            int guiHeight = mc.getWindow().getGuiScaledHeight();
            int x = guiWidth / 2 - 91;
            int y = guiHeight - gui.leftHeight + (rows - 1) * rowHeight + 10;

            int i = Mth.ceil(f / 2f);
            int j = Mth.ceil(absorptionAmount / 2f);
            int totalPixels = Math.min(i * 8, Mth.ceil(regenLeft / 2f * 8f));
            int tickCount = gui.getGuiTicks();
            random.setSeed((long) tickCount * 312871L);
            int offsetHeartIndex = player.hasEffect(MobEffects.REGENERATION) ? tickCount % Mth.ceil(f + 5f) : -1;

            if (!FMLLoader.isProduction())
                player.displayClientMessage(Component.literal("Health: " + currentHealth + " totalPixels: " + totalPixels + " regenLeft: " + regenLeft + " regenStrength: " + regenStrength), true);

            // ClientUtils.setRenderColor(1.2f - (regenStrength / 0.5f), 0.78f, 0.17f, 1f);
            ClientUtils.setRenderColor(0.9f, 0.9f, 0.9f, 1f);
            RenderSystem.enableBlend();
            for (int l = i + j - 1; l >= 0; l--) {
                int i1 = l / 10;
                int j1 = l % 10;
                int k1 = x + j1 * 8;
                int l1 = y - i1 * rowHeight;
                if (ceilCurrentHealth + absorptionAmount <= 4)
                    l1 += random.nextInt(2);
                if (l < i && l == offsetHeartIndex)
                    l1 -= 2;

                if (l < i) {
                    int coveredPixels = Mth.clamp(totalPixels - l * 8, 0, 9);
                    if (coveredPixels > 0)
                        guiGraphics.blit(OT_REGEN_LOCATION, k1, l1, 0, 0, coveredPixels, 9, 9, 9);
                }
            }
            RenderSystem.disableBlend();
            ClientUtils.resetRenderColor();
        });
    }

    //Render before Regenerating absorption
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void removeFoodBar(final RenderGuiLayerEvent.Pre event) {
        if (!Feature.isEnabled(NoHungerFeature.class))
            return;

        if (event.getName().equals(VanillaGuiLayers.FOOD_LEVEL))
            event.setCanceled(true);
        //Remove armor bar to render it on the right
        if (event.getName().equals(VanillaGuiLayers.ARMOR_LEVEL) && NoHungerFeature.renderArmorAtHunger)
            event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
		if (!Feature.isEnabled(NoHungerFeature.class)
				|| event.getItemStack().getItem().getFoodProperties(event.getItemStack(), event.getEntity()) == null
                || !NoHungerFeature.foodTooltip$enabled)
			return;
		Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null)
            return;

        FoodProperties food = event.getItemStack().getItem().getFoodProperties(event.getItemStack(), event.getEntity());
        if (food == null)
            return;

        //ChatFormatting color = FoodDrinks.isRawFood(event.getItemStack().getItem()) ? ChatFormatting.DARK_RED : ChatFormatting.GRAY;
        ChatFormatting color = ChatFormatting.GRAY;
        MutableComponent component = null;
        if (food.saturation() < NoHungerFeature.foodHeal$saturationThreshold && NoHungerFeature.doesHealInstantly()) {
            float heal = NoHungerFeature.getInstantHealAmount(food, false);
            if (mc.options.advancedItemTooltips || NoHungerFeature.foodTooltip$alwaysAdvancedTooltip) {
                //noinspection ConstantConditions
                component = Component.literal(InsaneLib.ONE_DECIMAL_FORMATTER.format(heal))
                        .append(" ")
                        .append(Component.translatable(HEALTH_LANG));
            }
            else if (heal >= NoHungerFeature.foodTooltip$noshThreshold)
                component = Component.translatable("nohunger.tooltip.nosh");
            else
                component = Component.translatable("nohunger.tooltip.snack");
        }
        if (food.saturation() >= NoHungerFeature.foodHeal$saturationThreshold && NoHungerFeature.doesHealOverTime()) {
            //noinspection ConstantConditions
            float heal = MCUtils.computeFoodFormula(food, NoHungerFeature.foodHeal$overTime);
            if (mc.options.advancedItemTooltips || NoHungerFeature.foodTooltip$alwaysAdvancedTooltip) {
                //Half heart per second by default
                float strength = MCUtils.computeFoodFormula(food, NoHungerFeature.foodHeal$overTimeStrength);
                component = Component.literal(InsaneLib.ONE_DECIMAL_FORMATTER.format(heal))
                        .append(" ")
                        .append(Component.translatable(HEALTH_LANG))
                        .append(" / ")
                        .append(InsaneLib.ONE_DECIMAL_FORMATTER.format(heal / strength))
                        .append(" ")
                        .append(Component.translatable(SEC_LANG));
            }
            else {
                component = Component.translatable("nohunger.tooltip.meal");
                if (heal > NoHungerFeature.foodTooltip$feastThreshold)
                    component = Component.translatable("nohunger.tooltip.feast");
            }
        }
        if (component != null)
            event.getToolTip().add(component.withStyle(color).withStyle(ChatFormatting.ITALIC));
    }
}
