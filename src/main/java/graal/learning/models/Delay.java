package graal.learning.models;

public class Delay {
    private double value;

    public Delay(double value) {
        this.value = value;
    }

    public static Delay mkD(double value){
        return new Delay(value);
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }
}
