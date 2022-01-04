package graal.learning.smt.experiments;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

import java.util.function.Supplier;

public class ExperimentRunner {
    private String name;
    private String outputPath;
    private Supplier<Triple<Integer, Long, String> > experiment;

    public ExperimentRunner(String name, String outputPath, Supplier<Triple<Integer, Long, String>> experiment) {
        this.name = name;
        this.outputPath = outputPath;
        this.experiment = experiment;
    }

    public Triple<Integer, Long, String> runExperiment(){
        return experiment.get();
    }
}
