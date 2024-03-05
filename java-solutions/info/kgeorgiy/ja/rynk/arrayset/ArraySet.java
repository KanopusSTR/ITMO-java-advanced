package info.kgeorgiy.ja.rynk.arrayset;

import java.util.*;

public class ArraySet<T> extends AbstractSet<T> implements java.util.SortedSet<T> {

    private final Comparator<? super T> comparator;
    private final List<T> list;

    public ArraySet() {
        this(List.of(), null);
    }

    public ArraySet(Collection<? extends T> collection) {
        this(collection, null);
    }

    public ArraySet(Collection<? extends T> collection, Comparator<? super T> comparator) {

        if (collection instanceof TreeSet && (new TreeSet<>(collection).comparator() == comparator)) {
            list = List.copyOf(collection);
        } else {
            TreeSet<T> treeset = new TreeSet<>(comparator);
            treeset.addAll(collection);
            list = List.copyOf(treeset);
        }
        this.comparator = comparator;
    }

    private ArraySet(List<T> list, Comparator<? super T> comparator) {
        this.list = list;
        this.comparator = comparator;
    }

    @SuppressWarnings("unchecked")
    private int binSearch(Object o) {
        return Collections.binarySearch(list, (T) o, comparator);
    }

    private int getIndex(Object o) {
        int loc = binSearch(o);
        return loc < 0 ? ~loc : loc;
    }

    @Override
    public boolean contains(Object o) {
        return binSearch(o) >= 0;
    }

    @Override
    public Iterator<T> iterator() {
        return list.iterator();
    }

    @Override
    public int size() {
        return list.size();
    }

    @Override
    public Comparator<? super T> comparator() {
        return comparator;
    }

    @SuppressWarnings("unchecked")
    @Override
    public SortedSet<T> subSet(Object fromElement, Object toElement) {
        int startPtr = getIndex(fromElement);
        int endPtr = getIndex(toElement);
        if (comparator == null && ((Comparable<T>) fromElement).compareTo((T) toElement) > 0
                || comparator != null && comparator.compare((T) fromElement, (T) toElement) > 0) {
            throw new java.lang.IllegalArgumentException("expected fromKey > toKey");
        }
        return new ArraySet<>(list.subList(startPtr, endPtr), comparator);
    }

    @Override
    public SortedSet<T> headSet(Object o) {
        int ptr = getIndex(o);
        return new ArraySet<>(list.subList(0, ptr), comparator);
    }

    @Override
    public SortedSet<T> tailSet(Object o) {
        int ptr = getIndex(o);
        return new ArraySet<>(list.subList(ptr, size()), comparator);
    }

    @Override
    public T first() {
        checkNotEmpty();
        return list.get(0);
    }

    @Override
    public T last() {
        checkNotEmpty();
        return list.get(list.size() - 1);
    }

    private void checkNotEmpty() {
        if (list.isEmpty()) {
            throw new NoSuchElementException();
        }
    }
}
