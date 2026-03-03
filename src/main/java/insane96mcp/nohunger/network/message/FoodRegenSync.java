package insane96mcp.nohunger.network.message;

import insane96mcp.nohunger.NoHunger;
import insane96mcp.nohunger.NoHungerFeature;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record FoodRegenSync(float regenAmount, float regenStrength) implements CustomPacketPayload {

    public static final Type<FoodRegenSync> TYPE = new Type<>(NoHunger.location("food_regen_sync"));

    public static final StreamCodec<FriendlyByteBuf, FoodRegenSync> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, FoodRegenSync::regenAmount,
            ByteBufCodecs.FLOAT, FoodRegenSync::regenStrength,
            FoodRegenSync::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(FoodRegenSync payload, IPayloadContext context) {
        context.enqueueWork(() -> NoHungerFeature.setHealOverTime(context.player(), payload.regenAmount(), payload.regenStrength()));
    }

    public static void sync(float regenAmount, float regenStrength, ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new FoodRegenSync(regenAmount, regenStrength));
    }
}