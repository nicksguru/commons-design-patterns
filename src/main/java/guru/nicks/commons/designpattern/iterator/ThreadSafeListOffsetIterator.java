package guru.nicks.commons.designpattern.iterator;

import guru.nicks.commons.utils.LockUtils;

import org.apache.commons.lang3.ArrayUtils;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.locks.StampedLock;

import static guru.nicks.commons.validation.dsl.ValiDsl.checkNotNull;

/**
 * Iterates {@link List} starting with the given offset, reaches list end and then goes from list start to the starting
 * position: {@code startIndex -> listSize-1 -> 0 -> startIndex-1}. If {@code startIndex} equals 0, this becomes an
 * ordinary iteration.
 * <p>
 * Thread safety is guaranteed at the cost of lock-related speed penalty.
 * <p>
 * State sequence is:
 * <ul>
 *     <li>if list is empty: {@link State#NOT_STARTED} -&gt; {@link State#FINISHED}</li>
 *     <li>if start index is 0: {@link State#NOT_STARTED} -&gt; {@link State#AT_START_INDEX} -&gt;
 *          {@link State#MOVED_FORWARD} -&gt; &hellip; -&gt; {@link State#FINISHED}
 *     </li>
 *     <li>if start index is not 0: {@link State#NOT_STARTED} -&gt; {@link State#AT_START_INDEX} -&gt;
 *          {@link State#MOVED_FORWARD} -&gt; &hellip; &gt; {@link State#ROLLED_OVER_LIST_END} -&gt;
 *          {@link State#MOVED_FORWARD} -&gt; &hellip; -&gt; {@link State#FINISHED}
 *     </li>
 * </ul>
 */
public class ThreadSafeListOffsetIterator<T> implements Iterator<T> {

    public static final int FINISHED_INDEX = ArrayUtils.INDEX_NOT_FOUND;

    private final StampedLock lock = new StampedLock();
    private final List<? extends T> items;

    /**
     * Not {@code final} because can be updated by {@link #fixStartIndex()}.
     */
    private int startIndex;

    private State state = State.NOT_STARTED;
    private int currentIndex = FINISHED_INDEX;

    /**
     * Transition suggested by {@link #suggestTransition()} - written there, read by {@link #hasNext()} and
     * {@link #next()}. Both fields are only touched under the exclusive lock, so no volatility is needed.
     */
    private State suggestedState;
    private int suggestedIndex;

    /**
     * Constructor.
     *
     * @param items      items to iterate - {@link List} because {@link List#get(int)} is needed
     * @param startIndex if not within {@code items} boundaries, becomes 0
     */
    public ThreadSafeListOffsetIterator(List<? extends T> items, int startIndex) {
        this.items = checkNotNull(items, "items");
        this.startIndex = startIndex;
        fixStartIndex();
    }

    @Override
    public boolean hasNext() {
        return LockUtils.withExclusiveLock(lock, () -> {
            suggestTransition();

            // next() won't be called if hasNext() returns false, so it's up to hasNext() to set final state
            if (suggestedState == State.FINISHED) {
                state = State.FINISHED;
            }

            return suggestedState != State.FINISHED;
        });
    }

    @Override
    public T next() {
        return LockUtils.withExclusiveLock(lock, () -> {
            suggestTransition();
            state = suggestedState;
            currentIndex = suggestedIndex;

            if (state == State.FINISHED) {
                throw new NoSuchElementException();
            }

            // the StampedLock guards iterator state only, not the external list - it may shrink between the size
            // check in suggestTransition() and this read, so never leak IndexOutOfBoundsException
            if (currentIndex >= items.size()) {
                state = State.FINISHED;
                currentIndex = FINISHED_INDEX;
                throw new NoSuchElementException("List shrank during iteration");
            }

            return items.get(currentIndex);
        });
    }

    /**
     * Returns current iterator state. Reads under the lock so external callers can't observe stale values.
     *
     * @return current state
     */
    public State getState() {
        return LockUtils.withOptimisticReadOrRetry(lock, () -> state);
    }

    /**
     * Returns index of the item returned by the last successful {@link #next()} call
     * ({@value #FINISHED_INDEX} before the first call and after iteration end). Reads under the lock so external
     * callers can't observe stale values.
     *
     * @return current index
     */
    public int getCurrentIndex() {
        return LockUtils.withOptimisticReadOrRetry(lock, () -> currentIndex);
    }

    /**
     * Calculates next state and index based on current values, storing them in {@link #suggestedState} and
     * {@link #suggestedIndex} ({@link State#FINISHED}/{@value #FINISHED_INDEX} if there's nowhere to go). Also
     * updates {@link #startIndex} to ensure it's within the list boundaries (the list may have been modified during
     * iteration).
     */
    private void suggestTransition() {
        // both fields are only touched under the exclusive lock - no volatility needed
        suggestedIndex = FINISHED_INDEX;

        if (items.isEmpty()) {
            suggestedState = State.FINISHED;
            return;
        }

        // safeguard in case list size changes during iteration
        fixStartIndex();
        State newState = state.suggestTransition();

        switch (newState) {
            case FINISHED, NOT_STARTED:
                suggestedIndex = FINISHED_INDEX;
                break;

            case AT_START_INDEX:
                suggestedIndex = startIndex;
                break;

            case ROLLED_OVER_LIST_END:
                suggestedIndex = 0;
                break;

            case MOVED_FORWARD:
                suggestedIndex = currentIndex + 1;

                // end of list reached ('>=', 'not '==', because the list may shrink) -
                // jump to #0 if startIndex isn't 0 (otherwise, the whole list has been processed)
                if (suggestedIndex >= items.size()) {
                    if (startIndex == 0) {
                        newState = State.FINISHED;
                        suggestedIndex = FINISHED_INDEX;
                    } else {
                        newState = State.ROLLED_OVER_LIST_END;
                        suggestedIndex = 0;
                    }
                }
                // went from #0 to startIndex (after rollover)
                else if (suggestedIndex == startIndex) {
                    newState = State.FINISHED;
                    suggestedIndex = FINISHED_INDEX;
                }

                break;

            default:
                throw new IllegalStateException("Unknown transition state");
        }

        suggestedState = newState;
    }

    /**
     * Updates {@code startIndex} to ensure it's within the list boundaries. If it's not, sets it to 0.
     */
    private void fixStartIndex() {
        if ((startIndex < 0) || (startIndex >= items.size())) {
            startIndex = 0;
        }
    }

    public enum State {

        NOT_STARTED {
            @Override
            public State suggestTransition() {
                return AT_START_INDEX;
            }
        },

        AT_START_INDEX {
            @Override
            public State suggestTransition() {
                return MOVED_FORWARD;
            }
        },

        MOVED_FORWARD {
            @Override
            public State suggestTransition() {
                return MOVED_FORWARD;
            }
        },

        ROLLED_OVER_LIST_END {
            @Override
            public State suggestTransition() {
                return MOVED_FORWARD;
            }
        },

        FINISHED {
            @Override
            public State suggestTransition() {
                return FINISHED;
            }
        };

        /**
         * Suggests next state with respect to current state. Doesn't take current list index or number of items into
         * consideration, therefore the result must be validated and adjusted accordingly.
         *
         * @return next state
         */
        public abstract State suggestTransition();

    }

}
