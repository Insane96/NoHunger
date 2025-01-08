package insane96mcp.nohunger.mixin;

import net.minecraft.world.food.FoodData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(FoodData.class)
public interface FoodDataAccessor {
    @Accessor
    int getFoodLevel();
    @Accessor
    void setFoodLevel(int foodLevel);
}
