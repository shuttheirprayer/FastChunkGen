package com.misanthropy.fastchunkgen.opts.worldgen.vanilla.mixin.surface_rules;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.misanthropy.fastchunkgen.opts.worldgen.vanilla.common.ReusableSurfaceRules;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.SurfaceRules;
import net.minecraft.world.level.levelgen.SurfaceSystem;
import net.minecraft.world.level.levelgen.WorldGenerationContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Function;

@Mixin(SurfaceSystem.class)
public class MixinSurfaceSystem {

    @Unique
    private final ConcurrentLinkedQueue<ReusableSurfaceRules> fcg$reusableRules = new ConcurrentLinkedQueue<>();

    @Inject(method = "buildSurface", at = @At("HEAD"))
    private void fcg$checkPerChunkPruning(CallbackInfo ci, @Share("fcg$pruned") LocalBooleanRef prunedRef) {
        prunedRef.set(ReusableSurfaceRules.hasModernFixPossibleBiomes());
    }

    @WrapOperation(method = "buildSurface", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/levelgen/SurfaceRules$RuleSource;apply(Ljava/lang/Object;)Ljava/lang/Object;"))
    private Object fcg$reuseRules(SurfaceRules.RuleSource source, Object context, Operation<Object> original,
                                  @Local(argsOnly = true) RandomState randomState,
                                  @Local(argsOnly = true) WorldGenerationContext generationContext,
                                  @Local(ordinal = 0) LocalRef<SurfaceRules.Context> contextRef,
                                  @Share("fcg$reusableRules") LocalRef<ReusableSurfaceRules> reusableRef,
                                  @Share("fcg$pruned") LocalBooleanRef prunedRef) {
        if (prunedRef.get()) return original.call(source, context);
        final SurfaceRules.Context fresh = (SurfaceRules.Context) context;
        ReusableSurfaceRules reusable = this.fcg$reusableRules.poll();
        if (reusable != null && reusable.matches(source, randomState, generationContext)) {
            reusable.repoint(fresh);
            contextRef.set(reusable.context());
        } else {
            reusable = new ReusableSurfaceRules(source, randomState, generationContext, fresh, (SurfaceRules.SurfaceRule) original.call(source, fresh));
        }
        reusableRef.set(reusable);
        return reusable.rule();
    }

    @Inject(method = "buildSurface", at = @At("TAIL"))
    private void fcg$returnRules(CallbackInfo ci, @Share("fcg$reusableRules") LocalRef<ReusableSurfaceRules> reusableRef) {
        final ReusableSurfaceRules reusable = reusableRef.get();
        if (reusable != null) {
            reusable.release();
            this.fcg$reusableRules.offer(reusable);
        }
    }

    @WrapOperation(method = "topMaterial", at = @At(value = "NEW", target = "Lnet/minecraft/world/level/levelgen/SurfaceRules$Context;"))
    private SurfaceRules.Context fcg$reuseTopMaterialContext(SurfaceSystem system, RandomState randomState, ChunkAccess chunk, NoiseChunk noiseChunk,
                                                             Function<BlockPos, Holder<Biome>> biomeGetter, Registry<Biome> biomes, WorldGenerationContext generationContext,
                                                             Operation<SurfaceRules.Context> original,
                                                             @Local(argsOnly = true) SurfaceRules.RuleSource source,
                                                             @Share("fcg$topMaterialRules") LocalRef<ReusableSurfaceRules> reusableRef) {
        final ReusableSurfaceRules reusable = this.fcg$reusableRules.poll();
        if (reusable != null && reusable.matches(source, randomState, generationContext)) {
            reusable.repoint(chunk, noiseChunk, biomeGetter, generationContext);
            reusableRef.set(reusable);
            return reusable.context();
        }
        return original.call(system, randomState, chunk, noiseChunk, biomeGetter, biomes, generationContext);
    }

    @WrapOperation(method = "topMaterial", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/levelgen/SurfaceRules$RuleSource;apply(Ljava/lang/Object;)Ljava/lang/Object;"))
    private Object fcg$reuseTopMaterialRules(SurfaceRules.RuleSource source, Object context, Operation<Object> original,
                                             @Share("fcg$topMaterialRules") LocalRef<ReusableSurfaceRules> reusableRef) {
        ReusableSurfaceRules reusable = reusableRef.get();
        if (reusable == null) {
            final SurfaceRules.Context fresh = (SurfaceRules.Context) context;
            final ISurfaceRulesContext access = (ISurfaceRulesContext) (Object) fresh;
            reusable = new ReusableSurfaceRules(source, access.fcg$getRandomState(), access.fcg$getGenerationContext(), fresh, (SurfaceRules.SurfaceRule) original.call(source, fresh));
            reusableRef.set(reusable);
        }
        return reusable.rule();
    }

    @Inject(method = "topMaterial", at = @At("RETURN"))
    private void fcg$returnTopMaterialRules(CallbackInfoReturnable<?> cir, @Share("fcg$topMaterialRules") LocalRef<ReusableSurfaceRules> reusableRef) {
        final ReusableSurfaceRules reusable = reusableRef.get();
        if (reusable != null) {
            reusable.release();
            this.fcg$reusableRules.offer(reusable);
        }
    }

}
