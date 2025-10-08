package guru.nicks.cucumber;

import guru.nicks.cucumber.world.IteratorWorld;
import guru.nicks.designpattern.iterator.ThreadSafeListOffsetIterator;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@RequiredArgsConstructor
public class ThreadSafeListOffsetIteratorSteps {

    // DI
    private final IteratorWorld iteratorWorld;

    @Given("list to iterate:")
    public void list_to_iterate(List<String> items) {
        iteratorWorld.setItems(items);
    }

    @When("iteration start index is {int}")
    public void iteration_start_index_is(int startIndex) {
        iteratorWorld.setStartIndex(startIndex);
    }

    @When("iteration finishes")
    public void iteration_finishes() {
        var result = new ArrayList<String>();

        for (var iterator =
                new ThreadSafeListOffsetIterator<>(iteratorWorld.getItems(), iteratorWorld.getStartIndex());
                iterator.hasNext(); ) {
            result.add(iterator.next());
        }

        iteratorWorld.setIterationResult(result);
    }

    @Then("iteration result should be:")
    public void iteration_result_is(List<String> items) {
        assertThat(iteratorWorld.getIterationResult())
                .as("iteration result")
                .isEqualTo(items);
    }

    @Then("iteration result should be the original list")
    public void iterationResultShouldBeTheOriginalList() {
        assertThat(iteratorWorld.getIterationResult())
                .as("iteration result")
                .isEqualTo(iteratorWorld.getItems());
    }

}
