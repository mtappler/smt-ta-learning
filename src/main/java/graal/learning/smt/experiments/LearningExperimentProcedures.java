package graal.learning.smt.experiments;

import graal.learning.models.Symbol;
import graal.learning.models.Trace;
import graal.learning.smt.IterativeTALearner;
import graal.learning.smt.TALearnerGeneric;
import graal.learning.smt.TALearnerIntIntRational;
import graal.learning.smt.learner_factory.*;
import org.apache.commons.lang3.tuple.Triple;
import org.sosy_lab.java_smt.SolverContextFactory;

import java.util.*;
import java.util.concurrent.*;

public class LearningExperimentProcedures {
    public enum LearningMode{
        Real, Integer, BV, FP
    }
    public static void learnRandomAutomata(int nrTraces,
            String modelNameExt,
            boolean bfsMode,
            int batchSize,
            int nrStates,
            int nrTrans,
            int kUrgency,
            int maxGuard, String traceBaseName, List<Integer> seeds){
        String modelPath = "learned_models/random";
        for(Integer i : seeds){
            String traceFile = traceBaseName + i + ".txt";
            learnAutomaton(traceFile,
                    nrTraces,
                    modelNameExt + "_" + i,
                    modelPath,
                    bfsMode,
                    batchSize,
                    nrStates,
                    nrTrans,
                    kUrgency,
                    maxGuard);
        }
    }
    public static Triple<Integer, Long, String>  learnRandomAutomataIter(int nrTraces,
                                                                         String modelNameExt,
                                                                         boolean bfsMode,
                                                                         int batchSize,
                                                                         int maxNrStates,
                                                                         int maxNrTrans,
                                                                         int kUrgency,
                                                                         int maxGuard, String traceBaseName, Integer seed,
                                                                         String modelPath, SolverContextFactory.Solvers solver,
                                                                         LearningMode mode, int roundingFactor){
        String traceFile = traceBaseName + seed + ".txt";
        return learnAutomatonIteratively(traceFile,
                nrTraces,
                modelNameExt + "_" + seed,
                modelPath,
                bfsMode,
                batchSize,
                maxNrStates,
                maxNrTrans,
                kUrgency,
                maxGuard, solver, mode, roundingFactor);
    }
    public static Triple<Integer, Long, String> learnAutomatonIteratively(String traceFile,
                                                                          int nrTraces,
                                                                          String modelNameExt,
                                                                          String modelPath,
                                                                          boolean bfsMode,
                                                                          int batchSize,
                                                                          int maxNrLocations,
                                                                          int maxNrEdges,
                                                                          int kUrgency,
                                                                          int maxGuard,
                                                                          SolverContextFactory.Solvers solver,
                                                                          LearningMode mode,
                                                                          int roundingFactor){
        return learnAutomatonIteratively(traceFile,  nrTraces, modelNameExt, modelPath, bfsMode,  batchSize, maxNrLocations,
         maxNrEdges, kUrgency, maxGuard, solver, mode, roundingFactor,2.0);
    }
    public static Triple<Integer, Long, String> learnAutomatonIteratively(String traceFile,
                                                                          int nrTraces,
                                                                          String modelNameExt,
                                                                          String modelPath,
                                                                          boolean bfsMode,
                                                                          int batchSize,
                                                                          int maxNrLocations,
                                                                          int maxNrEdges,
                                                                          int kUrgency,
                                                                          int maxGuard,
                                                                          SolverContextFactory.Solvers solver,
                                                                          LearningMode mode,
                                                                          int roundingFactor,
                                                                          double edgesPerLoc){

        List<Trace> traces = ExampleTraces.readTraceFile(traceFile, nrTraces);
        System.out.println("Learning " +modelNameExt+ " from " + traces.size() + " traces");
        Set<Symbol> alphabet = new HashSet<>();
        traces.forEach(t -> alphabet.addAll(t.usedDiscreteActions()));
        String modelFile = modelPath + "/learned_model_" + modelNameExt + "_" + solver.toString() + ".dot";

        AbstractTALearnerFactory<?,?,?> learnerFactory = null;
        switch (mode){
            case Real:
                learnerFactory = new IntIntRationalLearnerFactory();
                break;
            case Integer:
                learnerFactory = new IntIntIntLearnerFactory();
                break;
            case BV:
                learnerFactory = new BVBVLearnerFactory();
                break;
            case FP:
                learnerFactory = new BVBVFPLearnerFactory();
                break;
        }
        IterativeTALearner<?,?,?> learner = new IterativeTALearner<>(
                learnerFactory, modelFile, maxNrLocations, maxNrEdges, edgesPerLoc, kUrgency, bfsMode,
                maxGuard, alphabet,  solver,
                batchSize, traces, roundingFactor
        );
        long start = System.currentTimeMillis();
        int finalModelSize = 0;
        try {
            finalModelSize = learner.learn();
        }catch (Exception e){
            finalModelSize = Integer.MAX_VALUE;
            System.out.println("Learning problem " + e.getMessage());
            e.printStackTrace();
        }
        long end = System.currentTimeMillis();
        long learningTime = (end - start);
        System.out.println("Learning of a TA with " + finalModelSize + " states took " + learningTime + " milliseconds.");
        return Triple.of(finalModelSize, learningTime, learner.getModelFileName());
    }
    public static void learnAutomaton(String traceFile,
                                      int nrTraces,
                                      String modelNameExt,
                                      String modelPath,
                                      boolean bfsMode,
                                      int batchSize,
                                      int nrStates,
                                      int nrTrans,
                                      int kUrgency,
                                      int maxGuard){
        List<Trace> traces = ExampleTraces.readTraceFile(traceFile, nrTraces);
        System.out.println("Learning " +modelNameExt+ " from " + traces.size() + " traces");
        Set<Symbol> alphabet = new HashSet<>();
        traces.forEach(t -> alphabet.addAll(t.usedDiscreteActions()));
        SolverContextFactory.Solvers solver = SolverContextFactory.Solvers.YICES2;

        TALearnerGeneric<?,?,?> learner =
                new TALearnerIntIntRational(nrStates,nrTrans,
                        kUrgency,bfsMode, maxGuard, alphabet);
        learner.setBatchSize(batchSize);
        long start = System.currentTimeMillis();
        try {
            learner.init(solver, modelPath + "/learned_model_"
                    + modelNameExt + "_" +
                    solver.toString() + ".dot");
            learner.learn(traces);
        }catch (Exception e){
            System.out.println("Learning problem " + e.getMessage());
            e.printStackTrace();
        }
        long end = System.currentTimeMillis();

        System.out.println("Learning took " + (end - start) + " milliseconds.");
    }
    public static void main(String[] args) throws Exception{
//        learnRandomAutomata();
//        manualExperiments();
    }

