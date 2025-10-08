package guru.nicks.designpattern.visitor;

import guru.nicks.designpattern.SubclassBeforeSuperclassMap;
import guru.nicks.utils.ReflectionUtils;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import static guru.nicks.validation.dsl.ValiDsl.checkNotNull;

/**
 * {@link ReflectionVisitor} augmented with state (passed to each visitor call) added, which lets keep visitors
 * immutable.
 * <ul>
 *  <li>be annotated with {@link ReflectionVisitorMethod @ReflectionVisitorMethod}</li>
 *  <li>be public</li>
 *  <li>be called {@link ReflectionVisitorDefinition#VISITOR_METHOD_NAME}</li>
 *  <li>accept two arguments: the object being visited and the (mutable) state ({@code S})</li>
 *  <li>return optional {@code O}</li>
 * </ul>
 * If no visitors have been found, constructor fails because such class cannot 'visit' anything.
 *
 * @param <S> visitor state type
 * @param <O> visitor output type
 */
@Slf4j
public abstract class StatefulReflectionVisitor<S, O> implements BiFunction<Object, S, Optional<O>> {

    private final SubclassBeforeSuperclassMap<?, VisitorDefinition> visitorDefinitions =
            VisitorDefinition.collectVisitorMethodsOrThrow(getClass(), getStateClass());

    /**
     * Cache storing visitor definitions for visited classes. Needed for long-lived visitors, such as Spring beans.
     */
    private final Cache<Class<?>, Optional<VisitorDefinition>> cache = Caffeine.newBuilder()
            .maximumSize(300)
            .build();

    /**
     * Creates a state object which is then passed to {@link #apply(Object, Object)}. Default implementation
     * instantiates {@code S}.
     *
     * @return state object
     */
    public S createNewState() {
        Class<? extends S> clazz = getStateClass();
        return ReflectionUtils.instantiateEvenWithoutDefaultConstructor(clazz);
    }

    @SuppressWarnings("unchecked")
    public Class<? extends S> getStateClass() {
        return (Class<? extends S>) ReflectionUtils
                .findFirstMaterializedGenericType(getClass(), StatefulReflectionVisitor.class)
                .orElseThrow(() -> new IllegalStateException("Failed to infer state class"));
    }

    /**
     * Finds visitor whose input class is the closest to {@code input} actual class.
     *
     * @param visitable any object is accepted because the matching visitor will (or will not be) found dynamically
     * @return non-empty {@link Optional} if a visitor has been found and returned something
     */
    @SuppressWarnings("unchecked")
    @Override
    public Optional<O> apply(Object visitable, S state) {
        checkNotNull(visitable, "object to visit");

        Optional<VisitorDefinition> visitor = cache.get(visitable.getClass(),
                clazz -> visitorDefinitions.findEntryForClosestSuperclass(clazz).map(Map.Entry::getValue));

        // 'get' method may return null as per Caffeine specs, but never does in this particular case
        //noinspection DataFlowIssue
        return visitor.flatMap(it -> {
            try {
                return (Optional<O>) it.getVisitorMethod().invoke(this, visitable, state);
            } catch (InvocationTargetException | IllegalAccessException e) {
                throw new IllegalArgumentException("Visitor method access error: " + e.getMessage(), e);
            }
        });
    }

    private static class VisitorDefinition extends ReflectionVisitorDefinition {

        public VisitorDefinition(Method visitorMethod) {
            super(visitorMethod);
        }

        /**
         * Scans methods annotated with {@link ReflectionVisitorMethod @ReflectionVisitorMethod}.
         *
         * @param source     class to scan
         * @param stateClass visitor state class the methods accept as their second argument (the first one is the
         *                   object being visited)
         * @return map where the keys are visitable classes (methods' first arguments)
         */
        public static SubclassBeforeSuperclassMap<?, VisitorDefinition> collectVisitorMethodsOrThrow(Class<?> source,
                Class<?> stateClass) {
            return collectVisitorMethodsOrThrow(source, 2)
                    .stream()
                    .map(VisitorDefinition::new)
                    // make sure each visitor method accepts visitor state (S) in its second argument
                    .filter(visitorDefinition -> {
                        Method method = visitorDefinition.getVisitorMethod();

                        if (!stateClass.isAssignableFrom(method.getParameterTypes()[1])) {
                            throw new IllegalStateException("Visitor method '" + method
                                    + "' must accept state (of " + stateClass + ") as the second argument");
                        }

                        return true;
                    }).collect(Collectors.toMap(
                            ReflectionVisitorDefinition::getVisitableClass,
                            visitorDefinition -> visitorDefinition,
                            // don't allow visitors to process the same visitable class
                            (value1, value2) -> {
                                throw new IllegalStateException(
                                        "Two visitors cannot process the same argument: " + value1 + ", " + value2);
                            }, SubclassBeforeSuperclassMap::new));
        }

    }

}
