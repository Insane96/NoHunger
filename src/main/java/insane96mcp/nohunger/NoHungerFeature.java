package insane96mcp.nohunger;

import insane96mcp.insanelib.core.ModNBTData;
import insane96mcp.insanelib.core.feature.Feature;
import insane96mcp.insanelib.core.feature.LoadFeature;
import insane96mcp.insanelib.core.feature.Module;
import insane96mcp.insanelib.core.feature.config.Config;
import insane96mcp.insanelib.event.CakeEatEvent;
import insane96mcp.insanelib.event.PlayerExhaustionEvent;
import insane96mcp.insanelib.util.MCUtils;
import insane96mcp.nohunger.mixin.FoodDataAccessor;
import insane96mcp.nohunger.network.message.FoodRegenSync;
import insane96mcp.nohunger.network.message.NoHungerSync;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;

@LoadFeature(canBeDisabled = false)
public class NoHungerFeature extends Feature {
    private static final int FOOD_REGEN_TICK_RATE = 5;

    private static ResourceLocation FOOD_REGEN_LEFT;
    private static ResourceLocation FOOD_REGEN_STRENGTH;

    @Config(min = 0d, description = "The formula to calculate the health regenerated overtime when eating food. Leave empty to disable. Variables as nutrition, saturation, eat_seconds as numbers and fast_food as boolean can be used. This is evaluated with EvalEx https://ezylang.github.io/EvalEx/concepts/parsing_evaluation.html.")
    public static String foodHeal$overTime = "nutrition";
    @Config(description = "How much HP does food regen each second? Variables as nutrition, saturation, eat_seconds as numbers and fast_food as boolean can be used. This is evaluated with EvalEx https://ezylang.github.io/EvalEx/concepts/parsing_evaluation.html")
    public static String foodHeal$overTimeStrength = "MAX(0.15, 0.25 * saturation * (1 / nutrition))";
    @Config(description = "Over Time Heal will be consumed at the rate of exhaustion multiplied by this")
    public static Double foodHeal$overTimeDecay = 0.02d;
    @Config(min = 0d, description = "The formula to calculate the health restored instantly when eating. Leave empty to disable. To have the same effect as pre-Beta 1.8 food just use \"hunger\". Variables as nutrition, saturation, eat_seconds as numbers and fast_food as boolean can be used. This is evaluated with EvalEx https://ezylang.github.io/EvalEx/concepts/parsing_evaluation.html.")
    public static String foodHeal$instantHeal = "0.5 * ROUND((nutrition^1.3) * 0.35, 1) / 0.5";
    @Config(min = 0d, description = "Foods below this saturation will instantly heal, foods equal or above this threshold will have overtime heal.")
    public static Double foodHeal$saturationThreshold = 4d;

    @Config(description = "If true, Hunger effect is replaced by Weakness")
    public static Boolean convertHungerToWeakness = true;

    @Config(description = "If true, Saturation effect is replaced by Haste")
    public static Boolean convertSaturationToHaste = true;

    //@Config(description = "If true, Persistance effect from Atmospheric is replaced by Speed")
    //public static Boolean convertPersistenceToSpeed = true;

    @Config(min = 0, max = 1, description = "Make cakes restore this % missing health, min 1 health. Set to 0 to heal like other foods.")
    public static Double cakes$percentageHeal = 0.4d;
    @Config(description = "If true, cakes will heal overtime, otherwise will instantly heal.")
    public static Boolean cakes$healOverTime = true;

    @Config(description = "If true, you'll always be able to eat even if you're at full health")
    public static Boolean alwaysEat = false;

    //@Config(description = "How much health (each level) of the tasty modifier heals")
    //public static Double tconstruct$tastyHealthRegen = 0.25d;
    //@Config(description = "How much health per hunger point is restored when drinking food (e.g. stews with sipping)")
    //public static Double tconstruct$restoreHungerToHealthRatio = 1d;

    @Config(description = "(Client Only) If enabled, Foods will show \"Snack\" or \"Nosh\" when the food instantly heals and \"Meal\" or \"Feast\" when the food heals over time. If advanced tooltips are enabled, the food will show how much it restores")
    public static Boolean foodTooltip$enabled = true;
    @Config(description = "Above how much health restored food tooltip will show Nosh instead of Snack")
    public static Double foodTooltip$noshThreshold = 1d;
    @Config(description = "Above how much health restored food tooltip will show Feast instead of Meal")
    public static Double foodTooltip$feastThreshold = 7d;

