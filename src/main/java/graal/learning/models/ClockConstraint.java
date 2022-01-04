package graal.learning.models;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

public class ClockConstraint {
	
	public Clock getClock(){
		return c;
	}

	public int getK() {
		return k;
	}

	public void setK(int k) {
		this.k = k;
	}

	public Relation getRelation() {
		return relation;
	}

	public void setRelation(Relation relation) {
		this.relation = relation;
	}

	public static enum Relation{
		lt,leq,gt,geq;
		@Override
		public String toString(){
			switch(this){
			case lt:
				return "<";
			case leq:
				return "<=";
			case gt:
				return ">";
			case geq:
				return ">=";
			default: 
				return "???";
			}
		}
	}
	private Clock c = null;
	private int k = 0;
	private Relation relation;

	public ClockConstraint(Clock c, int k, Relation relation){
		this.c = c;
		this.k = k;
		this.relation = relation;
	}

	@Override 
	public String toString(){
		return String.format("%s %s %d", c.getName(),
				relation.toString(), k);
	}

	public boolean satisfied(ClockValuation cv, double additionalDelay) {
		double value = cv.read(c) + additionalDelay;
		switch(relation){
			case lt:
				return value < k;
			case leq:
				return value <= k;
			case geq:
				return value >= k;
			case gt:
				return value > k;
			default:
				throw new NotImplementedException("Unknow relation");
		}

	}
}
