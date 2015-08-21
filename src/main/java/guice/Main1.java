package guice;

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

public class Main1 {

	public static void main(String[] args) {
		Config config = ConfigUtils.createConfig();
		TravelTime travelTime = new SpecialTravelTime();
		TravelDisutility travelDisutility = DefaultTravelDisutility.create(travelTime);
		TripRouter tripRouter = DefaultTripRouter.create(travelTime, travelDisutility);
		Matsim matsim = Matsim.create(config, tripRouter);
		matsim.run();
	}

}
