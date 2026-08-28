package guru.nicks.commons.designpattern;

import guru.nicks.commons.utils.LockUtils;

import jakarta.annotation.Nullable;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.locks.StampedLock;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

import static guru.nicks.commons.validation.dsl.ValiDsl.checkNotNull;

/**
 * Decorates {@link LinkedHashMap} so that keys representing child classes are always stored before keys representing
 * their parent classes. This doesn't mean such keys are adjacent - an arbitrary number of other keys may be between
 * them, depending on how the map was populated.
 * <p>
 * Inserting a new key is a complex and destructive operation (some existing keys is removed, then the new key is added,
 * then the old keys are restored), therefore <b>the following methods are thread-safe</b> (using {@link StampedLock} in
 * optimistic mode whenever possible):
 * <ul>
 *  <li>{@link #size()}</li>
 *  <li>{@link #isEmpty()}</li>
 *  <li>{@link #containsKey(Object)}</li>
 *  <li>{@link #containsValue(Object)}</li>
 *  <li>{@link #get(Object)}</li>
 *  <li>{@link #getOrDefault(Object, Object)}</li>
 *  <li>{@link #put(Class, Object)}</li>
 *  <li>{@link #putAll(Map)}</li>
 *  <li>{@link #putIfAbsent(Class, Object)}</li>
 *  <li>{@link #remove(Object)}</li>
 *  <li>{@link #clear()}</li>
 *  <li>{@link #forEach(BiConsumer)}</li>
 *  <li>{@link #replaceAll(BiFunction)}</li>
 *  <li>{@link #computeIfAbsent(Class, Function)}</li>
 *  <li>{@link #compute(Class, BiFunction)}</li>
 *  <li>{@link #merge(Class, Object, BiFunction)}</li>
 *  <li>{@link #entrySet()} - use this care because this is a view, not a copy, so processing this collection while
 *      another thread is putting something leads to unpredictable results</li>
 *  <li>{@link #keySet()} - use this care because this is a view, not a copy, so processing this collection while
 *      another thread is putting something leads to unpredictable results</li>
 *  <li>{@link #values()} - use this care because this is a view, not a copy, so processing this collection while
 *       another thread is putting something leads to unpredictable results</li>
 *  <li>{@link #findEntryForClosestSuperclass(Class)}</li>
 *  <li>{@link #toString()}</li>
 * </ul>
 * <p>
 * This class does not inherit from {@link LinkedHashMap}, rather decorates it, because the latter is a
 * {@link java.util.HashMap} which doesn't route all methods via the custom {@link #put(Class, Object)} (like default
 * methods in {@link Map} do) which it's responsible for maintaining key order.
 * <p>
 * {@link LinkedHashMap} is used instead of {@link java.util.TreeMap} because the latter doesn't compare new keys to
 * ALL the existing keys (Red-Black trees just don't need that). This makes the custom key comparator malfunction
 * when it comes to comparing two unrelated classes: comparing, for example, their full names breaks the positions of
 * parent-child classes.
 *
 * @param <K> key type (its class will be stored, so don't pass {@code Class<I>}, it'd store {@code Class<Class>I>>})
 * @param <V> value type
 */
public class SubclassBeforeSuperclassMap<K, V> implements Map<Class<? extends K>, V> {

    private final LinkedHashMap<Class<? extends K>, V> delegate = new LinkedHashMap<>();

    private final StampedLock lock = new StampedLock();

    @Override
    public int size() {
        return LockUtils.withOptimisticReadOrRetry(lock, delegate::size);
    }

    @Override
    public boolean isEmpty() {
        return LockUtils.withOptimisticReadOrRetry(lock, delegate::isEmpty);
    }

    @Override
    public boolean containsKey(@Nullable Object key) {
        return LockUtils.withOptimisticReadOrRetry(lock, () -> delegate.containsKey(key));
    }

    @Override
    public boolean containsValue(@Nullable Object value) {
        return LockUtils.withOptimisticReadOrRetry(lock, () -> delegate.containsValue(value));
    }

    @Nullable
    @Override
    public V get(@Nullable Object key) {
        return LockUtils.withOptimisticReadOrRetry(lock, () -> delegate.get(key));
    }

    @Nullable
    @Override
    public V put(@Nullable Class<? extends K> key, @Nullable V value) {
        // WARNING: inside this critical section, call delegate Map methods DIRECTLY because wrapper methods in this
        // class need a (non-exclusive) read lock which can't be acquired - an (exclusive) write lock already exists
        return LockUtils.withExclusiveLock(lock, () ->
                putInternalWithoutLock(key, value));
    }

