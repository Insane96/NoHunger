package insane96mcp.nohunger.mixin.integration.atmospheric;

import com.teamabnormals.atmospheric.common.block.YuccaGateauBlock;
import com.teamabnormals.atmospheric.core.registry.AtmosphericBlocks;
import insane96mcp.nohunger.NoHungerFeature;
import insane96mcp.nohunger.integration.AtmosphericIntegration;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(YuccaGateauBlock.class)
public class YuccaGateauBlockMixin {
    @Inject(method = "eatCake", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/food/FoodData;eat(IF)V"))
    public void onEat(LevelAccessor worldIn, BlockPos pos, BlockState state, Player player, CallbackInfoReturnable<InteractionResult> cir) {
        NoHungerFeature.healOnEat(player, AtmosphericBlocks.YUCCA_GATEAU.get().asItem(), AtmosphericIntegration.YUCCA_GATEAU_FOOD_PROPERTIES);
    }
}
