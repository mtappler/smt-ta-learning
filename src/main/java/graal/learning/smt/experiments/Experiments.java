package graal.learning.smt.experiments;

import graal.learning.models.TimedAutomaton;
import graal.learning.models.Util;
import graal.learning.simulation.ModelEvaluator;
import org.apache.commons.lang3.tuple.Triple;
import org.sosy_lab.java_smt.SolverContextFactory;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static graal.learning.smt.experiments.ExperimentUtil.createConfigString;
import static graal.learning.smt.experiments.ExperimentUtil.writeExperimentLog;

public class Experiments {
    private static final int NR_EVAL_TRACES = 200;
    private static final long EVAL_SEED = 2337;
    private static final String PROPERTIES_FILE_NAME = "experiment-properties.prop";
    private static final Set<String> MANUAL_MODELS = new HashSet<>();
    private static final Set<String> RANDOM_MODELS = new HashSet<>();

    static {
        MANUAL_MODELS.add("train");
        MANUAL_MODELS.add("light");
        MANUAL_MODELS.add("cas-arming");
        MANUAL_MODELS.add("cas-alarm");
        RANDOM_MODELS.add("random-3loc");
        RANDOM_MODELS.add("random-4loc");
        RANDOM_MODELS.add("random-5loc");
        RANDOM_MODELS.add("random-6loc");
    }


    public static class ExperimentResults {
        public ExperimentResults(long learningTime, int autSize,
                                 ModelEvaluator.ModelQualityStatistics qualityStatistics) {
            this.learningTime = learningTime;
            this.autSize = autSize;
            this.qualityStatistics = qualityStatistics;
        }

        public long getLearningTime() {
            return learningTime;
        }

        public int getAutSize() {
            return autSize;
        }

        public ModelEvaluator.ModelQualityStatistics getQualityStatistics() {
            return qualityStatistics;
        }

        private long learningTime = 0;
        private int autSize = 0;
        private ModelEvaluator.ModelQualityStatistics qualityStatistics = null;
    }

