package guru.nicks.commons.designpattern.visitor;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks visitor methods (in {@link ReflectionVisitor} and {@link StatefulReflectionVisitor} subclasses) to be called by
 * {@link ReflectionVisitor#apply(Object)} and {@link StatefulReflectionVisitor#apply(Object, Object)} accordingly.
 * <p>
 * For important details on visitor method signatures (names, argument types, return value types), please see the above
 * base classes.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ReflectionVisitorMethod {
}
