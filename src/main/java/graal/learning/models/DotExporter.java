package graal.learning.models;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class DotExporter {
	public void export(TimedAutomaton ta, String fileName) throws Exception{
		File f = new File(fileName);
		try(FileWriter fw = new FileWriter(f)){
			BufferedWriter bw = new BufferedWriter(fw);
			bw.write(toDot(ta));
			bw.flush();
		}
	}

	public String toDot(TimedAutomaton ta) {
		StringBuilder sb = new StringBuilder();
		appendLine(sb, "digraph g {");
		appendLine(sb, "__start0 [label=\"\" shape=\"none\"];");

		ta.getLocations().forEach(s -> appendLine(sb,stateString(ta,s)));
		ta.getLocations().forEach(s -> appendTransitionLines(sb,ta,s));
		
		appendLine(sb, String.format("__start0 -> %s;", ta.getInitial().getId()));
		appendLine(sb, "}");
		return sb.toString();
	}
	private void appendTransitionLines(StringBuilder sb, TimedAutomaton ta, Location s) {
		List<Edge> trans = ta.getEdges(s);
		if(trans == null)
			return;
		trans.forEach(t -> appendLine(sb, transitionString(t)));
	}
	private String transitionString(Edge t) {
		return String.format("%s -> %s [label=\"%s if %s {%s} \"];", 
				t.getSource().getId(),
				t.getTarget().getId(),
				t.getAction().toString(), 
				t.getGuard().toString(),
				t.getResets().stream().map(Clock::getName).collect(Collectors.joining(",")));
	}

	public static void appendLine(StringBuilder sb, String line){
		sb.append(line);
		sb.append(System.lineSeparator());
	}
	public String stateString(TimedAutomaton ta, Location state){
		if(ta.getInvariant(state) == null)
			return String.format("%s [shape=\"circle\" margin=0 label=\"%s\"];", 
					state.getId(),state.getId());
		else{
			return String.format("%s [shape=\"circle\" margin=0 label=\"%s\", xlabel=\"%s\"];", 
				state.getId(),state.getId(),
				ta.getInvariant(state).toString());
		}
	}
}