    public static ModelEvaluator.ModelQualityStatistics evaluateLearnedModel(String uppaalExecutable,
                                                                             int maxDelay, int nrSteps,
                                                                      String originalDotFile,
                                                                      String learnedDotFile,
                                                                      String originalUppaalFile,
                                                                      int nrEvalTraces,
                                                                      long seed, int roundingFactor) throws Exception {
        TimedAutomaton original = Util.importFromDotFormat(originalDotFile);
        TimedAutomaton learned = Util.importFromDotFormat(learnedDotFile);

        ModelEvaluator evaluator = new ModelEvaluator(maxDelay, nrSteps,
                Optional.empty(),uppaalExecutable, seed, original,
                originalUppaalFile, learned, nrEvalTraces, roundingFactor);
        return evaluator.evaluate();
    }
    public static void random3LocationsIter(String uppaalExecutable, String modelNameExt, String resultsPath, SolverContextFactory.Solvers solver,
             LearningExperimentProcedures.LearningMode mode) throws Exception {
        int nrTraces = 30;
        String modelPath = "learned_models/random";
        boolean bfsMode = false;
        int batchSize = 1;
        int maxNrStates = 4;
        int maxNrTrans = 8;
        int kUrgency = 3;
        int maxGuard = 20;
        String uppaalFileBase = "eval_models/random/loc_3_clock_1_trans_6_in_out_2_cmax_15_";
        String traceBaseName = "traces/loc_3_clock_1_trans_6_in_out_2_cmax_15_";
        int nrEvalSteps = 10;
        int roundingFactor = (mode == LearningExperimentProcedures.LearningMode.Real) ? 1 : 100;
        performRandomExperiments(uppaalExecutable, nrTraces, modelNameExt, resultsPath, modelPath, bfsMode, batchSize, maxNrStates, maxNrTrans,
                kUrgency, maxGuard, uppaalFileBase, traceBaseName, nrEvalSteps, solver, mode, roundingFactor);
    }
    public static void random4LocationsIter(String uppaalExecutable, String modelNameExt, String resultsPath, SolverContextFactory.Solvers solver,
                                            LearningExperimentProcedures.LearningMode mode) throws Exception {
        int nrTraces = 40;
        String modelPath = "learned_models/random";
        boolean bfsMode = false;
        int batchSize = 1;
        int maxNrStates = 5;
        int maxNrTrans = 10;
        int kUrgency = 3;
        int maxGuard = 20;
        String uppaalFileBase = "eval_models/random/loc_4_clock_1_trans_7_in_out_2_cmax_15_";
        String traceBaseName = "traces/loc_4_clock_1_trans_7_in_out_2_cmax_15_";
        int nrEvalSteps = 10;

        int roundingFactor = (mode == LearningExperimentProcedures.LearningMode.Real) ? 1 : 100;
        performRandomExperiments(uppaalExecutable, nrTraces, modelNameExt, resultsPath, modelPath, bfsMode, batchSize, maxNrStates, maxNrTrans,
                kUrgency, maxGuard, uppaalFileBase, traceBaseName, nrEvalSteps, solver,mode, roundingFactor);
    }
    public static void random5LocationsIter(String uppaalExecutable) throws Exception {
        int nrTraces = 50;
        String modelNameExt = "5loc-real-dfs";
        String resultsPath = "results_yices";
        String modelPath = "learned_models/random";
        boolean bfsMode = false;
        int batchSize = 1;
        int maxNrStates = 7;
        int maxNrTrans = 12;
        int kUrgency = 3;
        int maxGuard = 20;
        String originalFileBase = "eval_models/random/loc_5_clock_1_trans_8_in_out_2_cmax_15_";
        String traceBaseName = "traces/loc_5_clock_1_trans_8_in_out_2_cmax_15_";
        int nrEvalSteps = 10;
        LearningExperimentProcedures.LearningMode mode = LearningExperimentProcedures.LearningMode.Real;
        int roundingFactor = (mode == LearningExperimentProcedures.LearningMode.Real) ? 1 : 100;
        SolverContextFactory.Solvers solver = SolverContextFactory.Solvers.YICES2;
        performRandomExperiments(uppaalExecutable, nrTraces, modelNameExt,resultsPath, modelPath, bfsMode, batchSize, maxNrStates, maxNrTrans,
                kUrgency, maxGuard, originalFileBase, traceBaseName, nrEvalSteps, solver,mode, roundingFactor);
    }
    public static void random6LocationsIter(String uppaalExecutable) throws Exception {
        int nrTraces = 60;
        String modelNameExt = "6loc-noninc";
        String resultsPath = "results_z3";
        String modelPath = "learned_models/random";
        boolean bfsMode = true;
        int batchSize = -1;
        int maxNrStates = 7;
        int maxNrTrans = 14;
        int kUrgency = 3;
        int maxGuard = 20;
        String originalFileBase = "eval_models/random/loc_6_clock_1_trans_10_in_out_2_cmax_15_";
        String traceBaseName = "traces/loc_6_clock_1_trans_10_in_out_2_cmax_15_";
        int nrEvalSteps = 10;
        LearningExperimentProcedures.LearningMode mode = LearningExperimentProcedures.LearningMode.Integer;
        int roundingFactor = (mode == LearningExperimentProcedures.LearningMode.Real) ? 1 : 100;
        SolverContextFactory.Solvers solver = SolverContextFactory.Solvers.Z3;
        performRandomExperiments(uppaalExecutable, nrTraces, modelNameExt,resultsPath, modelPath, bfsMode, batchSize, maxNrStates, maxNrTrans,
                kUrgency, maxGuard, originalFileBase, traceBaseName, nrEvalSteps, solver,mode, roundingFactor);
    }

