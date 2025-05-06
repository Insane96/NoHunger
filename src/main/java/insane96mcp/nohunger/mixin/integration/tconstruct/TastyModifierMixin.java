package insane96mcp.nohunger.mixin.integration.tconstruct;

import insane96mcp.nohunger.feature.NoHungerFeature;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.tools.modifiers.traits.general.TastyModifier;

@Mixin(TastyModifier.class)
public class TastyModifierMixin {

    @Inject(method = "eat", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/food/FoodData;eat(IF)V", shift = At.Shift.AFTER), remap = false)
    public void onEat(IToolStackView tool, ModifierEntry modifier, LivingEntity entity, CallbackInfo ci) {
        entity.heal(NoHungerFeature.tconstruct$tastyHealthRegen.floatValue() * modifier.getLevel());
    }
}
