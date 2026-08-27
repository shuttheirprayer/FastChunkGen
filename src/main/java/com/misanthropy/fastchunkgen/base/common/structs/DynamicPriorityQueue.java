package com.misanthropy.fastchunkgen.base.common.structs;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;

/**
 * A priority queue with fixed number of priorities and allows changing priorities of elements.
 * Not thread-safe.
 *
 * @param <E> the type of elements held in this collection
 */
public class DynamicPriorityQueue<E> {

    private static final int ABSENT = -1;

    private final ObjectLinkedOpenHashSet<E>[] priorities;
    private final Object2IntOpenHashMap<E> priorityMap = new Object2IntOpenHashMap<>();

    private int currentMinPriority = 0;

    @SuppressWarnings("unchecked")
    public DynamicPriorityQueue(int priorityCount) {
        this.priorities = new ObjectLinkedOpenHashSet[priorityCount];
        for (int i = 0; i < priorityCount; i++) {
            this.priorities[i] = new ObjectLinkedOpenHashSet<>();
        }
        this.priorityMap.defaultReturnValue(ABSENT);
    }

    private void checkPriority(int priority) {
        if (priority < 0 || priority >= priorities.length)
            throw new IllegalArgumentException("Priority out of range");
    }

    public void enqueue(E element, int priority) {
        checkPriority(priority);
        if (priorityMap.containsKey(element))
            throw new IllegalArgumentException("Element already in queue");

        priorities[priority].add(element);
        priorityMap.put(element, priority);
        if (priority < currentMinPriority)
            currentMinPriority = priority;
    }

    public void changePriority(E element, int priority) {
        checkPriority(priority);
        final int oldPriority = priorityMap.getInt(element);
        if (oldPriority == ABSENT || oldPriority == priority) return;

        priorities[oldPriority].remove(element);
        priorities[priority].add(element);
        priorityMap.put(element, priority);

        if (priority < currentMinPriority) currentMinPriority = priority;
    }

    public E dequeue() {
        while (currentMinPriority < priorities.length) {
            ObjectLinkedOpenHashSet<E> priority = this.priorities[currentMinPriority];
            if (priority.isEmpty()) {
                currentMinPriority++;
                continue;
            }
            E element = priority.removeFirst();
            priorityMap.removeInt(element);
            return element;
        }
        return null;
    }

    public boolean contains(E element) {
        return priorityMap.containsKey(element);
    }

    public void remove(E element) {
        final int priority = priorityMap.removeInt(element);
        if (priority != ABSENT) priorities[priority].remove(element);
    }

    public int size() {
        return priorityMap.size();
    }

}
