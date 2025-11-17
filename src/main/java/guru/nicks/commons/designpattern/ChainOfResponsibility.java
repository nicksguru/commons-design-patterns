package guru.nicks.commons.designpattern;

import guru.nicks.commons.designpattern.pipeline.Pipeline;
import guru.nicks.commons.designpattern.pipeline.PipelineState;
import guru.nicks.commons.designpattern.pipeline.PipelineStep;

import jakarta.annotation.Nullable;

import java.util.Collection;

/**
 * Chain of Responsibility pattern implementation. Technically this is a {@link Pipeline} having no accumulator:
 * {@code null} is passed to all steps. The chain stops as soon as its step returns non-null.
 *
 * @param <I> argument type (input)
 * @param <O> result type (output)
 * @see #shouldStop(PipelineState, PipelineStep, PipelineStep)
 */
public class ChainOfResponsibility<I, O, S extends PipelineStep<I, O>>
        extends Pipeline<I, O, S> {

    /**
     * @see Pipeline#Pipeline(Collection)
     */
    public ChainOfResponsibility(Collection<? extends S> steps) {
        super(steps);
    }

    /**
     * @return {@code true} if {@link PipelineState#getOutput()} is not {@code null}
     */
    @Override
    protected boolean shouldStop(PipelineState<I, O> pipelineState, @Nullable S previousStep, S nextStep) {
        return pipelineState.getOutput() != null;
    }

}
