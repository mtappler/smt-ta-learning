package graal.learning.smt;

import graal.learning.models.*;
import org.apache.commons.lang3.tuple.Pair;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.BasicLogManager;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.java_smt.SolverContextFactory;
import org.sosy_lab.java_smt.api.*;
import org.sosy_lab.java_smt.api.SolverContext.ProverOptions;

import java.io.File;
import java.util.*;

public abstract class TALearnerGeneric<DType extends Formula,GType extends Formula, CType extends Formula> {

    private boolean printIntermediateModels = false;
    protected int roundingFactor = 1;

    public boolean isPrintIntermediateModels() {
        return printIntermediateModels;
    }

    public void setPrintIntermediateModels(boolean printIntermediateModels) {
        this.printIntermediateModels = printIntermediateModels;
    }

    public boolean isSimplify() {
        return simplify;
    }

    public void setSimplify(boolean simplify) {
        this.simplify = simplify;
    }

    private boolean simplify = true;

    public boolean isStrictIsolatedOutputs() {
        return strictIsolatedOutputs;
    }

    public void setStrictIsolatedOutputs(boolean strictIsolatedOutputs) {
        this.strictIsolatedOutputs = strictIsolatedOutputs;
    }

    protected boolean strictIsolatedOutputs = false;
    protected boolean veryStrictIsolatedOutputs = false;
    protected int maxGuardValue = 20;
    protected int nrTrans;
    protected int nrStates;
    protected final int k_urgency;
    protected final boolean bfsMode;
    protected SolverContext context;
    protected UFManager fmgr;
    protected FunctionDeclaration<DType> source;
    protected FunctionDeclaration<DType> target;
    protected FunctionDeclaration<DType> label;
    protected FunctionDeclaration<BooleanFormula> reset;
    protected FunctionDeclaration<BooleanFormula> is_output;
    protected FunctionDeclaration<GType> invariant;
    protected FunctionDeclaration<GType> guard_below;
    protected FunctionDeclaration<GType> guard_above;
    protected String modelFileName;
    protected int batchSize = -1;
    protected BooleanFormulaManager bmgr;
    protected Map<Symbol,Integer> discreteToInt = null;
    protected Set<Symbol> alphabet = null;
    protected boolean halfStrictGuards = true;

    public boolean isIncrementalDiscreteThenTime() {
        return incrementalDiscreteThenTime;
    }

    public void setIncrementalDiscreteThenTime(boolean incrementalDiscreteThenTime) {
        this.incrementalDiscreteThenTime = incrementalDiscreteThenTime;
    }

    protected boolean incrementalDiscreteThenTime = true;

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public TALearnerGeneric(int nrStates, int nrTrans, int k_urgency, boolean bfsMode, int maxGuardValue, Set<Symbol> alphabet){
        this.nrStates = nrStates;
        this.nrTrans = nrTrans;
        this.k_urgency = k_urgency;
        this.bfsMode = bfsMode;
        this.maxGuardValue = maxGuardValue;
        this.alphabet = alphabet;
    }
    public void init(SolverContextFactory.Solvers solver, String modelFileName) throws InvalidConfigurationException {
        this.modelFileName = modelFileName;
        Configuration config = Configuration.defaultConfiguration();
        LogManager logger = BasicLogManager.create(config);
        ShutdownNotifier notifier = ShutdownNotifier.createDummy();
        context = SolverContextFactory.createSolverContext(config, logger, notifier, solver);
        fmgr = context.getFormulaManager().getUFManager();
        bmgr = context.getFormulaManager().getBooleanFormulaManager();

        discreteToInt = new HashMap<>();
        Iterator<Symbol> alphabetIter = alphabet.iterator();
        int discreteId = 0;
        while (alphabetIter.hasNext()) {
            Symbol alphabetElem = alphabetIter.next();
            int intId = discreteId++;
            discreteToInt.put(alphabetElem, intId);
        }
        // by default, discrete-first is enabled but can be disabled
        if(System.getProperties().get("discrete-first") != null && System.getProperties().get("discrete-first").equals("false")){
            this.incrementalDiscreteThenTime = false;
        }
        initAdditional();
        initUF();
    }

    protected void initAdditional(){

    }

    protected abstract void initUF();

    public boolean learn(List<Trace> traces) throws Exception {
        if(veryStrictIsolatedOutputs) {
                System.out.println("Very strict guards used");
        }
        try(ProverEnvironment prover = context.newProverEnvironment(ProverOptions.GENERATE_MODELS)){
            return learn(traces, prover,true,0,0);
        } catch (Exception e){
            throw e;
        }
    }

