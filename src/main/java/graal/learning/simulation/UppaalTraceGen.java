package graal.learning.simulation;

import graal.learning.models.Delay;
import graal.learning.models.Symbol;
import graal.learning.models.TimedAutomaton;
import graal.learning.models.Trace;
import graal.learning.smt.experiments.ExperimentUtil;
import org.apache.commons.lang3.tuple.Pair;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class UppaalTraceGen {
    private static final int TIMEOUT = 10;
    private final Random random;
    private final long seed;
    private final Set<String> inputs;
    private final ArrayList<Symbol> alphabet;
    private long uppaalSeed;
    private String uppaalFile = null;
    private static final int MAX_TRIES_GEN_NEG = 20000;

    public UppaalTraceGen(String uppaalFile, int nrTraces, Set<Symbol> alphabet, int maxDelay, int nrSteps,
                          String queryFileName, Optional<String> traceOutFileName, String uppaalExecutable, long seed) throws Exception {
        this(uppaalFile,nrTraces,alphabet,maxDelay,nrSteps,queryFileName,
                traceOutFileName.orElse("traces/dummy_tmp.txt"), uppaalExecutable,seed);
    }
    public UppaalTraceGen(String uppaalFile, int nrTraces, Set<Symbol> alphabet, int maxDelay, int nrSteps,
                          String queryFileName, String traceOutFileName, String uppaalExecutable, long seed) throws Exception {

        this.uppaalFile = addMaxDelayInTempFile(uppaalFile, maxDelay);//uppaalFile;
        this.nrTraces = nrTraces;
        this.maxDelay = maxDelay;
        this.nrSteps = nrSteps;
        this.queryFileName = queryFileName;
        this.traceOutFileName = traceOutFileName;
        this.uppaalExecutable = uppaalExecutable;
        this.seed = seed;
        this.uppaalSeed = seed;
        this.random = new Random(seed);
        this.inputs = alphabet.stream().filter(s -> !s.isOutput()).map(Symbol::getSymbol).collect(Collectors.toSet());
        this.alphabet = new ArrayList<>();
        this.alphabet.addAll(alphabet);
    }

    private String addMaxDelayInTempFile(String uppaalFile, int maxDelay) throws Exception {
        File tempFile = Files.createTempFile("temp_uppaal_file",".xml").toFile();
        File origFile = new File(uppaalFile);
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.parse(origFile);
        doc.getDocumentElement().normalize();
        NodeList locationXmlElems = doc.getElementsByTagName("location");
        String clockName = determineClockName(doc);
        for (int i = 0; i < locationXmlElems.getLength(); i++){
            Node locationNode = locationXmlElems.item(i);
            NodeList locationChildren = locationNode.getChildNodes();
            boolean containsInv = false;
            for (int j = 0; j < locationChildren.getLength(); j++){
                if(locationChildren.item(j) instanceof Element){
                    Element childElem = (Element) locationChildren.item(j);
                    if(childElem.getAttribute("kind").equals("invariant"))
                        containsInv = true;
                }
            }
            if(!containsInv){
                Element invariantElem = locationNode.getOwnerDocument().createElement("label");
                invariantElem.setAttribute("kind", "invariant");
                invariantElem.setTextContent(clockName + " <= " + maxDelay);
                locationNode.appendChild(invariantElem);
            }
        }
        DOMSource source = new DOMSource(doc);
        FileWriter writer = new FileWriter(tempFile);
        StreamResult result = new StreamResult(writer);
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.transform(source, result);
        return tempFile.getAbsolutePath();
    }

    private String determineClockName(Document doc) {
        NodeList declarationNodes = doc.getElementsByTagName("declaration");
        for (int i = 0; i < declarationNodes.getLength(); i++){
            Element declElem = (Element) declarationNodes.item(i);
            String declText = declElem.getTextContent();
            if(declText.contains("clock")){
                int startOfClock = declText.indexOf("clock");
                int endOfClock = declText.indexOf(";", startOfClock);
                return declText.substring(startOfClock + "clock".length(), endOfClock).trim();
            }
        }
        return null;
    }

    private int nrTraces = 0;
    private int maxDelay = 0;
    private int nrSteps = 0;
    private String queryFileName = null;
    private String traceOutFileName = null;
    private String uppaalExecutable = null;


    public Pair<List<Trace>,List<Trace>> generatePosAndNegTraces(TimedAutomaton ta, int nrTraces,
                                                                 Optional<Pair<String,String>> outputFiles) throws IOException, InterruptedException {
        List<Trace> positiveTraces = new ArrayList<>();
        for (int i = 0; i < nrTraces; i++){
            int steps = random.nextInt(nrSteps) + 1;
            Trace tr = generateSinglePositiveTrace(steps);
            if(tr == null){
                i--;
                continue;
            }
            if(!ta.accepts(tr,1)) {
                System.out.println(tr.toString());
                throw new RuntimeException("Generated traces from TA that is not accept by the TA.");
            }
            positiveTraces.add(tr);
        }
        List<Trace> negativeTraces = generateNegativeTraces(ta, nrTraces);
        if(outputFiles.isPresent()){
            writeTracesToFile(positiveTraces, outputFiles.get().getLeft());
            writeTracesToFile(negativeTraces, outputFiles.get().getRight());
        }
        return Pair.of(positiveTraces,negativeTraces);
    }

    private List<Trace> generateNegativeTraces(TimedAutomaton ta, int nrTraces) throws IOException, InterruptedException {
        List<Trace> negativeTraces = new ArrayList<>();
        int tries = 0;
        while(negativeTraces.size() < nrTraces){
            int steps = random.nextInt(nrSteps) + 1;
            Trace tr = generateSinglePositiveTrace(steps);
            if(tr == null){
                tries++;
                if(tries > MAX_TRIES_GEN_NEG)
                    throw new RuntimeException("Cannot generate negative traces");
                continue;
            }
            Delay randomDelay = Delay.mkD(random.nextDouble() * maxDelay);
            Symbol randomSymbol = generateRandomSymbol();
            tr.append(randomDelay,randomSymbol);
            if(!ta.accepts(tr,1)){
                negativeTraces.add(tr);
            }
            tries ++;
            if(tries > MAX_TRIES_GEN_NEG)
                throw new RuntimeException("Cannot generate negative traces");
        }
        return negativeTraces;
    }

    private Symbol generateRandomSymbol() {
        return this.alphabet.get(random.nextInt(this.alphabet.size()));
    }

    public void generateTraces() throws IOException, InterruptedException {
        List<Trace> traces = new ArrayList<>();
        for(int i = 0; i < nrTraces; i++){
            int steps = chooseSteps();
            Trace trace = generateSinglePositiveTrace(steps);
            if(trace == null){
                i--;
                continue;
            }
            System.out.printf("Trace %d \n",i + 1);
            System.out.println(trace.toString());
            traces.add(trace);
        }
        writeTrainingTracesToFile(traces);
    }

    private Trace generateSinglePositiveTrace(int steps) throws IOException, InterruptedException {
        writeQFile(steps);
        String traceStdout = execUppaal();
        if(traceStdout.contains("did not terminate")){
            System.out.println("Uppaal did not terminate");
            return null;
        }
        return parseUppaalTrace(traceStdout);
    }

    private void writeTrainingTracesToFile(List<Trace> traces) throws IOException {
        writeTracesToFile(traces,this.traceOutFileName);
    }


    private void writeTracesToFile(List<Trace> traces, String fileName) throws IOException {
        String traceStr = traces.stream().map(Trace::toString).collect(Collectors.joining("," + System.lineSeparator()));
        new File(fileName).getParentFile().mkdirs();
        BufferedWriter writer = new BufferedWriter(new FileWriter(fileName));
        writer.write(traceStr);
        writer.close();
    }

    private Trace parseUppaalTrace(String traceStdout) {
        String[] traceLines = traceStdout.split(System.lineSeparator());
        List<String> rawTraceContent = Arrays.stream(traceLines).dropWhile(s -> !s.startsWith("state"))
                .filter(s -> !s.contains(" -- Formula is satisfied."))
                .filter(s -> !s.isBlank())
                .collect(Collectors.toList());
        boolean delayIsNext = true;
        Double delay = null;
        Symbol s = null;
        List<Pair<Symbol, Delay>> trace = new ArrayList<>();
        for(int i = 0; i < rawTraceContent.size(); i++){
            String traceLine = rawTraceContent.get(i);
            if (delayIsNext && traceLine.startsWith("delay")){
                delay = processDelayLine(traceLine);
                delayIsNext = false;
            } else if (!delayIsNext && traceLine.contains("transition")){
                String actionLine = rawTraceContent.get(i+1);
                s = processActionLine(actionLine);
                trace.add(Pair.of(s,Delay.mkD(delay)));
                delayIsNext = true;
            }
        }
        return new Trace(trace);
    }

    private Symbol processActionLine(String actionLine) {
        int exclMarkIndex = actionLine.indexOf("!"); // in our TA, all actions have ! as suffix, so we look for that
        if (exclMarkIndex == -1){
            throw new RuntimeException("Error parsing action line: " + actionLine);
        } else {
            int nameIndex = exclMarkIndex;
            while (actionLine.charAt(nameIndex) != ' '){
                nameIndex--;
            }
            String actionName = actionLine.substring(nameIndex + 1, exclMarkIndex);
            if (inputs.contains(actionName)){
                return new Symbol(actionName, false);
            } else {
                return new Symbol(actionName, true);
            }
        }
    }

    private Double processDelayLine(String traceLine) {
        // check infinite (arbitrary) delay
        if (traceLine.contains("delay inf")){
            return random.nextDouble() * maxDelay;
        } else {
            return Double.parseDouble(traceLine.replace("delay", "").trim());
        }
    }

    private int chooseSteps() {
        return random.nextInt(nrSteps) + 1;
    }

    public String executeWithtimeout(Callable<String> callable,Runnable onTimeout, int timeOut) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<String> future = executor.submit(callable);
        try {
            String result = future.get(timeOut, TimeUnit.SECONDS);
            executor.shutdownNow();
            return result;
        } catch (TimeoutException | InterruptedException | ExecutionException e) {
            System.err.println("Timeout");
            onTimeout.run();
            executor.shutdownNow();
            return "Task did not terminate";
        }
    }

    private String execUppaal() throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(uppaalExecutable, "-t", "0", "--exploration", "1",
                "--rand-heur", "4", "--rdepth", "50", "--rtimeout", "10",  uppaalFile, queryFileName,
                "--seed", Long.toString(this.uppaalSeed++));
        Process process = pb.start();
        String stdout = executeWithtimeout(() -> {
            String stdoutInner = new String(process.getInputStream().readAllBytes());
            String stderrInner = new String(process.getErrorStream().readAllBytes());
            return stdoutInner;
        }, () -> process.destroyForcibly(), TIMEOUT);

        return stdout;
    }

    private void writeQFile(int steps) throws IOException {
        new File(queryFileName).getParentFile().mkdirs();
        BufferedWriter writer = new BufferedWriter(new FileWriter(queryFileName));
        writer.write(String.format("E<> s==%d",steps));
        writer.close();
    }

    public static void main(String[] args) throws Exception {
        createCASDiamondTraces();
    }
    private static void createRandomTATraces() throws Exception {
        Properties pathProperties = ExperimentUtil.loadProperties("paths.prop");
        String uppaalExecutable = pathProperties.getProperty("uppaalExecutable");
        String baseName = "loc_7_clock_1_trans_11_in_out_2_cmax_15_";
        Set<Symbol> alphabet = new HashSet<>();
        alphabet.add(new Symbol("in0", false));
        alphabet.add(new Symbol("in1", false));
        alphabet.add(new Symbol("out0", true));
        alphabet.add(new Symbol("out1", true));
        int maxDelay = 20;
        int nrSteps = 12; // 12 steps for 7 and 8 loc, 10 steps for 5 loc
        long seed = 1337;
        int nrTraces = 200;
        for (int i = 9; i <= 10; i++) {
            UppaalTraceGen traceGen = new UppaalTraceGen("../models/random/" + baseName + i + ".xml",
                    nrTraces, alphabet, maxDelay, nrSteps,
                    "../models/random/" + baseName + i + ".q", "src/main/resources/traces/" + baseName + i + ".txt",
                    uppaalExecutable,
                    seed);
            traceGen.generateTraces();
        }
    }
    private static void createLightTraces() throws Exception {
        Properties pathProperties = ExperimentUtil.loadProperties("paths.prop");
        String uppaalExecutable = pathProperties.getProperty("uppaalExecutable");
        Set<Symbol> alphabet = new HashSet<>();
        alphabet.add(new Symbol("press", false));
        alphabet.add(new Symbol("release", false));
        alphabet.add(new Symbol("starthold", true));
        alphabet.add(new Symbol("endhold", true));
        alphabet.add(new Symbol("touch", true));
        int maxDelay = 20;
        int nrSteps = 10;
        long seed = 1337;
        int nrTraces = 100;
        UppaalTraceGen traceGen = new UppaalTraceGen("../models/light_model.xml",nrTraces,alphabet,maxDelay,nrSteps,
                "../models/light_model.q","src/main/resources/traces/light.txt",
                uppaalExecutable,
                seed);
        traceGen.generateTraces();
    }

    private static void createTrainTraces() throws Exception {
        Properties pathProperties = ExperimentUtil.loadProperties("paths.prop");
        String uppaalExecutable = pathProperties.getProperty("uppaalExecutable");
        Set<Symbol> alphabet = new HashSet<>();
        alphabet.add(new Symbol("start", false));
        alphabet.add(new Symbol("stop", false));
        alphabet.add(new Symbol("go", false));
        alphabet.add(new Symbol("appr", true));
        alphabet.add(new Symbol("leave", true));
        alphabet.add(new Symbol("enter", true));
        int maxDelay = 20;
        int nrSteps = 10;
        long seed = 1337;
        int nrTraces = 200;
        UppaalTraceGen traceGen = new UppaalTraceGen("../models/train.xml",nrTraces,alphabet,maxDelay,nrSteps,
                "../models/train.q","src/main/resources/traces/train.txt",
                uppaalExecutable,
                seed);
        traceGen.generateTraces();
    }

    private static void createCASRestTraces() throws Exception {
        Properties pathProperties = ExperimentUtil.loadProperties("paths.prop");
        String uppaalExecutable = pathProperties.getProperty("uppaalExecutable");
        Set<Symbol> alphabet = new HashSet<>();
        alphabet.add(new Symbol("unlock", false));
        alphabet.add(new Symbol("alarmOn", false));

        alphabet.add(new Symbol("flashOff", true));
        alphabet.add(new Symbol("soundOff", true));
        alphabet.add(new Symbol("flashOn", true));
        alphabet.add(new Symbol("soundOn", true));
        int maxDelay = 30;
        int nrSteps = 20;
        long seed = 1337;
        int nrTraces = 100;
        UppaalTraceGen traceGen = new UppaalTraceGen("../models/CAS_rest.xml",nrTraces,alphabet,maxDelay,nrSteps,
                "../models/CAS_rest_model.q","src/main/resources/traces/CAS_rest.txt",
                uppaalExecutable,
                seed);
        traceGen.generateTraces();
    }
    private static void createCASDiamondTraces() throws Exception {
        Properties pathProperties = ExperimentUtil.loadProperties("paths.prop");
        String uppaalExecutable = pathProperties.getProperty("uppaalExecutable");
        Set<Symbol> alphabet = new HashSet<>();
        alphabet.add(new Symbol("unlock", false));
        alphabet.add(new Symbol("lock", false));
        alphabet.add(new Symbol("open", false));
        alphabet.add(new Symbol("close", false));
        alphabet.add(new Symbol("armedOn", true));
        alphabet.add(new Symbol("armedOff", true));
        alphabet.add(new Symbol("alarmOn", true));
        int maxDelay = 10;
        int nrSteps = 12;
        long seed = 1337;
        int nrTraces = 200;
        UppaalTraceGen traceGen = new UppaalTraceGen("../models/CAS_diamond.xml",nrTraces,alphabet,maxDelay,nrSteps,
                "../models/cas_diamond_model.q","src/main/resources/traces/cas_diamond.txt",
                uppaalExecutable,
                seed);
        traceGen.generateTraces();
    }
    private static void createCASTraces() throws Exception {
        Properties pathProperties = ExperimentUtil.loadProperties("paths.prop");
        String uppaalExecutable = pathProperties.getProperty("uppaalExecutable");
        Set<Symbol> alphabet = new HashSet<>();
        alphabet.add(new Symbol("unlock", false));
        alphabet.add(new Symbol("lock", false));
        alphabet.add(new Symbol("open", false));
        alphabet.add(new Symbol("close", false));
        alphabet.add(new Symbol("armedOn", true));
        alphabet.add(new Symbol("armedOff", true));
        alphabet.add(new Symbol("flashOff", true));
        alphabet.add(new Symbol("soundOff", true));
        alphabet.add(new Symbol("flashOn", true));
        alphabet.add(new Symbol("soundOn", true));
        int maxDelay = 40;
        int nrSteps = 20;
        long seed = 1337;
        int nrTraces = 100;
        UppaalTraceGen traceGen = new UppaalTraceGen("../models/casMaxSpecNewest.xml",nrTraces,alphabet,maxDelay,nrSteps,
                "../models/cas_model.q","src/main/resources/traces/cas_new.txt",
                uppaalExecutable,
                seed);
        traceGen.generateTraces();
    }

}
