# SMT-Based Learning of Timed Automata in Java

This repository contains the implementation of 
an approach to learning timed automata from timed 
traces via SMT solving. A Maven file facilitates 
executing the learning technique and reproducing 
the experiments discussed in the paper 
"Timed Automata Learning via SMT solving". 
The repository also contains reference data, 
including runtime measurement results
and learned automata, presented in the paper.

## Overview
Timed automata are formal models of real-time 
systems that produce timed traces. This kind of 
traces are sequences of input and output actions 
with real-valued delays between them. Our implemented
approach formulates the learning problem as an 
SMT formula containing three types of constraints:
1. Language inclusion/Acceptance of given timed 
traces
2. Determinism constraints to avoid learning overly 
general models
3. Bound constraints to learn models with a maximum 
number of locations and edges

We evaluate the approach by generating timed traces
from known timed automata and trying to learn 
models that are consistent with the generated traces.
While we treat the known automata as black boxes
during learning (we only consider timed traces
generated via simulation), using such known automata
aids in the analysis of learning results. For 
instance, we compute metrics such as precision and 
recall, which requires knowledge of traces that should
not be accepted. To ease reproducing our experiments
we provide the timed-trace training data that we 
used.

## Setup
This project has two main dependencies:
* JavaSMT via Maven
* Uppaal for trace generation to evaluate learned models