    public boolean learn(List<Trace> traces, ProverEnvironment prover, boolean addGeneralConstraints, int startTraceIndex,
                         int startStepIndex)
            throws Exception {

        List<BooleanFormula> constraints = new ArrayList<>();
        if(addGeneralConstraints) {
            for (Symbol symbol : alphabet) {
                int intId = discreteToInt.get(symbol);
                // there is at least one transition for each label
                constraints.add(discreteVarEquals(fmgr.callUF(label, makeDiscreteNumber(intId)), makeDiscreteNumber(intId)));
            }

            addGuardBoundConstraints(constraints);
            // determinism
            addDeterminismConstraints(constraints);

            if (veryStrictIsolatedOutputs) {
                for (int i = 0; i < nrTrans; i++) {
                    for (int j = 0; j < nrTrans; j++) {
                        if (i < j) {
                            constraints.add(bmgr.not(
                                    bmgr.and(
                                            discreteVarEquals(fmgr.callUF(source, makeDiscreteNumber(i)), fmgr.callUF(source, makeDiscreteNumber(j))),
                                            fmgr.callUF(is_output, makeDiscreteNumber(i)), fmgr.callUF(is_output, makeDiscreteNumber(j))
                                    )));
                        }
                    }
                }
            }
        }
        if(bfsMode){
            if(incrementalDiscreteThenTime) {
                addTraceConstraintsBFS(traces, prover, startTraceIndex, constraints, startStepIndex, true, false);
                addTraceConstraintsBFS(traces, prover, startTraceIndex, constraints, startStepIndex, false, true);
            } else {
                addTraceConstraintsBFS(traces, prover, startTraceIndex, constraints, startStepIndex, false, false);
            }
        } else {
            if(incrementalDiscreteThenTime) {
                addTraceConstraintsDFS(traces, prover, startTraceIndex, startStepIndex, constraints,true,false);
                addTraceConstraintsDFS(traces, prover, startTraceIndex, startStepIndex, constraints,false,true);
            } else {
                addTraceConstraintsDFS(traces, prover, startTraceIndex, startStepIndex, constraints,false,false);
            }
        }
        for(BooleanFormula c : constraints)
            prover.addConstraint(c);
        System.out.println("Starting check");
        boolean isUnsolvable = prover.isUnsat();
        if (isUnsolvable) {
            System.out.println("Not satisfiable");
            return false;
        }
        else {
            TimedAutomaton model = extractModel(prover, discreteToInt);
            if(simplify)
                removedUnusedParts(model,traces);
            writeModel(model);
            return true;
        }
    }

    private void addTraceConstraintsDFS(List<Trace> traces, ProverEnvironment prover, int startTraceIndex,
                                        int startStepIndex, List<BooleanFormula> constraints,
                                        boolean noTimeConstr, boolean noDiscreteConstr) throws InterruptedException, SolverException {
        int traceIndex = startTraceIndex;
        for (Trace t : traces) {
            int stepIndex = startStepIndex;
            addInitialLocationConstr(constraints, traceIndex);

            for (Pair<Symbol, Delay> step : t.getTrace()) {
                addStepConstraint(discreteToInt, constraints, traceIndex,
                        stepIndex, step,noTimeConstr,noDiscreteConstr);
                stepIndex++;
            }
            traceIndex++;
            if(!incrementalCheck(prover, constraints, traceIndex))
                break;
            else if(batchSize > 0 && traceIndex % batchSize == 0){
                if(printIntermediateModels) {
                    String modelFileNameSave = modelFileName;
                    modelFileName = modelFileName.replace(".dot", traceIndex + ".dot");
                    extractModel(prover, discreteToInt);
                    modelFileName = modelFileNameSave;
                }
            }
        }
    }

    private void addTraceConstraintsBFS(List<Trace> traces, ProverEnvironment prover, int startTraceIndex,
                                        List<BooleanFormula> constraints, int startStepIndex,
                                        boolean noTimeConstr, boolean noDiscreteConstr) throws InterruptedException, SolverException {
        int stepIndex = startStepIndex;
        while(true){
            boolean addedConstr = false;
            int traceIndex = startTraceIndex;
            for (Trace t : traces){
                if(stepIndex == 0){
                    addInitialLocationConstr(constraints, traceIndex);
                }
                if(stepIndex < t.length()) {
                    Pair<Symbol, Delay> step = t.get(stepIndex);
                    addedConstr = true;
                    addStepConstraint(discreteToInt, constraints, traceIndex,
                            stepIndex, step,noTimeConstr,noDiscreteConstr);
                }
                traceIndex++;
            }
            if(!incrementalCheck(prover, constraints, stepIndex))
                break;
            else if(batchSize > 0 && stepIndex % batchSize == 0){
                String modelFileNameSave = modelFileName;
                modelFileName = modelFileName.replace(".dot", stepIndex + ".dot" );
                if(printIntermediateModels)
                    extractModel(prover, discreteToInt);
                modelFileName = modelFileNameSave;
            }
            stepIndex++;

            if(!addedConstr)
                break;
        }
    }

