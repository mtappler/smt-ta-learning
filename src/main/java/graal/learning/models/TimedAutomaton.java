package graal.learning.models;

import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.Map.Entry;

public class TimedAutomaton {

	private List<Location> locations; // list because lists are easier to shuffle
	private Location initial;
	private List<Symbol> alphabet;
	private List<Clock> clocks;
	private Map<Location, ClockGuard> invariant = new HashMap<>();
	private Map<Location, List<Edge>> edges = new HashMap<>();
	private List<Edge> edgeList = null;
	private LinkedHashSet<Clock> clocksSet;

	public TimedAutomaton(List<Location> states, Location initial, List<Symbol> alphabet, List<Clock> clocks) {
		super();
		this.locations = states;
		this.initial = initial;
		this.alphabet = alphabet;
		this.clocks = clocks;
	}


	public boolean accepts(Trace trace,int roundingFactor){
		return accepts(trace,roundingFactor,null,null);
	}
	public boolean accepts(Trace trace, int roundingFactor, Set<Location> coveredLocationStorage, Set<Edge> coveredEdgeStorage){
		List<Pair<Symbol, Delay>> traceList = trace.getTrace();
		Location currentLocation = initial;
		ClockValuation currClockVal = ClockValuation.zero(getClocksSet());
		for(Pair<Symbol, Delay> step : traceList){
			if(coveredLocationStorage != null)
				coveredLocationStorage.add(currentLocation);
			Delay d = step.getRight();
			Symbol s = step.getLeft();
			currClockVal.delay(d.getValue() * roundingFactor);
			if(invariant.get(currentLocation) != null && !invariant.get(currentLocation).satisfied(currClockVal)){
				return false;
      }
			List<Edge> edgesFromCurr = edges.get(currentLocation);
			Edge matchingEdge = null;
			if(edgesFromCurr == null)
				edgesFromCurr = Collections.emptyList();
				
			for(Edge e : edgesFromCurr){
				if(e.getAction().equals(s) && e.getGuard().satisfied(currClockVal)){
					ClockValuation currClockValCopy = currClockVal.copy();
					for (Clock r : e.getResets())
						currClockValCopy.reset(r);
					if(invariant.get(e.getTarget()) == null || invariant.get(e.getTarget()).satisfied(currClockValCopy)) {
						matchingEdge = e;
						break;
					}
				}
			}
			if(matchingEdge == null)
				return false;
			if(coveredEdgeStorage != null)
				coveredEdgeStorage.add(matchingEdge);
			for (Clock r : matchingEdge.getResets())
				currClockVal.reset(r);
			currentLocation = matchingEdge.getTarget();
		}
		if(coveredLocationStorage != null)
			coveredLocationStorage.add(currentLocation);
		return true;
	}

	// simply check if automaton is connected without checking guard or
	// something
	// like that
	public boolean connected() {
		Set<Location> visited = new HashSet<>();
		Queue<Location> schedule = new LinkedList<>();
		schedule.add(initial);
		while (!schedule.isEmpty()) {
			Location current = schedule.remove();
			List<Edge> currentTrans = edges.get(current);
			visited.add(current);
			if (visited.size() == locations.size())
				break;
			if (currentTrans != null) {
				for (Edge t : currentTrans) {
					if (!visited.contains(t.getTarget()))
						schedule.add(t.getTarget());
				}
			}
		}

		return visited.size() == locations.size();
	}

	public List<Location> getLocations() {
		return locations;
	}

	public void setLocations(List<Location> locations) {
		this.locations = locations;
	}

	public Location getInitial() {
		return initial;
	}

	public void setInitial(Location initial) {
		this.initial = initial;
	}

	public List<Clock> getClocks() {
		return clocks;
	}

	public void setClocks(List<Clock> clocks) {
		this.clocks = clocks;
	}

	public Map<Location, ClockGuard> getInvariant() {
		return invariant;
	}

	public void setInvariant(Map<Location, ClockGuard> invariant) {
		this.invariant = invariant;
	}

	private Map<Location, List<Edge>> getEdges() {
		return edges;
	}

	private void setEdges(Map<Location, List<Edge>> edges) {
		this.edges = edges;
	}

