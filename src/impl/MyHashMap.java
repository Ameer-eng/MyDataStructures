package impl;

import java.util.*;

/**
 * A HashMap resolving collisions with chaining.
 * Iteration through the map's entries can be done using the iterator of the map.
 *
 * @param <K>
 * @param <V>
 *
 * @author Ameer Qaqish
 */
public class MyHashMap<K, V> implements MyMap<K, V>, Iterable<Map.Entry<K, V>> {
    private static final double DEFAULT_LOAD_FACTOR = 0.75;
    private static final int DEFAULT_INITIAL_CAPACITY = 17;

    private static class Node<K, V> implements Map.Entry<K, V> {
        K key;
        V value;
        Node<K, V> next;

        Node(K key, V value, Node<K, V> next) {
            this.key = key;
            this.value = value;
            this.next = next;
        }

        @Override
        public K getKey() {
            return key;
        }

        @Override
        public V getValue() {
            return value;
        }

        @Override
        public V setValue(V value) {
            V oldValue = this.value;
            this.value = value;
            return oldValue;
        }

        public int hashCode() {
            return Objects.hashCode(key) ^ Objects.hashCode(value);
        }

        public boolean equals(Object o) {
            if (o == this)
                return true;
            if (o instanceof Map.Entry) {
                Map.Entry<?, ?> e = (Map.Entry<?, ?>) o;
                return Objects.equals(key, e.getKey()) && Objects.equals(value, e.getValue());
            }
            return false;
        }

        @Override
        public String toString() {
            return "(" + key + ", " + value + ")";
        }
    }

    private static int nextPrime(int n) {
        if (n % 2 == 0) {
            n++;
        }
        while (!isPrime(n)) {
            n += 2;
        }
        return n;
    }

    private static boolean isPrime(int n) {
        if (n <= 1) {
            return false;
        }
        if (n == 2) {
            return true;
        }
        if (n % 2 == 0) {
            return false;
        }
        for (int i = 3; i * i <= n; i += 2) {
            if (n % i == 0) {
                return false;
            }
        }
        return true;
    }

    private Node<K, V>[] table; // table.length is always prime.
    private int size;
    private final double loadFactor;

    private int hash(Object key) {
        int h = key.hashCode() % table.length;
        return (h < 0) ? h + table.length : h;
    }

    /**
     * Constructs an empty {@code MyHashMap} with the specified initial
     * capacity and load factor.
     *
     * @param initialCapacity the initial capacity
     * @param loadFactor      the load factor
     * @throws IllegalArgumentException if the initial capacity is negative
     *                                  or the load factor is nonpositive
     */
    public MyHashMap(int initialCapacity, double loadFactor) {
        if (initialCapacity < 0) {
            throw new IllegalArgumentException("Illegal initial capacity: " + initialCapacity);
        }
        if (loadFactor <= 0 || Double.isNaN(loadFactor)) {
            throw new IllegalArgumentException("Illegal load factor: " + loadFactor);
        }
        this.loadFactor = loadFactor;
        table = new Node[nextPrime(initialCapacity)];
        size = 0;
    }

    /**
     * Constructs an empty {@code MyHashMap} with the specified initial
     * capacity and the default load factor (0.75).
     *
     * @param initialCapacity the initial capacity.
     * @throws IllegalArgumentException if the initial capacity is negative.
     */
    public MyHashMap(int initialCapacity) {
        this(initialCapacity, DEFAULT_LOAD_FACTOR);
    }

