package inject;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;

public class BikeTravelTime implements TravelTime {
	@Override
	public double getLinkTravelTime(Link link, double time, Person person, Vehicle vehicle) {
		return 0;
	}
}
