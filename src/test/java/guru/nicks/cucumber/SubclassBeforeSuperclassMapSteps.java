package guru.nicks.cucumber;

import guru.nicks.cucumber.world.TextWorld;
import guru.nicks.designpattern.SubclassBeforeSuperclassMap;

import io.cucumber.java.DataTableType;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.apache.commons.lang3.RandomUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * Step definitions for testing {@link SubclassBeforeSuperclassMap} functionality.
 */
@RequiredArgsConstructor
public class SubclassBeforeSuperclassMapSteps {

    // DI
    private final TextWorld textWorld;

    private SubclassBeforeSuperclassMap<Object, String> map;
    private Optional<Map.Entry<Class<?>, String>> foundEntry;

    @DataTableType
    public ClassEntry createClassEntry(Map<String, String> entry) {
        return ClassEntry.builder()
                .className(entry.get("class"))
                .value(entry.get("value"))
                .build();
    }

    @Given("an empty SubclassBeforeSuperclassMap")
    public void anEmptySubclassBeforeSuperclassMap() {
        map = new SubclassBeforeSuperclassMap<>();
    }

    @Given("a map with the following class entries:")
    public void aMapWithTheFollowingClassEntries(List<ClassEntry> entries) {
        map = new SubclassBeforeSuperclassMap<>();

        for (ClassEntry entry : entries) {
            Class<?> clazz = getClassByName(entry.getClassName());
            map.put(clazz, entry.getValue());
        }
    }

    @When("classes are added in the order: {word}, {word}, {word}, {word}")
    public void classesAreAddedInTheOrder(String class1, String class2, String class3, String class4) {
        textWorld.setLastException(catchThrowable(() -> {
            addClassToMap(class1, "value1");
            addClassToMap(class2, "value2");
            addClassToMap(class3, "value3");
            addClassToMap(class4, "value4");
        }));
    }

    @When("the closest superclass for {word} is found")
    public void theClosestSuperclassForClassIsFound(String className) {
        try {
            Class<?> clazz = getClassByName(className);
            foundEntry = map.findEntryForClosestSuperclass(clazz);
        } catch (Exception e) {
            textWorld.setLastException(e);
        }
    }

    @When("multiple threads simultaneously add and read from the map")
    public void multipleThreadsSimultaneouslyAddAndReadFromTheMap() {
        textWorld.setLastException(catchThrowable(() -> {
            int threadCount = 10;
            int operationsPerThread = 100;

            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch completionLatch = new CountDownLatch(threadCount);
            ExecutorService executorService = Executors.newFixedThreadPool(threadCount);

            for (int i = 0; i < threadCount; i++) {
                final int threadId = i;
                executorService.submit(() -> {
                    try {
                        // wait for all threads to be ready
                        startLatch.await();

                        for (int j = 0; j < operationsPerThread; j++) {
                            // mix of operations: reads, writes, and lookups
                            int operation = (j + threadId) % 3;

                            switch (operation) {
                                case 0 -> map.put(getRandomClass(), "Thread-" + threadId + "-" + j);
                                case 1 -> map.get(getRandomClass());
                                case 2 -> map.findEntryForClosestSuperclass(getRandomClass());
                                default -> throw new IllegalArgumentException();
                            }
                        }
                    } catch (Exception e) {
                        throw new RuntimeException("Thread " + threadId + " failed", e);
                    } finally {
                        completionLatch.countDown();
                    }
                });
            }

            // start all threads
            startLatch.countDown();

            boolean completed = completionLatch.await(10, TimeUnit.SECONDS);
            executorService.shutdown();

            if (!completed) {
                throw new RuntimeException("Concurrent test timed out");
            }
        }));
    }

