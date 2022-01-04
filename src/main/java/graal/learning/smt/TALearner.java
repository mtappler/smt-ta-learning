package graal.learning.smt;

import com.google.common.collect.ImmutableList;
import graal.learning.models.Delay;
import graal.learning.models.Symbol;
import graal.learning.models.Trace;
import org.apache.commons.lang3.tuple.Pair;
import org.sosy_lab.java_smt.SolverContextFactory;

import java.io.FileWriter;
import java.util.*;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.BasicLogManager;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.java_smt.api.*;

import org.sosy_lab.java_smt.api.NumeralFormula.IntegerFormula;
import org.sosy_lab.java_smt.api.SolverContext.ProverOptions;

public class TALearner {

    private static final long MAX_CLOCK = 20;
    private final int nrTrans;
    private final int nrStates;
    private final int k_urgency;
    private final boolean bfsMode;
    private SolverContext context;
    private IntegerFormulaManager imgr;
    private BooleanFormulaManager bmgr;
    private IntegerFormula k_urgency_f;
    private UFManager fmgr;
    private FunctionDeclaration<IntegerFormula> source;
    private FunctionDeclaration<IntegerFormula> target;
    private FunctionDeclaration<IntegerFormula> label;
    private FunctionDeclaration<BooleanFormula> reset;
    private FunctionDeclaration<BooleanFormula> is_output;
    private FunctionDeclaration<IntegerFormula> invariant;
    private FunctionDeclaration<IntegerFormula> guard_below;
    private FunctionDeclaration<IntegerFormula> guard_above;
    private String modelFileName;
    private int batchSize = -1;