    public static void random7LocationsIter(String uppaalExecutable) throws Exception {
        int nrTraces = 70;
        String modelNameExt = "7loc-dfs-inc-bsize1";
        String resultsPath = "results";
        String modelPath = "learned_models/random";
        boolean bfsMode = false;
        int batchSize = 1;
        int maxNrStates = 7;
        int maxNrTrans = 14;
        int kUrgency = 3;
        int maxGuard = 20;
        String originalFileBase = "eval_models/random/loc_7_clock_1_trans_11_in_out_2_cmax_15_";
        String traceBaseName = "traces/loc_7_clock_1_trans_11_in_out_2_cmax_15_";
        int nrEvalSteps = 10;
        LearningExperimentProcedures.LearningMode mode = LearningExperimentProcedures.LearningMode.Real;
        int roundingFactor = (mode == LearningExperimentProcedures.LearningMode.Real) ? 1 : 100;
        SolverContextFactory.Solvers solver = SolverContextFactory.Solvers.YICES2;
        performRandomExperiments(uppaalExecutable, nrTraces, modelNameExt, resultsPath, modelPath, bfsMode, batchSize,
                maxNrStates, maxNrTrans,
                kUrgency, maxGuard, originalFileBase, traceBaseName, nrEvalSteps, solver, mode, roundingFactor);
    }

    private static void performRandomExperiments(String uppaalExecutable, int nrTraces, String modelNameExt,
                                                 String resultsPath,
                                                 String modelPath, boolean bfsMode, int batchSize, int maxNrStates,
                                                 int maxNrTrans, int kUrgency, int maxGuard, String originalFileBase,
                                                 String traceBaseName, int nrEvalSteps, SolverContextFactory.Solvers solver,
                                                 LearningExperimentProcedures.LearningMode mode,
                                                 int roundingFactor) throws Exception {
        Map<String, ExperimentResults> experimentResults = new HashMap<>();

        for (int seed = 1; seed <= 10; seed++) {
            final int finalSeed = seed;
            Supplier<Triple<Integer, Long, String>> experiments = () ->
                    LearningExperimentProcedures.learnRandomAutomataIter(
                    nrTraces, modelNameExt, bfsMode, batchSize, maxNrStates,
                    maxNrTrans, kUrgency, maxGuard, traceBaseName, finalSeed, modelPath,
                    solver, mode, roundingFactor);
            ExperimentRunner runner = new ExperimentRunner("random", resultsPath, experiments);
            Triple<Integer, Long, String>  result = runner.runExperiment();
            long time = result.getMiddle();
            int automatonSize = result.getLeft();
            String originalDotFile = originalFileBase + seed + ".dot";
            String learnedDotFile = result.getRight();
            String originalUppaalFile = originalFileBase + seed + ".xml";
            ModelEvaluator.ModelQualityStatistics modelQuality = null;
            if(System.getProperty("skipQualityEval")==null ||
                "false".equals(System.getProperty("skipQualityEval"))){
                modelQuality = evaluateLearnedModel(uppaalExecutable,
                        maxGuard, nrEvalSteps,
                        originalDotFile,
                        learnedDotFile,
                        originalUppaalFile, NR_EVAL_TRACES,
                        EVAL_SEED, roundingFactor);
            }
            experimentResults.put("Seed " + seed,
                    new ExperimentResults(time,automatonSize,modelQuality));
        }
        String configString = createConfigString(bfsMode, batchSize, maxNrStates, maxNrTrans, nrTraces, kUrgency,
                maxGuard, solver,
            mode, roundingFactor);
        writeExperimentLog(modelNameExt,resultsPath, configString, experimentResults);
    }

