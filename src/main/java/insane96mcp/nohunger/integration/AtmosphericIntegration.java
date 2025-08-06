package insane96mcp.nohunger.integration;

import com.teamabnormals.atmospheric.core.registry.AtmosphericMobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodProperties;

public class AtmosphericIntegration {
    public static final FoodProperties YUCCA_GATEAU_FOOD_PROPERTIES = new FoodProperties.Builder().nutrition(1).saturationMod(0).build();
    public static void tryReplacePersistence(LivingEntity livingEntity) {
        if (livingEntity.hasEffect(AtmosphericMobEffects.PERSISTENCE.get())) {
            MobEffectInstance effect = livingEntity.getEffect(AtmosphericMobEffects.PERSISTENCE.get());
            //noinspection ConstantConditions; Checking with hasEffect
            livingEntity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, effect.getDuration(), effect.getAmplifier(), effect.isAmbient(), effect.isVisible(), effect.showIcon()));
            livingEntity.removeEffect(AtmosphericMobEffects.PERSISTENCE.get());
        }
    }
}
