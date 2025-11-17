package guru.nicks.commons.cucumber;

import guru.nicks.commons.cucumber.world.TextWorld;
import guru.nicks.commons.designpattern.pipeline.Pipeline;
import guru.nicks.commons.designpattern.pipeline.PipelineState;
import guru.nicks.commons.designpattern.pipeline.PipelineStep;

import io.cucumber.java.DataTableType;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import jakarta.annotation.Nullable;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

@RequiredArgsConstructor
public class PipelineSteps {

    // DI
    private final TextWorld textWorld;

    private final List<String> loggedSteps = new ArrayList<>();
    private TestPipeline pipeline;
    private PipelineState<String, String> pipelineState;
    private String pipelineToString;

    @DataTableType
    public StepDefinition createStepDefinition(Map<String, String> entry) {
        return StepDefinition.builder()
                .stepName(entry.get("stepName"))
                .outputValue(entry.get("outputValue"))
                .build();
    }

    @DataTableType
    public StepOrder createStepOrder(Map<String, String> entry) {
        return StepOrder.builder()
                .stepName(entry.get("stepName"))
                .build();
    }

    @Given("a pipeline with the following steps:")
    public void aPipelineWithTheFollowingSteps(List<StepDefinition> stepDefinitions) {
        List<TestStep> steps = stepDefinitions.stream()
                .map(def -> new TestStep(def.getStepName(), def.getOutputValue()))
                .toList();

        pipeline = new TestPipeline(steps);
    }

    @Given("a pipeline with custom iterator that reverses steps:")
    public void aPipelineWithCustomIteratorThatReversesSteps(List<StepDefinition> stepDefinitions) {
        List<TestStep> steps = stepDefinitions.stream()
                .map(def -> new TestStep(def.getStepName(), def.getOutputValue()))
                .toList();

        pipeline = new ReversePipeline(steps);
    }

    @Given("a custom step runner that logs step execution")
    public void aCustomStepRunnerThatLogsStepExecution() {
        loggedSteps.clear();
        pipeline.setStepRunner((input, previousOutput, step) -> {
            String output = step.apply(input, previousOutput);
            loggedSteps.add(step.toString());
            return output;
        });
    }

    @Given("the pipeline is configured to stop after the second step")
    public void thePipelineIsConfiguredToStopAfterTheSecondStep() {
        pipeline.setShouldStopAfterSecondStep(true);
    }

    @Given("a pipeline with a step that throws an exception")
    public void aPipelineWithAStepThatThrowsAnException() {
        List<TestStep> steps = new ArrayList<>();
        steps.add(new TestStep("FirstStep", "First"));
        steps.add(new TestStep("ExceptionStep", null) {
            @Override
            public String apply(String input, String output) {
                throw new RuntimeException("Step failed");
            }
        });

        steps.add(new TestStep("ThirdStep", "Third"));
        pipeline = new TestPipeline(steps);
    }

    @When("the pipeline is executed with input {string}")
    public void thePipelineIsExecutedWithInput(String input) {
        try {
            pipelineState = pipeline.apply(input);
        } catch (Exception e) {
            textWorld.setLastException(e);
        }
    }

    @When("the pipeline is executed with null input")
    public void thePipelineIsExecutedWithNullInput() {
        try {
            pipelineState = pipeline.apply(null);
        } catch (Exception e) {
            textWorld.setLastException(e);
        }
    }

    @When("the pipeline toString method is called")
    public void thePipelineToStringMethodIsCalled() {
        pipelineToString = pipeline.toString();
    }

    @Then("the pipeline output should be {string}")
    public void thePipelineOutputShouldBe(String expectedOutput) {
        assertThat(pipelineState.getOutput())
                .as("pipelineState.getOutput()")
                .isEqualTo(expectedOutput);
    }

    @Then("the pipeline input should be null")
    public void thePipelineInputShouldBeNull() {
        assertThat(pipelineState.getInput())
                .as("pipelineState.getInput()")
                .isNull();
    }

    @Then("the pipeline should have executed {int} steps")
    public void thePipelineShouldHaveExecutedSteps(int expectedStepCount) {
        assertThat(pipelineState.getExecutedStepCount())
                .as("pipelineState.getExecutedStepCount()")
                .isEqualTo(expectedStepCount);
    }

