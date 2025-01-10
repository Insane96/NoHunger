package insane96mcp.nohunger.integration;

import com.teamabnormals.autumnity.core.registry.AutumnityMobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodProperties;

public class AutumnityIntegration {
    public static final FoodProperties FOOD_PROPERTIES = new FoodProperties.Builder().nutrition(4).saturationMod(0.3f).build();
    public static float tryApplyFoulTaste(LivingEntity livingEntity, float amount) {
        if (livingEntity.hasEffect(AutumnityMobEffects.FOUL_TASTE.get()))
            amount = amount * 1.2f;
        return amount;
    }
}