    public static void casDiamond(String uppaalExecutable) throws Exception {
        int nrTraces = 50;
        String resultsPath = "results_yices";
        String modelNameExt = "cas-diamond";
        String modelPath = "learned_models/cas_diamond";
        boolean bfsMode = true;
        int batchSize = 1;
        int maxNrStates = 10;
        int maxNrTrans = 20;
        int kUrgency = 3;
        int maxGuard = 10;
        String traceFile = "traces/cas_diamond.txt";
        int nrEvalSteps = 14;
        LearningExperimentProcedures.LearningMode mode = LearningExperimentProcedures.LearningMode.Real;
        int roundingFactor = (mode == LearningExperimentProcedures.LearningMode.Real) ? 1 : 100;

        SolverContextFactory.Solvers solver = SolverContextFactory.Solvers.YICES2;
        Supplier<Triple<Integer, Long, String>> experiments = () ->
                LearningExperimentProcedures.learnAutomatonIteratively(
                        traceFile,
                        nrTraces,
                        modelNameExt,
                        modelPath,
                        bfsMode,
                        batchSize,
                        maxNrStates,
                        maxNrTrans,
                        kUrgency,
                        maxGuard,
                        solver,
                        mode,
                        roundingFactor
        );
        ExperimentRunner runner = new ExperimentRunner("cas-diamond", resultsPath, experiments);
        Triple<Integer, Long, String>  result = runner.runExperiment();
        long time = result.getMiddle();
        int automatonSize = result.getLeft();
        String originalDotFile = "eval_models/CAS_diamond.dot";
        String learnedDotFile = result.getRight();
        String originalUppaalFile = "eval_models/CAS_diamond.xml";
        ModelEvaluator.ModelQualityStatistics modelQuality = evaluateLearnedModel(uppaalExecutable,
                maxGuard, nrEvalSteps,
                originalDotFile,
                learnedDotFile,
                originalUppaalFile, NR_EVAL_TRACES,
                EVAL_SEED, roundingFactor);
        ExperimentResults results = new ExperimentResults(time, automatonSize, modelQuality);
        Map<String, ExperimentResults> experimentResults = new HashMap<>();
        experimentResults.put("cas-diamond", results);

        String configString = createConfigString(
                bfsMode, batchSize, maxNrStates, maxNrTrans, nrTraces, kUrgency, maxGuard, solver, mode, roundingFactor
        );
        writeExperimentLog(modelNameExt,resultsPath, configString, experimentResults);
    }
    public static void casRest(String uppaalExecutable) throws Exception {
        int nrTraces = 50;
        String resultsPath = "results_yices";
        String modelNameExt = "cas-rest";
        String modelPath = "learned_models/cas_rest";
        boolean bfsMode = true;
        int batchSize = 1;
        int maxNrStates = 9;
        int maxNrTrans = 18;
        int kUrgency = 3;
        int maxGuard = 33;
        String traceFile = "traces/CAS_rest.txt";
        int nrEvalSteps = 14;
        LearningExperimentProcedures.LearningMode mode = LearningExperimentProcedures.LearningMode.Real;
        int roundingFactor = (mode == LearningExperimentProcedures.LearningMode.Real) ? 1 : 100;

        SolverContextFactory.Solvers solver = SolverContextFactory.Solvers.YICES2;
        Supplier<Triple<Integer, Long, String>> experiments = () ->
                LearningExperimentProcedures.learnAutomatonIteratively(
                        traceFile,
                        nrTraces,
                        modelNameExt,
                        modelPath,
                        bfsMode,
                        batchSize,
                        maxNrStates,
                        maxNrTrans,
                        kUrgency,
                        maxGuard,
                        solver,
                        mode,
                        roundingFactor
                );
        ExperimentRunner runner = new ExperimentRunner("cas-rest", resultsPath, experiments);
        Triple<Integer, Long, String>  result = runner.runExperiment();
        long time = result.getMiddle();
        int automatonSize = result.getLeft();
        String originalDotFile = "eval_models/CAS_rest.dot";
        String learnedDotFile = result.getRight();
        String originalUppaalFile = "eval_models/CAS_rest.xml";
        ModelEvaluator.ModelQualityStatistics modelQuality = evaluateLearnedModel(uppaalExecutable,
                maxGuard, nrEvalSteps,
                originalDotFile,
                learnedDotFile,
                originalUppaalFile, NR_EVAL_TRACES,
                EVAL_SEED, roundingFactor);
        ExperimentResults results = new ExperimentResults(time, automatonSize, modelQuality);
        Map<String, ExperimentResults> experimentResults = new HashMap<>();
        experimentResults.put("cas-rest", results);

        String configString = createConfigString(
                bfsMode, batchSize, maxNrStates, maxNrTrans, nrTraces, kUrgency, maxGuard, solver, mode, roundingFactor
        );
        writeExperimentLog(modelNameExt,resultsPath, configString, experimentResults);
    }

