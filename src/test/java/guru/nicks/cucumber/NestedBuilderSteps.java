package guru.nicks.cucumber;

import guru.nicks.cucumber.world.TextWorld;
import guru.nicks.designpattern.NestedBuilder;

import io.cucumber.java.DataTableType;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Value;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * Step definitions for {@link NestedBuilder} functionality testing.
 */
@RequiredArgsConstructor
public class NestedBuilderSteps {

    // DI
    private final TextWorld textWorld;

    private TestParentBuilder parentBuilder;
    private TestNestedBuilder nestedBuilder;
    private TestNestedObject builtNestedObject;

    private TestParentBuilder returnedParentBuilder;
    private List<TestNestedBuilder> multipleNestedBuilders;
    private List<TestNestedObject> builtNestedObjects;

    private SpecializedNestedBuilder specializedBuilder;
    private SpecializedNestedObject specializedObject;

    @DataTableType
    public NestedBuilderData createNestedBuilderData(Map<String, String> entry) {
        return NestedBuilderData.builder()
                .name(entry.get("name"))
                .value(entry.get("value"))
                .build();
    }

    @Given("a parent builder with name {string}")
    public void aParentBuilderWithName(String parentName) {
        parentBuilder = TestParentBuilder.builder()
                .name(parentName)
                .build();
    }

    @Given("a nested builder with name {string} and value {string}")
    public void aNestedBuilderWithNameAndValue(String nestedName, String nestedValue) {
        nestedBuilder = new TestNestedBuilder(parentBuilder, nestedName, nestedValue);
    }

    @Given("a null parent builder")
    public void aNullParentBuilder() {
        parentBuilder = null;
    }

    @Given("a concrete nested builder implementation")
    public void aConcreteNestedBuilderImplementation() {
        parentBuilder = TestParentBuilder.builder()
                .name("ConcreteParent")
                .build();
        nestedBuilder = new TestNestedBuilder(parentBuilder, "ConcreteNested", "ConcreteValue");
    }

    @Given("multiple nested builders are created:")
    public void multipleNestedBuildersAreCreated(List<NestedBuilderData> builderDataList) {
        multipleNestedBuilders = builderDataList.stream()
                .map(data -> new TestNestedBuilder(parentBuilder, data.getName(), data.getValue()))
                .toList();
    }

    @Given("a specialized nested builder that extends the base nested builder")
    public void aSpecializedNestedBuilderThatExtendsTheBaseNestedBuilder() {
        parentBuilder = TestParentBuilder.builder()
                .name("SpecializedParent")
                .build();
        specializedBuilder = new SpecializedNestedBuilder(parentBuilder, "SpecializedNested", "SpecializedValue", 42);
    }

    @When("creating a nested builder with the null parent")
    public void creatingANestedBuilderWithTheNullParent() {
        var exception = catchThrowable(() -> new TestNestedBuilder(null, "TestName", "TestValue"));
        textWorld.setLastException(exception);
    }

    @When("the nested builder builds the object")
    public void theNestedBuilderBuildsTheObject() {
        var exception = catchThrowable(() -> {
            builtNestedObject = nestedBuilder.build();
        });
        textWorld.setLastException(exception);
    }

    @When("the nested builder returns to parent using and method")
    public void theNestedBuilderReturnsToParentUsingAndMethod() {
        var exception = catchThrowable(() ->
                returnedParentBuilder = nestedBuilder.and());
        textWorld.setLastException(exception);
    }

    @When("the build method is called")
    public void theBuildMethodIsCalled() {
        var exception = catchThrowable(() ->
                builtNestedObject = nestedBuilder.build());
        textWorld.setLastException(exception);
    }

    @When("each nested builder builds its object and returns to parent")
    public void eachNestedBuilderBuildsItsObjectAndReturnsToParent() {
        var exception = catchThrowable(() -> {
            builtNestedObjects = multipleNestedBuilders.stream()
                    .map(TestNestedBuilder::build)
                    .toList();

            returnedParentBuilder = multipleNestedBuilders
                    .getFirst()
                    .and();
        });

        textWorld.setLastException(exception);
    }

    @When("the specialized builder is used to build an object")
    public void theSpecializedBuilderIsUsedToBuildAnObject() {
        var exception = catchThrowable(() -> {
            specializedObject = specializedBuilder.build();
            returnedParentBuilder = specializedBuilder.and();
        });

        textWorld.setLastException(exception);
    }

    @Then("the nested object should have name {string} and value {string}")
    public void theNestedObjectShouldHaveNameAndValue(String expectedName, String expectedValue) {
        assertThat(builtNestedObject)
                .as("builtNestedObject")
                .isNotNull();

        assertThat(builtNestedObject.getName())
                .as("builtNestedObject.getName()")
                .isEqualTo(expectedName);

        assertThat(builtNestedObject.getValue())
                .as("builtNestedObject.getValue()")
                .isEqualTo(expectedValue);
    }

