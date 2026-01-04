package guru.nicks.commons.designpattern;

import guru.nicks.commons.utils.LockUtils;

import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.locks.StampedLock;

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
 *  <li>{@link #put(Class, Object)}</li>
 *  <li>{@link #putAll(Map)}</li>
 *  <li>{@link #remove(Object)}</li>
 *  <li>{@link #clear()}</li>
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
@Slf4j
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

        Class<? extends K> validClass;
        // if argument class doesn't inherit from K, there's no superclass in the map
        try {
            validClass = (Class<? extends K>) clazz;
        } catch (ClassCastException e) {
            return Optional.empty();
        }

        // WARNING: inside critical sections, call delegate Map's methods directly because locks are not reentrant
        return LockUtils.withOptimisticReadOrRetry(lock, () -> {
            Entry<Class<? extends K>, V> mapEntry;

            // try fast direct lookup first
            V directValue = delegate.get(validClass);
            // can't pass null value to Map.entry() - it calls checkNotNull() internally
            if (directValue != null) {
                mapEntry = Map.entry(validClass, directValue);
            } else {
                mapEntry = delegate.entrySet()
                        .stream()
                        .filter(entry -> (entry.getKey() != null) && entry.getKey().isAssignableFrom(clazz))
                        .findFirst()
                        .orElse(null);
            }

            if ((mapEntry != null) && log.isTraceEnabled()) {
                log.trace("Closest superclass of [{}]: [{}]", clazz.getName(), mapEntry.getKey().getName());
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
