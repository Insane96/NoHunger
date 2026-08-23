package insane96mcp.nohunger.network.message;

import insane96mcp.insanelib.core.feature.Module;
import insane96mcp.nohunger.NoHunger;
import insane96mcp.nohunger.NoHungerFeature;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record NoHungerSync(boolean noHunger) implements CustomPacketPayload {

    public static final Type<NoHungerSync> TYPE = new Type<>(NoHunger.id("no_hunger_sync"));

    public static final StreamCodec<FriendlyByteBuf, NoHungerSync> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, NoHungerSync::noHunger,
            NoHungerSync::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(NoHungerSync payload, IPayloadContext context) {
        context.enqueueWork(() -> Module.getFeature(NoHungerFeature.class).setEnabledConfig(payload.noHunger()));
    }

    public static void sync(boolean noHunger, ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new NoHungerSync(noHunger));
    }
}