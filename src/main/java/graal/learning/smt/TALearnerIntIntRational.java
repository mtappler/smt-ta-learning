package graal.learning.smt;

import com.google.common.collect.ImmutableList;
import graal.learning.models.*;
import org.sosy_lab.java_smt.SolverContextFactory;
import org.sosy_lab.java_smt.api.*;

import java.math.BigInteger;
import java.util.*;

public class TALearnerIntIntRational extends TALearnerGeneric<NumeralFormula.IntegerFormula,
        NumeralFormula.IntegerFormula,NumeralFormula.RationalFormula>{

    protected IntegerFormulaManager imgr;
    protected NumeralFormula.IntegerFormula k_urgency_f;
    protected RationalFormulaManager rmgr;

    public TALearnerIntIntRational(int nrStates, int nrTrans, int k_urgency, boolean bfsMode, int maxGuardValue, Set<Symbol> alphabet) {
        super(nrStates, nrTrans, k_urgency, bfsMode, maxGuardValue,alphabet);
    }

    @Override
    protected void initAdditional() {
        imgr = context.getFormulaManager().getIntegerFormulaManager();
        rmgr = context.getFormulaManager().getRationalFormulaManager();
        this.k_urgency_f = imgr.makeNumber(k_urgency);
    }

    @Override
    protected void initUF() {
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
    }


    @Override
    protected NumeralFormula.IntegerFormula makeDiscreteNumber(Integer number) {
        return imgr.makeNumber(number);
    }

    @Override
    protected NumeralFormula.IntegerFormula makeGuardValue(Integer number) {
        return imgr.makeNumber(number);
    }

    @Override
    protected NumeralFormula.RationalFormula makeTimeValue(Double number) {
        if(this.context.getSolverName().equals(SolverContextFactory.Solvers.CVC4) && number < 1e-3){
            number = 0.0; // there is an issue with parsing number in scientific notation with the CVC4 interface, so we round down
        }
        return rmgr.makeNumber(number);
    }

    @Override
    protected NumeralFormula.IntegerFormula makeDiscreteVar(String name) {
        return imgr.makeVariable(name);
    }

    @Override
    protected NumeralFormula.RationalFormula makeTimeVar(String name) {
        return rmgr.makeVariable(name);
    }

    @Override
    protected void addkUrgentConstraint(List<BooleanFormula> constraints, NumeralFormula.IntegerFormula curr_loc, NumeralFormula.IntegerFormula curr_trans) {
        constraints.add(imgr.lessOrEquals(fmgr.callUF(invariant, curr_loc),
                imgr.add(k_urgency_f, fmgr.callUF(guard_below, curr_trans))));
    }

    @Override
    protected NumeralFormula.RationalFormula performDelay(NumeralFormula.RationalFormula curr_clock, NumeralFormula.RationalFormula delay) {
        return rmgr.add(delay, curr_clock);
    }

    @Override
    protected void enforceStateBounds(List<BooleanFormula> constraints, NumeralFormula.IntegerFormula next_loc, NumeralFormula.IntegerFormula curr_trans) {
        constraints.add(imgr.lessOrEquals(imgr.makeNumber(0), next_loc));
        constraints.add(imgr.lessThan(next_loc, imgr.makeNumber(nrStates)));
        constraints.add(imgr.lessOrEquals(imgr.makeNumber(0), curr_trans));
        constraints.add(imgr.lessThan(curr_trans, imgr.makeNumber(nrTrans)));
    }


    @Override
    protected BooleanFormula guardEqual(NumeralFormula.IntegerFormula clockVar1, NumeralFormula.IntegerFormula clockVar2){
        return imgr.equal(clockVar1, clockVar2);
    }

    @Override
    protected BooleanFormula guardLessOrEqual(NumeralFormula.IntegerFormula clockVar1, NumeralFormula.IntegerFormula clockVar2){
        return imgr.lessOrEquals(clockVar1, clockVar2);
    }

    @Override
    protected BooleanFormula guardLessThan(NumeralFormula.IntegerFormula clockVar1, NumeralFormula.IntegerFormula clockVar2) {
        return imgr.lessThan(clockVar1, clockVar2);
    }

    @Override
    protected BooleanFormula guardGreaterThan(NumeralFormula.IntegerFormula clockVar1, NumeralFormula.IntegerFormula clockVar2) {
        return imgr.greaterThan(clockVar1, clockVar2);
    }

    @Override
    protected BooleanFormula guardGreaterOrEquals(NumeralFormula.IntegerFormula clockVar1, NumeralFormula.IntegerFormula clockVar2) {
        return imgr.greaterOrEquals(clockVar1, clockVar2);
    }
    @Override
    protected BooleanFormula discreteVarEquals(NumeralFormula.IntegerFormula discreteVar1, NumeralFormula.IntegerFormula discreteVar2){
        return imgr.equal(discreteVar1, discreteVar2);
    }

    @Override
    protected BooleanFormula timeEqual(NumeralFormula.RationalFormula clockVar1, NumeralFormula.RationalFormula clockVar2) {
        return rmgr.equal(clockVar1,clockVar2);
    }

    @Override
    protected void addInitialLocationConstr(List<BooleanFormula> constraints, int traceIndex) {
        NumeralFormula.IntegerFormula init_loc = imgr.makeVariable("l_" + traceIndex + "_0");
        constraints.add(imgr.equal(imgr.makeNumber(0), init_loc));
    }

    @Override
    protected BooleanFormula timeLessThan(NumeralFormula.RationalFormula clock, NumeralFormula.IntegerFormula guard) {
        return rmgr.lessThan(clock,guard);
    }

    @Override
    protected BooleanFormula timeLessOrEqual(NumeralFormula.RationalFormula clock, NumeralFormula.IntegerFormula guard) {
        return rmgr.lessOrEquals(clock,guard);
    }

    @Override
    protected BooleanFormula timeGreaterOrEqual(NumeralFormula.RationalFormula clock, NumeralFormula.IntegerFormula guard) {
        return rmgr.greaterOrEquals(clock,guard);
    }

    protected TimedAutomaton extractModel(ProverEnvironment prover, Map<Symbol, Integer> discreteToInt) throws SolverException {
        List<Location> locations = new ArrayList<>();
        List<Clock> clocks = new ArrayList<>();
        Clock singleClock = new Clock("c");
        clocks.add(singleClock);
        List<Symbol> alphabet = new ArrayList<>();
        alphabet.addAll(discreteToInt.keySet());
        Location initLocation = null;
        Map<Integer, Location> locationMap = new HashMap<>();
        for (int s= 0; s< nrStates;s++) {
            Location l = new Location("l" + s);
            if(s == 0)
                initLocation = l;
            locations.add(l);
            locationMap.put(s,l);
        }
        TimedAutomaton ta = new TimedAutomaton(locations,initLocation,alphabet,clocks);
        try (Model model = prover.getModel()) {
            for (int s= 0; s< nrStates;s++) {
//                dot_string.append(String.format("%s [shape=\"circle\" label=\"c <= %s\"];\n",s, .toString()));
                BigInteger invForSNumber = model.evaluate(fmgr.callUF(invariant, imgr.makeNumber(s)));
                ClockGuard invForS = new ClockGuard();
                invForS.add(singleClock,invForSNumber.intValue(), ClockConstraint.Relation.leq);
                ta.addInvariant(locationMap.get(s),invForS);
                Location sourceLoc = locationMap.get(s);
                for (int t=0; t<nrTrans;t++)
                {
                    if (model.evaluate(fmgr.callUF(source, imgr.makeNumber(t))).intValue()==s)
                    {
                        Symbol labelOnT = getKeyByValue(discreteToInt,model.evaluate(fmgr.callUF(label, imgr.makeNumber(t))).intValue());
                        if(labelOnT == null){
                            labelOnT = new Symbol("error", true);
                        }
                        boolean doesReset = model.evaluate(fmgr.callUF(reset, imgr.makeNumber(t))).booleanValue();
                        Location targetLoc = locationMap.get(model.evaluate(fmgr.callUF(target, imgr.makeNumber(t))).intValue());
                        int guardBelowK = model.evaluate(fmgr.callUF(guard_below, imgr.makeNumber(t))).intValue();
                        int guardAboveK = model.evaluate(fmgr.callUF(guard_above, imgr.makeNumber(t))).intValue();
                        ClockGuard guardOnT = new ClockGuard();
                        guardOnT.add(singleClock,guardBelowK, ClockConstraint.Relation.geq);
                        guardOnT.add(singleClock,guardAboveK, ClockConstraint.Relation.lt);
                        Set<Clock> resets = new HashSet<>();
                        if(doesReset)
                            resets.add(singleClock);
                        ta.addEdge(sourceLoc,targetLoc,guardOnT,resets,labelOnT);
                    }
                }
            }
            return ta;
        }
    }
}
