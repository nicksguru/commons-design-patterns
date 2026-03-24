package guru.nicks.commons.benchmark;

import guru.nicks.commons.designpattern.pipeline.Pipeline;
import guru.nicks.commons.designpattern.pipeline.PipelineState;
import guru.nicks.commons.designpattern.pipeline.PipelineStep;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * JMH benchmark for Pipeline performance using a text processing business case.
 * <p>
 * Business case: A text normalization pipeline that processes raw user input by: 1. Trimming whitespace 2. Converting
 * to lowercase 3. Removing extra spaces 4. Removing special characters 5. Capitalizing first letter
 */
@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 2, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
@Threads(1)
public class PipelineBenchmark {

    private Pipeline<String, String, TextProcessingStep> smallPipeline;
    private Pipeline<String, String, TextProcessingStep> mediumPipeline;
    private Pipeline<String, String, TextProcessingStep> largePipeline;

    private String shortInput;
    private String mediumInput;
    private String longInput;

    @Setup
    public void setup() {
        // create pipelines with different numbers of steps
        smallPipeline = new TextProcessingPipeline(List.of(
                new TrimStep(),
                new LowercaseStep()
        ));

        mediumPipeline = new TextProcessingPipeline(List.of(
                new TrimStep(),
                new LowercaseStep(),
                new RemoveExtraSpacesStep(),
                new RemoveSpecialCharactersStep()
        ));

        largePipeline = new TextProcessingPipeline(List.of(
                new TrimStep(),
                new LowercaseStep(),
                new RemoveExtraSpacesStep(),
                new RemoveSpecialCharactersStep(),
                new CapitalizeFirstLetterStep(),
                new RemoveNumbersStep(),
                new NormalizeQuotesStep()
        ));

        // prepare test inputs
        shortInput = "  Hello World!  ";
        mediumInput = "  The   QUICK   Brown   FOX...   Jumps   OVER   123   Lazy   Dog's   \"Tail\"???  ";
        longInput = "  In   a   Distant   GALAXY...   There   LIVED   a   Young   PADAWAN   Named   'Luke'...   " +
                "He   DREAMED   of   123   STARS   and   ADVENTURES!!!   \"May   the   Force   be   with   you\"...   " +
                "The   EMPIRE   struck   BACK...   But   HOPE   remained...   The   REBELLION   grew   STRONGER...   " +
                "And   SO...   The   SAGA   continues...   TO   be   continued...   456   END   \"\"\"...   ";
    }

    @Benchmark
    public void benchmarkSmallPipelineWithShortInput(Blackhole bh) {
        PipelineState<String, String> result = smallPipeline.apply(shortInput);
        bh.consume(result.getOutput());
    }

    @Benchmark
    public void benchmarkSmallPipelineWithMediumInput(Blackhole bh) {
        PipelineState<String, String> result = smallPipeline.apply(mediumInput);
        bh.consume(result.getOutput());
    }

    @Benchmark
    public void benchmarkSmallPipelineWithLongInput(Blackhole bh) {
        PipelineState<String, String> result = smallPipeline.apply(longInput);
        bh.consume(result.getOutput());
    }

    @Benchmark
    public void benchmarkMediumPipelineWithShortInput(Blackhole bh) {
        PipelineState<String, String> result = mediumPipeline.apply(shortInput);
        bh.consume(result.getOutput());
    }

    @Benchmark
    public void benchmarkMediumPipelineWithMediumInput(Blackhole bh) {
        PipelineState<String, String> result = mediumPipeline.apply(mediumInput);
        bh.consume(result.getOutput());
    }

    @Benchmark
    public void benchmarkMediumPipelineWithLongInput(Blackhole bh) {
        PipelineState<String, String> result = mediumPipeline.apply(longInput);
        bh.consume(result.getOutput());
    }

    @Benchmark
    public void benchmarkLargePipelineWithShortInput(Blackhole bh) {
        PipelineState<String, String> result = largePipeline.apply(shortInput);
        bh.consume(result.getOutput());
    }

