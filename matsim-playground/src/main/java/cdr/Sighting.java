package cdr;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.core.utils.misc.Time;

import java.util.Comparator;

public class Sighting extends Event {

	private double accuracy;

	public Id getAgentId() {
		return agentId;
	}

	public double getAccuracy() {
		return accuracy;
	}

	public void setAccuracy(double accuracy) {
		this.accuracy = accuracy;
	}

	public static class StartTimeComparator implements Comparator<Sighting> {
		
		@Override
		public int compare(Sighting o1, Sighting o2) {
			return Double.compare(o1.getTime(), o2.getTime());
		}
		
	}

    private String cellTowerId;
	private Id agentId;

	public Sighting(Id agentId, long timeInSeconds, String cellTowerId) {
		super(timeInSeconds);
		this.agentId = agentId;
        this.cellTowerId = cellTowerId;
	}

	public String getCellTowerId() {
		return cellTowerId;
	}

	@Override
	public String toString() {
		return Time.writeTime(getTime()) + " - Person: "+agentId + "  at zone: " + cellTowerId;
	}

	@Override
	public String getEventType() {
		return "cdr";
	}
	
	

}
