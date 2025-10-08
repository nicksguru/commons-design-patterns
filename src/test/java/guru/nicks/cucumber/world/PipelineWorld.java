package guru.nicks.cucumber.world;

import guru.nicks.designpattern.pipeline.Pipeline;

import io.cucumber.spring.ScenarioScope;
import lombok.Data;
import org.springframework.stereotype.Component;

/**
 * Domain-specific (not feature-specific) state shared between scenario steps. Thanks to
 * {@link ScenarioScope @ScenarioScope}, each scenario gets a fresh copy.
 */
@Component
@ScenarioScope
@Data
public class PipelineWorld {

    private Pipeline<?, ?, ?> pipeline;
    private String pipelineInput;

    private String step1Suffix;
    private PipelinePhase step1Phase;

    private String step2Suffix;
    private PipelinePhase step2Phase;

    private String step3Suffix;
    private PipelinePhase step3Phase;

    // null means chain didn't do anything
    private String pipelineOutput;

    public enum PipelinePhase {

        PHASE1,
        PHASE2,
        PHASE3

    }

}
