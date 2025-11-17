package guru.nicks.commons.cucumber;

import guru.nicks.commons.cucumber.world.ChainOfResponsibilityWorld;
import guru.nicks.commons.cucumber.world.TextWorld;
import guru.nicks.commons.designpattern.ChainOfResponsibility;
import guru.nicks.commons.designpattern.pipeline.PipelineStep;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import org.junit.platform.commons.util.StringUtils;

import java.util.Collection;
import java.util.List;

@RequiredArgsConstructor
public class ChainOfResponsibilitySteps {

    // DI
    private final ChainOfResponsibilityWorld chainOfResponsibilityWorld;
    private final TextWorld textWorld;

    @Given("first chain step returns {string}")
    public void firstChainStepReturns(String value) {
        // '' from text table means null
        if (StringUtils.isBlank(value)) {
            value = null;
        }

        chainOfResponsibilityWorld.setResultFromStep1(value);
    }

    @Given("second chain step returns {string}")
    public void secondChainStepReturns(String value) {
        // '' from text table means null
        if (StringUtils.isBlank(value)) {
            value = null;
        }

        chainOfResponsibilityWorld.setResultFromStep2(value);
    }

    @Given("third chain step returns {string}")
    public void thirdChainStepReturns(String value) {
        // '' from text table means null
        if (StringUtils.isBlank(value)) {
            value = null;
        }

        chainOfResponsibilityWorld.setResultFromStep3(value);
    }

    @When("chain is run")
    public void chainIsRun() {
        String result = getResult();
        textWorld.setOutput(result);
    }

    @Nullable
    private String getResult() {
        PipelineStep<String, String> step1 = new PipelineStep<>() {
            @Override
            public String apply(String s, String s2) {
                return chainOfResponsibilityWorld.getResultFromStep1();
            }
        };

        PipelineStep<String, String> step2 = new PipelineStep<>() {
            @Override
            public String apply(String s, String s2) {
                return chainOfResponsibilityWorld.getResultFromStep2();
            }
        };

        PipelineStep<String, String> step3 = new PipelineStep<>() {
            @Override
            public String apply(String s, String s2) {
                return chainOfResponsibilityWorld.getResultFromStep3();
            }
        };

        TestChain chain = new TestChain(List.of(step1, step2, step3));

        return chain
                .apply(textWorld.getInput())
                .getOutput();
    }

    public static class TestChain extends ChainOfResponsibility<String, String, PipelineStep<String, String>> {

        public TestChain(Collection<PipelineStep<String, String>> steps) {
            super(steps);
        }

    }

}
