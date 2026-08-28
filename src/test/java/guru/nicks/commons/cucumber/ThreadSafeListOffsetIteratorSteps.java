package guru.nicks.commons.cucumber;

import guru.nicks.commons.cucumber.world.IteratorWorld;
import guru.nicks.commons.cucumber.world.TextWorld;
import guru.nicks.commons.designpattern.iterator.ThreadSafeListOffsetIterator;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

@RequiredArgsConstructor
public class ThreadSafeListOffsetIteratorSteps {

    // DI
    private final IteratorWorld iteratorWorld;
    private final TextWorld textWorld;

    private ThreadSafeListOffsetIterator<String> iterator;

    @Given("list to iterate:")
    public void listToIterate(List<String> items) {
        // mutable copy because some scenarios modify the list mid-iteration (Cucumber's DataTable list is immutable)
        iteratorWorld.setItems(new ArrayList<>(items));
    }

    @When("iteration start index is {int}")
    public void iterationStartIndexIs(int startIndex) {
        iteratorWorld.setStartIndex(startIndex);
    }

    @When("iteration finishes")
    public void iterationFinishes() {
        var result = new ArrayList<String>();

        for (var iter = new ThreadSafeListOffsetIterator<>(iteratorWorld.getItems(), iteratorWorld.getStartIndex());
                iter.hasNext(); ) {
            result.add(iter.next());
        }

        iteratorWorld.setIterationResult(result);
    }

    @When("a thread-safe list offset iterator is created")
    public void aThreadSafeListOffsetIteratorIsCreated() {
        iterator = new ThreadSafeListOffsetIterator<>(iteratorWorld.getItems(), iteratorWorld.getStartIndex());
    }

    @When("the iterator advances one item")
    public void theIteratorAdvancesOneItem() {
        iterator.next();
    }

    @When("the iterator is drained")
    public void theIteratorIsDrained() {
        while (iterator.hasNext()) {
            iterator.next();
        }
    }

    @When("the list becomes empty after hasNext is called")
    public void theListBecomesEmptyAfterHasNextIsCalled() {
        iterator = new ThreadSafeListOffsetIterator<>(iteratorWorld.getItems(), iteratorWorld.getStartIndex());
        assertThat(iterator.hasNext()).isTrue();

        // simulate a concurrent modification between the state check in hasNext() and the read in next()
        iteratorWorld.getItems().clear();
    }

    @When("multiple threads iterate while the list shrinks and grows back")
    public void multipleThreadsIterateWhileTheListShrinksAndGrowsBack() {
        textWorld.setLastException(catchThrowable(() -> {
            // CopyOnWriteArrayList because the iterator guards only its own state, not the underlying list;
            // items/startIndex are captured up front because the cucumber-glue scope is thread-bound
            var items = new CopyOnWriteArrayList<>(iteratorWorld.getItems());
            int startIndex = iteratorWorld.getStartIndex();
            var startLatch = new CountDownLatch(1);
            ExecutorService executorService = Executors.newFixedThreadPool(2);

            var iteration = executorService.submit(() -> {
                startLatch.await();
                var listOffsetIterator = new ThreadSafeListOffsetIterator<>(items, startIndex);

                while (listOffsetIterator.hasNext()) {
                    listOffsetIterator.next();
                }

                return null;
            });

            var shrinking = executorService.submit(() -> {
                startLatch.await();

                for (int i = 0; i < 100; i++) {
                    items.remove(items.size() - 1);
                    items.add("extra" + i);
                }

                return null;
            });

            startLatch.countDown();
            shrinking.get(10, TimeUnit.SECONDS);

            try {
                iteration.get(10, TimeUnit.SECONDS);
            } catch (ExecutionException e) {
                // NoSuchElementException is the contract-compliant way to end iteration on a shrinking list -
                // any other exception (especially IndexOutOfBoundsException) is a bug
                if (!(e.getCause() instanceof NoSuchElementException)) {
                    throw e;
                }
            } finally {
                executorService.shutdown();
            }
        }));
    }

    @Then("iteration result should be:")
    public void iterationResultIs(List<String> items) {
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

    @Then("iterator state should be {word} and current index should be {int}")
    public void iteratorStateShouldBeAndCurrentIndexShouldBe(String stateName, int currentIndex) {
        assertThat(iterator.getState())
                .as("iterator state")
                .isEqualTo(ThreadSafeListOffsetIterator.State.valueOf(stateName));

        assertThat(iterator.getCurrentIndex())
                .as("iterator current index")
                .isEqualTo(currentIndex);
    }

    @Then("iteration should fail with NoSuchElementException")
    public void iterationShouldFailWithNoSuchElementException() {
        assertThat(catchThrowable(iterator::next))
                .as("exception thrown by next() after the list emptied")
                .isInstanceOf(NoSuchElementException.class);
    }

    @Then("no IndexOutOfBoundsException should occur")
    public void noIndexOutOfBoundsExceptionShouldOccur() {
        assertThat(textWorld.getLastException())
                .as("lastException")
                .isNull();
    }

}