	public Edge addEdge(Location source, Location target, ClockGuard guard, Set<Clock> resets, Symbol symbol) {
		if (!edges.containsKey(source))
			edges.put(source, new ArrayList<>());
		Edge newTrans = new Edge(source, target, guard, symbol, resets);
		if (edges.get(source).contains(newTrans))
			return newTrans;
		edges.get(source).add(newTrans);
		return newTrans;
	}

	public void addInvariant(Location s, ClockGuard guard) {
		invariant.put(s, guard);
	}

	public List<Edge> getEdges(Location currentState) {
		return edges.get(currentState);
	}

	public List<Edge> getInputEdges(Location currentState) {
		List<Edge> inputTrans = new ArrayList<>();
		if (edges.get(currentState) != null) {
			for (Edge t : edges.get(currentState)) {
				if (!t.getAction().isOutput()) {
					inputTrans.add(t);
				}
			}
		}
		return inputTrans;
	}

	public List<Edge> getOutputTransitions(Location currentState) {
		List<Edge> outputTrans = new ArrayList<>();
		if (edges.get(currentState) != null) {
			for (Edge t : edges.get(currentState)) {
				if (t.getAction().isOutput()) {
					outputTrans.add(t);
				}
			}
		}
		return outputTrans;
	}

	public ClockGuard getInvariant(Location currentState) {
		return invariant.get(currentState);
	}

	public TimedAutomaton copy() {
		TimedAutomaton copied = new TimedAutomaton(new ArrayList<>(locations), initial, alphabet, new ArrayList<>(clocks));
		for (Location s : locations) {
			if (edges.get(s) != null) {
				List<Edge> copiedTrans = new ArrayList<>();
				for (Edge t : edges.get(s)) {
					copiedTrans.add(t.copy());
				}
				copied.edges.put(s, copiedTrans);
			}
		}
		HashMap<Location,ClockGuard> invariantCopy = new HashMap<>();
		for (Entry<Location, ClockGuard> invEntry : this.invariant.entrySet()){
			invariantCopy.put(invEntry.getKey(), invEntry.getValue().copy());
		}
		copied.setInvariant(invariantCopy);
		return copied;
	}

	public List<Edge> getEdgeList() {
		if (edgeList == null) {
			edgeList = new ArrayList<>();
			for (List<Edge> transitionsForStates : edges.values()) {
				if (transitionsForStates != null)
					transitionsForStates.forEach(edgeList::add);
			}
		}
		return edgeList;// Collections.unmodifiableList(transitionList);
	}

	public void resetTransitionList() {
		edgeList = null;
	}

	private void addEdge(Location source, Edge edge) {
		if (edges.get(source) == null)
			edges.put(source, new ArrayList<>());
		if (edges.get(source).contains(edge))
			return;
		edges.get(source).add(edge);
	}

	public Set<Clock> getClocksSet() {
		if (this.clocksSet == null) {
			this.clocksSet = new LinkedHashSet<>(clocks);
		}
		return clocksSet;
	}

	public void removeEdges(Location s) {
		edges.remove(s);
	}

	public List<Symbol> getAlphabet() {
		Set<Symbol> alphabetSet = new HashSet<>();
		if(alphabet.isEmpty()){
			for(Edge e : this.getEdgeList()){
				alphabetSet.add(e.getAction());
			}
		}
		return new ArrayList<>(alphabetSet);
	}

	public void setAlphabet(List<Symbol> alphabet) {
		this.alphabet = alphabet;
	}

	public void removeTransition(Edge selectedTrans) {
		edges.get(selectedTrans.getSource()).remove(selectedTrans);
		resetTransitionList();
	}

	public void addEdge(Edge edge) {
		addEdge(edge.getSource(), edge);
	}

	public void removeInputs(Location source) {
		List<Edge> edgeFromLoc = edges.get(source);
		if (edgeFromLoc != null) {
			boolean removed = edgeFromLoc.removeIf(t -> !t.getAction().isOutput());
			if (removed)
				resetTransitionList();
		}
	}

	public Pair<Set<Location>, Set<Edge>> findCoveredElements(Trace t, int roundingFactor) {
		Set<Location> coveredLocations = new HashSet<>();
		Set<Edge> coveredEdges = new HashSet<>();
		accepts(t,roundingFactor,coveredLocations,coveredEdges);
		return Pair.of(coveredLocations,coveredEdges);
	}
}