    public static void light(String uppaalExecutable) throws Exception {
        int nrTraces = 50;
        String resultsPath = "results_yices";
        String modelNameExt = "light";
        String modelPath = "learned_models/light";
        boolean bfsMode = true;
        int batchSize = 1;
        int maxNrStates = 6;
        int maxNrTrans = 18;
        int kUrgency = 3;
        int maxGuard = 15;
        String traceFile = "traces/light.txt";
        int nrEvalSteps = 12;
        LearningExperimentProcedures.LearningMode mode = LearningExperimentProcedures.LearningMode.Real;
        int roundingFactor = (mode == LearningExperimentProcedures.LearningMode.Real) ? 1 : 100;
        long evalSeed = EVAL_SEED + 1000; // original seed causes error

        SolverContextFactory.Solvers solver = SolverContextFactory.Solvers.YICES2;
        Supplier<Triple<Integer, Long, String>> experiments = () ->
                LearningExperimentProcedures.learnAutomatonIteratively(
                        traceFile,
                        nrTraces,
                        modelNameExt,
                        modelPath,
                        bfsMode,
                        batchSize,
                        maxNrStates,
                        maxNrTrans,
                        kUrgency,
                        maxGuard,
                        solver,
                        mode,
                        roundingFactor
                );
        ExperimentRunner runner = new ExperimentRunner("light", resultsPath, experiments);
        Triple<Integer, Long, String>  result = runner.runExperiment();
        long time = result.getMiddle();
        int automatonSize = result.getLeft();
        String originalDotFile = "eval_models/light.dot";
        String learnedDotFile = result.getRight();
        String originalUppaalFile = "eval_models/light.xml";
        ModelEvaluator.ModelQualityStatistics modelQuality = evaluateLearnedModel(uppaalExecutable,
                maxGuard, nrEvalSteps,
                originalDotFile,
                learnedDotFile,
                originalUppaalFile, NR_EVAL_TRACES, evalSeed, roundingFactor);
        ExperimentResults results = new ExperimentResults(time, automatonSize, modelQuality);
        Map<String, ExperimentResults> experimentResults = new HashMap<>();
        experimentResults.put("light", results);

        String configString = createConfigString(
                bfsMode, batchSize, maxNrStates, maxNrTrans, nrTraces, kUrgency, maxGuard, solver, mode, roundingFactor
        );
        writeExperimentLog(modelNameExt,resultsPath, configString, experimentResults);
    }

    public static void train(String uppaalExecutable) throws Exception {
        int nrTraces = 50;
        String resultsPath = "results_yices";
        String modelNameExt = "train";
        String modelPath = "learned_models/train";
        boolean bfsMode = true;
        int batchSize = 1;
        int maxNrStates = 7;
        int maxNrTrans = 18;
        int kUrgency = 3;
        int maxGuard = 20;
        String traceFile = "traces/train.txt";
        int nrEvalSteps = 12;
        LearningExperimentProcedures.LearningMode mode = LearningExperimentProcedures.LearningMode.Real;
        int roundingFactor = (mode == LearningExperimentProcedures.LearningMode.Real) ? 1 : 100;
        long evalSeed = EVAL_SEED + 1000;
        double edgesPerLoc = 2;

        SolverContextFactory.Solvers solver = SolverContextFactory.Solvers.YICES2;
        Supplier<Triple<Integer, Long, String>> experiments = () ->
                LearningExperimentProcedures.learnAutomatonIteratively(
                        traceFile,
                        nrTraces,
                        modelNameExt,
                        modelPath,
                        bfsMode,
                        batchSize,
                        maxNrStates,
                        maxNrTrans,
                        kUrgency,
                        maxGuard,
                        solver,
                        mode,
                        roundingFactor,
                        edgesPerLoc
                );
        ExperimentRunner runner = new ExperimentRunner("train", resultsPath, experiments);
        Triple<Integer, Long, String>  result = runner.runExperiment();
        long time = result.getMiddle();
        int automatonSize = result.getLeft();
        String originalDotFile = "eval_models/train.dot";
        String learnedDotFile = result.getRight();
        String originalUppaalFile = "eval_models/train.xml";
        ModelEvaluator.ModelQualityStatistics modelQuality = evaluateLearnedModel(uppaalExecutable,
                maxGuard, nrEvalSteps,
                originalDotFile,
                learnedDotFile,
                originalUppaalFile, NR_EVAL_TRACES, evalSeed, roundingFactor);
        ExperimentResults results = new ExperimentResults(time, automatonSize, modelQuality);
        Map<String, ExperimentResults> experimentResults = new HashMap<>();
        experimentResults.put("train", results);

        String configString = createConfigString(
                bfsMode, batchSize, maxNrStates, maxNrTrans, nrTraces, kUrgency, maxGuard, solver, mode, roundingFactor
        );
        writeExperimentLog(modelNameExt,resultsPath, configString, experimentResults);
    }