### JavaSMT and Maven
We use [JavaSMT](https://github.com/sosy-lab/java-smt)
to work with different SMT solvers using a unified API.
The Maven POM file in this repository contains Linux-specific 
dependencies for JavaSMT itself and all available backends. 
Thus, the setup on Linux generally just amounts to 
1. Installing Maven and 
2. Compiling the Maven project via ```mvn compile``` on the 
command line. This will download the required dependencies, 
including native binaries, and compile the project. 

We developed and tested the project on Ubuntu Linux.
For other operating systems, additional setup steps may be
required, for instance, to download and install shared 
libraries. For a quick start, it may be simplest to start 
working with SMTInterpol (see below how to configure
experiments), as it is a Java-based SMT solver without
native dependencies. For more information, we refer to the
[JavaSMT](https://github.com/sosy-lab/java-smt) repository.

### Uppaal for Evaluation
We use a new version of the real-time model checker Uppaal
to generate timed traces for evaluating learned timed automata
models.
To reproduce the experiments presented in the paper:
1. Download the new version of Uppaal from this
[link](http://people.cs.aau.dk/~ulrik/submissions/874325/FMICS2021.zip).
2. Extract the zip archive
3. Specify the path to the unzipped Uppaal command line program ```verifyta``` 
in the properties file
```paths.prop``` (e.g., ```external_bin/uppaal64-4.1.20-prototype/bin-Linux/verifyta```)

Additionally, download version 1.1 of the Document Type Definition (DTD)
for Uppaal's XML files and move it into the directory containing the randomly
generated timed automaton. That is,
1. Download the DTD file from this [link](http://www.it.uu.se/research/group/darts/uppaal/flat-1_1.dtd)
2. Move it into the directory ```eval_models/random```

## Reproducing Experiments
After compiling the project using Maven, experiments can
be performed with the help of Maven, where we use the Exec Maven Plugin.
For this purpose, issue 
```
mvn exec:exec
```
on the command line. This will read an experiment configuration
from the Java-properties file ```experiment-properties.prop```
and start running the experiments. After producing console
output informing about the progress of learning, the learning
results, such as learning runtime will be placed in the 
directory ```results``` and learned models will be placed in 
the directory ```learned_models```.

### Experiment Configuration
An experiment configuration consists of several parameter settings
corresponding to the various learning parameters described in 
our paper "Timed Automata Learning via SMT solving". While the 
parameter names aim to be self-explanatory, we want to provide
a brief overview of the required parameters and their 
admissible values.
* ```experimentName```: the type of model to be used for the experiment;
see also Section 5.1 for a description of the experiment subjects.
Admissible settings are: ```light, cas-alarm, cas-arming, train, random-3loc,
random-4loc, random-5loc, random6-loc```
* ```nrTrainingTraces```: the number of traces to be used as training data.
Admissible values are positive integer literals lower than or equal to 100 
(100 traces have been precomputed for every experiment subject).
* ```maxNrLocations```: the maximum number of locations of a learned timed
automation. Admissible values are positive integer literals.
* ```maxNrEdges```: the maximum number of edges of a learned timed
   automation. Admissible values are positive integer literals.
* ```edgesPerLocation```: the ratio between the number of edges and the 
number of locations in one learning iteration. This parameter corresponds 
to the variable "el" in Algorithm 1.
Admissible values are positive integer literals.
* ```kUrgency```: the value of k for k-Urgency defined in the preliminaries.
Admissible values are non-negative integer literals.
* ```maxGuardConstant```: the maximum value of constants appearing in guards 
and invariants of learned automata that are affected by trace constraints. 
Larger values may occur to basically denote there is no upper timing bound.
Admissible values are positive integer literals.
* ```incremental```: set to true if incremental SMT solving shall be used.
Admissible values are Boolean literals.
* ```bfsMode```: set to true if constraints shall be solved in a breadth-first
manner. The setting is irrelevant if ```incremental==false```.
Admissible values are Boolean literals.
* ```discreteFirst```: set to true if discrete trace constraints shall be 
solved first before considering timing constraints. 
The setting is irrelevant if ```incremental==false```.
Admissible values are Boolean literals.
* ```solver```: the name of the SMT solver to be used.
Admissible values are ```MATHSAT5, SMTINTERPOL, Z3, PRINCESS, BOOLECTOR,
    CVC4, YICES2 ```
* ```theory```: the theory to be used for formulating constraints. See also
Section 4 and 5 of the paper for a discussion of the theories. Note that
not all theory-solver combinations can be used, as most solvers do not 
implement all theories. Admissible
values are: ```Real``` (real-valued encoding of delays), ```Integer``` (discretized
encoding of time using integers), ```BV``` (discretized
encoding of time using bitvectors), ```FP``` (floating-point encoding of time -- 
not covered in the paper, as it is very inefficient)
* ```roundingFactor```: a factor with which all time values are multiplied to 
enable discretization, see also Section 4.1 (value "r"). If the theory of reals
is used, this value must be set to 1. Otherwise, the model evaluation results 
will be wrong. Admissible values are positive integers.
* ```skipQualityEval```: set to true to avoid computing model quality metrics,
like precision and recall. By setting this value to true, the Uppaal setup 
can be skipped. Admissible values are Boolean literals. 

### Quick Start with Example Configurations
There are example configurations for experiments with all experiment subjects.
They are located in the subdirectories of ```reference_data/example_experiment_properties```
and configured to use the SMT solver SMTInterpol with a real-valued encoding of 
time. Note that some experiments may take a very long time to complete 
(see Section 5), especially when solvers other than YICES2 and Z3 are used. 

### Reference Data from Experiments
We provide reference data presented in our paper resulting 
from the experiments that we performed. The learned models in Graphviz 
dot-format are located in
```
reference_data/reference_learned_models
```
and measurement data can be found in
```
reference_data/reference_results
```


## Additional Experiments & Working with the Source Code 
If you want to configure experiments more freely, a good starting point
is the Java class ```Experiments``` in the package ```graal.learning.smt.experiments```,
which contains static methods to set up experiments. To dig deeper into how 
learning is implemented, the Java class ```TALearnerGeneric``` in the package
```graal.learning.smt``` may serve as a good starting point.
The implementation in this repository is currently a research prototype to
evaluate SMT-based learning of timed automata, so there is limited documentation. 
However, please do not hesitate to contact us if you have any questions. 



