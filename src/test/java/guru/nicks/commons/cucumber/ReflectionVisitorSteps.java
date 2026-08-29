package guru.nicks.commons.cucumber;

import guru.nicks.commons.cucumber.world.TextWorld;
import guru.nicks.commons.designpattern.visitor.ReflectionVisitor;
import guru.nicks.commons.designpattern.visitor.ReflectionVisitorMethod;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.RequiredArgsConstructor;
import lombok.Value;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * Step definitions for testing {@link ReflectionVisitor} functionality.
 */
@RequiredArgsConstructor
public class ReflectionVisitorSteps {

    // DI
    private final TextWorld textWorld;

    private final List<Object> visitedObjects = new ArrayList<>();
    private final List<Optional<?>> results = new ArrayList<>();
    private final Map<Class<?>, String> resultsByType = new HashMap<>();
    private ReflectionVisitor<?> visitor;
    private Object visitedObject;
    private Optional<?> result;

    @Given("a reflection visitor is created with methods for different types")
    public void aReflectionVisitorIsCreatedWithMethodsForDifferentTypes() {
        visitor = new TestVisitor();
        visitedObjects.clear();
        results.clear();
        resultsByType.clear();
    }

    @Given("a reflection visitor is created with methods for a class hierarchy")
    public void aReflectionVisitorIsCreatedWithMethodsForAClassHierarchy() {
        visitor = new HierarchyTestVisitor();
    }

    @Given("a reflection visitor is created with a method that throws an exception")
    public void aReflectionVisitorIsCreatedWithAMethodThatThrowsAnException() {
        visitor = new ExceptionThrowingVisitor();
    }

    @Given("a reflection visitor is created with a method that throws a checked exception")
    public void aReflectionVisitorIsCreatedWithAMethodThatThrowsACheckedException() {
        visitor = new CheckedExceptionThrowingVisitor();
    }

    @Given("a reflection visitor overrides an annotated visitor method without re-annotating it")
    public void aReflectionVisitorOverridesAnAnnotatedVisitorMethodWithoutReAnnotatingIt() {
        visitor = new OverridingVisitor();
    }

    @Given("a reflection visitor is created with duplicate method signatures")
    public void aReflectionVisitorIsCreatedWithDuplicateMethodSignatures() {
        textWorld.setLastException(catchThrowable(DuplicateMethodVisitor::new));
    }

    @Given("a reflection visitor is created with an invalid method signature")
    public void aReflectionVisitorIsCreatedWithAnInvalidMethodSignature() {
        textWorld.setLastException(catchThrowable(InvalidMethodVisitor::new));
    }

    @Given("a reflection visitor registers visit\\(UnrelatedBase) before visit\\(UnrelatedMarker)")
    public void aReflectionVisitorRegistersBaseBeforeMarker() {
        visitor = new BaseFirstVisitor();
    }

    @Given("a reflection visitor registers visit\\(UnrelatedMarker) before visit\\(UnrelatedBase)")
    public void aReflectionVisitorRegistersMarkerBeforeBase() {
        visitor = new MarkerFirstVisitor();
    }

    @When("a null object is visited")
    public void aNullObjectIsVisited() {
        try {
            result = visitor.apply(null);
        } catch (Exception e) {
            textWorld.setLastException(e);
        }
    }

    @When("an object is visited")
    public void anObjectIsVisited() {
        visitedObject = new TestString("test string");
        result = visitor.apply(visitedObject);
    }

    @When("a subclass object is visited")
    public void aSubclassObjectIsVisited() {
        visitedObject = new ChildTestObject();
        result = visitor.apply(visitedObject);
    }

    @When("an UnrelatedChild object is visited")
    public void anUnrelatedChildObjectIsVisited() {
        visitedObject = new UnrelatedChild();
        result = visitor.apply(visitedObject);
    }

    @When("an object that triggers the exception is visited")
    public void anObjectThatTriggersTheExceptionIsVisited() {
        try {
            visitedObject = new ExceptionTrigger();
            result = visitor.apply(visitedObject);
        } catch (Exception e) {
            textWorld.setLastException(e);
        }
    }

