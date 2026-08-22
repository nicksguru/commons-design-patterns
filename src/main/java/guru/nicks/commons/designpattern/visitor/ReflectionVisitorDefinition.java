package guru.nicks.commons.designpattern.visitor;

import guru.nicks.commons.utils.ReflectionUtils;

import am.ik.yavi.meta.ConstraintArguments;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static guru.nicks.commons.validation.dsl.ValiDsl.check;
import static guru.nicks.commons.validation.dsl.ValiDsl.checkNotNull;

/**
 * Visitor definition.
 */
@Getter
@Slf4j
public abstract class ReflectionVisitorDefinition {

    public static final String VISITOR_METHOD_NAME = "visit";

    private final Class<?> visitableClass;
    private final Method visitorMethod;

    /**
     * Constructor.
     *
     * @param visitorMethod non-null method whose first parameter is the object being visited
     */
    @ConstraintArguments
    protected ReflectionVisitorDefinition(Method visitorMethod) {
        this.visitorMethod = checkNotNull(visitorMethod,
                _ReflectionVisitorDefinitionArgumentsMeta.VISITORMETHOD.name());
        visitableClass = this.visitorMethod.getParameterTypes()[0];
    }

    /**
     * Finds methods annotated with {@link ReflectionVisitorMethod @ReflectionVisitorMethod} in the given class (also in
     * its superclasses/superinterfaces) and verifies their signatures:
     * <ul>
     *  <li>access level - must be {@code public}</li>
     *  <li>name - must be {@value #VISITOR_METHOD_NAME}</li>
     *  <li>return type - must be {@link Optional}</li>
     *  <li>number of arguments - must be {@code numberOfMethodArguments}</li>
     * </ul>
     *
     * @param source                  class to scan
     * @param numberOfMethodArguments number of visitor method arguments: 1 for {@link ReflectionVisitor#apply(Object)},
     *                                2 for {@link StatefulReflectionVisitor#apply(Object, Object)}
     * @return visitor methods found
     * @throws IllegalStateException no visitor methods found, which means the class cannot 'visit' anything
     */
    @ConstraintArguments
    public static List<Method> collectVisitorMethodsOrThrow(Class<?> source, int numberOfMethodArguments) {
        checkNotNull(source, _ReflectionVisitorDefinitionCollectVisitorMethodsOrThrowArgumentsMeta.SOURCE.name());
        check(numberOfMethodArguments,
                _ReflectionVisitorDefinitionCollectVisitorMethodsOrThrowArgumentsMeta.NUMBEROFMETHODARGUMENTS.name()
        ).greaterThan(0);

        List<Method> visitors = removeOverriddenMethods(
                ReflectionUtils.findAnnotatedMethods(source, ReflectionVisitorMethod.class));
        visitors.forEach(method -> validateVisitorMethod(method, numberOfMethodArguments));

        if (visitors.isEmpty()) {
            throw new IllegalStateException("No @" + ReflectionVisitorMethod.class.getSimpleName()
                    + "-annotated methods found in " + source);
        }

        return visitors;
    }

    /**
     * Removes methods that are override-equivalent (same name and parameter types, as per the Java Language
     * Specification) to another, more-derived method. Because
     * {@link ReflectionUtils#findAnnotatedMethods(Class, Class)} returns methods in class-hierarchy order (subclass
     * declarations first), keeping the first occurrence of each signature preserves the most-derived declaration.
     * <p>
     * This approach lets a subclass override an annotated visitor method without re-annotating it (or re-annotating it)
     * - the override gets invoked instead of both being registered and clashing with a confusing 'two visitors' error.
     *
     * @param methods visitor methods in class-hierarchy order (most-derived first)
     * @return methods with overridden (less-derived) duplicates removed, original relative order preserved
     */
    private static List<Method> removeOverriddenMethods(List<Method> methods) {
        record MethodSignature(String name, List<Class<?>> parameterTypes) {
        }

        var seenSignatures = new HashSet<MethodSignature>();
        return methods.stream()
                .filter(method -> seenSignatures.add(new MethodSignature(
                        method.getName(),
                        List.of(method.getParameterTypes()))))
                .toList();
    }

    private static void validateVisitorMethod(Method method, int expectedMethodArgumentCount) {
        if (log.isTraceEnabled()) {
            log.trace("Found a possibly visitor method: {}", method);
        }

        String methodLabel = "Visitor method '" + method + "'";

        check(method.getModifiers(), methodLabel + ": access level").constraint(
                Modifier::isPublic, "must be public");

        check(method.getName(), methodLabel + ": name").constraint(
                VISITOR_METHOD_NAME::equals, "must be '" + VISITOR_METHOD_NAME + "'");

        check(method.getReturnType(), methodLabel + ": return type").constraint(
                Optional.class::isAssignableFrom, "must be Optional");

        check(method.getParameterCount(), methodLabel + ": number of arguments").eq(expectedMethodArgumentCount);
    }

}