    @Override
    public V remove(@Nullable Object key) {
        return LockUtils.withExclusiveLock(lock, () ->
                delegate.remove(key));
    }

    @Override
    public void putAll(Map<? extends Class<? extends K>, ? extends V> source) {
        // WARNING: inside this critical section, call delegate Map methods DIRECTLY because wrapper methods in this
        // class need a (non-exclusive) read lock which can't be acquired - an (exclusive) write lock already exists
        LockUtils.withExclusiveLock(lock, () -> {
            for (var sourceEntry : source.entrySet()) {
                putInternalWithoutLock(sourceEntry.getKey(), sourceEntry.getValue());
            }

            return null;
        });
    }

    @Override
    public void clear() {
        LockUtils.withExclusiveLock(lock, () -> {
            delegate.clear();
            return null;
        });
    }

    @Override
    public Set<Class<? extends K>> keySet() {
        return LockUtils.withOptimisticReadOrRetry(lock, delegate::keySet);
    }

    @Override
    public Collection<V> values() {
        return LockUtils.withOptimisticReadOrRetry(lock, delegate::values);
    }

    @Override
    public Set<Entry<Class<? extends K>, V>> entrySet() {
        return LockUtils.withOptimisticReadOrRetry(lock, delegate::entrySet);
    }

    @Override
    public String toString() {
        return LockUtils.withOptimisticReadOrRetry(lock, delegate::toString);
    }

    @Override
    public V getOrDefault(@Nullable Object key, @Nullable V defaultValue) {
        return LockUtils.withOptimisticReadOrRetry(lock, () -> delegate.getOrDefault(key, defaultValue));
    }

    /**
     * Iterates over a consistent snapshot of the map entries. The snapshot is copied under the lock so the action never
     * traverses a half-mutated delegate, is never re-applied on optimistic-read retry and may safely call back into
     * this map.
     *
     * @param action action to apply to each entry
     */
    @Override
    public void forEach(BiConsumer<? super Class<? extends K>, ? super V> action) {
        checkNotNull(action, "action");

        var snapshot = LockUtils.withOptimisticReadOrRetry(lock, () -> new LinkedHashMap<>(delegate));
        snapshot.forEach(action);
    }

    /**
     * Replaces each value with the function result. The function runs under the exclusive lock, so it must be short
     * and must not call back into this map (locks are not reentrant).
     *
     * @param function function computing a new value from the key and the current value
     */
    @Override
    public void replaceAll(BiFunction<? super Class<? extends K>, ? super V, ? extends V> function) {
        checkNotNull(function, "function");

        // value-only update - key order (this map's core invariant) is unaffected, so delegate.replaceAll is safe
        LockUtils.withExclusiveLock(lock, () -> {
            delegate.replaceAll(function);
            return null;
        });
    }

    @Nullable
    @Override
    public V putIfAbsent(@Nullable Class<? extends K> key, @Nullable V value) {
        // WARNING: inside this critical section, call delegate Map methods DIRECTLY because wrapper methods in this
        // class need a (non-exclusive) read lock which can't be acquired - an (exclusive) write lock already exists
        return LockUtils.withExclusiveLock(lock, () -> {
            V existingValue = delegate.get(key);

            // putInternalWithoutLock (not delegate.putIfAbsent) because inserting a new key must maintain key order
            return (existingValue == null) ? putInternalWithoutLock(key, value) : existingValue;
        });
    }

    /**
     * Computes the value only when the key is missing. The mapping function runs OUTSIDE the lock (user code under
     * the lock could deadlock), so concurrent callers may compute the same value twice - last write wins.
     *
     * @param key            key to look up
     * @param mappingFunction function computing the value for a missing key
     * @return current (existing or computed) value, {@code null} when the mapping function returned {@code null}
     */
    @Nullable
    @Override
    public V computeIfAbsent(@Nullable Class<? extends K> key,
            Function<? super Class<? extends K>, ? extends V> mappingFunction) {
        checkNotNull(mappingFunction, "mappingFunction");

        V existingValue = get(key);
        if (existingValue != null) {
            return existingValue;
        }

        V computedValue = mappingFunction.apply(key);
        if (computedValue == null) {
            return null;
        }

        // putIfAbsent returns the winner's value when a concurrent call got there first
        V winnerValue = putIfAbsent(key, computedValue);
        return (winnerValue == null) ? computedValue : winnerValue;
    }

