package guice;

import org.matsim.core.router.TripRouter;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

public class DefaultTripRouter {

	public static TripRouter create(TravelTime travelTime, TravelDisutility travelDisutility) {
		return new TripRouter();
	}

}
