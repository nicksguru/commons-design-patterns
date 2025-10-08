package guru.nicks.cucumber.world;

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
public class ChainOfResponsibilityWorld {

    private String resultFromStep1;
    private String resultFromStep2;
    private String resultFromStep3;

}
