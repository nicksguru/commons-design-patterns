package guru.nicks.commons.designpattern.pipeline;

import jakarta.annotation.Nullable;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * Pipeline state, intermediate or final. Pipeline steps are executed sequentially, by definition, and each state is
 * created, mutated, and read by the single thread that runs {@link Pipeline#apply(Object)} - callers may inspect the
 * returned state only after the pipeline completes. The state is therefore thread-confined, plain non-volatile fields
 * are sufficient, and there's no need to care about thread safety.
 *
 * @param <I> pipeline input type
 * @param <O> pipeline output type
 * @see Pipeline#apply(Object)
 */
@Getter
@ToString
@Slf4j
public class PipelineState<I, O> {

    /**
     * Durations of the executed steps. Technically, the same step (as an object) can be queued multiple times in a
     * pipeline, therefore this is a list, not a map. Empty when timing is disabled (see {@link #isTimingEnabled()}).
     */
    private final List<StepDuration> stepDurations;

    /**
     * Whether per-step timing is recorded at all. Its only consumer is debug logging in {@link Pipeline#apply(Object)},
     * so it's captured once (from {@code log.isDebugEnabled()}) - when debugging is off, no timestamps are taken and no
     * durations recorded.
     */
    private final boolean timingEnabled;

    @Nullable
    private final I input;

    @Setter
    @Nullable
    private O output;

    /**
     * Running total of step durations in milliseconds, incremented as each step duration is recorded (thread-confined -
     * see class Javadoc). Stays 0 when timing is disabled.
     */
    @Getter
    private long millisElapsed;

    /**
     * Constructor.
     *
     * @param input         pipeline input
     * @param stepCount     number of steps in the pipeline
     * @param timingEnabled whether step timing should be recorded; when disabled, {@link #getMillisElapsed()} returns 0
     *                      and {@link #getStepDurations()} is empty
     */
    public PipelineState(@Nullable I input, int stepCount, boolean timingEnabled) {
        this.input = input;
        this.timingEnabled = timingEnabled;
        stepDurations = new ArrayList<>(stepCount);
    }

    /**
     * Convenience method - returns {@link #getStepDurations()} size.
     */
    public int getExecutedStepCount() {
        return stepDurations.size();
    }

    /**
     * Runs the given step and, when timing is enabled, records its {@link StepDuration} and adds it to the
     * {@link #getMillisElapsed()} running total.
     *
     * @param step       step to run
     * @param stepRunner step runner
     */
    public <S extends PipelineStep<I, O>> void runStep(S step, PipelineStepRunner<I, O, S> stepRunner) {
        // step name is consumed only by trace logging and recorded durations - don't pay for toString() otherwise
        String stepName = log.isTraceEnabled() || timingEnabled
                ? step.toString()
                : null;

        if (log.isTraceEnabled()) {
            log.trace("Running pipeline step '{}'", stepName);
        }

        // Timing is consumed only by debug logging - don't pay for it when debugging is off.
        // Not Duration, to optimize speed. Not nanos, as such precision is not needed.
        long startMillis = timingEnabled
                ? System.currentTimeMillis()
                : 0L;

        output = stepRunner.apply(input, output, step);

        if (timingEnabled) {
            long durationMillis = System.currentTimeMillis() - startMillis;
            stepDurations.add(new StepDuration(stepName, durationMillis));
            millisElapsed += durationMillis;
        }
    }

    /**
     * Duration of a single executed pipeline step.
     *
     * @param stepName       step name, as printed by {@link PipelineStep#toString()}
     * @param durationMillis step duration in milliseconds
     */
    public record StepDuration(

            String stepName,
            long durationMillis) {
    }

}