    /**
     * Constructs an empty {@code MyHashMap} with the default initial capacity
     * (17) and the default load factor (0.75).
     */
    public MyHashMap() {
        this(DEFAULT_INITIAL_CAPACITY, DEFAULT_LOAD_FACTOR);
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    @Override
    public boolean containsKey(Object key) {
        int h = hash(key);
        for (Node<K, V> cur = table[h]; cur != null; cur = cur.next) {
            if (key.equals(cur.key)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean containsValue(Object value) {
        for (Node<K, V> node : table) {
            for (Node<K, V> cur = node; cur != null; cur = cur.next) {
                if (Objects.equals(value, cur.value)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public V get(Object key) {
        int h = hash(key);
        for (Node<K, V> cur = table[h]; cur != null; cur = cur.next) {
            if (key.equals(cur.key)) {
                return cur.value;
            }
        }
        return null;
    }

    @Override
    public V put(K key, V value) {
        int h = hash(key);
        for (Node<K, V> cur = table[h]; cur != null; cur = cur.next) {
            if (key.equals(cur.key)) {
                V oldValue = cur.value;
                cur.value = value;
                return oldValue;
            }
        }
        table[h] = new Node<>(key, value, table[h]);
        size++;
        if (size >= table.length * loadFactor) {
            resize();
        }
        return null;
    }

    private void resize() {
        Node<K, V>[] oldTable = table;
        int newCap = nextPrime(2 * table.length);
        table = new Node[newCap];
        for (Node<K, V> node : oldTable) {
            for (Node<K, V> cur = node, next; cur != null; cur = next) {
                next = cur.next;
                int h = hash(cur.key);
                cur.next = table[h];
                table[h] = cur;
            }
        }
    }

    @Override
    public V remove(Object key) {
        int h = hash(key);
        for (Node<K, V> cur = table[h], prev = null; cur != null; prev = cur, cur = cur.next) {
            if (key.equals(cur.key)) {
                if (prev == null) {
                    table[h] = cur.next;
                } else {
                    prev.next = cur.next;
                }
                size--;
                return cur.value;
            }
        }
        return null;
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        for (Map.Entry<? extends K, ? extends V> entry : m.entrySet()) {
            this.put(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Removes all of the mappings from this map.
     * The map will be empty after this call returns.
     */
    @Override
    public void clear() {
        Arrays.fill(table, null);
        size = 0;
    }

    @Override
    public Iterator<Map.Entry<K, V>> iterator() {
        return new MyHashMapIterator();
    }

    private class MyHashMapIterator implements Iterator<Map.Entry<K, V>> {
        int i;
        Node<K, V> next;
        Node<K, V> lastRet;

        MyHashMapIterator() {
            lastRet = null;
            i = 0;
            while (i < table.length && table[i] == null) {
                i++;
            }
            next = (i < table.length) ? table[i] : null;
        }

        @Override
        public boolean hasNext() {
            return next != null;
        }

        @Override
        public Map.Entry<K, V> next() {
            if (next == null) {
                throw new NoSuchElementException();
            }
            lastRet = next;
            next = next.next;
            if (next == null) {
                i++;
                while (i < table.length && table[i] == null) {
                    i++;
                }
                if (i < table.length) {
                    next = table[i];
                }
            }
            return lastRet;
        }

        @Override
        public void remove() {
            if (lastRet == null) {
                throw new IllegalStateException();
            }
            MyHashMap.this.remove(lastRet.key);
            lastRet = null;
        }
    }

    @Override
    public int hashCode() {
        int h = 0;
        for (Map.Entry<K, V> entry : this) {
            h += entry.hashCode();
        }
        return h;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }

        if (!(o instanceof MyMap)) {
            return false;
        }
        MyMap<?, ?> m = (MyMap<?, ?>) o;
        if (m.size() != size()) {
            return false;
        }

        try {
            for (Map.Entry<K, V> e : this) {
                K key = e.getKey();
                V value = e.getValue();
                if (!m.containsKey(key) || !Objects.equals(m.get(key), value)) {
                    return false;
                }
            }
        } catch (ClassCastException | NullPointerException unused) {
            return false;
        }

        return true;
    }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<K, V> e : this) {
            if (!first) {
                s.append(", ");
            }
            first = false;
            s.append(e.toString());
        }
        s.append("}");
        return s.toString();
    }
}
