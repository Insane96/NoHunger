package insane96mcp.nohunger;

import insane96mcp.insanelib.core.ModNBTData;
import insane96mcp.insanelib.core.feature.Feature;
import insane96mcp.insanelib.core.feature.LoadFeature;
import insane96mcp.insanelib.core.feature.Module;
import insane96mcp.insanelib.core.feature.config.Config;
import insane96mcp.insanelib.event.CakeEatEvent;
import insane96mcp.insanelib.event.PlayerExhaustionEvent;
import insane96mcp.insanelib.util.MCUtils;
import insane96mcp.nohunger.event.NHEventFactory;
import insane96mcp.nohunger.mixin.FoodDataAccessor;
import insane96mcp.nohunger.module.RuneCompat;
import insane96mcp.nohunger.network.message.FoodRegenSync;
import insane96mcp.nohunger.setup.NHEnchantments;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

@LoadFeature(module = NoHunger.MAIN_MODULE, canBeDisabled = false)
public class NoHungerFeature extends Feature {
    private static final int FOOD_REGEN_TICK_RATE = 5;

    private static ResourceLocation FOOD_REGEN_LEFT;
    private static ResourceLocation FOOD_REGEN_STRENGTH;

    /**
     * Players whose overtime regen changed without being synced to the client (e.g. due to exhaustion). Synced every {@link #FOOD_REGEN_TICK_RATE} ticks instead of on every change
     */
    private static final Set<Player> PENDING_REGEN_SYNC = Collections.newSetFromMap(new WeakHashMap<>());

    @Config(description = "The formula to calculate the health regenerated overtime when eating food. Leave empty to disable. Variables as nutrition, saturation, eat_seconds as numbers and can_always_eat as boolean can be used. This is evaluated with EvalEx https://ezylang.github.io/EvalEx/concepts/parsing_evaluation.html.")
    public static String foodHeal$overTime = "nutrition";
    @Config(description = "How much HP does food regen each second? Variables as nutrition, saturation, eat_seconds as numbers and can_always_eat as boolean can be used. This is evaluated with EvalEx https://ezylang.github.io/EvalEx/concepts/parsing_evaluation.html")
    public static String foodHeal$overTimeStrength = "MAX(0.15, 0.25 * saturation * (1 / nutrition))";
    @Config(min = 0d, description = "Over Time Heal will be consumed at the rate of exhaustion multiplied by this")
    public static Double foodHeal$overTimeDecay = 0.02d;
    @Config(description = "The formula to calculate the health restored instantly when eating. Leave empty to disable. To have the same effect as pre-Beta 1.8 food just use \"nutrition\". Variables as nutrition, saturation, eat_seconds as numbers and can_always_eat as boolean can be used. This is evaluated with EvalEx https://ezylang.github.io/EvalEx/concepts/parsing_evaluation.html.")
    public static String foodHeal$instantHeal = "0.5 * ROUND((nutrition^1.3) * 0.35, 1) / 0.5";
    @Config(min = 0d, description = "Foods below this saturation will instantly heal, foods equal or above this threshold will have overtime heal.")
    public static Double foodHeal$saturationThreshold = 4d;

    @Config(description = "If true, Hunger effect is replaced by Weakness")
    public static Boolean convertHungerToWeakness = true;

    @Config(description = "If true, Saturation effect is replaced by Haste")
    public static Boolean convertSaturationToHaste = true;

    //@Config(description = "If true, Persistance effect from Atmospheric is replaced by Speed")
    //public static Boolean convertPersistenceToSpeed = true;

    @Config(min = 0, max = 1, description = "Make cakes restore this % missing health, min 'Min Heal' health. Set to 0 to heal like other foods.")
    public static Double cakes$percentageHeal = 0.5d;
    @Config(min = 0)
    public static Double cakes$minHeal = 2d;
    @Config(description = "If true, cakes will heal overtime, otherwise will instantly heal.")
    public static Boolean cakes$healOverTime = true;

