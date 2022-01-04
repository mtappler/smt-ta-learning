package graal.learning.smt.learner_factory;

import graal.learning.models.Symbol;
import graal.learning.smt.TALearnerBVBV;
import graal.learning.smt.TALearnerBVBVFP;
import graal.learning.smt.TALearnerGeneric;
import org.sosy_lab.java_smt.api.BitvectorFormula;
import org.sosy_lab.java_smt.api.FloatingPointFormula;

import java.util.Set;

public class BVBVFPLearnerFactory implements AbstractTALearnerFactory<BitvectorFormula, BitvectorFormula, FloatingPointFormula>{

    @Override
    public TALearnerGeneric<BitvectorFormula, BitvectorFormula,FloatingPointFormula>
    create(int nrStates, int nrTrans, int k_urgency, boolean bfsMode, int maxGuardValue, Set<Symbol> alphabet,
           int roundingFactor) {
        int bvSizeTimed = 20; // fixed to 20 bits
        return new TALearnerBVBVFP(nrStates, nrTrans, k_urgency, bfsMode, maxGuardValue,bvSizeTimed,alphabet);
    }
}
