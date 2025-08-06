package insane96mcp.nohunger.integration;

import com.teamabnormals.autumnity.core.registry.AutumnityMobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodProperties;

public class AtmosphericIntegration {
    public static final FoodProperties YUCCA_GATEAU_FOOD_PROPERTIES = new FoodProperties.Builder().nutrition(1).saturationMod(0).build();
    public static float tryApplyFoulTaste(LivingEntity livingEntity, float amount) {
        if (livingEntity.hasEffect(AutumnityMobEffects.FOUL_TASTE.get()))
            amount = amount * 1.2f;
        return amount;
    }
}
