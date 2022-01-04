package graal.learning.simulation;

import graal.learning.models.TimedAutomaton;
import graal.learning.models.Trace;
import org.apache.commons.lang3.tuple.Pair;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;


public class ModelEvaluator {
    private static final String EVAL_DIRECTORY = "eval_traces/";
    private final int roundingFactor;

    public static class ModelQualityStatistics{
        private int nrNegatives;
        private int nrPositives;
        private int truePositives  = 0;
        private int trueNegatives  = 0;
        private int falsePositives = 0;
        private int falseNegatives = 0;
        private double  recall;
        private double  precision;
        private double  f1Score;

        public int getNrNegatives() {
            return nrNegatives;
        }

        public int getNrPositives() {
            return nrPositives;
        }

        public int getTruePositives() {
            return truePositives;
        }

        public int getTrueNegatives() {
            return trueNegatives;
        }

        public int getFalsePositives() {
            return falsePositives;
        }

        public int getFalseNegatives() {
            return falseNegatives;
        }

        public double getRecall() {
            return recall;
        }

        public double getPrecision() {
            return precision;
        }

        public double getF1Score() {
            return f1Score;
        }

        public ModelQualityStatistics(int nrPositives, int nrNegatives,
                                      int truePositives, int trueNegatives, int falsePositives, int falseNegatives) {
            this.nrPositives = nrPositives;
            this.nrNegatives = nrNegatives;
            this.truePositives = truePositives;
            this.trueNegatives = trueNegatives;
            this.falsePositives = falsePositives;
            this.falseNegatives = falseNegatives;
            this.recall = computeRecall();
            this.precision = computePrecision();
            this.f1Score = computeF1Score();

        }

        private double computeRecall() {
            // true positive rate
            return this.truePositives / (double)this.nrPositives;
        }
        private double computePrecision() {
            // positive predictive value
            return this.truePositives / (double) (truePositives + falsePositives);
        }
        private double computeF1Score(){
            return (2*truePositives) / (double) (2*truePositives + falsePositives + falseNegatives);
        }
        public void computeMetrics(){
            this.recall = computeRecall();
            this.precision = computePrecision();
            this.f1Score = computeF1Score();
        }
        @Override
        public String toString(){
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("Evaluation Statistics for %d pos. and %d neg. traces: ",
                    this.nrPositives, this.nrNegatives)).append(System.lineSeparator());
            sb.append(String.format("True Positives: %d",truePositives)).append(System.lineSeparator());
            sb.append(String.format("True Negatives: %d",trueNegatives)).append(System.lineSeparator());
            sb.append(String.format("False Positives: %d",falsePositives)).append(System.lineSeparator());
            sb.append(String.format("False Negatives: %d",falseNegatives)).append(System.lineSeparator());
            sb.append(String.format("Precision: %.2f",precision)).append(System.lineSeparator());
            sb.append(String.format("Recall: %.2f",recall)).append(System.lineSeparator());
            sb.append(String.format("F1: %.2f",f1Score)).append(System.lineSeparator());
            return sb.toString();
        }
    }
    private int maxDelay;
    private int maxTraceSteps;
    private Optional<String> traceOutFileName;
    private String uppaalExecutable;
    private long seed;
    private TimedAutomaton original = null;
    private String uppaalFileOriginal = null;
    private TimedAutomaton learned = null;
    private int nrEvalTraces = 0;

    public ModelEvaluator(int maxDelay, int maxTraceSteps, Optional<String> traceOutFileName,
                          String uppaalExecutable, long seed, TimedAutomaton original, 
                          String uppaalFileOriginal, TimedAutomaton learned, int nrEvalTraces, int roundingFactor) {
        this.maxDelay = maxDelay;
        this.maxTraceSteps = maxTraceSteps;
        this.traceOutFileName = traceOutFileName.map(name -> EVAL_DIRECTORY + "/" + name);
        this.uppaalExecutable = uppaalExecutable;
        this.seed = seed;
        this.original = original;
        this.uppaalFileOriginal = uppaalFileOriginal;
        this.learned = learned;
        this.nrEvalTraces = nrEvalTraces;
        this.roundingFactor = roundingFactor;
    }

    public ModelQualityStatistics evaluate() throws Exception {
        UppaalTraceGen traceGen = new UppaalTraceGen(uppaalFileOriginal, nrEvalTraces, new HashSet<>(original.getAlphabet()),
        maxDelay, maxTraceSteps, "eval_tmp/eval.q", traceOutFileName, uppaalExecutable, seed);
        Optional<Pair<String, String>> outfileNames = Optional.empty();
        Pair<List<Trace>, List<Trace>> posNegTrace = traceGen.generatePosAndNegTraces(original, nrEvalTraces, outfileNames);
        List<Trace> positiveTraces = posNegTrace.getLeft();
        List<Trace> negativeTraces = posNegTrace.getValue();
        ModelQualityStatistics qualityStatistics = performEvaluation(positiveTraces, negativeTraces, roundingFactor);
        qualityStatistics.computeMetrics();
        return qualityStatistics;
    }

    private ModelQualityStatistics performEvaluation(List<Trace> positiveTraces, List<Trace> negativeTraces,int roundingFactor) {
        int truePositives  = 0;
        int trueNegatives  = 0;
        int falsePositives = 0;
        int falseNegatives = 0;

        for (Trace posTrace : positiveTraces){
            if (learned.accepts(posTrace,roundingFactor)){
                truePositives ++;
            } else {
                falseNegatives ++;
            }
        }

        for (Trace negTrace : negativeTraces){
            if (learned.accepts(negTrace,roundingFactor)){
                falsePositives ++;
            } else {
                trueNegatives ++;
            }
        }
        return new ModelQualityStatistics(nrEvalTraces, nrEvalTraces,truePositives,trueNegatives,falsePositives, falseNegatives);
    }
}
