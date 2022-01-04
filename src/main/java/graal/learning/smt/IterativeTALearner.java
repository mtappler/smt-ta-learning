package graal.learning.smt;

import graal.learning.models.Symbol;
import graal.learning.models.Trace;
import graal.learning.smt.learner_factory.AbstractTALearnerFactory;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.java_smt.SolverContextFactory;
import org.sosy_lab.java_smt.api.Formula;

import java.util.List;
import java.util.Set;

public class IterativeTALearner<DType extends Formula,GType extends Formula, CType extends Formula>  {

    public IterativeTALearner(AbstractTALearnerFactory<DType, GType, CType> learnerFactory, String modelFileName,
                              int maxNrLocations, int maxNrEdges, double edgesPerLoc, int k_urgency, boolean bfsMode,
                              int maxGuardValue, Set<Symbol> alphabet, SolverContextFactory.Solvers solver,
                              int batchSize, List<Trace> trainingData, int roundingFactor) {
        this.maxNrLocations = maxNrLocations;
        this.maxNrEdges = maxNrEdges;
        this.edgesPerLoc = edgesPerLoc;
        this.k_urgency = k_urgency;
        this.bfsMode = bfsMode;
        this.maxGuardValue = maxGuardValue;
        this.alphabet = alphabet;
        this.solver = solver;
        this.modelFileName = modelFileName;
        this.trainingData = trainingData;
        this.learnerFactory = learnerFactory;
        this.batchSize = batchSize;
        this.roundingFactor = roundingFactor;
    }
    private int minNrLocs = 1;
    private int maxNrLocations = 0;
    private int maxNrEdges = 0;
    private double edgesPerLoc = 0.0;
    private int k_urgency = 0;
    private boolean bfsMode = false;
    private int maxGuardValue = 0;
    private Set<Symbol> alphabet = null;
    private SolverContextFactory.Solvers solver;
    private String modelFileName = null;
    private List<Trace> trainingData = null;
    private AbstractTALearnerFactory<DType,GType,CType> learnerFactory = null;
    private int batchSize = 0;
    private final int roundingFactor;

    public String getModelFileName() {
        return modelFileName;
    }
    public int learn() throws Exception {
        int nrLocs = 1; //minNrLocs;
        int nrEdges = 1;
        do{
            nrEdges = (int) Math.round(nrLocs * edgesPerLoc);
            System.out.println("Learning with location bound of " + nrLocs  + " and edge bound of " + nrEdges);
            TALearnerGeneric<DType, GType, CType> taLearner =
                    learnerFactory.create(nrLocs, nrEdges, k_urgency, bfsMode, maxGuardValue, alphabet, roundingFactor);
            taLearner.setBatchSize(batchSize);
            taLearner.init(solver,modelFileName);
            if(taLearner.learn(trainingData)){
                return nrLocs;
            }
            nrLocs ++;
        } while (nrLocs <= maxNrLocations && nrEdges <= maxNrEdges);
        return Integer.MAX_VALUE;
    }

    public int getMinNrLocs() {
        return minNrLocs;
    }

    public void setMinNrLocs(int minNrLocs) {
        this.minNrLocs = minNrLocs;
    }
}
