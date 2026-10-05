# FastChunkGen

basically an actual forge 1.20.1 port of [C2ME](https://github.com/RelativityMC/C2ME-fabric)

does multi-threaded chunk gen, async chunk IO, and fixes a bunch of vanilla threading bugs. all packed into one forge mod where u can turn off whatever u don't like in the config.

- minecraft `1.20.1`
- forge `47.4.22` or newer
- mainly for server side, but works fine on client / singleplayer too

## Building

```bash
./gradlew build
```

output goes to `build/libs/`:

- `fastchunkgen-<version>-all.jar`: **the actual mod**. includes mixinextras and asyncutil.
- `fastchunkgen-<version>.jar`: plain jar without the shaded dependencies (u probably don't want this one).

## Config

makes a `config/fastchunkgen.toml` when u start the game. everything is set to `"default"` by default which just means "use the built-in value". if u wanna change something just put an actual value there. unused keys get deleted automatically when u close the game.

u can also override stuff with jvm flags if u really want:

```
-Dfastchunkgen.config.override.<key>=<value>
```

### Tier 1 -- turned on by default

safe optimizations and bug fixes. shouldn't break anything. (hopefully)

| Key | Default | What it does |
| --- | --- | --- |
| `globalExecutorParallelism` | depends on cpu | thread pool size for worldgen + io |
| `fixes.chunkIoThreadingIssues` | `true` | fixes vanilla chunk io thread safety |
| `fixes.generalThreadingIssues` | `true` | fixes general vanilla chunk thread safety |
| `fixes.worldGenThreadingIssues` | `true` | fixes world gen thread safety |
| `fixes.vanillaWorldGenBugs` | `true` | fixes vanilla world gen bugs |
| `fixes.disableLoggingShutdownHook` | `true` | makes server shutting down faster and less messy |
| `generalOptimizations.reduceAllocations` | `true` | cuts down ram allocation in hot paths |
| `generalOptimizations.optimizeScheduling` | `true` | speeds up chunk task scheduling |
| `generalOptimizations.optimizeAsyncChunkRequest` | `true` | async chunk requests don't lock up main thread |
| `generalOptimizations.midTickChunkTasksInterval` | `100000` ns | does chunk work between ticks |
| `vanillaWorldGenOptimizations.enabled` | `true` | speeds up aquifers and end biomes |
| `vanillaWorldGenOptimizations.optimizeRandomInstances` | `true` | cheaper random numbers during gen |
| `ioSystem.optimizations` | `true` | chunk io tweaks (compression, nbt cache) |
| `ioSystem.async` | `true` | async loading / unloading |
| `ioSystem.replaceImpl` | parallelism >= 2 | custom chunk io implementation |
| `ioSystem.fireForgeChunkDataEvents` | `true` | fires forge `ChunkDataEvent.Save` on async saves |
| `ioSystem.forgeChunkDataEventsOnMainThread` | `true` | runs chunk data events on server thread |
| `threadedLighting.enabled` | `true` | puts lighting on its own thread |
| `threadedWorldGen.enabled` | parallelism >= 3 | **parallel world gen** |
| `generalOptimizations.autoSave.mode` | `VANILLA` | vanilla auto save timing |

### Tier 2 -- turned off by default

experimental or breaks other mods. don't touch unless u know what u doing.

| Key | Default | Why is off                                                                                                                                                                            |
| --- | --- |---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `ioSystem.gcFreeChunkSerializer` | `false` | Writes raw NBT bytes. **Breaks `ChunkDataEvent.Save`**, so mods saving extra chunk data via Forge capabilities lose everything.                                                       |
| `ioSystem.recoverFromErrors` | `true`  | Regens chunks that fail to load instead of crashing. Its just like vanilla                                                                                                            |
| `fixes.enforceSafeWorldRandomAccess` | `false` | Crashes the game instead of logging a warning when a mod touches world random off-thread. Useful for finding broken mods. But generally it will just annoy long-term running servers. |

## Mod Compatibility

FastChunkGen checks what mods u have loaded when mixins run. If this mod does stuff faster, we take over. if another mod already does the job well. just back off. it logs whatever it decides at startup anyway. doesn't touch other mods config files.

| Mod | Conflict | What happens                                                                                                                                                                                                                                       |
| --- | --- |----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **ModernFix** | `mixin.perf.worldgen_allocation` touches the same 3 methods as us (`SurfaceRules$SequenceRule.tryApply`, `SurfaceRules$Context.updateY`, `MaterialRuleList.calculate`) | **FastChunkGen is better.** Ours uses prebuilt arrays instead of iterating lists. ModernFix's `optimize_surface_rules` is untouched. set `generalOptimizations.overrideModernFixWorldGenAllocations = false` if u want ModernFix to do it instead. |
| **ModernFix** | `mixin.perf.release_protochunks` clears futures while our threaded worldgen uses them | Both can run fine, but mod logs a warning telling u what config keys to tweak if chunks ever get stuck.                                                                                                                                            |
| **ModernFix** | `bugfix.chunk_deadlock`, `bugfix.paper_chunk_patches` | Works fine. They use `@Inject`/`@WrapOperation`, FastChunkGen use higher mixin priority so both work.                                                                                                                                              |
| **Radium** (and Lithium/Canary) | `NbtCompound.copy`, and a chunk access cache on `ServerChunkCache` | We turn off our `MixinNbtCompound`/`MixinNbtList` since Radium's version is identical code. Our chunk priority mixin stays on, it binds fine alongside Radium's cache.                                                                                                                          |
| **ThreadTweak** | `Util.backgroundExecutor` / `Util.ioPool` | Compatible. Touches different methods, no issues.                                                                                                                                                                                                  |
| **FastNoise (zfastnoise)** | `NoiseBasedChunkGenerator.createBiomes` / `fillFromNoise` | Compatible. We redirect different stuff in the same methods.                                                                                                                                                                                       |
| **FerriteCore** | `ResourceLocation`, `StateHolder` | Compatible. Just accessors, no overlap.                                                                                                                                                                                                            |

**Tested on a 485 mod server** (Create, Mekanism, AE2, Immersive Engineering, Thermal, Ars Nouveau, Sophisticated Storage, YUNG's stuff, Regions Unexplored, Undergarden) with ModernFix, ThreadTweak, FastNoise, Radium and FerriteCore etc etc.. did chunk stress test with them, loaded 5000 chunks. 0 issues!

## What's changed from upstream C2ME

Stuff ported: base, chunk IO thread fixes, general thread fixes, worldgen thread fixes, vanilla worldgen bugfixes, allocation tweaks, chunk access, chunk IO tweaks, scheduling, worldgen tweaks, chunk serializer rewrite, chunk IO rewrite, threaded chunk IO, threaded lighting, threaded world gen.

Stuff **NOT** ported: `client-uncapvd`, `notickvd`, `server-utils`, `natives-opts`, `opts-math`. These caused couple of problems. I gave up on porting them. Not so useful stuff anyway.

### Extra fixes added in this port

**Data integrity**

- Block entity NBT is captured on the server thread when saving starts instead of calling `saveWithFullMetadata()` inside the IO worker. Original C2ME saved live block entities off-thread which raced with the main thread and corrupted modded blocks.
- Async saving actually fires Forge `ChunkDataEvent.Save` on the server thread now. Upstream was missing this, which silently wiped extra data added by Forge mods on every save.
- `ChunkDataEvent.Load` got moved to the main thread and runs before the chunk reaches `FULL` status.

**Hangs and crashes fixed**

- Fixed an infinite loop in threaded worldgen errors where unwrapping `CompletionException` read the same exception forever and froze a worker thread.
- Chunk scheduling loop now resets its running flag in a `finally` block so an exception doesn't permanently freeze chunk loading.
- Neighbor locks get released properly if a chunk task fails. Previously it would leak the lock and that area of the map could never generate again.
- `ChunkHolder.getOrScheduleFuture` clears placeholder futures on failure so chunks don't get stuck forever.
- Off-thread requests now clean up their ticket if loading fails instead of keeping the chunk stuck in RAM.
- Dedicated server doesn't instantly run `System.exit(0)` on shutdown anymore. It sets a 30 second timeout instead so normal shutdowns keep their logs and exit codes properly.

**Performance tweaks**

- Chunk storage thread idle loop reduced from 5000 to 200, and replaced the array set with a hash set (way faster on packs with lots of dimensions).
- Removed an unnecessary per-read timeout task that allocated 2 futures and a timer on every single chunk read. Now it only runs if debug mode is on.
- Blocking waits use proper parking instead of polling every 100µs.

**Misc / Cleanup**

- Replaced all 18 raw `System.out`/`printStackTrace` calls with actual SLF4J logs so u can tell which mod broke.
- Off-thread access errors name the method that caused it instead of just saying "async".
- Rate-limited unsafe random access warnings to 1 per minute so a broken mod can't spam log files to 50GB.
- Removed `exp4j` and Arclight `mixin-tools` dependencies that weren't even being used.
- More..

## Credits

Original C2ME by RelativityMC / ishland (MIT License).
Forge port base from [sj-hub9796/c2meF](https://github.com/sj-hub9796/c2meF).