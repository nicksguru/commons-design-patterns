package guru.nicks.commons.designpattern.pipeline;

import guru.nicks.commons.utils.ReflectionUtils;

import am.ik.yavi.meta.ConstraintArguments;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.annotation.Nullable;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.NonFinal;
import lombok.extern.jackson.Jacksonized;
import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.annotation.AnnotatedElementUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Function;

import static guru.nicks.commons.validation.dsl.ValiDsl.check;
import static guru.nicks.commons.validation.dsl.ValiDsl.checkNotNull;

/**
 * Pipeline step feature: name and value.
 *
 * @see PipelineStepFeature
 */
@Value
@NonFinal
@Jacksonized
@Builder(toBuilder = true)
@SuppressWarnings("rawtypes") // can't use generic types ('Step<?, ?>') in cache because of type erasure
public class PipelineStepFeatureImpl {

    /**
     * Step features (for all {@link PipelineStep} classes).
     */
    private static final Cache<Class<? extends PipelineStep>, List<PipelineStepFeatureImpl>> FEATURE_CACHE = Caffeine
            .newBuilder()
            .maximumSize(300)
            .build();

    String name;
    Function<PipelineStep<?, ?>, Object> valueGetter;

    /**
     * Finds, in the given class, all methods annotated with {@link PipelineStepFeature @PipelineStepFeature}. Such
     * methods may be private but must be callable from this class via {@link Method#setAccessible(boolean)}.
     *
     * @param clazz step class to explore
     * @return step features (cached)
     */
    public static List<PipelineStepFeatureImpl> findFeatures(Class<? extends PipelineStep> clazz) {
        // 'get' method may return null as per Caffeine specs, but never does in this particular case
        //noinspection DataFlowIssue
        return FEATURE_CACHE.get(clazz, PipelineStepFeatureImpl::findFeaturesWithoutCache);
    }

    /**
     * Called from {@link #findFeatures(Class)} on cache miss. Stores step features in {@link #FEATURE_CACHE}.
     *
     * @param clazz step class to explore
     * @return step features
     */
    private static List<PipelineStepFeatureImpl> findFeaturesWithoutCache(Class<? extends PipelineStep> clazz) {
        var features = new ArrayList<PipelineStepFeatureImpl>();

        for (Method featureValueGetter : ReflectionUtils.findAnnotatedMethods(clazz, PipelineStepFeature.class)) {
            String methodLabel = "Feature getter '" + featureValueGetter + "'";
            check(featureValueGetter.getModifiers(),
                    methodLabel + ": accessibility").constraint(Modifier::isPublic, "must be public");
            check(featureValueGetter.getParameterCount(),
                    methodLabel + ": number of parameters").eq(0);

            String featureName = Optional.ofNullable(
                            // in fact never null (see search condition above), but method signature implies null
                            AnnotatedElementUtils.findMergedAnnotation(featureValueGetter, PipelineStepFeature.class))
                    .map(PipelineStepFeature::name)
                    // use explicit feature name...
                    .filter(StringUtils::isNotBlank)
                    // ...or, as a fallback, derive feature name from method name
                    .orElseGet(() -> StringUtils.uncapitalize(
                            PipelineStepFeature.FEATURE_METHOD_NAME_PATTERN
                                    .matcher(featureValueGetter.getName())
                                    .replaceFirst("$1")));

            features.add(PipelineStepFeatureImpl.builder()
                    .name(featureName)
                    // invokes the annotated method on ANY step in the future
                    .valueGetter(step -> invokeFeatureValueGetter(step, featureValueGetter))
                    .build());
        }

        return features;
    }

    /**
     * Invokes getter method on the given step, replacing {@code true} with 'yes' and {@code false} with {@code null} -
     * because defaults don't need to be mentioned, for example 'cacheable=no' is superfluous.
     *
     * @param step   step to read the value from
     * @param getter getter method to invoke on the step
     * @return feature value; {@code null} must be skipped in {@link #readFeatureValue(PipelineStep, Map)}
     */
    @Nullable
    private static Object invokeFeatureValueGetter(PipelineStep<?, ?> step, Method getter) {
        Object featureValue;

        try {
            featureValue = getter.invoke(step);
        } catch (Exception e) {
            throw new IllegalArgumentException("Getter method '" + getter + "' failed: " + e.getMessage(), e);
        }

        // true -> 'yes', false -> null (see method comment for details)
        if (featureValue instanceof Boolean) {
            featureValue = Boolean.TRUE.equals(featureValue)
                    ? "yes"
                    : null;
        }

        return featureValue;
    }

    /**
     * Reads feature value (possibly different each time) and stores it, if it's not {@code null}, in the given
     * container. The same feature name can be bound to multiple getters, or the feature value may be an
     * {@link Iterable} - in both cases the map value becomes a {@link SortedSet}.
     * <p>
     * First, this avoids duplicate values coming from multiple methods bound to a same-named feature. Second, for
     * generic methods, each method is found twice, the second time being a bridge method (with all generic types set to
     * {@link Object}). Thus, two technically different methods return the same value.
     *
     * @param step         pipeline step to read feature value from
     * @param whereToStore container to store the feature values in, keyed by {@link #getName()}
     */
    @ConstraintArguments
    public void readFeatureValue(PipelineStep<?, ?> step, Map<String, Object> whereToStore) {
        checkNotNull(step, _PipelineStepFeatureImplReadFeatureValueArgumentsMeta.STEP.name());
        checkNotNull(whereToStore, _PipelineStepFeatureImplReadFeatureValueArgumentsMeta.WHERETOSTORE.name());

        Object value = valueGetter.apply(step);
        // don't store nulls
        if (value == null) {
            return;
        }

        Object previousValue = whereToStore.putIfAbsent(name, value);
        // if there was no value previously for this key, we're done
        if (previousValue == null) {
            return;
        }

        List<?> previousValues = (previousValue instanceof Iterable<?> it)
                ? IterableUtils.toList(it)
                : List.of(previousValue);

        var sortedSet = new TreeSet<Object>(previousValues);
        sortedSet.add(value);

        // if multiple values have been squashed into one - i.e. they're duplicates - store 'value', not '[value]'
        whereToStore.put(name, (sortedSet.size() == 1)
                ? sortedSet.first()
                : Collections.unmodifiableSet(sortedSet));
    }

}
