package guru.nicks.designpattern.pipeline;

import jakarta.annotation.Nullable;
import org.apache.commons.lang3.ArrayUtils;

import java.util.Collection;
import java.util.Iterator;

import static org.apache.commons.lang3.ArrayUtils.INDEX_NOT_FOUND;

/**
 * Pipeline whose {@link #iterator()} sorts steps by the index of each step's {@link Step#getPhase()} in
 * {@link #getPhases()}.
 *
 * @param <I> input type
 * @param <O> output type
 * @param <P> step phase type
 */
public abstract class PhasedPipeline<I, O, P, S extends PhasedPipeline.Step<I, O, P>> extends Pipeline<I, O, S> {

    /**
     * @see Pipeline#Pipeline(Collection)
     */
    protected PhasedPipeline(Collection<? extends S> steps) {
        super(steps);
    }

    @Override
    public Iterator<S> iterator() {
        return getSteps()
                .stream()
                .sorted((S step1, S step2) -> {
                    int index1 = ArrayUtils.indexOf(getPhases(), step1.getPhase());
                    // Add number of phases to make phased steps go before non-phased ones. For this reason, this
                    // is an anonymous Lambda; a method would need to access 'this.steps'.
                    if (index1 == INDEX_NOT_FOUND) {
                        index1 = getPhases().length + getSteps().indexOf(step1);
                    }

                    int index2 = ArrayUtils.indexOf(getPhases(), step2.getPhase());
                    if (index2 == INDEX_NOT_FOUND) {
                        index2 = getPhases().length + getSteps().indexOf(step2);
                    }

                    return Integer.compare(index1, index2);
                }).iterator();
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
