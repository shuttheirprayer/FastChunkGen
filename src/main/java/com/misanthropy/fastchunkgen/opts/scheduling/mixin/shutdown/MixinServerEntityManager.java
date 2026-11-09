package com.misanthropy.fastchunkgen.opts.scheduling.mixin.shutdown;

import com.misanthropy.fastchunkgen.opts.scheduling.common.ITryFlushable;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.world.level.entity.EntityPersistentStorage;
import net.minecraft.world.level.entity.PersistentEntitySectionManager;
import net.minecraft.world.level.entity.Visibility;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.function.Consumer;

@Mixin(PersistentEntitySectionManager.class)
public abstract class MixinServerEntityManager<T> implements ITryFlushable {

    @Shadow protected abstract LongSet getAllChunksToSave();

    @Shadow @Final private EntityPersistentStorage<T> permanentStorage;

    @Shadow protected abstract void processPendingLoads();

    @Shadow @Final private Long2ObjectMap<Visibility> chunkVisibility;

    @Shadow protected abstract boolean processChunkUnload(long chunkPos);

    @Shadow protected abstract boolean storeChunkSections(long chunkPos, Consumer<T> action);

    public boolean fcg$tryFlush() {
        LongSet longSet = this.getAllChunksToSave();

        if(!longSet.isEmpty()) {
            this.permanentStorage.flush(false);
            this.processPendingLoads();
            longSet.removeIf((pos) -> {
                boolean bl = this.chunkVisibility.get(pos) == Visibility.HIDDEN;
                return bl ? this.processChunkUnload(pos) : this.storeChunkSections(pos, (entity) -> {
                });
            });
        }

        this.permanentStorage.flush(true);
        return longSet.isEmpty();
    }

}
