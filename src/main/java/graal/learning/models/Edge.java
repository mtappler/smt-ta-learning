package graal.learning.models;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class Edge {
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((action == null) ? 0 : action.hashCode());
		result = prime * result + ((guard == null) ? 0 : guard.hashCode());
		result = prime * result + ((resets == null) ? 0 : resets.hashCode());
		result = prime * result + ((source == null) ? 0 : source.hashCode());
		result = prime * result + ((target == null) ? 0 : target.hashCode());
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
		Edge other = (Edge) obj;
		if (action == null) {
			if (other.action != null)
				return false;
		} else if (!action.equals(other.action))
			return false;
		if (guard == null) {
			if (other.guard != null)
				return false;
		} else if (!guard.equals(other.guard))
			return false;
		if (resets == null) {
			if (other.resets != null)
				return false;
		} else if (!resets.equals(other.resets))
			return false;
		if (source == null) {
			if (other.source != null)
				return false;
		} else if (!source.equals(other.source))
			return false;
		if (target == null) {
			if (other.target != null)
				return false;
		} else if (!target.equals(other.target))
			return false;
		return true;
	}
	private Location source;
	private Location target;
	private ClockGuard guard;
	private Symbol action;
	private Set<Clock> resets;
	public Location getSource() {
		return source;
	}
	public Edge(Location source, Location target, ClockGuard guard, Symbol action, Set<Clock> resets) {
		super();
		this.source = source;
		this.target = target;
		this.guard = guard;
		this.action = action;
		this.resets = resets;
	}
	public Location getTarget() {
		return target;
	}

	public ClockGuard getGuard() {
		return guard;
	}

	public Symbol getAction() {
		return action;
	}

	public Set<Clock> getResets() {
		return Collections.unmodifiableSet(resets);
	}

	public Edge copy() {
		Edge copied = new Edge(source, target, guard.copy(), action, new HashSet<>(resets));
		return copied;
	}
	@Override
	public String toString() {
		return "Transition [source=" + source.getId() + ", target=" + target.getId() + ", guard=" + guard + ", action=" + action
				+ ", resets=" + resets + "]";
	}
	
	public Edge copyButReplace(Symbol replacement) {
		return new Edge(source, target, guard, replacement, resets);
	}
	public Edge copyButReplace(ClockGuard replacement) {
		return new Edge(source, target, replacement, action, resets);
	}
	public Edge copyButReplaceSource(Location replacementSource) {
		return new Edge(replacementSource, target, guard, action, resets);
	}
	public Edge copyButReplaceTarget(Location replacementTarget) {
		return new Edge(source, replacementTarget, guard, action, resets);
	}
	public Edge copyButReplaceResets(Set<Clock> newResets) {
		return new Edge(source, target, guard, action, newResets);
	}
}
