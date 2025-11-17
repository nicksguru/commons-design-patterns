package guru.nicks.commons.designpattern.visitor;

import guru.nicks.commons.designpattern.SubclassBeforeSuperclassMap;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Lets subclasses define visitor methods and picks the most appropriate one in {@link #apply(Object)}. Each visitor
 * method must:
 * <ul>
 *  <li>be annotated with {@link ReflectionVisitorMethod @ReflectionVisitorMethod}</li>
 *  <li>be public</li>
 *  <li>be called {@link ReflectionVisitorDefinition#VISITOR_METHOD_NAME}</li>
 *  <li>accept one argument - the object being visited</li>
 *  <li>return optional {@code O}</li>
 * </ul>
 * If no visitors have been found, constructor fails because such class cannot 'visit' anything.
 *
 * @param <O> visitor output type
 */
@Slf4j
public abstract class ReflectionVisitor<O> implements Function<Object, Optional<O>> {

    private final SubclassBeforeSuperclassMap<?, VisitorDefinition> visitorDefinitions =
            VisitorDefinition.collectVisitorMethodsOrThrow(getClass());

    /**
     * Cache storing visitor definitions for visited classes. Needed for long-lived visitors, such as Spring beans.
     */
    private final Cache<Class<?>, Optional<VisitorDefinition>> cache = Caffeine.newBuilder()
            .maximumSize(300)
            .build();

    /**
     * Finds visitor whose input class is the closest to the argument class. If the argument class is {@code null},
     * always returns {@link Optional#empty()}.
     *
     * @param visitable any object - because the matching visitor will (or will not be) found dynamically
     * @return non-empty {@link Optional} if a visitor has been found and returned something
     */
    @SuppressWarnings("unchecked")
    @Override
    public Optional<O> apply(@Nullable Object visitable) {
        if (visitable == null) {
            return Optional.empty();
        }

        Optional<VisitorDefinition> visitor = cache.get(visitable.getClass(),
                clazz -> visitorDefinitions.findEntryForClosestSuperclass(clazz).map(Map.Entry::getValue));

        // 'get' method may return null as per Caffeine specs, but never does in this particular case
        //noinspection DataFlowIssue
        return visitor.flatMap(it -> {
            try {
                return (Optional<O>) it.getVisitorMethod().invoke(this, visitable);
            } catch (InvocationTargetException | IllegalAccessException e) {
                throw new IllegalStateException("Visitor method access error: " + e.getMessage(), e);
            }
        });
    }

    private static class VisitorDefinition extends ReflectionVisitorDefinition {

        public VisitorDefinition(Method visitorMethod) {
            super(visitorMethod);
        }

        /**
         * Scans methods annotated with {@link ReflectionVisitorMethod @ReflectionVisitorMethod}. They should accept the
         * only argument - the object being visited.
         *
         * @param source class to scan
         * @return map where the keys are visitable classes (methods' only arguments)
         */
        public static SubclassBeforeSuperclassMap<?, VisitorDefinition> collectVisitorMethodsOrThrow(Class<?> source) {
            return collectVisitorMethodsOrThrow(source, 1)
                    .stream()
                    .map(VisitorDefinition::new)
                    .collect(Collectors.toMap(
                            ReflectionVisitorDefinition::getVisitableClass,
                            visitorDefinition -> visitorDefinition,
                            // don't allow visitors to process the same visitable class
                            (value1, value2) -> {
                                throw new IllegalStateException(
                                        "Two visitors for the same class: " + value1 + ", " + value2);
                            }, SubclassBeforeSuperclassMap::new));
        }

    }

}