    @Config(description = "If true, you'll always be able to eat even if you're at full health")
    public static Boolean alwaysEat = false;

    //@Config(description = "How much health (each level) of the tasty modifier heals")
    //public static Double tconstruct$tastyHealthRegen = 0.25d;
    //@Config(description = "How much health per hunger point is restored when drinking food (e.g. stews with sipping)")
    //public static Double tconstruct$restoreHungerToHealthRatio = 1d;

    @Config(min = 0d, description = "Percentage of health regeneration speed from eating added by Well Fed")
    public static Double enchantments$wellFedRegenSpeedBonus = 0.25d;
    @Config(min = 0d, max = 1d, description = "Percentage of health regeneration speed from eating removed by Curse of Malnutrition")
    public static Double enchantments$curseOfMalnutritionRegenSpeedReduction = 0.35d;
    @Config(description = "If Rune Enchanting is installed, whether chestplate-only runes granting Well Fed/Curse of Malnutrition are enabled.")
    public static Boolean enchantments$runesEnabled = true;

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

        FoodDataAccessor foodData = (FoodDataAccessor) player.getFoodData();
        if (isPlayerHurt(player) || alwaysEat)
            foodData.setFoodLevel(15);
        else {
            foodData.setFoodLevel(20);
            // Player#isHurt is true when missing less than 1 health, so with food level at 20 vanilla natural regeneration would kick in
            foodData.setTickTimer(0);
        }

