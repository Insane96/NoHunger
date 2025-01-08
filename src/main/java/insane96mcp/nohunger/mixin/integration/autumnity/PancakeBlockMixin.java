package insane96mcp.nohunger.mixin.integration.autumnity;

import com.teamabnormals.autumnity.common.block.PancakeBlock;
import com.teamabnormals.autumnity.core.registry.AutumnityBlocks;
import insane96mcp.nohunger.feature.NoHungerFeature;
import insane96mcp.nohunger.integration.AutumnityIntegration;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PancakeBlock.class)
public class PancakeBlockMixin {
    @Inject(method = "eatCake", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/food/FoodData;eat(IF)V"), remap = false)
    public void onEat(Level worldIn, BlockPos pos, BlockState state, Player player, ItemStack itemstack, CallbackInfoReturnable<InteractionResult> cir) {
        NoHungerFeature.healOnEat(player, AutumnityBlocks.PANCAKE.get().asItem(), AutumnityIntegration.FOOD_PROPERTIES);
    }
}
