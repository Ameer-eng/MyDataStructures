package impl;

import java.util.*;

/**
 * This is a hashmap implemented as an array of linked lists of key value pairs.
 * For simplicity reasons, it does not support the methods keySet(), valueSet(), entrySet().
 * Instead it has an iterator() method to allow iteration over the entries of the map.
 *
 * @param <K> The key type of the map
 * @param <V> The value type of the map
 * @author Ameer Qaqish
 */
public class MyHashMap<K, V> implements Map<K, V>, Iterable<Map.Entry<K, V>> {
    private static final int DEFAULT_INITIAL_CAPACITY = 1 << 4;
    private static final double DEFAULT_LOAD_FACTOR = 0.75;

    /**
     * Since the capacity is a power of 2, higher order bits are neglected when hashing.
     * To fix this the key's hashCode is xor-ed with its upper half.
     */
    private static int hash(Object key) {
        if (key == null) {
            return 0;
        }
        int h = key.hashCode();
        return h ^ (h >>> 16);
    }

    private static int smallestPow2Geq(int n) {
        return 1 << (32 - Integer.numberOfLeadingZeros(n - 1));
    }

    private static class Node<K, V> implements Entry<K, V> {
        final int hash;
        final K key;
        V value;

        Node(int hash, K key, V value) {
            this.hash = hash;
            this.key = key;
            this.value = value;
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

        @Override
        public String toString() {
            return "(" + key + ", " + value + ")";
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            }
            if (o instanceof Map.Entry) {
                Map.Entry<?, ?> e = (Map.Entry<?, ?>) o;
                return Objects.equals(key, e.getKey()) &&
                        Objects.equals(value, e.getValue());
            }
            return false;
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(key) ^ Objects.hashCode(value);
        }
    }

    /* ---------- Fields ---------- */

    /**
     * The hashtable storing the buckets of nodes.
     * The invariant is that the table's length is always a power of 2.
     * This allows for faster mod calculations using &.
     */
    private List<Node<K, V>>[] table;

    /**
     * The number of nodes in the map.
     */
    private int size;

    /**
     * The value of size for which the table needs to be doubled in length.
     * When size becomes greater than threshold, the table's length is doubled.
     */
    private int threshold;

    /* ---------- Methods ---------- */

    /**
     * Constructs a HashMap with the given initial capacity and load factor.
     *
     * @param initialCapacity the initial capacity
     * @param loadFactor      the load factor
     */
    public MyHashMap(int initialCapacity, double loadFactor) {
        table = new List[smallestPow2Geq(initialCapacity)];
        for (int i = 0; i < table.length; i++) {
            table[i] = new LinkedList<>();
        }
        size = 0;
        threshold = (int) (table.length * loadFactor);
    }

    /**
     * Constructs a HashMap with the given initial capacity and a load factor of 0.75.
     *
     * @param initialCapacity the initial capacity
     */
    public MyHashMap(int initialCapacity) {
        this(initialCapacity, DEFAULT_LOAD_FACTOR);
    }

    /**
     * Constructs a HashMap with an initial capacity of 16 and a load factor of 0.75.
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
        return findNode(hash(key), key) != null;
    }

    private Node<K, V> findNode(int h, Object key) {
        for (Node<K, V> node : table[h & (table.length - 1)]) {
            if (Objects.equals(node.key, key)) {
                return node;
            }
        }
        return null;
    }

    @Override
    public boolean containsValue(Object value) {
        for (List<Node<K, V>> list : table) {
            for (Node<K, V> node : list) {
                if (Objects.equals(node.value, value)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public V get(Object key) {
        Node<K, V> target = findNode(hash(key), key);
        return (target == null) ? null : target.value;
    }

    @Override
    public V put(K key, V value) {
        int h = hash(key);
        Node<K, V> target = findNode(h, key);
        if (target == null) {
            table[h & (table.length - 1)].add(new Node<>(h, key, value));
            size++;
            if (size > threshold) {
                resize();
            }
            return null;
        } else {
            V oldValue = target.value;
            target.value = value;
            return oldValue;
        }
    }

    private void resize() {
        int newCap = 2 * table.length;
        threshold *= 2;
        List<Node<K, V>>[] newTable = new List[newCap];
        for (int i = 0; i < newCap; i++) {
            newTable[i] = new LinkedList<>();
        }
        for (List<Node<K, V>> list : table) {
            for (Node<K, V> node : list) {
                newTable[node.hash & (newCap - 1)].add(node);
            }
        }
        table = newTable;
    }

    @Override
    public V remove(Object key) {
        int h = hash(key);
        for (Iterator<Node<K, V>> it = table[h & (table.length - 1)].iterator(); it.hasNext(); ) {
            Node<K, V> cur = it.next();
            if (Objects.equals(cur.key, key)) {
                it.remove();
                size--;
                return cur.value;
            }
        }
        return null;
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        for (Entry<? extends K, ? extends V> node : m.entrySet()) {
            put(node.getKey(), node.getValue());
        }
    }

    @Override
    public void clear() {
        for (List<Node<K, V>> list : table) {
            list.clear();
        }
        size = 0;
    }

    /**
     * Returns an iterator over the map's entries.
     * Removal is supported.
     *
     * @return an iterator over the map's entries
     */
    @Override
    public Iterator<Entry<K, V>> iterator() {
        return new MyHashMapIterator();
    }

    private class MyHashMapIterator implements Iterator<Entry<K, V>> {
        int i;
        Iterator<Node<K, V>> it;
        boolean canRemove;

        MyHashMapIterator() {
            i = 0;
            it = table[i].iterator();
            canRemove = false;
        }

        @Override
        public boolean hasNext() {
            // Check current bucket.
            if (it.hasNext()) {
                return true;
            }

            // Find first non-emtpy bucket if it exists.
            i++;
            while (i < table.length && table[i].isEmpty()) {
                i++;
            }
            if (i >= table.length) {
                return false;
            } else {
                it = table[i].iterator();
                return true;
            }
        }

        @Override
        public Entry<K, V> next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            canRemove = true;
            return it.next();
        }

        @Override
        public void remove() {
            if (!canRemove) {
                throw new IllegalStateException();
            }
            it.remove();
            canRemove = false;
            size--;
        }
    }

    public String toString() {
        StringBuilder s = new StringBuilder("{");
        boolean first = true;
        for (Entry<K, V> node : this) {
            if (!first) {
                s.append(", ");
            }
            first = false;
            s.append(node.toString());
        }
        s.append("}");
        return s.toString();
    }

    // Copied from Java.util.AbstractMap
    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;

        if (!(o instanceof Map))
            return false;
        Map<?, ?> m = (Map<?, ?>) o;
        if (m.size() != size())
            return false;

        try {
            for (Entry<K, V> e : this) {
                K key = e.getKey();
                V value = e.getValue();
                if (value == null) {
                    if (!(m.get(key) == null && m.containsKey(key)))
                        return false;
                } else {
                    if (!value.equals(m.get(key)))
                        return false;
                }
            }
        } catch (ClassCastException | NullPointerException unused) {
            return false;
        }

        return true;
    }

    // Copied from Java.util.AbstractMap
    @Override
    public int hashCode() {
        int h = 0;
        for (Entry<K, V> entry : this) {
            h += entry.hashCode();
        }
        return h;
    }

    // Unsupported operations.
    // The iterator provides their functionality.

    @Override
    public Set<K> keySet() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection<V> values() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        throw new UnsupportedOperationException();
    }
}
