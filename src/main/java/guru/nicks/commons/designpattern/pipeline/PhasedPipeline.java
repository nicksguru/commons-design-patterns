package guru.nicks.commons.designpattern.pipeline;

import jakarta.annotation.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Pipeline whose {@link #iterator()} returns steps sorted by the index of each step's {@link Step#getPhase()} in
 * {@link #getPhases()}. Steps are immutable after construction, so the sorted order is computed once, in the
 * constructor.
 *
 * @param <I> input type
 * @param <O> output type
 * @param <P> step phase type
 */
public abstract class PhasedPipeline<I, O, P, S extends PhasedPipeline.Step<I, O, P>> extends Pipeline<I, O, S> {

    /**
     * Steps in execution order: sorted by phase position in {@link #getPhases()}; steps whose phase is missing from
     * that array go after all the others, in original declaration order. Precomputed once because steps are immutable
     * after construction.
     */
    private final List<S> sortedSteps;

    /**
     * @see Pipeline#Pipeline(Collection)
     */
    protected PhasedPipeline(Collection<? extends S> steps) {
        super(steps);
        this.sortedSteps = sortStepsByPhase(getSteps(), getPhases());
    }

    /**
     * Sorts steps by the position of their phase in the phases array. Steps whose phase is missing from that array go
     * after all the others, in original order (the total number of phases is added to their original index). Steps
     * sharing a phase keep their original relative order (stable sort).
     *
     * @param steps  pipeline steps in their original order
     * @param phases pipeline phases
     * @param <I>    input type
     * @param <O>    output type
     * @param <P>    step phase type
     * @param <S>    pipeline step type
     * @return immutable steps list sorted by phase
     */
    private static <I, O, P, S extends Step<I, O, P>> List<S> sortStepsByPhase(List<S> steps, P[] phases) {
        // phase → its first position in the phases array (plain HashMap because phases may contain null, and
        // putIfAbsent keeps the first occurrence)
        Map<P, Integer> phaseIndex = HashMap.newHashMap(phases.length * 2);
        for (int i = 0; i < phases.length; i++) {
            phaseIndex.putIfAbsent(phases[i], i);
        }

        // precomputed sort keys: phase position, or (phase count + original index) for steps with unlisted phases
        int[] keys = new int[steps.size()];
        for (int i = 0; i < keys.length; i++) {
            keys[i] = phaseIndex.getOrDefault(steps.get(i).getPhase(), phases.length + i);
        }

        // sort original indexes (not steps themselves) so each step can access its key; TimSort keeps equal keys
        // (i.e. same-phase steps) in original order
        Integer[] order = new Integer[steps.size()];
        for (int i = 0; i < order.length; i++) {
            order[i] = i;
        }
        Arrays.sort(order, Comparator.comparingInt(i -> keys[i]));

        return Arrays.stream(order)
                .map(steps::get)
                .toList();
    }

    /**
     * Returns {@link #sortedSteps} iterator - no sorting or other allocation happens on each call (pipelines are
     * long-lived singletons, so {@link Pipeline#apply(Object)} and {@link #toString()} must stay cheap).
     *
     * @return step iterator
     */
    @Override
    public Iterator<S> iterator() {
        return sortedSteps.iterator();
    }

    /**
     * Returns all pipeline phases - typically {@code P.values()}. If a certain step's phase is missing from this list,
     * the step goes to list end during sorting (to be precise, the total number of phases is added to its index in
     * {@link #getSteps()}).
     *
     * @return pipeline phases
     */
    protected abstract P[] getPhases();

    /**
     * Phased pipeline step.
     *
     * @param <I> input type
     * @param <O> output type
     * @param <P> step phase type
     */
    public abstract static class Step<I, O, P> extends PipelineStep<I, O> {

        /**
         * Returns phase this step belongs to.
         *
         * @return step phase ({@code null} it phase - i.e. step position in the pipeline - doesn't matter)
         */
        @PipelineStepFeature
        @Nullable
        public abstract P getPhase();

    }

}
