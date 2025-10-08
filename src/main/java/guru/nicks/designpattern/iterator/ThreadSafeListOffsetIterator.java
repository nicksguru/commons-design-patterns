package guru.nicks.designpattern.iterator;

import guru.nicks.utils.LockUtils;

import lombok.Getter;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.locks.StampedLock;

import static guru.nicks.validation.dsl.ValiDsl.checkNotNull;

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

    @Getter
    private State state = State.NOT_STARTED;
    @Getter
    private int currentIndex = FINISHED_INDEX;

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
        return LockUtils.returnWithExclusiveLock(lock, () -> {
            Pair<State, Integer> transition = suggestTransition();

            // next() won't be called if hasNext() returns false, so it's up to hasNext() to set final state
            if (transition.getLeft() == State.FINISHED) {
                state = State.FINISHED;
            }

            return transition.getLeft() != State.FINISHED;
        });
    }

    @Override
    public T next() {
        return LockUtils.returnWithExclusiveLock(lock, () -> {
            Pair<State, Integer> transition = suggestTransition();
            state = transition.getLeft();
            currentIndex = transition.getRight();

            if (transition.getLeft() == State.FINISHED) {
                throw new NoSuchElementException();
            }

            return items.get(currentIndex);
        });
    }

    /**
     * Calculates next state and index based on current values. Also updates {@link #startIndex} to ensure it's within
     * the list boundaries (the list may have been modified during iteration).
     *
     * @return next state and index ({@link State#FINISHED}/{@value  #FINISHED_INDEX} if there's nowhere to go)
     */
    private Pair<State, Integer> suggestTransition() {
        if (items.isEmpty()) {
            return Pair.of(State.FINISHED, FINISHED_INDEX);
        }

        // safeguard in case list size changes during iteration
        fixStartIndex();
        State newState = state.suggestTransition();
        // not 'int' - null means calculation was overlooked
        @SuppressWarnings("WrapperTypeMayBePrimitive")
        Integer newIndex;

        switch (newState) {
            case FINISHED, NOT_STARTED:
                newIndex = FINISHED_INDEX;
                break;

            case AT_START_INDEX:
                newIndex = startIndex;
                break;

            case ROLLED_OVER_LIST_END:
                newIndex = 0;
                break;

            case MOVED_FORWARD:
                newIndex = currentIndex + 1;

                // end of list reached - jump to #0 if startIndex isn't 0 (otherwise, the whole list has been processed)
                if (newIndex == items.size()) {
                    if (startIndex == 0) {
                        newState = State.FINISHED;
                        newIndex = FINISHED_INDEX;
                    } else {
                        newState = State.ROLLED_OVER_LIST_END;
                        newIndex = 0;
                    }
                }
                // went from #0 to startIndex (after rollover)
                else if (newIndex == startIndex) {
                    newState = State.FINISHED;
                    newIndex = FINISHED_INDEX;
                }

                break;

            default:
                throw new IllegalStateException("Unknown transition state");
        }

        return Pair.of(newState, newIndex);
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
