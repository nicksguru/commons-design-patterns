package guru.nicks.commons.designpattern.pipeline;

import guru.nicks.commons.designpattern.ChainOfResponsibility;

import jakarta.annotation.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

/**
 * This pipeline stops after the first validation failure. After that, {@link PipelineState#getOutput()} contains the
 * (non-null) boolean validation result.
 */
public class ValidationPipeline<I> extends ChainOfResponsibility<I, Boolean, PipelineStep<I, Boolean>> {

    private ValidationPipeline(Collection<? extends PipelineStep<I, Boolean>> steps) {
        super(steps);
    }

    public static <T> ValidationPipeline<T> of(Collection<Predicate<T>> validationPredicates) {
        List<? extends PipelineStep<T, Boolean>> steps = validationPredicates.stream()
                .map(predicate -> new PipelineStep<T, Boolean>() {
                    /**
                     * @return {@code null} if the input is valid, so validation is passed on to the next step;
                     *          returning {@code false} (or any other non-null value) stops the pipeline
                     */
                    @Nullable
                    @Override
                    public Boolean apply(T input, Boolean alwaysNullAccumulator) {
                        // return null to pass on to the next step
                        return predicate.test(input)
                                ? null
                                : Boolean.FALSE;
                    }
                })
                .toList();
        return new ValidationPipeline<>(steps);
    }

    @Override
    public PipelineState<I, Boolean> apply(@Nullable I input) {
        var pipelineState = super.apply(input);

        // null actually meant true here
        if (pipelineState.getOutput() == null) {
            pipelineState.setOutput(Boolean.TRUE);
        }

        return pipelineState;
    }

}
