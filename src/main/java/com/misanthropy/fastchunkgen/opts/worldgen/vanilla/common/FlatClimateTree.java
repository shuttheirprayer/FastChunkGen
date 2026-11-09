package com.misanthropy.fastchunkgen.opts.worldgen.vanilla.common;

import net.minecraft.world.level.biome.Climate;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public final class FlatClimateTree {

    private static final int PARAMETERS = 7;
    private static final int[] BOUND_ORDER = {4, 6, 3, 2, 0, 1, 5};
    private static final Field INDEX, ROOT, CHILDREN, VALUE, PARAMETER_SPACE;
    private static final Class<?> SUB_TREE, LEAF;

    static {
        Field index = null, root = null, children = null, value = null, space = null;
        Class<?> subTree = null, leaf = null;
        try {
            final Class<?> rTree = Class.forName("net.minecraft.world.level.biome.Climate$RTree");
            subTree = Class.forName("net.minecraft.world.level.biome.Climate$RTree$SubTree");
            leaf = Class.forName("net.minecraft.world.level.biome.Climate$RTree$Leaf");
            final Class<?> node = Class.forName("net.minecraft.world.level.biome.Climate$RTree$Node");
            index = ObfuscationReflectionHelper.findField(Climate.ParameterList.class, "f_186847_");
            root = ObfuscationReflectionHelper.findField(rTree, "f_186910_");
            children = ObfuscationReflectionHelper.findField(subTree, "f_186965_");
            value = ObfuscationReflectionHelper.findField(leaf, "f_186948_");
            space = ObfuscationReflectionHelper.findField(node, "f_186956_");
        } catch (Throwable t) {
            index = null;
        }
        INDEX = index;
        ROOT = root;
        CHILDREN = children;
        VALUE = value;
        PARAMETER_SPACE = space;
        SUB_TREE = subTree;
        LEAF = leaf;
    }

    private static final class Search {
        final long[] target = new long[PARAMETERS];
        int last = -1;
    }

    private final long[] min;
    private final long[] max;
    private final int[] firstChild;
    private final int[] childCount;
    private final Object[] values;
    private final ThreadLocal<Search> searches = ThreadLocal.withInitial(Search::new);

    private FlatClimateTree(long[] min, long[] max, int[] firstChild, int[] childCount, Object[] values) {
        this.min = min;
        this.max = max;
        this.firstChild = firstChild;
        this.childCount = childCount;
        this.values = values;
    }

    public static FlatClimateTree build(Climate.ParameterList<?> list) {
        if (INDEX == null) return null;
        try {
            final Object root = ROOT.get(INDEX.get(list));
            final List<Object> nodes = new ArrayList<>();
            nodes.add(root);
            final List<int[]> links = new ArrayList<>();
            for (int i = 0; i < nodes.size(); i++) {
                final Object node = nodes.get(i);
                if (SUB_TREE.isInstance(node)) {
                    final Object[] children = (Object[]) CHILDREN.get(node);
                    links.add(new int[]{nodes.size(), children.length});
                    for (Object child : children) nodes.add(child);
                } else if (LEAF.isInstance(node)) {
                    links.add(new int[]{0, 0});
                } else {
                    return null;
                }
            }
            final int n = nodes.size();
            final long[] min = new long[n * PARAMETERS];
            final long[] max = new long[n * PARAMETERS];
            final int[] firstChild = new int[n];
            final int[] childCount = new int[n];
            final Object[] values = new Object[n];
            for (int i = 0; i < n; i++) {
                final Object node = nodes.get(i);
                final Climate.Parameter[] space = (Climate.Parameter[]) PARAMETER_SPACE.get(node);
                if (space.length != PARAMETERS) return null;
                for (int p = 0; p < PARAMETERS; p++) {
                    min[i * PARAMETERS + p] = space[p].min();
                    max[i * PARAMETERS + p] = space[p].max();
                }
                firstChild[i] = links.get(i)[0];
                childCount[i] = links.get(i)[1];
                if (childCount[i] == 0) values[i] = VALUE.get(node);
            }
            return new FlatClimateTree(min, max, firstChild, childCount, values);
        } catch (Throwable t) {
            return null;
        }
    }

    public Object find(Climate.TargetPoint point) {
        final Search search = this.searches.get();
        final long[] target = search.target;
        target[0] = point.temperature();
        target[1] = point.humidity();
        target[2] = point.continentalness();
        target[3] = point.erosion();
        target[4] = point.depth();
        target[5] = point.weirdness();
        final int found = this.childCount[0] == 0 ? 0 : this.search(0, target, search.last);
        search.last = found;
        return this.values[found];
    }

    private int search(int node, long[] target, int candidate) {
        long best = candidate < 0 ? Long.MAX_VALUE : this.distance(candidate, target);
        int leaf = candidate;
        if (best == 0L) return leaf;
        final int end = this.firstChild[node] + this.childCount[node];
        for (int child = this.firstChild[node]; child < end; child++) {
            final long distance = this.distanceBelow(child, target, best);
            if (best > distance) {
                final int found = this.childCount[child] == 0 ? child : this.search(child, target, leaf);
                final long foundDistance = found == child ? distance : this.distance(found, target);
                if (best > foundDistance) {
                    best = foundDistance;
                    leaf = found;
                    if (best == 0L) return leaf;
                }
            }
        }
        return leaf;
    }

    private long distanceBelow(int node, long[] target, long bound) {
        final long[] min = this.min;
        final long[] max = this.max;
        final int base = node * PARAMETERS;
        long sum = 0L;
        for (int p : BOUND_ORDER) {
            final long value = target[p];
            final long above = value - max[base + p];
            final long d = above > 0L ? above : Math.max(min[base + p] - value, 0L);
            sum += d * d;
            if (sum >= bound) return bound;
        }
        return sum;
    }

    private long distance(int node, long[] target) {
        final long[] min = this.min;
        final long[] max = this.max;
        final int base = node * PARAMETERS;
        long sum = 0L;
        for (int p = 0; p < PARAMETERS; p++) {
            final long value = target[p];
            final long above = value - max[base + p];
            final long d = above > 0L ? above : Math.max(min[base + p] - value, 0L);
            sum += d * d;
        }
        return sum;
    }
}
