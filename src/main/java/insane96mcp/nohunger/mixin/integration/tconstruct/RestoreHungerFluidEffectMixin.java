package insane96mcp.nohunger.mixin.integration.tconstruct;

import com.llamalad7.mixinextras.sugar.Local;
import insane96mcp.nohunger.NoHungerFeature;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import slimeknights.tconstruct.library.modifiers.fluid.EffectLevel;
import slimeknights.tconstruct.library.modifiers.fluid.FluidEffectContext;
import slimeknights.tconstruct.library.modifiers.fluid.entity.RestoreHungerFluidEffect;

@Mixin(RestoreHungerFluidEffect.class)
public class RestoreHungerFluidEffectMixin {
    @Inject(method = "apply(Lnet/minecraftforge/fluids/FluidStack;Lslimeknights/tconstruct/library/modifiers/fluid/EffectLevel;Lslimeknights/tconstruct/library/modifiers/fluid/FluidEffectContext$Entity;Lnet/minecraftforge/fluids/capability/IFluidHandler$FluidAction;)F", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/food/FoodData;eat(IF)V", shift = At.Shift.AFTER))
    public void onEat(FluidStack fluid, EffectLevel level, FluidEffectContext.Entity context, IFluidHandler.FluidAction action, CallbackInfoReturnable<Float> cir, @Local Player player, @Local int finalHunger) {
        player.heal(finalHunger * NoHungerFeature.tconstruct$restoreHungerToHealthRatio.floatValue());
    }
}