    protected void writeModel(TimedAutomaton model) throws Exception {
        DotExporter exporter = new DotExporter();
        new File(modelFileName).getParentFile().mkdirs();
        exporter.export(model,modelFileName);
    }

    private void removedUnusedParts(TimedAutomaton model, List<Trace> traces) {
        Set<Location> coveredLocs = new HashSet<>();
        Set<Edge> coveredEdges = new HashSet<>();
        for (Trace t : traces){
            Pair<Set<Location>,Set<Edge>> covered = model.findCoveredElements(t, this.roundingFactor);
            coveredLocs.addAll(covered.getKey());
            coveredEdges.addAll(covered.getValue());
        }
        List<Edge> tobeRemovedEdges = new ArrayList<>();
        for(Edge e : model.getEdgeList()){
            if(!coveredEdges.contains(e))
                tobeRemovedEdges.add(e);
        }
        for (Edge e : tobeRemovedEdges){
            model.removeTransition(e);
        }
        List<Location> tobeRemovedLocs = new ArrayList<>();
        for(Location l : model.getLocations()){
            if(!coveredLocs.contains(l))
                tobeRemovedLocs.add(l);
        }
        for (Location l : tobeRemovedLocs){
            model.getLocations().remove(l);
        }
    }

    protected abstract TimedAutomaton extractModel(ProverEnvironment prover, Map<Symbol, Integer> discreteToInt) throws SolverException;

    protected abstract void addInitialLocationConstr(List<BooleanFormula> constraints, int traceIndex);

    private boolean incrementalCheck(ProverEnvironment prover, List<BooleanFormula> constraints, int index) throws InterruptedException, SolverException {
        if(batchSize > 0 && index % batchSize == 0){
            for(BooleanFormula c : constraints)
                prover.addConstraint(c);
            prover.push();
            constraints.clear();
            long start = System.currentTimeMillis();
            boolean isUnsolvable = prover.isUnsat();
            long end = System.currentTimeMillis();
            System.out.printf("SAT check at index %d: %b (%d millisecond) \n", index, !isUnsolvable,
                    end-start);

            return !isUnsolvable;
        }
        return true;
    }


    protected void addDeterminismConstraints(List<BooleanFormula> constraints) {
        addDeterminismConstraints(constraints, 0, nrTrans);
    }
    protected void addDeterminismConstraints(List<BooleanFormula> constraints, int alreadyConstrTrans, int newTransNr) {
        for(int i = 0; i < newTransNr; i ++){
            for(int j = alreadyConstrTrans; j < newTransNr; j ++){
                if(i < j) {
                    constraints.add(bmgr.implication(
                            bmgr.and(
                                    discreteVarEquals(fmgr.callUF(source, makeDiscreteNumber(i)), fmgr.callUF(source, makeDiscreteNumber(j))),
                                    discreteVarEquals(fmgr.callUF(label, makeDiscreteNumber(i)), fmgr.callUF(label, makeDiscreteNumber(j)))
                            ),
                            // ->
                            (halfStrictGuards ?
                            bmgr.or(
                                    guardLessOrEqual(fmgr.callUF(guard_above, makeDiscreteNumber(i)), fmgr.callUF(guard_below, makeDiscreteNumber(j))),
                                    guardLessOrEqual(fmgr.callUF(guard_above, makeDiscreteNumber(j)), fmgr.callUF(guard_below, makeDiscreteNumber(i)))
//                                    guardGreaterOrEquals(fmgr.callUF(guard_below, makeDiscreteNumber(i)), fmgr.callUF(guard_above, makeDiscreteNumber(j)))
                            ) :
                            bmgr.or(
                                    guardLessThan(fmgr.callUF(guard_above, makeDiscreteNumber(i)), fmgr.callUF(guard_below, makeDiscreteNumber(j))),
                                    guardGreaterThan(fmgr.callUF(guard_below, makeDiscreteNumber(i)), fmgr.callUF(guard_above, makeDiscreteNumber(j)))
                            ))
                    ));
                    if(!veryStrictIsolatedOutputs) {
                        constraints.add(bmgr.implication(
                                bmgr.and(
                                        discreteVarEquals(fmgr.callUF(source, makeDiscreteNumber(i)), fmgr.callUF(source, makeDiscreteNumber(j))),
                                        // stricter version of isolated outputs
                                        strictIsolatedOutputs ?
                                                bmgr.or(fmgr.callUF(is_output, makeDiscreteNumber(i)), fmgr.callUF(is_output, makeDiscreteNumber(j))) :
                                                bmgr.and(fmgr.callUF(is_output, makeDiscreteNumber(i)), fmgr.callUF(is_output, makeDiscreteNumber(j)))
                                ),
                                // ->
                                (halfStrictGuards ?
                                        bmgr.or(
                                                guardLessOrEqual(fmgr.callUF(guard_above, makeDiscreteNumber(i)), fmgr.callUF(guard_below, makeDiscreteNumber(j))),
                                                guardLessOrEqual(fmgr.callUF(guard_above, makeDiscreteNumber(j)), fmgr.callUF(guard_below, makeDiscreteNumber(i)))
                                        ) :
                                        bmgr.or(
                                                guardLessThan(fmgr.callUF(guard_above, makeDiscreteNumber(i)), fmgr.callUF(guard_below, makeDiscreteNumber(j))),
                                                guardGreaterThan(fmgr.callUF(guard_below, makeDiscreteNumber(i)), fmgr.callUF(guard_above, makeDiscreteNumber(j)))
                                        ))
                        ));
                    }
                }

            }
        }
    }

