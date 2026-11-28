package com.misanthropy.fastchunkgen.opts.allocs.mixin;

import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.*;

import java.util.concurrent.ConcurrentHashMap;

@Mixin(ResourceLocation.class)
public class MixinIdentifier {

    @Unique
    private static final int fcg$TEXT_LIMIT = 1 << 16;
    @Unique
    private static final ConcurrentHashMap<String, String> fcg$texts = new ConcurrentHashMap<>();

    @Shadow @Final protected String namespace;
    @Shadow @Final protected String path;
    @Unique
    private String cachedString = null;

    /**
     * @author ishland
     * @reason cache toString
     */
    @Overwrite
    public String toString() {
        if (this.cachedString != null) return this.cachedString;
        String s = this.namespace + ":" + this.path;
        String shared = fcg$texts.size() < fcg$TEXT_LIMIT ? fcg$texts.putIfAbsent(s, s) : fcg$texts.get(s);
        if (shared != null) s = shared;
        this.cachedString = s;
        return s;
    }

}
