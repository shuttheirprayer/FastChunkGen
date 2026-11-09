package com.misanthropy.fastchunkgen.fixes.worldgen.threading_issues.mixin.threading;

import com.misanthropy.fastchunkgen.base.mixin.access.IWeightedListEntry;
import com.misanthropy.fastchunkgen.fixes.worldgen.threading_issues.common.IWeightedList;
import net.minecraft.world.entity.ai.behavior.ShufflingList;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Comparator;
import java.util.List;
import java.util.Random;

@Mixin(ShufflingList.class)
public class MixinWeightedList<U> implements IWeightedList<U> {

    @Shadow @Final public List<ShufflingList.WeightedEntry<U>> entries;

    @Shadow @Final private net.minecraft.util.RandomSource random;

    /**
     * @author ishland
     * @reason create new instance on shuffling
     */
    @SuppressWarnings("unchecked")
    @Overwrite
    public ShufflingList<U> shuffle() {
        // TODO [VanillaCopy]
        final ShufflingList<U> newList = new ShufflingList<>(entries); // C2ME - use new instance
        final Random random = new Random(); // C2ME - use new instance
        ((com.misanthropy.fastchunkgen.base.mixin.access.IWeightedList<U>) newList).getEntries().forEach((entry) -> { // C2ME - use new instance
            ((IWeightedListEntry) entry).invokeSetShuffledOrder(random.nextFloat());
        });
        ((com.misanthropy.fastchunkgen.base.mixin.access.IWeightedList<U>) newList).getEntries().sort(Comparator.comparingDouble((object) -> { // C2ME - use new instance
            return ((IWeightedListEntry)object).invokeGetShuffledOrder();
        }));
        return newList; // C2ME - use new instance
    }

    @SuppressWarnings({"ConstantConditions", "unchecked"})
    @Override
    public ShufflingList<U> shuffleVanilla() {
        // TODO [VanillaCopy]
        this.entries.forEach((entry) -> {
            ((IWeightedListEntry) entry).invokeSetShuffledOrder(this.random.nextFloat());
        });
        this.entries.sort(Comparator.comparingDouble(uEntry -> ((IWeightedListEntry) uEntry).invokeGetShuffledOrder()));
        return (ShufflingList<U>) (Object) this;
    }
}