    protected void addGuardBoundConstraints(List<BooleanFormula> constraints) {
        addGuardBoundConstraints(constraints, 0, nrTrans, 0, nrStates);
    }
    protected void addGuardBoundConstraints(List<BooleanFormula> constraints, int alreadyConstrTrans, int newTransNr,
                                          int alreadyConstrState, int newStateNr) {
        // guards >= 0
        for(int i = alreadyConstrTrans; i < newTransNr; i++){
            constraints.add(guardGreaterOrEquals(fmgr.callUF(guard_below,makeDiscreteNumber(i)), makeGuardValue(0)));
            constraints.add(guardGreaterOrEquals(fmgr.callUF(guard_above,makeDiscreteNumber(i)), makeGuardValue(0)));
            // potentially remove max clock, not strictly needed

            constraints.add(bmgr.or(
                    guardLessOrEqual(fmgr.callUF(guard_above,makeDiscreteNumber(i)), makeGuardValue(maxGuardValue)),
                    // assumption: if bitvectors are used, we can display 1000
                    guardGreaterOrEquals(fmgr.callUF(guard_above,makeDiscreteNumber(i)), makeGuardValue(1000))
            ));
            constraints.add(bmgr.or(
                    guardLessOrEqual(fmgr.callUF(guard_below,makeDiscreteNumber(i)), makeGuardValue(maxGuardValue)),
                    guardEqual(fmgr.callUF(guard_below,makeDiscreteNumber(i)), makeGuardValue(0))
            ));

            // not strictly needed, but more constraints make the search space smaller
            constraints.add(
                    guardLessThan(fmgr.callUF(guard_below,makeDiscreteNumber(i)),
                            fmgr.callUF(guard_above,makeDiscreteNumber(i))));
        }
        // invariants >= 0
        for(int i = alreadyConstrState; i < newStateNr; i++){
            constraints.add(guardGreaterOrEquals(fmgr.callUF(invariant,makeDiscreteNumber(i)), makeGuardValue(0)));
        }
    }

    public static <T, E> T getKeyByValue(Map<T, E> map, E value) {
        for (Map.Entry<T, E> entry : map.entrySet()) {
            if (Objects.equals(value, entry.getValue())) {
                return entry.getKey();
            }
        }
        return null;
    }

