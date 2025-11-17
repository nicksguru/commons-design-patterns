package guru.nicks.commons.cucumber;

import guru.nicks.commons.cucumber.world.TextWorld;
import guru.nicks.commons.designpattern.iterator.ThreadSafeInfiniteIterator;

import io.cucumber.java.DataTableType;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * Step definitions for testing {@link ThreadSafeInfiniteIterator} concurrent functionality.
 */
@RequiredArgsConstructor
public class ThreadSafeInfiniteIteratorSteps {

    // DI
    private final TextWorld textWorld;

    private final List<String> retrievedItems = new CopyOnWriteArrayList<>();
    private final Map<String, AtomicInteger> itemCounts = new ConcurrentHashMap<>();
    private final AtomicBoolean exceptionOccurred = new AtomicBoolean(false);

    private List<String> sourceItems;
    private List<String> modifiableSourceItems;
    private ThreadSafeInfiniteIterator<String> iterator;

    @DataTableType
    public ItemData createItemData(Map<String, String> entry) {
        return ItemData.builder()
                .value(entry.get("value"))
                .build();
    }

    @Given("a thread-safe infinite iterator with items:")
    public void aThreadSafeInfiniteIteratorWithItems(List<ItemData> items) {
        sourceItems = items.stream()
                .map(ItemData::getValue)
                .toList();
        try {
            iterator = new ThreadSafeInfiniteIterator<>(sourceItems);
        } catch (Exception e) {
            textWorld.setLastException(e);
        }
    }

    @Given("a thread-safe infinite iterator with a dynamic source")
    public void aThreadSafeInfiniteIteratorWithADynamicSource() {
        modifiableSourceItems = new CopyOnWriteArrayList<>();
        modifiableSourceItems.add("initial1");
        modifiableSourceItems.add("initial2");

        try {
            iterator = new ThreadSafeInfiniteIterator<>(modifiableSourceItems);
        } catch (Exception e) {
            textWorld.setLastException(e);
        }
    }

    @Given("a thread-safe infinite iterator with a modifiable source")
    public void aThreadSafeInfiniteIteratorWithAModifiableSource() {
        modifiableSourceItems = new CopyOnWriteArrayList<>();
        modifiableSourceItems.add("item1");
        modifiableSourceItems.add("item2");

        try {
            iterator = new ThreadSafeInfiniteIterator<>(modifiableSourceItems);
        } catch (Exception e) {
            textWorld.setLastException(e);
        }
    }

    @When("multiple threads read and modify the iterator concurrently")
    public void multipleThreadsReadAndModifyTheIteratorConcurrently() {
        textWorld.setLastException(catchThrowable(() -> {
            int threadCount = 10;
            ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
            CountDownLatch startLatch = new CountDownLatch(1);
            CyclicBarrier barrier = new CyclicBarrier(threadCount);
            List<Future<?>> futures = new ArrayList<>();

            for (int i = 0; i < threadCount; i++) {
                final int threadId = i;
                futures.add(executorService.submit(() -> {
                    try {
                        // wait for all threads to be ready
                        startLatch.await();

                        for (int j = 0; j < 100; j++) {
                            // modify source list occasionally
                            if ((threadId % 3 == 0) && (j % 10 == 0)) {
                                modifiableSourceItems.add("dynamic" + threadId + "-" + j);

                                if (!modifiableSourceItems.isEmpty() && (j % 20) == 0) {
                                    modifiableSourceItems.removeFirst();
                                }
                            }
                            // read from iterator
                            else {
                                String item = iterator.next();
                                retrievedItems.add(item);
                                itemCounts.computeIfAbsent(item, k -> new AtomicInteger()).incrementAndGet();
                            }

                            // synchronize threads occasionally
                            if (j % 25 == 0) {
                                barrier.await(1, TimeUnit.SECONDS);
                            }
                        }

                        return null;
                    } catch (Exception e) {
                        exceptionOccurred.set(true);
                        throw new RuntimeException(e);
                    }
                }));
            }

            // start all threads at once
            startLatch.countDown();

            // wait for completion
            for (Future<?> future : futures) {
                future.get(30, TimeUnit.SECONDS);
            }

            executorService.shutdown();
            executorService.awaitTermination(5, TimeUnit.SECONDS);
        }));
    }

    @When("the source is modified during iteration")
    public void theSourceIsModifiedDuringIteration() {
        textWorld.setLastException(catchThrowable(() -> {
            // first get some items
            for (int i = 0; i < 5; i++) {
                retrievedItems.add(iterator.next());
            }

            // modify the source
            modifiableSourceItems.add("newItem1");
            modifiableSourceItems.add("newItem2");
            modifiableSourceItems.remove("item1");

            // continue iteration to force a reset
            for (int i = 0; i < 10; i++) {
                retrievedItems.add(iterator.next());
            }
        }));
    }

