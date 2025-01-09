package insane96mcp.nohunger.integration;

import net.minecraft.world.entity.player.Player;
import vectorwing.farmersdelight.common.registry.ModEffects;

public class FarmersDelightIntegration {
    public static float tryApplyComfort(Player player, float amount) {
        if (player.hasEffect(ModEffects.COMFORT.get()))
            amount = amount * 1.2f;
        return amount;
    }
}
