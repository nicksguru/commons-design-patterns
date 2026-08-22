package guru.nicks.commons.designpattern.visitor;

import guru.nicks.commons.cache.domain.CacheConstants;
import guru.nicks.commons.designpattern.SubclassBeforeSuperclassMap;
import guru.nicks.commons.utils.ExceptionUtils;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.annotation.Nullable;

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
public abstract class ReflectionVisitor<O> implements Function<Object, Optional<O>> {

    private final SubclassBeforeSuperclassMap<?, VisitorDefinition> visitorDefinitions =
            VisitorDefinition.collectVisitorMethodsOrThrow(getClass());

    /**
     * Cache storing visitor definitions for visited classes. Needed for long-lived visitors, such as Spring beans.
     */
    private final Cache<Class<?>, Optional<VisitorDefinition>> visitorCache = Caffeine.newBuilder()
            .maximumSize(CacheConstants.DEFAULT_CAFFEINE_CACHE_CAPACITY)
            .build();

    /**
     * Invokes visitor whose input class is the closest to the argument class. If the argument class is {@code null},
     * always returns {@link Optional#empty()}.
     *
     * @param visitable any object - the matching visitor will (or will not be) found dynamically
     * @return non-empty {@link Optional} if a visitor has been found and returned something
     */
    @SuppressWarnings("unchecked")
    @Override
    public Optional<O> apply(@Nullable Object visitable) {
        if (visitable == null) {
            return Optional.empty();
        }

        // 'get' method may return null as per Caffeine specs, but never does in this particular case -
        // because it stores (possibly empty) Optional's
        Optional<VisitorDefinition> visitor = visitorCache.get(visitable.getClass(), this::findVisitorWithoutCache);

        return visitor.flatMap(visitorDefinition -> {
            try {
                Object result = visitorDefinition.getVisitorMethod().invoke(this, visitable);

                if (result == null) {
                    throw new IllegalStateException("Visitor method returned null instead of Optional: "
                            + visitorDefinition.getVisitorMethod());
                }

                return (Optional<O>) result;
            }
            // exception transparency: rethrow the original exception as-is, without wrapping
            catch (InvocationTargetException e) {
                throw ExceptionUtils.sneakyThrow(ExceptionUtils.unwrapInvocationTargetException(e));
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("Visitor method access error: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Resolves the visitor definition (or its absence) for a visitable class. Hoisted to a method to avoid allocating a
     * new Lambda on every {@link #apply(Object)} call.
     */
    private Optional<VisitorDefinition> findVisitorWithoutCache(Class<?> visitableClass) {
        return visitorDefinitions.findEntryForClosestSuperclass(visitableClass).map(Map.Entry::getValue);
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
