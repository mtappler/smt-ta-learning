package graal.learning.smt.learner_factory;

import graal.learning.models.Symbol;
import graal.learning.smt.TALearnerGeneric;
import org.sosy_lab.java_smt.api.Formula;

import java.util.Set;

public interface AbstractTALearnerFactory<DType extends Formula,GType extends Formula, CType extends Formula> {
    TALearnerGeneric<DType, GType, CType > create(int nrStates, int nrTrans, int k_urgency, boolean bfsMode,
                                                  int maxGuardValue, Set<Symbol> alphabet, int roundingFactor);
}