    public static void experimentFromPropFile(String uppaalExecutable,String propertiesFileName) throws Exception {
        Properties properties = ExperimentUtil.loadProperties(propertiesFileName);
        if(properties == null)
            return;
        if(!validateParameters(properties)){
            System.out.println("Stopping because of invalid configuration.");
            return;
        }
        if("true".equals(properties.getProperty("skipQualityEval"))){
            System.setProperty("skipQualityEval","true");
        }
        int nrTraces = Integer.parseInt(properties.getProperty("nrTrainingTraces")); 
        boolean bfsMode = Boolean.parseBoolean(properties.getProperty("bfsMode"));
        boolean incremental = Boolean.parseBoolean(properties.getProperty("incremental"));
        int batchSize = incremental ? 1 : -1; // fixed to one if incremental, -1 otherwise
        boolean discreteFirst = Boolean.parseBoolean(properties.getProperty("discreteFirst"));
        if(discreteFirst){
            System.setProperty("discrete-first","true");
        } else {
            System.setProperty("discrete-first","false");
        }

        int maxNrStates = Integer.parseInt(properties.getProperty("maxNrLocations"));
        int maxNrTrans = Integer.parseInt(properties.getProperty("maxNrEdges"));
        int kUrgency = Integer.parseInt(properties.getProperty("kUrgency"));
        int maxGuard = Integer.parseInt(properties.getProperty("maxGuardConstant"));
        int nrEvalSteps = 12; // max length of evaluation traces, fixed to twelve (longer than training traces)
        SolverContextFactory.Solvers solver = getSolver(properties.getProperty("solver"));
        LearningExperimentProcedures.LearningMode mode = getLearningMode(properties.getProperty("theory"));
        int roundingFactor = Integer.parseInt(properties.getProperty("roundingFactor"));
        double edgesPerLoc = Integer.parseInt(properties.getProperty("edgesPerLocation"));
        String experimentName = properties.getProperty("experimentName");
        
        String resultsPath = "results/" + solver;
        if(MANUAL_MODELS.contains(experimentName)) {
            String modelPath = "learned_models/" + experimentName;
            String traceFile = getTraceFileManualModel(experimentName);
            String originalDotFile = getDotFileManualModel(experimentName);
            String originalUppaalFile = getUppaalFileManualModel(experimentName);
            evalOnManualModel(uppaalExecutable,nrTraces, bfsMode, batchSize, maxNrStates, maxNrTrans, kUrgency,
                    maxGuard, nrEvalSteps, solver, mode, roundingFactor, edgesPerLoc, resultsPath,
                    experimentName, modelPath, traceFile, originalDotFile, originalUppaalFile);
        } else if (RANDOM_MODELS.contains(experimentName)){

            String originalFileBase = getModelBaseNameRandomModel(experimentName);
            String traceBaseName = getTraceBaseNameRandomModel(experimentName);
            String modelPath = "learned_models/random";
            performRandomExperiments(uppaalExecutable,nrTraces, experimentName,
                    resultsPath, modelPath, bfsMode, batchSize,
                    maxNrStates, maxNrTrans,kUrgency, maxGuard,
                    originalFileBase, traceBaseName,
                    nrEvalSteps, solver, mode, roundingFactor);

        } else {
            System.out.println("Unknown model name for evaluation options are: ");
            for(String name : MANUAL_MODELS)
                System.out.println(name);
            for(String name : RANDOM_MODELS)
                System.out.println(name);
        }
    }

