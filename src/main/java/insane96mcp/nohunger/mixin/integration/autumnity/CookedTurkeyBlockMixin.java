package insane96mcp.nohunger.mixin.integration.autumnity;

import com.teamabnormals.autumnity.common.block.CookedTurkeyBlock;
import com.teamabnormals.autumnity.core.registry.AutumnityItems;
import insane96mcp.nohunger.feature.NoHungerFeature;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CookedTurkeyBlock.class)
public class CookedTurkeyBlockMixin {
    @Inject(method = "restoreHunger", at = @At("HEAD"), remap = false)
    public void onEat(LevelAccessor worldIn, Player player, CallbackInfo ci) {
        NoHungerFeature.healOnEat(player, AutumnityItems.COOKED_TURKEY_PIECE.get(), AutumnityItems.AutumnityFoods.COOKED_TURKEY);
    }
}
