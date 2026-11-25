package com.misanthropy.fastchunkgen.opts.worldgen.vanilla.mixin.ore_veinifier;

import com.misanthropy.fastchunkgen.opts.worldgen.general.common.random_instances.RandomUtils;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.OreVeinifier;
import net.minecraft.world.level.levelgen.PositionalRandomFactory;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(OreVeinifier.class)
public class MixinOreVeinifier {

    @Unique
    private static final ThreadLocal<XoroshiroRandomSource> fcg$veinRandom = ThreadLocal.withInitial(() -> new XoroshiroRandomSource(0L, 0L));

    @Redirect(method = {"lambda$create$0", "m_209660_"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/levelgen/PositionalRandomFactory;at(III)Lnet/minecraft/util/RandomSource;"))
    private static RandomSource fcg$reuseVeinRandom(PositionalRandomFactory factory, int x, int y, int z) {
        if (factory instanceof XoroshiroRandomSource.XoroshiroPositionalRandomFactory) {
            XoroshiroRandomSource random = fcg$veinRandom.get();
            RandomUtils.derive(factory, random, x, y, z);
            return random;
        }
        return factory.at(x, y, z);
    }

}
