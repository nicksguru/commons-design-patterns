package guru.nicks.designpattern.pipeline;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;

/**
 * Pipeline step. It's desirable, for logging purposes, that step class names be self-descriptive (possibly augmented
 * with {@link PipelineStepFeature @PipelineStepFeature}). The only reason why steps are not interfaces is
 * {@link #toString()}. Declaring a dedicated base step class for each pipeline helps avoid repeating generic types in
 * step definitions.
 * <p>
 * The 1st argument to {@link #apply(Object, Object)} is always the same - it holds the original pipeline input.
 * <p>
 * The 2nd argument - intermediate pipeline output - is initially {@code null}. The 1st step receives {@code null} and
 * returns it or a new instance of {@code O} (remember to adhere to immutability - <b>create a new object instead of
 * changing the object state</b>). The value returned by the 1st step is passed to the 2nd step, etc., and eventually
 * becomes the final pipeline output.
 *
 * @param <I> step input type
 * @param <O> step output type
 * @see Pipeline#apply(Object)
 */
public abstract class PipelineStep<I, O> implements BiFunction<I, O, O> {

    /**
     * Value cached by {@link #toString()}.
     */
    private final AtomicReference<String> toStringResult = new AtomicReference<>();

    /**
     * Returns step name.
     *
     * @return default implementation returns {@link Class#getSimpleName()}
     */
    public String getName() {
        return getClass().getSimpleName();
    }

    /**
     * Pretty prints step name and its enable features (i.e. values returned by methods annotated with
     * {@link PipelineStepFeature @PipelineStepFeature} if they're not {@code null} and not {@code false}).
     * <p>
     * To speed things up, the <b>value is constructed only once and then cached</b>. If a certain step needs dynamic
     * behavior in this regard, it must <b>overload this method</b>.
     *
     * @return string
     */
    @Override
    public String toString() {
        // it's OK if another thread sets the value to non-null while this branch generates a new one
        if (toStringResult.get() == null) {
            var builder = new StringBuilder();

            builder.append(Optional.ofNullable(getName())
                    .filter(StringUtils::isNotBlank)
                    .orElse("noname"));

            Map<String, Object> featuresValues = getFeaturesValues();
            // append '{key1=val1, key2=val2, ...}' but not '{}'
            if (!MapUtils.isEmpty(featuresValues)) {
                builder.append(featuresValues);
            }

            String result = builder.toString();
            toStringResult.compareAndSet(null, result);
        }

        return toStringResult.get();
    }

    /**
     * Collects feature names and values (calls getter methods). Every call may yield different a result.
     *
     * @return features (unmodifiable): keys are feature names, values are feature values (never null/false)
     */
    public Map<String, Object> getFeaturesValues() {
        var features = new TreeMap<String, Object>();

        PipelineStepFeatureImpl.findFeatures(getClass()).forEach(feature ->
                feature.readFeatureValue(this, features));

        return Collections.unmodifiableMap(features);
    }

}
