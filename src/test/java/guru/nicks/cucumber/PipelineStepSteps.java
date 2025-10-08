package guru.nicks.cucumber;

import guru.nicks.cucumber.world.TextWorld;
import guru.nicks.designpattern.pipeline.PipelineStep;
import guru.nicks.designpattern.pipeline.PipelineStepFeature;

import io.cucumber.java.DataTableType;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Step definitions for testing {@link PipelineStep} functionality.
 */
@RequiredArgsConstructor
public class PipelineStepSteps {

    // DI
    private final TextWorld textWorld;

    private PipelineStep<String, String> pipelineStep;
    private String toStringResult;
    private Map<String, Object> featuresValues;
    private Map<String, Object> featuresValuesSecondCall;

    @DataTableType
    public FeatureData createFeatureData(Map<String, String> entry) {
        return FeatureData.builder()
                .name(entry.get("name"))
                .value(entry.get("value"))
                .build();
    }

    @Given("a pipeline step with duplicate feature names")
    public void aPipelineStepWithDuplicateFeatureNames() {
        pipelineStep = new DuplicateFeaturePipelineStep();
    }

    @Given("a pipeline step with iterable feature values")
    public void aPipelineStepWithIterableFeatureValues() {
        pipelineStep = new IterableFeaturePipelineStep();
    }

    @Given("a pipeline step with complex object feature values")
    public void aPipelineStepWithComplexObjectFeatureValues() {
        pipelineStep = new ComplexObjectFeaturePipelineStep();
    }

    @Given("a pipeline step with changing feature values")
    public void aPipelineStepWithChangingFeatureValues() {
        pipelineStep = new ChangingFeaturePipelineStep();
    }

    @Given("a pipeline step with no features")
    public void aPipelineStepWithNoFeatures() {
        pipelineStep = new NoFeaturePipelineStep();
    }

    @Given("a pipeline step with multiple alphabetical features:")
    public void aPipelineStepWithMultipleAlphabeticalFeatures(List<FeatureData> features) {
        pipelineStep = new AlphabeticalFeaturePipelineStep(features);
    }

    @When("I call toString on the pipeline step")
    public void iCallToStringOnThePipelineStep() {
        try {
            toStringResult = pipelineStep.toString();
        } catch (Exception e) {
            textWorld.setLastException(e);
        }
    }

    @When("I call getFeaturesValues on the pipeline step")
    public void iCallGetFeaturesValuesOnThePipelineStep() {
        try {
            featuresValues = pipelineStep.getFeaturesValues();
        } catch (Exception e) {
            textWorld.setLastException(e);
        }
    }

    @When("I call getFeaturesValues multiple times")
    public void iCallGetFeaturesValuesMultipleTimes() {
        try {
            featuresValues = pipelineStep.getFeaturesValues();

            // change the feature value after first call
            if (pipelineStep instanceof ChangingFeaturePipelineStep changingStep) {
                changingStep.incrementCounter();
            }

            featuresValuesSecondCall = pipelineStep.getFeaturesValues();
        } catch (Exception e) {
            textWorld.setLastException(e);
        }
    }

    @Then("the same map instance should not be returned each time")
    public void theSameMapInstanceShouldNotBeReturnedEachTime() {
        assertThat(textWorld.getLastException())
                .as("No exception should be thrown")
                .isNull();

        assertThat(featuresValuesSecondCall)
                .as("featuresValuesSecondCall")
                .isNotSameAs(featuresValues);
    }

    @Then("the result should contain combined feature values")
    public void theResultShouldContainCombinedFeatureValues() {
        assertThat(textWorld.getLastException())
                .as("No exception should be thrown")
                .isNull();

        assertThat(featuresValues)
                .as("featuresValues")
                .containsKey("duplicateFeature");

        Object value = featuresValues.get("duplicateFeature");
        assertThat(value)
                .as("duplicateFeature value")
                .isInstanceOf(Set.class);

        @SuppressWarnings("unchecked")
        Set<String> valueSet = (Set<String>) value;
        assertThat(valueSet)
                .as("duplicateFeature value set")
                .containsExactlyInAnyOrder("value1", "value2");
    }

    @Then("the result should contain all iterable values")
    public void theResultShouldContainAllIterableValues() {
        assertThat(textWorld.getLastException())
                .as("No exception should be thrown")
                .isNull();

        assertThat(featuresValues)
                .as("featuresValues")
                .containsKey("iterableFeature");

        Object value = featuresValues.get("iterableFeature");
        assertThat(value)
                .as("iterableFeature value")
                .isInstanceOf(List.class);

        @SuppressWarnings("unchecked")
        List<String> valueList = (List<String>) value;
        assertThat(valueList)
                .as("iterableFeature value list")
                .containsExactly("item1", "item2", "item3");
    }

    @Then("the result should contain the object's toString representation")
    public void theResultShouldContainTheObjectsToStringRepresentation() {
        assertThat(textWorld.getLastException())
                .as("No exception should be thrown")
                .isNull();

        assertThat(toStringResult)
                .as("toStringResult")
                .contains("complexFeature=ComplexObject[value=test]");
    }

