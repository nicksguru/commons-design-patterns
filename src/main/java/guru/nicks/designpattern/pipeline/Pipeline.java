package guru.nicks.designpattern.pipeline;

import jakarta.annotation.Nullable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.util.Streamable;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Pipeline which can be stopped after any step by {@link #shouldStop(PipelineState, PipelineStep, PipelineStep)}. Steps
 * are retrieved with {@link #iterator()} (which depends on the concrete subclass: plain iteration / with priority /
 * ...) or with {@link Streamable} methods (which is {@link Iterable} too, so {@code for (var step : pipeline)} works).
 * <p>
 * Declaring a dedicated base step class for each pipeline helps avoid repeating generic types in step definitions.
 *
 * @param <I> pipeline input type
 * @param <O> pipeline output type
 * @param <S> pipeline step type
 */
@Slf4j
public class Pipeline<I, O, S extends PipelineStep<I, O>>
        implements Function<I, PipelineState<I, O>>, Streamable<S> {

    /**
     * Pipeline steps (immutable list). Order doesn't matter because the steps are always run via {@link #iterator()}.
     * This collection is not a raw {@link Iterable} because item indexes (which only {@link List} has) and/or item
     * count (which {@link Collection} has) may be needed for the iteration logic.
     * <p>
     * The only reason why this field has a getter is to make it available to subclasses' {@link #iterator()}.
     */
    @Getter(value = AccessLevel.PROTECTED)
    private final List<S> steps;

    /**
     * @see #ensureStepsStringified()
     */
    private final AtomicBoolean stepsStringified = new AtomicBoolean();

    /**
     * Step runner, calls {@link PipelineStep#apply(Object, Object)} by default.
     */
    @Getter
    @Setter
    @NonNull // Lombok creates runtime nullness check for this own annotation only
    private PipelineStepRunner<I, O, S> stepRunner = (I pipelineInput, O previousStepResult, S step) ->
            step.apply(pipelineInput, previousStepResult);

    /**
     * Constructor. Copies steps to ensure they have indexes ({@link Collection} doesn't have them) which may be needed
     * in {@link #iterator()}.
     *
     * @param steps pipeline steps; order doesn't matter because they are executed via {@link #iterator()}
     */
    protected Pipeline(Collection<? extends S> steps) {
        this.steps = List.copyOf(steps);
    }

    /**
     * Returns pipeline name.
     *
     * @return default implementation returns {@link Class#getSimpleName()}
     */
    public String getName() {
        return getClass().getSimpleName();
    }

    /**
     * Creates an iterator (never use a shared object, otherwise thread safety will be devastated!) which defines
     * {@link #getSteps()} execution order. Default implementation simply calls {@link List#iterator()}. Subclasses may
     * override this method to change the order each time the pipeline is run - for example, randomize the steps. If the
     * order is constant, it's usually simpler to pass the steps in a pre-sorted form to the constructor.
     *
     * @return step iterator
     */
    @Override
    public Iterator<S> iterator() {
        return steps.iterator();
    }

    /**
     * Runs the pipeline until either:
     * <ul>
     *  <li>all the steps have been executed (using {@link #iterator()}, not {@link #getSteps()}!), or</li>
     *  <li>a step throws an exception, or</li>
     *  <li>{@link #shouldStop(PipelineState, PipelineStep, PipelineStep)} returns {@code true}</li>
     * </ul>
     *
     * @param input pipeline input
     * @return pipeline state
     */
    @Override
    public PipelineState<I, O> apply(@Nullable I input) {
        ensureStepsStringified();
        var state = initPipelineState(input);
        S previousStep = null;

        // iterator() is called implicitly
        for (S step : this) {
            if (shouldStop(state, previousStep, step)) {
                break;
            }

            state.runAndRegisterStep(step, stepRunner);
            previousStep = step;
        }

        if (log.isDebugEnabled()) {
            log.debug("Pipeline [{}] completed in {}ms: {}", getName(), state.getMillisElapsed(),
                    state.getStepDurations()
                            .stream()
                            .map(stepInfo -> String.format("%s/%dms", stepInfo.getLeft(), stepInfo.getRight()))
                            .collect(Collectors.joining(" -> ")));
        }

        return state;
    }

    /**
     * Pretty prints pipeline {@link #getName() name} and steps using {@link PipelineStep#toString()}. Invokes
     * {@link #iterator()}.
     *
     * @return string
     */
    @Override
    public String toString() {
        return stream()
                .map(PipelineStep::toString)
                .collect(Collectors.joining(" -> ", getName() + "[", "]"));
    }

    /**
     * Calls {@link PipelineState#PipelineState(Object, int)}. Can be overridden by subclasses to do something else.
     *
     * @param input pipeline input
     * @return pipeline state
     */
    protected PipelineState<I, O> initPipelineState(@Nullable I input) {
        return new PipelineState<>(input, steps.size());
    }

    /**
     * Commands pipeline to stop. Called before each step (including the first one), so
     * {@link PipelineState#getOutput()} can be {@code null}.
     *
     * @param pipelineState current (intermediate) pipeline state
     * @param previousStep  previous step, can be {@code null}
     * @param nextStep      step to be invoked, never {@code null}
     * @return default implementation returns {@code false}, which means all steps are executed
     */
    protected boolean shouldStop(PipelineState<I, O> pipelineState, @Nullable S previousStep, S nextStep) {
        return false;
    }

    /**
     * If {@link #stepsStringified} holds {@code false}, calls {@link PipelineStep#toString()} on each step to collect
     * their {@link PipelineStepFeature} preemptively, so there's no time penalty during pipeline execution.
     */
    private void ensureStepsStringified() {
        if (!stepsStringified.get()) {
            steps.forEach(PipelineStep::toString);
            stepsStringified.set(true);
        }
    }

}