        if (player.hasEffect(MobEffects.HUNGER) && convertHungerToWeakness) {
            MobEffectInstance effect = player.getEffect(MobEffects.HUNGER);
            //noinspection ConstantConditions; Checking with hasEffect
            int duration = effect.isInfiniteDuration() ? MobEffectInstance.INFINITE_DURATION : effect.getDuration() + 1;
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, duration, effect.getAmplifier(), effect.isAmbient(), effect.isVisible(), effect.showIcon()));
            player.removeEffect(MobEffects.HUNGER);
        }
        if (player.hasEffect(MobEffects.SATURATION) && convertSaturationToHaste) {
            MobEffectInstance effect = player.getEffect(MobEffects.SATURATION);
            //noinspection ConstantConditions; Checking with hasEffect
            int duration = effect.isInfiniteDuration() ? MobEffectInstance.INFINITE_DURATION : (effect.getDuration() + 1) * 20;
            player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, duration, effect.getAmplifier(), effect.isAmbient(), effect.isVisible(), effect.showIcon()));
            player.removeEffect(MobEffects.SATURATION);
        }
        //if (ModList.get().isLoaded("atmospheric") && convertPersistenceToSpeed) {
        //    AtmosphericIntegration.tryReplacePersistence(player);
        //}

        if (player.tickCount % FOOD_REGEN_TICK_RATE == 0) {
            if (getFoodRegenLeft(player) > 0f)
                consumeAndHealFromFoodRegen(player);
            if (PENDING_REGEN_SYNC.remove(player) && player instanceof ServerPlayer serverPlayer)
                syncHealOverTime(serverPlayer);
        }
    }

    @SubscribeEvent
    public void onPlayerEat(LivingEntityUseItemEvent.Finish event) {
        if (!this.isEnabled()
                || event.getItem().getItem().getFoodProperties(event.getItem(), event.getEntity()) == null
                || !(event.getEntity() instanceof Player player)
                || event.getEntity().level().isClientSide)
            return;

        ItemStack stack = event.getItem();
        healOnEat(player, stack, stack.getItem().getFoodProperties(stack, player));
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

        Player player = event.getEntity();

        float regenLeft = getFoodRegenLeft(player);
        if (regenLeft <= 0)
            return;
        float regenStrength = getFoodRegenStrength(player);
        regenLeft = Math.max(regenLeft - event.getAmount() * foodHeal$overTimeDecay.floatValue(), 0f);
        // Exhaustion happens almost every tick while sprinting or swimming, so the sync is delayed
        storeHealOverTime(player, regenLeft, regenStrength);
        PENDING_REGEN_SYNC.add(player);
    }

    /**
     * stack is null when eating cakes
     */
    public static void healOnEat(Player player, @Nullable ItemStack stack, FoodProperties foodProperties) {
        //TODO Raw Food
        //boolean isRawFood = stack != null && FoodDrinks.isRawFood(stack.getItem());
        if (healsOverTime(foodProperties, stack == null))
            //TODO Raw Food
            onEatHealOverTime(player, stack, foodProperties, false);
        else
            //TODO Raw Food
            onEatInstantHeal(player, stack, foodProperties, false);
    }

    /**
     * Whether the food heals over time or instantly. If one of the two heal types is disabled, the other one is used
     */
    public static boolean healsOverTime(FoodProperties foodProperties, boolean isCake) {
        if (!doesHealOverTime())
            return false;
        if (!doesHealInstantly())
            return true;
        return (isCake && cakes$healOverTime) || foodProperties.saturation() >= foodHeal$saturationThreshold;
    }

    /**
     * stack is null when is a cake
     */
    public static void onEatHealOverTime(Player player, @Nullable ItemStack stack, FoodProperties foodProperties, boolean isRawFood) {
        if (!doesHealOverTime())
            return;

        float heal = MCUtils.computeFoodFormula(foodProperties, foodHeal$overTime);
        if (heal <= 0f)
            return;
        if (cakes$percentageHeal > 0 && stack == null)
            heal = Math.max((player.getMaxHealth() - player.getHealth()) * cakes$percentageHeal.floatValue(), cakes$minHeal.floatValue());
        /*if (isRawFood && rawFoodHealPercentage != 1d)
            heal *= rawFoodHealPercentage;*/
        heal = NHEventFactory.getApplyOverTimeHealAmount(stack == null ? new ItemStack(Items.CAKE) : stack, heal, asServerPlayer(player));
        // The overtime regen left is replaced only if the new food heals more
        if (heal <= getFoodRegenLeft(player))
            return;

        float strength = MCUtils.computeFoodFormula(foodProperties, foodHeal$overTimeStrength) / 20f;
        // Invalid formula
        if (strength <= 0f)
            return;
        setHealOverTime(player, heal, strength);
    }

    public static boolean doesHealOverTime() {
        return !StringUtils.isBlank(foodHeal$overTime) && !StringUtils.isBlank(foodHeal$overTimeStrength);
    }

    private static void onEatInstantHeal(Player player, @Nullable ItemStack stack, FoodProperties foodProperties, boolean isRawFood) {
        if (!doesHealInstantly())
            return;

        float heal = cakes$percentageHeal > 0 && stack == null
                ? Math.max((player.getMaxHealth() - player.getHealth()) * cakes$percentageHeal.floatValue(), cakes$minHeal.floatValue())
                : getInstantHealAmount(foodProperties, isRawFood);
        heal = NHEventFactory.getInstantHealAmount(stack == null ? new ItemStack(Items.CAKE) : stack, heal, asServerPlayer(player));
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
        return ModNBTData.getPersisted(player, FOOD_REGEN_LEFT, Float.class);
    }

    /**
     * Sets the overtime regen and, if on server side, syncs it to the client
     */
    public static void setHealOverTime(Player player, float amount, float strength) {
        storeHealOverTime(player, amount, strength);
        if (player instanceof ServerPlayer serverPlayer)
            syncHealOverTime(serverPlayer);
    }

    /**
     * Saved in the persisted data so it's kept when leaving the End. Reset on death in {@link #onPlayerClone}
     */
    private static void storeHealOverTime(Player player, float amount, float strength) {
        ModNBTData.putPersisted(player, FOOD_REGEN_LEFT, amount);
        ModNBTData.putPersisted(player, FOOD_REGEN_STRENGTH, strength);
    }

    private static void syncHealOverTime(ServerPlayer player) {
        PENDING_REGEN_SYNC.remove(player);
        FoodRegenSync.sync(getFoodRegenLeft(player), getFoodRegenStrength(player), player);
    }

    @Nullable
    private static ServerPlayer asServerPlayer(Player player) {
        return player instanceof ServerPlayer serverPlayer ? serverPlayer : null;
    }

    private static void consumeAndHealFromFoodRegen(Player player) {
        float regenLeft = getFoodRegenLeft(player);
        float regenStrength = getFoodRegenStrength(player);
        if (player.getHealth() >= player.getMaxHealth())
            return;
        float healAmount = regenStrength * getRegenSpeedMultiplier(player) * FOOD_REGEN_TICK_RATE;
        // Invalid strength formula
        if (healAmount <= 0f)
            return;
        if (regenLeft < healAmount)
            healAmount = regenLeft;
        if (player.getMaxHealth() - player.getHealth() < healAmount)
            healAmount = player.getMaxHealth() - player.getHealth();
        //if (ModList.get().isLoaded("farmersdelight"))
        //    healAmount = FarmersDelightIntegration.tryApplyComfort(player, healAmount);
        player.heal(NHEventFactory.getOverTimeHealAmount(healAmount, asServerPlayer(player)));
        regenLeft -= healAmount;
        if (regenLeft <= 0f)
            regenLeft = 0f;
        setHealOverTime(player, regenLeft, regenStrength);
    }

    /**
     * Multiplier applied to the overtime regen speed, from Well Fed and Curse of Malnutrition (either as enchantments or
     * as Rune Enchanting runes) on the chestplate. The total amount of health to regenerate is not affected.
     */
    public static float getRegenSpeedMultiplier(Player player) {
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        if (chest.isEmpty())
            return 1f;

        double multiplier = 1d;
        if (enchantments$wellFedRegenSpeedBonus > 0) {
            Holder<Enchantment> wellFed = player.level().registryAccess().holderOrThrow(NHEnchantments.WELL_FED);
            if (chest.getEnchantmentLevel(wellFed) > 0 || RuneCompat.getRuneLevel(chest, NHEnchantments.WELL_FED) > 0)
                multiplier *= 1d + enchantments$wellFedRegenSpeedBonus;
        }
        if (enchantments$curseOfMalnutritionRegenSpeedReduction > 0) {
            Holder<Enchantment> curseOfMalnutrition = player.level().registryAccess().holderOrThrow(NHEnchantments.CURSE_OF_MALNUTRITION);
            if (chest.getEnchantmentLevel(curseOfMalnutrition) > 0 || RuneCompat.getRuneLevel(chest, NHEnchantments.CURSE_OF_MALNUTRITION) > 0)
                multiplier *= 1d - enchantments$curseOfMalnutritionRegenSpeedReduction;
        }
        return (float) multiplier;
    }

    static float getFoodRegenStrength(Player player) {
        return ModNBTData.getPersisted(player, FOOD_REGEN_STRENGTH, Float.class);
    }

    @SubscribeEvent
    public void onPlayerClone(PlayerEvent.Clone event) {
        // Persisted data is copied on death too, but the overtime regen must be lost when dying
        if (event.isWasDeath())
            storeHealOverTime(event.getEntity(), 0f, 0f);
    }

    // The client player is created again on login, respawn and dimension change, losing the overtime regen data
    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer)
            syncHealOverTime(serverPlayer);
    }

    @SubscribeEvent
    public void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer)
            syncHealOverTime(serverPlayer);
    }

    @SubscribeEvent
    public void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer)
            syncHealOverTime(serverPlayer);
    }

    /**
     * Different from Players#isHurt as doesn't return true if missing less than half a heart
     */
    public static boolean isPlayerHurt(Player player) {
        return player.getHealth() > 0 && player.getHealth() <= player.getMaxHealth() - 1;
    }

}
