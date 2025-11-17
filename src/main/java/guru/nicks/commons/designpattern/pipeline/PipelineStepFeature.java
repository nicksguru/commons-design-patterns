package guru.nicks.commons.designpattern.pipeline;

import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.regex.Pattern;

/**
 * Marks getter methods in {@link PipelineStep} subclasses as feature names. Such methods must be publicly accessible
 * and demand no arguments.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface PipelineStepFeature {

    /**
     * Specifies the following optional prefixes to be stripped off method names to infer feature names: 'get', 'is',
     * 'has', 'have', 'can', 'may', 'should', 'must', 'wants', 'want'.
     * <p>
     * NOTE: {@code matcher.matches()} needs a regexp covering the whole string, {@code matcher.find()} does not;
     * '$1' is the feature name (will be un-capitalized).
     */
    Pattern FEATURE_METHOD_NAME_PATTERN = Pattern.compile("^(?:get|is|has|have|can|may|should|must|wants|want)(.+)$");

    /**
     * Alias for {@link #name()}.
     */
    @AliasFor("name")
    String value() default "";

    /**
     * If omitted, the feature name is derived from the method name: 'getFeatureX' becomes 'featureX', as per
     * {@link #FEATURE_METHOD_NAME_PATTERN}.
     *
     * @return feature name
     */
    String name() default "";

}