    @When("an object that triggers the checked exception is visited")
    public void anObjectThatTriggersTheCheckedExceptionIsVisited() {
        try {
            visitedObject = new ExceptionTrigger();
            result = visitor.apply(visitedObject);
        } catch (Exception e) {
            textWorld.setLastException(e);
        }
    }

    @When("objects of different types are visited")
    public void objectsOfDifferentTypesAreVisited() {
        visitedObjects.add(new TestString("string value"));
        visitedObjects.add(new TestInteger(42));
        visitedObjects.add(new TestBoolean(true));

        for (Object obj : visitedObjects) {
            Optional<?> res = visitor.apply(obj);
            results.add(res);

            res.ifPresent(it ->
                    resultsByType.put(obj.getClass(), it.toString()));
        }
    }

    @Then("the appropriate visitor method should be called")
    public void theAppropriateVisitorMethodShouldBeCalled() {
        assertThat(result)
                .as("result")
                .isPresent();
        assertThat(result.get().toString())
                .as("result.get().toString()")
                .contains("TestString");
    }

    @Then("the most specific visitor method should be called")
    public void theMostSpecificVisitorMethodShouldBeCalled() {
        assertThat(result)
                .as("result")
                .isPresent();
        assertThat(result.get())
                .as("result.get().toString()")
                .hasToString("Visited ChildTestObject");
    }

    @Then("the overridden visitor method should be invoked")
    public void theOverriddenVisitorMethodShouldBeInvoked() {
        assertThat(result)
                .as("result")
                .isPresent();
        assertThat(result.get())
                .as("result.get()")
                .hasToString("Visited TestString: overridden");
    }

    @Then("an empty Optional should be returned")
    public void anEmptyOptionalShouldBeReturned() {
        assertThat(result)
                .as("result")
                .isEmpty();
    }

    @Then("the result should be returned as an Optional")
    public void theResultShouldBeReturnedAsAnOptional() {
        assertThat(result)
                .as("result")
                .isInstanceOf(Optional.class);
    }

    @Then("the result should be {string}")
    public void theResultShouldBe(String expectedResult) {
        assertThat(result)
                .as("result")
                .isPresent();
        assertThat(result.get())
                .as("result.get()")
                .isEqualTo(expectedResult);
    }

    @Then("each object should be handled by the appropriate visitor method")
    public void eachObjectShouldBeHandledByTheAppropriateVisitorMethod() {
        assertThat(results)
                .as("results")
                .hasSize(visitedObjects.size());

        // all results should be present
        assertThat(results)
                .as("results")
                .allMatch(Optional::isPresent);

        // Check specific results by type
        assertThat(resultsByType.get(TestString.class))
                .as("resultsByType.get(TestString.class)")
                .contains("TestString");
        assertThat(resultsByType.get(TestInteger.class))
                .as("resultsByType.get(TestInteger.class)")
                .contains("TestInteger");
        assertThat(resultsByType.get(TestBoolean.class))
                .as("resultsByType.get(TestBoolean.class)")
                .contains("TestBoolean");
    }

    // Test classes
    @Value
    public static class TestString {

        String value;

    }

    @Value
    public static class TestInteger {

        Integer value;

    }

    @Value
    public static class TestBoolean {

        Boolean value;

    }

    public static class ParentTestObject {
    }

    public static class ChildTestObject extends ParentTestObject {
    }

    public static class ExceptionTrigger {
    }

    // UnrelatedChild is assignable to BOTH ancestors, which are not related to each other - dispatch must
    // follow registration order, not type specificity
    public static class UnrelatedBase {
    }

    public interface UnrelatedMarker {
    }

    public static class UnrelatedChild extends UnrelatedBase implements UnrelatedMarker {
    }

    public static class TestVisitor extends ReflectionVisitor<String> {

        @ReflectionVisitorMethod
        public Optional<String> visit(TestString testString) {
            return Optional.of("Visited TestString: " + testString.getValue());
        }

        @ReflectionVisitorMethod
        public Optional<String> visit(TestInteger testInteger) {
            return Optional.of("Visited TestInteger: " + testInteger.getValue());
        }

        @ReflectionVisitorMethod
        public Optional<String> visit(TestBoolean testBoolean) {
            return Optional.of("Visited TestBoolean: " + testBoolean.getValue());
        }
    }