    @When("high concurrency load is applied")
    public void highConcurrencyLoadIsApplied() {
        textWorld.setLastException(catchThrowable(() -> {
            int threadCount = 50;
            int iterationsPerThread = 1000;

            ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
            CountDownLatch startLatch = new CountDownLatch(1);
            List<Future<?>> futures = new ArrayList<>();

            for (int i = 0; i < threadCount; i++) {
                futures.add(executorService.submit(() -> {
                    try {
                        // wait for all threads to be ready
                        startLatch.await();

                        for (int j = 0; j < iterationsPerThread; j++) {
                            String item = iterator.next();
                            itemCounts.computeIfAbsent(item, k -> new AtomicInteger()).incrementAndGet();
                        }

                        return null;
                    } catch (Exception e) {
                        exceptionOccurred.set(true);
                        throw new RuntimeException(e);
                    }
                }));
            }

            // start all threads at once
            startLatch.countDown();

            // wait for completion
            for (Future<?> future : futures) {
                future.get(60, TimeUnit.SECONDS);
            }

            executorService.shutdown();
            executorService.awaitTermination(5, TimeUnit.SECONDS);
        }));
    }

    @When("'hasNext' and 'next' are called concurrently")
    public void hasNextAndNextAreCalledConcurrently() {
        textWorld.setLastException(catchThrowable(() -> {
            int threadCount = 20;
            ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
            CountDownLatch startLatch = new CountDownLatch(1);
            List<Future<?>> futures = new ArrayList<>();

            for (int i = 0; i < threadCount; i++) {
                final int threadId = i;

                futures.add(executorService.submit(() -> {
                    try {
                        // wait for all threads to be ready
                        startLatch.await();

                        for (int j = 0; j < 100; j++) {
                            // call hasNext()
                            if (threadId % 2 == 0) {
                                boolean hasNext = iterator.hasNext();

                                // sometimes follow with next()
                                if (hasNext && (j % 3) == 0) {
                                    String item = iterator.next();
                                    retrievedItems.add(item);
                                }
                            }
                            // call next() directly
                            else {
                                String item = iterator.next();
                                retrievedItems.add(item);
                            }
                        }

                        return null;
                    } catch (Exception e) {
                        exceptionOccurred.set(true);
                        throw new RuntimeException(e);
                    }
                }));
            }

            // start all threads at once
            startLatch.countDown();

            // wait for completion
            for (Future<?> future : futures) {
                future.get(30, TimeUnit.SECONDS);
            }

            executorService.shutdown();
            executorService.awaitTermination(5, TimeUnit.SECONDS);
        }));
    }

    @Then("all operations should complete without exceptions")
    public void allOperationsShouldCompleteWithoutExceptions() {
        assertThat(textWorld.getLastException())
                .as("lastException")
                .isNull();
        assertThat(exceptionOccurred.get())
                .as("exceptionOccurred")
                .isFalse();
    }

    @Then("the iterator should remain in a consistent state")
    public void theIteratorShouldRemainInAConsistentState() {
        assertThat(textWorld.getLastException())
                .as("lastException")
                .isNull();

        // verify we can still use the iterator
        Throwable postOperationException = catchThrowable(() -> {
            for (int i = 0; i < 10; i++) {
                String item = iterator.next();
                assertThat(item)
                        .as("item")
                        .isNotNull();
            }
        });

        assertThat(postOperationException)
                .as("exception after concurrent operations")
                .isNull();
    }

    @Then("the iterator should reflect the changes after reset")
    public void theIteratorShouldReflectTheChangesAfterReset() {
        assertThat(textWorld.getLastException())
                .as("lastException")
                .isNull();

        // check that new items are present in the retrieved items
        boolean foundNewItems = retrievedItems.stream()
                .anyMatch(item -> item.equals("newItem1") || item.equals("newItem2"));
        assertThat(foundNewItems)
                .as("foundNewItems")
                .isTrue();

        // check that removed items are no longer returned after a full cycle
        int fullCycleCount = modifiableSourceItems.size() * 2;
        List<String> latestItems = new ArrayList<>();

        for (int i = 0; i < fullCycleCount; i++) {
            latestItems.add(iterator.next());
        }

        assertThat(latestItems)
                .as("latestItems")
                .doesNotContain("item1");
    }

    @Then("all threads should receive valid items")
    public void allThreadsShouldReceiveValidItems() {
        assertThat(textWorld.getLastException())
                .as("lastException")
                .isNull();

        // check that all retrieved items are from the source list
        for (String key : itemCounts.keySet()) {
            assertThat(sourceItems)
                    .as("sourceItems")
                    .contains(key);
        }

        // check that we have a roughly even distribution
        int totalItems = itemCounts.values().stream()
                .mapToInt(AtomicInteger::get)
                .sum();

        int expectedPerItem = totalItems / sourceItems.size();
        int allowedVariance = expectedPerItem / 2; // Allow 50% variance

        for (String sourceItem : sourceItems) {
            AtomicInteger count = itemCounts.get(sourceItem);
            assertThat(count)
                    .as("count for " + sourceItem)
                    .isNotNull();

            int actualCount = count.get();
            assertThat(actualCount)
                    .as("count for %s", sourceItem)
                    .isGreaterThanOrEqualTo(expectedPerItem - allowedVariance)
                    .isLessThanOrEqualTo(expectedPerItem + allowedVariance);
        }
    }

    @Then("all retrieved items should be valid")
    public void allRetrievedItemsShouldBeValid() {
        assertThat(textWorld.getLastException())
                .as("lastException")
                .isNull();

        // Check that all retrieved items are from the source list
        for (String item : retrievedItems) {
            assertThat(sourceItems)
                    .as("sourceItems")
                    .contains(item);
        }
    }

    /**
     * Data class for test items.
     */
    @Value
    @Builder
    public static class ItemData {

        String value;

    }

}
