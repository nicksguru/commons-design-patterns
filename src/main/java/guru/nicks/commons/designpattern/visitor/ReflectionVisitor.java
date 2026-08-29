package guru.nicks.commons.designpattern.visitor;

import guru.nicks.commons.cache.domain.CacheConstants;
import guru.nicks.commons.designpattern.SubclassBeforeSuperclassMap;
import guru.nicks.commons.utils.ExceptionUtils;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.annotation.Nullable;

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
 * <p>
 * When a visited type is assignable to multiple registered visit parameter types that are not related to each other
 * (e.g., a superclass and an interface), the visitor method registered first wins.
 *
 * @param <O> visitor output type
 */
public abstract class ReflectionVisitor<O> implements Function<Object, Optional<O>> {

    /**
     * Runtimes (visitor definitions plus resolution caches) shared by all instances of the same visitor class. Static,
     * so that short-lived visitors don't pay the reflection scan and a throwaway cache on every instantiation. Keyed by
     * visitor class, so visitable resolutions of different visitor classes never mix.
     */
    private static final Cache<Class<?>, VisitorClassRuntime> VISITOR_CLASS_RUNTIMES = Caffeine.newBuilder()
            .maximumSize(CacheConstants.DEFAULT_CAFFEINE_CACHE_CAPACITY)
            .build();

    private final VisitorClassRuntime visitorClassRuntime = VISITOR_CLASS_RUNTIMES.get(getClass(),
            VisitorClassRuntime::createFor);

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

        Optional<VisitorDefinition> visitor = visitorClassRuntime.findVisitor(visitable.getClass());
        if (visitor.isEmpty()) {
            return Optional.empty();
        }

        VisitorDefinition visitorDefinition = visitor.get();
        try {
            // handle invocation (unlike Method.invoke) allocates no varargs array and does no per-call access checks
            Object result = visitorDefinition.getVisitorMethodHandle().invoke(this, visitable);

            if (result == null) {
                throw new IllegalStateException("Visitor method returned null instead of Optional: "
                        + visitorDefinition.getVisitorMethod());
            }

            return (Optional<O>) result;
        }
        // exception transparency: handle invocation throws the target's exception as-is (no
        // InvocationTargetException wrapping), so rethrow it unchanged
        catch (Throwable e) {
            throw ExceptionUtils.sneakyThrow(e);
        }
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

    /**
     * Runtime data shared by all instances of one visitor class: collected visitor definitions plus a cache resolving
     * visitable classes to matching definitions. Stored per visitor class (not per instance) in
     * {@link #VISITOR_CLASS_RUNTIMES}, so that short-lived visitors reuse both the reflection results and the
     * resolution cache.
     */
    private static final class VisitorClassRuntime {

        private final SubclassBeforeSuperclassMap<?, VisitorDefinition> visitorDefinitions;

        /**
         * Cache storing visitor definitions for visited classes.
         */
        private final Cache<Class<?>, Optional<VisitorDefinition>> visitorCache = Caffeine.newBuilder()
                .maximumSize(CacheConstants.DEFAULT_CAFFEINE_CACHE_CAPACITY)
                .build();

        /**
         * Cache loader for {@link #visitorCache}, hoisted into a field because the method reference captures
         * {@code this} - evaluated per call, it would allocate a fresh lambda on every lookup.
         */
        private final Function<Class<?>, Optional<VisitorDefinition>> visitorLoader =
                this::findVisitorWithoutCache;

        private VisitorClassRuntime(SubclassBeforeSuperclassMap<?, VisitorDefinition> visitorDefinitions) {
            this.visitorDefinitions = visitorDefinitions;
        }

        /**
         * Collects visitor definitions for the given visitor class. Called on cache miss only, so the reflection scan
         * runs once per visitor class; if it throws (invalid or missing visitor methods), the exception propagates to
         * the constructor of the first instance.
         *
         * @param visitorClass visitor class to collect definitions for
         * @return runtime for the visitor class
         */
        private static VisitorClassRuntime createFor(Class<?> visitorClass) {
            return new VisitorClassRuntime(VisitorDefinition.collectVisitorMethodsOrThrow(visitorClass));
        }

        /**
         * Resolves the visitor definition (or its absence) for a visitable class, using the shared cache.
         *
         * @param visitableClass class of the object being visited
         * @return matching visitor definition, empty if this visitor class cannot visit it
         */
        private Optional<VisitorDefinition> findVisitor(Class<?> visitableClass) {
            // 'get' method may return null as per Caffeine specs, but never does in this particular case -
            // because it stores (possibly empty) Optional's
            return visitorCache.get(visitableClass, visitorLoader);
        }

        /**
         * Resolves the visitor definition (or its absence) for a visitable class.
         */
        private Optional<VisitorDefinition> findVisitorWithoutCache(Class<?> visitableClass) {
            return visitorDefinitions.findEntryForClosestSuperclass(visitableClass).map(Map.Entry::getValue);
        }

    }

}
