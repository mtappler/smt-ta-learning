package graal.learning.models;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import graal.learning.models.ClockConstraint.Relation;

public class ClockGuard {

	private static final String clockRegex = "([a-zA-Z0-9_]+)";
	private static final String singleGuardRegex = clockRegex + "\\s*(<|<=|>|>=)\\s*(\\d+)";
	public static final String guardRegex = String.format("(true|(%s\\s*&\\s*)*(%s))", singleGuardRegex,
			singleGuardRegex);
	private static final Pattern constraintPattern = Pattern.compile(singleGuardRegex);
	public static final String guardRegexUppaal = String.format("(true|(%s\\s*&&\\s*)*(%s))", singleGuardRegex,
			singleGuardRegex);

	private List<ClockConstraint> conjuncts = new ArrayList<>();

	public ClockGuard() {
	}

	public static ClockGuard guardFromString(String guardString) {
		return guardFromString(guardString, false);
	}

	public static ClockGuard guardFromString(String guardString, boolean uppaal) {
		ClockGuard guard = new ClockGuard();
		if (guardString.trim().equals("true")) {
			return guard;
		}
		Arrays.stream(guardString.split(uppaal ? "&&" : "&")).map(s -> s.replace("(", "").replace(")", ""))
				.filter(s -> !s.trim().equals("true") )
				.map(ClockGuard::constraintFromString)
				.forEach(constr -> guard.add(constr.getClock(), constr.getK(), constr.getRelation()));
		return guard;
	}

	public static ClockConstraint constraintFromString(String constraintString) {
		Matcher constraintMatcher = constraintPattern
				.matcher(constraintString.trim());
		if (!constraintMatcher.matches())
			System.out.println(constraintString);
		int k = Integer.parseInt(constraintMatcher.group(3).trim());

		ClockConstraint.Relation relation = null;
		switch (constraintMatcher.group(2).trim()) {
		case "<":
			relation = ClockConstraint.Relation.lt;
			break;
		case "<=":
			relation = ClockConstraint.Relation.leq;
			break;
		case ">":
			relation = ClockConstraint.Relation.gt;
			break;
		case ">=":
			relation = ClockConstraint.Relation.geq;
			break;
		}
		return new ClockConstraint(new Clock(constraintMatcher.group(1).trim()), k, relation);
	}

	public void add(Clock c, int k, Relation relation) {
		if (relation == Relation.gt || relation == Relation.geq) {
			Optional<ClockConstraint> constrOnSameBelow = conjuncts.stream()
					.filter(constr -> constr.getClock().equals(c)
							&& (constr.getRelation() == Relation.geq || constr.getRelation() == Relation.gt))
					.findAny();
			if (constrOnSameBelow.isPresent()) {
				int effectiveK = Math.max(k, constrOnSameBelow.get().getK());
				Relation effectiveRel = relation == Relation.gt || constrOnSameBelow.get().getRelation() == Relation.gt
						? Relation.gt : Relation.geq;
				constrOnSameBelow.get().setK(effectiveK);
				constrOnSameBelow.get().setRelation(effectiveRel);
				return;
			}
		} else if (relation == Relation.lt || relation == Relation.leq) {
			Optional<ClockConstraint> constrOnSameAbove = conjuncts.stream()
					.filter(constr -> constr.getClock().equals(c)
							&& (constr.getRelation() == Relation.leq || constr.getRelation() == Relation.lt))
					.findAny();
			if (constrOnSameAbove.isPresent()) {
				int effectiveK = Math.min(k, constrOnSameAbove.get().getK());
				Relation effectiveRel = relation == Relation.lt || constrOnSameAbove.get().getRelation() == Relation.lt
						? Relation.lt : Relation.leq;
				constrOnSameAbove.get().setK(effectiveK);
				constrOnSameAbove.get().setRelation(effectiveRel);
				return;
			}
		}
		conjuncts.add(new ClockConstraint(c, k, relation));
	}

	public <T> boolean satisfied(ClockValuation cv) {
		boolean allSat = true;
		for (ClockConstraint cc : conjuncts)
			allSat &= cc.satisfied(cv,0);
		// return conjuncts.stream().allMatch(cc -> cc.satisfied(cv));
		return allSat;
	}

	@Override
	public String toString() {
		if (conjuncts.isEmpty())
			return "true";
		return conjuncts.stream().map(Object::toString).collect(Collectors.joining(" & "));
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((conjuncts == null) ? 0 : conjuncts.hashCode());
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
		ClockGuard other = (ClockGuard) obj;
		if (conjuncts == null) {
			if (other.conjuncts != null)
				return false;
		} else if (!conjuncts.equals(other.conjuncts))
			return false;
		return true;
	}

	public ClockGuard copy() {
		ClockGuard copied = new ClockGuard();
		conjuncts.forEach(constraint -> copied.add(constraint.getClock(), constraint.getK(), constraint.getRelation()));
		return copied;
	}

	public List<ClockConstraint> getConstraints() {
		return conjuncts; // Collections.unmodifiableList(conjuncts);
	}

	public boolean isTrue() {
		return conjuncts.isEmpty();
	}

	
}
