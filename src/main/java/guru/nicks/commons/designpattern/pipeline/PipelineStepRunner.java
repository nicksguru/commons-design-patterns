package guru.nicks.commons.designpattern.pipeline;

import org.apache.commons.lang3.function.TriFunction;

/**
 * Runs step and does something before/after/instead, such as caching.
 *
 * @param <I> pipeline input type
 * @param <O> pipeline output type
 * @param <S> pipeline step type
 */
@FunctionalInterface
public interface PipelineStepRunner<I, O, S extends PipelineStep<I, O>> extends TriFunction<I, O, S, O> {
}
