package guru.nicks.designpattern.pipeline;

import jakarta.annotation.Nullable;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Pipeline state, intermediate or final. There's no need to care about thread safety - pipeline steps are executed
 * sequentially, by definition.
 *
 * @param <I> pipeline input type
 * @param <O> pipeline output type
 * @see Pipeline#apply(Object)
 */
@Getter
@ToString
@Slf4j
public class PipelineState<I, O> {

    public static final int MILLIS_ELAPSED_UNKNOWN = -1;
    private final AtomicLong millisElapsed = new AtomicLong(MILLIS_ELAPSED_UNKNOWN);

    /**
     * In each pair, {@link Pair#getLeft()} is the step name, and {@link Pair#getRight()} is the step duration in
     * milliseconds. Technically, the same step (as an object) can be queued multiple times in a pipeline, therefore
     * this is not a {@link Map}.
     */
    private final List<Pair<String, Long>> stepDurations;

    @Nullable
    private final I input;

    @Setter
    @Nullable
    private O output;

    /**
     * Constructor.
     *
     * @param input     pipeline input
     * @param stepCount number of steps in the pipeline
     */
    public PipelineState(@Nullable I input, int stepCount) {
        this.input = input;
        stepDurations = new ArrayList<>(stepCount);
    }

    /**
     * Returns the total time, in milliseconds, that the registered steps took. If a cached value is available (it's
     * reset by {@link #runAndRegisterStep(PipelineStep, PipelineStepRunner)}), it's returned, otherwise the sum of
     * {@link #getStepDurations()} is calculated and cached.
     *
     * @return total time the registered steps took
     */
    public long getMillisElapsed() {
        if (millisElapsed.get() == MILLIS_ELAPSED_UNKNOWN) {
            long newValue = stepDurations.stream()
                    .mapToLong(Pair::getRight)
                    .sum();

            // if another thread has already set the value, don't overwrite it
            millisElapsed.compareAndSet(MILLIS_ELAPSED_UNKNOWN, newValue);
        }

        return millisElapsed.get();
    }

    /**
     * Convenience method - returns {@link #getStepDurations()} size.
     */
    public int getExecutedStepCount() {
        return stepDurations.size();
    }

    /**
     * Runs the given step, adds an entry to {@link #getStepDurations()}, and resets {@link #getMillisElapsed()}.
     *
     * @param step       step to run
     * @param stepRunner step runner
     */
    public <S extends PipelineStep<I, O>> void runAndRegisterStep(S step, PipelineStepRunner<I, O, S> stepRunner) {
        String stepName = step.toString();

        if (log.isTraceEnabled()) {
            log.trace("Running pipeline step '{}'", stepName);
        }

        // Not Duration, to optimize speed. Not nanos, as such precision is not needed.
        long millis = System.currentTimeMillis();
        output = stepRunner.apply(input, output, step);
        millis = System.currentTimeMillis() - millis;

        stepDurations.add(Pair.of(stepName, millis));
        // reset, so getter will re-calculate it
        millisElapsed.set(MILLIS_ELAPSED_UNKNOWN);
    }

}
