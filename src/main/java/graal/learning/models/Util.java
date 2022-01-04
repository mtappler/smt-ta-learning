package graal.learning.models;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class Util {
    private static final double EPSILON = 1e-6;

    public static boolean approxEqual(double v1, double v2){
        return Math.abs(v1 - v2) < EPSILON;
    }

    public static TimedAutomaton importFromDotFormat(String fileName) throws Exception {

        List<Location> locationList = new ArrayList<>();
        List<Edge> edgeList = new ArrayList<>();

        Location init = null;
        List<Symbol> symbolList = new ArrayList<>();
        Map<Location, ClockGuard> invariantMap = new HashMap<>();


        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(new File(fileName)));

            String line;
            while ((line = br.readLine()) != null) {

                if (line.contains("circle")) { // online in locations
                    String label, invariant;
                    if (line.contains("xlabel")) {
                        label = line.substring(line.indexOf(" label=") + 8, line.indexOf("\", xlabel"));
                        invariant = line.substring(line.indexOf(" xlabel=") + 9, line.indexOf("]") - 1);
                    }
                    else
                    {
                        label = line.substring(line.indexOf(" label=") + 8, line.indexOf("]") - 1);
                        invariant = "";
                    }

                        if (locationList.stream().filter(l -> l.getId().equals(label)).collect(Collectors.toList()).isEmpty()) { // first time we encounter the location
                            Location loc = new Location(label);
                            locationList.add(loc);
                            if (!invariant.isEmpty())  // if there is an invariant, put it in the invariant map
                                invariantMap.put(loc, ClockGuard.guardFromString(invariant, false));
                        } else {
                            assert (locationList.stream().filter(l -> l.getId().equals(label)).collect(Collectors.toList()).size() == 1);
                            Location loc = locationList.stream().filter(l -> l.getId().equals(label)).collect(Collectors.toList()).get(0);
                            assert (!invariantMap.containsKey(loc));
                            if (!invariant.isEmpty()) { // if there is an invariant, put it in the invariant map
                                invariantMap.put(loc, ClockGuard.guardFromString(invariant, false));

                            }
                        }
                }
                if (line.contains("if")) // only in transitions
                {
                    String sourceName = line.substring(0,line.indexOf("->")-1);
                    String targetName = line.substring(line.indexOf("->")+3, line.indexOf("[label=")-1);
                    String label = line.substring(line.indexOf("[label=")+8, line.indexOf("if")-1 );
                    String guard = line.substring(line.indexOf("if")+3, line.indexOf("{")-1 );
                    String reset = line.substring(line.indexOf("{")+1, line.indexOf("}") );

                    Location sourceLocation, targetLocation;
                    if (locationList.stream().filter(l -> l.getId().equals(sourceName)).collect(Collectors.toList()).isEmpty()) { // first time we encounter the source location
                        sourceLocation = new Location(sourceName);
                        locationList.add(sourceLocation);
                    }
                    else
                        sourceLocation = locationList.stream().filter(l -> l.getId().equals(sourceName)).collect(Collectors.toList()).get(0);

                    if (locationList.stream().filter(l -> l.getId().equals(targetName)).collect(Collectors.toList()).isEmpty()) { // first time we encounter the target location
                        targetLocation = new Location(targetName);
                        locationList.add(targetLocation);
                    }
                    else
                        targetLocation = locationList.stream().filter(l -> l.getId().equals(targetName)).collect(Collectors.toList()).get(0);

                    String[] resets = reset.split(",");
                    List<String> resetStringList = Arrays.stream(resets).filter(clock ->
                            !clock.trim().isEmpty()).collect(Collectors.toList());
                    Set<Clock> resetClockList = new HashSet<>();
                    for (String oneClock : resetStringList) {
                       Clock c = new Clock(oneClock);
                       resetClockList.add(c);
                  }
                    Symbol labelSymbol = new Symbol(label.substring(0,label.length()-1),  label.charAt(label.length()-1)=='!' ? true : false );
                    Edge edge = new Edge(sourceLocation,targetLocation, ClockGuard.guardFromString(guard),labelSymbol,resetClockList);
                    edgeList.add(edge);
                }
                if (line.contains("__") && line.contains("->")) // only in transitions
                {
                    String initName = line.substring(line.indexOf("->")+3, line.indexOf(";"));
                    if (locationList.stream().filter(l -> l.getId().equals(initName)).collect(Collectors.toList()).isEmpty()) { // first time we encounter the target location
                        init = new Location(initName);
                        locationList.add(init);
                    }
                    else
                        init = locationList.stream().filter(l -> l.getId().equals(initName)).collect(Collectors.toList()).get(0);

                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        Set<Clock> clockSet = new HashSet<>();
        // retrieve all clocks in the TA
        for(Edge e : edgeList){
            clockSet.addAll(e.getResets());
            clockSet.addAll(clocksInGuard(e.getGuard()));
        }
        for(ClockGuard inv : invariantMap.values()){
            clockSet.addAll(clocksInGuard(inv));
        }
        List<Clock> clockList = new ArrayList<>(clockSet);
        TimedAutomaton result = new TimedAutomaton(locationList,init,symbolList,clockList);
      for (Edge e: edgeList) {
            result.addEdge(e);
        }
        result.setInvariant(invariantMap);
        return result;

    }

    private static Collection<? extends Clock> clocksInGuard(ClockGuard guard) {
        return guard.getConstraints().stream().map(ClockConstraint::getClock).collect(Collectors.toList());
    }
}
