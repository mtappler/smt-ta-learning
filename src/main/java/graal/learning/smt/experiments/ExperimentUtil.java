package graal.learning.smt.experiments;

import org.sosy_lab.java_smt.SolverContextFactory;

import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;

public class ExperimentUtil {

    public static final int EXP_NAME_LEN = 14;
    public static final int SEC_VAL_LEN = 15;
    public static final int MIN_VAL_LEN = 10;

    public static double median(List<Double> values) {
        if(values.size() % 2 == 1){
            return values.get((values.size() + 1)/2 - 1); // - 1 for zero based indexes
        } else {
            int belowMedIndex = values.size() / 2 - 1;
            int aboveMedIndex = belowMedIndex + 1;
            return (values.get(belowMedIndex) + values.get(aboveMedIndex)) / 2;
        }
    }

    public static String createConfigString(boolean bfsMode, int batchSize, int nrStates, int nrTrans, int nrTraces,
                                            int kUrgency, int maxGuard, SolverContextFactory.Solvers solver,
                                            LearningExperimentProcedures.LearningMode mode, int roundingFactor) {
        return String.format("BFS: %b %s", bfsMode, System.lineSeparator()) +
                String.format("Batch size: %d %s", batchSize, System.lineSeparator()) +
                String.format("Location Bound: %d %s", nrStates, System.lineSeparator()) +
                String.format("Edge Bound: %d %s", nrTrans, System.lineSeparator()) +
                String.format("# Training Traces: %d %s", nrTraces, System.lineSeparator()) +
                String.format("k-Urgency: %d %s", kUrgency, System.lineSeparator()) +
                String.format("Max. Guard Constant: %d %s", maxGuard, System.lineSeparator()) +
                String.format("Solver: %s %s", solver.toString(), System.lineSeparator()) +
                String.format("Mode: %s %s", mode.toString(), System.lineSeparator()) +
                String.format("Rounding: %d %s", roundingFactor, System.lineSeparator());
    }

    public static void writeExperimentLog(String modelNameExt, String resultsPath, String configString,
                                          Map<String, Experiments.ExperimentResults> expResults) throws IOException {
        File resultsLog = new File(resultsPath + "/" + modelNameExt + ".log");
        resultsLog.getParentFile().mkdirs();
        BufferedWriter bw = Files.newBufferedWriter(resultsLog.toPath());
        bw.write("Time logs for " + modelNameExt);
        bw.write(System.lineSeparator());
        bw.write("==================================");
        bw.write(System.lineSeparator());
        bw.write("Configuration:");
        bw.write(System.lineSeparator());
        bw.write("==================================");
        bw.write(System.lineSeparator());
        bw.write(configString);
        bw.write("Learning time summary: ");
        bw.write(System.lineSeparator());
        bw.write("==================================");
        bw.write(System.lineSeparator());
        Set<Long> learningTimes = expResults.values().stream().map(Experiments.ExperimentResults::getLearningTime)
                .collect(Collectors.toSet());
        bw.write(timeSummary(learningTimes));
        bw.write("Automaton size summary: ");
        bw.write(System.lineSeparator());
        bw.write("==================================");
        bw.write(System.lineSeparator());
        List<Double> autSizes = expResults.values().stream().map(r -> (double)r.getAutSize())
                .collect(Collectors.toList());
        bw.write(valueSummary(autSizes));
        if(System.getProperty("skipQualityEval")==null ||
                "false".equals(System.getProperty("skipQualityEval"))) {
            bw.write("Model metrics summary: ");
            bw.write(System.lineSeparator());
            bw.write("==================================");

            bw.append(System.lineSeparator());
            bw.write("Precision:");
            bw.append(System.lineSeparator());
            List<Double> precisions = expResults.values().stream().map(r -> r.getQualityStatistics().getPrecision())
                    .collect(Collectors.toList());
            bw.write(valueSummary(precisions));
            bw.write("Recall:");
            bw.append(System.lineSeparator());
            List<Double> recalls = expResults.values().stream().map(r -> r.getQualityStatistics().getRecall())
                    .collect(Collectors.toList());
            bw.write(valueSummary(recalls));
            bw.write("F1:");
            bw.append(System.lineSeparator());
            List<Double> f1Scores = expResults.values().stream().map(r -> r.getQualityStatistics().getF1Score())
                    .collect(Collectors.toList());
            bw.write(valueSummary(f1Scores));
        }

        bw.write("Results for individual experiments: ");
        bw.write(System.lineSeparator());
        bw.write("==================================");
        bw.write(System.lineSeparator());
        for (Map.Entry<String, Experiments.ExperimentResults> resultsEntry : expResults.entrySet()){
            bw.write(String.format("%s :", formatExpName(resultsEntry.getKey())));
            bw.write(System.lineSeparator());
            bw.write(String.format("%s : %s", formatExpName("Learning Time"),
                    formatTimeValue(resultsEntry.getValue().getLearningTime() / 1000.0)));
            bw.write(System.lineSeparator());
            bw.write(String.format("%s : %d", formatExpName("TA Size"),
                    resultsEntry.getValue().getAutSize()));
            bw.write(System.lineSeparator());
            if(System.getProperty("skipQualityEval")==null ||
                    "false".equals(System.getProperty("skipQualityEval"))){
                bw.write("Model statistics");
                bw.write(System.lineSeparator());
                bw.write(resultsEntry.getValue().getQualityStatistics().toString());
            }
        }

        bw.close();
    }