    /**
     * Computes a new value for the key. The remapping function runs under the exclusive lock, so it must be short and
     * must not call back into this map (locks are not reentrant).
     *
     * @param key               key to look up
     * @param remappingFunction function computing a new value from the key and the current value
     * @return new value, or {@code null} when nothing is mapped (a {@code null} result removes the mapping)
     */
    @Nullable
    @Override
    public V compute(@Nullable Class<? extends K> key,
            BiFunction<? super Class<? extends K>, ? super V, ? extends V> remappingFunction) {
        checkNotNull(remappingFunction, "remappingFunction");

        // WARNING: inside this critical section, call delegate Map methods DIRECTLY because wrapper methods in this
        // class need a (non-exclusive) read lock which can't be acquired - an (exclusive) write lock already exists
        return LockUtils.withExclusiveLock(lock, () -> {
            V newValue = remappingFunction.apply(key, delegate.get(key));

            if (newValue == null) {
                // Map contract: null result removes the mapping (removing a missing key is a no-op)
                delegate.remove(key);
            } else {
                putInternalWithoutLock(key, newValue);
            }

            return newValue;
        });
    }

    /**
     * Merges the given value with the current one. The remapping function runs under the exclusive lock, so it must
     * be short and must not call back into this map (locks are not reentrant).
     *
     * @param key               key to look up
     * @param value             value to merge when the key is missing (or mapped to {@code null})
     * @param remappingFunction function merging the current value with the given one
     * @return new value, or {@code null} when nothing is mapped (a {@code null} result removes the mapping)
     */
    @Nullable
    @Override
    public V merge(@Nullable Class<? extends K> key, @Nullable V value,
            BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
        checkNotNull(remappingFunction, "remappingFunction");

        // WARNING: inside this critical section, call delegate Map methods DIRECTLY because wrapper methods in this
        // class need a (non-exclusive) read lock which can't be acquired - an (exclusive) write lock already exists
        return LockUtils.withExclusiveLock(lock, () -> {
            V oldValue = delegate.get(key);
            V newValue = (oldValue == null) ? value : remappingFunction.apply(oldValue, value);

            if (newValue == null) {
                // Map contract: null result removes the mapping (removing a missing key is a no-op)
                delegate.remove(key);
            } else {
                putInternalWithoutLock(key, newValue);
            }

            return newValue;
        });
    }

    /**
     * Finds map entry corresponding to the closest superclass (or direct class) of {@code clazz} in the key set.
     *
     * @param clazz class to look up - it's <b>any class</b> intentionally, not only {@code K}
     * @return map entry (empty if the argument is {@code null})
     */
    public Optional<Entry<Class<? extends K>, V>> findEntryForClosestSuperclass(@Nullable Class<?> clazz) {
        if (clazz == null) {
            return Optional.empty();
        }

        // generic erasure makes this cast unchecked-only: it never fails at runtime, so unrelated classes are
        // filtered later, by isAssignableFrom during the entry scan
        @SuppressWarnings("unchecked")
        Class<? extends K> validClass = (Class<? extends K>) clazz;

        // WARNING: inside critical sections, call delegate Map's methods directly because locks are not reentrant
        return LockUtils.withOptimisticReadOrRetry(lock, () -> {
            Entry<Class<? extends K>, V> mapEntry = null;

            // exact hit short-circuits in O(1) even when the stored value is null (put accepts @Nullable V)
            if (delegate.containsKey(validClass)) {
                // unlike Map.entry(), SimpleImmutableEntry tolerates null values
                mapEntry = new AbstractMap.SimpleImmutableEntry<>(validClass, delegate.get(validClass));
            } else {
                for (var entry : delegate.entrySet()) {
                    if ((entry.getKey() != null) && entry.getKey().isAssignableFrom(clazz)) {
                        // delegate's own entry is a live view - copy it, so callers can't mutate the map lock-free
                        mapEntry = new AbstractMap.SimpleImmutableEntry<>(entry);
                        break;
                    }
                }
            }

            return Optional.ofNullable(mapEntry);
        });
    }

    /**
     * Called from {@link #put(Class, Object)} and {@link #putAll(Map)}.
     */
    @Nullable
    private V putInternalWithoutLock(@Nullable Class<? extends K> key, @Nullable V value) {
        // add to empty map or replace existing key's value
        if (delegate.isEmpty() || (key == null) || delegate.containsKey(key)) {
            return delegate.put(key, value);
        }

        // storage for temporarily removed (and then shifted rightwards) entries
        var entriesForRightShift = new LinkedHashMap<Class<? extends K>, V>();

        delegate.entrySet()
                .removeIf(mapEntry -> {
                    if ((mapEntry.getKey() != null) && mapEntry.getKey().isAssignableFrom(key)) {
                        entriesForRightShift.put(mapEntry.getKey(), mapEntry.getValue());
                        return true;
                    }

                    return false;
                });

        V oldValue = delegate.put(key, value);
        delegate.putAll(entriesForRightShift);
        return oldValue;
    }

}
