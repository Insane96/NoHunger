package insane96mcp.nohunger.network.message;

import insane96mcp.nohunger.feature.NoHungerFeature;
import insane96mcp.nohunger.network.NetworkHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class FoodRegenSync {

    float regenAmount;
    float regenStrength;

    public FoodRegenSync(float regenAmount, float regenStrength) {
        this.regenAmount = regenAmount;
        this.regenStrength = regenStrength;
    }

    public static void encode(FoodRegenSync pkt, FriendlyByteBuf buf) {
        buf.writeFloat(pkt.regenAmount);
        buf.writeFloat(pkt.regenStrength);
    }

    public static FoodRegenSync decode(FriendlyByteBuf buf) {
        return new FoodRegenSync(buf.readFloat(), buf.readFloat());
    }

    public static void handle(final FoodRegenSync message, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> NoHungerFeature.setHealOverTime(NetworkHelper.getSidedPlayer(ctx.get()), message.regenAmount, message.regenStrength));
        ctx.get().setPacketHandled(true);
    }
}