    public TALearner(int nrStates, int nrTrans, int k_urgency, boolean bfsMode){
        this.nrStates = nrStates;
        this.nrTrans = nrTrans;
        this.k_urgency = k_urgency;
        this.bfsMode = bfsMode;
    }
    public void init(SolverContextFactory.Solvers solver, String modelFileName) throws InvalidConfigurationException {
        this.modelFileName = modelFileName;
        Configuration config = Configuration.defaultConfiguration();
        LogManager logger = BasicLogManager.create(config);
        ShutdownNotifier notifier = ShutdownNotifier.createDummy();
        context = SolverContextFactory.createSolverContext(config, logger, notifier, solver);
        imgr = context.getFormulaManager().getIntegerFormulaManager();
        bmgr = context.getFormulaManager().getBooleanFormulaManager();
        fmgr = context.getFormulaManager().getUFManager();

        source = fmgr.declareUF("source",
                FormulaType.IntegerType, ImmutableList.of(FormulaType.IntegerType));
        target =
                fmgr.declareUF("target", FormulaType.IntegerType,ImmutableList.of(FormulaType.IntegerType));
        label = fmgr.declareUF("label",
                FormulaType.IntegerType,ImmutableList.of(FormulaType.IntegerType));
        guard_above = fmgr.declareUF("guard_above",
                FormulaType.IntegerType,ImmutableList.of(FormulaType.IntegerType));
        guard_below = fmgr.declareUF("guard_below",
                FormulaType.IntegerType,ImmutableList.of(FormulaType.IntegerType));
        invariant = fmgr.declareUF("invariant",
                FormulaType.IntegerType,ImmutableList.of(FormulaType.IntegerType));
        is_output = fmgr.declareUF("is_output",
                FormulaType.BooleanType,ImmutableList.of(FormulaType.IntegerType));
        reset = fmgr.declareUF("reset",
                FormulaType.BooleanType,ImmutableList.of(FormulaType.IntegerType));
        this.k_urgency_f = imgr.makeNumber(k_urgency);
    }
    public void learn(List<Trace> traces) throws InvalidConfigurationException, InterruptedException, SolverException {

        try (ProverEnvironment prover = context.newProverEnvironment(ProverOptions.GENERATE_MODELS)) {
            Set<Symbol> alphabet = new HashSet<>();
            traces.forEach(t -> alphabet.addAll(t.usedDiscreteActions()));
            Map<Symbol,Integer> discreteToInt = new HashMap<>();
            Iterator<Symbol> alphabetIter = alphabet.iterator();
            int discreteId = 0;

            List<BooleanFormula> constraints = new ArrayList<>();

            while (alphabetIter.hasNext()) {
                Symbol alphabetElem = alphabetIter.next();
                int intId = discreteId ++;
                discreteToInt.put(alphabetElem, intId);
                // there is at least one transition for each label
                constraints.add(imgr.equal(fmgr.callUF(label, imgr.makeNumber(intId)), imgr.makeNumber(intId)));
            }

            // guards >= 0
            // and max guards
            for(int i = 0; i < nrTrans; i++){
                constraints.add(imgr.greaterOrEquals(fmgr.callUF(guard_below,imgr.makeNumber(i)), imgr.makeNumber(0)));
                constraints.add(imgr.greaterOrEquals(fmgr.callUF(guard_above,imgr.makeNumber(i)), imgr.makeNumber(0)));

                constraints.add(bmgr.or(
                        imgr.lessOrEquals(fmgr.callUF(guard_above,imgr.makeNumber(i)), imgr.makeNumber(MAX_CLOCK)),
                        imgr.greaterOrEquals(fmgr.callUF(guard_above,imgr.makeNumber(i)), imgr.makeNumber(10000))
                ));
                constraints.add(bmgr.or(
                        imgr.lessOrEquals(fmgr.callUF(guard_below,imgr.makeNumber(i)), imgr.makeNumber(MAX_CLOCK)),
                        imgr.equal(fmgr.callUF(guard_below,imgr.makeNumber(i)), imgr.makeNumber(0))
                ));
            }
            // invariants >= 0
            for(int i = 0; i < nrStates; i++){
                constraints.add(imgr.greaterOrEquals(fmgr.callUF(invariant,imgr.makeNumber(i)), imgr.makeNumber(0)));
            }
            // determinism
            for(int i = 0; i < nrTrans; i ++){
                for(int j = 0; j < nrTrans; j ++){
                    if(i < j) {
                        constraints.add(bmgr.implication(
                                bmgr.and(
                                        imgr.equal(fmgr.callUF(source, imgr.makeNumber(i)), fmgr.callUF(source, imgr.makeNumber(j))),
                                        imgr.equal(fmgr.callUF(label, imgr.makeNumber(i)), fmgr.callUF(label, imgr.makeNumber(j)))
                                ),
                                // ->
                                bmgr.or(
                                        imgr.lessThan(fmgr.callUF(guard_above, imgr.makeNumber(i)), fmgr.callUF(guard_below, imgr.makeNumber(j))),
                                        imgr.greaterThan(fmgr.callUF(guard_below, imgr.makeNumber(i)), fmgr.callUF(guard_above, imgr.makeNumber(j)))
                                )));
                        constraints.add(bmgr.implication(
                                bmgr.and(
                                        imgr.equal(fmgr.callUF(source, imgr.makeNumber(i)), fmgr.callUF(source, imgr.makeNumber(j))),
                                        bmgr.and(fmgr.callUF(is_output, imgr.makeNumber(i)), fmgr.callUF(is_output, imgr.makeNumber(j)))
                                ),
                                // ->
                                bmgr.or(
                                        imgr.lessThan(fmgr.callUF(guard_above, imgr.makeNumber(i)), fmgr.callUF(guard_below, imgr.makeNumber(j))),
                                        imgr.greaterThan(fmgr.callUF(guard_below, imgr.makeNumber(i)), fmgr.callUF(guard_above, imgr.makeNumber(j)))
                                )));
                    }

                }
            }
            if(bfsMode){
                int stepIndex = 0;
                while(true){
                    boolean addedConstr = false;
                    int traceIndex = 0;
                    for (Trace t : traces){
                        if(stepIndex == 0){
                            IntegerFormula init_loc = imgr.makeVariable("l_" + traceIndex + "_0");
                            constraints.add(imgr.equal(imgr.makeNumber(0), init_loc));
                        }
                        if(stepIndex < t.length()) {
                            Pair<Symbol, Delay> step = t.get(stepIndex);
                            addedConstr = true;
                            addStepConstraint(discreteToInt, constraints, traceIndex,
                                    stepIndex, step);
                        }
                    }
                    stepIndex++;

                    incrementalCheck(prover, constraints, stepIndex);
                    if(!addedConstr)
                        break;
                }
            } else {
                int traceIndex = 0;
                for (Trace t : traces) {
                    int stepIndex = 0;
                    IntegerFormula init_loc = imgr.makeVariable("l_" + traceIndex + "_0");
                    constraints.add(imgr.equal(imgr.makeNumber(0), init_loc));

                    for (Pair<Symbol, Delay> step : t.getTrace()) {
                        addStepConstraint(discreteToInt, constraints, traceIndex,
                                stepIndex, step);
                        stepIndex++;
                    }
                    traceIndex++;
                    incrementalCheck(prover, constraints, traceIndex);
                }
            }
//            prover.push(bmgr.and(constraints));
//
            for(BooleanFormula c : constraints)
                prover.addConstraint(c);
            System.out.println("Starting check");
            boolean isUnsolvable = prover.isUnsat();
            if (isUnsolvable) {
                System.out.println("Not satisfiable");
            }
            else {
//                System.out.println(prover.getModel().toString());
                try (Model model = prover.getModel()) {
                    StringBuilder dot_string = new StringBuilder();
                    dot_string.append("digraph g {\n" +
                            "                        __start0 [label=\"\" shape=\"none\"];\n");
                    for (int s= 0; s< nrStates;s++) {
                        System.out.println(s + ": c <= " + model.evaluate(fmgr.callUF(invariant, imgr.makeNumber(s))));
                        dot_string.append(String.format("%s [shape=\"circle\" label=\"c <= %s\"];\n",s, model.evaluate(fmgr.callUF(invariant, imgr.makeNumber(s))).toString()));
                        for (int t=0; t<nrTrans;t++)
                        {
                            if (model.evaluate(fmgr.callUF(source, imgr.makeNumber(t))).intValue()==s)
                            {

                                String t_label=getKeyByValue(discreteToInt,model.evaluate(fmgr.callUF(label, imgr.makeNumber(t))).intValue()).getSymbol();
                                String reset_str = model.evaluate(fmgr.callUF(reset, imgr.makeNumber(t))).booleanValue() ? "true" : "false";


                                dot_string.append(String.format("%s -> %s [label=\"%s,%s <= c <= %s,res=%s\"];\n",model.evaluate(fmgr.callUF(source, imgr.makeNumber(t))),
                                        model.evaluate(fmgr.callUF(target, imgr.makeNumber(t))),
                                        t_label,
                                        model.evaluate(fmgr.callUF(guard_below, imgr.makeNumber(t))),
                                        model.evaluate(fmgr.callUF(guard_above, imgr.makeNumber(t))),
                                        reset_str));
                                System.out.println(String.format("trans %s: %s -> %s [label=\"%s,%s <= c <= %s,res=%s\"];\n",t,model.evaluate(fmgr.callUF(source, imgr.makeNumber(t))),
                                        model.evaluate(fmgr.callUF(target, imgr.makeNumber(t))),
                                        t_label,
                                        model.evaluate(fmgr.callUF(guard_below, imgr.makeNumber(t))),
                                        model.evaluate(fmgr.callUF(guard_above, imgr.makeNumber(t))),
                                        reset_str));

                            }
                        }
                    }
                    dot_string.append(String.format("__start0 -> %d; \n }",0));

//                    System.out.println(dot_string);
                    try {
                        FileWriter myWriter = new FileWriter(modelFileName);
                        myWriter.write(dot_string.toString());
                        myWriter.close();
                    } catch (Exception e) {
                        System.out.println("Could not write file.");
                        e.printStackTrace();
                    }
                }
            }

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
                                   Pair<Symbol, Delay> step) {
        IntegerFormula curr_loc = imgr.makeVariable("l_" + trace_index + "_" + step_index);
        IntegerFormula curr_clock = imgr.makeVariable("c_" + trace_index + "_" + step_index);

        IntegerFormula next_loc = imgr.makeVariable("l_" + trace_index + "_"+ (step_index+1));
        IntegerFormula next_clock = imgr.makeVariable("c_" + trace_index + "_"+ (step_index+1));

        IntegerFormula curr_trans = imgr.makeVariable("t_" + trace_index + "_" + step_index);

        IntegerFormula clock_after_delay = imgr.makeVariable("c_" + trace_index + "_"+ (step_index) + "delay");
        IntegerFormula stepInt = imgr.makeNumber(discreteToInt.get(step.getLeft()));
        IntegerFormula delay = imgr.makeNumber(step.getRight().getValue());

        if(step_index == 0){
            constraints.add(imgr.equal(clock_after_delay,delay));
        } else {
            constraints.add(imgr.equal(clock_after_delay,imgr.add(delay,curr_clock)));
        }

        // invariant
        constraints.add(imgr.lessOrEquals(clock_after_delay,fmgr.callUF(invariant, curr_loc)));

        // bounds
        constraints.add(imgr.greaterOrEquals(next_loc,imgr.makeNumber(0)));
        constraints.add(imgr.lessThan(next_loc, imgr.makeNumber(nrStates)));
        constraints.add(imgr.greaterOrEquals(curr_trans,imgr.makeNumber(0)));
        constraints.add(imgr.lessThan(curr_trans, imgr.makeNumber(nrTrans)));
        if(step.getLeft().isOutput()){
            constraints.add(fmgr.callUF(is_output, curr_trans));
        } else{
            constraints.add(bmgr.not(fmgr.callUF(is_output, curr_trans)));
        }

        // discrete check
        constraints.add(imgr.equal(fmgr.callUF(source, curr_trans), curr_loc));
        constraints.add(imgr.equal(fmgr.callUF(label, curr_trans), stepInt));

        // guard check
        constraints.add(imgr.lessOrEquals(clock_after_delay,fmgr.callUF(guard_above, curr_trans)));
        constraints.add(imgr.lessOrEquals(fmgr.callUF(guard_below, curr_trans),clock_after_delay));

        // invariant
        if(step.getLeft().isOutput()){
            constraints.add(imgr.lessOrEquals(fmgr.callUF(invariant, curr_loc),
                    imgr.add(fmgr.callUF(guard_below, curr_trans), k_urgency_f)));
        }

        // make step
        constraints.add(imgr.equal(fmgr.callUF(target, curr_trans), next_loc));

        // reset
        constraints.add(bmgr.implication(fmgr.callUF(reset,curr_trans), imgr.equal(next_clock, imgr.makeNumber(0))));
        constraints.add(bmgr.implication(bmgr.not(fmgr.callUF(reset,curr_trans)), imgr.equal(next_clock, clock_after_delay)));
        // add input enabledness if necessary

        // invariant after step
        constraints.add(imgr.lessOrEquals(next_clock,fmgr.callUF(invariant, next_loc)));
    }
    private void incrementalCheck(ProverEnvironment prover, List<BooleanFormula> constraints, int traceIndex) throws InterruptedException, SolverException {
        if(batchSize > 0 && traceIndex % batchSize == 0){
            for(BooleanFormula c : constraints)
                prover.addConstraint(c);
            prover.push();
            constraints.clear();
            long start = System.currentTimeMillis();
            boolean isUnsolvable = prover.isUnsat();
            long end = System.currentTimeMillis();
            System.out.printf("SAT check at index %d: %b (%d millisecond) \n", traceIndex, !isUnsolvable,
                    end-start);
        }
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

}