    @Config(description = "(Client Only) Armor is rendered in the place of Hunger bar")
    public static Boolean renderArmorAtHunger = true;

    public void init(Module module, boolean enabledByDefault, boolean canBeDisabled) {
        super.init(module, enabledByDefault, canBeDisabled);

        FOOD_REGEN_LEFT = this.createDataKey("food_regen_left");
        FOOD_REGEN_STRENGTH = this.createDataKey("food_regen_strength");
    }

    @SubscribeEvent
    public void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (!this.isEnabled()
                || player.level().isClientSide)
            return;

        if (isPlayerHurt(player) || alwaysEat)
            ((FoodDataAccessor) player.getFoodData()).setFoodLevel(15);
        else
            ((FoodDataAccessor) player.getFoodData()).setFoodLevel(20);

        if (player.hasEffect(MobEffects.HUNGER) && convertHungerToWeakness) {
            MobEffectInstance effect = player.getEffect(MobEffects.HUNGER);
            //noinspection ConstantConditions; Checking with hasEffect
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, effect.getDuration() + 1, effect.getAmplifier(), effect.isAmbient(), effect.isVisible(), effect.showIcon()));
            player.removeEffect(MobEffects.HUNGER);
        }
        if (player.hasEffect(MobEffects.SATURATION) && convertSaturationToHaste) {
            MobEffectInstance effect = player.getEffect(MobEffects.SATURATION);
            //noinspection ConstantConditions; Checking with hasEffect
            player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, (effect.getDuration() + 1) * 20, effect.getAmplifier(), effect.isAmbient(), effect.isVisible(), effect.showIcon()));
            player.removeEffect(MobEffects.SATURATION);
        }
        //if (ModList.get().isLoaded("atmospheric") && convertPersistenceToSpeed) {
        //    AtmosphericIntegration.tryReplacePersistence(player);
        //}

        if (player.tickCount % FOOD_REGEN_TICK_RATE == 0 && getFoodRegenLeft(player) > 0f)
            consumeAndHealFromFoodRegen(player);
    }

    @SubscribeEvent
    public void onPlayerEat(LivingEntityUseItemEvent.Finish event) {
        if (!this.isEnabled()
                || event.getItem().getItem().getFoodProperties(event.getItem(), event.getEntity()) == null
                || !(event.getEntity() instanceof Player player)
                || event.getEntity().level().isClientSide)
            return;

        Item item = event.getItem().getItem();
        healOnEat(player, item, item.getFoodProperties(event.getItem(), player));
    }

    private static final FoodProperties CAKE_FOOD_PROPERTIES = new FoodProperties.Builder().nutrition(2).saturationModifier(0.1f).build();

    @SubscribeEvent
    public void onCakeEat(CakeEatEvent event) {
        if (!this.isEnabled()
                || ((Level)event.getLevel()).isClientSide)
            return;

        healOnEat(event.getEntity(), null, CAKE_FOOD_PROPERTIES);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onFoodExhaustion(PlayerExhaustionEvent event) {
        if (!isEnabled(NoHungerFeature.class)
                || event.getEntity().level().isClientSide
                || foodHeal$overTimeDecay == 0)
            return;

        ServerPlayer player = (ServerPlayer) event.getEntity();

        float regenLeft = getFoodRegenLeft(player);
        if (regenLeft <= 0)
            return;
        float regenStrength = getFoodRegenStrength(player);
        regenLeft -= event.getAmount() * foodHeal$overTimeDecay.floatValue();
        setHealOverTime(player, regenLeft, regenStrength);
    }

    /**
     * item is null when eating cakes
     */
    @SuppressWarnings("ConstantConditions")
    public static void healOnEat(Player player, @Nullable Item item, FoodProperties foodProperties) {
        //TODO Raw Food
        //boolean isRawFood = item != null && FoodDrinks.isRawFood(item);
        if ((item == null && cakes$healOverTime) || foodProperties.saturation() >= foodHeal$saturationThreshold)
            //TODO Raw Food
            onEatHealOverTime(player, item, foodProperties, false);
        else
            //TODO Raw Food
            onEatInstantHeal(player, item, foodProperties, false);
    }

    /**
     * item is null when is a cake
     */
    public static void onEatHealOverTime(Player player, @Nullable Item item, FoodProperties foodProperties, boolean isRawFood) {
        if (!doesHealOverTime())
            return;

        float heal = MCUtils.computeFoodFormula(foodProperties, foodHeal$overTime);
        if (heal <= 0f)
            return;
        if (cakes$percentageHeal > 0 && item == null)
            heal = Math.max((player.getMaxHealth() - player.getHealth()) * cakes$percentageHeal.floatValue(), 1f);
        /*if (isRawFood && rawFoodHealPercentage != 1d)
            heal *= rawFoodHealPercentage;*/
        heal = applyModifiers(player, heal);

        float strength = MCUtils.computeFoodFormula(foodProperties, foodHeal$overTimeStrength) / 20f;
        setHealOverTime(player, heal, strength);
    }

    public static boolean doesHealOverTime() {
        return !StringUtils.isBlank(foodHeal$overTime) && !StringUtils.isBlank(foodHeal$overTimeStrength);
    }

    private static void onEatInstantHeal(Player player, @Nullable Item item, FoodProperties foodProperties, boolean isRawFood) {
        if (!doesHealInstantly())
            return;

        float heal = cakes$percentageHeal > 0 && item == null
                ? Math.max((player.getMaxHealth() - player.getHealth()) * cakes$percentageHeal.floatValue(), 1f)
                : getInstantHealAmount(foodProperties, isRawFood);
        heal = applyModifiers(player, heal);
        player.heal(heal);
    }

    public static float getInstantHealAmount(FoodProperties foodProperties, boolean isRawFood) {
        float heal = MCUtils.computeFoodFormula(foodProperties, foodHeal$instantHeal);
        /*if (isRawFood && rawFoodHealPercentage != 1d)
            heal *= rawFoodHealPercentage;*/
        return heal;
    }

    public static boolean doesHealInstantly() {
        return !StringUtils.isBlank(foodHeal$instantHeal);
    }

    static float getFoodRegenLeft(Player player) {
        return ModNBTData.get(player, FOOD_REGEN_LEFT, Float.class);
    }

    public static void setHealOverTime(Player player, float amount, float strength) {
        ModNBTData.put(player, FOOD_REGEN_LEFT, amount);
        ModNBTData.put(player, FOOD_REGEN_STRENGTH, strength);
        if (player instanceof ServerPlayer serverPlayer) {
            FoodRegenSync.sync(amount, strength, serverPlayer);
        }
    }

    private static void consumeAndHealFromFoodRegen(Player player) {
        float regenLeft = getFoodRegenLeft(player);
        float regenStrength = getFoodRegenStrength(player);
        if (player.getHealth() >= player.getMaxHealth())
            return;
        float healAmount = regenStrength * FOOD_REGEN_TICK_RATE;
        if (regenLeft < healAmount)
            healAmount = regenLeft;
        if (player.getMaxHealth() - player.getHealth() < healAmount)
            healAmount = player.getMaxHealth() - player.getHealth();
        //if (ModList.get().isLoaded("farmersdelight"))
        //    healAmount = FarmersDelightIntegration.tryApplyComfort(player, healAmount);
        player.heal(healAmount);
        regenLeft -= healAmount;
        if (regenLeft <= 0f)
            regenLeft = 0f;
        setHealOverTime(player, regenLeft, regenStrength);
    }

    static float getFoodRegenStrength(Player player) {
        return ModNBTData.get(player, FOOD_REGEN_STRENGTH, Float.class);
    }

    private static float applyModifiers(Player player, float amount) {
        //if (ModList.get().isLoaded("autumnity"))
        //    amount = AutumnityIntegration.tryApplyFoulTaste(player, amount);
        return amount;
    }

    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        NoHungerSync.sync(this.isEnabled(), (ServerPlayer) event.getEntity());
    }

    /**
     * Different from Players#isHurt as doesn't return true if missing less than half a heart
     */
    public static boolean isPlayerHurt(Player player) {
        return player.getHealth() > 0 && player.getHealth() <= player.getMaxHealth() - 1;
    }

}
