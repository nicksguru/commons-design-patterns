package guru.nicks.commons.designpattern.iterator;

import guru.nicks.commons.utils.LockUtils;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.locks.StampedLock;

import static guru.nicks.commons.validation.dsl.ValiDsl.checkNotNull;

/**
 * Wrapper over {@link Iterable} to cycle through its items infinitely. Namely, when the original iterator becomes empty
 * (as per {@link Iterator#hasNext()}), {@link Iterable#iterator()} is called again.
 * <p>
 * Thread safety is guaranteed at the cost of lock-related speed penalty.
 */
public class ThreadSafeInfiniteIterator<T> implements Iterator<T> {

    private final StampedLock lock = new StampedLock();

    private final Iterable<T> source;
    private boolean hasItems;

    private Iterator<T> delegate;

    /**
     * Constructor.
     *
     * @param source the original {@link Iterable}
     * @throws NullPointerException {@code source} is {@code null}
     */
    public ThreadSafeInfiniteIterator(Iterable<T> source) {
        this.source = checkNotNull(source, "source");
        resetState(source);
    }

    /**
     * Always returns {@code true} unless the underlying iterator reported 'no items' initially (such as for an empty
     * list), in which case always returns {@code false}.
     */
    @Override
    public boolean hasNext() {
        return hasItems;
    }

    @Override
    public T next() {
        // do the same as hasNext() but faster
        if (!hasItems) {
            throw new NoSuchElementException();
        }

        return LockUtils.returnWithExclusiveLock(lock, () -> {
            if (!delegate.hasNext()) {
                resetState(source);
            }

            return delegate.next();
        });
    }

    private void resetState(Iterable<T> parent) {
        delegate = parent.iterator();
        hasItems = delegate.hasNext();
    }

}