    private void addStepConstraint(Map<Symbol, Integer> discreteToInt,
                                   List<BooleanFormula> constraints,
                                   int trace_index,
                                   int step_index,
                                   Pair<Symbol, Delay> step){
        addStepConstraint(discreteToInt,constraints,trace_index,step_index,step,false,false);
    }
    private void addStepConstraint(Map<Symbol, Integer> discreteToInt,
                                   List<BooleanFormula> constraints,
                                   int trace_index,
                                   int step_index,
                                   Pair<Symbol, Delay> step,
                                   boolean noTimeConstr,
                                   boolean noDiscreteConstr) {
        DType curr_loc = makeDiscreteVar("l_" + trace_index + "_" + step_index);
        CType curr_clock = makeTimeVar("c_" + trace_index + "_" + step_index);

        DType next_loc = makeDiscreteVar("l_" + trace_index + "_"+ (step_index+1));
        CType next_clock = makeTimeVar("c_" + trace_index + "_"+ (step_index+1));

        DType curr_trans = makeDiscreteVar("t_" + trace_index + "_" + step_index);

        CType clock_after_delay = makeTimeVar("c_" + trace_index + "_"+ (step_index) + "delay");
        DType actionDiscretized = makeDiscreteNumber(discreteToInt.get(step.getLeft()));
        CType delay = makeTimeValue(step.getRight().getValue());

        if(!noTimeConstr) {
            if (step_index == 0) {
                constraints.add(timeEqual(clock_after_delay, delay));
            } else {
                constraints.add(timeEqual(clock_after_delay, performDelay(curr_clock, delay)));
            }
        }
        if(!noDiscreteConstr){
            // bounds
            enforceStateBounds(constraints, next_loc, curr_trans);
        }

        if(step.getLeft().isOutput()){
            if(!noDiscreteConstr)
                constraints.add(fmgr.callUF(is_output, curr_trans));
            // invariant
            if(!noTimeConstr) {
                addkUrgentConstraint(constraints, curr_loc, curr_trans);
                // not strictly needed
                constraints.add(guardLessOrEqual(fmgr.callUF(guard_above,curr_trans),
                        fmgr.callUF(invariant,fmgr.callUF(source, curr_trans))));
            }

        } else{
            if(!noDiscreteConstr)
                constraints.add(bmgr.not(fmgr.callUF(is_output, curr_trans)));
        }
        // discrete
        DType currSource = fmgr.callUF(source, curr_trans);
        DType currTarget = fmgr.callUF(target, curr_trans);
        DType currLabel = fmgr.callUF(label, curr_trans);

        if(!noDiscreteConstr) {
            constraints.add(discreteVarEquals(currSource, curr_loc));
            constraints.add(discreteVarEquals(currTarget, next_loc));
            constraints.add(discreteVarEquals(currLabel, actionDiscretized));
        }
        if(!noTimeConstr) {
            // guard
            GType currGuardBelow = fmgr.callUF(guard_below, curr_trans);
            GType currGuardAbove = fmgr.callUF(guard_above, curr_trans);
            if(halfStrictGuards)
                constraints.add(timeLessThan(clock_after_delay, currGuardAbove));
            else
                constraints.add(timeLessOrEqual(clock_after_delay, currGuardAbove));
            constraints.add(timeGreaterOrEqual(clock_after_delay, currGuardBelow));

            // invariant
            GType currInv = fmgr.callUF(invariant, curr_loc);
            constraints.add(timeLessOrEqual(clock_after_delay, currInv));

            // reset
            constraints.add(bmgr.implication(fmgr.callUF(reset, curr_trans), timeEqual(next_clock, makeTimeValue(0.0))));
            constraints.add(bmgr.implication(bmgr.not(fmgr.callUF(reset, curr_trans)),
                    timeEqual(next_clock, clock_after_delay)));
            // add input enabledness if necessary

            // invariant after step
            constraints.add(timeLessOrEqual(next_clock, fmgr.callUF(invariant, next_loc)));
        }
    }

    protected abstract BooleanFormula timeLessThan(CType clock, GType guard);

    protected abstract BooleanFormula timeLessOrEqual(CType clock, GType guard);

    protected abstract BooleanFormula timeGreaterOrEqual(CType clock, GType guard);

    protected abstract DType makeDiscreteNumber(Integer number);

    protected abstract GType makeGuardValue(Integer number);

    protected abstract CType makeTimeValue(Double number);
    
    protected abstract DType makeDiscreteVar(String name);
    
    protected abstract CType makeTimeVar(String name);
    
    protected abstract void addkUrgentConstraint(List<BooleanFormula> constraints, DType curr_loc, DType curr_trans);

    protected abstract CType performDelay(CType curr_clock, CType delay);

    protected abstract void enforceStateBounds(List<BooleanFormula> constraints, DType next_loc, DType curr_trans);

    protected abstract BooleanFormula guardEqual(GType clockVar1, GType clockVar2);

    protected abstract BooleanFormula guardLessOrEqual(GType clockVar1, GType clockVar2);

    protected abstract BooleanFormula guardLessThan(GType clockVar1, GType clockVar2);

    protected abstract BooleanFormula guardGreaterThan(GType clockVar1, GType clockVar2);

    protected abstract BooleanFormula guardGreaterOrEquals(GType clockVar1, GType clockVar2);

    protected abstract BooleanFormula discreteVarEquals(DType discreteVar1, DType discreteVar2);

    protected abstract BooleanFormula timeEqual(CType clockVar1, CType clockVar2);
}
