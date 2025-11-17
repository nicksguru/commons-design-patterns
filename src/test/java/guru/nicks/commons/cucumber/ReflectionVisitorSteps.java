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

    @Given("a reflection visitor is created with duplicate method signatures")
    public void aReflectionVisitorIsCreatedWithDuplicateMethodSignatures() {
        textWorld.setLastException(catchThrowable(DuplicateMethodVisitor::new));
    }

    @Given("a reflection visitor is created with an invalid method signature")
    public void aReflectionVisitorIsCreatedWithAnInvalidMethodSignature() {
        textWorld.setLastException(catchThrowable(InvalidMethodVisitor::new));
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

    @When("an object that triggers the exception is visited")
    public void anObjectThatTriggersTheExceptionIsVisited() {
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

    public static class ExceptionThrowingVisitor extends ReflectionVisitor<String> {

        @ReflectionVisitorMethod
        public Optional<String> visit(ExceptionTrigger trigger) {
            throw new RuntimeException("Test exception");
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