    @Then("the feature values should not reflect the initial state")
    public void theFeatureValuesShouldReflectTheInitialState() {
        assertThat(textWorld.getLastException())
                .as("No exception should be thrown")
                .isNull();

        assertThat(featuresValues)
                .as("featuresValues")
                .containsEntry("changingFeature", "value-0");

        assertThat(featuresValuesSecondCall)
                .as("featuresValuesSecondCall")
                .containsEntry("changingFeature", "value-1");
    }

    @Then("the result should not contain {string}")
    public void theResultShouldNotContain(String unexpectedContent) {
        assertThat(textWorld.getLastException())
                .as("No exception should be thrown")
                .isNull();

        assertThat(toStringResult)
                .as("toStringResult")
                .doesNotContain(unexpectedContent);
    }

    @Then("the features should appear in alphabetical order")
    public void theFeaturesShouldAppearInAlphabeticalOrder() {
        assertThat(textWorld.getLastException())
                .as("No exception should be thrown")
                .isNull();

        // extract the features part from the toString result
        Pattern pattern = Pattern.compile("\\{(.+)\\}");
        Matcher matcher = pattern.matcher(toStringResult);

        assertThat(matcher.find())
                .as("Features part should be found in toString result")
                .isTrue();

        String featuresString = matcher.group(1);
        assertThat(featuresString)
                .as("featuresString")
                .matches("apple=a, monkey=m, zebra=z");
    }

    /**
     * Implementation of PipelineStep with duplicate feature names.
     */
    public static class DuplicateFeaturePipelineStep extends PipelineStep<String, String> {

        @Override
        public String apply(String input, String output) {
            return input;
        }

        @PipelineStepFeature(name = "duplicateFeature")
        public String mustFirstDuplicateFeature() {
            return "value1";
        }

        @PipelineStepFeature(name = "duplicateFeature")
        public String wantSecondDuplicateFeature() {
            return "value2";
        }

    }

    /**
     * Implementation of PipelineStep with iterable feature values.
     */
    public static class IterableFeaturePipelineStep extends PipelineStep<String, String> {

        @Override
        public String apply(String input, String output) {
            return input;
        }

        @PipelineStepFeature
        public List<String> getIterableFeature() {
            return Arrays.asList("item1", "item2", "item3");
        }

    }

    /**
     * Implementation of PipelineStep with complex object feature values.
     */
    public static class ComplexObjectFeaturePipelineStep extends PipelineStep<String, String> {

        @Override
        public String apply(String input, String output) {
            return input;
        }

        @PipelineStepFeature
        public ComplexObject getComplexFeature() {
            return ComplexObject.builder()
                    .value("test")
                    .build();
        }

    }

    /**
     * Implementation of PipelineStep with changing feature values - to check that the first value becomes cached.
     */
    public static class ChangingFeaturePipelineStep extends PipelineStep<String, String> {

        private final AtomicInteger counter = new AtomicInteger(0);

        @Override
        public String apply(String input, String output) {
            return input;
        }

        @PipelineStepFeature
        public String getChangingFeature() {
            return "value-" + counter.get();
        }

        public void incrementCounter() {
            counter.incrementAndGet();
        }

    }

    /**
     * Implementation of PipelineStep with no features.
     */
    public static class NoFeaturePipelineStep extends PipelineStep<String, String> {

        @Override
        public String apply(String input, String output) {
            return input;
        }

    }

    /**
     * Implementation of PipelineStep with alphabetically ordered features.
     * <p>
     * Feature getters are private on purpose: @PipelineStepFeature processor must be able to access them anyway.
     */
    @RequiredArgsConstructor
    public static class AlphabeticalFeaturePipelineStep extends PipelineStep<String, String> {

        private final List<FeatureData> features;

        @Override
        public String apply(String input, String output) {
            return input;
        }

        @PipelineStepFeature(name = "apple")
        public String getAppleFeature() {
            return features.stream()
                    .filter(f -> f.getName().equals("apple"))
                    .findFirst()
                    .map(FeatureData::getValue)
                    .orElse(null);
        }

        @PipelineStepFeature(name = "monkey")
        public String hasMonkeyFeature() {
            return features.stream()
                    .filter(f -> f.getName().equals("monkey"))
                    .findFirst()
                    .map(FeatureData::getValue)
                    .orElse(null);
        }

        @PipelineStepFeature(name = "zebra")
        public String canZebraFeature() {
            return features.stream()
                    .filter(f -> f.getName().equals("zebra"))
                    .findFirst()
                    .map(FeatureData::getValue)
                    .orElse(null);
        }

    }

    /**
     * Complex object for testing toString representation.
     */
    @Value
    @Builder
    public static class ComplexObject {

        String value;

        @Override
        public String toString() {
            return "ComplexObject[value=" + value + "]";
        }

    }

    /**
     * Data class for feature data.
     */
    @Value
    @Builder
    public static class FeatureData {

        String name;
        String value;

    }

}
