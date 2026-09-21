package insane96mcp.nohunger.module;

import insane96mcp.insanelib.InsaneLib;
import insane96mcp.nohunger.NoHunger;
import insane96mcp.nohunger.NoHungerFeature;
import insane96mcp.nohunger.setup.NHEnchantments;
import insane96mcp.runeenchanting.RuneHelper;
import insane96mcp.runeenchanting.runes.Rune;
import insane96mcp.runeenchanting.setup.RERunes;
import net.minecraft.core.Holder;
import net.minecraft.data.tags.IntrinsicHolderTagsProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.RegisterEvent;

import javax.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Registers a Rune Enchanting equivalent for No Hunger's own enchantments (Well Fed, Curse of Malnutrition), so the
 * effect can also be obtained from a rune instead of only from the real enchantment. Only ever constructed if
 * Rune Enchanting is installed (see {@link RuneCompat} and {@link NoHunger}), so this class - and therefore its
 * references to Rune Enchanting's classes - is never loaded otherwise.
 * <p>
 * These runes carry no gameplay logic of their own: {@link RuneCompat#getRuneLevel} is queried directly from
 * the same place that already reads the real enchantment level ({@link NoHungerFeature#getRegenSpeedMultiplier}),
 * and the returned rune-equivalent level is simply added on top.
 */
public class NHRunes {
    private static final Map<ResourceKey<Enchantment>, Rune> RUNES = new LinkedHashMap<>();

    public NHRunes(IEventBus modEventBus) {
        modEventBus.addListener(this::onRegister);
    }

    private void onRegister(RegisterEvent event) {
        event.register(RERunes.REGISTRY_KEY, helper -> {
            register(helper, NHEnchantments.WELL_FED, "well_fed", new WellFedRune());
            register(helper, NHEnchantments.CURSE_OF_MALNUTRITION, "curse_of_malnutrition", new CurseOfMalnutritionRune());
        });
    }

    private static void register(RegisterEvent.RegisterHelper<Rune> helper, ResourceKey<Enchantment> enchantment, String id, Rune rune) {
        helper.register(NoHunger.id(id), rune);
        RUNES.put(enchantment, rune);
    }

    /**
     * Returns {@code levelEquivalent} if {@code stack} carries the rune equivalent to {@code enchantment}, 0
     * otherwise. Called only through {@link RuneCompat#getRuneLevel}, which already checked this class is safe
     * to touch.
     */
    public static int getRuneLevel(ItemStack stack, ResourceKey<Enchantment> enchantment, int levelEquivalent) {
        Rune rune = RUNES.get(enchantment);
        if (rune == null)
            return 0;

        Holder<Rune> holder = RERunes.REGISTRY.wrapAsHolder(rune);
        return RuneHelper.hasRune(stack, holder) ? levelEquivalent : 0;
    }

    public static class WellFedRune extends Rune {
        @Override
        public String getName() {
            return "Well Fed";
        }

        @Override
        public String getDescription() {
            return "Increases health regeneration speed from eating";
        }

        @Override
        public @Nullable String getInfo() {
            return "Health regeneration speed from eating: +%s%%";
        }

        @Override
        public MutableComponent getInfoComponent() {
            return Component.translatable(getInfoTranslationKey(),
                    InsaneLib.ONE_DECIMAL_FORMATTER.format(NoHungerFeature.enchantments$wellFedRegenSpeedBonus * 100));
        }

        @Override
        public void addItemsToApplicableTag(IntrinsicHolderTagsProvider.IntrinsicTagAppender<Item> appender) {}
    }

    public static class CurseOfMalnutritionRune extends Rune {
        @Override
        public String getName() {
            return "Curse of Malnutrition";
        }

        @Override
        public String getDescription() {
            return "Reduces health regeneration speed from eating";
        }

        @Override
        public @Nullable String getInfo() {
            return "Health regeneration speed from eating: -%s%%";
        }

        @Override
        public MutableComponent getInfoComponent() {
            return Component.translatable(getInfoTranslationKey(),
                    InsaneLib.ONE_DECIMAL_FORMATTER.format(NoHungerFeature.enchantments$curseOfMalnutritionRegenSpeedReduction * 100));
        }

        @Override
        public void addItemsToApplicableTag(IntrinsicHolderTagsProvider.IntrinsicTagAppender<Item> appender) {}
    }
}
