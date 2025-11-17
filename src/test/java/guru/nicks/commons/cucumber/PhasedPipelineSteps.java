package guru.nicks.commons.cucumber;

import guru.nicks.commons.cucumber.world.PipelineWorld;
import guru.nicks.commons.designpattern.pipeline.PhasedPipeline;
import guru.nicks.commons.designpattern.pipeline.PipelineStepFeature;

import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.junit.platform.commons.util.StringUtils;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

@RequiredArgsConstructor
public class PhasedPipelineSteps {

    // DI
    private final PipelineWorld pipelineWorld;

    @Given("pipeline input is {string}")
    public void pipelineInputIs(String value) {
        pipelineWorld.setPipelineInput(value);
    }

    @Given("first pipeline step appends {string}")
    public void firstPipelineStepAppends(String value) {
        pipelineWorld.setStep1Suffix(value);
    }

    @Given("first pipeline step phase is {string}")
    public void firstPipelineStepPhaseIs(String value) {
        pipelineWorld.setStep1Phase(StringUtils.isBlank(value)
                ? null
                : PipelineWorld.PipelinePhase.valueOf(value));
    }

    @Given("second pipeline step appends {string}")
    public void secondPipelineStepAppends(String value) {
        pipelineWorld.setStep2Suffix(value);
    }

    @Given("second pipeline step phase is {string}")
    public void secondPipelineStepPhaseIs(String value) {
        pipelineWorld.setStep2Phase(StringUtils.isBlank(value)
                ? null
                : PipelineWorld.PipelinePhase.valueOf(value));
    }

    @Given("third pipeline step appends {string}")
    public void thirdPipelineStepAppends(String value) {
        pipelineWorld.setStep3Suffix(value);
    }

    @Given("third pipeline step phase is {string}")
    public void thirdPipelineStepPhaseIs(String value) {
        pipelineWorld.setStep3Phase(StringUtils.isBlank(value)
                ? null
                : PipelineWorld.PipelinePhase.valueOf(value));
    }

    @When("pipeline is run")
    public void pipelineIsRun() {
        var step1 = new PhasedPipelineTest.Step(pipelineWorld.getStep1Suffix(), pipelineWorld.getStep1Phase());
        var step2 = new PhasedPipelineTest.Step(pipelineWorld.getStep2Suffix(), pipelineWorld.getStep2Phase());
        var step3 = new PhasedPipelineTest.Step(pipelineWorld.getStep3Suffix(), pipelineWorld.getStep3Phase());
        var pipeline = new PhasedPipelineTest(step1, step2, step3);
        pipelineWorld.setPipeline(pipeline);

        String result = pipeline
                .apply(pipelineWorld.getPipelineInput())
                .getOutput();
        pipelineWorld.setPipelineOutput(result);
    }

    @Then("pipeline output should be {string}")
    public void pipelineOutputIs(String expectedValue) {
        assertThat(pipelineWorld.getPipelineOutput()).isEqualTo(expectedValue);
    }

    @And("pipeline as string should contain {string}")
    public void pipelineAsStringShouldContain(String str) {
        assertThat(pipelineWorld.getPipeline().toString()).contains(str);
    }

    public static class PhasedPipelineTest
            extends PhasedPipeline<String, String, PipelineWorld.PipelinePhase, PhasedPipelineTest.Step> {

        public PhasedPipelineTest(Step... steps) {
            super(Arrays.asList(steps));
        }

        @Nonnull
        @Override
        protected PipelineWorld.PipelinePhase[] getPhases() {
            return PipelineWorld.PipelinePhase.values();
        }

        /**
         * Step which appends a fixed value to its argument.
         */
        @RequiredArgsConstructor
        public static class Step extends PhasedPipeline.Step<String, String, PipelineWorld.PipelinePhase> {

            private final String suffix;

            @Getter(onMethod_ = @Override)
            private final PipelineWorld.PipelinePhase phase;

            @PipelineStepFeature
            public boolean isTestFeature() {
                return true;
            }

            @PipelineStepFeature
            public String getDummyFeature() {
                return "test";
            }

            @Override
            public String apply(String pipelineInput, @Nullable String accumulator) {
                return (accumulator == null)
                        ? pipelineInput + suffix
                        : accumulator + suffix;
            }

        }

    }

}
