package insane96mcp.nohunger;

import insane96mcp.insanelib.InsaneLib;
import insane96mcp.insanelib.core.feature.Feature;
import insane96mcp.insanelib.util.MCUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.food.FoodProperties;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

@EventBusSubscriber(modid = NoHunger.MOD_ID, value = Dist.CLIENT)
public class NoHungerClientEvents {
    private static final String HEALTH_LANG = NoHunger.lang("tooltip.health");
    private static final String MISSING_HEALTH_LANG = NoHunger.lang("tooltip.missing_health");
    private static final String SEC_LANG = NoHunger.lang("tooltip.sec");

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
                || event.getItemStack().getItem().getFoodProperties(event.getItemStack(), event.getEntity()) == null)
            return;

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null)
            return;

        if (!NoHungerFeature.foodTooltip$enabled)
            return;

        FoodProperties food = event.getItemStack().getItem().getFoodProperties(event.getItemStack(), event.getEntity());

        //TODO Raw food
        //ChatFormatting color = FoodDrinks.isRawFood(event.getItemStack().getItem()) ? ChatFormatting.DARK_RED : ChatFormatting.GRAY;
        ChatFormatting color = ChatFormatting.GRAY;
        MutableComponent component = null;
        if (food.saturation() < NoHungerFeature.foodHeal$saturationThreshold && NoHungerFeature.doesHealInstantly()) {
            //TODO Raw food
            //boolean isRawFood = FoodDrinks.isRawFood(event.getItemStack().getItem());
            //TODO Raw food
            float heal = NoHungerFeature.getInstantHealAmount(food, false);
            if (mc.options.advancedItemTooltips) {
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
            if (mc.options.advancedItemTooltips) {
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