    @Benchmark
    public void benchmarkLargePipelineWithMediumInput(Blackhole bh) {
        PipelineState<String, String> result = largePipeline.apply(mediumInput);
        bh.consume(result.getOutput());
    }

    @Benchmark
    public void benchmarkLargePipelineWithLongInput(Blackhole bh) {
        PipelineState<String, String> result = largePipeline.apply(longInput);
        bh.consume(result.getOutput());
    }

    /**
     * Simple text processing pipeline implementation.
     */
    private static class TextProcessingPipeline extends Pipeline<String, String, TextProcessingStep> {
        public TextProcessingPipeline(List<TextProcessingStep> steps) {
            super(steps);
        }
    }

    /**
     * Base class for text processing steps.
     */
    @Getter
    @RequiredArgsConstructor
    private abstract static class TextProcessingStep extends PipelineStep<String, String> {

        private final String name;

        @Override
        public String toString() {
            return name;
        }

    }

    /**
     * Step that trims leading and trailing whitespace.
     */
    private static class TrimStep extends TextProcessingStep {

        public TrimStep() {
            super("Trim");
        }

        @Override
        public String apply(String input, String previousResult) {
            String text = previousResult != null ? previousResult : input;
            return text.trim();
        }

    }

    /**
     * Step that converts text to lowercase.
     */
    private static class LowercaseStep extends TextProcessingStep {

        public LowercaseStep() {
            super("Lowercase");
        }

        @Override
        public String apply(String input, String previousResult) {
            String text = previousResult != null ? previousResult : input;
            return text.toLowerCase();
        }

    }

    /**
     * Step that removes extra spaces (replaces multiple spaces with single space).
     */
    private static class RemoveExtraSpacesStep extends TextProcessingStep {

        public RemoveExtraSpacesStep() {
            super("RemoveExtraSpaces");
        }

        @Override
        public String apply(String input, String previousResult) {
            String text = previousResult != null ? previousResult : input;
            return text.replaceAll("\\s+", " ");
        }

    }

    /**
     * Step that removes special characters (keeps only alphanumeric and spaces).
     */
    private static class RemoveSpecialCharactersStep extends TextProcessingStep {

        public RemoveSpecialCharactersStep() {
            super("RemoveSpecialCharacters");
        }

        @Override
        public String apply(String input, String previousResult) {
            String text = previousResult != null ? previousResult : input;
            return text.replaceAll("[^a-zA-Z0-9\\s]", "");
        }

    }

    /**
     * Step that capitalizes the first letter of the text.
     */
    private static class CapitalizeFirstLetterStep extends TextProcessingStep {

        public CapitalizeFirstLetterStep() {
            super("CapitalizeFirstLetter");
        }

        @Override
        public String apply(String input, String previousResult) {
            String text = previousResult != null ? previousResult : input;
            if (text.isEmpty()) {
                return text;
            }
            return text.substring(0, 1).toUpperCase() + text.substring(1);
        }

    }

    /**
     * Step that removes all numbers from the text.
     */
    private static class RemoveNumbersStep extends TextProcessingStep {

        public RemoveNumbersStep() {
            super("RemoveNumbers");
        }

        @Override
        public String apply(String input, String previousResult) {
            String text = previousResult != null ? previousResult : input;
            return text.replaceAll("\\d+", "");
        }

    }

    /**
     * Step that normalizes quotes (replaces smart quotes with regular quotes).
     */
    private static class NormalizeQuotesStep extends TextProcessingStep {

        public NormalizeQuotesStep() {
            super("NormalizeQuotes");
        }

        @Override
        public String apply(String input, String previousResult) {
            String text = previousResult != null ? previousResult : input;
            return text
                    .replace("\u201C", "\"")
                    .replace("\u201D", "\"")
                    .replace("\u2019", "'");
        }

    }

}
