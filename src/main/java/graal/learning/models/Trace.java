package graal.learning.models;

import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class Trace{
    private List<Pair<Symbol, Delay>> trace;

    public List<Pair<Symbol, Delay>> getTrace() {
        return trace;
    }

    public Trace(List<Pair<Symbol, Delay>> trace) {
        this.trace = trace;
    }

    public Set<Symbol> usedDiscreteActions(){
        return trace.stream().map(p -> p.getLeft()).collect(Collectors.toSet());
    }
    @Override
    public String toString() {
        return "[" + trace.stream().map(p -> String.format("(%f, \"%s\")",p.getRight().getValue(),p.getLeft().toString()))
                .collect(Collectors.joining(",")) + "]";
    }

    public static Trace mkTr(Pair<Symbol,Delay> ... elems) {
        List<Pair<Symbol, Delay>> trace = new ArrayList<>(Arrays.asList(elems));
        return new Trace(trace);
    }

    public Pair<Symbol,Delay> get(int i){
        return trace.get(i);
    }
    public int length() {
        return trace.size();
    }

    public Trace limit(int maxLen) {
        if(maxLen > length())
            return this;
        return  new Trace(new ArrayList<>(trace.subList(0,maxLen)));
    }

    public void append(Delay delay, Symbol symbol) {
        trace.add(Pair.of(symbol,delay));
    }
}
