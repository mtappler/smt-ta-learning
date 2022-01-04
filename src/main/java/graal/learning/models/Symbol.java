package graal.learning.models;

import java.util.Objects;

public class Symbol {
    private String symbol;
    private boolean isOutput;

    public Symbol(String symbol, boolean isOutput) {
        this.symbol = symbol;
        this.isOutput = isOutput;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Symbol symbol1 = (Symbol) o;
        return isOutput == symbol1.isOutput && Objects.equals(symbol, symbol1.symbol);
    }

    @Override
    public int hashCode() {
        return Objects.hash(symbol, isOutput);
    }

    public static Symbol mkS(String symbol){
        boolean output = symbol.endsWith("!");
        return new Symbol(symbol.substring(0, symbol.length()-1),output);
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public boolean isOutput() {
        return isOutput;
    }

    public void setOutput(boolean output) {
        isOutput = output;
    }

    @Override
    public String toString() {
        return symbol + (isOutput ? "!" : "?");
    }
}
