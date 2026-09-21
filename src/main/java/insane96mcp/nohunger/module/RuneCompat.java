package insane96mcp.nohunger.module;

import insane96mcp.nohunger.NoHungerFeature;
import insane96mcp.nohunger.setup.NHEnchantments;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.neoforged.fml.ModList;

import javax.annotation.Nullable;

/**
 * Thin, always-safe bridge to the optional Rune Enchanting integration ({@link NHRunes}). This class never
 * references any Rune Enchanting class directly, so it loads fine even when the mod isn't installed -
 * {@link #getRuneLevel} only touches {@link NHRunes} once {@link #isLoaded()} confirms it's safe to, keeping
 * {@link NHRunes} (and its hard references to Rune Enchanting's classes) from ever being loaded otherwise.
 */
public class RuneCompat {
    private static final String MOD_ID = "runeenchanting";
    @Nullable
    private static Boolean loaded;

    public static boolean isLoaded() {
        if (loaded == null)
            loaded = ModList.get().isLoaded(MOD_ID);
        return loaded;
    }

    /**
     * Returns the extra enchantment level granted by a No Hunger rune equivalent to {@code enchantment} on
     * {@code stack}, or 0 if there's none. Meant to be added to the item's real enchantment level.
     */
    public static int getRuneLevel(ItemStack stack, ResourceKey<Enchantment> enchantment) {
        if (!NoHungerFeature.enchantments$runesEnabled || !isLoaded())
            return 0;

        // Both enchantments only have a single level, so their rune equivalent is always level 1.
        if (!enchantment.equals(NHEnchantments.WELL_FED) && !enchantment.equals(NHEnchantments.CURSE_OF_MALNUTRITION))
            return 0;

        return NHRunes.getRuneLevel(stack, enchantment, 1);
    }
}
