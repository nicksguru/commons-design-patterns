package guru.nicks.commons.cucumber;

import guru.nicks.commons.cucumber.world.TextWorld;
import guru.nicks.commons.designpattern.visitor.ReflectionVisitorMethod;
import guru.nicks.commons.designpattern.visitor.StatefulReflectionVisitor;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Step definitions for testing {@link StatefulReflectionVisitor}.
 */
@RequiredArgsConstructor
public class StatefulReflectionVisitorSteps {

    // DI
    private final TextWorld textWorld;

    private TestVisitor visitor;
    private VisitorState state;
    private Optional<String> result;

    @Given("a stateful reflection visitor is created")
    public void aStatefulReflectionVisitorIsCreated() {
        visitor = new TestVisitor();
        state = visitor.createNewState();
        assertThat(state)
                .as("state")
                .isNotNull();
    }

    @Given("a visitor with invalid visitor method is created")
    public void aVisitorWithInvalidVisitorMethodIsCreated() {
        try {
            new InvalidVisitor();
        } catch (Exception e) {
            textWorld.setLastException(e);
        }
    }

    @When("a visitable object of type {string} is visited")
    public void aVisitableObjectOfTypeIsVisited(String type) {
        Object visitable;

        switch (type) {
            case "SimpleVisitable" -> visitable = new SimpleVisitable();
            case "ChildVisitable" -> visitable = new ChildVisitable();
            case "AnotherVisitable" -> visitable = new AnotherVisitable();
            default -> throw new IllegalArgumentException("Unknown visitable type: " + type);
        }

        result = visitor.apply(visitable, state);
    }

    @When("a null object is visited statefully")
    public void aNullObjectIsVisitedStatefully() {
        try {
            result = visitor.apply(null, state);
        } catch (Exception e) {
            textWorld.setLastException(e);
        }
    }

    @Then("the visitor should process the object")
    public void theVisitorShouldProcessTheObject() {
        assertThat(result)
                .as("result")
                .isPresent();
    }

    @Then("the visitor should process the object using the most specific visitor method")
    public void theVisitorShouldProcessTheObjectUsingTheMostSpecificVisitorMethod() {
        assertThat(result)
                .as("result")
                .isPresent();
        assertThat(result).contains("child-visited");
    }

    @Then("the state should be updated")
    public void theStateShouldBeUpdated() {
        assertThat(state.getValue()).isNotNull();
    }

    @Then("the visitor should return an empty result")
    public void theVisitorShouldReturnAnEmptyResult() {
        assertThat(textWorld.getLastException()).isNotNull();
        assertThat(textWorld.getLastException()).isInstanceOf(NullPointerException.class);
    }

    @Then("the state should contain the value {string}")
    public void theStateShouldContainTheValue(String value) {
        assertThat(state.getValue()).isEqualTo(value);
    }

    /**
     * Visitor state class for testing.
     */
    @Data
    public static class VisitorState {

        private String value;

    }

    /**
     * Simple visitable class for testing.
     */
    public static class SimpleVisitable {
    }

    /**
     * Child visitable class for testing.
     */
    public static class ChildVisitable extends SimpleVisitable {
    }

    /**
     * Another visitable class for testing.
     */
    public static class AnotherVisitable {
    }

    /**
     * Test visitor implementation.
     */
    public static class TestVisitor extends StatefulReflectionVisitor<VisitorState, String> {

        @ReflectionVisitorMethod
        public Optional<String> visit(SimpleVisitable visitable, VisitorState state) {
            state.setValue("simple-processed");
            return Optional.of("simple-visited");
        }

        @ReflectionVisitorMethod
        public Optional<String> visit(ChildVisitable visitable, VisitorState state) {
            state.setValue("child-processed");
            return Optional.of("child-visited");
        }

        @ReflectionVisitorMethod
        public Optional<String> visit(AnotherVisitable visitable, VisitorState state) {
            state.setValue("another-processed");
            return Optional.of("another-visited");
        }
    }

    /**
     * Invalid visitor with wrong state parameter type. Its instantiation must fail when parent class, in constructor,
     * collects and parses visitor methods.
     */
    public static class InvalidVisitor extends StatefulReflectionVisitor<VisitorState, String> {

        @ReflectionVisitorMethod
        public Optional<String> visit(SimpleVisitable visitable, String wrongStateType) {
            return Optional.empty();
        }
    }

}