    private static boolean validateParameters(Properties properties) {
        return validateString(properties,"experimentName", "light","train","cas-arming",
                "cas-alarm","random-3loc","random-4loc","random-5loc","random-6loc")
                && validatePositiveInteger(properties,"nrTrainingTraces", false)
                && validatePositiveInteger(properties,"maxNrLocations", false)
                && validatePositiveInteger(properties,"maxNrEdges", false)
                && validatePositiveInteger(properties,"edgesPerLocation", false)
                && validatePositiveInteger(properties,"kUrgency", true)
                && validatePositiveInteger(properties,"maxGuardConstant", false)
                && validatePositiveInteger(properties,"roundingFactor", false)
                && validateBoolean(properties,"incremental")
                && validateBoolean(properties,"bfsMode")
                && validateBoolean(properties,"discreteFirst")
                && validateString(properties,"solver", "MATHSAT5" , "SMTINTERPOL",
                "Z3", "PRINCESS", "BOOLECTOR", "CVC4", "YICES2")
                && validateString(properties,"theory", "Real", "BV", "Integer", "FP");
    }

    private static boolean validateBoolean(Properties properties, String propertyName) {
        String value = properties.getProperty(propertyName);
        if(value == null) {
            System.out.println("Required parameter \"" + propertyName + "\" not specified.");
            return false;
        }
        boolean invalid = !value.equals("true") && !value.equals("false");
        if(invalid)
            System.out.printf("Invalid value for parameter \"%s\". Please specify either true or false.%n",
                    propertyName);
        return !invalid;
    }
    private static boolean validatePositiveInteger(Properties properties, String propertyName, boolean includeZero) {
        String value = properties.getProperty(propertyName);
        if(value == null) {
            System.out.println("Required parameter \"" + propertyName + "\" not specified.");
            return false;
        }
        boolean invalid = false;
        try{
            int intValue = Integer.parseInt(value);
            if (includeZero && intValue < 0)
                invalid = true;
            else if (!includeZero && intValue <= 0)
                invalid = true;
        } catch (NumberFormatException exception){
            invalid = true;
        }
        if(invalid)
        System.out.printf("Invalid value for parameter \"%s\". Please specify a %s integer literal.%n",
                propertyName, includeZero? "non-negative" : "positive");
        return !invalid;
    }

    private static boolean validateString(Properties properties, String propertyName, String... admissibleValues) {
        String value = properties.getProperty(propertyName);
        if(value == null) {
            System.out.println("Required parameter \"" + propertyName + "\" not specified.");
            return false;
        }
        Set<String> valueSet = Arrays.stream(admissibleValues).collect(Collectors.toSet());
        if(!valueSet.contains(value)){
            System.out.printf("Specified parameter value \"%s\" for parameter \"%s\" " +
                    "is not admissible. %n",value, propertyName);
            System.out.printf("Possible values are %s. %n", String.join(", ", valueSet));
            return false;
        }
        return true;
    }

    private static LearningExperimentProcedures.LearningMode getLearningMode(String theory) {
        return LearningExperimentProcedures.LearningMode.valueOf(theory);
    }

    private static SolverContextFactory.Solvers getSolver(String solver) {
        return SolverContextFactory.Solvers.valueOf(solver);
    }

    private static String getTraceBaseNameRandomModel(String experimentName) {
        switch (experimentName){
            case "random-3loc":
                return "traces/loc_3_clock_1_trans_6_in_out_2_cmax_15_";
            case "random-4loc":
                return "traces/loc_4_clock_1_trans_7_in_out_2_cmax_15_";
            case "random-5loc":
                return "traces/loc_5_clock_1_trans_8_in_out_2_cmax_15_";
            case "random-6loc":
                return "traces/loc_6_clock_1_trans_10_in_out_2_cmax_15_";
            default:
                return null;
        }
    }

