package guice;

import org.matsim.core.config.Config;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.router.TripRouter;
import org.matsim.core.scoring.ScoringFunction;

public class Matsim {
	public Matsim(Config config, TripRouter tripRouter) {

	}

	public void run() {

	}

	public static Matsim create(Config config, TripRouter tripRouter) {
		return new Matsim(config, tripRouter);
	}

	public static Matsim create(AbstractModule abstractModule) {
		return null;
	}
}
