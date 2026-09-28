package com.misanthropy.fastchunkgen.client.embeddium;

import com.google.common.collect.ImmutableList;
import com.misanthropy.fastchunkgen.base.ModuleEntryPoint;
import com.misanthropy.fastchunkgen.opts.scheduling.common.Config.AutoSaveMode;
import me.jellysquid.mods.sodium.client.gui.options.OptionGroup;
import me.jellysquid.mods.sodium.client.gui.options.OptionPage;
import me.jellysquid.mods.sodium.client.gui.options.control.ControlValueFormatter;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class FastChunkGenPage extends OptionPage {

    public FastChunkGenPage() {
        super(Component.translatableWithFallback("fastchunkgen.options.page", "FastChunkGen"), build());
    }

    private static Component label(String suffix) {
        final String key = "generalOptimizations.autoSave.mode." + suffix;
        return Component.translatableWithFallback("fastchunkgen.option." + key, OptionText.name(key));
    }

    private static ImmutableList<OptionGroup> build() {
        final List<OptionGroup> groups = new ArrayList<>();

        final int defaultParallelism = Math.max(1, ModuleEntryPoint.defaultParallelism);

        groups.add(OptionGroup.createBuilder()
                .add(OptionFactory.slider("globalExecutorParallelism", defaultParallelism, 1, 32, 1, ControlValueFormatter.number()))
                .add(OptionFactory.bool("threadedWorldGen.enabled", defaultParallelism >= 3))
                .add(OptionFactory.bool("threadedWorldGen.allowThreadedFeatures", true))
                .add(OptionFactory.bool("threadedWorldGen.reduceLockRadius", true))
                .add(OptionFactory.bool("threadedWorldGen.asyncScheduling", true))
                .add(OptionFactory.bool("threadedLighting.enabled", true))
                .add(OptionFactory.slider("maxViewDistance", 32, 32, 125, 1, ControlValueFormatter.number()))
                .build());

        groups.add(OptionGroup.createBuilder()
                .add(OptionFactory.bool("ioSystem.async", true))
                .add(OptionFactory.bool("ioSystem.replaceImpl", defaultParallelism >= 2))
                .add(OptionFactory.bool("ioSystem.optimizations", true))
                .add(OptionFactory.bool("ioSystem.serializeBlockEntitiesOnMainThread", true))
                .add(OptionFactory.bool("ioSystem.fireForgeChunkDataEvents", true))
                .add(OptionFactory.bool("ioSystem.forgeChunkDataEventsOnMainThread", true))
                .build());

        groups.add(OptionGroup.createBuilder()
                .add(OptionFactory.slider("ioSystem.chunkDataCacheSoftLimit", 8192, 512, 32768, 512, ControlValueFormatter.number()))
                .add(OptionFactory.slider("ioSystem.chunkDataCacheLimit", 32768, 1024, 65536, 1024, ControlValueFormatter.number()))
                .add(OptionFactory.slider("ioSystem.chunkStreamVersion", -1, -1, 3, 1, ControlValueFormatter.number()))
                .build());

        groups.add(OptionGroup.createBuilder()
                .add(OptionFactory.bool("generalOptimizations.optimizeScheduling", true))
                .add(OptionFactory.bool("generalOptimizations.optimizeAsyncChunkRequest", true))
                .add(OptionFactory.bool("generalOptimizations.reduceAllocations", true))
                .add(OptionFactory.bool("generalOptimizations.overrideModernFixWorldGenAllocations", true))
                .add(OptionFactory.sliderDisabledAtZero("generalOptimizations.midTickChunkTasksInterval", 100_000, 1_000_000, 10_000,
                        ControlValueFormatter.quantityOrDisabled("ns", "Off")))
                .build());

        groups.add(OptionGroup.createBuilder()
                .add(OptionFactory.cycling("generalOptimizations.autoSave.mode", AutoSaveMode.class, AutoSaveMode.VANILLA,
                        new Component[]{label("VANILLA"), label("ENHANCED"), label("PERIODIC")}))
                .add(OptionFactory.slider("generalOptimizations.autoSave.delay", 20_000, 1_000, 120_000, 1_000,
                        ControlValueFormatter.quantityOrDisabled("ms", "Disabled")))
                .build());

        groups.add(OptionGroup.createBuilder()
                .add(OptionFactory.bool("vanillaWorldGenOptimizations.enabled", true))
                .add(OptionFactory.bool("vanillaWorldGenOptimizations.optimizeAquifer", true))
                .add(OptionFactory.bool("vanillaWorldGenOptimizations.useEndBiomeCache", true))
                .add(OptionFactory.bool("vanillaWorldGenOptimizations.optimizeRandomInstances", true))
                .build());

        groups.add(OptionGroup.createBuilder()
                .add(OptionFactory.bool("fixes.generalThreadingIssues", true))
                .add(OptionFactory.bool("fixes.chunkIoThreadingIssues", true))
                .add(OptionFactory.bool("fixes.worldGenThreadingIssues", true))
                .add(OptionFactory.bool("fixes.vanillaWorldGenBugs", true))
                .add(OptionFactory.bool("fixes.disableLoggingShutdownHook", true))
                .add(OptionFactory.bool("fixes.enforceSafeWorldRandomAccess", false))
                .build());

        groups.add(OptionGroup.createBuilder()
                .add(OptionFactory.bool("ioSystem.gcFreeChunkSerializer", false))
                .add(OptionFactory.bool("ioSystem.recoverFromErrors", false))
                .build());

        return ImmutableList.copyOf(groups);
    }
}