    @Then("the parent builder should be returned")
    public void theParentBuilderShouldBeReturned() {
        assertThat(returnedParentBuilder)
                .as("returnedParentBuilder")
                .isNotNull()
                .isSameAs(parentBuilder);
    }

    @Then("the concrete object should be built")
    public void theConcreteObjectShouldBeBuilt() {
        assertThat(builtNestedObject)
                .as("builtNestedObject")
                .isNotNull();

        assertThat(builtNestedObject.getName())
                .as("builtNestedObject.getName()")
                .isEqualTo("ConcreteNested");

        assertThat(builtNestedObject.getValue())
                .as("builtNestedObject.getValue()")
                .isEqualTo("ConcreteValue");
    }

    @Then("the and method should return the parent builder")
    public void theAndMethodShouldReturnTheParentBuilder() {
        var exception = catchThrowable(() ->
                returnedParentBuilder = nestedBuilder.and());
        textWorld.setLastException(exception);

        assertThat(returnedParentBuilder)
                .as("returnedParentBuilder")
                .isNotNull()
                .isSameAs(parentBuilder);
    }

    @Then("all nested objects should be built correctly")
    public void allNestedObjectsShouldBeBuiltCorrectly() {
        assertThat(builtNestedObjects)
                .as("builtNestedObjects")
                .isNotNull()
                .hasSize(multipleNestedBuilders.size());

        for (int i = 0; i < builtNestedObjects.size(); i++) {
            var builtObject = builtNestedObjects.get(i);
            var originalBuilder = multipleNestedBuilders.get(i);

            assertThat(builtObject.getName())
                    .as("builtObject[%d].getName()", i)
                    .isEqualTo(originalBuilder.getName());

            assertThat(builtObject.getValue())
                    .as("builtObject[%d].getValue()", i)
                    .isEqualTo(originalBuilder.getValue());
        }
    }

    @Then("the final parent builder should be returned")
    public void theFinalParentBuilderShouldBeReturned() {
        assertThat(returnedParentBuilder)
                .as("returnedParentBuilder")
                .isNotNull()
                .isSameAs(parentBuilder);
    }

    @Then("the specialized functionality should work correctly")
    public void theSpecializedFunctionalityShouldWorkCorrectly() {
        assertThat(specializedObject)
                .as("specializedObject")
                .isNotNull();

        assertThat(specializedObject.getName())
                .as("specializedObject.getName()")
                .isEqualTo("SpecializedNested");

        assertThat(specializedObject.getValue())
                .as("specializedObject.getValue()")
                .isEqualTo("SpecializedValue");

        assertThat(specializedObject.getSpecialProperty())
                .as("specializedObject.getSpecialProperty()")
                .isEqualTo(42);
    }

    @Then("the parent builder should still be accessible")
    public void theParentBuilderShouldStillBeAccessible() {
        assertThat(returnedParentBuilder)
                .as("returnedParentBuilder")
                .isNotNull()
                .isSameAs(parentBuilder);
    }

    /**
     * Test implementation of a parent builder.
     */
    @Value
    @Builder
    public static class TestParentBuilder {

        String name;

    }

    /**
     * Test implementation of a nested object.
     */
    @Value
    @Builder
    public static class TestNestedObject {

        String name;
        String value;

    }

    /**
     * Test implementation of {@link NestedBuilder}.
     */
    @Getter
    public static class TestNestedBuilder extends NestedBuilder<TestNestedObject, TestParentBuilder> {

        private final String name;
        private final String value;

        public TestNestedBuilder(TestParentBuilder parentBuilder, String name, String value) {
            super(parentBuilder);
            this.name = name;
            this.value = value;
        }

        @Override
        public TestNestedObject build() {
            return TestNestedObject.builder()
                    .name(name)
                    .value(value)
                    .build();
        }

    }

    /**
     * Specialized nested object for inheritance testing.
     */
    @Value
    @Builder
    public static class SpecializedNestedObject {

        String name;
        String value;

        int specialProperty;

    }

    /**
     * Specialized nested builder for inheritance testing.
     */
    public static class SpecializedNestedBuilder extends NestedBuilder<SpecializedNestedObject, TestParentBuilder> {

        private final String name;
        private final String value;
        private final int specialProperty;

        public SpecializedNestedBuilder(TestParentBuilder parentBuilder, String name, String value,
                int specialProperty) {
            super(parentBuilder);
            this.name = name;
            this.value = value;
            this.specialProperty = specialProperty;
        }

        @Override
        public SpecializedNestedObject build() {
            return SpecializedNestedObject.builder()
                    .name(name)
                    .value(value)
                    .specialProperty(specialProperty)
                    .build();
        }

    }

    /**
     * DTO for nested builder data from Gherkin tables.
     */
    @Value
    @Builder
    public static class NestedBuilderData {

        String name;
        String value;

    }

}
