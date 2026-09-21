package insane96mcp.nohunger.setup;

import insane96mcp.nohunger.NoHunger;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.enchantment.Enchantment;

/**
 * Enchantments are data-driven (data/nohunger/enchantment/*.json), this is just a typed key pointing to
 * that data. The effect on health regeneration speed can't be expressed with vanilla's data-driven effects, so
 * it's applied from {@link insane96mcp.nohunger.NoHungerFeature#getRegenSpeedMultiplier}.
 */
public class NHEnchantments {
    public static final ResourceKey<Enchantment> WELL_FED = key("well_fed");
    public static final ResourceKey<Enchantment> CURSE_OF_MALNUTRITION = key("curse_of_malnutrition");

    private static ResourceKey<Enchantment> key(String id) {
        return ResourceKey.create(Registries.ENCHANTMENT, NoHunger.id(id));
    }
}