    public static String valueSummary(List<Double> values) {
        StringBuilder sb = new StringBuilder();
        DoubleSummaryStatistics statistics = values.stream().mapToDouble(d -> d).summaryStatistics();
        double mean = statistics.getAverage();
        double maximum = statistics.getMax();
        double minimum = statistics.getMin();
        double median = ExperimentUtil.median(values);
        sb.append(formatExpName("Mean")).append(":").append(String.format("%.2f",mean));
        sb.append(System.lineSeparator());
        sb.append(formatExpName("Max")).append(":").append(String.format("%.2f",maximum));
        sb.append(System.lineSeparator());
        sb.append(formatExpName("Min")).append(":").append(String.format("%.2f",minimum));
        sb.append(System.lineSeparator());
        sb.append(formatExpName("Median")).append(":").append(String.format("%.2f",median));
        sb.append(System.lineSeparator());
        return sb.toString();
    }
    public static String timeSummary(Collection<Long> timeValueMs) {
        StringBuilder sb = new StringBuilder();
        List<Double> seconds = timeValueMs.stream().map(l -> l/1000.0).sorted(Double::compareTo).collect(Collectors.toList());
        DoubleSummaryStatistics statistics = seconds.stream().mapToDouble(d -> d).summaryStatistics();
        double meanSeconds = statistics.getAverage();
        double maxSeconds = statistics.getMax();
        double minSeconds = statistics.getMin();
        double median = ExperimentUtil.median(seconds);
        sb.append(formatExpName("Mean")).append(":").append(formatTimeValue(meanSeconds));
        sb.append(System.lineSeparator());
        sb.append(formatExpName("Max")).append(":").append(formatTimeValue(maxSeconds));
        sb.append(System.lineSeparator());
        sb.append(formatExpName("Min")).append(":").append(formatTimeValue(minSeconds));
        sb.append(System.lineSeparator());
        sb.append(formatExpName("Median")).append(":").append(formatTimeValue(median));
        sb.append(System.lineSeparator());
        return sb.toString();
    }

    public static String formatExpName(String expName) {
        int expNameLen = expName.length();
        for (int i = 0; i < EXP_NAME_LEN - expNameLen; i++){
            expName += " ";
        }
        return expName;
    }

    public static String formatTimeValue(Long milliseconds) {
        return formatTimeValue(milliseconds / 1000.0);
    }
    public static String formatTimeValue(double seconds) {
        String timeString = "";
        double minutes = seconds / 60;
        timeString = createTimeString(timeString, seconds, minutes);
        return timeString;
    }

    public static String createTimeString(String timeString, double seconds, double minutes) {
        String secondsString = String.format("%.2f", seconds);
        String minutesString = String.format("%.2f", minutes);
        int secondLen = secondsString.length();
        for(int i = 0; i < SEC_VAL_LEN - secondLen; i++){
            secondsString += " ";
        }
        int minuteLen = minutesString.length();
        for(int i = 0; i < MIN_VAL_LEN - minuteLen; i++){
            minutesString += " ";
        }
        timeString += secondsString;
        timeString += " sec / ";
        timeString += minutesString;
        timeString += " min";
        return timeString;
    }

    public static Properties loadProperties(String propertiesFileName){
        try (InputStream input = new FileInputStream(propertiesFileName)) {
            Properties prop = new Properties();
            prop.load(input);
            return prop;
        } catch (IOException ex) {
            System.out.println("Unable to read the property file for configuring an experiment to" +
                            "be run, because of an exception: " + ex.getMessage());
            return null;
        }
    }
}