    private static void manualExperiments() {
        //        List<Trace> traces = ExampleTraces.lightTraces();
//        List<Trace> traces = ExampleTraces.casTraces(14, -1);
//        List<Trace> traces = ExampleTraces.readTraceFile("traces/light.txt", 40);
        List<Trace> traces = ExampleTraces.readTraceFile("traces/cas_new.txt", 10);
        String modelNameExt = "cas";
        System.out.println("Learning from " + traces.size() + " traces");
        System.out.println(traces.toString());
        Set<Symbol> alphabet = new HashSet<>();
        traces.forEach(t -> alphabet.addAll(t.usedDiscreteActions()));
        for(Symbol s : alphabet)
            System.out.println(s);

        boolean bfsMode = false;
//        TALearnerGeneric<?,?,?> learner = new TALearnerIntInt(20,40,2,bfsMode, 40, alphabet);
        TALearnerGeneric<?,?,?> learner = new TALearnerIntIntRational(14,25,3,bfsMode, 40, alphabet);
//        TALearnerGeneric<?,?,?> learner = new TALearnerIntRationalIncremental(15,25,2, 40, alphabet);
//        TALearnerGeneric<?,?,?> learner = new TALearnerBVBV(5,9,2, bfsMode, 20,16);
//          TALearnerGeneric<?,?,?> learner = new TALearnerBVBVFP(5,8,1, bfsMode, 40,10,alphabet);
//
        learner.setBatchSize(1);
//        for (SolverContextFactory.Solvers solver : SolverContextFactory.Solvers.values()) {
        for (SolverContextFactory.Solvers solver : Collections.singleton(SolverContextFactory.Solvers.YICES2)) {
            System.out.println("Trying " + solver.toString());
            Callable<Object> task = new Callable<Object>() {
                public Object call() {
                    long start = System.currentTimeMillis();
                    try {
                        learner.init(solver, "learned_model_" + modelNameExt + "_" + solver.toString() + ".dot");
                        learner.learn(traces);
                    }catch (Exception e){
                        System.out.println("Learning problem " + e.getMessage());
                        e.printStackTrace();
                    }
                    long end = System.currentTimeMillis();

                    System.out.println("Learning took " + (end - start) + " milliseconds.");
                    return null;
                }
            };
            callWithTimeout(task,1200);
        }
    }

    public static void callWithTimeout(Callable<Object> task, int timeout){
        ExecutorService executor = Executors.newCachedThreadPool();

        Future<Object> future = executor.submit(task);
        try {
            Object result = future.get(timeout, TimeUnit.HOURS);
        } catch (TimeoutException ex) {
            // handle the timeout
            System.out.println("time out");
        } catch (InterruptedException e) {
            // handle the interrupts
            System.out.println("Interrupted");
        } catch (ExecutionException e) {
            System.out.println("execution exception");
            // handle other exceptions
        } finally {
            future.cancel(true); // may or may not desire this
        }

    }
}
