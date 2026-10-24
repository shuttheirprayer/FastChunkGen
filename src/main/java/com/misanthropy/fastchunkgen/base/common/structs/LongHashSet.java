package com.misanthropy.fastchunkgen.base.common.structs;

import it.unimi.dsi.fastutil.longs.LongCollection;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongSet;
import org.jetbrains.annotations.NotNull;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

import java.util.Collection;

public class LongHashSet implements LongSet {

    private final LongOpenHashSet delegate = new LongOpenHashSet();

    @Override
    public int size() {
        return delegate.size();
    }

    @Override
    public boolean isEmpty() {
        return delegate.isEmpty();
    }

    @Override
    public @NotNull LongIterator iterator() {
        return delegate.iterator();
    }

    @NotNull
    @Override
    public Object @NotNull [] toArray() {
        return delegate.toArray();
    }

    @NotNull
    @Override
    public <T> T[] toArray(@NotNull T @NotNull [] a) {
        return delegate.toArray(a);
    }

    @Override
    public boolean containsAll(@NotNull Collection<?> c) {
        return delegate.containsAll(c);
    }

    @Override
    public boolean addAll(@NotNull Collection<? extends Long> c) {
        return delegate.addAll(c);
    }

    @Override
    public boolean removeAll(@NotNull Collection<?> c) {
        return delegate.removeAll(c);
    }

    @Override
    public boolean retainAll(@NotNull Collection<?> c) {
        return delegate.retainAll(c);
    }

    @Override
    public void clear() {
        delegate.clear();
    }

    @Override
    public boolean add(long key) {
        return delegate.add(key);
    }

    @Override
    public boolean contains(long key) {
        return delegate.contains(key);
    }

    @Override
    public long[] toLongArray() {
        return delegate.toLongArray();
    }

    @Override
    public long[] toLongArray(long[] a) {
        final long[] longs = toLongArray();
        for (int i = 0; i < longs.length && i < a.length; i++) {
            a[i] = longs[i];
        }
        return a;
    }

    @Override
    public long[] toArray(long[] a) {
        return toLongArray(a);
    }

    @Override
    public boolean addAll(LongCollection c) {
        return delegate.addAll(c);
    }

    @Override
    public boolean containsAll(LongCollection c) {
        return delegate.containsAll(c);
    }

    @Override
    public boolean removeAll(LongCollection c) {
        return delegate.removeAll(c);
    }

    @Override
    public boolean retainAll(LongCollection c) {
        return delegate.retainAll(c);
    }

    @Override
    public boolean remove(long k) {
        return delegate.remove(k);
    }
}
