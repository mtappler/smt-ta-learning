package graal.learning.models;

public class Clock {

	private String name;

	public Clock(String name) {
		this.name = name;
	}
	public static Clock nullClock(){
		return new Clock("$null");
	}
	public String getName() {
		return name;
	}
	@Override
	public String toString(){
		return "Clock(" + name + ")";
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
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
		Clock other = (Clock) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}
}
