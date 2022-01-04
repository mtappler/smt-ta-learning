package graal.learning.smt.experiments;

import graal.learning.models.Delay;
import graal.learning.models.Symbol;
import graal.learning.models.Trace;
import org.apache.commons.lang3.tuple.Pair;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

import static graal.learning.models.Symbol.mkS;
import static graal.learning.models.Delay.mkD;

public class ExampleTraces {
    private static List<Trace> readUppaalTraceFile(String fileName, int nrTraces, Set<String> overrideInputs, int maxLen) {
        return readTraceFile(fileName)
                .stream()
                .limit(nrTraces)
                .map(t -> ExampleTraces.changeToInputs(t,overrideInputs))
                .map(t -> limitTraceLen(t,maxLen))
                .collect(Collectors.toList());
    }

    private static Trace limitTraceLen(Trace t, int maxLen) {
        if(maxLen < 0)
            return t;
        else{
            return t.limit(maxLen);
        }
    }

    private static Trace changeToInputs(Trace t, Set<String> newInputs) {
        List<Pair<Symbol, Delay>> rawTrace = t.getTrace();
        List<Pair<Symbol,Delay>> rawChangedTrace = new ArrayList<>();
        for (Pair<Symbol, Delay> elem : rawTrace){
            String symbolString = elem.getLeft().getSymbol().replace("!", "");
            if(newInputs.contains(symbolString)){
                rawChangedTrace.add(Pair.of(new Symbol(symbolString + "?", false), elem.getRight()));
            } else {
                rawChangedTrace.add(elem);
            }
        }
        return new Trace(rawChangedTrace);
    }

    private static Trace fromUppaalString(String str) {
        return fromString(str);
    }

    public static List<Trace> readTraceFile(String fileName){
        return readTraceFile(fileName,-1);
    }
    public static List<Trace> readTraceFile(String fileName, int limit){
        System.out.println(fileName);
        StringBuilder fileContent = readResourceFile(fileName);
        String[] splitIntoRawTraces = fileContent.toString().trim().split("]\\s*,\\s*\\[");
        List<Trace> traces = new ArrayList<>();
        for(String trString : splitIntoRawTraces){
            String rawTraceString = trString.replace(']', ' ').replace('[', ' ');
            traces.add(fromString(rawTraceString));
            if(traces.size() == limit)
                break;
        }
        return traces;
    }

    private static StringBuilder readResourceFile(String fileName) {
        InputStream is = (ExampleTraces.class.getClassLoader().getResourceAsStream(fileName));
        StringBuilder fileContent = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
            String line;
            while ((line = br.readLine()) != null) {
                fileContent.append(line.trim());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return fileContent;
    }

    public static Trace fromString(String str){
        List<Pair<Symbol,Delay>> rawTrace = new ArrayList<>();
        boolean readingDelay = false;
        boolean readingAction = false;
        StringBuilder delayString = new StringBuilder();
        StringBuilder actionString = new StringBuilder();

        for (int i = 0; i < str.length(); i ++){
            char c = str.charAt(i);
            if(c == '('){
                readingDelay = true;
            } else if (readingDelay && c == ','){
                readingAction = true;
                readingDelay = false;
            } else if(readingAction && c == ')') {
              readingAction = false;
                double delayVal = Double.parseDouble(delayString.toString().trim());
              String trimmedAction = actionString.toString().replace("\"","").trim();
              rawTrace.add(Pair.of(mkS(trimmedAction),mkD(delayVal)));
              delayString.setLength(0);
              actionString.setLength(0);

            } else if (readingDelay){
                delayString.append(c);
            } else if (readingAction){
                actionString.append(c);
            }
        }
        return new Trace(rawTrace);
    }
    public static List<Trace> lightTraces(){
        return readTraceFile("light_traces_all.txt");
    }

    public static List<Trace> trainTraces(){
        return readTraceFile("train_traces.txt");
    }


    public static List<Trace> casTraces(int nrTraces){
        return casTraces(nrTraces, -1);
    }
    public static List<Trace> casTraces(int nrTraces, int maxLen){
        Set<String> inputs = new HashSet<>();
        inputs.add("open");
        inputs.add("close");
        inputs.add("lock");
        inputs.add("unlock");

        return readUppaalTraceFile("CAS_traces.txt", nrTraces,inputs, maxLen);
    }


}
