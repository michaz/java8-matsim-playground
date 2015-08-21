package guice;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;

import javax.inject.Inject;

public class DefaultTravelDisutility implements TravelDisutility {

	@Inject
	DefaultTravelDisutility(TravelTime travelTime) {

	}

	public static TravelDisutility create(TravelTime travelTime) {
		return new DefaultTravelDisutility(travelTime);
	}

	@Override
	public double getLinkTravelDisutility(Link link, double v, Person person, Vehicle vehicle) {
		return 0;
	}

	@Override
	public double getLinkMinimumTravelDisutility(Link link) {
		return 0;
	}
}
