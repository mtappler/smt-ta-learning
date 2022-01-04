package graal.learning.smt;

import com.google.common.collect.ImmutableList;
import graal.learning.models.*;
import org.sosy_lab.java_smt.api.*;

import java.io.FileWriter;
import java.math.BigInteger;
import java.util.*;

public class TALearnerBVBVFP extends TALearnerGeneric<BitvectorFormula, BitvectorFormula,FloatingPointFormula>{
    private BitvectorFormula k_urgency_bv;
    private int bvSizeDiscrete;
    private int bvSizeTimed;
    private boolean useSigned = false;
    private BitvectorFormulaManager bvMgr;
    private FloatingPointFormulaManager fpMgr;
    private FormulaType.FloatingPointType fpType;

    public TALearnerBVBVFP(int nrStates, int nrTrans, int k_urgency, boolean bfsMode, int maxGuardValue, int bvSizeTimed,
                           Set<Symbol> alphabet) {
        super(nrStates, nrTrans, k_urgency, bfsMode, maxGuardValue, alphabet);
        // assume that #labels < nrTrans
        this.bvSizeDiscrete = (int) Math.ceil(Math.log(Math.max(nrStates,nrTrans)) / Math.log(2)) + 1;
        this.bvSizeTimed = bvSizeTimed;
    }

    @Override
    protected void initAdditional() {
        this.bvMgr = context.getFormulaManager().getBitvectorFormulaManager();
        this.fpMgr = context.getFormulaManager().getFloatingPointFormulaManager();
        this.k_urgency_bv = bvMgr.makeBitvector(bvSizeTimed,k_urgency);
    }

    @Override
    protected void initUF() {
        fpType = FormulaType.getFloatingPointType(4,10); // simply use half the IEEE754 specs for single prec.
        source = fmgr.declareUF("source",
                FormulaType.getBitvectorTypeWithSize(bvSizeDiscrete), ImmutableList.of(FormulaType.getBitvectorTypeWithSize(bvSizeDiscrete)));
        target =
                fmgr.declareUF("target", FormulaType.getBitvectorTypeWithSize(bvSizeDiscrete),ImmutableList.of(FormulaType.getBitvectorTypeWithSize(bvSizeDiscrete)));
        label = fmgr.declareUF("label",
                FormulaType.getBitvectorTypeWithSize(bvSizeDiscrete),ImmutableList.of(FormulaType.getBitvectorTypeWithSize(bvSizeDiscrete)));
        guard_above = fmgr.declareUF("guard_above",
                FormulaType.getBitvectorTypeWithSize(bvSizeTimed),ImmutableList.of(FormulaType.getBitvectorTypeWithSize(bvSizeDiscrete)));
        guard_below = fmgr.declareUF("guard_below",
                FormulaType.getBitvectorTypeWithSize(bvSizeTimed),ImmutableList.of(FormulaType.getBitvectorTypeWithSize(bvSizeDiscrete)));
        invariant = fmgr.declareUF("invariant",
                FormulaType.getBitvectorTypeWithSize(bvSizeTimed),ImmutableList.of(FormulaType.getBitvectorTypeWithSize(bvSizeDiscrete)));

        is_output = fmgr.declareUF("is_output",
                FormulaType.BooleanType,ImmutableList.of(FormulaType.getBitvectorTypeWithSize(bvSizeDiscrete)));
        reset = fmgr.declareUF("reset",
                FormulaType.BooleanType,ImmutableList.of(FormulaType.getBitvectorTypeWithSize(bvSizeDiscrete)));
    }

    @Override
    protected BitvectorFormula makeDiscreteNumber(Integer number) {
        return bvMgr.makeBitvector(bvSizeDiscrete,number);
    }
    protected BitvectorFormula makeGuardValue(Integer number) {
        return bvMgr.makeBitvector(bvSizeTimed,number);
    }

    @Override
    protected FloatingPointFormula makeTimeValue(Double number) {
        return fpMgr.makeNumber(number.intValue(),fpType);
    }

    protected BitvectorFormula makeDiscreteVar(String name) {
        return bvMgr.makeVariable(bvSizeDiscrete,name);
    }
    protected FloatingPointFormula makeTimeVar(String name) {
        return fpMgr.makeVariable(name,fpType);
    }
    protected void addkUrgentConstraint(List<BooleanFormula> constraints, BitvectorFormula curr_loc, BitvectorFormula curr_trans) {
        constraints.add(bvMgr.lessOrEquals(fmgr.callUF(invariant, curr_loc),
                bvMgr.add(k_urgency_bv, fmgr.callUF(guard_below, curr_trans)),useSigned));
    }

    @Override
    protected FloatingPointFormula performDelay(FloatingPointFormula curr_clock, FloatingPointFormula delay) {
        return fpMgr.add(delay, curr_clock);
    }

