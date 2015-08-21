package guice;

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.router.util.TravelTime;

public class Main2 {

	public static void main(String[] args) {
		Config config = ConfigUtils.createConfig();
		Matsim matsim = Matsim.create(new AbstractModule() {
			@Override
			public void install() {
				bind(Config.class).toInstance(config);
				bind(TravelTime.class).to(SpecialTravelTime.class);
			}
		});
		matsim.run();
	}

}
