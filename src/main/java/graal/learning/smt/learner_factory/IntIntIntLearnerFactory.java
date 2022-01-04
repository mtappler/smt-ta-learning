package graal.learning.smt.learner_factory;

import graal.learning.models.Symbol;
import graal.learning.smt.TALearnerGeneric;
import graal.learning.smt.TALearnerIntIntInt;
import graal.learning.smt.TALearnerIntIntRational;
import org.sosy_lab.java_smt.api.NumeralFormula;

import java.util.Set;

public class IntIntIntLearnerFactory implements AbstractTALearnerFactory<NumeralFormula.IntegerFormula,
        NumeralFormula.IntegerFormula,NumeralFormula.IntegerFormula>{

    @Override
    public TALearnerGeneric<NumeralFormula.IntegerFormula, NumeralFormula.IntegerFormula, NumeralFormula.IntegerFormula>
    create(int nrStates, int nrTrans, int k_urgency, boolean bfsMode, int maxGuardValue, Set<Symbol> alphabet,
           int roundingFactor) {
        return new TALearnerIntIntInt(nrStates, nrTrans, k_urgency, bfsMode, maxGuardValue,alphabet,roundingFactor);
    }
}
