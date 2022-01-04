package graal.learning.models;

import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

public class ClockValuation {

	private Map<Clock, Double> values;

	// TODO proper equals for clock valuations
	public ClockValuation(Set<Clock> clocks, Double right) {
		values = new LinkedHashMap<>(clocks.size());
		for (Clock c : clocks)
			values.put(c, right);
	}

	public ClockValuation copy() {
		// no deep-copy because clocks and symbols are immutable
		ClockValuation cloned = new ClockValuation(new HashSet<>(values.keySet()), 0.0);
		for (Clock c : values.keySet()) {
			cloned.values.put(c, values.get(c));
		}
		return cloned;
	}

	public static ClockValuation zero(Set<Clock> clocks){
		return new ClockValuation(clocks,0.0);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((values == null) ? 0 : values.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ClockValuation other = (ClockValuation) obj;
		if (values == null) {
			if (other.values != null)
				return false;
		} else if (!values.equals(other.values))
			return false;
		return true;
	}

	public void reset(Clock c) {
		values.put(c, 0.0);
	}

	public void delay(double d) {
		for (Entry<Clock, Double> entry : values.entrySet())
			entry.setValue(entry.getValue() + d);

	}

	public double read(Clock c) {
		return values.get(c);
	}

	@Override
	public String toString() {
		return "{" + values.entrySet().stream()
				.map(cvaluation -> cvaluation.getKey().getName() + "->" + cvaluation.getValue())
				.collect(Collectors.joining(",")) + "}";
	}

	public boolean approxEquals(ClockValuation otherCv) {

		for (Entry<Clock, Double> valuation : values.entrySet()) {
			if (Util.approxEqual(valuation.getValue(), otherCv.read(valuation.getKey())))
				return false;
		}
		return true;

	}
}
