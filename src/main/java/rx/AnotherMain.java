package rx;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.internal.HasPersonId;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.*;
import org.matsim.core.controler.corelisteners.ControlerDefaultCoreListenersModule;
import org.matsim.core.scenario.ScenarioByInstanceModule;
import org.matsim.core.scenario.ScenarioUtils;
import rx.schedulers.Schedulers;

public class AnotherMain {

	public static void main(String[] args) throws InterruptedException {
		Logger.getRootLogger().setLevel(Level.OFF);
		Config config = ConfigUtils.loadConfig("input/equil/config.xml");
		config.controler().setLastIteration(1);
		config.controler().setOutputDirectory("output/equil");
		config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
		final Scenario scenario = ScenarioUtils.loadScenario(config);
		com.google.inject.Injector injector = Injector.createInjector(config, new AbstractModule() {
			@Override
			public void install() {
				install(new NewControlerModule());
				install(new ControlerDefaultCoreListenersModule());
				install(new ControlerDefaultsModule());
				install(new ScenarioByInstanceModule(scenario));
				install(new RxModule());
			}
		});
		MatsimRx matsimRx = injector.getInstance(MatsimRx.class);
		matsimRx.iterations().flatMap(iteration -> iteration.events().count()).forEach(i -> {throw new RuntimeException();});
		matsimRx.iterations().forEach(iteration -> {
			iteration.events()
					.filter(e -> e instanceof HasPersonId)
					.groupBy(e -> ((HasPersonId) e).getPersonId())
					.flatMap(g -> g.count())
					.forEach(i -> {throw new RuntimeException();});
		});
		matsimRx.run();
	}

}