    @Then("the map keys should be in the order: {word}, {word}, {word}, {word}")
    public void theMapKeysShouldBeInTheOrder(String class1, String class2, String class3, String class4) {
        assertThat(textWorld.getLastException())
                .as("lastException")
                .isNull();

        List<String> expectedOrder = List.of(class1, class2, class3, class4);
        List<String> actualOrder = map.keySet().stream()
                .map(Class::getSimpleName)
                .toList();

        assertThat(actualOrder)
                .as("map keys order")
                .containsExactlyElementsOf(expectedOrder);
    }

    @Then("the found entry should have key {word} and value {string}")
    public void theFoundEntryShouldHaveKeyAndValue(String className, String value) {
        assertThat(textWorld.getLastException())
                .as("lastException")
                .isNull();
        assertThat(foundEntry)
                .as("foundEntry")
                .isPresent();

        Class<?> expectedClass = getClassByName(className);
        assertThat(foundEntry.get().getKey())
                .as("foundEntry.key")
                .isEqualTo(expectedClass);
        assertThat(foundEntry.get().getValue())
                .as("foundEntry.value")
                .isEqualTo(value);
    }

    @Then("no entry should be found")
    public void noEntryShouldBeFound() {
        assertThat(textWorld.getLastException())
                .as("lastException")
                .isNull();
        assertThat(foundEntry)
                .as("foundEntry")
                .isEmpty();
    }

    @Then("no concurrency exceptions should occur")
    public void noConcurrencyExceptionsShouldOccur() {
        assertThat(textWorld.getLastException())
                .as("lastException")
                .isNull();
    }

    @Then("the map should maintain its integrity")
    public void theMapShouldMaintainItsIntegrity() {
        assertThat(textWorld.getLastException())
                .as("lastException")
                .isNull();

        // verify that subclasses still come before superclasses
        List<Class<?>> keyList = new ArrayList<>(map.keySet());

        for (int i = 0; i < keyList.size(); i++) {
            for (int j = i + 1; j < keyList.size(); j++) {
                // if j is a superclass of i, this is correct ordering
                if (keyList.get(j).isAssignableFrom(keyList.get(i))) {
                    continue;
                }

                // if 'i' is a superclass of 'j', this is incorrect ordering
                assertThat(keyList.get(i).isAssignableFrom(keyList.get(j)))
                        .as("Class %s should not be before its subclass %s",
                                keyList.get(i).getSimpleName(),
                                keyList.get(j).getSimpleName())
                        .isFalse();
            }
        }
    }

    private void addClassToMap(String className, String value) {
        Class<?> clazz = getClassByName(className);
        map.put(clazz, value);
    }

    private Class<?> getClassByName(String className) {
        try {
            return switch (className) {
                case "Object" -> Object.class;
                case "String" -> String.class;
                case "Number" -> Number.class;
                case "Integer" -> Integer.class;
                case "Boolean" -> Boolean.class;
                case "Long" -> Long.class;
                case "Double" -> Double.class;
                case "Character" -> Character.class;
                default -> Class.forName("java.lang." + className);
            };
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Class not found: " + className, e);
        }
    }

    private Class<?> getRandomClass() {
        Class<?>[] classes = {
                Object.class, String.class, Number.class, Integer.class,
                Boolean.class, Long.class, Double.class, Character.class
        };
        return classes[RandomUtils.insecure().randomInt(0, classes.length)];
    }

    @When("multiple threads concurrently put and get values")
    public void concurrentOperations() throws InterruptedException {
        int threadCount = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            int index = i;

            executor.submit(() -> {
                try {
                    map.put(String.class, "value" + index);
                    map.get(String.class);

                    map.put(CharSequence.class, "value" + index);
                    map.get(CharSequence.class);

                    // not in this Map
                    map.get(Integer.class);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();
    }

    @Then("map should maintain thread-safe consistency")
    public void verifyConsistency() {
        assertThat(map).hasSize(2);
        assertThat(map.get(String.class)).isNotNull();
    }

    /**
     * Data class for class entries in the map.
     */
    @Value
    @Builder
    public static class ClassEntry {

        String className;
        String value;

    }

}