    public static class HierarchyTestVisitor extends ReflectionVisitor<String> {

        @ReflectionVisitorMethod
        public Optional<String> visit(ParentTestObject parent) {
            return Optional.of("Visited ParentTestObject");
        }

        @ReflectionVisitorMethod
        public Optional<String> visit(ChildTestObject child) {
            return Optional.of("Visited ChildTestObject");
        }
    }

    /**
     * Registers visit(UnrelatedBase) BEFORE visit(UnrelatedMarker): the method scan is breadth-first from the
     * visitor class itself up, so the own visit(UnrelatedBase) registers before the superclass's
     * visit(UnrelatedMarker) - for UnrelatedChild (assignable to both unrelated ancestors) the first-registered
     * method must win. Splitting across classes keeps the order deterministic: getDeclaredMethods order within
     * a single class is JVM-specific.
     */
    public static class BaseFirstVisitor extends MarkerMethodVisitor {

        @ReflectionVisitorMethod
        public Optional<String> visit(UnrelatedBase base) {
            return Optional.of("Visited UnrelatedBase");
        }
    }

    /**
     * Declares the marker-ancestor visit method that {@link BaseFirstVisitor} registers SECOND (superclass
     * methods are scanned after the subclass's own).
     */
    public static class MarkerMethodVisitor extends ReflectionVisitor<String> {

        @ReflectionVisitorMethod
        public Optional<String> visit(UnrelatedMarker marker) {
            return Optional.of("Visited UnrelatedMarker");
        }
    }

    /**
     * Registers visit(UnrelatedMarker) BEFORE visit(UnrelatedBase) - reverse of {@link BaseFirstVisitor},
     * pinning that dispatch follows registration order, not type specificity.
     */
    public static class MarkerFirstVisitor extends BaseMethodVisitor {

        @ReflectionVisitorMethod
        public Optional<String> visit(UnrelatedMarker marker) {
            return Optional.of("Visited UnrelatedMarker");
        }
    }

    /**
     * Declares the base-ancestor visit method that {@link MarkerFirstVisitor} registers SECOND.
     */
    public static class BaseMethodVisitor extends ReflectionVisitor<String> {

        @ReflectionVisitorMethod
        public Optional<String> visit(UnrelatedBase base) {
            return Optional.of("Visited UnrelatedBase");
        }
    }

    public static class ExceptionThrowingVisitor extends ReflectionVisitor<String> {

        @ReflectionVisitorMethod
        public Optional<String> visit(ExceptionTrigger trigger) {
            throw new RuntimeException("Test exception");
        }
    }

    public static class CheckedExceptionThrowingVisitor extends ReflectionVisitor<String> {

        @ReflectionVisitorMethod
        public Optional<String> visit(ExceptionTrigger trigger) throws Exception {
            throw new Exception("Checked test exception");
        }
    }

    public static class AnnotatedMethodVisitor extends ReflectionVisitor<String> {

        @ReflectionVisitorMethod
        public Optional<String> visit(TestString testString) {
            return Optional.of("Visited TestString: annotated");
        }
    }

    /**
     * Overrides the superclass's annotated visitor method WITHOUT re-annotating it - the override must be invoked
     * instead of both methods being registered and clashing.
     */
    public static class OverridingVisitor extends AnnotatedMethodVisitor {

        @Override
        public Optional<String> visit(TestString testString) {
            return Optional.of("Visited TestString: overridden");
        }
    }

    // This class has two methods for the same type, which should cause an exception
    public static class DuplicateMethodVisitor extends ReflectionVisitor<String> {

        @ReflectionVisitorMethod
        public Optional<String> visit(TestString testString) {
            return Optional.of("First method for TestString");
        }

        // This is a duplicate method for the same type
        @ReflectionVisitorMethod
        public Optional<String> visit1(TestString duplicateParam) {
            return Optional.of("Second method for TestString");
        }

    }

    public static class InvalidMethodVisitor extends ReflectionVisitor<String> {

        // This method has an invalid signature (wrong return type)
        @ReflectionVisitorMethod
        public String visit(TestString testString) {
            return "Invalid return type";
        }

    }

}