    @Then("the step durations should be recorded")
    public void theStepDurationsShouldBeRecorded() {
        List<Pair<String, Long>> stepDurations = pipelineState.getStepDurations();
        assertThat(stepDurations)
                .as("stepDurations")
                .isNotEmpty();

        for (Pair<String, Long> duration : stepDurations) {
            assertThat(duration.getLeft())
                    .as("duration.getLeft()")
                    .isNotEmpty();
            assertThat(duration.getRight())
                    .as("duration.getRight()")
                    .isNotNegative();
        }
    }

    @Then("the steps should have been executed in order:")
    public void theStepsShouldHaveBeenExecutedInOrder(List<StepOrder> expectedStepOrders) {
        List<String> expectedStepNames = expectedStepOrders.stream()
                .map(StepOrder::getStepName)
                .toList();

        List<String> actualStepNames = pipelineState.getStepDurations().stream()
                .map(Pair::getLeft)
                .map(name -> {
                    int lastDot = name.lastIndexOf('.');
                    return lastDot >= 0 ? name.substring(lastDot + 1) : name;
                })
                .toList();

        assertThat(actualStepNames)
                .as("actualStepNames")
                .containsExactlyElementsOf(expectedStepNames);
    }

    @Then("each step should have been logged")
    public void eachStepShouldHaveBeenLogged() {
        assertThat(loggedSteps)
                .as("loggedSteps")
                .hasSize(3);
        assertThat(loggedSteps)
                .as("loggedSteps")
                .allSatisfy(step -> assertThat(step).as("step").isNotEmpty());
    }

    @Then("the result should contain all step names")
    public void theResultShouldContainAllStepNames() {
        assertThat(pipelineToString)
                .as("pipelineToString")
                .isNotNull();
        assertThat(pipelineToString)
                .as("pipelineToString")
                .contains("TestPipeline");
        assertThat(pipelineToString)
                .as("pipelineToString")
                .contains("FirstStep");
        assertThat(pipelineToString)
                .as("pipelineToString")
                .contains("SecondStep");
        assertThat(pipelineToString)
                .as("pipelineToString")
                .contains("ThirdStep");
    }

    /**
     * Test implementation of Pipeline for testing purposes
     */
    private static class TestPipeline extends Pipeline<String, String, TestStep> {

        private final AtomicBoolean shouldStopAfterSecondStep = new AtomicBoolean(false);

        public TestPipeline(Collection<? extends TestStep> steps) {
            super(steps);
        }

        public void setShouldStopAfterSecondStep(boolean shouldStop) {
            shouldStopAfterSecondStep.set(shouldStop);
        }

        @Override
        protected boolean shouldStop(PipelineState<String, String> pipelineState, TestStep previousStep,
                @Nullable TestStep nextStep) {
            return shouldStopAfterSecondStep.get()
                    && (previousStep != null)
                    && previousStep.toString().contains("SecondStep");
        }

    }

    /**
     * Test implementation of Pipeline with reversed step order.
     */
    private static class ReversePipeline extends TestPipeline {

        public ReversePipeline(Collection<? extends TestStep> steps) {
            super(steps);
        }

        @Override
        public Iterator<TestStep> iterator() {
            List<TestStep> reversedSteps = new ArrayList<>(getSteps());
            Collections.reverse(reversedSteps);
            return reversedSteps.iterator();
        }

    }

    /**
     * Test implementation of PipelineStep.
     */
    @Data
    @EqualsAndHashCode(callSuper = true)
    private static class TestStep extends PipelineStep<String, String> {

        private final String name;
        private final String outputValue;

        @Override
        public String apply(String input, String output) {
            return outputValue;
        }

        @Override
        public String toString() {
            return name;
        }

    }

    /**
     * DTO for step definitions from Gherkin tables.
     */
    @Value
    @Builder
    public static class StepDefinition {

        String stepName;
        String outputValue;

    }

    /**
     * DTO for step order from Gherkin tables
     */
    @Value
    @Builder
    public static class StepOrder {

        String stepName;

    }

}
