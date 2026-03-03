package insane96mcp.nohunger.event;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;

import javax.annotation.Nullable;

public class NHEventFactory {
    /**
     * Called to calculate the instant heal amount
     */
    public static float getInstantHealAmount(ItemStack stack, float heal, @Nullable ServerPlayer player) {
        NHHealEvent event = new NHHealEvent.Apply.Instant(heal, player, stack);
        NeoForge.EVENT_BUS.post(event);
        return event.getFinalAmount();
    }

    public static float getApplyOverTimeHealAmount(ItemStack stack, float heal, @Nullable ServerPlayer player) {
        NHHealEvent event = new NHHealEvent.Apply.OverTime(heal, player, stack);
        NeoForge.EVENT_BUS.post(event);
        return event.getFinalAmount();
    }

    public static float getOverTimeHealAmount(float heal, @Nullable ServerPlayer player) {
        NHHealEvent event = new NHHealEvent.OverTime(heal, player);
        NeoForge.EVENT_BUS.post(event);
        return event.getFinalAmount();
    }
}
