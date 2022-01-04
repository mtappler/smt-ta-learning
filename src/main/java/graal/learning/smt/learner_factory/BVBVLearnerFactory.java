package graal.learning.smt.learner_factory;

import graal.learning.models.Symbol;
import graal.learning.smt.TALearnerBVBV;
import graal.learning.smt.TALearnerGeneric;
import graal.learning.smt.TALearnerIntIntInt;
import org.sosy_lab.java_smt.api.BitvectorFormula;
import org.sosy_lab.java_smt.api.NumeralFormula;

import java.util.Set;

public class BVBVLearnerFactory implements AbstractTALearnerFactory<BitvectorFormula, BitvectorFormula,BitvectorFormula>{

    @Override
    public TALearnerGeneric<BitvectorFormula, BitvectorFormula,BitvectorFormula>
    create(int nrStates, int nrTrans, int k_urgency, boolean bfsMode, int maxGuardValue, Set<Symbol> alphabet,
           int roundingFactor) {
        int bvSizeTimed = 20; // fixed to 20 bits
        return new TALearnerBVBV(nrStates, nrTrans, k_urgency, bfsMode, maxGuardValue,bvSizeTimed,alphabet,roundingFactor);
    }
}
