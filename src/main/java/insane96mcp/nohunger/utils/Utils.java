package insane96mcp.nohunger.utils;

import com.ezylang.evalex.Expression;
import com.ezylang.evalex.data.EvaluationValue;
import insane96mcp.insanelib.util.LogHelper;
import insane96mcp.insanelib.util.MCUtils;
import net.minecraft.world.food.FoodProperties;

public class Utils {
    /**
     * Returns the hp regenerated each second
     */
    public static float computeFoodFormula(FoodProperties food, String formula) {
        Expression expression = new Expression(formula);
        try {
            //noinspection ConstantConditions
            EvaluationValue result = expression
                    .with("hunger", food.getNutrition())
                    .and("saturation_modifier", food.getSaturationModifier())
                    .and("effectiveness", MCUtils.getFoodEffectiveness(food))
                    .and("fast_food", food.isFastFood())
                    .evaluate();
            return result.getNumberValue().floatValue();
        }
        catch (Exception ex) {
            LogHelper.error("Failed to evaluate food formula: %s", expression);
            return -1f;
        }
    }
}