    protected void enforceStateBounds(List<BooleanFormula> constraints, BitvectorFormula next_loc, BitvectorFormula curr_trans) {
        constraints.add(bvMgr.lessOrEquals(bvMgr.makeBitvector(bvSizeDiscrete,0), next_loc,useSigned));
        constraints.add(bvMgr.lessThan(next_loc, bvMgr.makeBitvector(bvSizeDiscrete,nrStates),useSigned));
        constraints.add(bvMgr.lessOrEquals(bvMgr.makeBitvector(bvSizeDiscrete,0), curr_trans,useSigned));
        constraints.add(bvMgr.lessThan(curr_trans, bvMgr.makeBitvector(bvSizeDiscrete,nrTrans),useSigned));
    }

    protected BooleanFormula clockEqualZeroFormula(BitvectorFormula clockVar){
        return bvMgr.equal(clockVar, bvMgr.makeBitvector(bvSizeTimed,0));
    }
    @Override
    protected BooleanFormula guardEqual(BitvectorFormula clockVar1, BitvectorFormula clockVar2){
        return bvMgr.equal(clockVar1, clockVar2);
    }
    @Override
    protected BooleanFormula guardLessOrEqual(BitvectorFormula clockVar1, BitvectorFormula clockVar2){
        return bvMgr.lessOrEquals(clockVar1, clockVar2,useSigned);
    }

    @Override
    protected BooleanFormula guardLessThan(BitvectorFormula clockVar1, BitvectorFormula clockVar2) {
        return bvMgr.lessThan(clockVar1, clockVar2,useSigned);
    }

    @Override
    protected BooleanFormula guardGreaterThan(BitvectorFormula clockVar1, BitvectorFormula clockVar2) {
        return bvMgr.greaterThan(clockVar1, clockVar2,useSigned);
    }

    @Override
    protected BooleanFormula guardGreaterOrEquals(BitvectorFormula clockVar1, BitvectorFormula clockVar2) {
        return bvMgr.greaterOrEquals(clockVar1,clockVar2,useSigned);
    }

    @Override
    protected BooleanFormula discreteVarEquals(BitvectorFormula discreteVar1, BitvectorFormula discreteVar2){
        return bvMgr.equal(discreteVar1, discreteVar2);
    }

    @Override
    protected BooleanFormula timeEqual(FloatingPointFormula clockVar1, FloatingPointFormula clockVar2) {
        return fpMgr.equalWithFPSemantics(clockVar1,clockVar2);
    }

    @Override
    protected void addInitialLocationConstr(List<BooleanFormula> constraints, int traceIndex) {
        BitvectorFormula init_loc = bvMgr.makeVariable(bvSizeDiscrete,"l_" + traceIndex + "_0");
        constraints.add(bvMgr.equal(bvMgr.makeBitvector(bvSizeDiscrete,0), init_loc));
    }

    @Override
    protected BooleanFormula timeLessThan(FloatingPointFormula clock, BitvectorFormula guard) {
        return fpMgr.lessThan(clock,fpMgr.castFrom(guard,useSigned,fpType));
    }

    @Override
    protected BooleanFormula timeLessOrEqual(FloatingPointFormula clock, BitvectorFormula guard) {
        return fpMgr.lessOrEquals(clock,fpMgr.castFrom(guard,useSigned,fpType));
    }

    @Override
    protected BooleanFormula timeGreaterOrEqual(FloatingPointFormula clock, BitvectorFormula guard) {
        return fpMgr.greaterOrEquals(clock,fpMgr.castFrom(guard,useSigned,fpType));
    }

    protected TimedAutomaton extractModel(ProverEnvironment prover, Map<Symbol, Integer> discreteToInt) throws SolverException {
        // copied from rational learner TODO refactor
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
                BigInteger invForSNumber = model.evaluate(fmgr.callUF(invariant,  bvMgr.makeBitvector(bvSizeDiscrete,s)));
                ClockGuard invForS = new ClockGuard();
                invForS.add(singleClock,invForSNumber.intValue(), ClockConstraint.Relation.leq);
                ta.addInvariant(locationMap.get(s),invForS);
                Location sourceLoc = locationMap.get(s);
                for (int t=0; t<nrTrans;t++)
                {
                    if (model.evaluate(fmgr.callUF(source, bvMgr.makeBitvector(bvSizeDiscrete,t))).intValue()==s)
                    {
                        Symbol labelOnT = getKeyByValue(discreteToInt,model.evaluate(fmgr.callUF(label, bvMgr.makeBitvector(bvSizeDiscrete,t))).intValue());
                        if(labelOnT == null){
                            labelOnT = new Symbol("error", true);
                        }
                        boolean doesReset = model.evaluate(fmgr.callUF(reset,  bvMgr.makeBitvector(bvSizeDiscrete,t))).booleanValue();
                        Location targetLoc = locationMap.get(model.evaluate(fmgr.callUF(target, bvMgr.makeBitvector(bvSizeDiscrete,t))).intValue());
                        int guardBelowK = model.evaluate(fmgr.callUF(guard_below, bvMgr.makeBitvector(bvSizeDiscrete,t))).intValue();
                        int guardAboveK = model.evaluate(fmgr.callUF(guard_above, bvMgr.makeBitvector(bvSizeDiscrete,t))).intValue();
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

    public void setUseSigned(boolean useSigned) {
        this.useSigned = useSigned;
    }
}