    private static String getModelBaseNameRandomModel(String experimentName) {
        switch (experimentName){
            case "random-3loc":
                return "eval_models/random/loc_3_clock_1_trans_6_in_out_2_cmax_15_";
            case "random-4loc":
                return "eval_models/random/loc_4_clock_1_trans_7_in_out_2_cmax_15_";
            case "random-5loc":
                return "eval_models/random/loc_5_clock_1_trans_8_in_out_2_cmax_15_";
            case "random-6loc":
                return "eval_models/random/loc_6_clock_1_trans_10_in_out_2_cmax_15_";
            default:
                return null;
        }
    }

    private static String getTraceFileManualModel(String experimentName) {
        switch (experimentName){
            case "train":
                return "traces/train.txt";
            case "light":
                return "traces/light.txt";
            case "cas-arming":
                return "traces/cas_diamond.txt";
            case "cas-alarm":
                return "traces/CAS_rest.dot";
            default:
                return null;
        }
    }
    private static String getDotFileManualModel(String experimentName) {
        switch (experimentName){
            case "train":
                return "eval_models/train.dot";
            case "light":
                return "eval_models/light.dot";
            case "cas-arming":
                return "eval_models/CAS_diamond.dot";
            case "cas-alarm":
                return "eval_models/CAS_rest.dot";
            default:
                return null;
        }
    }

    private static String getUppaalFileManualModel(String experimentName) {
        switch (experimentName){
            case "train":
                return "eval_models/train.xml";
            case "light":
                return "eval_models/light.xml";
            case "cas-arming":
                return "eval_models/CAS_diamond.xml";
            case "cas-alarm":
                return "eval_models/CAS_rest.xml";
            default:
                return null;
        }
    }

    private static void evalOnManualModel(String uppaalExecutable,
                                          int nrTraces, boolean bfsMode, int batchSize, int maxNrStates, int maxNrTrans, int kUrgency, int maxGuard, int nrEvalSteps, SolverContextFactory.Solvers solver, LearningExperimentProcedures.LearningMode mode, int roundingFactor, double edgesPerLoc, String resultsPath, String experimentName, String modelPath, String traceFile, String originalDotFile, String originalUppaalFile) throws Exception {
        Supplier<Triple<Integer, Long, String>> experiments = () ->
                LearningExperimentProcedures.learnAutomatonIteratively(
                        traceFile,
                        nrTraces,
                        experimentName,
                        modelPath,
                        bfsMode,
                        batchSize,
                        maxNrStates,
                        maxNrTrans,
                        kUrgency,
                        maxGuard,
                        solver,
                        mode,
                        roundingFactor,
                        edgesPerLoc
                );

        ExperimentRunner runner = new ExperimentRunner(experimentName, resultsPath, experiments);
        Triple<Integer, Long, String>  result = runner.runExperiment();
        long time = result.getMiddle();
        int automatonSize = result.getLeft();
        String learnedDotFile = result.getRight();
        ModelEvaluator.ModelQualityStatistics modelQuality = null;

        if(System.getProperty("skipQualityEval")==null ||
                "false".equals(System.getProperty("skipQualityEval"))){
            modelQuality = evaluateLearnedModel(uppaalExecutable,
                    maxGuard, nrEvalSteps,
                    originalDotFile,
                    learnedDotFile,
                    originalUppaalFile, NR_EVAL_TRACES, EVAL_SEED, roundingFactor);
        }
        ExperimentResults results = new ExperimentResults(time, automatonSize, modelQuality);
        Map<String, ExperimentResults> experimentResults = new HashMap<>();
        experimentResults.put(experimentName, results);

        String configString = createConfigString(
                bfsMode, batchSize, maxNrStates, maxNrTrans, nrTraces, kUrgency, maxGuard,
                solver, mode, roundingFactor
        );
        writeExperimentLog(experimentName, resultsPath, configString, experimentResults);
    }


    public static void main(String[] args) throws Exception {
        Properties pathProperties = ExperimentUtil.loadProperties("paths.prop");
        String uppaalExecutable = pathProperties.getProperty("uppaalExecutable");
        experimentFromPropFile(uppaalExecutable,PROPERTIES_FILE_NAME);
    }
}
