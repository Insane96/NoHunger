package insane96mcp.nohunger.event;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.Event;

import javax.annotation.Nullable;

/**
 * Triggered when healing is about to be applied, either instant or stored for over time, and when the player is healed due to over time healing. Check subclasses for details.
 */
public class NHHealEvent extends Event {
    private final float originalAmount;
    private float finalAmount;
    @Nullable
    private final ServerPlayer player;

    public NHHealEvent(float amount, @Nullable ServerPlayer player) {
        this.originalAmount = amount;
        this.finalAmount = amount;
        this.player = player;
    }

    /**
     * The original heal amount
     */
    public float getOriginalAmount() {
        return this.originalAmount;
    }

    /**
     * The heal amount that will be applied
     */
    public float getFinalAmount() {
        return this.finalAmount;
    }

    /**
     * Set the final heal amount that will be applied
     */
    public void setFinalAmount(float amount) {
        this.finalAmount = amount;
    }

    /**
     * The player that will be healed, or null if looking at the item's tooltip
     */
    @Nullable
    public ServerPlayer getPlayer() {
        return this.player;
    }

    /**
     * Triggered when healing is about to be applied, either instant or stored for over time. Check subclasses for details.
     */
    public static class Apply extends NHHealEvent {
        ItemStack stack;

        public Apply(float amount, ServerPlayer player, ItemStack stack) {
            super(amount, player);
            this.stack = stack;
        }

        /**
         * Triggered when healing is about to be stored into the player or when looking at the item's tooltip (in that case, the player is null).
         */
        public static class OverTime extends Apply {
            public OverTime(float amount, @Nullable ServerPlayer player, ItemStack stack) {
                super(amount, player, stack);
            }
        }

        /**
         * Triggered when healing is about to be applied to the player or when looking at the item's tooltip (in that case, the player is null).
         */
        public static class Instant extends Apply {
            public Instant(float amount, @Nullable ServerPlayer player, ItemStack stack) {
                super(amount, player, stack);
            }
        }
    }

    /**
     * Triggered when the player is healed due to over time healing
     */
    public static class OverTime extends NHHealEvent {
        public OverTime(float amount, @Nullable ServerPlayer player) {
            super(amount, player);
        }
    }
}
